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
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "participantes_reserva", indexes = {
        @Index(name = "idx_participantes_reserva", columnList = "reserva_id")
})
public class ParticipanteReserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reserva_id", nullable = false)
    private Reserva reserva;

    @NotBlank
    @Size(max = 150)
    @Column(name = "nome_completo", nullable = false, length = 150)
    private String nomeCompleto;

    @NotBlank
    @Size(max = 30)
    @Column(name = "rm_ou_rg", nullable = false, length = 30)
    private String rmOuRg;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private CategoriaParticipante categoria;

    /** R09: obrigatório na categoria Outro; nulo para Aluno e nos registros antigos. */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private VinculoParticipante vinculo;

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

    public String getNomeCompleto() {
        return nomeCompleto;
    }

    public void setNomeCompleto(String nomeCompleto) {
        this.nomeCompleto = nomeCompleto;
    }

    public String getRmOuRg() {
        return rmOuRg;
    }

    public void setRmOuRg(String rmOuRg) {
        this.rmOuRg = rmOuRg;
    }

    public CategoriaParticipante getCategoria() {
        return categoria;
    }

    public void setCategoria(CategoriaParticipante categoria) {
        this.categoria = categoria;
    }

    public VinculoParticipante getVinculo() {
        return vinculo;
    }

    public void setVinculo(VinculoParticipante vinculo) {
        this.vinculo = vinculo;
    }

    /** Texto do vínculo nas telas; registros antigos aparecem como não informado. */
    public String getDescricaoVinculo() {
        if (categoria != CategoriaParticipante.OUTRO) {
            return null;
        }
        return vinculo == null ? "Vínculo não informado" : vinculo.getDescricao();
    }
}
