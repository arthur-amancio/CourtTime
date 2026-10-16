package br.com.courttime.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public record ConsultaDisponibilidade(
        LocalDate data,
        boolean quadraAtiva,
        List<HorarioDisponibilidade> horarios) {

    private static final DateTimeFormatter FORMATADOR_DATA = DateTimeFormatter.ofPattern(
            "EEEE, dd 'de' MMMM 'de' yyyy", new Locale("pt", "BR"));

    public ConsultaDisponibilidade {
        horarios = List.copyOf(horarios);
    }

    public LocalDate getData() {
        return data;
    }

    public boolean isQuadraAtiva() {
        return quadraAtiva;
    }

    public List<HorarioDisponibilidade> getHorarios() {
        return horarios;
    }

    public String getDataFormatada() {
        String dataFormatada = data.format(FORMATADOR_DATA);
        return Character.toUpperCase(dataFormatada.charAt(0)) + dataFormatada.substring(1);
    }
}
