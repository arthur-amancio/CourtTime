package br.com.courttime.service;

import br.com.courttime.entity.CategoriaParticipante;
import br.com.courttime.entity.ConferenciaDocumento;
import br.com.courttime.entity.DocumentoReserva;
import br.com.courttime.entity.Perfil;
import br.com.courttime.entity.ParticipanteReserva;
import br.com.courttime.entity.Quadra;
import br.com.courttime.entity.Reserva;
import br.com.courttime.entity.ResultadoConferenciaDocumento;
import br.com.courttime.entity.StatusReserva;
import br.com.courttime.entity.Usuario;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "COURTTIME_POSTGRES_TEST", matches = "true")
class ConferenciaDocumentoPostgreSqlIntegrationTest {

    private static final Path DIRETORIO = Path.of(
            System.getProperty("java.io.tmpdir"),
            "courttime-conferencia-" + UUID.randomUUID()).toAbsolutePath();

    @DynamicPropertySource
    static void configurarStorage(DynamicPropertyRegistry registry) {
        registry.add("courttime.documentos.diretorio", DIRETORIO::toString);
    }

    @Autowired
    private ConferenciaDocumentoService conferenciaService;

    @Autowired
    private DataSource dataSource;
    @Autowired
    private DocumentoReservaService documentoService;
    @Autowired
    private ConferenciaDocumentoRepository conferenciaRepository;
    @Autowired
    private AjusteContribuicaoApmRepository ajusteContribuicaoRepository;
    @Autowired
    private ContribuicaoApmRepository contribuicaoRepository;
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
        limparBanco();
        limparArquivos();
        quadra = novaQuadra();
        aluno = novoUsuario("aluno.conferencia@teste.local", Perfil.ALUNO);
        secretaria = novoUsuario("secretaria.um@teste.local", Perfil.SECRETARIA);
        outraSecretaria = novoUsuario("secretaria.dois@teste.local", Perfil.SECRETARIA);
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
    void postgresCriaRegistroAppendOnlyComDocumentoUnico() {
        List<String> colunas = jdbcTemplate.queryForList("""
                select column_name
                from information_schema.columns
                where table_schema = current_schema()
                  and table_name = 'conferencias_documento'
                """, String.class);
        Long documentoUnico = jdbcTemplate.queryForObject("""
                select count(*)
                from information_schema.table_constraints
                where table_schema = current_schema()
                  and table_name = 'conferencias_documento'
                  and constraint_type = 'UNIQUE'
                """, Long.class);

        assertTrue(colunas.containsAll(List.of(
                "id", "documento_id", "resultado", "justificativa",
                "conferido_por_id", "conferido_em")));
        assertTrue(documentoUnico >= 1L);
    }

    @Test
    void filaMostraSomenteVersaoMaisRecenteNaoConferida() {
        DocumentoReserva versaoUm = enviar("v1.pdf", "versão um");
        conferenciaService.devolver(
                versaoUm.getId(), secretaria, "Envio de nova versão no teste.");
        DocumentoReserva versaoDois = enviar("v2.pdf", "versão dois");

        List<DocumentoReserva> fila = conferenciaService.listarPendentes(secretaria);

        assertEquals(List.of(versaoDois.getId()),
                fila.stream().map(DocumentoReserva::getId).toList());
        assertNull(versaoUm.getConferencia());
    }

