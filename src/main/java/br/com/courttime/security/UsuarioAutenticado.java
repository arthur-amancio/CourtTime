package br.com.courttime.security;

import br.com.courttime.entity.Perfil;
import br.com.courttime.entity.Usuario;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.List;

public class UsuarioAutenticado extends User {

    private static final long serialVersionUID = 1L;

    private final Long id;
    private final Perfil perfil;
    private final long versaoAcesso;

    public UsuarioAutenticado(Usuario usuario) {
        super(
                usuario.getEmail(),
                usuario.getSenha(),
                usuario.isAtivo(),
                true,
                true,
                true,
                List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getPerfil().name())));
        this.id = usuario.getId();
        this.perfil = usuario.getPerfil();
        this.versaoAcesso = usuario.getVersaoAcesso();
    }

    public Long getId() {
        return id;
    }

    public Perfil getPerfil() {
        return perfil;
    }

    public long getVersaoAcesso() {
        return versaoAcesso;
    }
}
