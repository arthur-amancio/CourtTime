package br.com.courttime.entity;

/** R08.1: marcação feita pela portaria no dia da reserva. */
public enum PresencaParticipante {
    PRESENTE("Presente"),
    AUSENTE("Ausente");

    private final String descricao;

    PresencaParticipante(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
