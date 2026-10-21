package br.com.courttime.entity;

/**
 * R09: vínculo obrigatório de quem participa na categoria Outro.
 * Amigos ou convidados sem um destes vínculos não podem participar.
 */
public enum VinculoParticipante {
    EX_ALUNO("Ex-aluno da ETEC", "Ex-aluno da ETEC"),
    PAI_OU_MAE("Pai ou mãe de aluno", "Pai/mãe de aluno"),
    IRMAO_OU_IRMA("Irmão ou irmã de aluno", "Irmão(ã) de aluno"),
    PROFESSOR("Professor da ETEC", "Professor da ETEC");

    private final String descricao;
    private final String textoFormulario;

    VinculoParticipante(String descricao, String textoFormulario) {
        this.descricao = descricao;
        this.textoFormulario = textoFormulario;
    }

    public String getDescricao() {
        return descricao;
    }

    /** Texto curto que cabe no espaço "( ) Outro: ____" do formulário oficial. */
    public String getTextoFormulario() {
        return textoFormulario;
    }
}
