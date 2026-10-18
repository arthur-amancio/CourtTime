package br.com.courttime.controller.form;

import br.com.courttime.entity.CategoriaParticipante;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ParticipanteReservaForm {

    @NotBlank(message = "Informe o nome completo do participante")
    @Size(max = 150, message = "O nome completo deve possuir no máximo 150 caracteres")
    private String nomeCompleto;

    @NotBlank(message = "Informe o RM ou RG do participante")
    @Size(max = 30, message = "O RM ou RG deve possuir no máximo 30 caracteres")
    private String rmOuRg;

    @NotNull(message = "Selecione a categoria do participante")
    private CategoriaParticipante categoria;

    public String getNomeCompleto() {
        return nomeCompleto;
    }

    public void setNomeCompleto(String nomeCompleto) {
        this.nomeCompleto = nomeCompleto;
    }

    public String getRmOuRg() {
        return rmOuRg;
    }

    public void setRmOuRg(String rmOuRg) {
        this.rmOuRg = rmOuRg;
    }

    public CategoriaParticipante getCategoria() {
        return categoria;
    }

    public void setCategoria(CategoriaParticipante categoria) {
        this.categoria = categoria;
    }
}
