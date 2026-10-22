package br.com.courttime.service;

import br.com.courttime.entity.CategoriaParticipante;
import br.com.courttime.entity.ParticipanteReserva;
import br.com.courttime.entity.Perfil;
import br.com.courttime.entity.Quadra;
import br.com.courttime.entity.Reserva;
import br.com.courttime.entity.StatusReserva;
import br.com.courttime.entity.Usuario;
import br.com.courttime.entity.VinculoParticipante;
import br.com.courttime.repository.AuditoriaUsuarioRepository;
import br.com.courttime.repository.AjusteContribuicaoApmRepository;
import br.com.courttime.repository.BloqueioQuadraRepository;
import br.com.courttime.repository.ConferenciaDocumentoRepository;
import br.com.courttime.repository.ContribuicaoApmRepository;
import br.com.courttime.repository.DisponibilidadeQuadraRepository;
import br.com.courttime.repository.DocumentoReservaRepository;
import br.com.courttime.repository.ParticipanteReservaRepository;
import br.com.courttime.repository.QuadraRepository;
import br.com.courttime.repository.ReservaRepository;
import br.com.courttime.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import br.com.courttime.support.BancoDeTeste;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "COURTTIME_POSTGRES_TEST", matches = "true")
class ParticipanteReservaPostgreSqlIntegrationTest {

    @Autowired
    private ParticipanteReservaService participanteService;

    @Autowired
    private DataSource dataSource;
    @Autowired
    private ParticipanteReservaRepository participanteRepository;
    @Autowired
    private AjusteContribuicaoApmRepository ajusteContribuicaoRepository;
    @Autowired
    private ContribuicaoApmRepository contribuicaoRepository;
    @Autowired
    private ConferenciaDocumentoRepository conferenciaDocumentoRepository;
    @Autowired
    private DocumentoReservaRepository documentoReservaRepository;
    @Autowired
    private ReservaRepository reservaRepository;
    @Autowired
    private AuditoriaUsuarioRepository auditoriaRepository;
    @Autowired
    private BloqueioQuadraRepository bloqueioRepository;
    @Autowired
    private DisponibilidadeQuadraRepository disponibilidadeRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private QuadraRepository quadraRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Quadra quadra;
    private Usuario aluno;
    private Usuario outroAluno;
    private Reserva reserva;

    @BeforeEach
    void prepararBancoIsolado() {
        limparBanco();
        quadra = novaQuadra();
        aluno = novoUsuario("aluno.participante@teste.local");
        outroAluno = novoUsuario("outro.aluno.participante@teste.local");
        reserva = novaReserva(aluno, StatusReserva.PENDENTE_DOCUMENTACAO);
    }

    @AfterEach
    void limparRegistrosCriados() {
        limparBanco();
    }

    @Test
    void postgresCriaTabelaRelacionamentoObrigatorioEIndice() {
        List<String> colunas = jdbcTemplate.queryForList("""
                select column_name
                from information_schema.columns
                where table_schema = current_schema()
                  and table_name = 'participantes_reserva'
                """, String.class);
        List<String> indices = jdbcTemplate.queryForList("""
                select indexname
                from pg_indexes
                where schemaname = current_schema()
                  and tablename = 'participantes_reserva'
                """, String.class);
        Boolean reservaObrigatoria = jdbcTemplate.queryForObject("""
                select is_nullable = 'NO'
                from information_schema.columns
                where table_schema = current_schema()
                  and table_name = 'participantes_reserva'
                  and column_name = 'reserva_id'
                """, Boolean.class);

        assertTrue(colunas.containsAll(List.of(
                "id", "reserva_id", "nome_completo", "rm_ou_rg", "categoria")));
        assertTrue(indices.contains("idx_participantes_reserva"));
        assertTrue(Boolean.TRUE.equals(reservaObrigatoria));
    }

