package br.com.courttime.service;

import br.com.courttime.entity.ConferenciaDocumento;
import br.com.courttime.entity.DocumentoReserva;
import br.com.courttime.entity.Perfil;
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
import org.springframework.security.access.AccessDeniedException;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "COURTTIME_POSTGRES_TEST", matches = "true")
class DocumentoReservaPostgreSqlIntegrationTest {

    private static final Path DIRETORIO = Path.of(
            System.getProperty("java.io.tmpdir"),
            "courttime-documentos-" + UUID.randomUUID()).toAbsolutePath();

    @DynamicPropertySource
    static void configurarStorage(DynamicPropertyRegistry registry) {
        registry.add("courttime.documentos.diretorio", DIRETORIO::toString);
    }

    @Autowired
    private DocumentoReservaService documentoService;

    @Autowired
    private DataSource dataSource;
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
    private Usuario outroAluno;
    private Usuario secretaria;
    private Reserva reserva;

    @BeforeEach
    void prepararBancoEStorageIsolados() throws IOException {
        BancoDeTeste.exigirBancoDedicado(dataSource);
        limparBanco();
        limparArquivos();
        quadra = novaQuadra();
        aluno = novoUsuario("aluno.documento@teste.local");
        outroAluno = novoUsuario("outro.aluno.documento@teste.local");
        secretaria = novoUsuario("secretaria.documento@teste.local", Perfil.SECRETARIA);
        reserva = novaReserva(aluno, StatusReserva.PENDENTE_DOCUMENTACAO);
    }

    @AfterEach
    void limparRegistrosCriados() throws IOException {
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
    void postgresCriaMetadadosRestricoesEIndicesDeVersao() {
        List<String> colunas = jdbcTemplate.queryForList("""
                select column_name
                from information_schema.columns
                where table_schema = current_schema()
                  and table_name = 'documentos_reserva'
                """, String.class);
        List<String> indices = jdbcTemplate.queryForList("""
                select indexname
                from pg_indexes
                where schemaname = current_schema()
                  and tablename = 'documentos_reserva'
                """, String.class);
        Long restricaoVersao = jdbcTemplate.queryForObject("""
                select count(*)
                from information_schema.table_constraints
                where table_schema = current_schema()
                  and table_name = 'documentos_reserva'
                  and constraint_name = 'uk_documentos_reserva_versao'
                  and constraint_type = 'UNIQUE'
                """, Long.class);

        assertTrue(colunas.containsAll(List.of(
                "id", "reserva_id", "versao", "nome_original", "chave_armazenamento",
                "sha256", "tamanho_bytes", "enviado_por_id", "enviado_em",
                "revisao_participantes")));
        assertTrue(indices.contains("idx_documentos_reserva"));
        assertEquals(1L, restricaoVersao);
    }

    @Test
    void doisEnviosPreservamVersoesOriginaisBytesEHash() throws Exception {
        byte[] primeiraVersao = pdf("primeira versão");
        byte[] segundaVersao = pdf("segunda versão corrigida");

        DocumentoReserva primeira = documentoService.enviar(
                reserva.getId(), aluno, "termo original.pdf", "application/pdf", primeiraVersao);
        registrarDevolucao(primeira);
        DocumentoReserva segunda = documentoService.enviar(
                reserva.getId(), aluno, "termo corrigido.pdf", "application/pdf", segundaVersao);

        assertEquals(1, primeira.getVersao());
        assertEquals(2, segunda.getVersao());
        assertFalse(primeira.getChaveArmazenamento().equals(segunda.getChaveArmazenamento()));
        assertFalse(primeira.getSha256().equals(segunda.getSha256()));
        assertEquals(List.of(2, 1), documentoService.listar(reserva.getId(), aluno)
                .stream().map(DocumentoReserva::getVersao).toList());
        assertArrayEquals(
                primeiraVersao,
                documentoService.baixar(reserva.getId(), primeira.getId(), aluno)
                        .conteudo().getInputStream().readAllBytes());
        assertArrayEquals(
                segundaVersao,
                documentoService.baixar(reserva.getId(), segunda.getId(), aluno)
                        .conteudo().getInputStream().readAllBytes());
        assertEquals(2, quantidadeArquivos());
    }

    @Test
    void outroAlunoNaoListaBaixaOuEnviaNaSolicitacao() {
        DocumentoReserva documento = documentoService.enviar(
                reserva.getId(), aluno, "termo.pdf", "application/pdf", pdf("original"));

        assertThrows(AccessDeniedException.class,
                () -> documentoService.listar(reserva.getId(), outroAluno));
        assertThrows(AccessDeniedException.class,
                () -> documentoService.baixar(reserva.getId(), documento.getId(), outroAluno));
        assertThrows(AccessDeniedException.class,
                () -> documentoService.enviar(
                        reserva.getId(), outroAluno, "outro.pdf", "application/pdf", pdf("outro")));

        assertEquals(1, documentoRepository.count());
        assertEquals(1, quantidadeArquivos());
    }

    @Test
    void documentoDeOutraSolicitacaoDoMesmoAlunoNaoPodeSerBaixadoPorIdMisturado() {
        Reserva outraReserva = novaReserva(aluno, StatusReserva.PENDENTE_DOCUMENTACAO);
        DocumentoReserva documento = documentoService.enviar(
                outraReserva.getId(), aluno, "outro-termo.pdf", "application/pdf", pdf("outro"));

        assertThrows(AccessDeniedException.class,
                () -> documentoService.baixar(reserva.getId(), documento.getId(), aluno));
    }

    @Test
    void statusQueAvancouImpedeNovoArquivoSemApagarAnterior() {
        documentoService.enviar(
                reserva.getId(), aluno, "termo.pdf", "application/pdf", pdf("original"));
        reserva.setStatus(StatusReserva.PENDENTE_ANALISE);
        reservaRepository.saveAndFlush(reserva);

        assertThrows(IllegalStateException.class, () -> documentoService.enviar(
                reserva.getId(), aluno, "novo.pdf", "application/pdf", pdf("novo")));

        assertEquals(1, documentoRepository.count());
        assertEquals(1, quantidadeArquivos());
    }

    @Test
    void doisEnviosConcorrentesPermitemSomenteUmaVersaoAguardandoConferencia() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch preparados = new CountDownLatch(2);
        CountDownLatch iniciar = new CountDownLatch(1);
        try {
            Future<DocumentoReserva> primeiro = executor.submit(() -> enviarAposSinal(
                    preparados, iniciar, "concorrente-a.pdf", pdf("arquivo A")));
            Future<DocumentoReserva> segundo = executor.submit(() -> enviarAposSinal(
                    preparados, iniciar, "concorrente-b.pdf", pdf("arquivo B")));
            preparados.await();
            iniciar.countDown();

            int sucessos = 0;
            int recusas = 0;
            for (Future<DocumentoReserva> futuro : List.of(primeiro, segundo)) {
                try {
                    DocumentoReserva enviado = futuro.get();
                    assertEquals(1, enviado.getVersao());
                    sucessos++;
                } catch (ExecutionException exception) {
                    assertInstanceOf(IllegalStateException.class, exception.getCause());
                    recusas++;
                }
            }

            assertEquals(1, sucessos);
            assertEquals(1, recusas);
            assertEquals(1, documentoRepository.count());
            assertEquals(1, quantidadeArquivos());
        } finally {
            executor.shutdownNow();
        }
    }

