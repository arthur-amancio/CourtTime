package br.com.courttime.service;

import br.com.courttime.entity.Perfil;
import br.com.courttime.entity.Quadra;
import br.com.courttime.entity.Reserva;
import br.com.courttime.entity.StatusReserva;
import br.com.courttime.entity.Usuario;
import br.com.courttime.repository.QuadraRepository;
import br.com.courttime.repository.ReservaRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

@Service
public class ReservaService {

    private static final int TAMANHO_MAXIMO_FINALIDADE = 300;

    private final ReservaRepository reservaRepository;
    private final QuadraRepository quadraRepository;
    private final CalendarioDisponibilidadeService calendarioDisponibilidadeService;

    public ReservaService(
            ReservaRepository reservaRepository,
            QuadraRepository quadraRepository,
            CalendarioDisponibilidadeService calendarioDisponibilidadeService) {
        this.reservaRepository = reservaRepository;
        this.quadraRepository = quadraRepository;
        this.calendarioDisponibilidadeService = calendarioDisponibilidadeService;
    }

    @Transactional
    public Reserva criarSolicitacao(
            Usuario solicitante,
            Quadra quadra,
            LocalDate data,
            LocalTime horarioInicio,
            LocalTime horarioFim,
            String finalidade) {
        validarUsuarioAtivoComPerfil(solicitante, Perfil.ALUNO);
        Objects.requireNonNull(quadra, "A quadra é obrigatória");
        Objects.requireNonNull(data, "A data é obrigatória");
        validarPeriodo(horarioInicio, horarioFim);
        String finalidadeNormalizada = validarFinalidade(finalidade);

        if (!calendarioDisponibilidadeService.periodoEstaDisponivel(
                quadra, data, horarioInicio, horarioFim)) {
            throw new IllegalStateException("O período selecionado não está disponível");
        }

        Reserva reserva = new Reserva();
        reserva.setQuadra(quadra);
        reserva.setSolicitante(solicitante);
        reserva.setDataUtilizacao(data);
        reserva.setHorarioInicio(horarioInicio);
        reserva.setHorarioFim(horarioFim);
        reserva.setFinalidade(finalidadeNormalizada);
        reserva.setStatus(StatusReserva.PENDENTE_DOCUMENTACAO);
        return reservaRepository.save(reserva);
    }

    @Transactional(readOnly = true)
    public List<Reserva> listarSolicitacoesDoUsuario(Usuario solicitante) {
        validarUsuarioAtivoComPerfil(solicitante, Perfil.ALUNO);
        return reservaRepository.findBySolicitanteOrderByCriadaEmDesc(solicitante);
    }

    @Transactional
    public Reserva aprovar(Long reservaId, Usuario administrador) {
        validarUsuarioAtivoComPerfil(administrador, Perfil.ADMINISTRADOR);
        Reserva reserva = obterParaAnalise(reservaId);
        impedirAnaliseDaPropriaSolicitacao(reserva, administrador);
        validarPendente(reserva);

        Quadra quadraBloqueada = quadraRepository.findByIdForUpdate(reserva.getQuadra().getId())
                .orElseThrow(() -> new IllegalStateException("Quadra não encontrada"));

        if (!calendarioDisponibilidadeService.periodoEstaDisponivel(
                quadraBloqueada,
                reserva.getDataUtilizacao(),
                reserva.getHorarioInicio(),
                reserva.getHorarioFim())) {
            throw new IllegalStateException(
                    "A reserva não pode ser aprovada porque o período não está mais disponível");
        }

        reserva.setStatus(StatusReserva.APROVADA);
        reserva.setAnalisadaPor(administrador);
        reserva.setAnalisadaEm(Instant.now());
        reserva.setMotivoRejeicao(null);
        return reservaRepository.save(reserva);
    }

    @Transactional
    public Reserva rejeitar(Long reservaId, Usuario administrador, String motivo) {
        validarUsuarioAtivoComPerfil(administrador, Perfil.ADMINISTRADOR);
        Reserva reserva = obterParaAnalise(reservaId);
        impedirAnaliseDaPropriaSolicitacao(reserva, administrador);
        validarPendente(reserva);

        String motivoNormalizado = motivo == null ? "" : motivo.trim();
        if (motivoNormalizado.isEmpty() || motivoNormalizado.length() > 500) {
            throw new IllegalArgumentException(
                    "O motivo da rejeição é obrigatório e deve possuir no máximo 500 caracteres");
        }

        reserva.setStatus(StatusReserva.REJEITADA);
        reserva.setAnalisadaPor(administrador);
        reserva.setAnalisadaEm(Instant.now());
        reserva.setMotivoRejeicao(motivoNormalizado);
        return reservaRepository.save(reserva);
    }

    private Reserva obterParaAnalise(Long reservaId) {
        if (reservaId == null) {
            throw new IllegalArgumentException("A reserva é obrigatória");
        }
        return reservaRepository.findByIdForUpdate(reservaId)
                .orElseThrow(() -> new IllegalArgumentException("Reserva não encontrada"));
    }

    private void validarUsuarioAtivoComPerfil(Usuario usuario, Perfil perfilEsperado) {
        if (usuario == null || !usuario.isAtivo() || usuario.getPerfil() != perfilEsperado) {
            throw new AccessDeniedException("Usuário sem permissão para esta operação");
        }
    }

    private void impedirAnaliseDaPropriaSolicitacao(Reserva reserva, Usuario administrador) {
        if (Objects.equals(reserva.getSolicitante().getId(), administrador.getId())) {
            throw new AccessDeniedException("O usuário não pode analisar a própria solicitação");
        }
    }

    private void validarPendente(Reserva reserva) {
        if (reserva.getStatus() != StatusReserva.PENDENTE_ANALISE) {
            throw new IllegalStateException(
                    "Somente solicitações com documentação conferida podem ser analisadas");
        }
    }

    private void validarPeriodo(LocalTime horarioInicio, LocalTime horarioFim) {
        if (horarioInicio == null || horarioFim == null
                || !horarioInicio.isBefore(horarioFim)
                || !Duration.between(horarioInicio, horarioFim).equals(Duration.ofHours(1))) {
            throw new IllegalArgumentException("A solicitação deve possuir duração de uma hora");
        }
    }

    private String validarFinalidade(String finalidade) {
        String finalidadeNormalizada = finalidade == null ? "" : finalidade.trim();
        if (finalidadeNormalizada.isEmpty()
                || finalidadeNormalizada.length() > TAMANHO_MAXIMO_FINALIDADE) {
            throw new IllegalArgumentException(
                    "A finalidade é obrigatória e deve possuir no máximo 300 caracteres");
        }
        return finalidadeNormalizada;
    }
}
