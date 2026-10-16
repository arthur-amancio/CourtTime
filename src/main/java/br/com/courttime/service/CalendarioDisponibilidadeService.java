package br.com.courttime.service;

import br.com.courttime.entity.BloqueioQuadra;
import br.com.courttime.entity.DiaSemana;
import br.com.courttime.entity.DisponibilidadeQuadra;
import br.com.courttime.entity.Quadra;
import br.com.courttime.repository.BloqueioQuadraRepository;
import br.com.courttime.repository.DisponibilidadeQuadraRepository;
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
    private static final Duration DURACAO_AGENDAMENTO = Duration.ofHours(1);

    private final DisponibilidadeQuadraRepository disponibilidadeQuadraRepository;
    private final BloqueioQuadraRepository bloqueioQuadraRepository;

    public CalendarioDisponibilidadeService(
            DisponibilidadeQuadraRepository disponibilidadeQuadraRepository,
            BloqueioQuadraRepository bloqueioQuadraRepository) {
        this.disponibilidadeQuadraRepository = disponibilidadeQuadraRepository;
        this.bloqueioQuadraRepository = bloqueioQuadraRepository;
    }

    public LocalDate obterDataAtual() {
        return LocalDate.now(FUSO_HORARIO);
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

        TreeMap<LocalTime, HorarioDisponibilidade> horarios = new TreeMap<>();
        for (DisponibilidadeQuadra disponibilidade : disponibilidades) {
            adicionarHorarios(disponibilidade, bloqueios, horarios);
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

        return bloqueioQuadraRepository
                .findByQuadraAndDataAndAtivoTrueOrderByHorarioInicio(quadra, data)
                .stream()
                .peek(this::validarBloqueio)
                .noneMatch(bloqueio -> periodosConflitam(
                        horarioInicio,
                        horarioFim,
                        bloqueio.getHorarioInicio(),
                        bloqueio.getHorarioFim()));
    }

    private List<DisponibilidadeQuadra> obterDisponibilidadesDoDia(Quadra quadra, LocalDate data) {
        DiaSemana diaSemana = DiaSemana.from(data.getDayOfWeek());
        return disponibilidadeQuadraRepository
                .findByQuadraAndDiaSemanaAndAtivaTrueOrderByHorarioInicio(quadra, diaSemana);
    }

    private void adicionarHorarios(
            DisponibilidadeQuadra disponibilidade,
            List<BloqueioQuadra> bloqueios,
            TreeMap<LocalTime, HorarioDisponibilidade> horarios) {
        LocalTime inicio = disponibilidade.getHorarioInicio();
        while (Duration.between(inicio, disponibilidade.getHorarioFim())
                .compareTo(DURACAO_AGENDAMENTO) >= 0) {
            LocalTime horarioInicio = inicio;
            LocalTime horarioFim = horarioInicio.plus(DURACAO_AGENDAMENTO);
            SituacaoHorario situacao = bloqueios.stream()
                    .anyMatch(bloqueio -> periodosConflitam(
                            horarioInicio,
                            horarioFim,
                            bloqueio.getHorarioInicio(),
                            bloqueio.getHorarioFim()))
                    ? SituacaoHorario.BLOQUEADO
                    : SituacaoHorario.DISPONIVEL;

            HorarioDisponibilidade horario = new HorarioDisponibilidade(
                    horarioInicio, horarioFim, situacao);
            horarios.merge(horarioInicio, horario, this::manterSituacaoMaisRestritiva);
            inicio = horarioFim;
        }
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
}
