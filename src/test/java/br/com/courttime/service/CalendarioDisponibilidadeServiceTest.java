package br.com.courttime.service;

import br.com.courttime.entity.BloqueioQuadra;
import br.com.courttime.entity.DiaSemana;
import br.com.courttime.entity.DisponibilidadeQuadra;
import br.com.courttime.entity.Quadra;
import br.com.courttime.entity.Reserva;
import br.com.courttime.entity.StatusReserva;
import br.com.courttime.repository.BloqueioQuadraRepository;
import br.com.courttime.repository.DisponibilidadeQuadraRepository;
import br.com.courttime.repository.ReservaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalendarioDisponibilidadeServiceTest {

    private static final LocalDate SEGUNDA_FEIRA = LocalDate.of(2026, 10, 12);

    @Mock
    private DisponibilidadeQuadraRepository disponibilidadeQuadraRepository;

    @Mock
    private BloqueioQuadraRepository bloqueioQuadraRepository;

    @Mock
    private ReservaRepository reservaRepository;

    private CalendarioDisponibilidadeService service;
    private Quadra quadra;

    @BeforeEach
    void setUp() {
        service = new CalendarioDisponibilidadeService(
                disponibilidadeQuadraRepository, bloqueioQuadraRepository, reservaRepository);
        quadra = new Quadra();
        quadra.setAtiva(true);
    }

    @Test
    void minimumRequestDateIsTheDayAfterTheLocalCurrentDate() {
        assertEquals(service.obterDataAtual().plusDays(1), service.obterDataMinimaParaSolicitacao());
    }

    @Test
    void currentDateUsesSaoPauloZone() {
        assertEquals(
                LocalDate.now(CalendarioDisponibilidadeService.FUSO_HORARIO),
                service.obterDataAtual());
    }

    @Test
    void twoHourAvailabilityCreatesTwoCompletePeriods() {
        DisponibilidadeQuadra disponibilidade = disponibilidade(8, 0, 10, 0);
        when(disponibilidadeQuadraRepository
                .findByQuadraAndDiaSemanaAndAtivaTrueOrderByHorarioInicio(
                        quadra, DiaSemana.SEGUNDA))
                .thenReturn(List.of(disponibilidade));
        when(bloqueioQuadraRepository
                .findByQuadraAndDataAndAtivoTrueOrderByHorarioInicio(quadra, SEGUNDA_FEIRA))
                .thenReturn(List.of());
        when(reservaRepository.findByQuadraAndDataUtilizacaoAndStatusOrderByHorarioInicio(
                quadra, SEGUNDA_FEIRA, StatusReserva.APROVADA)).thenReturn(List.of());

        ConsultaDisponibilidade consulta = service.consultar(quadra, SEGUNDA_FEIRA);

        assertEquals(2, consulta.horarios().size());
        assertEquals("08:00 – 09:00", consulta.horarios().get(0).getPeriodoFormatado());
        assertEquals("09:00 – 10:00", consulta.horarios().get(1).getPeriodoFormatado());
        assertTrue(consulta.horarios().stream().allMatch(HorarioDisponibilidade::isDisponivel));
    }

    @Test
    void availabilityWithRemainingHalfHourCreatesOnlyCompletePeriod() {
        prepararConsulta(disponibilidade(8, 0, 9, 30), List.of());

        ConsultaDisponibilidade consulta = service.consultar(quadra, SEGUNDA_FEIRA);

        assertEquals(1, consulta.horarios().size());
        assertEquals("08:00 – 09:00", consulta.horarios().get(0).getPeriodoFormatado());
    }

    @Test
    void availabilityMayStartOutsideFullHour() {
        prepararConsulta(disponibilidade(8, 15, 9, 15), List.of());

        ConsultaDisponibilidade consulta = service.consultar(quadra, SEGUNDA_FEIRA);

        assertEquals(1, consulta.horarios().size());
        assertEquals("08:15 – 09:15", consulta.horarios().get(0).getPeriodoFormatado());
    }

    @Test
    void availabilityShorterThanOneHourCreatesNoPeriods() {
        prepararConsulta(disponibilidade(8, 0, 8, 30), List.of());

        ConsultaDisponibilidade consulta = service.consultar(quadra, SEGUNDA_FEIRA);

        assertTrue(consulta.horarios().isEmpty());
    }

    @Test
    void lateAvailabilityCreatesPeriodWithoutChangingDay() {
        prepararConsulta(disponibilidade(22, 30, 23, 30), List.of());

        ConsultaDisponibilidade consulta = service.consultar(quadra, SEGUNDA_FEIRA);

        assertEquals(1, consulta.horarios().size());
        assertEquals("22:30 – 23:30", consulta.horarios().get(0).getPeriodoFormatado());
    }

    @Test
    void availabilityEndingBeforeMidnightDoesNotWrapPeriodToNextDay() {
        prepararConsulta(disponibilidade(23, 0, 23, 59), List.of());

        ConsultaDisponibilidade consulta = service.consultar(quadra, SEGUNDA_FEIRA);

        assertTrue(consulta.horarios().isEmpty());
    }

    @Test
    void dateWithoutRegisteredAvailabilityReturnsEmptySchedule() {
        when(disponibilidadeQuadraRepository
                .findByQuadraAndDiaSemanaAndAtivaTrueOrderByHorarioInicio(
                        quadra, DiaSemana.SEGUNDA))
                .thenReturn(List.of());

        ConsultaDisponibilidade consulta = service.consultar(quadra, SEGUNDA_FEIRA);

        assertTrue(consulta.horarios().isEmpty());
        verify(bloqueioQuadraRepository, never())
                .findByQuadraAndDataAndAtivoTrueOrderByHorarioInicio(quadra, SEGUNDA_FEIRA);
    }

    @Test
    void inactiveCourtDoesNotExposeAvailablePeriods() {
        quadra.setAtiva(false);

        ConsultaDisponibilidade consulta = service.consultar(quadra, SEGUNDA_FEIRA);

        assertFalse(consulta.quadraAtiva());
        assertTrue(consulta.horarios().isEmpty());
        verify(disponibilidadeQuadraRepository, never())
                .findByQuadraAndDiaSemanaAndAtivaTrueOrderByHorarioInicio(
                        quadra, DiaSemana.SEGUNDA);
    }

    @Test
    void periodOutsideWeeklyAvailabilityIsUnavailable() {
        when(disponibilidadeQuadraRepository
                .findByQuadraAndDiaSemanaAndAtivaTrueOrderByHorarioInicio(
                        quadra, DiaSemana.SEGUNDA))
                .thenReturn(List.of(disponibilidade(8, 0, 10, 0)));

        boolean disponivel = service.periodoEstaDisponivel(
                quadra, SEGUNDA_FEIRA, LocalTime.of(10, 0), LocalTime.of(11, 0));

        assertFalse(disponivel);
        verify(bloqueioQuadraRepository, never())
                .findByQuadraAndDataAndAtivoTrueOrderByHorarioInicio(quadra, SEGUNDA_FEIRA);
    }

    @Test
    void periodContainedButNotAlignedWithGeneratedSlotsIsUnavailable() {
        when(disponibilidadeQuadraRepository
                .findByQuadraAndDiaSemanaAndAtivaTrueOrderByHorarioInicio(
                        quadra, DiaSemana.SEGUNDA))
                .thenReturn(List.of(disponibilidade(8, 15, 11, 15)));

        assertFalse(service.periodoEstaDisponivel(
                quadra, SEGUNDA_FEIRA, LocalTime.of(9, 0), LocalTime.of(10, 0)));
        assertFalse(service.periodoEstaDisponivel(
                quadra, SEGUNDA_FEIRA, LocalTime.of(10, 30), LocalTime.of(11, 30)));
        verify(bloqueioQuadraRepository, never())
                .findByQuadraAndDataAndAtivoTrueOrderByHorarioInicio(quadra, SEGUNDA_FEIRA);
    }

    @Test
    void invalidRequestedPeriodIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.periodoEstaDisponivel(
                quadra, SEGUNDA_FEIRA, LocalTime.of(8, 0), LocalTime.of(8, 30)));
    }

    @Test
    void specificDateBlockMakesConflictingPeriodUnavailable() {
        BloqueioQuadra bloqueio = bloqueio(9, 0, 10, 0);
        when(disponibilidadeQuadraRepository
                .findByQuadraAndDiaSemanaAndAtivaTrueOrderByHorarioInicio(
                        quadra, DiaSemana.SEGUNDA))
                .thenReturn(List.of(disponibilidade(8, 0, 10, 0)));
        when(bloqueioQuadraRepository
                .findByQuadraAndDataAndAtivoTrueOrderByHorarioInicio(quadra, SEGUNDA_FEIRA))
                .thenReturn(List.of(bloqueio));
        when(reservaRepository.findByQuadraAndDataUtilizacaoAndStatusOrderByHorarioInicio(
                quadra, SEGUNDA_FEIRA, StatusReserva.APROVADA)).thenReturn(List.of());

        ConsultaDisponibilidade consulta = service.consultar(quadra, SEGUNDA_FEIRA);

        assertTrue(consulta.horarios().get(0).isDisponivel());
        assertFalse(consulta.horarios().get(1).isDisponivel());
        assertEquals(SituacaoHorario.BLOQUEADO, consulta.horarios().get(1).situacao());
    }

    @Test
    void approvedReservationMakesConflictingPeriodUnavailable() {
        Reserva reserva = reservaAprovada(8, 9);
        when(disponibilidadeQuadraRepository
                .findByQuadraAndDiaSemanaAndAtivaTrueOrderByHorarioInicio(
                        quadra, DiaSemana.SEGUNDA))
                .thenReturn(List.of(disponibilidade(8, 0, 10, 0)));
        when(bloqueioQuadraRepository
                .findByQuadraAndDataAndAtivoTrueOrderByHorarioInicio(quadra, SEGUNDA_FEIRA))
                .thenReturn(List.of());
        when(reservaRepository.findByQuadraAndDataUtilizacaoAndStatusOrderByHorarioInicio(
                quadra, SEGUNDA_FEIRA, StatusReserva.APROVADA)).thenReturn(List.of(reserva));

        ConsultaDisponibilidade consulta = service.consultar(quadra, SEGUNDA_FEIRA);

        assertEquals(SituacaoHorario.RESERVADO, consulta.horarios().get(0).situacao());
        assertFalse(consulta.horarios().get(0).isDisponivel());
        assertTrue(consulta.horarios().get(1).isDisponivel());
    }

    @Test
    void partialBlockMakesWholeConflictingPeriodUnavailable() {
        prepararConsulta(
                disponibilidade(8, 0, 10, 0),
                List.of(bloqueio(8, 30, 8, 45)));

        ConsultaDisponibilidade consulta = service.consultar(quadra, SEGUNDA_FEIRA);

        assertEquals(SituacaoHorario.BLOQUEADO, consulta.horarios().get(0).situacao());
        assertEquals(SituacaoHorario.DISPONIVEL, consulta.horarios().get(1).situacao());
    }

    @Test
    void overlappingWeeklyIntervalsDoNotDuplicateEqualPeriods() {
        DisponibilidadeQuadra primeira = disponibilidade(8, 0, 10, 0);
        DisponibilidadeQuadra segunda = disponibilidade(8, 0, 9, 30);
        when(disponibilidadeQuadraRepository
                .findByQuadraAndDiaSemanaAndAtivaTrueOrderByHorarioInicio(
                        quadra, DiaSemana.SEGUNDA))
                .thenReturn(List.of(primeira, segunda));
        when(bloqueioQuadraRepository
                .findByQuadraAndDataAndAtivoTrueOrderByHorarioInicio(quadra, SEGUNDA_FEIRA))
                .thenReturn(List.of());
        when(reservaRepository.findByQuadraAndDataUtilizacaoAndStatusOrderByHorarioInicio(
                quadra, SEGUNDA_FEIRA, StatusReserva.APROVADA)).thenReturn(List.of());

        ConsultaDisponibilidade consulta = service.consultar(quadra, SEGUNDA_FEIRA);

        assertEquals(2, consulta.horarios().size());
        assertEquals("08:00 – 09:00", consulta.horarios().get(0).getPeriodoFormatado());
        assertEquals("09:00 – 10:00", consulta.horarios().get(1).getPeriodoFormatado());
    }

    @Test
    void invalidWeeklyConfigurationIsRejected() {
        DisponibilidadeQuadra invalida = disponibilidade(10, 0, 9, 0);
        when(disponibilidadeQuadraRepository
                .findByQuadraAndDiaSemanaAndAtivaTrueOrderByHorarioInicio(
                        quadra, DiaSemana.SEGUNDA))
                .thenReturn(List.of(invalida));

        assertThrows(IllegalStateException.class,
                () -> service.consultar(quadra, SEGUNDA_FEIRA));
    }

    private void prepararConsulta(
            DisponibilidadeQuadra disponibilidade, List<BloqueioQuadra> bloqueios) {
        when(disponibilidadeQuadraRepository
                .findByQuadraAndDiaSemanaAndAtivaTrueOrderByHorarioInicio(
                        quadra, DiaSemana.SEGUNDA))
                .thenReturn(List.of(disponibilidade));
        when(bloqueioQuadraRepository
                .findByQuadraAndDataAndAtivoTrueOrderByHorarioInicio(quadra, SEGUNDA_FEIRA))
                .thenReturn(bloqueios);
        when(reservaRepository.findByQuadraAndDataUtilizacaoAndStatusOrderByHorarioInicio(
                quadra, SEGUNDA_FEIRA, StatusReserva.APROVADA)).thenReturn(List.of());
    }

    private DisponibilidadeQuadra disponibilidade(
            int horaInicio, int minutoInicio, int horaFim, int minutoFim) {
        DisponibilidadeQuadra disponibilidade = new DisponibilidadeQuadra();
        disponibilidade.setDiaSemana(DiaSemana.SEGUNDA);
        disponibilidade.setHorarioInicio(LocalTime.of(horaInicio, minutoInicio));
        disponibilidade.setHorarioFim(LocalTime.of(horaFim, minutoFim));
        disponibilidade.setAtiva(true);
        disponibilidade.setQuadra(quadra);
        return disponibilidade;
    }

    private BloqueioQuadra bloqueio(
            int horaInicio, int minutoInicio, int horaFim, int minutoFim) {
        BloqueioQuadra bloqueio = new BloqueioQuadra();
        bloqueio.setData(SEGUNDA_FEIRA);
        bloqueio.setHorarioInicio(LocalTime.of(horaInicio, minutoInicio));
        bloqueio.setHorarioFim(LocalTime.of(horaFim, minutoFim));
        bloqueio.setMotivo("Evento institucional");
        bloqueio.setAtivo(true);
        bloqueio.setQuadra(quadra);
        return bloqueio;
    }

    private Reserva reservaAprovada(int horaInicio, int horaFim) {
        Reserva reserva = new Reserva();
        reserva.setQuadra(quadra);
        reserva.setDataUtilizacao(SEGUNDA_FEIRA);
        reserva.setHorarioInicio(LocalTime.of(horaInicio, 0));
        reserva.setHorarioFim(LocalTime.of(horaFim, 0));
        reserva.setFinalidade("Atividade esportiva");
        reserva.setStatus(StatusReserva.APROVADA);
        return reserva;
    }
}
