package br.com.courttime.service;

import br.com.courttime.entity.Perfil;
import br.com.courttime.entity.Quadra;
import br.com.courttime.entity.Reserva;
import br.com.courttime.entity.StatusReserva;
import br.com.courttime.entity.Usuario;
import br.com.courttime.repository.QuadraRepository;
import br.com.courttime.repository.ReservaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

    private ReservaService service;
    private Quadra quadra;
    private Usuario aluno;
    private Usuario administrador;

    @BeforeEach
    void setUp() {
        service = new ReservaService(
                reservaRepository, quadraRepository, calendarioDisponibilidadeService);
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
        when(reservaRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(reserva));
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
        when(reservaRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(reserva));
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
        when(reservaRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(reserva));

        assertThrows(IllegalStateException.class, () -> service.aprovar(30L, administrador));
        verify(quadraRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void administratorCannotApproveOwnRequest() {
        Reserva reserva = reservaPendente(administrador);
        when(reservaRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(reserva));

        assertThrows(AccessDeniedException.class, () -> service.aprovar(30L, administrador));
        verify(quadraRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void rejectionRequiresReasonAndRecordsResponsibleAdministrator() {
        Reserva reserva = reservaPendente(aluno);
        when(reservaRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(reserva));
        when(reservaRepository.save(reserva)).thenReturn(reserva);

        Reserva rejeitada = service.rejeitar(30L, administrador, "  Documentação incompleta  ");

        assertEquals(StatusReserva.REJEITADA, rejeitada.getStatus());
        assertEquals("Documentação incompleta", rejeitada.getMotivoRejeicao());
        assertEquals(administrador, rejeitada.getAnalisadaPor());
        assertNotNull(rejeitada.getAnalisadaEm());
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
