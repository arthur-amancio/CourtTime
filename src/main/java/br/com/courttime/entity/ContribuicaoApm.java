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
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "contribuicoes_apm",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_contribuicoes_apm_reserva",
                        columnNames = "reserva_id"),
                @UniqueConstraint(
                        name = "uk_contribuicoes_apm_conferencia",
                        columnNames = "conferencia_documento_id")
        },
        indexes = @Index(
                name = "idx_contribuicoes_apm_data",
                columnList = "registrada_em"))
public class ContribuicaoApm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reserva_id", nullable = false, updatable = false)
    private Reserva reserva;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conferencia_documento_id", nullable = false, updatable = false)
    private ConferenciaDocumento conferenciaDocumento;

    @NotNull
    @DecimalMin(value = "0.00", inclusive = false)
    @Digits(integer = 10, fraction = 2)
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    /**
     * R03: valor calculado no registro (R$ 2,00 por participante). Nulo nos
     * registros feitos antes desta regra.
     */
    @Digits(integer = 10, fraction = 2)
    @Column(name = "valor_devido", precision = 12, scale = 2)
    private BigDecimal valorDevido;

    @Size(max = 500)
    @Column(length = 500)
    private String observacoes;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registrada_por_id", nullable = false, updatable = false)
    private Usuario registradaPor;

    @Column(name = "registrada_em", nullable = false, updatable = false)
    private Instant registradaEm;

    @PrePersist
    void prePersist() {
        if (registradaEm == null) {
            registradaEm = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Reserva getReserva() {
        return reserva;
    }

    public void setReserva(Reserva reserva) {
        this.reserva = reserva;
    }

    public ConferenciaDocumento getConferenciaDocumento() {
        return conferenciaDocumento;
    }

    public void setConferenciaDocumento(ConferenciaDocumento conferenciaDocumento) {
        this.conferenciaDocumento = conferenciaDocumento;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public BigDecimal getValorDevido() {
        return valorDevido;
    }

    public void setValorDevido(BigDecimal valorDevido) {
        this.valorDevido = valorDevido;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    public Usuario getRegistradaPor() {
        return registradaPor;
    }

    public void setRegistradaPor(Usuario registradaPor) {
        this.registradaPor = registradaPor;
    }

    public Instant getRegistradaEm() {
        return registradaEm;
    }
}
