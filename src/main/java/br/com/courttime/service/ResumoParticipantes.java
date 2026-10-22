package br.com.courttime.service;

public record ResumoParticipantes(
        long quantidadeAlunos,
        long quantidadeOutros,
        long total,
        boolean minimoDeParticipantesAtendido) {
}
