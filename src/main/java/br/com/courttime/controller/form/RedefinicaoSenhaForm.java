package br.com.courttime.controller.form;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RedefinicaoSenhaForm {

    @NotBlank(message = "Informe a nova senha")
    @Size(min = 8, max = 72, message = "A senha deve possuir entre 8 e 72 caracteres")
    private String novaSenha;

    @NotBlank(message = "Confirme a nova senha")
    private String confirmacaoSenha;

    public String getNovaSenha() {
        return novaSenha;
    }

    public void setNovaSenha(String novaSenha) {
        this.novaSenha = novaSenha;
    }

    public String getConfirmacaoSenha() {
        return confirmacaoSenha;
    }

    public void setConfirmacaoSenha(String confirmacaoSenha) {
        this.confirmacaoSenha = confirmacaoSenha;
    }

    @AssertTrue(message = "A confirmação deve ser igual à nova senha")
    public boolean isConfirmacaoValida() {
        return novaSenha == null || confirmacaoSenha == null
                || novaSenha.equals(confirmacaoSenha);
    }
}
