package br.com.courttime.service;

import br.com.courttime.entity.AjusteContribuicaoApm;
import br.com.courttime.entity.CategoriaParticipante;
import br.com.courttime.entity.ConferenciaDocumento;
import br.com.courttime.entity.ContribuicaoApm;
import br.com.courttime.entity.DocumentoReserva;
import br.com.courttime.entity.Perfil;
import br.com.courttime.entity.ParticipanteReserva;
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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import br.com.courttime.support.BancoDeTeste;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import javax.sql.DataSource;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "COURTTIME_POSTGRES_TEST", matches = "true")
class ContribuicaoApmPostgreSqlIntegrationTest {

    private static final Path DIRETORIO = Path.of(
            System.getProperty("java.io.tmpdir"),
            "courttime-contribuicao-" + UUID.randomUUID()).toAbsolutePath();

    @DynamicPropertySource
    static void configurarStorage(DynamicPropertyRegistry registry) {
        registry.add("courttime.documentos.diretorio", DIRETORIO::toString);
    }

    @Autowired
    private ContribuicaoApmService contribuicaoService;

    @Autowired
    private DataSource dataSource;
    @Autowired
    private ConferenciaDocumentoService conferenciaService;
    @Autowired
    private DocumentoReservaService documentoService;
    @Autowired
    private AjusteContribuicaoApmRepository ajusteRepository;
    @Autowired
    private ContribuicaoApmRepository contribuicaoRepository;
    @Autowired
    private ConferenciaDocumentoRepository conferenciaRepository;
    @Autowired
    private DocumentoReservaRepository documentoRepository;
    @Autowired
    private ParticipanteReservaRepository participanteRepository;
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
    private Usuario secretaria;
    private Usuario outraSecretaria;
    private Reserva reserva;

    @BeforeEach
    void prepararBancoEStorage() throws IOException {
        BancoDeTeste.exigirBancoDedicado(dataSource);
        limparBanco();
        limparArquivos();
        quadra = novaQuadra();
        aluno = novoUsuario("aluno.contribuicao@teste.local", Perfil.ALUNO);
        secretaria = novoUsuario("secretaria.contribuicao.um@teste.local", Perfil.SECRETARIA);
        outraSecretaria = novoUsuario(
                "secretaria.contribuicao.dois@teste.local", Perfil.SECRETARIA);
        reserva = novaReserva();
        cadastrarDezAlunos();
    }

    @AfterEach
    void limparRegistros() throws IOException {
        limparBanco();
        limparArquivos();
    }

    @AfterAll
    static void removerDiretorio() throws IOException {
        if (!Files.exists(DIRETORIO)) {
            return;
        }
        try (var caminhos = Files.walk(DIRETORIO)) {
            for (Path caminho : caminhos.sorted((a, b) -> b.compareTo(a)).toList()) {
                Files.deleteIfExists(caminho);
            }
        }
    }

    @Test
    void postgresCriaValorMonetarioEUnicidadePorReservaEConferencia() {
        List<String> colunas = jdbcTemplate.queryForList("""
                select column_name
                from information_schema.columns
                where table_schema = current_schema()
                  and table_name = 'contribuicoes_apm'
                """, String.class);
        Integer precisao = jdbcTemplate.queryForObject("""
                select numeric_precision
                from information_schema.columns
                where table_schema = current_schema()
                  and table_name = 'contribuicoes_apm'
                  and column_name = 'valor'
                """, Integer.class);
        Integer escala = jdbcTemplate.queryForObject("""
                select numeric_scale
                from information_schema.columns
                where table_schema = current_schema()
                  and table_name = 'contribuicoes_apm'
                  and column_name = 'valor'
                """, Integer.class);
        Long restricoesUnicas = jdbcTemplate.queryForObject("""
                select count(*)
                from information_schema.table_constraints
                where table_schema = current_schema()
                  and table_name = 'contribuicoes_apm'
                  and constraint_type = 'UNIQUE'
                """, Long.class);

        assertTrue(colunas.containsAll(List.of(
                "reserva_id", "conferencia_documento_id", "valor", "observacoes",
                "registrada_por_id", "registrada_em")));
        assertEquals(12, precisao);
        assertEquals(2, escala);
        assertTrue(restricoesUnicas >= 2L);
    }