    @Test
    void persisteListaEditaERemoveParticipanteDaPropriaSolicitacao() {
        ParticipanteReserva criado = participanteService.adicionar(
                reserva.getId(),
                aluno,
                "  Beatriz   Almeida  ",
                "  123.456-X  ",
                CategoriaParticipante.OUTRO, VinculoParticipante.EX_ALUNO);

        ParticipanteReserva recuperado = participanteRepository
                .findById(criado.getId())
                .orElseThrow();
        assertEquals("Beatriz Almeida", recuperado.getNomeCompleto());
        assertEquals("123.456-X", recuperado.getRmOuRg());
        assertEquals(reserva.getId(), recuperado.getReserva().getId());
        assertEquals(List.of(criado.getId()), participanteService.listar(reserva.getId(), aluno)
                .stream().map(ParticipanteReserva::getId).toList());

        participanteService.editar(
                reserva.getId(),
                criado.getId(),
                aluno,
                "Beatriz Souza",
                "RM-9988",
                CategoriaParticipante.ALUNO, null);
        ParticipanteReserva editado = participanteRepository.findById(criado.getId()).orElseThrow();
        assertEquals("Beatriz Souza", editado.getNomeCompleto());
        assertEquals("RM-9988", editado.getRmOuRg());
        assertEquals(CategoriaParticipante.ALUNO, editado.getCategoria());

        participanteService.remover(reserva.getId(), criado.getId(), aluno);
        assertFalse(participanteRepository.existsById(criado.getId()));
    }

    @Test
    void outroAlunoNaoAcessaNemManipulaParticipantesDaSolicitacao() {
        ParticipanteReserva participante = participanteService.adicionar(
                reserva.getId(),
                aluno,
                "Ana Souza",
                "RM-123",
                CategoriaParticipante.ALUNO, null);

        assertThrows(AccessDeniedException.class,
                () -> participanteService.listar(reserva.getId(), outroAluno));
        assertThrows(AccessDeniedException.class,
                () -> participanteService.obter(
                        reserva.getId(), participante.getId(), outroAluno));
        assertThrows(AccessDeniedException.class,
                () -> participanteService.editar(
                        reserva.getId(),
                        participante.getId(),
                        outroAluno,
                        "Nome adulterado",
                        "RG-999",
                        CategoriaParticipante.OUTRO, VinculoParticipante.EX_ALUNO));
        assertThrows(AccessDeniedException.class,
                () -> participanteService.remover(
                        reserva.getId(), participante.getId(), outroAluno));

        ParticipanteReserva preservado = participanteRepository
                .findById(participante.getId())
                .orElseThrow();
        assertEquals("Ana Souza", preservado.getNomeCompleto());
    }

    @Test
    void idDeParticipanteDeOutraSolicitacaoNaoPodeSerManipulado() {
        Reserva outraReservaDoMesmoAluno = novaReserva(
                aluno, StatusReserva.PENDENTE_DOCUMENTACAO);
        ParticipanteReserva participanteDaOutraReserva = participanteService.adicionar(
                outraReservaDoMesmoAluno.getId(),
                aluno,
                "Carlos Pereira",
                "RG-777",
                CategoriaParticipante.OUTRO, VinculoParticipante.EX_ALUNO);

        assertThrows(AccessDeniedException.class, () -> participanteService.editar(
                reserva.getId(),
                participanteDaOutraReserva.getId(),
                aluno,
                "Nome adulterado",
                "RG-000",
                CategoriaParticipante.ALUNO, null));
        assertThrows(AccessDeniedException.class, () -> participanteService.remover(
                reserva.getId(), participanteDaOutraReserva.getId(), aluno));

        assertTrue(participanteRepository.existsById(participanteDaOutraReserva.getId()));
    }

    @Test
    void statusForaDaDocumentacaoPendenteImpedeTodasAsMutacoes() {
        ParticipanteReserva participante = participanteService.adicionar(
                reserva.getId(),
                aluno,
                "Ana Souza",
                "RM-123",
                CategoriaParticipante.ALUNO, null);
        reserva.setStatus(StatusReserva.PENDENTE_ANALISE);
        reservaRepository.saveAndFlush(reserva);

        assertThrows(IllegalStateException.class, () -> participanteService.adicionar(
                reserva.getId(),
                aluno,
                "Novo participante",
                "RG-789",
                CategoriaParticipante.OUTRO, VinculoParticipante.EX_ALUNO));
        assertThrows(IllegalStateException.class, () -> participanteService.editar(
                reserva.getId(),
                participante.getId(),
                aluno,
                "Nome adulterado",
                "RG-000",
                CategoriaParticipante.OUTRO, VinculoParticipante.EX_ALUNO));
        assertThrows(IllegalStateException.class, () -> participanteService.remover(
                reserva.getId(), participante.getId(), aluno));

        assertEquals(1L, participanteRepository.count());
        assertEquals("Ana Souza", participanteRepository.findById(participante.getId())
                .orElseThrow().getNomeCompleto());
    }

