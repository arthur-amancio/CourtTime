package br.com.courttime.service;

import br.com.courttime.entity.BloqueioQuadra;
import br.com.courttime.entity.CategoriaParticipante;
import br.com.courttime.entity.ConferenciaDocumento;
import br.com.courttime.entity.ContribuicaoApm;
import br.com.courttime.entity.DiaSemana;
import br.com.courttime.entity.DisponibilidadeQuadra;
import br.com.courttime.entity.DocumentoReserva;
import br.com.courttime.entity.ParticipanteReserva;
import br.com.courttime.entity.Perfil;
import br.com.courttime.entity.Quadra;
import br.com.courttime.entity.Reserva;
import br.com.courttime.entity.ResultadoConferenciaDocumento;
import br.com.courttime.entity.StatusReserva;
import br.com.courttime.entity.Usuario;
import br.com.courttime.repository.BloqueioQuadraRepository;
import br.com.courttime.repository.AuditoriaUsuarioRepository;
import br.com.courttime.repository.AjusteContribuicaoApmRepository;
import br.com.courttime.repository.ConferenciaDocumentoRepository;
import br.com.courttime.repository.ContribuicaoApmRepository;
import br.com.courttime.repository.DisponibilidadeQuadraRepository;
import br.com.courttime.repository.DocumentoReservaRepository;
import br.com.courttime.repository.ParticipanteReservaRepository;
import br.com.courttime.repository.QuadraRepository;
import br.com.courttime.repository.ReservaRepository;
import br.com.courttime.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "COURTTIME_POSTGRES_TEST", matches = "true")
class ReservaPostgreSqlIntegrationTest {

    private static final LocalDate DATA = LocalDate.now(CalendarioDisponibilidadeService.FUSO_HORARIO)
            .plusDays(7);
    private static final LocalTime INICIO = LocalTime.of(8, 0);
    private static final LocalTime FIM = LocalTime.of(9, 0);

    @Autowired
    private ReservaService reservaService;

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private AuditoriaUsuarioRepository auditoriaUsuarioRepository;

    @Autowired
    private QuadraRepository quadraRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private DisponibilidadeQuadraRepository disponibilidadeQuadraRepository;

    @Autowired
    private BloqueioQuadraRepository bloqueioQuadraRepository;

    @Autowired
    private ParticipanteReservaRepository participanteReservaRepository;

    @Autowired
    private DocumentoReservaRepository documentoReservaRepository;

    @Autowired
    private ConferenciaDocumentoRepository conferenciaDocumentoRepository;

    @Autowired
    private ContribuicaoApmRepository contribuicaoApmRepository;

    @Autowired
    private AjusteContribuicaoApmRepository ajusteContribuicaoApmRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Quadra quadra;
    private Usuario aluno;
    private Usuario administrador;

    @BeforeEach
    void prepararBanco() {
        ajusteContribuicaoApmRepository.deleteAllInBatch();
        contribuicaoApmRepository.deleteAllInBatch();
        conferenciaDocumentoRepository.deleteAllInBatch();
        documentoReservaRepository.deleteAllInBatch();
        participanteReservaRepository.deleteAllInBatch();
        auditoriaUsuarioRepository.deleteAllInBatch();
        reservaRepository.deleteAllInBatch();
        bloqueioQuadraRepository.deleteAllInBatch();
        disponibilidadeQuadraRepository.deleteAllInBatch();
        usuarioRepository.deleteAllInBatch();
        quadraRepository.deleteAllInBatch();

        quadra = novaQuadra();
        aluno = novoUsuario("aluno@teste.local", Perfil.ALUNO);
        administrador = novoUsuario("administrador@teste.local", Perfil.ADMINISTRADOR);
        disponibilidadeQuadraRepository.saveAndFlush(disponibilidade(8, 0, 10, 0));
    }

    @Test
    void postgresCreatesReservationTableAndDeclaredIndexes() {
        Long tabelaExiste = jdbcTemplate.queryForObject("""
                select count(*)
                from information_schema.tables
                where table_schema = current_schema() and table_name = 'reservas'
                """, Long.class);
        List<String> indices = jdbcTemplate.queryForList("""
                select indexname
                from pg_indexes
                where schemaname = current_schema() and tablename = 'reservas'
                """, String.class);

        assertEquals(1L, tabelaExiste);
        assertTrue(indices.contains("idx_reservas_quadra_data_status"));
        assertTrue(indices.contains("idx_reservas_solicitante"));
    }

