package br.com.courttime.service;

import java.util.List;

public record DetalheReservaAutorizada(
        ReservaAutorizadaResumo reserva,
        List<ParticipanteConferencia> participantes,
        boolean presencaPermitida) {

    public DetalheReservaAutorizada {
        participantes = List.copyOf(participantes);
    }

    public DetalheReservaAutorizada(
            ReservaAutorizadaResumo reserva, List<ParticipanteConferencia> participantes) {
        this(reserva, participantes, false);
    }
}
