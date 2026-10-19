package br.com.courttime.controller.form;

import br.com.courttime.entity.Perfil;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CadastroUsuarioForm {

    @NotBlank(message = "Informe o nome completo")
    @Size(max = 150, message = "O nome deve possuir no máximo 150 caracteres")
    private String nome;

    @NotBlank(message = "Informe o email")
    @Email(message = "Informe um email válido")
    @Size(max = 254, message = "O email deve possuir no máximo 254 caracteres")
    private String email;

    @NotNull(message = "Selecione o perfil")
    private Perfil perfil;

    @NotBlank(message = "Informe a senha inicial")
    @Size(min = 8, max = 72, message = "A senha deve possuir entre 8 e 72 caracteres")
    private String senha;

    @NotBlank(message = "Confirme a senha inicial")
    private String confirmacaoSenha;

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Perfil getPerfil() {
        return perfil;
    }

    public void setPerfil(Perfil perfil) {
        this.perfil = perfil;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public String getConfirmacaoSenha() {
        return confirmacaoSenha;
    }

    public void setConfirmacaoSenha(String confirmacaoSenha) {
        this.confirmacaoSenha = confirmacaoSenha;
    }

    @AssertTrue(message = "A confirmação deve ser igual à senha inicial")
    public boolean isConfirmacaoValida() {
        return senha == null || confirmacaoSenha == null || senha.equals(confirmacaoSenha);
    }
}
