package br.com.courttime.service;

import br.com.courttime.entity.BloqueioQuadra;
import br.com.courttime.entity.BloqueioSemanalQuadra;
import br.com.courttime.entity.DiaSemana;
import br.com.courttime.entity.DisponibilidadeQuadra;
import br.com.courttime.entity.Perfil;
import br.com.courttime.entity.Quadra;
import br.com.courttime.entity.Reserva;
import br.com.courttime.entity.StatusReserva;
import br.com.courttime.entity.Usuario;
import br.com.courttime.repository.BloqueioQuadraRepository;
import br.com.courttime.repository.BloqueioSemanalQuadraRepository;
import br.com.courttime.repository.DisponibilidadeQuadraRepository;
import br.com.courttime.repository.QuadraRepository;
import br.com.courttime.repository.ReservaRepository;
import br.com.courttime.security.Permissao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Mantém grade e indisponibilidades sob a mesma trava usada para aprovar reservas. */
@Service
public class AdministracaoQuadraService {

    private static final int TAMANHO_MAXIMO_MOTIVO = 200;

    private final QuadraService quadraService;
    private final QuadraRepository quadraRepository;
    private final DisponibilidadeQuadraRepository disponibilidadeRepository;
    private final BloqueioQuadraRepository bloqueioRepository;
    private final BloqueioSemanalQuadraRepository bloqueioSemanalRepository;
    private final ReservaRepository reservaRepository;
    private final CalendarioDisponibilidadeService calendarioService;

    public AdministracaoQuadraService(
            QuadraService quadraService,
            QuadraRepository quadraRepository,
            DisponibilidadeQuadraRepository disponibilidadeRepository,
            BloqueioQuadraRepository bloqueioRepository,
            ReservaRepository reservaRepository,
            CalendarioDisponibilidadeService calendarioService,
            BloqueioSemanalQuadraRepository bloqueioSemanalRepository) {
        this.quadraService = quadraService;
        this.quadraRepository = quadraRepository;
        this.disponibilidadeRepository = disponibilidadeRepository;
        this.bloqueioRepository = bloqueioRepository;
        this.reservaRepository = reservaRepository;
        this.calendarioService = calendarioService;
        this.bloqueioSemanalRepository = bloqueioSemanalRepository;
    }

    @Transactional(readOnly = true)
    public PainelAdministrativo carregarPainel(Usuario administrador) {
        Permissao.exigir(administrador, Perfil.ADMINISTRADOR);
        Quadra quadra = quadraService.obterQuadra();
        LocalDate hoje = calendarioService.obterDataAtual();
        return new PainelAdministrativo(
                quadra,
                obterHorariosOrdenados(quadra),
                bloqueioRepository
                        .findByQuadraAndDataGreaterThanEqualOrderByDataAscHorarioInicioAsc(
                                quadra, hoje)
                        .stream()
                        .filter(BloqueioQuadra::isAtivo)
                        .toList(),
                reservaRepository.countByRascunhoFalse(),
                reservaRepository.countByStatusAndRascunhoFalse(StatusReserva.PENDENTE_DOCUMENTACAO),
                reservaRepository.countByStatus(StatusReserva.PENDENTE_ANALISE),
                reservaRepository.countByStatus(StatusReserva.APROVADA),
                reservaRepository.countByStatus(StatusReserva.REJEITADA),
                reservaRepository.countByStatus(StatusReserva.CANCELADA));
    }

    @Transactional(readOnly = true)
    public List<DisponibilidadeQuadra> listarHorarios(Usuario administrador) {
        Permissao.exigir(administrador, Perfil.ADMINISTRADOR);
        Quadra quadra = quadraService.obterQuadra();
        return obterHorariosOrdenados(quadra);
    }

