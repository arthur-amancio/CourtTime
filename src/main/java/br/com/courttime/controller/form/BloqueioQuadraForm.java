package br.com.courttime.controller.form;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalTime;

public class BloqueioQuadraForm {

    @NotNull(message = "Informe a data do bloqueio")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate data;

    @NotNull(message = "Informe o horário inicial")
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime horarioInicio;

    @NotNull(message = "Informe o horário final")
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime horarioFim;

    @NotBlank(message = "Informe o motivo do bloqueio")
    @Size(max = 200, message = "O motivo deve possuir no máximo 200 caracteres")
    private String motivo;

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

    public LocalTime getHorarioFim() {
        return horarioFim;
    }

    public void setHorarioFim(LocalTime horarioFim) {
        this.horarioFim = horarioFim;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    @AssertTrue(message = "O bloqueio deve terminar depois do horário inicial")
    public boolean isIntervaloValido() {
        return horarioInicio == null || horarioFim == null || horarioInicio.isBefore(horarioFim);
    }
}
