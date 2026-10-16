package br.com.courttime.service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public record HorarioDisponibilidade(
        LocalTime horarioInicio,
        LocalTime horarioFim,
        SituacaoHorario situacao) {

    private static final DateTimeFormatter FORMATADOR_HORARIO = DateTimeFormatter.ofPattern("HH:mm");

    public SituacaoHorario getSituacao() {
        return situacao;
    }

    public boolean isDisponivel() {
        return situacao == SituacaoHorario.DISPONIVEL;
    }

    public String getPeriodoFormatado() {
        return horarioInicio.format(FORMATADOR_HORARIO)
                + " – "
                + horarioFim.format(FORMATADOR_HORARIO);
    }
}
