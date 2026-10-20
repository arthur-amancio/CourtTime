package br.com.courttime.controller;

import br.com.courttime.entity.Perfil;
import br.com.courttime.security.UsuarioAutenticado;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.ArrayList;
import java.util.List;

/**
 * Dados da navegacao compartilhada (barra lateral) para todas as telas.
 *
 * Apenas monta os links do perfil autenticado. Nao concede nem verifica acesso:
 * as rotas continuam protegidas pelo SecurityConfig, e um link ausente na tela
 * nao substitui essa protecao.
 */
@ControllerAdvice
public class NavegacaoControllerAdvice {

    /** Usuario exibido na barra lateral. Nome pode ser nulo; perfil vem da autoridade. */
    public record UsuarioLogado(String nome, Perfil perfil) {
    }

    /** Item do menu. A URL nao inclui o context path; o template usa @{...}. */
    public record ItemNavegacao(String rotulo, String url, String icone, boolean ativo) {
    }

    @ModelAttribute("usuarioLogado")
    public UsuarioLogado usuarioLogado(Authentication authentication) {
        Perfil perfil = perfilDe(authentication);
        if (perfil == null) {
            return null;
        }
        String nome = authentication.getPrincipal() instanceof UsuarioAutenticado principal
                ? principal.getNome()
                : null;
        return new UsuarioLogado(nome, perfil);
    }

    @ModelAttribute("menuPrincipal")
    public List<ItemNavegacao> menuPrincipal(Authentication authentication, HttpServletRequest request) {
        Perfil perfil = perfilDe(authentication);
        if (perfil == null) {
            return List.of();
        }
        String caminho = caminhoAtual(request);
        List<ItemNavegacao> itens = new ArrayList<>();
        itens.add(item("Início", "/inicio", "casa", caminho.equals("/inicio")));
        switch (perfil) {
            case ALUNO -> {
                itens.add(item("Consultar quadra", "/quadra", "calendario", comeca(caminho, "/quadra")));
                itens.add(item("Nova solicitação", "/solicitacoes/nova", "calendario-mais",
                        caminho.equals("/solicitacoes/nova")));
                itens.add(item("Minhas solicitações", "/solicitacoes", "lista",
                        comeca(caminho, "/solicitacoes") && !caminho.equals("/solicitacoes/nova")));
            }
            case ADMINISTRADOR -> {
                itens.add(item("Painel administrativo", "/admin", "grade", caminho.equals("/admin")));
                itens.add(item("Análise de solicitações", "/admin/solicitacoes", "bandeja",
                        comeca(caminho, "/admin/solicitacoes")));
                itens.add(item("Consultar quadra", "/quadra", "calendario", comeca(caminho, "/quadra")));
                itens.add(item("Horários semanais", "/admin/horarios", "relogio", comeca(caminho, "/admin/horarios")));
                itens.add(item("Bloqueios", "/admin/bloqueios", "proibido", comeca(caminho, "/admin/bloqueios")));
                itens.add(item("Usuários", "/admin/usuarios", "usuarios", comeca(caminho, "/admin/usuarios")));
            }
            case SECRETARIA -> {
                itens.add(item("Consultar quadra", "/quadra", "calendario", comeca(caminho, "/quadra")));
                itens.add(item("Conferência documental", "/secretaria/documentos", "documento-check",
                        comeca(caminho, "/secretaria/documentos")));
                itens.add(item("Contribuições presenciais", "/secretaria/contribuicoes", "recibo",
                        comeca(caminho, "/secretaria/contribuicoes")));
            }
            case SEGURANCA -> {
                itens.add(item("Consultar quadra", "/quadra", "calendario", comeca(caminho, "/quadra")));
                itens.add(item("Reservas autorizadas", "/seguranca/reservas", "escudo",
                        comeca(caminho, "/seguranca/reservas")));
            }
        }
        return List.copyOf(itens);
    }

    private static ItemNavegacao item(String rotulo, String url, String icone, boolean ativo) {
        return new ItemNavegacao(rotulo, url, icone, ativo);
    }

    /** Verdadeiro para o proprio caminho e seus subcaminhos (evita "/quadras" casar com "/quadra"). */
    private static boolean comeca(String caminho, String base) {
        return caminho.equals(base) || caminho.startsWith(base + "/");
    }

    private static String caminhoAtual(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contexto = request.getContextPath();
        if (contexto != null && !contexto.isEmpty() && uri.startsWith(contexto)) {
            uri = uri.substring(contexto.length());
        }
        return uri.isEmpty() ? "/" : uri;
    }

    private static Perfil perfilDe(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String nome = authority.getAuthority();
            if (nome != null && nome.startsWith("ROLE_")) {
                for (Perfil perfil : Perfil.values()) {
                    if (perfil.name().equals(nome.substring("ROLE_".length()))) {
                        return perfil;
                    }
                }
            }
        }
        return null;
    }
}
