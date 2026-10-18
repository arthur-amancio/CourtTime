package br.com.courttime.entity;

public enum ResultadoConferenciaDocumento {
    VALIDADO("Validado"),
    DEVOLVIDO_CORRECAO("Devolvido para correção");

    private final String descricao;

    ResultadoConferenciaDocumento(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
