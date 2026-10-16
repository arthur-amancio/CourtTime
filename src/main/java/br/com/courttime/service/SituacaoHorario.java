package br.com.courttime.service;

public enum SituacaoHorario {
    DISPONIVEL("Disponível"),
    BLOQUEADO("Indisponível"),
    RESERVADO("Reservado");

    private final String descricao;

    SituacaoHorario(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
