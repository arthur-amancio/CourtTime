package br.com.courttime.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "reservas", indexes = {
        @Index(name = "idx_reservas_quadra_data_status",
                columnList = "quadra_id, data_utilizacao, status"),
        @Index(name = "idx_reservas_solicitante", columnList = "solicitante_id")
})
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quadra_id", nullable = false)
    private Quadra quadra;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitante_id", nullable = false)
    private Usuario solicitante;

    @NotNull
    @Column(name = "data_utilizacao", nullable = false)
    private LocalDate dataUtilizacao;

    @NotNull
    @Column(nullable = false)
    private LocalTime horarioInicio;

    @NotNull
    @Column(nullable = false)
    private LocalTime horarioFim;

    @NotBlank
    @Size(max = 300)
    @Column(nullable = false, length = 300)
    private String finalidade;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusReserva status = StatusReserva.PENDENTE_DOCUMENTACAO;

    @PositiveOrZero
    @Column(name = "revisao_participantes", nullable = false,
            columnDefinition = "integer default 0")
    private int revisaoParticipantes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analisada_por_id")
    private Usuario analisadaPor;

    @Column(length = 500)
    private String motivoRejeicao;

    @Column(name = "motivo_cancelamento_institucional", length = 500)
    private String motivoCancelamentoInstitucional;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelada_por_id")
    private Usuario canceladaPor;

    /**
     * Solicitação cancelada que esta remarca, quando houver. A anterior não é
     * alterada: o vínculo só registra de onde a nova solicitação veio. Cada
     * cancelamento pode ser remarcado uma única vez (coluna única).
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "remarcacao_de_id", unique = true)
    private Reserva remarcacaoDe;

    @Column(nullable = false, updatable = false)
    private Instant criadaEm;

    @Column(nullable = false)
    private Instant atualizadaEm;

    private Instant analisadaEm;

    private Instant canceladaEm;

    /** R11: quando e por quem a portaria liberou a quadra. Nulo até a liberação. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "liberada_por_id")
    private Usuario liberadaPor;

    @Column(name = "liberada_em")
    private Instant liberadaEm;

    @PrePersist
    void prePersist() {
        Instant agora = Instant.now();
        criadaEm = agora;
        atualizadaEm = agora;
    }

    @PreUpdate
    void preUpdate() {
        atualizadaEm = Instant.now();
    }

    @AssertTrue(message = "A reserva deve possuir duração de uma hora")
    public boolean isIntervaloValido() {
        return horarioInicio != null
                && horarioFim != null
                && horarioInicio.isBefore(horarioFim)
                && Duration.between(horarioInicio, horarioFim).equals(Duration.ofHours(1));
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Quadra getQuadra() {
        return quadra;
    }

    public void setQuadra(Quadra quadra) {
        this.quadra = quadra;
    }

    public Usuario getSolicitante() {
        return solicitante;
    }

    public void setSolicitante(Usuario solicitante) {
        this.solicitante = solicitante;
    }

    public LocalDate getDataUtilizacao() {
        return dataUtilizacao;
    }

    public void setDataUtilizacao(LocalDate dataUtilizacao) {
        this.dataUtilizacao = dataUtilizacao;
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

    public String getFinalidade() {
        return finalidade;
    }

    public void setFinalidade(String finalidade) {
        this.finalidade = finalidade;
    }

    public StatusReserva getStatus() {
        return status;
    }

    public void setStatus(StatusReserva status) {
        this.status = status;
    }

    public int getRevisaoParticipantes() {
        return revisaoParticipantes;
    }

    public void setRevisaoParticipantes(int revisaoParticipantes) {
        if (revisaoParticipantes < 0) {
            throw new IllegalArgumentException("A revisão dos participantes não pode ser negativa");
        }
        this.revisaoParticipantes = revisaoParticipantes;
    }

    public void incrementarRevisaoParticipantes() {
        revisaoParticipantes++;
    }

    public Usuario getAnalisadaPor() {
        return analisadaPor;
    }

    public void setAnalisadaPor(Usuario analisadaPor) {
        this.analisadaPor = analisadaPor;
    }

    public String getMotivoRejeicao() {
        return motivoRejeicao;
    }

    public void setMotivoRejeicao(String motivoRejeicao) {
        this.motivoRejeicao = motivoRejeicao;
    }

    public String getMotivoCancelamentoInstitucional() {
        return motivoCancelamentoInstitucional;
    }

    public void setMotivoCancelamentoInstitucional(String motivoCancelamentoInstitucional) {
        this.motivoCancelamentoInstitucional = motivoCancelamentoInstitucional;
    }

    public Usuario getCanceladaPor() {
        return canceladaPor;
    }

    public void setCanceladaPor(Usuario canceladaPor) {
        this.canceladaPor = canceladaPor;
    }

    public Reserva getRemarcacaoDe() {
        return remarcacaoDe;
    }

    public void setRemarcacaoDe(Reserva remarcacaoDe) {
        this.remarcacaoDe = remarcacaoDe;
    }

    public Instant getCriadaEm() {
        return criadaEm;
    }

    public Instant getAtualizadaEm() {
        return atualizadaEm;
    }

    public Instant getAnalisadaEm() {
        return analisadaEm;
    }

    public void setAnalisadaEm(Instant analisadaEm) {
        this.analisadaEm = analisadaEm;
    }

    public Instant getCanceladaEm() {
        return canceladaEm;
    }

    public void setCanceladaEm(Instant canceladaEm) {
        this.canceladaEm = canceladaEm;
    }

    public Usuario getLiberadaPor() {
        return liberadaPor;
    }

    public Instant getLiberadaEm() {
        return liberadaEm;
    }

    public boolean isLiberada() {
        return liberadaEm != null;
    }

    public void registrarLiberacao(Usuario seguranca, Instant instante) {
        this.liberadaPor = seguranca;
        this.liberadaEm = instante;
    }
}
