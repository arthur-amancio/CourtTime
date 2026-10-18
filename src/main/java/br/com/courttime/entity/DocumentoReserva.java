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
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

@Entity
@Table(name = "documentos_reserva",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_documentos_reserva_versao",
                columnNames = {"reserva_id", "versao"}),
        indexes = @Index(
                name = "idx_documentos_reserva",
                columnList = "reserva_id, versao"))
public class DocumentoReserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reserva_id", nullable = false, updatable = false)
    private Reserva reserva;

    @Min(1)
    @Column(nullable = false, updatable = false)
    private int versao;

    @NotBlank
    @Size(max = 255)
    @Column(name = "nome_original", nullable = false, length = 255, updatable = false)
    private String nomeOriginal;

    @NotBlank
    @Size(max = 80)
    @Column(name = "chave_armazenamento", nullable = false, length = 80,
            unique = true, updatable = false)
    private String chaveArmazenamento;

    @NotBlank
    @Size(min = 64, max = 64)
    @Column(name = "sha256", nullable = false, length = 64, updatable = false)
    private String sha256;

    @Min(1)
    @Column(name = "tamanho_bytes", nullable = false, updatable = false)
    private long tamanhoBytes;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enviado_por_id", nullable = false, updatable = false)
    private Usuario enviadoPor;

    @Column(name = "enviado_em", nullable = false, updatable = false)
    private Instant enviadoEm;

    @PrePersist
    void prePersist() {
        if (enviadoEm == null) {
            enviadoEm = Instant.now();
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

    public int getVersao() {
        return versao;
    }

    public void setVersao(int versao) {
        this.versao = versao;
    }

    public String getNomeOriginal() {
        return nomeOriginal;
    }

    public void setNomeOriginal(String nomeOriginal) {
        this.nomeOriginal = nomeOriginal;
    }

    public String getChaveArmazenamento() {
        return chaveArmazenamento;
    }

    public void setChaveArmazenamento(String chaveArmazenamento) {
        this.chaveArmazenamento = chaveArmazenamento;
    }

    public String getSha256() {
        return sha256;
    }

    public void setSha256(String sha256) {
        this.sha256 = sha256;
    }

    public long getTamanhoBytes() {
        return tamanhoBytes;
    }

    public void setTamanhoBytes(long tamanhoBytes) {
        this.tamanhoBytes = tamanhoBytes;
    }

    public Usuario getEnviadoPor() {
        return enviadoPor;
    }

    public void setEnviadoPor(Usuario enviadoPor) {
        this.enviadoPor = enviadoPor;
    }

    public Instant getEnviadoEm() {
        return enviadoEm;
    }
}
