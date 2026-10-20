package br.com.courttime.controller;

import br.com.courttime.config.SecurityConfig;
import br.com.courttime.repository.UsuarioRepository;
import br.com.courttime.service.ConsultaSegurancaService;
import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O MockMvc nao encaminha erros para /error como o servidor real faz; por isso
 * o teste chama /error com o codigo de status que o servidor informaria.
 */
@WebMvcTest(SegurancaReservaController.class)
@Import(SecurityConfig.class)
class PaginaDeErroTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConsultaSegurancaService consultaSegurancaService;
    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @Test
    void registroNaoEncontradoMostraPaginaEmPortugues() throws Exception {
        erro(404)
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("Não encontrado")))
                .andExpect(content().string(containsString("Voltar ao início")))
                .andExpect(content().string(not(containsString("Whitelabel"))));
    }

    @Test
    void acessoNegadoMostraPaginaEmPortugues() throws Exception {
        erro(403)
                .andExpect(status().isForbidden())
                .andExpect(content().string(containsString("Acesso não permitido")))
                .andExpect(content().string(not(containsString("Whitelabel"))));
    }

    @Test
    void erroInesperadoNaoExibeDetalhesTecnicos() throws Exception {
        mockMvc.perform(get("/error")
                        .with(user("seguranca@escola.test").roles("SEGURANCA"))
                        .accept(MediaType.TEXT_HTML)
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 500)
                        .requestAttr(RequestDispatcher.ERROR_EXCEPTION,
                                new IllegalStateException("detalhe interno do banco")))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("Não foi possível concluir")))
                .andExpect(content().string(not(containsString("detalhe interno do banco"))))
                .andExpect(content().string(not(containsString("IllegalStateException"))));
    }

    private ResultActions erro(int codigo) throws Exception {
        return mockMvc.perform(get("/error")
                .with(user("seguranca@escola.test").roles("SEGURANCA"))
                .accept(MediaType.TEXT_HTML)
                .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, codigo));
    }
}
