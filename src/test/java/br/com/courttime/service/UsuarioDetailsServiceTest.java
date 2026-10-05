package br.com.courttime.service;

import br.com.courttime.config.SecurityConfig;
import br.com.courttime.entity.Perfil;
import br.com.courttime.entity.Usuario;
import br.com.courttime.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioDetailsServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    private UsuarioDetailsService usuarioDetailsService;

    @BeforeEach
    void setUp() {
        usuarioDetailsService = new UsuarioDetailsService(usuarioRepository);
    }

    @ParameterizedTest
    @EnumSource(Perfil.class)
    void findsActiveUserAndMapsProfileToAuthority(Perfil perfil) {
        Usuario usuario = usuario(perfil, true);
        when(usuarioRepository.findByEmailIgnoreCase("usuario@escola.test"))
                .thenReturn(Optional.of(usuario));

        UserDetails userDetails = usuarioDetailsService.loadUserByUsername("usuario@escola.test");

        assertEquals("usuario@escola.test", userDetails.getUsername());
        assertEquals("hash-armazenado", userDetails.getPassword());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + perfil.name())));
    }

    @Test
    void rejectsInactiveUser() {
        Usuario usuario = usuario(Perfil.ALUNO, false);
        when(usuarioRepository.findByEmailIgnoreCase("usuario@escola.test"))
                .thenReturn(Optional.of(usuario));

        assertThrows(UsernameNotFoundException.class,
                () -> usuarioDetailsService.loadUserByUsername("usuario@escola.test"));
    }

    @Test
    void bcryptEncoderValidatesPassword() {
        PasswordEncoder passwordEncoder = new SecurityConfig().passwordEncoder();
        String encodedPassword = passwordEncoder.encode("senha-de-teste");

        assertFalse(encodedPassword.equals("senha-de-teste"));
        assertTrue(passwordEncoder.matches("senha-de-teste", encodedPassword));
        assertFalse(passwordEncoder.matches("senha-incorreta", encodedPassword));
    }

    private Usuario usuario(Perfil perfil, boolean ativo) {
        Usuario usuario = new Usuario();
        usuario.setNome("Usuário de Teste");
        usuario.setEmail("usuario@escola.test");
        usuario.setSenha("hash-armazenado");
        usuario.setPerfil(perfil);
        usuario.setAtivo(ativo);
        return usuario;
    }
}
