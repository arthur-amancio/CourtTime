package br.com.courttime.service;

import br.com.courttime.entity.ConferenciaDocumento;
import br.com.courttime.entity.DocumentoReserva;
import br.com.courttime.entity.Perfil;
import br.com.courttime.entity.Reserva;
import br.com.courttime.entity.ResultadoConferenciaDocumento;
import br.com.courttime.entity.StatusReserva;
import br.com.courttime.entity.Usuario;
import br.com.courttime.repository.ConferenciaDocumentoRepository;
import br.com.courttime.repository.DocumentoReservaRepository;
import br.com.courttime.repository.ReservaRepository;
import br.com.courttime.storage.DocumentoStorage;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ConferenciaDocumentoService {

    private final DocumentoReservaRepository documentoRepository;
    private final ConferenciaDocumentoRepository conferenciaRepository;
    private final ReservaRepository reservaRepository;
    private final ParticipanteReservaService participanteService;
    private final DocumentoStorage documentoStorage;

    public ConferenciaDocumentoService(
            DocumentoReservaRepository documentoRepository,
            ConferenciaDocumentoRepository conferenciaRepository,
            ReservaRepository reservaRepository,
            ParticipanteReservaService participanteService,
            DocumentoStorage documentoStorage) {
        this.documentoRepository = documentoRepository;
        this.conferenciaRepository = conferenciaRepository;
        this.reservaRepository = reservaRepository;
        this.participanteService = participanteService;
        this.documentoStorage = documentoStorage;
    }

    @Transactional(readOnly = true)
    public List<DocumentoReserva> listarPendentes(Usuario secretaria) {
        validarSecretaria(secretaria);
        return documentoRepository.listarVersoesMaisRecentesPendentes(
                StatusReserva.PENDENTE_DOCUMENTACAO);
    }

    @Transactional(readOnly = true)
    public List<ConferenciaDocumento> listarHistorico(Usuario secretaria) {
        validarSecretaria(secretaria);
        return conferenciaRepository.listarHistorico();
    }

    @Transactional(readOnly = true)
    public DocumentoReserva obter(Long documentoId, Usuario secretaria) {
        validarSecretaria(secretaria);
        validarDocumentoId(documentoId);
        return documentoRepository.obterComSolicitacao(documentoId)
                .orElseThrow(() -> new IllegalArgumentException("Documento não encontrado"));
    }

    @Transactional(readOnly = true)
    public boolean podeConferir(DocumentoReserva documento, Usuario secretaria) {
        validarSecretaria(secretaria);
        if (documento == null
                || documento.getConferencia() != null
                || documento.getReserva().getStatus() != StatusReserva.PENDENTE_DOCUMENTACAO) {
            return false;
        }
        return documentoRepository.obterMaiorVersao(documento.getReserva())
                == documento.getVersao();
    }

    @Transactional(readOnly = true)
    public DownloadDocumento baixar(Long documentoId, Usuario secretaria) {
        DocumentoReserva documento = obter(documentoId, secretaria);
        return new DownloadDocumento(
                documento.getNomeOriginal(),
                documento.getTamanhoBytes(),
                documentoStorage.carregar(documento.getChaveArmazenamento()));
    }

    @Transactional
    public ConferenciaDocumento validar(Long documentoId, Usuario secretaria) {
        return registrar(
                documentoId,
                secretaria,
                ResultadoConferenciaDocumento.VALIDADO,
                null);
    }

    @Transactional
    public ConferenciaDocumento devolver(
            Long documentoId, Usuario secretaria, String justificativa) {
        String motivo = justificativa == null ? "" : justificativa.trim();
        if (motivo.isEmpty() || motivo.length() > 500) {
            throw new IllegalArgumentException(
                    "A justificativa da devolução é obrigatória e deve possuir no máximo 500 caracteres");
        }
        return registrar(
                documentoId,
                secretaria,
                ResultadoConferenciaDocumento.DEVOLVIDO_CORRECAO,
                motivo);
    }

    private ConferenciaDocumento registrar(
            Long documentoId,
            Usuario secretaria,
            ResultadoConferenciaDocumento resultado,
            String justificativa) {
        validarSecretaria(secretaria);
        validarDocumentoId(documentoId);
        Long reservaId = documentoRepository.obterReservaId(documentoId)
                .orElseThrow(() -> new IllegalArgumentException("Documento não encontrado"));
        Reserva reserva = reservaRepository.findByIdForUpdate(reservaId)
                .orElseThrow(() -> new IllegalArgumentException("Solicitação não encontrada"));
        DocumentoReserva documento = documentoRepository.obterComSolicitacao(documentoId)
                .orElseThrow(() -> new IllegalArgumentException("Documento não encontrado"));

        if (reserva.getStatus() != StatusReserva.PENDENTE_DOCUMENTACAO) {
            throw new IllegalStateException(
                    "Somente documentos de solicitações com documentação pendente podem ser conferidos");
        }
        if (documentoRepository.obterMaiorVersao(reserva) != documento.getVersao()) {
            throw new IllegalStateException("Somente a versão mais recente pode ser conferida");
        }
        if (conferenciaRepository.existsByDocumento(documento)) {
            throw new IllegalStateException("Esta versão já possui uma conferência registrada");
        }
        if (resultado == ResultadoConferenciaDocumento.VALIDADO) {
            participanteService.validarDocumentoAtual(reserva, documento);
        }

        ConferenciaDocumento conferencia = new ConferenciaDocumento();
        conferencia.setDocumento(documento);
        conferencia.setResultado(resultado);
        conferencia.setJustificativa(justificativa);
        conferencia.setConferidoPor(secretaria);
        return conferenciaRepository.saveAndFlush(conferencia);
    }

    private void validarSecretaria(Usuario secretaria) {
        if (secretaria == null
                || secretaria.getId() == null
                || !secretaria.isAtivo()
                || secretaria.getPerfil() != Perfil.SECRETARIA) {
            throw new AccessDeniedException("Usuário sem permissão para conferir documentos");
        }
    }

    private void validarDocumentoId(Long documentoId) {
        if (documentoId == null) {
            throw new IllegalArgumentException("O documento é obrigatório");
        }
    }
}
