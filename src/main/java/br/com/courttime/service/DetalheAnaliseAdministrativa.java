package br.com.courttime.service;

import br.com.courttime.entity.ContribuicaoApm;
import br.com.courttime.entity.DocumentoReserva;
import br.com.courttime.entity.ParticipanteReserva;
import br.com.courttime.entity.Reserva;

import java.util.List;

public record DetalheAnaliseAdministrativa(
        Reserva solicitacao,
        List<ParticipanteReserva> participantes,
        ResumoParticipantes resumoParticipantes,
        DocumentoReserva documento,
        ContribuicaoApm contribuicao) {
}