    private List<DisponibilidadeQuadra> obterHorariosOrdenados(Quadra quadra) {
        // EnumType.STRING e ordenado alfabeticamente no PostgreSQL; a ordem
        // cronologica deve seguir a declaracao de DiaSemana (segunda a domingo).
        return disponibilidadeRepository.findByQuadraOrderByDiaSemanaAscHorarioInicioAsc(quadra)
                .stream()
                .sorted(Comparator.comparing(DisponibilidadeQuadra::getDiaSemana)
                        .thenComparing(DisponibilidadeQuadra::getHorarioInicio))
                .toList();
    }

    @Transactional(readOnly = true)
    public DisponibilidadeQuadra obterHorario(Long id, Usuario administrador) {
        Permissao.exigir(administrador, Perfil.ADMINISTRADOR);
        return disponibilidadeRepository.findByIdComQuadra(id)
                .filter(horario -> Objects.equals(
                        horario.getQuadra().getId(), quadraService.obterQuadra().getId()))
                .orElseThrow(() -> new IllegalArgumentException("Horário não encontrado"));
    }

    @Transactional
    public DisponibilidadeQuadra cadastrarHorario(
            Usuario administrador,
            DiaSemana diaSemana,
            LocalTime horarioInicio,
            LocalTime horarioFim) {
        Permissao.exigir(administrador, Perfil.ADMINISTRADOR);
        validarPeriodo(horarioInicio, horarioFim);
        validarDiaSemana(diaSemana);

        Quadra quadra = bloquearQuadra();
        List<DisponibilidadeQuadra> horariosDoDia = disponibilidadeRepository
                .findByQuadraAndDiaSemanaOrderByHorarioInicioAsc(quadra, diaSemana);
        validarDuplicidadeESobreposicao(
                horariosDoDia, null, horarioInicio, horarioFim, true);

        DisponibilidadeQuadra horario = new DisponibilidadeQuadra();
        horario.setQuadra(quadra);
        horario.setDiaSemana(diaSemana);
        horario.setHorarioInicio(horarioInicio);
        horario.setHorarioFim(horarioFim);
        horario.setAtiva(true);
        return disponibilidadeRepository.save(horario);
    }

    @Transactional
    public DisponibilidadeQuadra editarHorario(
            Long id,
            Usuario administrador,
            DiaSemana diaSemana,
            LocalTime horarioInicio,
            LocalTime horarioFim) {
        Permissao.exigir(administrador, Perfil.ADMINISTRADOR);
        validarPeriodo(horarioInicio, horarioFim);
        validarDiaSemana(diaSemana);

        Quadra quadra = bloquearQuadra();
        DisponibilidadeQuadra horario = obterHorarioDaQuadra(id, quadra);
        List<DisponibilidadeQuadra> horariosDoDia = disponibilidadeRepository
                .findByQuadraAndDiaSemanaOrderByHorarioInicioAsc(quadra, diaSemana);
        validarDuplicidadeESobreposicao(
                horariosDoDia, id, horarioInicio, horarioFim, horario.isAtiva());

        horario.setDiaSemana(diaSemana);
        horario.setHorarioInicio(horarioInicio);
        horario.setHorarioFim(horarioFim);
        validarReservasAprovadasCobertas(quadra);
        return disponibilidadeRepository.save(horario);
    }

    @Transactional
    public DisponibilidadeQuadra alternarHorario(Long id, Usuario administrador) {
        Permissao.exigir(administrador, Perfil.ADMINISTRADOR);
        Quadra quadra = bloquearQuadra();
        DisponibilidadeQuadra horario = obterHorarioDaQuadra(id, quadra);

        if (!horario.isAtiva()) {
            List<DisponibilidadeQuadra> horariosDoDia = disponibilidadeRepository
                    .findByQuadraAndDiaSemanaOrderByHorarioInicioAsc(
                            quadra, horario.getDiaSemana());
            validarDuplicidadeESobreposicao(
                    horariosDoDia,
                    id,
                    horario.getHorarioInicio(),
                    horario.getHorarioFim(),
                    true);
            horario.setAtiva(true);
        } else {
            horario.setAtiva(false);
            validarReservasAprovadasCobertas(quadra);
        }
        return disponibilidadeRepository.save(horario);
    }