    @Test
    void registraSomenteAposValidacaoEVinculaConferenciaExataAvancandoStatus() {
        DocumentoReserva documento = enviar("termo.pdf", "documento conferido");
        ConferenciaDocumento conferencia = conferenciaService.validar(
                documento.getId(), secretaria);

        assertEquals(List.of(reserva.getId()), contribuicaoService.listarPendentes(secretaria)
                .stream().map(Reserva::getId).toList());

        DetalheContribuicaoApm detalhe = contribuicaoService.registrar(
                reserva.getId(), secretaria, "24,50", "Recebimento presencial.");

        ContribuicaoApm contribuicao = detalhe.contribuicao();
        assertEquals(new BigDecimal("24.50"), contribuicao.getValor());
        assertEquals(conferencia.getId(), contribuicao.getConferenciaDocumento().getId());
        assertEquals(secretaria.getId(), contribuicao.getRegistradaPor().getId());
        assertNotNull(contribuicao.getRegistradaEm());
        assertEquals(StatusReserva.PENDENTE_ANALISE,
                reservaRepository.findById(reserva.getId()).orElseThrow().getStatus());
        assertTrue(contribuicaoService.listarPendentes(secretaria).isEmpty());
        assertEquals(1, contribuicaoService.listarRegistradas(secretaria).size());
    }

    @Test
    void versaoCorrigidaExigeNovaConferenciaAntesDoRegistro() {
        DocumentoReserva versaoUm = enviar("v1.pdf", "versão devolvida");
        conferenciaService.devolver(
                versaoUm.getId(), secretaria, "Documento precisa de correção.");
        enviar("v2.pdf", "versão ainda não conferida");

        assertTrue(contribuicaoService.listarPendentes(secretaria).isEmpty());
        assertThrows(IllegalStateException.class, () -> contribuicaoService.registrar(
                reserva.getId(), secretaria, "20.00", null));
        assertEquals(0, contribuicaoRepository.count());
    }

    @Test
    void ajustePreservaHistoricoAntesEDepois() {
        DocumentoReserva documento = enviar("termo.pdf", "documento conferido");
        conferenciaService.validar(documento.getId(), secretaria);
        ContribuicaoApm contribuicao = contribuicaoService.registrar(
                reserva.getId(), secretaria, "20.00", "Valor inicial").contribuicao();

        DetalheContribuicaoApm detalhe = contribuicaoService.ajustar(
                contribuicao.getId(),
                outraSecretaria,
                "22.00",
                "Valor corrigido",
                "Conferência do comprovante físico.");

        assertEquals(new BigDecimal("22.00"), detalhe.contribuicao().getValor());
        assertEquals("Valor corrigido", detalhe.contribuicao().getObservacoes());
        assertEquals(1, detalhe.ajustes().size());
        AjusteContribuicaoApm ajuste = detalhe.ajustes().get(0);
        assertEquals(new BigDecimal("20.00"), ajuste.getValorAnterior());
        assertEquals(new BigDecimal("22.00"), ajuste.getValorNovo());
        assertEquals("Valor inicial", ajuste.getObservacoesAnteriores());
        assertEquals("Valor corrigido", ajuste.getObservacoesNovas());
        assertEquals(outraSecretaria.getId(), ajuste.getAjustadaPor().getId());
        assertNotNull(ajuste.getAjustadaEm());
    }

