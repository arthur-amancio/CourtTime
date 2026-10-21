package br.com.courttime.service;

import br.com.courttime.entity.CategoriaParticipante;
import br.com.courttime.entity.DocumentoReserva;
import br.com.courttime.entity.ParticipanteReserva;
import br.com.courttime.entity.Perfil;
import br.com.courttime.entity.Reserva;
import br.com.courttime.entity.StatusReserva;
import br.com.courttime.entity.Usuario;
import br.com.courttime.repository.ContribuicaoApmRepository;
import br.com.courttime.repository.DocumentoReservaRepository;
import br.com.courttime.repository.ParticipanteReservaRepository;
import br.com.courttime.repository.ReservaRepository;
import br.com.courttime.storage.DocumentoStorage;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class AnaliseAdministrativaService {

    private final ReservaRepository reservaRepository;
    private final ParticipanteReservaRepository participanteRepository;
    private final DocumentoReservaRepository documentoRepository;
    private final ContribuicaoApmRepository contribuicaoRepository;
    private final DocumentoStorage documentoStorage;

    public AnaliseAdministrativaService(
            ReservaRepository reservaRepository,
            ParticipanteReservaRepository participanteRepository,
            DocumentoReservaRepository documentoRepository,
            ContribuicaoApmRepository contribuicaoRepository,
            DocumentoStorage documentoStorage) {
        this.reservaRepository = reservaRepository;
        this.participanteRepository = participanteRepository;
        this.documentoRepository = documentoRepository;
        this.contribuicaoRepository = contribuicaoRepository;
        this.documentoStorage = documentoStorage;
    }

    @Transactional(readOnly = true)
    public List<Reserva> listarPendentes(Usuario administrador) {
        validarAdministrador(administrador);
        return reservaRepository.listarParaAnaliseAdministrativa(
                StatusReserva.PENDENTE_ANALISE);
    }

    @Transactional(readOnly = true)
    public List<Reserva> listarAutorizadasAtuais(Usuario administrador) {
        validarAdministrador(administrador);
        LocalDate dataAtual = LocalDate.now(
                CalendarioDisponibilidadeService.FUSO_HORARIO);
        return reservaRepository.listarAutorizadasParaAdministracao(
                StatusReserva.APROVADA, dataAtual);
    }

    /**
     * Rejeitadas e canceladas, para a administração consultar decisões já
     * registradas. Saem da fila e das reservas autorizadas, mas o histórico fica.
     */
    @Transactional(readOnly = true)
    public List<Reserva> listarEncerradas(Usuario administrador) {
        validarAdministrador(administrador);
        return reservaRepository.listarEncerradasParaAdministracao(
                List.of(StatusReserva.REJEITADA, StatusReserva.CANCELADA));
    }

    @Transactional(readOnly = true)
    public DetalheAnaliseAdministrativa detalhar(
            Long reservaId, Usuario administrador) {
        validarAdministrador(administrador);
        Reserva reserva = obterReserva(reservaId);
        List<ParticipanteReserva> participantes =
                participanteRepository.listarComPresenca(reserva);
        long alunos = participantes.stream()
                .filter(participante -> participante.getCategoria() == CategoriaParticipante.ALUNO)
                .count();
        DocumentoReserva documento = documentoRepository
                .obterVersaoMaisRecenteComConferencia(reserva)
                .orElse(null);
        return new DetalheAnaliseAdministrativa(
                reserva,
                participantes,
                new ResumoParticipantes(
                        alunos,
                        participantes.size() - alunos,
                        participantes.size(),
                        alunos >= ParticipanteReservaService.MINIMO_DE_ALUNOS),
                documento,
                contribuicaoRepository.obterPorReservaId(reservaId).orElse(null));
    }

    @Transactional(readOnly = true)
    public DownloadDocumento baixar(
            Long reservaId, Long documentoId, Usuario administrador) {
        validarAdministrador(administrador);
        Reserva reserva = obterReserva(reservaId);
        if (documentoId == null) {
            throw new IllegalArgumentException("O documento é obrigatório");
        }
        DocumentoReserva documento = documentoRepository.findByIdAndReserva(
                        documentoId, reserva)
                .orElseThrow(() -> new AccessDeniedException(
                        "O documento não pertence à solicitação informada"));
        return new DownloadDocumento(
                documento.getNomeOriginal(),
                documento.getTamanhoBytes(),
                documentoStorage.carregar(documento.getChaveArmazenamento()));
    }

    private Reserva obterReserva(Long reservaId) {
        if (reservaId == null) {
            throw new IllegalArgumentException("A solicitação é obrigatória");
        }
        return reservaRepository.obterParaConsultaAdministrativa(reservaId)
                .orElseThrow(() -> new IllegalArgumentException("Solicitação não encontrada"));
    }

    private void validarAdministrador(Usuario administrador) {
        if (administrador == null
                || administrador.getId() == null
                || !administrador.isAtivo()
                || administrador.getPerfil() != Perfil.ADMINISTRADOR) {
            throw new AccessDeniedException(
                    "Usuário sem permissão para consultar a análise administrativa");
        }
    }
}
