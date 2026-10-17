package br.com.courttime.service;

import br.com.courttime.entity.BloqueioQuadra;
import br.com.courttime.entity.DisponibilidadeQuadra;
import br.com.courttime.entity.Quadra;

import java.util.List;

public record PainelAdministrativo(
        Quadra quadra,
        List<DisponibilidadeQuadra> horarios,
        List<BloqueioQuadra> bloqueiosFuturos,
        long totalSolicitacoes,
        long pendentesDocumentacao,
        long pendentesAnalise,
        long aprovadas,
        long rejeitadas) {
}