    @Test
    void persistsRecoversAndQueriesStudentRequest() {
        Reserva criada = reservaService.criarSolicitacao(
                aluno, quadra, DATA, INICIO, FIM, "  Treino da turma  ");

        Reserva recuperada = reservaRepository.findById(criada.getId()).orElseThrow();
        List<Reserva> solicitacoes = reservaService.listarSolicitacoesDoUsuario(aluno);

        assertEquals(StatusReserva.PENDENTE_DOCUMENTACAO, recuperada.getStatus());
        assertEquals("Treino da turma", recuperada.getFinalidade());
        assertEquals(aluno.getId(), recuperada.getSolicitante().getId());
        assertNotNull(recuperada.getCriadaEm());
        assertEquals(List.of(criada.getId()), solicitacoes.stream().map(Reserva::getId).toList());
        assertTrue(reservaRepository.findByIdAndSolicitante(criada.getId(), aluno).isPresent());
        assertTrue(reservaRepository.findByIdAndSolicitante(criada.getId(), administrador).isEmpty());
    }

    @Test
    void approvalPersistsTransitionAndApprovedReservationQuery() {
        Reserva pendente = reservaPendente(aluno, INICIO, FIM);

        Reserva aprovada = reservaService.aprovar(pendente.getId(), administrador);
        Reserva recuperada = reservaRepository.findById(aprovada.getId()).orElseThrow();
        List<Reserva> aprovadas = reservaRepository
                .findByQuadraAndDataUtilizacaoAndStatusOrderByHorarioInicio(
                        quadra, DATA, StatusReserva.APROVADA);

        assertEquals(StatusReserva.APROVADA, recuperada.getStatus());
        assertEquals(administrador.getId(), recuperada.getAnalisadaPor().getId());
        assertNotNull(recuperada.getAnalisadaEm());
        assertEquals(List.of(aprovada.getId()), aprovadas.stream().map(Reserva::getId).toList());
    }

    @Test
    void institutionalCancellationPersistsAuditAndPreservesReservationEvidence() {
        Reserva pendente = reservaPendente(aluno, INICIO, FIM);
        Reserva aprovada = reservaService.aprovar(pendente.getId(), administrador);
        Long documentoId = documentoReservaRepository.findAll().get(0).getId();
        Long contribuicaoId = contribuicaoApmRepository.findAll().get(0).getId();
        var analisadaEm = reservaRepository.findById(aprovada.getId())
                .orElseThrow()
                .getAnalisadaEm();

        List<Reserva> autorizadasAntes = reservaRepository
                .listarAutorizadasParaAdministracao(
                        StatusReserva.APROVADA,
                        LocalDate.now(CalendarioDisponibilidadeService.FUSO_HORARIO));
        assertEquals(List.of(aprovada.getId()),
                autorizadasAntes.stream().map(Reserva::getId).toList());

        reservaService.cancelarInstitucionalmente(
                aprovada.getId(), administrador, "  Interdição institucional  ");

        Reserva recuperada = reservaRepository.findById(aprovada.getId()).orElseThrow();
        assertEquals(StatusReserva.CANCELADA, recuperada.getStatus());
        assertEquals("Interdição institucional",
                recuperada.getMotivoCancelamentoInstitucional());
        assertEquals(administrador.getId(), recuperada.getCanceladaPor().getId());
        assertNotNull(recuperada.getCanceladaEm());
        assertEquals(administrador.getId(), recuperada.getAnalisadaPor().getId());
        assertEquals(analisadaEm, recuperada.getAnalisadaEm());
        assertTrue(documentoReservaRepository.findById(documentoId).isPresent());
        assertTrue(contribuicaoApmRepository.findById(contribuicaoId).isPresent());
        assertEquals(10, participanteReservaRepository.count());
        assertTrue(reservaRepository.listarAutorizadasParaSeguranca(
                StatusReserva.APROVADA,
                LocalDate.now(CalendarioDisponibilidadeService.FUSO_HORARIO)).isEmpty());
        assertTrue(reservaRepository.listarAutorizadasParaAdministracao(
                StatusReserva.APROVADA,
                LocalDate.now(CalendarioDisponibilidadeService.FUSO_HORARIO)).isEmpty());
    }

