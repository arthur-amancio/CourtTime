package br.com.courttime.controller;

import br.com.courttime.controller.NavegacaoControllerAdvice.ItemNavegacao;
import br.com.courttime.controller.NavegacaoControllerAdvice.UsuarioLogado;
import br.com.courttime.entity.Perfil;
import br.com.courttime.entity.Usuario;
import br.com.courttime.security.UsuarioAutenticado;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NavegacaoControllerAdviceTest {

    private final NavegacaoControllerAdvice advice = new NavegacaoControllerAdvice();

    @Test
    void alunoSeesOnlyStudentRoutes() {
        List<ItemNavegacao> menu = advice.menuPrincipal(autenticado("ALUNO"), requisicao("/solicitacoes/15"));

        assertEquals(List.of("/inicio", "/quadra", "/solicitacoes/nova", "/solicitacoes"), urls(menu));
        assertEquals("/solicitacoes", ativo(menu));
    }

    @Test
    void newRequestPageMarksOnlyTheNewRequestItem() {
        List<ItemNavegacao> menu = advice.menuPrincipal(autenticado("ALUNO"), requisicao("/solicitacoes/nova"));

        assertEquals("/solicitacoes/nova", ativo(menu));
    }

    @Test
    void administradorSeesAdministrativeRoutes() {
        List<ItemNavegacao> menu = advice.menuPrincipal(autenticado("ADMINISTRADOR"), requisicao("/admin/usuarios/7/editar"));

        assertEquals(List.of("/inicio", "/admin", "/admin/solicitacoes", "/quadra",
                "/admin/horarios", "/admin/bloqueios", "/admin/usuarios"), urls(menu));
        assertEquals("/admin/usuarios", ativo(menu));
    }

    @Test
    void adminPanelIsActiveOnlyOnItsExactPath() {
        assertEquals("/admin", ativo(advice.menuPrincipal(autenticado("ADMINISTRADOR"), requisicao("/admin"))));
        assertEquals("/admin/solicitacoes",
                ativo(advice.menuPrincipal(autenticado("ADMINISTRADOR"), requisicao("/admin/solicitacoes/3"))));
    }

    @Test
    void secretariaSeesDocumentsAndContributions() {
        List<ItemNavegacao> menu = advice.menuPrincipal(autenticado("SECRETARIA"), requisicao("/secretaria/contribuicoes"));

        assertEquals(List.of("/inicio", "/quadra", "/secretaria/documentos", "/secretaria/contribuicoes"), urls(menu));
        assertEquals("/secretaria/contribuicoes", ativo(menu));
    }

    @Test
    void segurancaSeesOnlyAuthorizedReservationsWithoutDocumentsOrContributions() {
        List<ItemNavegacao> menu = advice.menuPrincipal(autenticado("SEGURANCA"), requisicao("/seguranca/reservas/9"));

        assertEquals(List.of("/inicio", "/quadra", "/seguranca/reservas"), urls(menu));
        assertEquals("/seguranca/reservas", ativo(menu));
        assertTrue(menu.stream().noneMatch(item -> item.rotulo().contains("Documento")
                || item.rotulo().contains("Contribuição")));
    }

    @Test
    void similarPrefixDoesNotMarkItemAsActive() {
        List<ItemNavegacao> menu = advice.menuPrincipal(autenticado("ALUNO"), requisicao("/quadras"));

        assertNull(ativo(menu));
    }

    @Test
    void contextPathIsIgnoredWhenMatchingTheCurrentPage() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/courttime/quadra");
        request.setContextPath("/courttime");

        assertEquals("/quadra", ativo(advice.menuPrincipal(autenticado("ALUNO"), request)));
    }

    @Test
    void anonymousVisitorHasNoMenuNorUser() {
        Authentication anonimo = new AnonymousAuthenticationToken("chave", "anonymousUser",
                AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));

        assertTrue(advice.menuPrincipal(anonimo, requisicao("/")).isEmpty());
        assertTrue(advice.menuPrincipal(null, requisicao("/")).isEmpty());
        assertNull(advice.usuarioLogado(anonimo));
        assertNull(advice.usuarioLogado(null));
    }

    @Test
    void displayNameComesFromAuthenticatedPrincipal() {
        Usuario usuario = new Usuario();
        usuario.setId(4L);
        usuario.setNome("Ana Souza");
        usuario.setEmail("ana@escola.test");
        usuario.setSenha("hash");
        usuario.setPerfil(Perfil.ALUNO);
        usuario.setAtivo(true);
        UsuarioAutenticado principal = new UsuarioAutenticado(usuario);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());

        UsuarioLogado logado = advice.usuarioLogado(authentication);

        assertEquals("Ana Souza", logado.nome());
        assertEquals(Perfil.ALUNO, logado.perfil());
    }

    @Test
    void plainUserWithoutDisplayNameStillHasProfile() {
        UsuarioLogado logado = advice.usuarioLogado(autenticado("SEGURANCA"));

        assertNull(logado.nome());
        assertEquals(Perfil.SEGURANCA, logado.perfil());
        assertFalse(logado.perfil().getDescricao().isBlank());
    }

    private static Authentication autenticado(String perfil) {
        return new UsernamePasswordAuthenticationToken("pessoa@escola.test", null,
                AuthorityUtils.createAuthorityList("ROLE_" + perfil));
    }

    private static MockHttpServletRequest requisicao(String uri) {
        return new MockHttpServletRequest("GET", uri);
    }

    private static List<String> urls(List<ItemNavegacao> menu) {
        return menu.stream().map(ItemNavegacao::url).toList();
    }

    private static String ativo(List<ItemNavegacao> menu) {
        List<String> ativos = menu.stream().filter(ItemNavegacao::ativo).map(ItemNavegacao::url).toList();
        assertTrue(ativos.size() <= 1, "no máximo um item ativo: " + ativos);
        return ativos.isEmpty() ? null : ativos.get(0);
    }
}
