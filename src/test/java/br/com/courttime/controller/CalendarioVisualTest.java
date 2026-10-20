package br.com.courttime.controller;

import br.com.courttime.controller.CalendarioVisual.DiaCalendario;
import br.com.courttime.entity.DiaSemana;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CalendarioVisualTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 10, 20);       // terca-feira
    private static final LocalDate AMANHA = HOJE.plusDays(1);
    private static final Set<DiaSemana> SEMANA_UTIL = Set.of(
            DiaSemana.SEGUNDA, DiaSemana.TERCA, DiaSemana.QUARTA, DiaSemana.QUINTA, DiaSemana.SEXTA);

    @Test
    void monthGridStartsOnSundayAndContainsEveryDayOfTheMonth() {
        CalendarioVisual calendario = CalendarioVisual.montar(AMANHA, HOJE, AMANHA, SEMANA_UTIL);

        assertEquals("Outubro de 2026", calendario.tituloMes());
        List<DiaCalendario> dias = calendario.semanas().stream().flatMap(List::stream).filter(Objects::nonNull).toList();
        assertEquals(31, dias.size());
        // 1o de outubro de 2026 e quinta-feira: quatro casas vazias antes dele.
        assertNull(calendario.semanas().get(0).get(0));
        assertEquals(1, calendario.semanas().get(0).get(4).dia());
        assertTrue(calendario.semanas().stream().allMatch(semana -> semana.size() == 7));
    }

    @Test
    void operatingDaysComeFromWeeklyScheduleOnly() {
        CalendarioVisual calendario = CalendarioVisual.montar(AMANHA, HOJE, AMANHA, SEMANA_UTIL);

        DiaCalendario sabado = dia(calendario, LocalDate.of(2026, 10, 24));
        DiaCalendario segunda = dia(calendario, LocalDate.of(2026, 10, 26));
        assertFalse(sabado.funciona());
        assertTrue(segunda.funciona());
        assertTrue(sabado.rotuloAcessivel().contains("sem funcionamento cadastrado"));
        assertTrue(segunda.rotuloAcessivel().startsWith("Segunda-feira, 26 de outubro de 2026"));
    }

    @Test
    void datesBeforeMinimumAreNotSelectableForRequests() {
        CalendarioVisual calendario = CalendarioVisual.montar(AMANHA, HOJE, AMANHA, SEMANA_UTIL);

        assertFalse(dia(calendario, HOJE).habilitada());
        assertTrue(dia(calendario, HOJE).hoje());
        assertTrue(dia(calendario, AMANHA).habilitada());
        assertTrue(dia(calendario, AMANHA).selecionada());
        assertTrue(dia(calendario, HOJE).rotuloAcessivel().contains("indisponível para solicitação"));
        // Nao ha como voltar para um mes inteiramente antes da data minima.
        assertNull(calendario.mesAnterior());
        assertEquals(LocalDate.of(2026, 11, 1), calendario.mesSeguinte());
    }

    @Test
    void consultationAllowsAnyDateAndPreviousMonth() {
        CalendarioVisual calendario = CalendarioVisual.montar(HOJE, HOJE, null, SEMANA_UTIL);

        assertTrue(calendario.semanas().stream().flatMap(List::stream).filter(Objects::nonNull)
                .allMatch(DiaCalendario::habilitada));
        assertEquals(LocalDate.of(2026, 9, 1), calendario.mesAnterior());
    }

    @Test
    void stripShowsFourteenDaysFromTheFirstSelectableDate() {
        CalendarioVisual calendario = CalendarioVisual.montar(AMANHA, HOJE, AMANHA, SEMANA_UTIL);

        assertEquals(CalendarioVisual.DIAS_NA_FAIXA, calendario.faixa().size());
        assertEquals(AMANHA, calendario.faixa().get(0).data());
        assertEquals("qua", calendario.faixa().get(0).diaSemanaCurto());
        assertTrue(calendario.faixa().stream().allMatch(DiaCalendario::habilitada));
    }

    @Test
    void stripFollowsADistantSelectedDate() {
        LocalDate distante = LocalDate.of(2026, 12, 10);
        CalendarioVisual calendario = CalendarioVisual.montar(distante, HOJE, AMANHA, SEMANA_UTIL);

        assertTrue(calendario.faixa().stream().anyMatch(DiaCalendario::selecionada));
        assertEquals(distante.minusDays(3), calendario.faixa().get(0).data());
        assertNotNull(calendario.mesAnterior());
    }

    @Test
    void withoutWeeklyScheduleNoDayIsMarkedAsOperating() {
        CalendarioVisual calendario = CalendarioVisual.montar(AMANHA, HOJE, AMANHA, Set.of());

        assertTrue(calendario.faixa().stream().noneMatch(DiaCalendario::funciona));
        assertEquals(7, calendario.getDiasDaSemana().size());
        assertEquals("domingo", calendario.getDiasDaSemana().get(0));
    }

    private static DiaCalendario dia(CalendarioVisual calendario, LocalDate data) {
        return calendario.semanas().stream().flatMap(List::stream)
                .filter(d -> d != null && d.data().equals(data))
                .findFirst().orElseThrow();
    }
}
