package br.com.courttime.controller.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalTime;

public class SolicitacaoReservaForm {

    @NotNull(message = "Selecione a data de utilização")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate data;

    @NotNull(message = "Selecione um horário disponível")
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime horarioInicio;

    @NotBlank(message = "Informe a finalidade da utilização")
    @Size(max = 300, message = "A finalidade deve possuir no máximo 300 caracteres")
    private String finalidade;

    /** Reserva cancelada que esta solicitação remarca; vazio numa solicitação comum. */
    private Long origemId;

    private boolean copiarParticipantes;

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public LocalTime getHorarioInicio() {
        return horarioInicio;
    }

    public void setHorarioInicio(LocalTime horarioInicio) {
        this.horarioInicio = horarioInicio;
    }

    public String getFinalidade() {
        return finalidade;
    }

    public void setFinalidade(String finalidade) {
        this.finalidade = finalidade;
    }

    public Long getOrigemId() {
        return origemId;
    }

    public void setOrigemId(Long origemId) {
        this.origemId = origemId;
    }

    public boolean isCopiarParticipantes() {
        return copiarParticipantes;
    }

    public void setCopiarParticipantes(boolean copiarParticipantes) {
        this.copiarParticipantes = copiarParticipantes;
    }
}
