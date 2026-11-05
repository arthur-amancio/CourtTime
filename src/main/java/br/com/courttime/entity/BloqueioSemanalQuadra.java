package br.com.courttime.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Uma indisponibilidade semanal, sem gerar bloqueios para cada data futura.
 * A vigência inclui as duas datas; o horário final é exclusivo, permitindo
 * utilizar a quadra imediatamente depois da aula. Desativar preserva a regra.
 */
@Entity
@Table(name = "bloqueios_semanais_quadra", indexes = {
        @Index(name = "idx_bloqueios_semanais_quadra_dia", columnList = "quadra_id, dia_semana")
})
public class BloqueioSemanalQuadra {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private DiaSemana diaSemana;

    @NotNull
    @Column(nullable = false)
    private LocalTime horarioInicio;

    @NotNull
    @Column(nullable = false)
    private LocalTime horarioFim;

    @NotNull
    @Column(nullable = false)
    private LocalDate vigenciaInicio;

    @NotNull
    @Column(nullable = false)
    private LocalDate vigenciaFim;

    @NotBlank
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String motivo;

    @Column(nullable = false)
    private boolean ativo = true;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quadra_id", nullable = false)
    private Quadra quadra;

    /** Verifica uma data sem materializar ocorrências nem percorrer a vigência. */
    public boolean ocorreEm(LocalDate data) {
        return ativo && data != null && diaSemana != null
                && vigenciaInicio != null && vigenciaFim != null
                && !data.isBefore(vigenciaInicio) && !data.isAfter(vigenciaFim)
                && DiaSemana.from(data.getDayOfWeek()) == diaSemana;
    }

    @AssertTrue(message = "O bloqueio deve terminar depois do horário inicial")
    public boolean isIntervaloValido() {
        return horarioInicio != null && horarioFim != null && horarioInicio.isBefore(horarioFim);
    }

    @AssertTrue(message = "A vigência final não pode ser anterior à inicial")
    public boolean isVigenciaValida() {
        return vigenciaInicio != null && vigenciaFim != null && !vigenciaFim.isBefore(vigenciaInicio);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public Quadra getQuadra() {
        return quadra;
    }

    public void setQuadra(Quadra quadra) {
        this.quadra = quadra;
    }

}
