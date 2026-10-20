package br.com.courttime.controller;

import br.com.courttime.entity.DiaSemana;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Dados de apresentacao do calendario (opcao A aprovada no prototipo V2).
 *
 * Mostra apenas dia da semana, numero do dia, data selecionada e se o dia
 * possui funcionamento cadastrado na grade semanal. NAO informa vagas: a
 * situacao de cada periodo continua vindo da consulta do dia, e o servidor
 * revalida tudo no envio da solicitacao.
 *
 * Cada dia vira um link GET com ?data=AAAA-MM-DD, entao o calendario funciona
 * sem JavaScript.
 */
public record CalendarioVisual(
        String tituloMes,
        List<List<DiaCalendario>> semanas,
        List<DiaCalendario> faixa,
        LocalDate mesAnterior,
        LocalDate mesSeguinte) {

    public static final int DIAS_NA_FAIXA = 14;
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter EXTENSO = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", PT_BR);
    private static final DateTimeFormatter MES = DateTimeFormatter.ofPattern("MMMM 'de' yyyy", PT_BR);

    /**
     * Um dia do calendario.
     *
     * @param habilitada falso para datas anteriores a {@code dataMinima}
     */
    public record DiaCalendario(
            LocalDate data,
            int dia,
            String diaSemanaCurto,
            String rotuloAcessivel,
            boolean funciona,
            boolean selecionada,
            boolean hoje,
            boolean habilitada) {
    }

    /**
     * @param selecionada data exibida
     * @param hoje        data atual em America/Sao_Paulo
     * @param dataMinima  primeira data que pode ser escolhida (null = sem limite)
     * @param funcionamento dias da semana com horario semanal ativo
     */
    public static CalendarioVisual montar(
            LocalDate selecionada, LocalDate hoje, LocalDate dataMinima, Set<DiaSemana> funcionamento) {
        if (selecionada == null) {
            return null;
        }
        if (hoje == null) {
            hoje = dataMinima != null ? dataMinima : selecionada;
        }
        Set<DiaSemana> dias = funcionamento == null ? Set.of() : funcionamento;

        LocalDate primeiroDoMes = selecionada.withDayOfMonth(1);
        List<List<DiaCalendario>> semanas = new ArrayList<>();
        // Semana comeca no domingo, como no calendario escolar impresso.
        LocalDate cursor = primeiroDoMes.minusDays(primeiroDoMes.getDayOfWeek().getValue() % 7);
        while (!cursor.isAfter(primeiroDoMes.plusMonths(1).minusDays(1))) {
            List<DiaCalendario> semana = new ArrayList<>(7);
            for (int i = 0; i < 7; i++) {
                semana.add(cursor.getMonth() == primeiroDoMes.getMonth()
                        ? dia(cursor, selecionada, hoje, dataMinima, dias)
                        : null);
                cursor = cursor.plusDays(1);
            }
            semanas.add(Collections.unmodifiableList(semana));
        }

        LocalDate base = dataMinima != null && dataMinima.isAfter(hoje) ? dataMinima : hoje;
        LocalDate inicioFaixa = selecionada.isBefore(base) || !selecionada.isBefore(base.plusDays(DIAS_NA_FAIXA))
                ? selecionada.minusDays(3)
                : base;
        if (dataMinima != null && inicioFaixa.isBefore(dataMinima)) {
            inicioFaixa = dataMinima;
        }
        List<DiaCalendario> faixa = new ArrayList<>(DIAS_NA_FAIXA);
        for (int i = 0; i < DIAS_NA_FAIXA; i++) {
            faixa.add(dia(inicioFaixa.plusDays(i), selecionada, hoje, dataMinima, dias));
        }

        LocalDate anterior = primeiroDoMes.minusMonths(1);
        if (dataMinima != null && primeiroDoMes.minusDays(1).isBefore(dataMinima)) {
            anterior = null;
        }
        return new CalendarioVisual(
                capitalizar(primeiroDoMes.format(MES)),
                List.copyOf(semanas),
                List.copyOf(faixa),
                anterior,
                primeiroDoMes.plusMonths(1));
    }

    /** Cabecalhos das colunas, de domingo a sabado. */
    public List<String> getDiasDaSemana() {
        List<String> nomes = new ArrayList<>(7);
        DayOfWeek dia = DayOfWeek.SUNDAY;
        for (int i = 0; i < 7; i++) {
            nomes.add(dia.getDisplayName(TextStyle.FULL, PT_BR));
            dia = dia.plus(1);
        }
        return nomes;
    }

    private static DiaCalendario dia(
            LocalDate data, LocalDate selecionada, LocalDate hoje, LocalDate dataMinima, Set<DiaSemana> funcionamento) {
        boolean funciona = funcionamento.contains(DiaSemana.from(data.getDayOfWeek()));
        boolean habilitada = dataMinima == null || !data.isBefore(dataMinima);
        String rotulo = capitalizar(data.format(EXTENSO))
                + (funciona ? ", com funcionamento cadastrado" : ", sem funcionamento cadastrado")
                + (habilitada ? "" : ", indisponível para solicitação");
        String curto = data.getDayOfWeek().getDisplayName(TextStyle.SHORT, PT_BR).replace(".", "");
        return new DiaCalendario(data, data.getDayOfMonth(), curto, rotulo, funciona,
                data.equals(selecionada), data.equals(hoje), habilitada);
    }

    private static String capitalizar(String texto) {
        return texto.isEmpty() ? texto : Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }
}
