package br.com.courttime.controller;

import br.com.courttime.config.SecurityConfig;
import br.com.courttime.entity.Quadra;
import br.com.courttime.repository.UsuarioRepository;
import br.com.courttime.service.CalendarioDisponibilidadeService;
import br.com.courttime.service.ConsultaDisponibilidade;
import br.com.courttime.service.HorarioDisponibilidade;
import br.com.courttime.service.QuadraService;
import br.com.courttime.service.SituacaoHorario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verify;
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

    @MockitoBean
    private CalendarioDisponibilidadeService calendarioDisponibilidadeService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    private Quadra quadra;
    private LocalDate data;

    @BeforeEach
    void setUp() {
        quadra = new Quadra();
        quadra.setNome("Quadra Poliesportiva");
        quadra.setDescricao("Quadra esportiva da ETEC.");
        quadra.setAtiva(true);
        data = LocalDate.of(2026, 10, 12);
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
        when(calendarioDisponibilidadeService.obterDataAtual()).thenReturn(data);
        when(calendarioDisponibilidadeService.consultar(quadra, data))
                .thenReturn(new ConsultaDisponibilidade(data, true, List.of()));

        mockMvc.perform(get("/quadra").with(user("aluno@escola.test").roles("ALUNO")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Quadra Poliesportiva")))
                .andExpect(content().string(containsString("Quadra esportiva da ETEC.")))
                .andExpect(content().string(containsString("Ativa")))
                .andExpect(content().string(containsString(
                        "Não há horários de funcionamento cadastrados para esta data.")));
    }

    @Test
    void authenticatedUserCanConsultSpecificDate() throws Exception {
        HorarioDisponibilidade horario = new HorarioDisponibilidade(
                LocalTime.of(8, 0), LocalTime.of(9, 0), SituacaoHorario.DISPONIVEL);

        when(quadraService.obterQuadra()).thenReturn(quadra);
        when(calendarioDisponibilidadeService.consultar(quadra, data))
                .thenReturn(new ConsultaDisponibilidade(data, true, List.of(horario)));

        mockMvc.perform(get("/quadra")
                        .param("data", "2026-10-12")
                        .with(user("aluno@escola.test").roles("ALUNO")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Segunda-feira, 12 de outubro de 2026")))
                .andExpect(content().string(containsString("08:00 – 09:00")))
                .andExpect(content().string(containsString("Disponível")));

        verify(calendarioDisponibilidadeService).consultar(quadra, data);
    }

    @Test
    void inactiveCourtShowsOperationalStateWithoutAvailableHours() throws Exception {
        quadra.setAtiva(false);
        when(quadraService.obterQuadra()).thenReturn(quadra);
        when(calendarioDisponibilidadeService.obterDataAtual()).thenReturn(data);
        when(calendarioDisponibilidadeService.consultar(quadra, data))
                .thenReturn(new ConsultaDisponibilidade(data, false, List.of()));

        mockMvc.perform(get("/quadra").with(user("aluno@escola.test").roles("ALUNO")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Inativa")))
                .andExpect(content().string(containsString(
                        "A quadra está inativa. Não há horários disponíveis")));
    }

    @Test
    void invalidDateReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/quadra")
                        .param("data", "12-10-2026")
                        .with(user("aluno@escola.test").roles("ALUNO")))
                .andExpect(status().isBadRequest());
    }
}