    @Test
    void validacaoRegistraResponsavelSemAprovarNemAlterarPdf() throws Exception {
        DocumentoReserva documento = enviar("termo.pdf", "conteúdo original");
        byte[] antes = documentoService.baixar(reserva.getId(), documento.getId(), aluno)
                .conteudo().getInputStream().readAllBytes();

        ConferenciaDocumento conferencia = conferenciaService.validar(
                documento.getId(), secretaria, true);

        assertEquals(ResultadoConferenciaDocumento.VALIDADO, conferencia.getResultado());
        assertEquals(secretaria.getId(), conferencia.getConferidoPor().getId());
        assertEquals(StatusReserva.PENDENTE_DOCUMENTACAO,
                reservaRepository.findById(reserva.getId()).orElseThrow().getStatus());
        assertEquals(0, conferenciaService.listarPendentes(secretaria).size());
        assertEquals(1, conferenciaService.listarHistorico(secretaria).size());
        byte[] depois = documentoService.baixar(reserva.getId(), documento.getId(), aluno)
                .conteudo().getInputStream().readAllBytes();
        assertEquals(new String(antes, StandardCharsets.UTF_8),
                new String(depois, StandardCharsets.UTF_8));
    }

    @Test
    void devolucaoPreservaHistoricoENovaVersaoVoltaParaFila() {
        DocumentoReserva versaoUm = enviar("v1.pdf", "incompleto");
        conferenciaService.devolver(
                versaoUm.getId(), secretaria, "Falta a página final.");
        DocumentoReserva versaoDois = enviar("v2.pdf", "corrigido");

        List<DocumentoReserva> fila = conferenciaService.listarPendentes(secretaria);
        List<ConferenciaDocumento> historico = conferenciaService.listarHistorico(secretaria);

        assertEquals(List.of(versaoDois.getId()),
                fila.stream().map(DocumentoReserva::getId).toList());
        assertEquals(1, historico.size());
        assertEquals(ResultadoConferenciaDocumento.DEVOLVIDO_CORRECAO,
                historico.get(0).getResultado());
        assertEquals("Falta a página final.", historico.get(0).getJustificativa());
    }

    @Test
    void duasSecretariasConcorrentesNaoDuplicamDecisao() throws Exception {
        DocumentoReserva documento = enviar("termo.pdf", "concorrência");
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch preparados = new CountDownLatch(2);
        CountDownLatch iniciar = new CountDownLatch(1);
        try {
            Future<ConferenciaDocumento> primeira = executor.submit(() -> validarAposSinal(
                    documento.getId(), secretaria, preparados, iniciar));
            Future<ConferenciaDocumento> segunda = executor.submit(() -> validarAposSinal(
                    documento.getId(), outraSecretaria, preparados, iniciar));
            preparados.await();
            iniciar.countDown();

            int sucessos = 0;
            int recusas = 0;
            for (Future<ConferenciaDocumento> futuro : List.of(primeira, segunda)) {
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
            assertEquals(1, conferenciaRepository.count());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void versaoAntigaNaoPodeSerConferidaDepoisDeNovoUpload() {
        DocumentoReserva versaoUm = enviar("v1.pdf", "antigo");
        conferenciaService.devolver(
                versaoUm.getId(), secretaria, "Envio de nova versão no teste.");
        enviar("v2.pdf", "novo");

        assertThrows(IllegalStateException.class,
                () -> conferenciaService.validar(versaoUm.getId(), secretaria, true));
        assertEquals(1, conferenciaRepository.count());
    }

    private ConferenciaDocumento validarAposSinal(
            Long documentoId,
            Usuario operadora,
            CountDownLatch preparados,
            CountDownLatch iniciar) throws Exception {
        preparados.countDown();
        iniciar.await();
        return conferenciaService.validar(documentoId, operadora, true);
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
            participante.setRmOuRg("RM-CONF-" + indice);
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
        // A guarda fica aqui, e nao so no @BeforeEach: o JUnit executa o @AfterEach
        // mesmo quando o @BeforeEach falha, e este metodo tambem e chamado por ele.
        BancoDeTeste.exigirBancoDedicado(dataSource);
        ajusteContribuicaoRepository.deleteAllInBatch();
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
        nova.setNome("Quadra da conferência");
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
        nova.setFinalidade("Atividade esportiva com conferência documental");
        nova.setStatus(StatusReserva.PENDENTE_DOCUMENTACAO);
        return reservaRepository.saveAndFlush(nova);
    }
}
