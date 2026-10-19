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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;

@Service
public class CalendarioDisponibilidadeService {

    public static final ZoneId FUSO_HORARIO = ZoneId.of("America/Sao_Paulo");

    /**
     * O formulário oficial da ETEC exige a solicitação com pelo menos um dia de
     * antecedência. A contagem é feita em dias do calendário local, e não em
     * horas corridas: uma solicitação feita hoje vale a partir de amanhã.
     */
    public static final int DIAS_MINIMOS_DE_ANTECEDENCIA = 1;

    private static final Duration DURACAO_AGENDAMENTO = Duration.ofHours(1);

    private final DisponibilidadeQuadraRepository disponibilidadeQuadraRepository;
    private final BloqueioQuadraRepository bloqueioQuadraRepository;
    private final ReservaRepository reservaRepository;

    public CalendarioDisponibilidadeService(
            DisponibilidadeQuadraRepository disponibilidadeQuadraRepository,
            BloqueioQuadraRepository bloqueioQuadraRepository,
            ReservaRepository reservaRepository) {
        this.disponibilidadeQuadraRepository = disponibilidadeQuadraRepository;
        this.bloqueioQuadraRepository = bloqueioQuadraRepository;
        this.reservaRepository = reservaRepository;
    }

    public LocalDate obterDataAtual() {
        return LocalDate.now(FUSO_HORARIO);
    }

    public LocalDate obterDataMinimaParaSolicitacao() {
        return obterDataAtual().plusDays(DIAS_MINIMOS_DE_ANTECEDENCIA);
    }

    @Transactional(readOnly = true)
    public ConsultaDisponibilidade consultar(Quadra quadra, LocalDate data) {
        Objects.requireNonNull(quadra, "A quadra é obrigatória");
        Objects.requireNonNull(data, "A data é obrigatória");

        if (!quadra.isAtiva()) {
            return new ConsultaDisponibilidade(data, false, List.of());
        }

        List<DisponibilidadeQuadra> disponibilidades = obterDisponibilidadesDoDia(quadra, data);
        if (disponibilidades.isEmpty()) {
            return new ConsultaDisponibilidade(data, true, List.of());
        }
        disponibilidades.forEach(this::validarDisponibilidade);

        List<BloqueioQuadra> bloqueios = bloqueioQuadraRepository
                .findByQuadraAndDataAndAtivoTrueOrderByHorarioInicio(quadra, data);
        bloqueios.forEach(this::validarBloqueio);
        List<Reserva> reservasAprovadas = obterReservasAprovadas(quadra, data);
        reservasAprovadas.forEach(this::validarReserva);

        TreeMap<LocalTime, HorarioDisponibilidade> horarios = new TreeMap<>();
        for (DisponibilidadeQuadra disponibilidade : disponibilidades) {
            adicionarHorarios(disponibilidade, bloqueios, reservasAprovadas, horarios);
        }

        return new ConsultaDisponibilidade(data, true, new ArrayList<>(horarios.values()));
    }

    @Transactional(readOnly = true)
    public boolean periodoEstaDisponivel(
            Quadra quadra, LocalDate data, LocalTime horarioInicio, LocalTime horarioFim) {
        Objects.requireNonNull(quadra, "A quadra é obrigatória");
        Objects.requireNonNull(data, "A data é obrigatória");
        validarPeriodoSolicitado(horarioInicio, horarioFim);

        if (!quadra.isAtiva()) {
            return false;
        }

        List<DisponibilidadeQuadra> disponibilidades = obterDisponibilidadesDoDia(quadra, data);
        disponibilidades.forEach(this::validarDisponibilidade);
        boolean dentroDoFuncionamento = disponibilidades.stream()
                .anyMatch(disponibilidade -> !horarioInicio.isBefore(disponibilidade.getHorarioInicio())
                        && !horarioFim.isAfter(disponibilidade.getHorarioFim()));

        if (!dentroDoFuncionamento) {
            return false;
        }

        List<BloqueioQuadra> bloqueios = bloqueioQuadraRepository
                .findByQuadraAndDataAndAtivoTrueOrderByHorarioInicio(quadra, data);
        bloqueios.forEach(this::validarBloqueio);
        boolean semBloqueio = bloqueios.stream()
                .noneMatch(bloqueio -> periodosConflitam(
                        horarioInicio,
                        horarioFim,
                        bloqueio.getHorarioInicio(),
                        bloqueio.getHorarioFim()));

        if (!semBloqueio) {
            return false;
        }

        List<Reserva> reservasAprovadas = obterReservasAprovadas(quadra, data);
        reservasAprovadas.forEach(this::validarReserva);
        return reservasAprovadas.stream()
                .noneMatch(reserva -> periodosConflitam(
                        horarioInicio,
                        horarioFim,
                        reserva.getHorarioInicio(),
                        reserva.getHorarioFim()));
    }

