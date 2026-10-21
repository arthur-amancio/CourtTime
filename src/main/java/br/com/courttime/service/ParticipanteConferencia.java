package br.com.courttime.service;

import br.com.courttime.entity.PresencaParticipante;

/** O que a portaria vê de cada participante: sem CPF, e-mail, valor ou PDF. */
public record ParticipanteConferencia(
        Long id,
        String nomeCompleto,
        String rmOuRg,
        String categoria,
        PresencaParticipante presenca) {

    public ParticipanteConferencia(String nomeCompleto, String rmOuRg, String categoria) {
        this(null, nomeCompleto, rmOuRg, categoria, null);
    }
}
