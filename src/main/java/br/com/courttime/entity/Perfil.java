package br.com.courttime.entity;

public enum Perfil {
    ALUNO("Aluno"),
    ADMINISTRADOR("Administrador"),
    SECRETARIA("Secretaria"),
    SEGURANCA("Segurança");

    private final String descricao;

    Perfil(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
