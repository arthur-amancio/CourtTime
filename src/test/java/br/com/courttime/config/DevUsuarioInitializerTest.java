package br.com.courttime.config;

import br.com.courttime.entity.Perfil;
import br.com.courttime.entity.Usuario;
import br.com.courttime.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DevUsuarioInitializerTest {

    @Mock
    private Environment environment;
    @Mock
    private UsuarioRepository usuarioRepository;

    private BCryptPasswordEncoder passwordEncoder;
    private DevUsuarioInitializer initializer;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        initializer = new DevUsuarioInitializer(
                environment, usuarioRepository, passwordEncoder);
    }

    @Test
    void bootstrapDesabilitadoNaoCriaUsuarioMesmoComVariaveisAntigas() throws Exception {
        when(environment.getProperty("COURTTIME_BOOTSTRAP_ADMIN_ENABLED", "false"))
                .thenReturn("false");

        initializer.run(null);

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void bootstrapExplicitoCriaPrimeiroAdministradorComBCrypt() throws Exception {
        prepararAmbienteAtivo();
        when(usuarioRepository.existsByPerfilAndAtivoTrue(Perfil.ADMINISTRADOR))
                .thenReturn(false);
        when(usuarioRepository.existsByEmailIgnoreCase("admin@escola.test"))
                .thenReturn(false);

        initializer.run(null);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        Usuario criado = captor.getValue();
        assertEquals(Perfil.ADMINISTRADOR, criado.getPerfil());
        assertEquals("admin@escola.test", criado.getEmail());
        assertTrue(passwordEncoder.matches("senha-segura", criado.getSenha()));
    }

    @Test
    void reinicializacaoNaoSobrescreveAdministradorExistente() throws Exception {
        prepararAmbienteAtivo();
        Usuario existente = new Usuario();
        existente.setEmail("admin@escola.test");
        existente.setPerfil(Perfil.ADMINISTRADOR);
        existente.setAtivo(true);
        existente.setSenha("hash-existente");
        when(usuarioRepository.existsByPerfilAndAtivoTrue(Perfil.ADMINISTRADOR))
                .thenReturn(true);
        when(usuarioRepository.findByEmailIgnoreCase("admin@escola.test"))
                .thenReturn(Optional.of(existente));

        initializer.run(null);

        assertEquals("hash-existente", existente.getSenha());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void bootstrapRecusaCriarOutroAdministradorQuandoJaExisteUmAtivo() {
        prepararAmbienteAtivo();
        when(usuarioRepository.existsByPerfilAndAtivoTrue(Perfil.ADMINISTRADOR))
                .thenReturn(true);
        when(usuarioRepository.findByEmailIgnoreCase("admin@escola.test"))
                .thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> initializer.run(null));
        verify(usuarioRepository, never()).save(any());
    }

    private void prepararAmbienteAtivo() {
        when(environment.getProperty("COURTTIME_BOOTSTRAP_ADMIN_ENABLED", "false"))
                .thenReturn("true");
        when(environment.getProperty("COURTTIME_DEV_USER_NAME"))
                .thenReturn("Administrador inicial");
        when(environment.getProperty("COURTTIME_DEV_USER_EMAIL"))
                .thenReturn("ADMIN@ESCOLA.TEST");
        when(environment.getProperty("COURTTIME_DEV_USER_PASSWORD"))
                .thenReturn("senha-segura");
        when(environment.getProperty("COURTTIME_DEV_USER_PROFILE"))
                .thenReturn("ADMINISTRADOR");
    }
}
