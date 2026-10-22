package br.com.courttime.service;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * R10 e R11: a conta da portaria no dia da reserva.
 *
 * presentes  = participantes marcados como presentes (da lista e acrescentados)
 * pagos      = participantes da lista do documento + pessoas já pagas a mais
 * a pagar    = presentes - pagos, nunca menor que zero
 */
public record ResumoPortaria(
        long presentes,
        long pagos,
        long pessoasAPagar,
        BigDecimal valorAPagar,
        BigDecimal valorRecebidoNaPortaria,
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

    /** A tela da segurança só mostra valores quando há algo a pagar ou já recebido. */
    public boolean mostrarValores() {
        return temValorAPagar() || valorRecebidoNaPortaria.signum() > 0;
    }
}