    private List<DisponibilidadeQuadra> obterDisponibilidadesDoDia(Quadra quadra, LocalDate data) {
        DiaSemana diaSemana = DiaSemana.from(data.getDayOfWeek());
        return disponibilidadeQuadraRepository
                .findByQuadraAndDiaSemanaAndAtivaTrueOrderByHorarioInicio(quadra, diaSemana);
    }

    private void adicionarHorarios(
            DisponibilidadeQuadra disponibilidade,
            List<BloqueioQuadra> bloqueios,
            List<Reserva> reservasAprovadas,
            TreeMap<LocalTime, HorarioDisponibilidade> horarios) {
        LocalTime inicio = disponibilidade.getHorarioInicio();
        while (Duration.between(inicio, disponibilidade.getHorarioFim())
                .compareTo(DURACAO_AGENDAMENTO) >= 0) {
            LocalTime horarioInicio = inicio;
            LocalTime horarioFim = horarioInicio.plus(DURACAO_AGENDAMENTO);
            SituacaoHorario situacao = obterSituacao(
                    horarioInicio, horarioFim, bloqueios, reservasAprovadas);

            HorarioDisponibilidade horario = new HorarioDisponibilidade(
                    horarioInicio, horarioFim, situacao);
            horarios.merge(horarioInicio, horario, this::manterSituacaoMaisRestritiva);
            inicio = horarioFim;
        }
    }

    private SituacaoHorario obterSituacao(
            LocalTime horarioInicio,
            LocalTime horarioFim,
            List<BloqueioQuadra> bloqueios,
            List<Reserva> reservasAprovadas) {
        boolean bloqueado = bloqueios.stream()
                .anyMatch(bloqueio -> periodosConflitam(
                        horarioInicio,
                        horarioFim,
                        bloqueio.getHorarioInicio(),
                        bloqueio.getHorarioFim()));
        if (bloqueado) {
            return SituacaoHorario.BLOQUEADO;
        }

        boolean reservado = reservasAprovadas.stream()
                .anyMatch(reserva -> periodosConflitam(
                        horarioInicio,
                        horarioFim,
                        reserva.getHorarioInicio(),
                        reserva.getHorarioFim()));
        return reservado ? SituacaoHorario.RESERVADO : SituacaoHorario.DISPONIVEL;
    }

    private List<Reserva> obterReservasAprovadas(Quadra quadra, LocalDate data) {
        return reservaRepository.findByQuadraAndDataUtilizacaoAndStatusOrderByHorarioInicio(
                quadra, data, StatusReserva.APROVADA);
    }

    private HorarioDisponibilidade manterSituacaoMaisRestritiva(
            HorarioDisponibilidade atual, HorarioDisponibilidade novoHorario) {
        return atual.isDisponivel() ? novoHorario : atual;
    }

    private boolean periodosConflitam(
            LocalTime inicioA, LocalTime fimA, LocalTime inicioB, LocalTime fimB) {
        return inicioA.isBefore(fimB) && inicioB.isBefore(fimA);
    }

    private void validarPeriodoSolicitado(LocalTime horarioInicio, LocalTime horarioFim) {
        if (horarioInicio == null || horarioFim == null
                || !horarioInicio.isBefore(horarioFim)
                || !Duration.between(horarioInicio, horarioFim).equals(DURACAO_AGENDAMENTO)) {
            throw new IllegalArgumentException("O período solicitado deve ter duração de uma hora");
        }
    }

    private void validarDisponibilidade(DisponibilidadeQuadra disponibilidade) {
        if (!disponibilidade.isIntervaloValido()) {
            throw new IllegalStateException(
                    "Existe um horário semanal inválido cadastrado para a quadra");
        }
    }

    private void validarBloqueio(BloqueioQuadra bloqueio) {
        if (!bloqueio.isIntervaloValido()) {
            throw new IllegalStateException("Existe um bloqueio inválido cadastrado para a quadra");
        }
    }

    private void validarReserva(Reserva reserva) {
        if (!reserva.isIntervaloValido()) {
            throw new IllegalStateException("Existe uma reserva inválida cadastrada para a quadra");
        }
    }
}
