package br.com.courttime.config;

import br.com.courttime.entity.Perfil;
import br.com.courttime.entity.Usuario;
import br.com.courttime.repository.UsuarioRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.stream.Stream;

@Component
public class DevUsuarioInitializer implements ApplicationRunner {

    private static final String NAME = "COURTTIME_DEV_USER_NAME";
    private static final String EMAIL = "COURTTIME_DEV_USER_EMAIL";
    private static final String PASSWORD = "COURTTIME_DEV_USER_PASSWORD";
    private static final String PROFILE = "COURTTIME_DEV_USER_PROFILE";

    private final Environment environment;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public DevUsuarioInitializer(Environment environment, UsuarioRepository usuarioRepository,
                                 PasswordEncoder passwordEncoder) {
        this.environment = environment;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        String nome = environment.getProperty(NAME);
        String email = environment.getProperty(EMAIL);
        String senha = environment.getProperty(PASSWORD);
        String perfilInformado = environment.getProperty(PROFILE);

        if (Stream.of(nome, email, senha, perfilInformado).allMatch(this::isBlank)) {
            return;
        }

        if (Stream.of(nome, email, senha, perfilInformado).anyMatch(this::isBlank)) {
            throw new IllegalStateException("Configure todas as variáveis COURTTIME_DEV_USER_*");
        }

        String emailNormalizado = email.trim().toLowerCase(Locale.ROOT);
        if (usuarioRepository.existsByEmailIgnoreCase(emailNormalizado)) {
            return;
        }

        Perfil perfil;
        try {
            perfil = Perfil.valueOf(perfilInformado.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("COURTTIME_DEV_USER_PROFILE possui um perfil inválido", exception);
        }

        Usuario usuario = new Usuario();
        usuario.setNome(nome.trim());
        usuario.setEmail(emailNormalizado);
        usuario.setSenha(passwordEncoder.encode(senha));
        usuario.setPerfil(perfil);
        usuario.setAtivo(true);
        usuarioRepository.save(usuario);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
