package br.com.courttime.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "ajustes_contribuicao_apm", indexes = @Index(
        name = "idx_ajustes_contribuicao_apm_historico",
        columnList = "contribuicao_id, ajustada_em"))
public class AjusteContribuicaoApm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contribuicao_id", nullable = false, updatable = false)
    private ContribuicaoApm contribuicao;

    @NotNull
    // Pode ser zero quando o crédito da remarcação cobriu todo o valor (R04.3).
    @DecimalMin(value = "0.00")
    @Digits(integer = 10, fraction = 2)
    @Column(name = "valor_anterior", nullable = false, precision = 12, scale = 2,
            updatable = false)
    private BigDecimal valorAnterior;

    @NotNull
    @DecimalMin(value = "0.00", inclusive = false)
    @Digits(integer = 10, fraction = 2)
    @Column(name = "valor_novo", nullable = false, precision = 12, scale = 2,
            updatable = false)
    private BigDecimal valorNovo;

    @Size(max = 500)
    @Column(name = "observacoes_anteriores", length = 500, updatable = false)
    private String observacoesAnteriores;

    @Size(max = 500)
    @Column(name = "observacoes_novas", length = 500, updatable = false)
    private String observacoesNovas;

    @NotBlank
    @Size(max = 500)
    @Column(nullable = false, length = 500, updatable = false)
    private String motivo;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ajustada_por_id", nullable = false, updatable = false)
    private Usuario ajustadaPor;

    @Column(name = "ajustada_em", nullable = false, updatable = false)
    private Instant ajustadaEm;

    @PrePersist
    void prePersist() {
        if (ajustadaEm == null) {
            ajustadaEm = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public ContribuicaoApm getContribuicao() {
        return contribuicao;
    }

    public void setContribuicao(ContribuicaoApm contribuicao) {
        this.contribuicao = contribuicao;
    }

    public BigDecimal getValorAnterior() {
        return valorAnterior;
    }

    public void setValorAnterior(BigDecimal valorAnterior) {
        this.valorAnterior = valorAnterior;
    }

    public BigDecimal getValorNovo() {
        return valorNovo;
    }

    public void setValorNovo(BigDecimal valorNovo) {
        this.valorNovo = valorNovo;
    }

    public String getObservacoesAnteriores() {
        return observacoesAnteriores;
    }

    public void setObservacoesAnteriores(String observacoesAnteriores) {
        this.observacoesAnteriores = observacoesAnteriores;
    }

    public String getObservacoesNovas() {
        return observacoesNovas;
    }

    public void setObservacoesNovas(String observacoesNovas) {
        this.observacoesNovas = observacoesNovas;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public Usuario getAjustadaPor() {
        return ajustadaPor;
    }

    public void setAjustadaPor(Usuario ajustadaPor) {
        this.ajustadaPor = ajustadaPor;
    }

    public Instant getAjustadaEm() {
        return ajustadaEm;
    }
}
