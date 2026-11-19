package br.com.courttime.service;

import br.com.courttime.entity.DocumentoReserva;
import br.com.courttime.entity.Perfil;
import br.com.courttime.entity.Reserva;
import br.com.courttime.entity.ResultadoConferenciaDocumento;
import br.com.courttime.entity.StatusReserva;
import br.com.courttime.entity.Usuario;
import br.com.courttime.repository.DocumentoReservaRepository;
import br.com.courttime.repository.ReservaRepository;
import br.com.courttime.security.Permissao;
import br.com.courttime.storage.DocumentoStorage;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class DocumentoReservaService {

    public static final long TAMANHO_MAXIMO_BYTES = 10L * 1024L * 1024L;

    /** R01.2: mensagem exibida quando o envio chega no dia da utilização ou depois. */
    public static final String MENSAGEM_PRAZO_ENCERRADO =
            "O documento assinado deve ser entregue até o dia anterior ao uso.";

    private static final byte[] ASSINATURA_PDF = "%PDF-".getBytes(StandardCharsets.US_ASCII);
    private static final Set<String> TIPOS_ACEITOS = Set.of(
            "application/pdf", "application/octet-stream");

    private final DocumentoReservaRepository documentoRepository;
    private final ReservaRepository reservaRepository;
    private final DocumentoStorage documentoStorage;

    public DocumentoReservaService(
            DocumentoReservaRepository documentoRepository,
            ReservaRepository reservaRepository,
            DocumentoStorage documentoStorage) {
        this.documentoRepository = documentoRepository;
        this.reservaRepository = reservaRepository;
        this.documentoStorage = documentoStorage;
    }

    @Transactional(readOnly = true)
    public List<DocumentoReserva> listar(Long reservaId, Usuario solicitante) {
        Reserva reserva = obterReservaDoSolicitante(reservaId, solicitante);
        return documentoRepository.findByReservaOrderByVersaoDesc(reserva);
    }

    @Transactional(readOnly = true)
    public boolean podeEnviar(Long reservaId, Usuario solicitante) {
        Reserva reserva = obterReservaDoSolicitante(reservaId, solicitante);
        return envioEstaPermitido(reserva);
    }

    @Transactional
    public DocumentoReserva enviar(
            Long reservaId,
            Usuario solicitante,
            String nomeOriginal,
            String tipoConteudo,
            byte[] conteudo) {
        Permissao.exigir(solicitante, Perfil.ALUNO);
        if (reservaId == null) {
            throw new IllegalArgumentException("A solicitação é obrigatória");
        }
        String nomeSeguro = validarNome(nomeOriginal);
        validarTipoConteudo(tipoConteudo);
        validarConteudo(conteudo);

        Reserva reserva = reservaRepository.findByIdAndSolicitanteIdForUpdate(
                        reservaId, solicitante.getId())
                .orElseThrow(() -> new AccessDeniedException(
                        "A solicitação não existe ou não pertence ao usuário autenticado"));
        if (reserva.getStatus() != StatusReserva.PENDENTE_DOCUMENTACAO) {
            throw new IllegalStateException(
                    "O documento só pode ser enviado enquanto a documentação está pendente");
        }
        if (prazoDeEnvioEncerrado(reserva)) {
            throw new IllegalStateException(MENSAGEM_PRAZO_ENCERRADO);
        }
        if (!envioEstaPermitido(reserva)) {
            throw new IllegalStateException(
                    "Uma nova versão só pode ser enviada após a devolução do documento para correção");
        }

        String chave = UUID.randomUUID() + ".pdf";
        documentoStorage.armazenar(chave, conteudo);
        registrarRemocaoEmRollback(chave);

        DocumentoReserva documento = new DocumentoReserva();
        documento.setReserva(reserva);
        documento.setVersao(documentoRepository.obterMaiorVersao(reserva) + 1);
        documento.setRevisaoParticipantes(reserva.getRevisaoParticipantes());
        documento.setNomeOriginal(nomeSeguro);
        documento.setChaveArmazenamento(chave);
        documento.setSha256(calcularSha256(conteudo));
        documento.setTamanhoBytes(conteudo.length);
        documento.setEnviadoPor(solicitante);
        return documentoRepository.saveAndFlush(documento);
    }

    /**
     * R01: último dia para entregar o PDF assinado, o dia anterior ao uso.
     *
     * D1: a ETEC ainda não definiu um horário-limite nesse dia. Até lá, o envio
     * vale durante todo o dia anterior e é recusado a partir do dia do uso.
     */
    public static LocalDate prazoFinalDeEnvio(Reserva reserva) {
        return reserva == null || reserva.getDataUtilizacao() == null
                ? null
                : reserva.getDataUtilizacao().minusDays(1);
    }

    /** Verdadeiro a partir do dia da utilização, no fuso da escola. */
    public static boolean prazoDeEnvioEncerrado(Reserva reserva) {
        LocalDate prazo = prazoFinalDeEnvio(reserva);
        return prazo != null
                && LocalDate.now(CalendarioDisponibilidadeService.FUSO_HORARIO).isAfter(prazo);
    }

    private boolean envioEstaPermitido(Reserva reserva) {
        if (reserva.getStatus() != StatusReserva.PENDENTE_DOCUMENTACAO
                || prazoDeEnvioEncerrado(reserva)) {
            return false;
        }
        DocumentoReserva documentoMaisRecente = documentoRepository
                .obterVersaoMaisRecenteComConferencia(reserva)
                .orElse(null);
        return documentoMaisRecente == null
                || documentoMaisRecente.getConferencia() != null
                && documentoMaisRecente.getConferencia().getResultado()
                == ResultadoConferenciaDocumento.DEVOLVIDO_CORRECAO;
    }

    @Transactional(readOnly = true)
    public DownloadDocumento baixar(
            Long reservaId, Long documentoId, Usuario solicitante) {
        Reserva reserva = obterReservaDoSolicitante(reservaId, solicitante);
        if (documentoId == null) {
            throw new IllegalArgumentException("O documento é obrigatório");
        }
        DocumentoReserva documento = documentoRepository.findByIdAndReserva(
                        documentoId, reserva)
                .orElseThrow(() -> new AccessDeniedException(
                        "O documento não existe ou não pertence à solicitação informada"));
        return new DownloadDocumento(
                documento.getNomeOriginal(),
                documento.getTamanhoBytes(),
                documentoStorage.carregar(documento.getChaveArmazenamento()));
    }

    private Reserva obterReservaDoSolicitante(Long reservaId, Usuario solicitante) {
        Permissao.exigir(solicitante, Perfil.ALUNO);
        if (reservaId == null) {
            throw new IllegalArgumentException("A solicitação é obrigatória");
        }
        return reservaRepository.findByIdAndSolicitante(reservaId, solicitante)
                .orElseThrow(() -> new AccessDeniedException(
                        "A solicitação não existe ou não pertence ao usuário autenticado"));
    }

    private String validarNome(String nomeOriginal) {
        String nome = nomeOriginal == null ? "" : nomeOriginal
                .replace('\\', '/')
                .replaceAll("[\\p{Cntrl}]", "")
                .trim();
        int ultimoSeparador = nome.lastIndexOf('/');
        if (ultimoSeparador >= 0) {
            nome = nome.substring(ultimoSeparador + 1);
        }
        if (nome.isBlank() || nome.length() > 255
                || !nome.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            throw new IllegalArgumentException(
                    "Selecione um arquivo PDF com nome de até 255 caracteres");
        }
        return nome;
    }

    private void validarTipoConteudo(String tipoConteudo) {
        if (tipoConteudo == null || tipoConteudo.isBlank()) {
            return;
        }
        String tipo = tipoConteudo.toLowerCase(Locale.ROOT);
        int separadorParametros = tipo.indexOf(';');
        if (separadorParametros >= 0) {
            tipo = tipo.substring(0, separadorParametros).trim();
        }
        if (!TIPOS_ACEITOS.contains(tipo)) {
            throw new IllegalArgumentException("O arquivo enviado deve ser um PDF");
        }
    }

    private void validarConteudo(byte[] conteudo) {
        if (conteudo == null || conteudo.length < ASSINATURA_PDF.length) {
            throw new IllegalArgumentException("O arquivo PDF não pode estar vazio");
        }
        if (conteudo.length > TAMANHO_MAXIMO_BYTES) {
            throw new IllegalArgumentException("O arquivo PDF deve possuir no máximo 10 MB");
        }
        for (int indice = 0; indice < ASSINATURA_PDF.length; indice++) {
            if (conteudo[indice] != ASSINATURA_PDF[indice]) {
                throw new IllegalArgumentException(
                        "O conteúdo enviado não corresponde a um arquivo PDF válido");
            }
        }
    }

    private String calcularSha256(byte[] conteudo) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(conteudo));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Não foi possível calcular a integridade do documento", exception);
        }
    }

    private void registrarRemocaoEmRollback(String chave) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            documentoStorage.remover(chave);
            throw new IllegalStateException("O envio do documento exige uma transação ativa");
        }
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status != STATUS_COMMITTED) {
                            try {
                                documentoStorage.remover(chave);
                            } catch (RuntimeException ignored) {
                                // A falha original da transação deve ser preservada.
                            }
                        }
                    }
                });
    }
}