    @Test
    void pessimisticReservationLockAllowsOnlyOneInstitutionalCancellation() throws Exception {
        Reserva pendente = reservaPendente(aluno, INICIO, FIM);
        Reserva aprovada = reservaService.aprovar(pendente.getId(), administrador);
        Usuario outroAdministrador = novoUsuario(
                "outro.administrador@teste.local", Perfil.ADMINISTRADOR);
        CountDownLatch prontas = new CountDownLatch(2);
        CountDownLatch iniciar = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> primeiroResultado = executor.submit(tentativaDeCancelamento(
                    aprovada.getId(), administrador.getId(), "Interdição da direção", prontas, iniciar));
            Future<Boolean> segundoResultado = executor.submit(tentativaDeCancelamento(
                    aprovada.getId(), outroAdministrador.getId(), "Manutenção da quadra", prontas, iniciar));

            assertTrue(prontas.await(5, TimeUnit.SECONDS));
            iniciar.countDown();

            List<Boolean> resultados = List.of(
                    primeiroResultado.get(10, TimeUnit.SECONDS),
                    segundoResultado.get(10, TimeUnit.SECONDS));
            assertEquals(1L, resultados.stream().filter(Boolean::booleanValue).count());
            assertEquals(1L, resultados.stream().filter(resultado -> !resultado).count());
        } finally {
            executor.shutdownNow();
        }