    @Transactional(readOnly = true)
    public List<BloqueioQuadra> listarBloqueiosFuturos(Usuario administrador) {
        Permissao.exigir(administrador, Perfil.ADMINISTRADOR);
        Quadra quadra = quadraService.obterQuadra();
        return bloqueioRepository.findByQuadraAndDataGreaterThanEqualOrderByDataAscHorarioInicioAsc(
                quadra, calendarioService.obterDataAtual());
    }

    /**
     * Trava a quadra e recusa sobreposição pontual ou reserva aprovada. O cálculo diário
     * combina este registro com regras semanais sem modificar os bloqueios existentes.
     */
    @Transactional
    public BloqueioQuadra cadastrarBloqueio(
            Usuario administrador,
            LocalDate data,
            LocalTime horarioInicio,
            LocalTime horarioFim,
            String motivo) {
        Permissao.exigir(administrador, Perfil.ADMINISTRADOR);
        if (data == null) {
            throw new IllegalArgumentException("A data do bloqueio é obrigatória");
        }
        if (data.isBefore(calendarioService.obterDataAtual())) {
            throw new IllegalArgumentException("A data do bloqueio não pode estar no passado");
        }
        validarPeriodo(horarioInicio, horarioFim);
        String motivoNormalizado = validarMotivo(motivo);

        Quadra quadra = bloquearQuadra();
        List<BloqueioQuadra> bloqueiosDaData = bloqueioRepository
                .findByQuadraAndDataOrderByHorarioInicio(quadra, data);
        boolean existeConflito = bloqueiosDaData.stream()
                .filter(BloqueioQuadra::isAtivo)
                .anyMatch(bloqueio -> CalendarioDisponibilidadeService.periodosSobrepostos(
                        horarioInicio,
                        horarioFim,
                        bloqueio.getHorarioInicio(),
                        bloqueio.getHorarioFim()));
        if (existeConflito) {
            throw new IllegalStateException("Já existe um bloqueio ativo nesse período");
        }

        boolean atingeReservaAprovada = reservaRepository
                .findByQuadraAndDataUtilizacaoAndStatusOrderByHorarioInicio(
                        quadra, data, StatusReserva.APROVADA)
                .stream()
                .anyMatch(reserva -> CalendarioDisponibilidadeService.periodosSobrepostos(
                        horarioInicio,
                        horarioFim,
                        reserva.getHorarioInicio(),
                        reserva.getHorarioFim()));
        if (atingeReservaAprovada) {
            throw new IllegalStateException(
                    "O bloqueio atinge uma reserva aprovada e não pode ser cadastrado");
        }

        BloqueioQuadra bloqueio = new BloqueioQuadra();
        bloqueio.setQuadra(quadra);
        bloqueio.setData(data);
        bloqueio.setHorarioInicio(horarioInicio);
        bloqueio.setHorarioFim(horarioFim);
        bloqueio.setMotivo(motivoNormalizado);
        bloqueio.setAtivo(true);
        return bloqueioRepository.save(bloqueio);
    }

    @Transactional
    public BloqueioQuadra desativarBloqueio(Long id, Usuario administrador) {
        Permissao.exigir(administrador, Perfil.ADMINISTRADOR);
        Quadra quadra = bloquearQuadra();
        BloqueioQuadra bloqueio = bloqueioRepository.findById(id)
                .filter(item -> Objects.equals(item.getQuadra().getId(), quadra.getId()))
                .orElseThrow(() -> new IllegalArgumentException("Bloqueio não encontrado"));
        bloqueio.setAtivo(false);
        return bloqueioRepository.save(bloqueio);
    }

    /** Consulta também regras vencidas/inativas; desativação não apaga o cadastro. */
    @Transactional(readOnly = true)
    public List<BloqueioSemanalQuadra> listarBloqueiosSemanais(Usuario administrador) {
        Permissao.exigir(administrador, Perfil.ADMINISTRADOR);
        return bloqueioSemanalRepository.findByQuadraOrderByVigenciaInicioDescHorarioInicioAsc(
                quadraService.obterQuadra());
    }

