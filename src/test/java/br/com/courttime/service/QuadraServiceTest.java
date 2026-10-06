package br.com.courttime.service;

import br.com.courttime.entity.DiaSemana;
import br.com.courttime.entity.DisponibilidadeQuadra;
import br.com.courttime.entity.Quadra;
import br.com.courttime.repository.DisponibilidadeQuadraRepository;
import br.com.courttime.repository.QuadraRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuadraServiceTest {

    @Mock
    private QuadraRepository quadraRepository;

    @Mock
    private DisponibilidadeQuadraRepository disponibilidadeQuadraRepository;

    @Test
    void ordersAvailabilityByWeekdayAndStartTime() {
        Quadra quadra = new Quadra();
        DisponibilidadeQuadra quartaAsDez = disponibilidade(DiaSemana.QUARTA, 10);
        DisponibilidadeQuadra segundaAsNove = disponibilidade(DiaSemana.SEGUNDA, 9);
        DisponibilidadeQuadra segundaAsOito = disponibilidade(DiaSemana.SEGUNDA, 8);
        when(disponibilidadeQuadraRepository.findByQuadraAndAtivaTrue(quadra))
                .thenReturn(List.of(quartaAsDez, segundaAsNove, segundaAsOito));
        QuadraService quadraService = new QuadraService(quadraRepository, disponibilidadeQuadraRepository);

        List<DisponibilidadeQuadra> resultado = quadraService.obterDisponibilidadesAtivas(quadra);

        assertEquals(List.of(segundaAsOito, segundaAsNove, quartaAsDez), resultado);
    }

    private DisponibilidadeQuadra disponibilidade(DiaSemana diaSemana, int horaInicio) {
        DisponibilidadeQuadra disponibilidade = new DisponibilidadeQuadra();
        disponibilidade.setDiaSemana(diaSemana);
        disponibilidade.setHorarioInicio(LocalTime.of(horaInicio, 0));
        disponibilidade.setHorarioFim(LocalTime.of(horaInicio + 1, 0));
        disponibilidade.setAtiva(true);
        return disponibilidade;
    }
}
