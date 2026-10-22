package br.com.courttime.service;

import br.com.courttime.entity.Reserva;

import java.math.BigDecimal;

/** R11: reserva de hoje com pessoas acrescentadas ainda sem pagamento. */
public record ValorAMaisPendente(Reserva reserva, long pessoasAPagar, BigDecimal valor) {
}
