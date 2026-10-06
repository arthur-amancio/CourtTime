package br.com.courttime.controller;

import br.com.courttime.config.SecurityConfig;
import br.com.courttime.entity.DiaSemana;
import br.com.courttime.entity.DisponibilidadeQuadra;
import br.com.courttime.entity.Quadra;
import br.com.courttime.service.QuadraService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(QuadraController.class)
@Import(SecurityConfig.class)
class QuadraControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private QuadraService quadraService;

    private Quadra quadra;

    @BeforeEach
    void setUp() {
        quadra = new Quadra();
        quadra.setNome("Quadra Poliesportiva");
        quadra.setDescricao("Quadra esportiva da ETEC.");
        quadra.setAtiva(true);
    }

    @Test
    void quadraRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/quadra"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void authenticatedUserSeesQuadraAndEmptyAvailabilityMessage() throws Exception {
        when(quadraService.obterQuadra()).thenReturn(quadra);
        when(quadraService.obterDisponibilidadesAtivas(quadra)).thenReturn(List.of());

        mockMvc.perform(get("/quadra").with(user("aluno@escola.test").roles("ALUNO")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Quadra Poliesportiva")))
                .andExpect(content().string(containsString("Quadra esportiva da ETEC.")))
                .andExpect(content().string(containsString("Disponível")))
                .andExpect(content().string(containsString(
                        "Nenhum horário de disponibilidade foi cadastrado.")));
    }

    @Test
    void authenticatedUserSeesRegisteredAvailability() throws Exception {
        DisponibilidadeQuadra disponibilidade = new DisponibilidadeQuadra();
        disponibilidade.setDiaSemana(DiaSemana.SEGUNDA);
        disponibilidade.setHorarioInicio(LocalTime.of(8, 0));
        disponibilidade.setHorarioFim(LocalTime.of(10, 0));
        disponibilidade.setAtiva(true);
        disponibilidade.setQuadra(quadra);

        when(quadraService.obterQuadra()).thenReturn(quadra);
        when(quadraService.obterDisponibilidadesAtivas(quadra)).thenReturn(List.of(disponibilidade));

        mockMvc.perform(get("/quadra").with(user("aluno@escola.test").roles("ALUNO")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Segunda-feira")))
                .andExpect(content().string(containsString("08:00 – 10:00")));
    }
}
