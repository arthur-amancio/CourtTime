package br.com.courttime.service;

import br.com.courttime.entity.Reserva;

import java.math.BigDecimal;

/**
 * Solicitação com documento validado, ainda sem contribuição registrada.
 * Numa remarcação (R04), traz o crédito da reserva cancelada de origem.
 */
public record ContribuicaoPendente(
        Reserva reserva,
        long participantes,
        Long reservaOrigemId,
        BigDecimal creditoDisponivel) {

    public ContribuicaoPendente(Reserva reserva, long participantes) {
        this(reserva, participantes, null, null);
    }

    public BigDecimal valorDevido() {
        return ContribuicaoApmService.calcularValorDevido(participantes);
    }

    public boolean temCredito() {
        return creditoDisponivel != null && creditoDisponivel.signum() > 0;
    }

    /** R04.2: o crédito usado nunca passa do valor devido. */
    public BigDecimal creditoAplicado() {
        return temCredito() ? creditoDisponivel.min(valorDevido()) : BigDecimal.ZERO.setScale(2);
    }

    /** R04.2: diferença a cobrar, nunca negativa. */
    public BigDecimal valorACobrar() {
        return valorDevido().subtract(creditoAplicado());
    }

    /** D2: sobra do crédito quando a remarcação tem menos participantes. Sem destino. */
    public BigDecimal saldoExcedente() {
        return temCredito() ? creditoDisponivel.subtract(creditoAplicado()) : BigDecimal.ZERO.setScale(2);
    }
}
