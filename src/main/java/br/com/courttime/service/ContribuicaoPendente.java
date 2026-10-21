package br.com.courttime.service;

import br.com.courttime.entity.Reserva;

import java.math.BigDecimal;

/** Solicitação com documento validado, ainda sem contribuição registrada. */
public record ContribuicaoPendente(Reserva reserva, long participantes) {

    public BigDecimal valorDevido() {
        return ContribuicaoApmService.calcularValorDevido(participantes);
    }
}
