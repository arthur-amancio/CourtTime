package br.com.courttime.entity;

/** Onde o valor das pessoas acrescentadas na portaria foi pago. */
public enum LocalPagamento {
    SECRETARIA("Na secretaria"),
    PORTARIA("Na portaria, por acordo");

    private final String descricao;

    LocalPagamento(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
