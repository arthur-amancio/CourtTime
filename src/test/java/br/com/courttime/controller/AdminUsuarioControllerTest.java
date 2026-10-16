package br.com.courttime.controller;

import br.com.courttime.config.SecurityConfig;
import br.com.courttime.entity.Perfil;
import br.com.courttime.entity.Usuario;
import br.com.courttime.repository.UsuarioRepository;
import br.com.courttime.service.AdministracaoUsuarioService;
import br.com.courttime.service.UsuarioEdicao;
import br.com.courttime.service.UsuarioResumo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminUsuarioController.class)
@Import(SecurityConfig.class)
class AdminUsuarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdministracaoUsuarioService administracaoUsuarioService;
    @MockitoBean
    private UsuarioRepository usuarioRepository;

    private Usuario administrador;

    @BeforeEach
    void setUp() {
        administrador = new Usuario();
        administrador.setId(1L);
        administrador.setNome("Administrador");
        administrador.setEmail("admin@escola.test");
        administrador.setSenha("hash-nao-exibido");
        administrador.setPerfil(Perfil.ADMINISTRADOR);
        administrador.setAtivo(true);
    }

    @Test
    void usuarioNaoAutenticadoEEnviadoAoLogin() throws Exception {
        mockMvc.perform(get("/admin/usuarios"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void alunoSecretariaESegurancaNaoAcessamGerenciamento() throws Exception {
        mockMvc.perform(get("/admin/usuarios")
                        .with(user("aluno@escola.test").roles("ALUNO")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/admin/usuarios")
                        .with(user("secretaria@escola.test").roles("SECRETARIA"))
                        .with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/usuarios")
                        .with(user("seguranca@escola.test").roles("SEGURANCA")))
                .andExpect(status().isForbidden());
    }

    @Test
    void administradorListaUsuariosSemExporHash() throws Exception {
        prepararAdministrador();
        when(administracaoUsuarioService.listar(
                administrador, "", null, null, 0)).thenReturn(new PageImpl<>(List.of(
                        new UsuarioResumo(
                                20L,
                                "Ana Souza",
                                "ana@escola.test",
                                Perfil.ALUNO,
                                true))));

        mockMvc.perform(get("/admin/usuarios")
                        .with(user(administrador.getEmail()).roles("ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ana Souza")))
                .andExpect(content().string(containsString("ana@escola.test")))
                .andExpect(content().string(not(containsString("hash-nao-exibido"))));
    }

    @Test
    void cadastroValidoUsaOperadorAutenticado() throws Exception {
        prepararAdministrador();

        mockMvc.perform(post("/admin/usuarios")
                        .with(user(administrador.getEmail()).roles("ADMINISTRADOR"))
                        .with(csrf())
                        .param("nome", "Ana Souza")
                        .param("email", "ana@escola.test")
                        .param("perfil", "ALUNO")
                        .param("senha", "senha-segura")
                        .param("confirmacaoSenha", "senha-segura"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/usuarios"));

        verify(administracaoUsuarioService).cadastrar(
                administrador,
                "Ana Souza",
                "ana@escola.test",
                Perfil.ALUNO,
                "senha-segura");
    }

    @Test
    void cadastroSemCsrfERecusado() throws Exception {
        mockMvc.perform(post("/admin/usuarios")
                        .with(user(administrador.getEmail()).roles("ADMINISTRADOR"))
                        .param("nome", "Ana Souza")
                        .param("email", "ana@escola.test")
                        .param("perfil", "ALUNO")
                        .param("senha", "senha-segura")
                        .param("confirmacaoSenha", "senha-segura"))
                .andExpect(status().isForbidden());
    }

    @Test
    void senhaComConfirmacaoInvalidaNaoEExpostaNoHtml() throws Exception {
        when(administracaoUsuarioService.perfisCadastraveis())
                .thenReturn(List.of(Perfil.ALUNO, Perfil.SECRETARIA, Perfil.SEGURANCA));

        mockMvc.perform(post("/admin/usuarios")
                        .with(user(administrador.getEmail()).roles("ADMINISTRADOR"))
                        .with(csrf())
                        .param("nome", "Ana Souza")
                        .param("email", "ana@escola.test")
                        .param("perfil", "ALUNO")
                        .param("senha", "segredo-privado")
                        .param("confirmacaoSenha", "senha-diferente"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("A confirmação deve ser igual")))
                .andExpect(content().string(not(containsString("segredo-privado"))));
    }

    @Test
    void tentativaDeCadastrarAdministradorRecebeAcessoNegado() throws Exception {
        prepararAdministrador();
        when(administracaoUsuarioService.cadastrar(
                administrador,
                "Outro administrador",
                "outro@escola.test",
                Perfil.ADMINISTRADOR,
                "senha-segura"))
                .thenThrow(new AccessDeniedException("Operação não autorizada"));

        mockMvc.perform(post("/admin/usuarios")
                        .with(user(administrador.getEmail()).roles("ADMINISTRADOR"))
                        .with(csrf())
                        .param("nome", "Outro administrador")
                        .param("email", "outro@escola.test")
                        .param("perfil", "ADMINISTRADOR")
                        .param("senha", "senha-segura")
                        .param("confirmacaoSenha", "senha-segura"))
                .andExpect(status().isForbidden());
    }

    @Test
    void redefinicaoDeSenhaNaoApresentaCredencialAnterior() throws Exception {
        prepararAdministrador();
        when(administracaoUsuarioService.obterParaEdicao(20L, administrador))
                .thenReturn(new UsuarioEdicao(
                        20L, "Ana Souza", "ana@escola.test", Perfil.ALUNO, true));

        mockMvc.perform(get("/admin/usuarios/20/senha")
                        .with(user(administrador.getEmail()).roles("ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Redefinir senha")))
                .andExpect(content().string(not(containsString("hash-nao-exibido"))));
    }

    private void prepararAdministrador() {
        when(usuarioRepository.findByEmailIgnoreCase(administrador.getEmail()))
                .thenReturn(Optional.of(administrador));
    }
}
