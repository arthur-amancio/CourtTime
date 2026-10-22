package br.com.courttime.service;

import br.com.courttime.entity.CategoriaParticipante;
import br.com.courttime.entity.DocumentoReserva;
import br.com.courttime.entity.ParticipanteReserva;
import br.com.courttime.entity.Perfil;
import br.com.courttime.entity.Reserva;
import br.com.courttime.entity.ResultadoConferenciaDocumento;
import br.com.courttime.entity.StatusReserva;
import br.com.courttime.entity.Usuario;
import br.com.courttime.entity.VinculoParticipante;
import br.com.courttime.repository.DocumentoReservaRepository;
import br.com.courttime.repository.ParticipanteReservaRepository;
import br.com.courttime.repository.ReservaRepository;
import br.com.courttime.security.Permissao;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
public class ParticipanteReservaService {

    private static final int TAMANHO_MAXIMO_NOME = 150;
    private static final int TAMANHO_MAXIMO_RM_OU_RG = 30;
    public static final int MINIMO_DE_PARTICIPANTES = 10;

    public static final String MENSAGEM_VINCULO_OBRIGATORIO =
            "Na categoria Outro, escolha o vínculo: ex-aluno, pai ou mãe, irmão ou irmã de aluno, ou professor da ETEC";
    public static final String MENSAGEM_VINCULO_SOMENTE_OUTRO =
            "O vínculo só é informado na categoria Outro";

    private final ParticipanteReservaRepository participanteRepository;
    private final ReservaRepository reservaRepository;
    private final DocumentoReservaRepository documentoRepository;

