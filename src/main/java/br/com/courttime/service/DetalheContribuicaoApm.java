package br.com.courttime.service;

import br.com.courttime.entity.AjusteContribuicaoApm;
import br.com.courttime.entity.ContribuicaoApm;

import java.math.BigDecimal;
import java.util.List;

/**
 * Registro da contribuição com o histórico de ajustes. Na remarcação (R04),
 * traz também a reserva cancelada de onde veio o crédito e o saldo que sobrou.
 */
public record DetalheContribuicaoApm(
        ContribuicaoApm contribuicao,
        List<AjusteContribuicaoApm> ajustes,
        Long reservaOrigemId,
        BigDecimal saldoExcedente) {

    public DetalheContribuicaoApm {
        ajustes = List.copyOf(ajustes);
    }

    public DetalheContribuicaoApm(ContribuicaoApm contribuicao, List<AjusteContribuicaoApm> ajustes) {
        this(contribuicao, ajustes, null, null);
    }

    /** Participantes cobrados no registro, ou nulo em registros anteriores à R03. */
    public Long participantesCobrados() {
        BigDecimal devido = contribuicao.getValorDevido();
        return devido == null
                ? null
                : devido.divide(ContribuicaoApmService.VALOR_POR_PARTICIPANTE).longValue();
    }
}