    /**
     * Grava uma única regra. A trava da quadra serializa esta decisão com a
     * aprovação: uma reserva aprovada nunca é atingida por uma regra nova.
     * Solicitações ainda pendentes serão revalidadas quando o administrador aprovar.
     */
    @Transactional
    public BloqueioSemanalQuadra cadastrarBloqueioSemanal(
            Usuario administrador, DiaSemana diaSemana, LocalTime horarioInicio,
            LocalTime horarioFim, LocalDate vigenciaInicio, LocalDate vigenciaFim, String motivo) {
        Permissao.exigir(administrador, Perfil.ADMINISTRADOR);
        validarDiaSemana(diaSemana);
        validarPeriodo(horarioInicio, horarioFim);
        validarVigencia(diaSemana, vigenciaInicio, vigenciaFim);
        String motivoNormalizado = validarMotivo(motivo);

        Quadra quadra = bloquearQuadra();
        BloqueioSemanalQuadra regra = new BloqueioSemanalQuadra();
        regra.setQuadra(quadra);
        regra.setDiaSemana(diaSemana);
        regra.setHorarioInicio(horarioInicio);
        regra.setHorarioFim(horarioFim);
        regra.setVigenciaInicio(vigenciaInicio);
        regra.setVigenciaFim(vigenciaFim);
        regra.setMotivo(motivoNormalizado);
        validarReservasNaoAtingidas(quadra, regra);
        return bloqueioSemanalRepository.save(regra);
    }

    /** Desativa a regra inteira, sem alterar reservas, bloqueios pontuais ou histórico. */
    @Transactional
    public void desativarBloqueioSemanal(Long id, Usuario administrador) {
        Permissao.exigir(administrador, Perfil.ADMINISTRADOR);
        if (id == null) {
            throw new IllegalArgumentException("O bloqueio semanal é obrigatório");
        }
        Quadra quadra = bloquearQuadra();
        BloqueioSemanalQuadra regra = bloqueioSemanalRepository.findById(id)
                .filter(item -> Objects.equals(item.getQuadra().getId(), quadra.getId()))
                .orElseThrow(() -> new IllegalArgumentException("Bloqueio semanal não encontrado"));
        regra.setAtivo(false);
        bloqueioSemanalRepository.save(regra);
    }

    private void validarVigencia(DiaSemana dia, LocalDate inicio, LocalDate fim) {
        if (inicio == null || fim == null || fim.isBefore(inicio)) {
            throw new IllegalArgumentException("Informe uma vigência com início e fim em ordem");
        }
        LocalDate hoje = calendarioService.obterDataAtual();
        LocalDate inicioEfetivo = inicio.isBefore(hoje) ? hoje : inicio;
        LocalDate primeiraOcorrencia = inicioEfetivo.with(
                TemporalAdjusters.nextOrSame(DayOfWeek.of(dia.ordinal() + 1)));
        if (primeiraOcorrencia.isAfter(fim)) {
            throw new IllegalArgumentException("A vigência deve conter uma ocorrência atual ou futura");
        }
    }

    private void validarReservasNaoAtingidas(Quadra quadra, BloqueioSemanalQuadra regra) {
        boolean atingeAprovada = reservaRepository
                .findByQuadraAndStatusAndDataUtilizacaoGreaterThanEqualOrderByDataUtilizacaoAscHorarioInicioAsc(
                        quadra, StatusReserva.APROVADA, calendarioService.obterDataAtual())
                .stream()
                .filter(reserva -> regra.ocorreEm(reserva.getDataUtilizacao()))
                .anyMatch(reserva -> CalendarioDisponibilidadeService.periodosSobrepostos(
                        regra.getHorarioInicio(), regra.getHorarioFim(),
                        reserva.getHorarioInicio(), reserva.getHorarioFim()));
        if (atingeAprovada) {
            throw new IllegalStateException("O bloqueio semanal atinge uma reserva aprovada");
        }
    }

