package br.com.courttime.controller;

import br.com.courttime.config.SecurityConfig;
import br.com.courttime.entity.Perfil;
import br.com.courttime.entity.Usuario;
import br.com.courttime.repository.UsuarioRepository;
import br.com.courttime.service.FormularioOficialService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FormularioOficialController.class)
@Import(SecurityConfig.class)
class FormularioOficialControllerTest {

    private static final byte[] PDF = "%PDF-1.7 teste".getBytes();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FormularioOficialService formularioService;
    @MockitoBean
    private UsuarioRepository usuarioRepository;

    private Usuario aluno;

    @BeforeEach
    void setUp() {
        aluno = new Usuario();
        aluno.setId(10L);
        aluno.setNome("Ana");
        aluno.setEmail("ana@escola.test");
        aluno.setSenha("hash");
        aluno.setPerfil(Perfil.ALUNO);
        aluno.setAtivo(true);
    }

    @Test
    void alunoBaixaOFormularioPreenchidoSemCache() throws Exception {
        when(usuarioRepository.findByEmailIgnoreCase(aluno.getEmail())).thenReturn(Optional.of(aluno));
        when(formularioService.gerar(30L, aluno, "123.456.789-09", "Informática")).thenReturn(PDF);

        mockMvc.perform(post("/solicitacoes/30/formulario-oficial")
                        .with(user(aluno.getEmail()).roles("ALUNO"))
                        .with(csrf())
                        .param("cpf", "123.456.789-09")
                        .param("curso", "Informática"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(header().string("Content-Disposition",
                        containsString("formulario-reserva-quadra-solicitacao-30.pdf")))
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(content().bytes(PDF));
    }

    @Test
    void dadoInvalidoVoltaParaDocumentosComAMensagem() throws Exception {
        when(usuarioRepository.findByEmailIgnoreCase(aluno.getEmail())).thenReturn(Optional.of(aluno));
        when(formularioService.gerar(30L, aluno, "111", "Informática"))
                .thenThrow(new IllegalArgumentException("Informe um CPF válido"));

        mockMvc.perform(post("/solicitacoes/30/formulario-oficial")
                        .with(user(aluno.getEmail()).roles("ALUNO"))
                        .with(csrf())
                        .param("cpf", "111")
                        .param("curso", "Informática"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/solicitacoes/30/documentos"))
                .andExpect(flash().attribute("mensagemErro", "Informe um CPF válido"));
    }

    @Test
    void reservaDeOutroAlunoRespondeNaoEncontrado() throws Exception {
        when(usuarioRepository.findByEmailIgnoreCase(aluno.getEmail())).thenReturn(Optional.of(aluno));
        when(formularioService.gerar(99L, aluno, "123.456.789-09", "Informática"))
                .thenThrow(new AccessDeniedException("não pertence"));

        mockMvc.perform(post("/solicitacoes/99/formulario-oficial")
                        .with(user(aluno.getEmail()).roles("ALUNO"))
                        .with(csrf())
                        .param("cpf", "123.456.789-09")
                        .param("curso", "Informática"))
                .andExpect(status().isNotFound());
    }

    @Test
    void semTokenCsrfERecusado() throws Exception {
        mockMvc.perform(post("/solicitacoes/30/formulario-oficial")
                        .with(user(aluno.getEmail()).roles("ALUNO"))
                        .param("cpf", "123.456.789-09")
                        .param("curso", "Informática"))
                .andExpect(status().isForbidden());
        verify(formularioService, never()).gerar(any(), any(), any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"SECRETARIA", "ADMINISTRADOR", "SEGURANCA"})
    void outrosPerfisNaoGeramFormularioDeAluno(String perfil) throws Exception {
        mockMvc.perform(post("/solicitacoes/30/formulario-oficial")
                        .with(user("usuario@escola.test").roles(perfil))
                        .with(csrf())
                        .param("cpf", "123.456.789-09")
                        .param("curso", "Informática"))
                .andExpect(status().isForbidden());
    }

    @Test
    void cpfNaoPodeIrNaUrl() throws Exception {
        mockMvc.perform(get("/solicitacoes/30/formulario-oficial")
                        .with(user(aluno.getEmail()).roles("ALUNO"))
                        .param("cpf", "123.456.789-09"))
                .andExpect(status().isMethodNotAllowed());
    }
}
