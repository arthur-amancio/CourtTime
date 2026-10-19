package br.com.courttime.service;

import java.time.LocalDate;
import java.time.LocalTime;

public record ReservaAutorizadaResumo(
        Long id,
        LocalDate dataUtilizacao,
        LocalTime horarioInicio,
        LocalTime horarioFim,
        String quadra,
        String solicitante,
        long quantidadeParticipantes) {
}
