package br.com.courttime.service;

import java.util.List;

/**
 * O que a portaria vê de uma reserva aprovada. O resumo da portaria (R10 e R11)
 * só existe no dia da reserva; nos outros dias fica nulo.
 */
public record DetalheReservaAutorizada(
        ReservaAutorizadaResumo reserva,
        List<ParticipanteConferencia> participantes,
        boolean presencaPermitida,
        ResumoPortaria portaria) {

    public DetalheReservaAutorizada {
        participantes = List.copyOf(participantes);
    }

    public DetalheReservaAutorizada(
            ReservaAutorizadaResumo reserva,
            List<ParticipanteConferencia> participantes,
            boolean presencaPermitida) {
        this(reserva, participantes, presencaPermitida, null);
    }

    public DetalheReservaAutorizada(
            ReservaAutorizadaResumo reserva, List<ParticipanteConferencia> participantes) {
        this(reserva, participantes, false, null);
    }
}