    private DocumentoReserva enviarAposSinal(
            CountDownLatch preparados,
            CountDownLatch iniciar,
            String nome,
            byte[] conteudo) throws Exception {
        preparados.countDown();
        iniciar.await();
        return documentoService.enviar(
                reserva.getId(), aluno, nome, "application/pdf", conteudo);
    }

    private int quantidadeArquivos() {
        if (!Files.exists(DIRETORIO)) {
            return 0;
        }
        try (var arquivos = Files.list(DIRETORIO)) {
            return (int) arquivos.filter(Files::isRegularFile).count();
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private void registrarDevolucao(DocumentoReserva documento) {
        ConferenciaDocumento conferencia = new ConferenciaDocumento();
        conferencia.setDocumento(documento);
        conferencia.setResultado(ResultadoConferenciaDocumento.DEVOLVIDO_CORRECAO);
        conferencia.setJustificativa("Correção solicitada no teste");
        conferencia.setConferidoPor(secretaria);
        conferenciaRepository.saveAndFlush(conferencia);
    }

    private byte[] pdf(String conteudo) {
        return ("%PDF-1.7\n" + conteudo).getBytes(StandardCharsets.UTF_8);
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
        nova.setNome("Quadra de documentos");
        nova.setDescricao("Registro criado somente no PostgreSQL temporário de testes");
        nova.setAtiva(true);
        return quadraRepository.saveAndFlush(nova);
    }

    private Usuario novoUsuario(String email) {
        return novoUsuario(email, Perfil.ALUNO);
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

    private Reserva novaReserva(Usuario solicitante, StatusReserva status) {
        Reserva nova = new Reserva();
        nova.setQuadra(quadra);
        nova.setSolicitante(solicitante);
        nova.setDataUtilizacao(LocalDate.now().plusDays(7));
        nova.setHorarioInicio(LocalTime.of(8, 0));
        nova.setHorarioFim(LocalTime.of(9, 0));
        nova.setFinalidade("Atividade esportiva com termo externo");
        nova.setStatus(status);
        return reservaRepository.saveAndFlush(nova);
    }
}
