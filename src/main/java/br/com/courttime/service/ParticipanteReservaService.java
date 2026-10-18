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

import java.util.List;

@Service
public class ParticipanteReservaService {

    private static final int TAMANHO_MAXIMO_NOME = 150;
    private static final int TAMANHO_MAXIMO_RM_OU_RG = 30;
    private static final int MINIMO_DE_ALUNOS = 10;

    private final ParticipanteReservaRepository participanteRepository;
    private final ReservaRepository reservaRepository;

    public ParticipanteReservaService(
            ParticipanteReservaRepository participanteRepository,
            ReservaRepository reservaRepository) {
        this.participanteRepository = participanteRepository;
        this.reservaRepository = reservaRepository;
    }

    @Transactional(readOnly = true)
    public List<ParticipanteReserva> listar(Long reservaId, Usuario solicitante) {
        Reserva reserva = obterReservaDoSolicitante(reservaId, solicitante);
        return participanteRepository.findByReservaOrderByNomeCompletoAscIdAsc(reserva);
    }

    @Transactional(readOnly = true)
    public ParticipanteReserva obter(
            Long reservaId, Long participanteId, Usuario solicitante) {
        Reserva reserva = obterReservaDoSolicitante(reservaId, solicitante);
        return obterParticipanteDaReserva(participanteId, reserva);
    }

    @Transactional(readOnly = true)
    public ResumoParticipantes resumir(Long reservaId, Usuario solicitante) {
        Reserva reserva = obterReservaDoSolicitante(reservaId, solicitante);
        List<ParticipanteReserva> participantes = participanteRepository
                .findByReservaOrderByNomeCompletoAscIdAsc(reserva);
        long quantidadeAlunos = participantes.stream()
                .filter(item -> item.getCategoria() == CategoriaParticipante.ALUNO)
                .count();
        long quantidadeOutros = participantes.size() - quantidadeAlunos;
        return new ResumoParticipantes(
                quantidadeAlunos,
                quantidadeOutros,
                participantes.size(),
                quantidadeAlunos >= MINIMO_DE_ALUNOS);
    }

    @Transactional
    public ParticipanteReserva adicionar(
            Long reservaId,
            Usuario solicitante,
            String nomeCompleto,
            String rmOuRg,
            CategoriaParticipante categoria) {
        Reserva reserva = obterReservaEditavel(reservaId, solicitante);

        ParticipanteReserva participante = new ParticipanteReserva();
        participante.setReserva(reserva);
        preencher(participante, nomeCompleto, rmOuRg, categoria);
        return participanteRepository.save(participante);
    }

    @Transactional
    public ParticipanteReserva editar(
            Long reservaId,
            Long participanteId,
            Usuario solicitante,
            String nomeCompleto,
            String rmOuRg,
            CategoriaParticipante categoria) {
        Reserva reserva = obterReservaEditavel(reservaId, solicitante);
        ParticipanteReserva participante = obterParticipanteDaReserva(participanteId, reserva);
        preencher(participante, nomeCompleto, rmOuRg, categoria);
        return participanteRepository.save(participante);
    }

    @Transactional
    public void remover(Long reservaId, Long participanteId, Usuario solicitante) {
        Reserva reserva = obterReservaEditavel(reservaId, solicitante);
        ParticipanteReserva participante = obterParticipanteDaReserva(participanteId, reserva);
        participanteRepository.delete(participante);
    }

    private Reserva obterReservaDoSolicitante(Long reservaId, Usuario solicitante) {
        validarSolicitante(solicitante);
        validarReservaId(reservaId);
        return reservaRepository.findByIdAndSolicitante(reservaId, solicitante)
                .orElseThrow(() -> new AccessDeniedException(
                        "A solicitação não existe ou não pertence ao usuário autenticado"));
    }

    private Reserva obterReservaEditavel(Long reservaId, Usuario solicitante) {
        validarSolicitante(solicitante);
        validarReservaId(reservaId);
        Reserva reserva = reservaRepository.findByIdAndSolicitanteIdForUpdate(
                        reservaId, solicitante.getId())
                .orElseThrow(() -> new AccessDeniedException(
                        "A solicitação não existe ou não pertence ao usuário autenticado"));
        if (reserva.getStatus() != StatusReserva.PENDENTE_DOCUMENTACAO) {
            throw new IllegalStateException(
                    "Os participantes só podem ser alterados enquanto a documentação está pendente");
        }
        return reserva;
    }

    private ParticipanteReserva obterParticipanteDaReserva(
            Long participanteId, Reserva reserva) {
        if (participanteId == null) {
            throw new IllegalArgumentException("O participante é obrigatório");
        }
        return participanteRepository.findByIdAndReserva(participanteId, reserva)
                .orElseThrow(() -> new AccessDeniedException(
                        "O participante não existe ou não pertence à solicitação informada"));
    }

    private void preencher(
            ParticipanteReserva participante,
            String nomeCompleto,
            String rmOuRg,
            CategoriaParticipante categoria) {
        participante.setNomeCompleto(normalizarCampo(
                nomeCompleto,
                TAMANHO_MAXIMO_NOME,
                "O nome completo é obrigatório e deve possuir no máximo 150 caracteres"));
        participante.setRmOuRg(normalizarCampo(
                rmOuRg,
                TAMANHO_MAXIMO_RM_OU_RG,
                "O RM ou RG é obrigatório e deve possuir no máximo 30 caracteres"));
        if (categoria == null) {
            throw new IllegalArgumentException("A categoria do participante é obrigatória");
        }
        participante.setCategoria(categoria);
    }

    private String normalizarCampo(String valor, int tamanhoMaximo, String mensagem) {
        String normalizado = valor == null ? "" : valor.trim().replaceAll("\\s+", " ");
        if (normalizado.isEmpty() || normalizado.length() > tamanhoMaximo) {
            throw new IllegalArgumentException(mensagem);
        }
        return normalizado;
    }

    private void validarSolicitante(Usuario solicitante) {
        if (solicitante == null
                || solicitante.getId() == null
                || !solicitante.isAtivo()
                || solicitante.getPerfil() != Perfil.ALUNO) {
            throw new AccessDeniedException("Usuário sem permissão para gerenciar participantes");
        }
    }

    private void validarReservaId(Long reservaId) {
        if (reservaId == null) {
            throw new IllegalArgumentException("A solicitação é obrigatória");
        }
    }
}