    public ParticipanteReservaService(
            ParticipanteReservaRepository participanteRepository,
            ReservaRepository reservaRepository,
            DocumentoReservaRepository documentoRepository) {
        this.participanteRepository = participanteRepository;
        this.reservaRepository = reservaRepository;
        this.documentoRepository = documentoRepository;
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
                participantes.size() >= MINIMO_DE_PARTICIPANTES);
    }

    @Transactional(readOnly = true)
    public boolean podeAlterar(Long reservaId, Usuario solicitante) {
        Reserva reserva = obterReservaDoSolicitante(reservaId, solicitante);
        return alteracaoEstaPermitida(reserva);
    }

    /** Total de participantes da solicitação, de qualquer categoria. */
    @Transactional(readOnly = true)
    public long contarParticipantes(Reserva reserva) {
        return participanteRepository.countByReserva(reserva);
    }

    /** R09.4: participantes Outro cadastrados antes da regra, sem vínculo. */
    @Transactional(readOnly = true)
    public long contarOutrosSemVinculo(Reserva reserva) {
        return participanteRepository.countByReservaAndCategoriaAndVinculoIsNull(
                reserva, CategoriaParticipante.OUTRO);
    }

    @Transactional(readOnly = true)
    public void validarDocumentoAtual(Reserva reserva, DocumentoReserva documento) {
        if (reserva == null || documento == null) {
            throw new IllegalArgumentException("A solicitação e o documento são obrigatórios");
        }
        if (documento.getRevisaoParticipantes() != reserva.getRevisaoParticipantes()) {
            throw new IllegalStateException(
                    "A lista de participantes foi alterada após o envio do documento");
        }
        long quantidadeParticipantes = participanteRepository.countByReserva(reserva);
        if (quantidadeParticipantes < MINIMO_DE_PARTICIPANTES) {
            throw new IllegalStateException(
                    "A solicitação deve possuir no mínimo 10 participantes, de qualquer categoria");
        }
    }

    @Transactional
    public ParticipanteReserva adicionar(
            Long reservaId,
            Usuario solicitante,
            String nomeCompleto,
            String rmOuRg,
            CategoriaParticipante categoria,
            VinculoParticipante vinculo) {
        Reserva reserva = obterReservaEditavel(reservaId, solicitante);

        ParticipanteReserva participante = new ParticipanteReserva();
        participante.setReserva(reserva);
        preencher(participante, nomeCompleto, rmOuRg, categoria, vinculo);
        impedirIdentificacaoDuplicada(reserva, participante.getRmOuRg(), null);
        participante = participanteRepository.save(participante);
        incrementarRevisao(reserva);
        return participante;
    }

    /**
     * R10: pessoa acrescentada pela segurança no dia da reserva. Segue as mesmas
     * regras do cadastro do aluno (nome, RM ou RG, categoria e vínculo R09), mas
     * não altera a revisão da lista: o documento assinado continua o mesmo.
     * Chamado pelo PortariaService, que já conferiu o perfil, a reserva e o dia.
     */
    @Transactional
    public ParticipanteReserva acrescentarNaPortaria(
            Reserva reserva,
            Usuario seguranca,
            String nomeCompleto,
            String rmOuRg,
            CategoriaParticipante categoria,
            VinculoParticipante vinculo) {
        ParticipanteReserva participante = new ParticipanteReserva();
        participante.setReserva(reserva);
        preencher(participante, nomeCompleto, rmOuRg, categoria, vinculo);
        impedirIdentificacaoDuplicada(reserva, participante.getRmOuRg(), null);
        participante.marcarAdicionadoNaPortaria(seguranca, Instant.now());
        return participanteRepository.save(participante);
    }

    @Transactional
    public ParticipanteReserva editar(
            Long reservaId,
            Long participanteId,
            Usuario solicitante,
            String nomeCompleto,
            String rmOuRg,
            CategoriaParticipante categoria,
            VinculoParticipante vinculo) {
        Reserva reserva = obterReservaEditavel(reservaId, solicitante);
        ParticipanteReserva participante = obterParticipanteDaReserva(participanteId, reserva);
        preencher(participante, nomeCompleto, rmOuRg, categoria, vinculo);
        impedirIdentificacaoDuplicada(reserva, participante.getRmOuRg(), participante.getId());
        participante = participanteRepository.save(participante);
        incrementarRevisao(reserva);
        return participante;
    }

    @Transactional
    public void remover(Long reservaId, Long participanteId, Usuario solicitante) {
        Reserva reserva = obterReservaEditavel(reservaId, solicitante);
        ParticipanteReserva participante = obterParticipanteDaReserva(participanteId, reserva);
        participanteRepository.delete(participante);
        incrementarRevisao(reserva);
    }

    private Reserva obterReservaDoSolicitante(Long reservaId, Usuario solicitante) {
        Permissao.exigir(solicitante, Perfil.ALUNO);
        validarReservaId(reservaId);
        return reservaRepository.findByIdAndSolicitante(reservaId, solicitante)
                .orElseThrow(() -> new AccessDeniedException(
                        "A solicitação não existe ou não pertence ao usuário autenticado"));
    }

    private Reserva obterReservaEditavel(Long reservaId, Usuario solicitante) {
        Permissao.exigir(solicitante, Perfil.ALUNO);
        validarReservaId(reservaId);
        Reserva reserva = reservaRepository.findByIdAndSolicitanteIdForUpdate(
                        reservaId, solicitante.getId())
                .orElseThrow(() -> new AccessDeniedException(
                        "A solicitação não existe ou não pertence ao usuário autenticado"));
        if (!alteracaoEstaPermitida(reserva)) {
            throw new IllegalStateException(
                    "Os participantes só podem ser alterados antes do envio ou após a devolução do documento");
        }
        return reserva;
    }

    private boolean alteracaoEstaPermitida(Reserva reserva) {
        if (reserva.getStatus() != StatusReserva.PENDENTE_DOCUMENTACAO) {
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

    private void impedirIdentificacaoDuplicada(
            Reserva reserva, String rmOuRg, Long participanteId) {
        boolean duplicada = participanteId == null
                ? participanteRepository.existsByReservaAndRmOuRgIgnoreCase(reserva, rmOuRg)
                : participanteRepository.existsByReservaAndRmOuRgIgnoreCaseAndIdNot(
                        reserva, rmOuRg, participanteId);
        if (duplicada) {
            throw new IllegalArgumentException(
                    "Já existe um participante com este RM ou RG na solicitação");
        }
    }

    /**
     * Copia nome, RM/RG, categoria e vínculo dos participantes de uma reserva cancelada
     * para a remarcação dela. Chamado pelo ReservaService na mesma transação que
     * já conferiu o dono e a situação da reserva cancelada.
     */
    @Transactional
    public int copiarParticipantes(Reserva cancelada, Reserva remarcacao) {
        if (cancelada == null || remarcacao == null
                || cancelada.getStatus() != StatusReserva.CANCELADA
                || remarcacao.getStatus() != StatusReserva.PENDENTE_DOCUMENTACAO
                || !Objects.equals(
                        cancelada.getSolicitante().getId(),
                        remarcacao.getSolicitante().getId())) {
            throw new IllegalStateException(
                    "Os participantes só podem ser copiados para uma nova solicitação do mesmo aluno");
        }
        List<ParticipanteReserva> originais =
                participanteRepository.findByReservaOrderByNomeCompletoAscIdAsc(cancelada);
        // Quem foi acrescentado na portaria não faz parte da lista do aluno.
        originais = originais.stream()
                .filter(original -> !original.isAdicionadoNaPortaria())
                .toList();
        for (ParticipanteReserva original : originais) {
            ParticipanteReserva copia = new ParticipanteReserva();
            copia.setReserva(remarcacao);
            copia.setNomeCompleto(original.getNomeCompleto());
            copia.setRmOuRg(original.getRmOuRg());
            copia.setCategoria(original.getCategoria());
            copia.setVinculo(original.getVinculo());
            participanteRepository.save(copia);
        }
        if (!originais.isEmpty()) {
            incrementarRevisao(remarcacao);
        }
        return originais.size();
    }

    private void incrementarRevisao(Reserva reserva) {
        reserva.incrementarRevisaoParticipantes();
        reservaRepository.save(reserva);
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
            CategoriaParticipante categoria,
            VinculoParticipante vinculo) {
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
        // R09: só entra como Outro quem tem um dos vínculos da ETEC.
        if (categoria == CategoriaParticipante.OUTRO && vinculo == null) {
            throw new IllegalArgumentException(MENSAGEM_VINCULO_OBRIGATORIO);
        }
        if (categoria == CategoriaParticipante.ALUNO && vinculo != null) {
            throw new IllegalArgumentException(MENSAGEM_VINCULO_SOMENTE_OUTRO);
        }
        participante.setCategoria(categoria);
        participante.setVinculo(vinculo);
    }

    private String normalizarCampo(String valor, int tamanhoMaximo, String mensagem) {
        String normalizado = valor == null ? "" : valor.trim().replaceAll("\\s+", " ");
        if (normalizado.isEmpty() || normalizado.length() > tamanhoMaximo) {
            throw new IllegalArgumentException(mensagem);
        }
        return normalizado;
    }

    private void validarReservaId(Long reservaId) {
        if (reservaId == null) {
            throw new IllegalArgumentException("A solicitação é obrigatória");
        }
    }
}
