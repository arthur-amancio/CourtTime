package br.com.courttime.security;

import br.com.courttime.entity.Usuario;
import br.com.courttime.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
public class RevalidacaoSessaoUsuarioFilter extends OncePerRequestFilter {

    private final UsuarioRepository usuarioRepository;

    public RevalidacaoSessaoUsuarioFilter(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof UsuarioAutenticado principal
                && !sessaoContinuaValida(principal)) {
            SecurityContextHolder.clearContext();
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            response.sendRedirect(request.getContextPath() + "/login?expirada");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean sessaoContinuaValida(UsuarioAutenticado principal) {
        Optional<Usuario> atual = usuarioRepository.findById(principal.getId());
        return atual.filter(Usuario::isAtivo)
                .filter(usuario -> usuario.getVersaoAcesso() == principal.getVersaoAcesso())
                .filter(usuario -> usuario.getPerfil() == principal.getPerfil())
                .filter(usuario -> usuario.getEmail().equalsIgnoreCase(principal.getUsername()))
                .isPresent();
    }
}
