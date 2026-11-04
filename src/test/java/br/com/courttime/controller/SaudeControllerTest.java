package br.com.courttime.controller;

import br.com.courttime.config.SecurityConfig;
import br.com.courttime.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SaudeController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "courttime.https-obrigatorio=true")
class SaudeControllerTest {
    @Autowired
    MockMvc mvc;
    @MockitoBean
    ApplicationAvailability disponibilidade;
    @MockitoBean
    UsuarioRepository usuarios;

    @Test
    void sondaPublicaNaoExpoeDetalhesNemRebaixaHttps() throws Exception {
        when(disponibilidade.getReadinessState()).thenReturn(ReadinessState.ACCEPTING_TRAFFIC);
        mvc.perform(get("/healthz")).andExpect(status().isOk()).andExpect(content().string("ok"))
                .andExpect(header().string("Cache-Control", "no-store"));
        mvc.perform(get("/healthz").secure(true)).andExpect(status().isOk());
    }

    @Test
    void naoAnunciaProntidaoAntesDosInicializadores() throws Exception {
        when(disponibilidade.getReadinessState()).thenReturn(ReadinessState.REFUSING_TRAFFIC);
        mvc.perform(get("/healthz")).andExpect(status().isServiceUnavailable());
    }

    @Test
    void paginasExigemHttpsESemTokenCsrfLoginERecusado() throws Exception {
        mvc.perform(get("/login")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("https://localhost/login"));
        mvc.perform(post("/login").secure(true).param("email", "nao-existe@exemplo.invalid"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/admin/usuarios").secure(true)).andExpect(status().is3xxRedirection());
    }

    @ParameterizedTest
    @ValueSource(ints = {10000, 18084, 8080})
    void portaHttpInternaRedirecionaParaHttpsPublicoSemCiclo(int porta) throws Exception {
        mvc.perform(get("http://localhost:" + porta + "/login?origem=demo"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("https://localhost/login?origem=demo"));
    }
}