        Reserva cancelada = reservaRepository.findById(aprovada.getId()).orElseThrow();
        assertEquals(StatusReserva.CANCELADA, cancelada.getStatus());
        assertNotNull(cancelada.getCanceladaPor());
        assertNotNull(cancelada.getCanceladaEm());
        assertTrue(List.of("Interdição da direção", "Manutenção da quadra")
                .contains(cancelada.getMotivoCancelamentoInstitucional()));
        assertEquals(1, contribuicaoApmRepository.count());
        assertEquals(1, documentoReservaRepository.count());
    }

    @Test
    void conflictingApprovalRollsBackAllStateChanges() {
        Reserva pendente = reservaPendente(aluno, INICIO, FIM);
        bloqueioQuadraRepository.saveAndFlush(bloqueio(8, 30, 8, 45));

        assertThrows(IllegalStateException.class,
                () -> reservaService.aprovar(pendente.getId(), administrador));

        Reserva recuperada = reservaRepository.findById(pendente.getId()).orElseThrow();
        assertEquals(StatusReserva.PENDENTE_ANALISE, recuperada.getStatus());
        assertNull(recuperada.getAnalisadaPor());
        assertNull(recuperada.getAnalisadaEm());
    }

    @Test
    void pessimisticCourtLockAllowsOnlyOneConcurrentApprovalForSamePeriod() throws Exception {
        Reserva primeira = reservaPendente(aluno, INICIO, FIM);
        Usuario outroAluno = novoUsuario("outro.aluno@teste.local", Perfil.ALUNO);
        Reserva segunda = reservaPendente(outroAluno, INICIO, FIM);
        CountDownLatch prontas = new CountDownLatch(2);
        CountDownLatch iniciar = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> resultadoPrimeira = executor.submit(
                    tentativaDeAprovacao(primeira.getId(), prontas, iniciar));
            Future<Boolean> resultadoSegunda = executor.submit(
                    tentativaDeAprovacao(segunda.getId(), prontas, iniciar));

            assertTrue(prontas.await(5, TimeUnit.SECONDS));
            iniciar.countDown();

            List<Boolean> resultados = List.of(
                    resultadoPrimeira.get(10, TimeUnit.SECONDS),
                    resultadoSegunda.get(10, TimeUnit.SECONDS));
            assertEquals(1L, resultados.stream().filter(Boolean::booleanValue).count());
            assertEquals(1L, resultados.stream().filter(resultado -> !resultado).count());
        } finally {
            executor.shutdownNow();
        }

        List<Reserva> aprovadas = reservaRepository
                .findByQuadraAndDataUtilizacaoAndStatusOrderByHorarioInicio(
                        quadra, DATA, StatusReserva.APROVADA);
        long pendentes = reservaRepository.findAll().stream()
                .filter(reserva -> reserva.getStatus() == StatusReserva.PENDENTE_ANALISE)
                .count();

        assertEquals(1, aprovadas.size());
        assertEquals(1, pendentes);
    }

    private Callable<Boolean> tentativaDeAprovacao(
            Long reservaId, CountDownLatch prontas, CountDownLatch iniciar) {
        return () -> {
            prontas.countDown();
            if (!iniciar.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("As aprovações não iniciaram de forma coordenada");
            }
            Usuario adminDaTransacao = usuarioRepository.findById(administrador.getId()).orElseThrow();
            try {
                reservaService.aprovar(reservaId, adminDaTransacao);
                return true;
            } catch (IllegalStateException conflitoEsperado) {
                return false;
            }
        };
    }

    private Callable<Boolean> tentativaDeCancelamento(
            Long reservaId,
            Long administradorId,
            String motivo,
            CountDownLatch prontas,
            CountDownLatch iniciar) {
        return () -> {
            prontas.countDown();
            if (!iniciar.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Os cancelamentos não iniciaram de forma coordenada");
            }
            Usuario adminDaTransacao = usuarioRepository.findById(administradorId).orElseThrow();
            try {
                reservaService.cancelarInstitucionalmente(reservaId, adminDaTransacao, motivo);
                return true;
            } catch (IllegalStateException repeticaoEsperada) {
                return false;
            }
        };
    }

    private Quadra novaQuadra() {
        Quadra nova = new Quadra();
        nova.setNome("Quadra de teste");
        nova.setDescricao("Criada somente no PostgreSQL temporário do CI");
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

    private DisponibilidadeQuadra disponibilidade(
            int horaInicio, int minutoInicio, int horaFim, int minutoFim) {
        DisponibilidadeQuadra disponibilidade = new DisponibilidadeQuadra();
        disponibilidade.setQuadra(quadra);
        disponibilidade.setDiaSemana(DiaSemana.from(DATA.getDayOfWeek()));
        disponibilidade.setHorarioInicio(LocalTime.of(horaInicio, minutoInicio));
        disponibilidade.setHorarioFim(LocalTime.of(horaFim, minutoFim));
        disponibilidade.setAtiva(true);
        return disponibilidade;
    }

    private Reserva reservaPendente(
            Usuario solicitante, LocalTime horarioInicio, LocalTime horarioFim) {
        Reserva reserva = new Reserva();
        reserva.setQuadra(quadra);
        reserva.setSolicitante(solicitante);
        reserva.setDataUtilizacao(DATA);
        reserva.setHorarioInicio(horarioInicio);
        reserva.setHorarioFim(horarioFim);
        reserva.setFinalidade("Atividade esportiva de teste");
        reserva.setStatus(StatusReserva.PENDENTE_ANALISE);
        reserva = reservaRepository.saveAndFlush(reserva);
        prepararElegibilidade(reserva);
        return reserva;
    }

    private void prepararElegibilidade(Reserva reserva) {
        for (int indice = 1; indice <= 10; indice++) {
            ParticipanteReserva participante = new ParticipanteReserva();
            participante.setReserva(reserva);
            participante.setNomeCompleto("Aluno " + indice);
            participante.setRmOuRg("RM-" + reserva.getId() + "-" + indice);
            participante.setCategoria(CategoriaParticipante.ALUNO);
            participanteReservaRepository.save(participante);
        }
        participanteReservaRepository.flush();

        DocumentoReserva documento = new DocumentoReserva();
        documento.setReserva(reserva);
        documento.setVersao(1);
        documento.setRevisaoParticipantes(reserva.getRevisaoParticipantes());
        documento.setNomeOriginal("termo.pdf");
        documento.setChaveArmazenamento("reserva-" + reserva.getId() + ".pdf");
        documento.setSha256("0".repeat(64));
        documento.setTamanhoBytes(10);
        documento.setEnviadoPor(reserva.getSolicitante());
        documento = documentoReservaRepository.saveAndFlush(documento);

        ConferenciaDocumento conferencia = new ConferenciaDocumento();
        conferencia.setDocumento(documento);
        conferencia.setResultado(ResultadoConferenciaDocumento.VALIDADO);
        conferencia.setConferidoPor(administrador);
        conferencia = conferenciaDocumentoRepository.saveAndFlush(conferencia);

        ContribuicaoApm contribuicao = new ContribuicaoApm();
        contribuicao.setReserva(reserva);
        contribuicao.setConferenciaDocumento(conferencia);
        contribuicao.setValor(new BigDecimal("20.00"));
        contribuicao.setRegistradaPor(administrador);
        contribuicaoApmRepository.saveAndFlush(contribuicao);
    }

    private BloqueioQuadra bloqueio(
            int horaInicio, int minutoInicio, int horaFim, int minutoFim) {
        BloqueioQuadra bloqueio = new BloqueioQuadra();
        bloqueio.setQuadra(quadra);
        bloqueio.setData(DATA);
        bloqueio.setHorarioInicio(LocalTime.of(horaInicio, minutoInicio));
        bloqueio.setHorarioFim(LocalTime.of(horaFim, minutoFim));
        bloqueio.setMotivo("Bloqueio de teste");
        bloqueio.setAtivo(true);
        return bloqueio;
    }
}
