package br.com.courttime.service;

import br.com.courttime.entity.AjusteContribuicaoApm;
import br.com.courttime.entity.ContribuicaoApm;

import java.math.BigDecimal;
import java.util.List;

public record DetalheContribuicaoApm(
        ContribuicaoApm contribuicao,
        List<AjusteContribuicaoApm> ajustes) {

    public DetalheContribuicaoApm {
        ajustes = List.copyOf(ajustes);
    }

    /** Participantes cobrados no registro, ou nulo em registros anteriores à R03. */
    public Long participantesCobrados() {
        BigDecimal devido = contribuicao.getValorDevido();
        return devido == null
                ? null
                : devido.divide(ContribuicaoApmService.VALOR_POR_PARTICIPANTE).longValue();
    }
}