    private Quadra bloquearQuadra() {
        Quadra quadra = quadraService.obterQuadra();
        return quadraRepository.findByIdForUpdate(quadra.getId())
                .orElseThrow(() -> new IllegalStateException("Quadra não encontrada"));
    }

    private DisponibilidadeQuadra obterHorarioDaQuadra(Long id, Quadra quadra) {
        if (id == null) {
            throw new IllegalArgumentException("O horário é obrigatório");
        }
        return disponibilidadeRepository.findByIdComQuadra(id)
                .filter(horario -> Objects.equals(horario.getQuadra().getId(), quadra.getId()))
                .orElseThrow(() -> new IllegalArgumentException("Horário não encontrado"));
    }

    private void validarDuplicidadeESobreposicao(
            List<DisponibilidadeQuadra> horarios,
            Long idIgnorado,
            LocalTime inicio,
            LocalTime fim,
            boolean ficaraAtivo) {
        for (DisponibilidadeQuadra existente : horarios) {
            if (Objects.equals(existente.getId(), idIgnorado)) {
                continue;
            }
            if (existente.getHorarioInicio().equals(inicio)
                    && existente.getHorarioFim().equals(fim)) {
                throw new IllegalStateException("Já existe um horário igual para esse dia");
            }
            if (ficaraAtivo && existente.isAtiva() && CalendarioDisponibilidadeService.periodosSobrepostos(
                    inicio, fim, existente.getHorarioInicio(), existente.getHorarioFim())) {
                throw new IllegalStateException("O período se sobrepõe a outro horário ativo");
            }
        }
    }

    private void validarReservasAprovadasCobertas(Quadra quadra) {
        List<DisponibilidadeQuadra> ativos = disponibilidadeRepository
                .findByQuadraAndAtivaTrue(quadra);
        List<Reserva> aprovadasFuturas = reservaRepository
                .findByQuadraAndStatusAndDataUtilizacaoGreaterThanEqualOrderByDataUtilizacaoAscHorarioInicioAsc(
                        quadra, StatusReserva.APROVADA, calendarioService.obterDataAtual());

        boolean existeReservaSemFuncionamento = aprovadasFuturas.stream()
                .anyMatch(reserva -> !estaCobertaPorHorarioAtivo(reserva, ativos));
        if (existeReservaSemFuncionamento) {
            throw new IllegalStateException(
                    "A alteração deixaria uma reserva aprovada fora do funcionamento da quadra");
        }
    }

    private boolean estaCobertaPorHorarioAtivo(
            Reserva reserva, List<DisponibilidadeQuadra> horariosAtivos) {
        DiaSemana dia = DiaSemana.from(reserva.getDataUtilizacao().getDayOfWeek());
        return horariosAtivos.stream().anyMatch(horario -> horario.getDiaSemana() == dia
                && CalendarioDisponibilidadeService.periodoAlinhadoComGrade(
                        horario.getHorarioInicio(), horario.getHorarioFim(),
                        reserva.getHorarioInicio(), reserva.getHorarioFim()));
    }

    private void validarPeriodo(LocalTime inicio, LocalTime fim) {
        if (inicio == null || fim == null || !inicio.isBefore(fim)) {
            throw new IllegalArgumentException(
                    "O horário final deve ser posterior ao horário inicial no mesmo dia");
        }
    }

    private void validarDiaSemana(DiaSemana diaSemana) {
        if (diaSemana == null) {
            throw new IllegalArgumentException("O dia da semana é obrigatório");
        }
    }

    private String validarMotivo(String motivo) {
        String normalizado = motivo == null ? "" : motivo.trim();
        if (normalizado.isEmpty() || normalizado.length() > TAMANHO_MAXIMO_MOTIVO) {
            throw new IllegalArgumentException(
                    "O motivo é obrigatório e deve possuir no máximo 200 caracteres");
        }
        return normalizado;
    }

}
