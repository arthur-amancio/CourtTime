package br.com.courttime.service;

import br.com.courttime.entity.CategoriaParticipante;
import br.com.courttime.entity.ParticipanteReserva;
import br.com.courttime.entity.Perfil;
import br.com.courttime.entity.Reserva;
import br.com.courttime.entity.StatusReserva;
import br.com.courttime.entity.Usuario;
import br.com.courttime.repository.ParticipanteReservaRepository;
import br.com.courttime.repository.ReservaRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class ConsultaSegurancaService {

    private final ReservaRepository reservaRepository;
    private final ParticipanteReservaRepository participanteRepository;

    public ConsultaSegurancaService(
            ReservaRepository reservaRepository,
            ParticipanteReservaRepository participanteRepository) {
        this.reservaRepository = reservaRepository;
        this.participanteRepository = participanteRepository;
    }

    @Transactional(readOnly = true)
    public List<ReservaAutorizadaResumo> listarAutorizadas(Usuario seguranca) {
        validarSeguranca(seguranca);
        LocalDate dataAtual = LocalDate.now(CalendarioDisponibilidadeService.FUSO_HORARIO);
        return reservaRepository.listarAutorizadasParaSeguranca(
                        StatusReserva.APROVADA, dataAtual)
                .stream()
                .map(this::resumir)
                .toList();
    }

    @Transactional(readOnly = true)
    public DetalheReservaAutorizada detalhar(Long reservaId, Usuario seguranca) {
        validarSeguranca(seguranca);
        if (reservaId == null) {
            throw new IllegalArgumentException("A reserva é obrigatória");
        }

        LocalDate dataAtual = LocalDate.now(CalendarioDisponibilidadeService.FUSO_HORARIO);
        Reserva reserva = reservaRepository.obterAutorizadaParaSeguranca(
                        reservaId, StatusReserva.APROVADA, dataAtual)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Reserva autorizada não encontrada"));
        List<ParticipanteConferencia> participantes = participanteRepository
                .findByReservaOrderByNomeCompletoAscIdAsc(reserva)
                .stream()
                .map(this::identificacao)
                .toList();
        return new DetalheReservaAutorizada(resumir(reserva), participantes);
    }

    private ReservaAutorizadaResumo resumir(Reserva reserva) {
        return new ReservaAutorizadaResumo(
                reserva.getId(),
                reserva.getDataUtilizacao(),
                reserva.getHorarioInicio(),
                reserva.getHorarioFim(),
                reserva.getQuadra().getNome(),
                reserva.getSolicitante().getNome(),
                participanteRepository.countByReserva(reserva));
    }

    private ParticipanteConferencia identificacao(ParticipanteReserva participante) {
        String categoria = participante.getCategoria() == CategoriaParticipante.ALUNO
                ? "Aluno"
                : "Outro";
        return new ParticipanteConferencia(
                participante.getNomeCompleto(),
                participante.getRmOuRg(),
                categoria);
    }

    private void validarSeguranca(Usuario seguranca) {
        if (seguranca == null
                || seguranca.getId() == null
                || !seguranca.isAtivo()
                || seguranca.getPerfil() != Perfil.SEGURANCA) {
            throw new AccessDeniedException(
                    "Usuário sem permissão para consultar reservas autorizadas");
        }
    }
}
