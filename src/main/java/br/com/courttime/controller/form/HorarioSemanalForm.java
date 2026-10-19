package br.com.courttime.controller.form;

import br.com.courttime.entity.DiaSemana;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalTime;

public class HorarioSemanalForm {

    @NotNull(message = "Selecione o dia da semana")
    private DiaSemana diaSemana;

    @NotNull(message = "Informe o horário inicial")
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime horarioInicio;

    @NotNull(message = "Informe o horário de encerramento")
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime horarioFim;

    public DiaSemana getDiaSemana() {
        return diaSemana;
    }

    public void setDiaSemana(DiaSemana diaSemana) {
        this.diaSemana = diaSemana;
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

    @AssertTrue(message = "O encerramento deve ser posterior ao horário inicial")
    public boolean isIntervaloValido() {
        return horarioInicio == null || horarioFim == null || horarioInicio.isBefore(horarioFim);
    }
}
