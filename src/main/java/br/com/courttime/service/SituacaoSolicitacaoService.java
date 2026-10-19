package br.com.courttime.service;

import br.com.courttime.entity.ContribuicaoApm;
import br.com.courttime.entity.DocumentoReserva;
import br.com.courttime.entity.Reserva;
import br.com.courttime.entity.ResultadoConferenciaDocumento;
import br.com.courttime.entity.StatusReserva;
import br.com.courttime.repository.ContribuicaoApmRepository;
import br.com.courttime.repository.DocumentoReservaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SituacaoSolicitacaoService {

    private final DocumentoReservaRepository documentoRepository;
    private final ContribuicaoApmRepository contribuicaoRepository;

    public SituacaoSolicitacaoService(
            DocumentoReservaRepository documentoRepository,
            ContribuicaoApmRepository contribuicaoRepository) {
        this.documentoRepository = documentoRepository;
        this.contribuicaoRepository = contribuicaoRepository;
    }

    @Transactional(readOnly = true)
    public List<SolicitacaoAluno> descrever(List<Reserva> solicitacoes) {
        return solicitacoes.stream()
                .map(solicitacao -> new SolicitacaoAluno(
                        solicitacao, descrever(solicitacao)))
                .toList();
    }

    @Transactional(readOnly = true)
    public SituacaoSolicitacao descrever(Reserva solicitacao) {
        if (solicitacao == null || solicitacao.getStatus() == null) {
            throw new IllegalArgumentException("A solicitação é obrigatória");
        }
        return switch (solicitacao.getStatus()) {
            case APROVADA -> new SituacaoSolicitacao(
                    "Aprovada",
                    "A utilização da quadra foi autorizada para a data e o horário informados.",
                    true);
            case REJEITADA -> new SituacaoSolicitacao(
                    "Rejeitada",
                    "A administração encerrou a análise sem autorizar a utilização.",
                    false);
            case CANCELADA -> new SituacaoSolicitacao(
                    "Cancelada",
                    "Esta solicitação não autoriza mais a utilização da quadra.",
                    false);
            case PENDENTE_ANALISE -> new SituacaoSolicitacao(
                    "Pronta para análise administrativa",
                    "Documento e contribuição foram registrados. A solicitação aguarda a decisão da administração.",
                    false);
            case PENDENTE_DOCUMENTACAO -> descreverEtapaDocumental(solicitacao);
        };
    }

    private SituacaoSolicitacao descreverEtapaDocumental(Reserva solicitacao) {
        DocumentoReserva documento = documentoRepository
                .obterVersaoMaisRecenteComConferencia(solicitacao)
                .orElse(null);
        if (documento == null) {
            return new SituacaoSolicitacao(
                    "Pendente de documentação",
                    "Revise os participantes e envie o PDF preparado e assinado fora do sistema.",
                    false);
        }
        if (documento.getConferencia() == null) {
            return new SituacaoSolicitacao(
                    "Documento aguardando conferência",
                    "A secretaria ainda precisa conferir a versão mais recente enviada.",
                    false);
        }
        if (documento.getConferencia().getResultado()
                == ResultadoConferenciaDocumento.DEVOLVIDO_CORRECAO) {
            return new SituacaoSolicitacao(
                    "Documento devolvido para correção",
                    "Consulte a justificativa, corrija o documento fora do sistema e envie uma nova versão.",
                    false);
        }
        ContribuicaoApm contribuicao = contribuicaoRepository
                .obterPorReservaId(solicitacao.getId())
                .orElse(null);
        if (contribuicao == null) {
            return new SituacaoSolicitacao(
                    "Contribuição pendente",
                    "O documento foi conferido. A secretaria ainda precisa registrar a contribuição presencial.",
                    false);
        }
        if (contribuicao.getConferenciaDocumento() == null
                || contribuicao.getConferenciaDocumento().getId() == null
                || documento.getConferencia().getId() == null
                || !contribuicao.getConferenciaDocumento().getId()
                .equals(documento.getConferencia().getId())) {
            return new SituacaoSolicitacao(
                    "Contribuição precisa de revisão",
                    "A secretaria precisa relacionar o registro presencial ao documento mais recente.",
                    false);
        }
        return new SituacaoSolicitacao(
                "Pronta para análise administrativa",
                "Documento e contribuição foram registrados. A solicitação aguarda a decisão da administração.",
                false);
    }
}
