package br.com.courttime.config;

import br.com.courttime.entity.Perfil;
import br.com.courttime.repository.UsuarioRepository;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;

import java.net.URI;

@Configuration(proxyBeanMethods = false)
@Profile("render")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ConfiguracaoRender implements ApplicationRunner {

    private final JdbcTemplate jdbc;
    private final UsuarioRepository usuarios;

    public ConfiguracaoRender(JdbcTemplate jdbc, UsuarioRepository usuarios) {
        this.jdbc = jdbc;
        this.usuarios = usuarios;
    }

    @Bean
    static BeanFactoryPostProcessor validarAntesDoHibernate(Environment environment) {
        return factory -> validar(environment);
    }

    static void validar(Environment environment) {
        if (!"validate".equals(environment.getProperty("spring.jpa.hibernate.ddl-auto"))
                || !"s3".equals(environment.getProperty("courttime.documentos.storage"))
                || !environment.getProperty("COURTTIME_DEMO_SYNTHETIC_DATA", Boolean.class, false)
                || environment.getProperty("COURTTIME_POSTGRES_TEST", Boolean.class, false)
                || environment.getProperty("COURTTIME_BOOTSTRAP_ADMIN_ENABLED", Boolean.class, false)) {
            throw new IllegalStateException("Render exige dados fictícios confirmados, schema validate, "
                    + "storage S3 e testes/bootstrap desligados");
        }
        String url = environment.getProperty("spring.datasource.url", "");
        if (!urlValida(url)
                || environment.getProperty("spring.datasource.username", "").isBlank()
                || environment.getProperty("spring.datasource.password", "").isBlank()) {
            throw new IllegalStateException("Render exige courttime_demo, credenciais privadas e TLS verify-full");
        }
    }

    private static boolean urlValida(String url) {
        try {
            if (!url.startsWith("jdbc:postgresql://")) {
                return false;
            }
            URI uri = URI.create(url.substring(5));
            return uri.getHost() != null && uri.getUserInfo() == null
                    && "/courttime_demo".equals(uri.getPath())
                    && "sslmode=verify-full".equals(uri.getRawQuery())
                    && uri.getFragment() == null;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!"courttime_demo".equals(jdbc.queryForObject("select current_database()", String.class))) {
            throw new IllegalStateException("A conexão não aponta para courttime_demo");
        }
        if (!usuarios.existsByPerfilAndAtivoTrue(Perfil.ADMINISTRADOR)) {
            throw new IllegalStateException("Provisione um administrador com senha privada antes do deploy público");
        }
    }
}
