package br.com.courttime.security;

import br.com.courttime.entity.Perfil;
import br.com.courttime.entity.Usuario;
import org.springframework.security.access.AccessDeniedException;

/**
 * Única verificação de perfil usada pelos serviços.
 *
 * O SecurityConfig já bloqueia as rotas de cada perfil. Os serviços conferem
 * de novo porque podem ser chamados de outros lugares: só passa uma conta
 * gravada, ativa e com o perfil pedido.
 */
public final class Permissao {

    private Permissao() {
    }

    /** Verdadeiro quando a conta existe, está ativa e tem o perfil pedido. */
    public static boolean temPerfil(Usuario usuario, Perfil perfil) {
        return usuario != null
                && usuario.getId() != null
                && usuario.isAtivo()
                && usuario.getPerfil() == perfil;
    }

    /** Interrompe a operação com "acesso negado" quando a conta não tem o perfil pedido. */
    public static void exigir(Usuario usuario, Perfil perfil) {
        if (!temPerfil(usuario, perfil)) {
            throw new AccessDeniedException("Usuário sem permissão para esta operação");
        }
    }
}
