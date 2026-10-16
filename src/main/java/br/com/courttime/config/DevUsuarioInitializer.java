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

    private static final String ENABLED = "COURTTIME_BOOTSTRAP_ADMIN_ENABLED";
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
        if (!Boolean.parseBoolean(environment.getProperty(ENABLED, "false"))) {
            return;
        }

        String nome = environment.getProperty(NAME);
        String email = environment.getProperty(EMAIL);
        String senha = environment.getProperty(PASSWORD);
        String perfilInformado = environment.getProperty(PROFILE);

        if (Stream.of(nome, email, senha).anyMatch(this::isBlank)) {
            throw new IllegalStateException(
                    "Configure nome, email e senha para o bootstrap administrativo");
        }
        if (senha.length() < 8 || senha.length() > 72) {
            throw new IllegalStateException(
                    "A senha do bootstrap deve possuir entre 8 e 72 caracteres");
        }

        String emailNormalizado = email.trim().toLowerCase(Locale.ROOT);
        if (!isBlank(perfilInformado)
                && !Perfil.ADMINISTRADOR.name().equals(
                        perfilInformado.trim().toUpperCase(Locale.ROOT))) {
            throw new IllegalStateException(
                    "O bootstrap controlado cria somente o primeiro ADMINISTRADOR");
        }

        if (usuarioRepository.existsByPerfilAndAtivoTrue(Perfil.ADMINISTRADOR)) {
            Usuario existente = usuarioRepository.findByEmailIgnoreCase(emailNormalizado)
                    .orElse(null);
            if (existente != null
                    && existente.isAtivo()
                    && existente.getPerfil() == Perfil.ADMINISTRADOR) {
                return;
            }
            throw new IllegalStateException(
                    "O bootstrap foi recusado porque já existe um administrador ativo");
        }
        if (usuarioRepository.existsByEmailIgnoreCase(emailNormalizado)) {
            throw new IllegalStateException(
                    "O email do bootstrap já pertence a uma conta existente");
        }

        Usuario usuario = new Usuario();
        usuario.setNome(nome.trim());
        usuario.setEmail(emailNormalizado);
        usuario.setSenha(passwordEncoder.encode(senha));
        usuario.setPerfil(Perfil.ADMINISTRADOR);
        usuario.setAtivo(true);
        usuarioRepository.save(usuario);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
