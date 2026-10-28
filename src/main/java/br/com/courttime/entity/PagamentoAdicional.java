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

import java.math.BigDecimal;
import java.time.Instant;

/**
 * R11: valor pago pelas pessoas acrescentadas na portaria (R$ 2,00 por pessoa
 * a mais). Pode ser pago na secretaria ou, se ela estiver fechada, entregue à
 * segurança por acordo; nesse caso a secretaria confirma o repasse depois.
 */
@Entity
@Table(name = "pagamentos_adicionais", indexes = {
        @Index(name = "idx_pagamentos_adicionais_reserva", columnList = "reserva_id")
})
public class PagamentoAdicional {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reserva_id", nullable = false)
    private Reserva reserva;

    /** Quantas pessoas a mais este pagamento cobre. */
    @Column(name = "quantidade_pessoas", nullable = false)
    private int quantidadePessoas;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private LocalPagamento local;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registrado_por_id", nullable = false)
    private Usuario registradoPor;

    @Column(name = "registrado_em", nullable = false)
    private Instant registradoEm;

    /** Só para pagamento recebido na portaria: a secretaria confirma que recebeu o dinheiro. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repasse_confirmado_por_id")
    private Usuario repasseConfirmadoPor;

    @Column(name = "repasse_confirmado_em")
    private Instant repasseConfirmadoEm;

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

    public int getQuantidadePessoas() {
        return quantidadePessoas;
    }

    public void setQuantidadePessoas(int quantidadePessoas) {
        this.quantidadePessoas = quantidadePessoas;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public LocalPagamento getLocal() {
        return local;
    }

    public void setLocal(LocalPagamento local) {
        this.local = local;
    }

    public Usuario getRegistradoPor() {
        return registradoPor;
    }

    public void setRegistradoPor(Usuario registradoPor) {
        this.registradoPor = registradoPor;
    }

    public Instant getRegistradoEm() {
        return registradoEm;
    }

    public void setRegistradoEm(Instant registradoEm) {
        this.registradoEm = registradoEm;
    }

    public Usuario getRepasseConfirmadoPor() {
        return repasseConfirmadoPor;
    }

    public Instant getRepasseConfirmadoEm() {
        return repasseConfirmadoEm;
    }

    public void confirmarRepasse(Usuario secretaria, Instant instante) {
        this.repasseConfirmadoPor = secretaria;
        this.repasseConfirmadoEm = instante;
    }

    public boolean aguardaRepasse() {
        return local == LocalPagamento.PORTARIA && repasseConfirmadoEm == null;
    }
}
