package br.com.courttime.service;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * R10 e R11: a conta da portaria no dia da reserva.
 *
 * presentes  = participantes marcados como presentes (da lista e acrescentados)
 * pagos      = participantes da lista do documento + pessoas já pagas a mais
 * a pagar    = presentes - pagos, nunca menor que zero. O total pago não é
 *              exposto ao perfil Segurança; somente o saldo devido é exibido.
 */
public record ResumoPortaria(
        long presentes,
        long pessoasAPagar,
        BigDecimal valorAPagar,
        Instant liberadaEm) {

    public boolean minimoAtingido() {
        return presentes >= PortariaService.MINIMO_DE_PRESENTES;
    }

    public boolean temValorAPagar() {
        return pessoasAPagar > 0;
    }

    public boolean liberada() {
        return liberadaEm != null;
    }

    /** A segurança só vê o complemento que ainda precisa ser pago. */
    public boolean mostrarValores() {
        return temValorAPagar();
    }
}
