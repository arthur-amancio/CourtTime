package br.com.courttime.service;

import br.com.courttime.entity.BloqueioQuadra;
import br.com.courttime.entity.BloqueioSemanalQuadra;
import br.com.courttime.entity.DiaSemana;
import br.com.courttime.entity.DisponibilidadeQuadra;
import br.com.courttime.entity.Quadra;
import br.com.courttime.entity.Reserva;
import br.com.courttime.entity.StatusReserva;
import br.com.courttime.repository.BloqueioQuadraRepository;
import br.com.courttime.repository.BloqueioSemanalQuadraRepository;
import br.com.courttime.repository.DisponibilidadeQuadraRepository;
import br.com.courttime.repository.ReservaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/** Calcula a disponibilidade diária usada tanto pela tela quanto pelas decisões de reserva. */
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
    private final BloqueioSemanalQuadraRepository bloqueioSemanalRepository;

    public CalendarioDisponibilidadeService(
            DisponibilidadeQuadraRepository disponibilidadeQuadraRepository,
            BloqueioQuadraRepository bloqueioQuadraRepository,
            ReservaRepository reservaRepository,
            BloqueioSemanalQuadraRepository bloqueioSemanalRepository) {
        this.disponibilidadeQuadraRepository = disponibilidadeQuadraRepository;
        this.bloqueioQuadraRepository = bloqueioQuadraRepository;
        this.reservaRepository = reservaRepository;
        this.bloqueioSemanalRepository = bloqueioSemanalRepository;
    }

    public LocalDate obterDataAtual() {
        return LocalDate.now(FUSO_HORARIO);
    }

    public LocalDateTime obterDataHoraAtual() {
        return LocalDateTime.now(FUSO_HORARIO);
    }

    public LocalDate obterDataMinimaParaSolicitacao() {
        return obterDataAtual().plusDays(DIAS_MINIMOS_DE_ANTECEDENCIA);
    }

    /** Divide a grade em horas e marca bloqueios antes de apresentar períodos disponíveis. */
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

        List<PeriodoBloqueado> bloqueios = obterBloqueios(quadra, data);
        List<Reserva> reservasAprovadas = obterReservasAprovadas(quadra, data);
        reservasAprovadas.forEach(this::validarReserva);

        TreeMap<LocalTime, HorarioDisponibilidade> horarios = new TreeMap<>();
        for (DisponibilidadeQuadra disponibilidade : disponibilidades) {
            adicionarHorarios(disponibilidade, bloqueios, reservasAprovadas, horarios);
        }

        return new ConsultaDisponibilidade(data, true, new ArrayList<>(horarios.values()));
    }

    /**
     * Dias da semana com ao menos um horario semanal ativo. Usado apenas para
     * indicar, no calendario, quais dias tem funcionamento cadastrado; nao
     * informa vagas nem substitui a consulta do dia.
     */
    @Transactional(readOnly = true)
    public Set<DiaSemana> obterDiasComFuncionamento(Quadra quadra) {
        Objects.requireNonNull(quadra, "A quadra é obrigatória");
        if (!quadra.isAtiva()) {
            return Set.of();
        }
        EnumSet<DiaSemana> dias = EnumSet.noneOf(DiaSemana.class);
        disponibilidadeQuadraRepository.findByQuadraAndAtivaTrue(quadra)
                .forEach(disponibilidade -> dias.add(disponibilidade.getDiaSemana()));
        return Set.copyOf(dias);
    }

    /**
     * Revalida grade, bloqueios e reservas aprovadas no servidor. A aprovação chama
     * este método dentro da transação que já detém a trava da quadra.
     */
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
                .anyMatch(disponibilidade -> periodoAlinhadoComGrade(
                        disponibilidade.getHorarioInicio(), disponibilidade.getHorarioFim(),
                        horarioInicio, horarioFim));

        if (!dentroDoFuncionamento) {
            return false;
        }

        List<PeriodoBloqueado> bloqueios = obterBloqueios(quadra, data);
        boolean semBloqueio = bloqueios.stream()
                .noneMatch(bloqueio -> periodosSobrepostos(
                        horarioInicio,
                        horarioFim,
                        bloqueio.inicio(),
                        bloqueio.fim()));

        if (!semBloqueio) {
            return false;
        }

        List<Reserva> reservasAprovadas = obterReservasAprovadas(quadra, data);
        reservasAprovadas.forEach(this::validarReserva);
        return reservasAprovadas.stream()
                .noneMatch(reserva -> periodosSobrepostos(
                        horarioInicio,
                        horarioFim,
                        reserva.getHorarioInicio(),
                        reserva.getHorarioFim()));
    }

    static boolean periodoAlinhadoComGrade(
            LocalTime inicioFuncionamento, LocalTime fimFuncionamento,
            LocalTime inicioReserva, LocalTime fimReserva) {
        if (inicioFuncionamento == null || fimFuncionamento == null
                || inicioReserva == null || fimReserva == null
                || inicioReserva.isBefore(inicioFuncionamento)
                || fimReserva.isAfter(fimFuncionamento)
                || !Duration.between(inicioReserva, fimReserva).equals(DURACAO_AGENDAMENTO)) {
            return false;
        }
        Duration deslocamentoDesdeAbertura = Duration.between(
                inicioFuncionamento, inicioReserva);
        return deslocamentoDesdeAbertura.toNanos() % DURACAO_AGENDAMENTO.toNanos() == 0;
    }

    /** Une indisponibilidades pontuais e semanais somente para a data consultada. */
    private List<PeriodoBloqueado> obterBloqueios(Quadra quadra, LocalDate data) {
        List<PeriodoBloqueado> periodos = new ArrayList<>();
        for (BloqueioQuadra bloqueio : bloqueioQuadraRepository
                .findByQuadraAndDataAndAtivoTrueOrderByHorarioInicio(quadra, data)) {
            validarBloqueio(bloqueio);
            periodos.add(new PeriodoBloqueado(bloqueio.getHorarioInicio(), bloqueio.getHorarioFim()));
        }
        for (BloqueioSemanalQuadra regra : bloqueioSemanalRepository.listarAtivosNaData(
                quadra, DiaSemana.from(data.getDayOfWeek()), data)) {
            if (!regra.isIntervaloValido() || !regra.isVigenciaValida()) {
                throw new IllegalStateException("Existe um bloqueio semanal inválido cadastrado");
            }
            periodos.add(new PeriodoBloqueado(regra.getHorarioInicio(), regra.getHorarioFim()));
        }
        return periodos;
    }

    private record PeriodoBloqueado(LocalTime inicio, LocalTime fim) { }

    private List<DisponibilidadeQuadra> obterDisponibilidadesDoDia(Quadra quadra, LocalDate data) {
        DiaSemana diaSemana = DiaSemana.from(data.getDayOfWeek());
        return disponibilidadeQuadraRepository
                .findByQuadraAndDiaSemanaAndAtivaTrueOrderByHorarioInicio(quadra, diaSemana);
    }

    private void adicionarHorarios(
            DisponibilidadeQuadra disponibilidade,
            List<PeriodoBloqueado> bloqueios,
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
            List<PeriodoBloqueado> bloqueios,
            List<Reserva> reservasAprovadas) {
        boolean bloqueado = bloqueios.stream()
                .anyMatch(bloqueio -> periodosSobrepostos(
                        horarioInicio,
                        horarioFim,
                        bloqueio.inicio(),
                        bloqueio.fim()));
        if (bloqueado) {
            return SituacaoHorario.BLOQUEADO;
        }

        boolean reservado = reservasAprovadas.stream()
                .anyMatch(reserva -> periodosSobrepostos(
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

    /** Intervalos adjacentes não conflitam: o fim de cada período é exclusivo. */
    static boolean periodosSobrepostos(
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
