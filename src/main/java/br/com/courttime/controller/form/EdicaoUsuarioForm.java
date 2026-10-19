package br.com.courttime.controller.form;

import br.com.courttime.entity.Perfil;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class EdicaoUsuarioForm {

    @NotBlank(message = "Informe o nome completo")
    @Size(max = 150, message = "O nome deve possuir no máximo 150 caracteres")
    private String nome;

    @NotBlank(message = "Informe o email")
    @Email(message = "Informe um email válido")
    @Size(max = 254, message = "O email deve possuir no máximo 254 caracteres")
    private String email;

    @NotNull(message = "Selecione o perfil")
    private Perfil perfil;

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
}
