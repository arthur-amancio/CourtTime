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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Layout compartilhado (Fase B): navegacao por perfil, pagina atual,
 * identificacao do usuario e logout por POST com CSRF.
 */
@WebMvcTest(InicioController.class)
@Import({SecurityConfig.class, UsuarioDetailsService.class})
class LayoutNavegacaoTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @MockitoBean
    private ReservaService reservaService;

    @MockitoBean
    private SituacaoSolicitacaoService situacaoSolicitacaoService;

    @Test
    void studentNavigationListsOnlyStudentRoutesAndMarksCurrentPage() throws Exception {
        String html = pagina(Perfil.ALUNO);

        assertTrue(html.contains("href=\"/quadra\""));
        assertTrue(html.contains("href=\"/solicitacoes/nova\""));
        assertTrue(html.contains("href=\"/solicitacoes\""));
        assertTrue(!html.contains("href=\"/admin"));
        assertTrue(!html.contains("href=\"/secretaria"));
        assertTrue(!html.contains("href=\"/seguranca"));
        assertTrue(linkAtual(html).equals("/inicio"), "Início é a página atual");
    }

    @Test
    void administratorNavigationListsAdministrativeRoutes() throws Exception {
        String html = pagina(Perfil.ADMINISTRADOR);

        for (String rota : List.of("/admin", "/admin/solicitacoes", "/admin/usuarios", "/admin/horarios", "/admin/bloqueios")) {
            assertTrue(html.contains("href=\"" + rota + "\""), rota);
        }
        assertTrue(!html.contains("href=\"/solicitacoes"));
        assertTrue(!html.contains("href=\"/secretaria"));
    }

    @Test
    void secretaryNavigationListsDocumentsAndContributions() throws Exception {
        String html = pagina(Perfil.SECRETARIA);

        assertTrue(html.contains("href=\"/secretaria/documentos\""));
        assertTrue(html.contains("href=\"/secretaria/contribuicoes\""));
        assertTrue(!html.contains("href=\"/admin"));
        assertTrue(!html.contains("href=\"/seguranca"));
    }

    @Test
    void securityNavigationHasNoDocumentsContributionsOrEmail() throws Exception {
        String html = pagina(Perfil.SEGURANCA);

        assertTrue(html.contains("href=\"/seguranca/reservas\""));
        String navegacao = trecho(html, "<aside", "</aside>");
        assertTrue(!navegacao.contains("Documento") && !navegacao.contains("Contribuição"));
        assertTrue(!html.contains("pessoa.seguranca@teste.local"), "email não aparece no layout");
    }

    @Test
    void layoutShowsNameAndProfileAndLogsOutWithPostAndCsrf() throws Exception {
        String html = pagina(Perfil.ALUNO);

        String lateral = trecho(html, "<aside", "</aside>");
        assertTrue(lateral.contains("Pessoa Aluno"));
        assertTrue(lateral.contains("Perfil: <span>Aluno</span>"));
        Matcher logout = Pattern.compile("<form[^>]*action=\"/logout\"[^>]*method=\"post\"[^>]*>(.*?)</form>",
                Pattern.DOTALL).matcher(lateral);
        assertTrue(logout.find(), "logout continua sendo formulário POST");
        assertTrue(logout.group(1).contains("name=\"_csrf\""), "logout inclui o token CSRF");
        assertTrue(!html.contains("href=\"/logout\""), "logout não vira link GET");
    }

    @Test
    void layoutKeepsSkipLinkMainLandmarkAndNoJavascriptFallback() throws Exception {
        mockMvc.perform(get("/inicio").with(user(principal(Perfil.ALUNO))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"#conteudo\"")))
                .andExpect(content().string(containsString("id=\"menu-principal\"")))
                .andExpect(content().string(containsString("href=\"#menu-principal\"")))
                .andExpect(content().string(containsString("aria-controls=\"menu-principal\"")))
                .andExpect(content().string(containsString("aria-expanded=\"false\"")))
                .andExpect(content().string(containsString("/js/navegacao.js")))
                .andExpect(content().string(not(containsString("th:replace"))));
    }

    private String pagina(Perfil perfil) throws Exception {
        return mockMvc.perform(get("/inicio").with(user(principal(perfil))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private UsuarioAutenticado principal(Perfil perfil) {
        Usuario usuario = new Usuario();
        usuario.setId(50L + perfil.ordinal());
        usuario.setNome("Pessoa " + perfil.getDescricao());
        usuario.setEmail("pessoa." + perfil.name().toLowerCase() + "@teste.local");
        usuario.setSenha("hash-de-teste");
        usuario.setPerfil(perfil);
        usuario.setAtivo(true);
        when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        when(usuarioRepository.findByEmailIgnoreCase(usuario.getEmail())).thenReturn(Optional.of(usuario));
        when(reservaService.listarSolicitacoesDoUsuario(usuario)).thenReturn(List.of());
        return new UsuarioAutenticado(usuario);
    }

    private static String linkAtual(String html) {
        Matcher m = Pattern.compile("<a[^>]*href=\"([^\"]+)\"[^>]*aria-current=\"page\"|<a[^>]*aria-current=\"page\"[^>]*href=\"([^\"]+)\"")
                .matcher(html);
        assertTrue(m.find(), "existe um item marcado como página atual");
        return m.group(1) != null ? m.group(1) : m.group(2);
    }

    private static String trecho(String html, String inicio, String fim) {
        int a = html.indexOf(inicio);
        int b = html.indexOf(fim, a);
        assertTrue(a >= 0 && b > a, "trecho " + inicio);
        return html.substring(a, b + fim.length());
    }
}
