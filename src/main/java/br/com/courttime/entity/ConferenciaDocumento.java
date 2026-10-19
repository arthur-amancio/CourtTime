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
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

@Entity
@Table(name = "conferencias_documento", indexes = {
        @Index(name = "idx_conferencias_documento_data", columnList = "conferido_em")
})
public class ConferenciaDocumento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "documento_id", nullable = false, unique = true, updatable = false)
    private DocumentoReserva documento;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, updatable = false)
    private ResultadoConferenciaDocumento resultado;

    @Size(max = 500)
    @Column(length = 500, updatable = false)
    private String justificativa;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conferido_por_id", nullable = false, updatable = false)
    private Usuario conferidoPor;

    @Column(name = "conferido_em", nullable = false, updatable = false)
    private Instant conferidoEm;

    @PrePersist
    void prePersist() {
        if (conferidoEm == null) {
            conferidoEm = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public DocumentoReserva getDocumento() {
        return documento;
    }

    public void setDocumento(DocumentoReserva documento) {
        this.documento = documento;
    }

    public ResultadoConferenciaDocumento getResultado() {
        return resultado;
    }

    public void setResultado(ResultadoConferenciaDocumento resultado) {
        this.resultado = resultado;
    }

    public String getJustificativa() {
        return justificativa;
    }

    public void setJustificativa(String justificativa) {
        this.justificativa = justificativa;
    }

    public Usuario getConferidoPor() {
        return conferidoPor;
    }

    public void setConferidoPor(Usuario conferidoPor) {
        this.conferidoPor = conferidoPor;
    }

    public Instant getConferidoEm() {
        return conferidoEm;
    }
}
