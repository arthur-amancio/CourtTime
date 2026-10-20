package br.com.courttime.service;

import br.com.courttime.entity.ConferenciaDocumento;
import br.com.courttime.entity.ContribuicaoApm;
import br.com.courttime.entity.DocumentoReserva;
import br.com.courttime.entity.Perfil;
import br.com.courttime.entity.Quadra;
import br.com.courttime.entity.Reserva;
import br.com.courttime.entity.StatusReserva;
import br.com.courttime.entity.ResultadoConferenciaDocumento;
import br.com.courttime.entity.Usuario;
import br.com.courttime.repository.ContribuicaoApmRepository;
import br.com.courttime.repository.DocumentoReservaRepository;
import br.com.courttime.repository.QuadraRepository;
import br.com.courttime.repository.ReservaRepository;
import br.com.courttime.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

    private static final LocalDate DATA = LocalDate.of(2026, 10, 19);
    private static final LocalTime INICIO = LocalTime.of(8, 0);
    private static final LocalTime FIM = LocalTime.of(9, 0);

    /** Data de hoje usada nos testes da regra de antecedência. */
    private static final LocalDate HOJE = LocalDate.of(2026, 10, 19);
    private static final LocalDate AMANHA = HOJE.plusDays(1);

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private QuadraRepository quadraRepository;

    @Mock
    private CalendarioDisponibilidadeService calendarioDisponibilidadeService;
    @Mock
    private DocumentoReservaRepository documentoRepository;
    @Mock
    private ParticipanteReservaService participanteService;
    @Mock
    private ContribuicaoApmRepository contribuicaoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;

    private ReservaService service;
    private Quadra quadra;
    private Usuario aluno;
    private Usuario administrador;

    @BeforeEach
    void setUp() {
        service = new ReservaService(
                reservaRepository,
                quadraRepository,
                calendarioDisponibilidadeService,
                documentoRepository,
                participanteService,
                contribuicaoRepository,
                usuarioRepository);
        quadra = new Quadra();
        quadra.setId(1L);
        quadra.setAtiva(true);
        aluno = usuario(10L, Perfil.ALUNO);
        administrador = usuario(20L, Perfil.ADMINISTRADOR);
    }

    @Test
    void activeStudentCreatesPendingRequestForAvailablePeriod() {
        when(calendarioDisponibilidadeService.obterDataMinimaParaSolicitacao())
                .thenReturn(DATA);
        when(calendarioDisponibilidadeService.periodoEstaDisponivel(
                quadra, DATA, INICIO, FIM)).thenReturn(true);
        when(reservaRepository.save(any(Reserva.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Reserva reserva = service.criarSolicitacao(
                aluno, quadra, DATA, INICIO, FIM, "  Treino da turma  ");

        assertEquals(StatusReserva.PENDENTE_DOCUMENTACAO, reserva.getStatus());
        assertEquals("Treino da turma", reserva.getFinalidade());
        assertEquals(aluno, reserva.getSolicitante());
    }

    @Test
    void requestIsRejectedWhenPeriodIsUnavailable() {
        when(calendarioDisponibilidadeService.obterDataMinimaParaSolicitacao())
                .thenReturn(DATA);
        when(calendarioDisponibilidadeService.periodoEstaDisponivel(
                quadra, DATA, INICIO, FIM)).thenReturn(false);

        assertThrows(IllegalStateException.class, () -> service.criarSolicitacao(
                aluno, quadra, DATA, INICIO, FIM, "Treino"));
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void nonStudentCannotCreateRequest() {
        assertThrows(AccessDeniedException.class, () -> service.criarSolicitacao(
                administrador, quadra, DATA, INICIO, FIM, "Treino"));
    }

    @Test
    void requestCannotBeCreatedForPastDate() {
        when(calendarioDisponibilidadeService.obterDataMinimaParaSolicitacao())
                .thenReturn(AMANHA);

        assertThrows(IllegalArgumentException.class, () -> service.criarSolicitacao(
                aluno, quadra, HOJE.minusDays(1), INICIO, FIM, "Treino"));
        verify(calendarioDisponibilidadeService, never()).periodoEstaDisponivel(
                any(), any(), any(), any());
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void requestCannotBeCreatedForToday() {
        when(calendarioDisponibilidadeService.obterDataMinimaParaSolicitacao())
                .thenReturn(AMANHA);

        IllegalArgumentException erro = assertThrows(
                IllegalArgumentException.class, () -> service.criarSolicitacao(
                        aluno, quadra, HOJE, INICIO, FIM, "Treino"));

        assertTrue(erro.getMessage().contains("um dia de antecedência"));
        verify(calendarioDisponibilidadeService, never()).periodoEstaDisponivel(
                any(), any(), any(), any());
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void requestIsAcceptedForTomorrowWhenPeriodIsAvailable() {
        when(calendarioDisponibilidadeService.obterDataMinimaParaSolicitacao())
                .thenReturn(AMANHA);
        when(calendarioDisponibilidadeService.periodoEstaDisponivel(
                quadra, AMANHA, INICIO, FIM)).thenReturn(true);
        when(reservaRepository.save(any(Reserva.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Reserva reserva = service.criarSolicitacao(
                aluno, quadra, AMANHA, INICIO, FIM, "Treino da turma");

        assertEquals(AMANHA, reserva.getDataUtilizacao());
        assertEquals(StatusReserva.PENDENTE_DOCUMENTACAO, reserva.getStatus());
    }

    @Test
    void requestIsAcceptedForLaterDateWhenPeriodIsAvailable() {
        LocalDate dataFutura = AMANHA.plusDays(7);
        when(calendarioDisponibilidadeService.obterDataMinimaParaSolicitacao())
                .thenReturn(AMANHA);
        when(calendarioDisponibilidadeService.periodoEstaDisponivel(
                quadra, dataFutura, INICIO, FIM)).thenReturn(true);
        when(reservaRepository.save(any(Reserva.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Reserva reserva = service.criarSolicitacao(
                aluno, quadra, dataFutura, INICIO, FIM, "Campeonato interno");

        assertEquals(dataFutura, reserva.getDataUtilizacao());
        assertEquals(StatusReserva.PENDENTE_DOCUMENTACAO, reserva.getStatus());
    }

    @Test
    void studentCanOnlyObtainOwnRequest() {
        Reserva reserva = reservaPendente(aluno);
        when(reservaRepository.findByIdAndSolicitante(30L, aluno))
                .thenReturn(Optional.of(reserva));

        Reserva encontrada = service.obterSolicitacaoDoUsuario(30L, aluno);

        assertEquals(reserva, encontrada);
    }

    @Test
    void studentCannotObtainAnotherStudentsRequest() {
        when(reservaRepository.findByIdAndSolicitante(30L, aluno))
                .thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class,
                () -> service.obterSolicitacaoDoUsuario(30L, aluno));
    }

    @Test
    void administratorApprovesPendingRequestAfterLockingCourtAndRecheckingAvailability() {
        Reserva reserva = reservaPendente(aluno);
        prepararAdministradorAtual();
        when(reservaRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(reserva));
        prepararPeriodoAindaNaoIniciado();
        prepararElegibilidade(reserva);
        when(quadraRepository.findByIdForUpdate(quadra.getId())).thenReturn(Optional.of(quadra));
        when(calendarioDisponibilidadeService.periodoEstaDisponivel(
                quadra, DATA, INICIO, FIM)).thenReturn(true);
        when(reservaRepository.save(reserva)).thenReturn(reserva);

        Reserva aprovada = service.aprovar(30L, administrador);

        assertEquals(StatusReserva.APROVADA, aprovada.getStatus());
        assertEquals(administrador, aprovada.getAnalisadaPor());
        assertNotNull(aprovada.getAnalisadaEm());
        verify(quadraRepository).findByIdForUpdate(quadra.getId());
    }

    @Test
    void approvalIsRejectedWhenPeriodIsNoLongerAvailable() {
        Reserva reserva = reservaPendente(aluno);
        prepararAdministradorAtual();
        when(reservaRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(reserva));
        prepararPeriodoAindaNaoIniciado();
        prepararElegibilidade(reserva);
        when(quadraRepository.findByIdForUpdate(quadra.getId())).thenReturn(Optional.of(quadra));
        when(calendarioDisponibilidadeService.periodoEstaDisponivel(
                quadra, DATA, INICIO, FIM)).thenReturn(false);

        assertThrows(IllegalStateException.class, () -> service.aprovar(30L, administrador));
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void requestCannotBeApprovedBeforeDocumentReviewStage() {
        Reserva reserva = reservaPendente(aluno);
        reserva.setStatus(StatusReserva.PENDENTE_DOCUMENTACAO);
        prepararAdministradorAtual();
        when(reservaRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(reserva));

        assertThrows(IllegalStateException.class, () -> service.aprovar(30L, administrador));
        verify(quadraRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void administratorCannotApproveOwnRequest() {
        Reserva reserva = reservaPendente(administrador);
        prepararAdministradorAtual();
        when(reservaRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(reserva));

        assertThrows(AccessDeniedException.class, () -> service.aprovar(30L, administrador));
        verify(quadraRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void approvalIsRejectedWhenUtilizationDateIsInThePast() {
        Reserva reserva = reservaPendente(aluno);
        prepararAdministradorAtual();
        when(reservaRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(reserva));
        agoraEm(DATA.plusDays(1).atTime(8, 0));

        IllegalStateException erro = assertThrows(
                IllegalStateException.class, () -> service.aprovar(30L, administrador));

        assertTrue(erro.getMessage().contains("já começou ou já passou"));
        verify(quadraRepository, never()).findByIdForUpdate(any());
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void approvalIsRejectedWhenPeriodHasAlreadyStarted() {
        Reserva reserva = reservaPendente(aluno);
        prepararAdministradorAtual();
        when(reservaRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(reserva));
        // Exatamente o instante de inicio: o periodo ja comecou.
        agoraEm(LocalDateTime.of(DATA, INICIO));

        assertThrows(IllegalStateException.class, () -> service.aprovar(30L, administrador));
        verify(quadraRepository, never()).findByIdForUpdate(any());
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void approvalIsRejectedWhenPeriodHasAlreadyEnded() {
        Reserva reserva = reservaPendente(aluno);
        prepararAdministradorAtual();
        when(reservaRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(reserva));
        agoraEm(LocalDateTime.of(DATA, FIM).plusMinutes(30));

        assertThrows(IllegalStateException.class, () -> service.aprovar(30L, administrador));
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void administratorMayApproveOnTheSameDayBeforeTheStart() {
        Reserva reserva = reservaPendente(aluno);
        prepararAdministradorAtual();
        when(reservaRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(reserva));
        // Mesmo dia da utilizacao, uma hora antes do inicio.
        agoraEm(LocalDateTime.of(DATA, INICIO).minusHours(1));
        prepararElegibilidade(reserva);
        when(quadraRepository.findByIdForUpdate(quadra.getId())).thenReturn(Optional.of(quadra));
        when(calendarioDisponibilidadeService.periodoEstaDisponivel(
                quadra, DATA, INICIO, FIM)).thenReturn(true);
        when(reservaRepository.save(reserva)).thenReturn(reserva);

        Reserva aprovada = service.aprovar(30L, administrador);

        assertEquals(StatusReserva.APROVADA, aprovada.getStatus());
    }

    @Test
    void expiredRequestCanStillBeRejectedWithReason() {
        Reserva reserva = reservaPendente(aluno);
        prepararAdministradorAtual();
        when(reservaRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(reserva));
        when(reservaRepository.save(reserva)).thenReturn(reserva);

        Reserva rejeitada = service.rejeitar(30L, administrador, "Data de utilização expirada");

        assertEquals(StatusReserva.REJEITADA, rejeitada.getStatus());
        assertEquals("Data de utilização expirada", rejeitada.getMotivoRejeicao());
    }

    @Test
    void rejectionRequiresReasonAndRecordsResponsibleAdministrator() {
        Reserva reserva = reservaPendente(aluno);
        prepararAdministradorAtual();
        when(reservaRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(reserva));
        when(reservaRepository.save(reserva)).thenReturn(reserva);

        Reserva rejeitada = service.rejeitar(30L, administrador, "  Documentação incompleta  ");

        assertEquals(StatusReserva.REJEITADA, rejeitada.getStatus());
        assertEquals("Documentação incompleta", rejeitada.getMotivoRejeicao());
        assertEquals(administrador, rejeitada.getAnalisadaPor());
        assertNotNull(rejeitada.getAnalisadaEm());
    }

    @Test
    void approvalRevalidatesCurrentAdministratorAndCompleteEligibility() {
        Reserva reserva = reservaPendente(aluno);
        Usuario desativado = usuario(20L, Perfil.ADMINISTRADOR);
        desativado.setAtivo(false);
        when(usuarioRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(desativado));

        assertThrows(AccessDeniedException.class,
                () -> service.aprovar(30L, administrador));
        verify(reservaRepository, never()).findByIdForUpdate(any());

        when(usuarioRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(administrador));
        when(reservaRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(reserva));
        prepararPeriodoAindaNaoIniciado();
        when(documentoRepository.obterVersaoMaisRecenteComConferencia(reserva))
                .thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class,
                () -> service.aprovar(30L, administrador));
        verify(quadraRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void administratorCancelsApprovedCurrentReservationAndPreservesApprovalData() {
        Reserva reserva = reservaAprovada(aluno);
        Usuario administradorDaAprovacao = usuario(21L, Perfil.ADMINISTRADOR);
        Instant analisadaEm = Instant.parse("2026-10-18T12:00:00Z");
        reserva.setAnalisadaPor(administradorDaAprovacao);
        reserva.setAnalisadaEm(analisadaEm);
        reserva.setRevisaoParticipantes(4);
        prepararAdministradorAtual();
        when(reservaRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(reserva));
        when(calendarioDisponibilidadeService.obterDataAtual()).thenReturn(DATA);
        when(reservaRepository.save(reserva)).thenReturn(reserva);

        Reserva cancelada = service.cancelarInstitucionalmente(
                30L, administrador, "  Manutenção emergencial da quadra  ");

        assertEquals(StatusReserva.CANCELADA, cancelada.getStatus());
        assertEquals("Manutenção emergencial da quadra",
                cancelada.getMotivoCancelamentoInstitucional());
        assertSame(administrador, cancelada.getCanceladaPor());
        assertNotNull(cancelada.getCanceladaEm());
        assertSame(administradorDaAprovacao, cancelada.getAnalisadaPor());
        assertEquals(analisadaEm, cancelada.getAnalisadaEm());
        assertEquals(4, cancelada.getRevisaoParticipantes());
    }

    @Test
    void cancellationRevalidatesActiveAdministratorAndOwnership() {
        Usuario administradorDesativado = usuario(20L, Perfil.ADMINISTRADOR);
        administradorDesativado.setAtivo(false);
        when(usuarioRepository.findByIdForUpdate(20L))
                .thenReturn(Optional.of(administradorDesativado));

        assertThrows(AccessDeniedException.class,
                () -> service.cancelarInstitucionalmente(30L, administrador, "Interdição"));
        verify(reservaRepository, never()).findByIdForUpdate(any());

        Usuario seguranca = usuario(20L, Perfil.SEGURANCA);
        when(usuarioRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(seguranca));
        assertThrows(AccessDeniedException.class,
                () -> service.cancelarInstitucionalmente(30L, administrador, "Interdição"));
        verify(reservaRepository, never()).findByIdForUpdate(any());

        prepararAdministradorAtual();
        Reserva propriaReserva = reservaAprovada(administrador);
        when(reservaRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(propriaReserva));

        assertThrows(AccessDeniedException.class,
                () -> service.cancelarInstitucionalmente(30L, administrador, "Interdição"));
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void cancellationRequiresValidReasonApprovedStateAndCurrentDate() {
        prepararAdministradorAtual();

        assertThrows(IllegalArgumentException.class,
                () -> service.cancelarInstitucionalmente(30L, administrador, "  "));
        assertThrows(IllegalArgumentException.class,
                () -> service.cancelarInstitucionalmente(
                        30L, administrador, "a".repeat(501)));

        Reserva pendente = reservaPendente(aluno);
        when(reservaRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(pendente));
        assertThrows(IllegalStateException.class,
                () -> service.cancelarInstitucionalmente(30L, administrador, "Interdição"));

        Reserva passada = reservaAprovada(aluno);
        passada.setDataUtilizacao(DATA.minusDays(1));
        when(reservaRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(passada));
        when(calendarioDisponibilidadeService.obterDataAtual()).thenReturn(DATA);
        assertThrows(IllegalStateException.class,
                () -> service.cancelarInstitucionalmente(30L, administrador, "Interdição"));

        verify(reservaRepository, never()).save(any());
    }

    @Test
    void repeatedCancellationIsRejectedWithoutOverwritingOriginalRecord() {
        Reserva reserva = reservaAprovada(aluno);
        prepararAdministradorAtual();
        when(reservaRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(reserva));
        when(calendarioDisponibilidadeService.obterDataAtual()).thenReturn(DATA);
        when(reservaRepository.save(reserva)).thenReturn(reserva);

        service.cancelarInstitucionalmente(30L, administrador, "Primeiro motivo");
        Instant canceladaEm = reserva.getCanceladaEm();

        assertThrows(IllegalStateException.class,
                () -> service.cancelarInstitucionalmente(
                        30L, administrador, "Motivo que não deve substituir o primeiro"));
        assertEquals("Primeiro motivo", reserva.getMotivoCancelamentoInstitucional());
        assertEquals(canceladaEm, reserva.getCanceladaEm());
    }

    /** Fixa o instante atual visto pelo servico. */
    private void agoraEm(LocalDateTime instante) {
        when(calendarioDisponibilidadeService.obterDataHoraAtual()).thenReturn(instante);
    }

    /** O periodo da reserva de teste ainda nao comecou. */
    private void prepararPeriodoAindaNaoIniciado() {
        agoraEm(LocalDateTime.of(DATA, INICIO).minusDays(1));
    }

    private void prepararAdministradorAtual() {
        when(usuarioRepository.findByIdForUpdate(administrador.getId()))
                .thenReturn(Optional.of(administrador));
    }

    private void prepararElegibilidade(Reserva reserva) {
        DocumentoReserva documento = new DocumentoReserva();
        documento.setId(40L);
        documento.setReserva(reserva);
        documento.setVersao(1);
        documento.setRevisaoParticipantes(reserva.getRevisaoParticipantes());
        ConferenciaDocumento conferencia = new ConferenciaDocumento();
        conferencia.setDocumento(documento);
        conferencia.setResultado(ResultadoConferenciaDocumento.VALIDADO);
        conferencia.setConferidoPor(administrador);
        ReflectionTestUtils.setField(documento, "conferencia", conferencia);
        ContribuicaoApm contribuicao = new ContribuicaoApm();
        contribuicao.setReserva(reserva);
        contribuicao.setConferenciaDocumento(conferencia);

        when(documentoRepository.obterVersaoMaisRecenteComConferencia(reserva))
                .thenReturn(Optional.of(documento));
        when(contribuicaoRepository.obterPorReservaId(reserva.getId()))
                .thenReturn(Optional.of(contribuicao));
    }

    // ---------- Remarcação após cancelamento institucional ----------

    @Test
    void remarcacaoCriaNovaSolicitacaoPendenteVinculadaACanceladaECopiaParticipantes() {
        Reserva cancelada = reservaCancelada(aluno);
        when(reservaRepository.findByIdAndSolicitanteIdForUpdate(30L, aluno.getId()))
                .thenReturn(Optional.of(cancelada));
        when(reservaRepository.existsByRemarcacaoDe(cancelada)).thenReturn(false);
        when(calendarioDisponibilidadeService.obterDataMinimaParaSolicitacao()).thenReturn(DATA);
        when(calendarioDisponibilidadeService.periodoEstaDisponivel(quadra, DATA, INICIO, FIM))
                .thenReturn(true);
        when(reservaRepository.save(any(Reserva.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Reserva nova = service.criarRemarcacao(
                aluno, quadra, 30L, DATA, INICIO, FIM, "Treino remarcado", true);

        assertEquals(StatusReserva.PENDENTE_DOCUMENTACAO, nova.getStatus());
        assertSame(cancelada, nova.getRemarcacaoDe());
        assertEquals(StatusReserva.CANCELADA, cancelada.getStatus(),
                "a reserva cancelada não é alterada");
        verify(participanteService).copiarParticipantes(cancelada, nova);
    }

    @Test
    void remarcacaoSemCopiaNaoCopiaParticipantes() {
        Reserva cancelada = reservaCancelada(aluno);
        when(reservaRepository.findByIdAndSolicitanteIdForUpdate(30L, aluno.getId()))
                .thenReturn(Optional.of(cancelada));
        when(reservaRepository.existsByRemarcacaoDe(cancelada)).thenReturn(false);
        when(calendarioDisponibilidadeService.obterDataMinimaParaSolicitacao()).thenReturn(DATA);
        when(calendarioDisponibilidadeService.periodoEstaDisponivel(quadra, DATA, INICIO, FIM))
                .thenReturn(true);
        when(reservaRepository.save(any(Reserva.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.criarRemarcacao(aluno, quadra, 30L, DATA, INICIO, FIM, "Treino", false);

        verify(participanteService, never()).copiarParticipantes(any(), any());
    }

    @Test
    void remarcacaoDeReservaDeOutroAlunoENegada() {
        when(reservaRepository.findByIdAndSolicitanteIdForUpdate(30L, aluno.getId()))
                .thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () -> service.criarRemarcacao(
                aluno, quadra, 30L, DATA, INICIO, FIM, "Treino", true));
        verify(reservaRepository, never()).save(any(Reserva.class));
    }

    @Test
    void remarcacaoExigeReservaCancelada() {
        Reserva aprovada = reservaAprovada(aluno);
        when(reservaRepository.findByIdAndSolicitanteIdForUpdate(30L, aluno.getId()))
                .thenReturn(Optional.of(aprovada));

        IllegalStateException erro = assertThrows(IllegalStateException.class,
                () -> service.criarRemarcacao(aluno, quadra, 30L, DATA, INICIO, FIM, "Treino", true));
        assertTrue(erro.getMessage().contains("canceladas"));
        verify(reservaRepository, never()).save(any(Reserva.class));
    }

    @Test
    void reservaCanceladaSoPodeSerRemarcadaUmaVez() {
        Reserva cancelada = reservaCancelada(aluno);
        when(reservaRepository.findByIdAndSolicitanteIdForUpdate(30L, aluno.getId()))
                .thenReturn(Optional.of(cancelada));
        when(reservaRepository.existsByRemarcacaoDe(cancelada)).thenReturn(true);

        IllegalStateException erro = assertThrows(IllegalStateException.class,
                () -> service.criarRemarcacao(aluno, quadra, 30L, DATA, INICIO, FIM, "Treino", true));
        assertTrue(erro.getMessage().contains("já possui"));
        verify(reservaRepository, never()).save(any(Reserva.class));
    }

    @Test
    void remarcacaoRespeitaADisponibilidadeReal() {
        Reserva cancelada = reservaCancelada(aluno);
        when(reservaRepository.findByIdAndSolicitanteIdForUpdate(30L, aluno.getId()))
                .thenReturn(Optional.of(cancelada));
        when(reservaRepository.existsByRemarcacaoDe(cancelada)).thenReturn(false);
        when(calendarioDisponibilidadeService.obterDataMinimaParaSolicitacao()).thenReturn(DATA);
        when(calendarioDisponibilidadeService.periodoEstaDisponivel(quadra, DATA, INICIO, FIM))
                .thenReturn(false);

        assertThrows(IllegalStateException.class, () -> service.criarRemarcacao(
                aluno, quadra, 30L, DATA, INICIO, FIM, "Treino", true));
        verify(reservaRepository, never()).save(any(Reserva.class));
        verify(participanteService, never()).copiarParticipantes(any(), any());
    }

    @Test
    void remarcacaoRespeitaAAntecedenciaMinima() {
        Reserva cancelada = reservaCancelada(aluno);
        when(reservaRepository.findByIdAndSolicitanteIdForUpdate(30L, aluno.getId()))
                .thenReturn(Optional.of(cancelada));
        when(reservaRepository.existsByRemarcacaoDe(cancelada)).thenReturn(false);
        when(calendarioDisponibilidadeService.obterDataMinimaParaSolicitacao()).thenReturn(AMANHA);

        assertThrows(IllegalArgumentException.class, () -> service.criarRemarcacao(
                aluno, quadra, 30L, HOJE, INICIO, FIM, "Treino", true));
        verify(reservaRepository, never()).save(any(Reserva.class));
    }

    @Test
    void telaDeRemarcacaoSoAbreParaReservaCanceladaDoAluno() {
        Reserva pendente = reservaPendente(aluno);
        when(reservaRepository.findByIdAndSolicitante(30L, aluno)).thenReturn(Optional.of(pendente));

        assertThrows(IllegalStateException.class, () -> service.obterParaRemarcacao(30L, aluno));
    }

    private Reserva reservaCancelada(Usuario solicitante) {
        Reserva reserva = reservaPendente(solicitante);
        reserva.setStatus(StatusReserva.CANCELADA);
        reserva.setMotivoCancelamentoInstitucional("Interdição da quadra");
        return reserva;
    }

    private Reserva reservaPendente(Usuario solicitante) {
        Reserva reserva = new Reserva();
        reserva.setId(30L);
        reserva.setQuadra(quadra);
        reserva.setSolicitante(solicitante);
        reserva.setDataUtilizacao(DATA);
        reserva.setHorarioInicio(INICIO);
        reserva.setHorarioFim(FIM);
        reserva.setFinalidade("Treino");
        reserva.setStatus(StatusReserva.PENDENTE_ANALISE);
        return reserva;
    }

    private Reserva reservaAprovada(Usuario solicitante) {
        Reserva reserva = reservaPendente(solicitante);
        reserva.setStatus(StatusReserva.APROVADA);
        return reserva;
    }

    private Usuario usuario(Long id, Perfil perfil) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNome("Usuário");
        usuario.setEmail("usuario" + id + "@escola.test");
        usuario.setSenha("hash");
        usuario.setPerfil(perfil);
        usuario.setAtivo(true);
        return usuario;
    }
}
