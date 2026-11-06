package br.com.courttime.controller.form;

import br.com.courttime.entity.DiaSemana;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalTime;

/** Recebe apenas os campos editáveis; quadra e estado ativo são definidos pelo serviço. */
public class BloqueioSemanalQuadraForm {
    @NotNull(message = "Informe o dia da semana")
    private DiaSemana diaSemana;
    @NotNull(message = "Informe o horário inicial")
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime horarioInicio;
    @NotNull(message = "Informe o horário final")
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime horarioFim;
    @NotNull(message = "Informe o início da vigência")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate vigenciaInicio;
    @NotNull(message = "Informe o fim da vigência")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate vigenciaFim;
    @NotBlank(message = "Informe o motivo do bloqueio")
    @Size(max = 200, message = "O motivo deve possuir no máximo 200 caracteres")
    private String motivo;

    @AssertTrue(message = "O bloqueio deve terminar depois do horário inicial")
    public boolean isIntervaloValido() {
        return horarioInicio == null || horarioFim == null || horarioInicio.isBefore(horarioFim);
    }

    @AssertTrue(message = "A vigência final não pode ser anterior à inicial")
    public boolean isVigenciaValida() {
        return vigenciaInicio == null || vigenciaFim == null || !vigenciaFim.isBefore(vigenciaInicio);
    }

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

    public LocalDate getVigenciaInicio() {
        return vigenciaInicio;
    }

    public void setVigenciaInicio(LocalDate vigenciaInicio) {
        this.vigenciaInicio = vigenciaInicio;
    }

    public LocalDate getVigenciaFim() {
        return vigenciaFim;
    }

    public void setVigenciaFim(LocalDate vigenciaFim) {
        this.vigenciaFim = vigenciaFim;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

}
