package br.com.courttime.controller;

import br.com.courttime.config.SecurityConfig;
import br.com.courttime.entity.Perfil;
import br.com.courttime.entity.Usuario;
import br.com.courttime.repository.UsuarioRepository;
import br.com.courttime.security.UsuarioAutenticado;
import br.com.courttime.service.ReservaService;
import br.com.courttime.service.SituacaoSolicitacaoService;
import br.com.courttime.service.UsuarioDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InicioController.class)
@Import({SecurityConfig.class, UsuarioDetailsService.class})
class PaginasPublicasTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @MockitoBean
    private ReservaService reservaService;

    @MockitoBean
    private SituacaoSolicitacaoService situacaoSolicitacaoService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void homePageIsPublic() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("CourtTime")))
                .andExpect(content().string(containsString(
                        "Consulta e solicitação de uso da quadra")))
                .andExpect(content().string(containsString("/css/style.css")))
                .andExpect(content().string(containsString("/img/etec-vista-aerea-960.jpg")))
                .andExpect(content().string(containsString("/img/logos/etec-fernandopolis.svg")))
                .andExpect(content().string(containsString("/img/logos/cps.svg")))
                .andExpect(content().string(containsString("/img/logos/governo-sp.svg")))
                .andExpect(content().string(containsString("/img/favicon.svg")));
    }

    @Test
    void schoolImagesArePublic() throws Exception {
        mockMvc.perform(get("/img/etec-vista-aerea-960.jpg")).andExpect(status().isOk());
        mockMvc.perform(get("/img/etec-fachada-1200.jpg")).andExpect(status().isOk());
        mockMvc.perform(get("/img/logos/etec-fernandopolis.svg")).andExpect(status().isOk());
        mockMvc.perform(get("/img/logos/cps.svg")).andExpect(status().isOk());
        mockMvc.perform(get("/img/logos/governo-sp.svg")).andExpect(status().isOk());
        mockMvc.perform(get("/img/favicon.svg")).andExpect(status().isOk());
    }

    @Test
    void fonteDoSistemaEPublica() throws Exception {
        // O login também usa a fonte, então ela não pode exigir sessão.
        mockMvc.perform(get("/css/fonts/noto-sans-latin.woff2")).andExpect(status().isOk());
    }

    @Test
    void loginPageIsPublic() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Entrar")))
                .andExpect(content().string(containsString("/img/etec-fachada-1200.jpg")));
    }

    @Test
    void inicioRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/inicio"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void authenticatedUserCanAccessInicio() throws Exception {
        Usuario usuario = usuario("Ana", "ana@escola.test", Perfil.ALUNO, true);
        when(usuarioRepository.findByEmailIgnoreCase(usuario.getEmail())).thenReturn(Optional.of(usuario));

        mockMvc.perform(get("/inicio").with(user(usuario.getEmail()).roles("ALUNO")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ana")))
                .andExpect(content().string(containsString("Aluno")))
                .andExpect(content().string(containsString("Acesso rápido")))
                .andExpect(content().string(containsString("Consultar horários")));
    }

    @Test
    void secretariaEncontraAtalhoParaConferenciaDocumental() throws Exception {
        Usuario usuario = usuario(
                "Sofia", "secretaria@escola.test", Perfil.SECRETARIA, true);
        when(usuarioRepository.findByEmailIgnoreCase(usuario.getEmail()))
                .thenReturn(Optional.of(usuario));

        mockMvc.perform(get("/inicio")
                        .with(user(usuario.getEmail()).roles("SECRETARIA")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Conferir documentos")))
                .andExpect(content().string(containsString("/secretaria/documentos")))
                .andExpect(content().string(containsString("Contribuições presenciais")))
                .andExpect(content().string(containsString("/secretaria/contribuicoes")));
    }

    @Test
    void segurancaEncontraAtalhoParaReservasAutorizadas() throws Exception {
        Usuario usuario = usuario(
                "Sérgio", "seguranca@escola.test", Perfil.SEGURANCA, true);
        when(usuarioRepository.findByEmailIgnoreCase(usuario.getEmail()))
                .thenReturn(Optional.of(usuario));

        mockMvc.perform(get("/inicio")
                        .with(user(usuario.getEmail()).roles("SEGURANCA")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Reservas autorizadas")))
                .andExpect(content().string(containsString("/seguranca/reservas")));
    }

    @Test
    void logoutRedirectsToLoginMessage() throws Exception {
        mockMvc.perform(post("/logout").with(user("ana@escola.test")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"));
    }

    @Test
    void inactiveUserCannotAuthenticate() throws Exception {
        Usuario usuario = usuario("Ana", "ana@escola.test", Perfil.ALUNO, false);
        usuario.setSenha(passwordEncoder.encode("senha-de-teste"));
        when(usuarioRepository.findByEmailIgnoreCase(usuario.getEmail())).thenReturn(Optional.of(usuario));

        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("email", usuario.getEmail())
                        .param("password", "senha-de-teste"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?erro"));
    }

    @Test
    void previouslyAuthenticatedSessionIsInvalidatedAfterAccountDeactivation() throws Exception {
        Usuario usuario = usuario("Ana", "ana@escola.test", Perfil.ALUNO, true);
        usuario.setId(15L);
        usuario.setSenha(passwordEncoder.encode("senha-de-teste"));
        UsuarioAutenticado principal = new UsuarioAutenticado(usuario);
        usuario.setAtivo(false);
        usuario.incrementarVersaoAcesso();
        when(usuarioRepository.findById(15L)).thenReturn(Optional.of(usuario));

        mockMvc.perform(get("/inicio").with(user(principal)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?expirada"));
    }

    @Test
    void activeSessionWithCurrentCredentialsRemainsValid() throws Exception {
        Usuario usuario = usuario("Ana", "ana@escola.test", Perfil.ALUNO, true);
        usuario.setId(16L);
        UsuarioAutenticado principal = new UsuarioAutenticado(usuario);
        when(usuarioRepository.findById(16L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.findByEmailIgnoreCase(usuario.getEmail()))
                .thenReturn(Optional.of(usuario));

        mockMvc.perform(get("/inicio").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ana")));
    }

    @Test
    void sessionWithOutdatedAccessVersionIsInvalidated() throws Exception {
        Usuario usuario = usuario("Ana", "ana@escola.test", Perfil.ALUNO, true);
        usuario.setId(17L);
        UsuarioAutenticado principal = new UsuarioAutenticado(usuario);
        usuario.incrementarVersaoAcesso();
        when(usuarioRepository.findById(17L)).thenReturn(Optional.of(usuario));

        mockMvc.perform(get("/inicio").with(user(principal)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?expirada"));
    }

    private Usuario usuario(String nome, String email, Perfil perfil, boolean ativo) {
        Usuario usuario = new Usuario();
        usuario.setNome(nome);
        usuario.setEmail(email);
        usuario.setSenha("hash-de-teste");
        usuario.setPerfil(perfil);
        usuario.setAtivo(ativo);
        return usuario;
    }
}
