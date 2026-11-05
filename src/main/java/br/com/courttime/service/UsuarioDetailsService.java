package br.com.courttime.service;

import br.com.courttime.entity.Usuario;
import br.com.courttime.repository.UsuarioRepository;
import br.com.courttime.security.UsuarioAutenticado;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Carrega a conta persistida para autenticação por sessão. A autorização por perfil e por
 * proprietário continua sendo verificada nas rotas e nos serviços.
 */
@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .filter(Usuario::isAtivo)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado ou inativo"));

        return new UsuarioAutenticado(usuario);
    }
}
