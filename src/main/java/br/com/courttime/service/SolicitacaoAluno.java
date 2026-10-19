package br.com.courttime.service;

import br.com.courttime.entity.Reserva;

public record SolicitacaoAluno(
        Reserva solicitacao,
        SituacaoSolicitacao situacao) {
}
