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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "auditorias_usuario", indexes = {
        @Index(name = "idx_auditorias_usuario_afetado_data",
                columnList = "usuario_afetado_id, realizada_em")
})
public class AuditoriaUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operador_id", nullable = false)
    private Usuario operador;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_afetado_id", nullable = false)
    private Usuario usuarioAfetado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OperacaoAuditoriaUsuario operacao;

    @Enumerated(EnumType.STRING)
    @Column(name = "perfil_anterior", length = 20)
    private Perfil perfilAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "perfil_novo", length = 20)
    private Perfil perfilNovo;

    @Column(name = "ativo_anterior")
    private Boolean ativoAnterior;

    @Column(name = "ativo_novo")
    private Boolean ativoNovo;

    @Column(name = "realizada_em", nullable = false, updatable = false)
    private Instant realizadaEm;

    @PrePersist
    void prePersist() {
        realizadaEm = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Usuario getOperador() {
        return operador;
    }

    public void setOperador(Usuario operador) {
        this.operador = operador;
    }

    public Usuario getUsuarioAfetado() {
        return usuarioAfetado;
    }

    public void setUsuarioAfetado(Usuario usuarioAfetado) {
        this.usuarioAfetado = usuarioAfetado;
    }

    public OperacaoAuditoriaUsuario getOperacao() {
        return operacao;
    }

    public void setOperacao(OperacaoAuditoriaUsuario operacao) {
        this.operacao = operacao;
    }

    public Perfil getPerfilAnterior() {
        return perfilAnterior;
    }

    public void setPerfilAnterior(Perfil perfilAnterior) {
        this.perfilAnterior = perfilAnterior;
    }

    public Perfil getPerfilNovo() {
        return perfilNovo;
    }

    public void setPerfilNovo(Perfil perfilNovo) {
        this.perfilNovo = perfilNovo;
    }

    public Boolean getAtivoAnterior() {
        return ativoAnterior;
    }

    public void setAtivoAnterior(Boolean ativoAnterior) {
        this.ativoAnterior = ativoAnterior;
    }

    public Boolean getAtivoNovo() {
        return ativoNovo;
    }

    public void setAtivoNovo(Boolean ativoNovo) {
        this.ativoNovo = ativoNovo;
    }

    public Instant getRealizadaEm() {
        return realizadaEm;
    }
}
