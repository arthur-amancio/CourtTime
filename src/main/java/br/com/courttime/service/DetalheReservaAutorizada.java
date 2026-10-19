package br.com.courttime.service;

import java.util.List;

public record DetalheReservaAutorizada(
        ReservaAutorizadaResumo reserva,
        List<ParticipanteConferencia> participantes) {

    public DetalheReservaAutorizada {
        participantes = List.copyOf(participantes);
    }
}