    @Test
    void duasSecretariasConcorrentesNaoDuplicamRecebimento() throws Exception {
        DocumentoReserva documento = enviar("termo.pdf", "concorrência financeira");
        conferenciaService.validar(documento.getId(), secretaria);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch preparadas = new CountDownLatch(2);
        CountDownLatch iniciar = new CountDownLatch(1);
        try {
            Future<DetalheContribuicaoApm> primeira = executor.submit(() -> registrarAposSinal(
                    secretaria, "20.00", preparadas, iniciar));
            Future<DetalheContribuicaoApm> segunda = executor.submit(() -> registrarAposSinal(
                    outraSecretaria, "22.00", preparadas, iniciar));
            preparadas.await();
            iniciar.countDown();

            int sucessos = 0;
            int recusas = 0;
            for (Future<DetalheContribuicaoApm> futuro : List.of(primeira, segunda)) {
                try {
                    futuro.get();
                    sucessos++;
                } catch (ExecutionException exception) {
                    assertInstanceOf(IllegalStateException.class, exception.getCause());
                    recusas++;
                }
            }

            assertEquals(1, sucessos);
            assertEquals(1, recusas);
            assertEquals(1, contribuicaoRepository.count());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void doisAjustesConcorrentesFormamHistoricoSerialSemPerderAlteracao() throws Exception {
        DocumentoReserva documento = enviar("termo.pdf", "ajustes concorrentes");
        conferenciaService.validar(documento.getId(), secretaria);
        ContribuicaoApm contribuicao = contribuicaoService.registrar(
                reserva.getId(), secretaria, "20.00", "Valor inicial").contribuicao();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch preparadas = new CountDownLatch(2);
        CountDownLatch iniciar = new CountDownLatch(1);
        try {
            Future<DetalheContribuicaoApm> primeiro = executor.submit(() ->
                    ajustarAposSinal(
                            contribuicao.getId(), secretaria, "21.00",
                            preparadas, iniciar));
            Future<DetalheContribuicaoApm> segundo = executor.submit(() ->
                    ajustarAposSinal(
                            contribuicao.getId(), outraSecretaria, "22.00",
                            preparadas, iniciar));
            preparadas.await();
            iniciar.countDown();
            primeiro.get();
            segundo.get();

            DetalheContribuicaoApm detalhe = contribuicaoService.obterPorReserva(
                    reserva.getId(), secretaria);
            assertEquals(2, detalhe.ajustes().size());
            AjusteContribuicaoApm maisRecente = detalhe.ajustes().get(0);
            AjusteContribuicaoApm anterior = detalhe.ajustes().get(1);
            assertEquals(new BigDecimal("20.00"), anterior.getValorAnterior());
            assertEquals(anterior.getValorNovo(), maisRecente.getValorAnterior());
            assertEquals(maisRecente.getValorNovo(), detalhe.contribuicao().getValor());
        } finally {
            executor.shutdownNow();
        }
    }

    private DetalheContribuicaoApm registrarAposSinal(
            Usuario operadora,
            String valor,
            CountDownLatch preparadas,
            CountDownLatch iniciar) throws Exception {
        preparadas.countDown();
        iniciar.await();
        return contribuicaoService.registrar(
                reserva.getId(), operadora, valor, "Recebimento concorrente");
    }

    private DetalheContribuicaoApm ajustarAposSinal(
            Long contribuicaoId,
            Usuario operadora,
            String valor,
            CountDownLatch preparadas,
            CountDownLatch iniciar) throws Exception {
        preparadas.countDown();
        iniciar.await();
        return contribuicaoService.ajustar(
                contribuicaoId,
                operadora,
                valor,
                "Valor ajustado",
                "Conferência concorrente do registro físico");
    }

    private DocumentoReserva enviar(String nome, String conteudo) {
        return documentoService.enviar(
                reserva.getId(),
                aluno,
                nome,
                "application/pdf",
                ("%PDF-1.7\n" + conteudo).getBytes(StandardCharsets.UTF_8));
    }

    private void cadastrarDezAlunos() {
        for (int indice = 1; indice <= 10; indice++) {
            ParticipanteReserva participante = new ParticipanteReserva();
            participante.setReserva(reserva);
            participante.setNomeCompleto("Aluno " + indice);
            participante.setRmOuRg("RM-APM-" + indice);
            participante.setCategoria(CategoriaParticipante.ALUNO);
            participanteRepository.save(participante);
        }
        participanteRepository.flush();
    }

    private void limparArquivos() throws IOException {
        Files.createDirectories(DIRETORIO);
        try (var arquivos = Files.list(DIRETORIO)) {
            for (Path arquivo : arquivos.toList()) {
                Files.deleteIfExists(arquivo);
            }
        }
    }

    private void limparBanco() {
        ajusteRepository.deleteAllInBatch();
        contribuicaoRepository.deleteAllInBatch();
        conferenciaRepository.deleteAllInBatch();
        documentoRepository.deleteAllInBatch();
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
        nova.setNome("Quadra da contribuição");
        nova.setDescricao("Registro criado somente no PostgreSQL temporário de testes");
        nova.setAtiva(true);
        return quadraRepository.saveAndFlush(nova);
    }

    private Usuario novoUsuario(String email, Perfil perfil) {
        Usuario usuario = new Usuario();
        usuario.setNome("Usuário de teste");
        usuario.setEmail(email);
        usuario.setSenha("hash-de-teste");
        usuario.setPerfil(perfil);
        usuario.setAtivo(true);
        return usuarioRepository.saveAndFlush(usuario);
    }

    private Reserva novaReserva() {
        Reserva nova = new Reserva();
        nova.setQuadra(quadra);
        nova.setSolicitante(aluno);
        nova.setDataUtilizacao(LocalDate.now().plusDays(7));
        nova.setHorarioInicio(LocalTime.of(8, 0));
        nova.setHorarioFim(LocalTime.of(9, 0));
        nova.setFinalidade("Atividade esportiva com contribuição presencial");
        nova.setStatus(StatusReserva.PENDENTE_DOCUMENTACAO);
        return reservaRepository.saveAndFlush(nova);
    }
}