    @Test
    void resumoContaSeparadamenteEAtendeMinimoComDezParticipantesDeQualquerCategoria() {
        for (int indice = 1; indice <= 9; indice++) {
            adicionarParticipante(indice, CategoriaParticipante.ALUNO);
        }
        adicionarParticipante(20, CategoriaParticipante.OUTRO);
        adicionarParticipante(21, CategoriaParticipante.OUTRO);

        ResumoParticipantes abaixoDoMinimo = participanteService.resumir(
                reserva.getId(), aluno);
        assertEquals(9, abaixoDoMinimo.quantidadeAlunos());
        assertEquals(2, abaixoDoMinimo.quantidadeOutros());
        assertEquals(11, abaixoDoMinimo.total());
        assertTrue(abaixoDoMinimo.minimoDeParticipantesAtendido());

        adicionarParticipante(10, CategoriaParticipante.ALUNO);
        ResumoParticipantes minimoAtendido = participanteService.resumir(
                reserva.getId(), aluno);
        assertEquals(10, minimoAtendido.quantidadeAlunos());
        assertEquals(2, minimoAtendido.quantidadeOutros());
        assertEquals(12, minimoAtendido.total());
        assertTrue(minimoAtendido.minimoDeParticipantesAtendido());
    }

    private void adicionarParticipante(int indice, CategoriaParticipante categoria) {
        participanteService.adicionar(
                reserva.getId(),
                aluno,
                "Participante " + indice,
                "ID-" + indice,
                categoria,
                categoria == CategoriaParticipante.OUTRO ? VinculoParticipante.PAI_OU_MAE : null);
    }

    private void limparBanco() {
        // A guarda fica aqui, e nao so no @BeforeEach: o JUnit executa o @AfterEach
        // mesmo quando o @BeforeEach falha, e este metodo tambem e chamado por ele.
        BancoDeTeste.exigirBancoDedicado(dataSource);
        ajusteContribuicaoRepository.deleteAllInBatch();
        contribuicaoRepository.deleteAllInBatch();
        conferenciaDocumentoRepository.deleteAllInBatch();
        documentoReservaRepository.deleteAllInBatch();
        participanteRepository.deleteAllInBatch();
        auditoriaRepository.deleteAllInBatch();
        reservaRepository.deleteAllInBatch();
        bloqueioRepository.deleteAllInBatch();
        disponibilidadeRepository.deleteAllInBatch();
        usuarioRepository.deleteAllInBatch();
        quadraRepository.deleteAllInBatch();
    }

    private Quadra novaQuadra() {
        Quadra nova = new Quadra();
        nova.setNome("Quadra de participantes");
        nova.setDescricao("Registro criado somente no PostgreSQL temporário de testes");
        nova.setAtiva(true);
        return quadraRepository.saveAndFlush(nova);
    }

    private Usuario novoUsuario(String email) {
        Usuario usuario = new Usuario();
        usuario.setNome("Aluno de teste");
        usuario.setEmail(email);
        usuario.setSenha("hash-de-teste");
        usuario.setPerfil(Perfil.ALUNO);
        usuario.setAtivo(true);
        return usuarioRepository.saveAndFlush(usuario);
    }

    private Reserva novaReserva(Usuario solicitante, StatusReserva status) {
        Reserva nova = new Reserva();
        nova.setQuadra(quadra);
        nova.setSolicitante(solicitante);
        nova.setDataUtilizacao(LocalDate.now().plusDays(7));
        nova.setHorarioInicio(LocalTime.of(8, 0));
        nova.setHorarioFim(LocalTime.of(9, 0));
        nova.setFinalidade("Atividade esportiva de integração");
        nova.setStatus(status);
        return reservaRepository.saveAndFlush(nova);
    }
}
