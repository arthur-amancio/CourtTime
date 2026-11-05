package br.com.courttime.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class BloqueioSemanalQuadraTest {
    @ParameterizedTest
    @CsvSource({"2026-11-01,false", "2026-11-02,true", "2026-11-03,false",
            "2026-12-14,true", "2026-12-18,false", "2026-12-21,false"})
    void respeitaDiaEDatasInclusivasSemGerarOcorrencias(String data, boolean esperado) {
        assertEquals(esperado, regra().ocorreEm(LocalDate.parse(data)));
    }

    @Test
    void desativadaNuncaOcorreEFinalInclusivoPodeSerUnicoDia() {
        var regra = regra();
        regra.setVigenciaFim(regra.getVigenciaInicio());
        assertEquals(true, regra.ocorreEm(regra.getVigenciaFim()));
        regra.setAtivo(false);
        assertFalse(regra.ocorreEm(regra.getVigenciaInicio()));
        assertFalse(regra.ocorreEm(null));
    }

    private BloqueioSemanalQuadra regra() {
        var regra = new BloqueioSemanalQuadra();
        regra.setDiaSemana(DiaSemana.SEGUNDA);
        regra.setVigenciaInicio(LocalDate.of(2026, 11, 2));
        regra.setVigenciaFim(LocalDate.of(2026, 12, 18));
        return regra;
    }
}
