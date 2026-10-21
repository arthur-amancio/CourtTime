package br.com.courttime.service;

import br.com.courttime.entity.CategoriaParticipante;
import br.com.courttime.entity.ParticipanteReserva;
import br.com.courttime.entity.Perfil;
import br.com.courttime.entity.PresencaParticipante;
import br.com.courttime.entity.Quadra;
import br.com.courttime.entity.Reserva;
import br.com.courttime.entity.StatusReserva;
import br.com.courttime.entity.Usuario;
import br.com.courttime.repository.AjusteContribuicaoApmRepository;
import br.com.courttime.repository.AuditoriaUsuarioRepository;
import br.com.courttime.repository.BloqueioQuadraRepository;
import br.com.courttime.repository.ConferenciaDocumentoRepository;
import br.com.courttime.repository.ContribuicaoApmRepository;
import br.com.courttime.repository.DisponibilidadeQuadraRepository;
import br.com.courttime.repository.DocumentoReservaRepository;
import br.com.courttime.repository.ParticipanteReservaRepository;
import br.com.courttime.repository.QuadraRepository;
import br.com.courttime.repository.ReservaRepository;
import br.com.courttime.repository.UsuarioRepository;
import br.com.courttime.support.BancoDeTeste;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** R08: presença marcada pela portaria, gravada no PostgreSQL real. */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "COURTTIME_POSTGRES_TEST", matches = "true")
class PresencaPostgreSqlIntegrationTest {

    @Autowired
    private ConsultaSegurancaService consultaSegurancaService;
    @Autowired
    private DataSource dataSource;
    @Autowired
    private JdbcTemplate jdbcTemplate;
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

    private Usuario portaria;
    private Usuario outraPortaria;
    private Reserva reserva;
    private ParticipanteReserva ana;
    private ParticipanteReserva bruno;

    @BeforeEach
    void prepararReservaAprovadaDeHoje() {
        limparBanco();
        Quadra quadra = new Quadra();
        quadra.setNome("Quadra da portaria");
        quadra.setDescricao("Registro criado somente no PostgreSQL temporário de testes");
        quadra.setAtiva(true);
        quadra = quadraRepository.saveAndFlush(quadra);
        Usuario aluno = novoUsuario("aluno.presenca@teste.local", Perfil.ALUNO);
        portaria = novoUsuario("portaria.um@teste.local", Perfil.SEGURANCA);
        outraPortaria = novoUsuario("portaria.dois@teste.local", Perfil.SEGURANCA);

        // Gravada direto no banco: a aprovação tem testes próprios.
        Reserva nova = new Reserva();
        nova.setQuadra(quadra);
        nova.setSolicitante(aluno);
        nova.setDataUtilizacao(LocalDate.now(CalendarioDisponibilidadeService.FUSO_HORARIO));
        nova.setHorarioInicio(LocalTime.of(21, 0));
        nova.setHorarioFim(LocalTime.of(22, 0));
        nova.setFinalidade("Atividade de hoje para a portaria");
        nova.setStatus(StatusReserva.APROVADA);
        reserva = reservaRepository.saveAndFlush(nova);
        ana = novoParticipante("Ana Exemplo", "RM-PRES-1");
        bruno = novoParticipante("Bruno Exemplo", "RM-PRES-2");
    }

    @AfterEach
    void limparRegistros() {
        limparBanco();
    }

    @Test
    void colunasDePresencaAceitamNuloParaOsRegistrosAntigos() {
        List<Map<String, Object>> colunas = jdbcTemplate.queryForList("""
                select column_name, is_nullable
                from information_schema.columns
                where table_schema = current_schema()
                  and table_name = 'participantes_reserva'
                  and column_name in ('presenca', 'presenca_registrada_por_id', 'presenca_registrada_em')
                """);

        assertEquals(3, colunas.size());
        assertTrue(colunas.stream().allMatch(coluna -> "YES".equals(coluna.get("is_nullable"))));
    }

    @Test
    void marcacaoFicaGravadaComQuemMarcouEAparecaParaAAdministracao() {
        consultaSegurancaService.registrarPresencas(reserva.getId(), portaria, Map.of(
                ana.getId(), PresencaParticipante.PRESENTE,
                bruno.getId(), PresencaParticipante.AUSENTE));

        List<ParticipanteReserva> lista = participanteRepository.listarComPresenca(reserva);
        assertEquals(PresencaParticipante.PRESENTE, lista.get(0).getPresenca());
        assertEquals(PresencaParticipante.AUSENTE, lista.get(1).getPresenca());
        // A consulta da administração já traz quem marcou, fora da transação.
        assertEquals(portaria.getNome(), lista.get(0).getPresencaRegistradaPor().getNome());
        assertNotNull(lista.get(0).getPresencaRegistradaEm());
        assertEquals("AUSENTE", jdbcTemplate.queryForObject(
                "select presenca from participantes_reserva where id = ?", String.class, bruno.getId()));
    }

    @Test
    void duasPortariasAoMesmoTempoNaoPerdemMarcacoes() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch prontas = new CountDownLatch(2);
        CountDownLatch iniciar = new CountDownLatch(1);
        try {
            Future<Integer> primeira = executor.submit(() -> marcarAposSinal(
                    portaria, ana.getId(), PresencaParticipante.PRESENTE, prontas, iniciar));
            Future<Integer> segunda = executor.submit(() -> marcarAposSinal(
                    outraPortaria, bruno.getId(), PresencaParticipante.PRESENTE, prontas, iniciar));
            prontas.await();
            iniciar.countDown();
            primeira.get();
            segunda.get();
        } finally {
            executor.shutdownNow();
        }

        assertEquals(PresencaParticipante.PRESENTE,
                participanteRepository.findById(ana.getId()).orElseThrow().getPresenca());
        assertEquals(PresencaParticipante.PRESENTE,
                participanteRepository.findById(bruno.getId()).orElseThrow().getPresenca());
    }

    @Test
    void reservaCanceladaNaoRecebeMarcacao() {
        jdbcTemplate.update("update reservas set status = 'CANCELADA' where id = ?", reserva.getId());

        assertThrows(IllegalArgumentException.class, () -> consultaSegurancaService.registrarPresencas(
                reserva.getId(), portaria, Map.of(ana.getId(), PresencaParticipante.PRESENTE)));
        assertEquals(null, jdbcTemplate.queryForObject(
                "select presenca from participantes_reserva where id = ?", String.class, ana.getId()));
    }

    private int marcarAposSinal(
            Usuario seguranca, Long participanteId, PresencaParticipante presenca,
            CountDownLatch prontas, CountDownLatch iniciar) throws Exception {
        prontas.countDown();
        iniciar.await();
        return consultaSegurancaService.registrarPresencas(
                reserva.getId(), seguranca, Map.of(participanteId, presenca));
    }

    private ParticipanteReserva novoParticipante(String nome, String rm) {
        ParticipanteReserva participante = new ParticipanteReserva();
        participante.setReserva(reserva);
        participante.setNomeCompleto(nome);
        participante.setRmOuRg(rm);
        participante.setCategoria(CategoriaParticipante.ALUNO);
        return participanteRepository.saveAndFlush(participante);
    }

    private Usuario novoUsuario(String email, Perfil perfil) {
        Usuario usuario = new Usuario();
        usuario.setNome("Usuário " + email);
        usuario.setEmail(email);
        usuario.setSenha("hash-de-teste");
        usuario.setPerfil(perfil);
        usuario.setAtivo(true);
        return usuarioRepository.saveAndFlush(usuario);
    }

    private void limparBanco() {
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
}
