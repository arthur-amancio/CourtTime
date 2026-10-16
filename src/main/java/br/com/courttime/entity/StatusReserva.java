package br.com.courttime.entity;

public enum StatusReserva {
    PENDENTE_DOCUMENTACAO("Pendente de documentação"),
    PENDENTE_ANALISE("Pendente de análise"),
    APROVADA("Aprovada"),
    REJEITADA("Rejeitada"),
    CANCELADA("Cancelada");

    private final String descricao;

    StatusReserva(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
