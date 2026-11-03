package br.com.courttime.config;

import br.com.courttime.entity.Perfil;
import br.com.courttime.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConfiguracaoRenderTest {
    private MockEnvironment ambiente() {
        return new MockEnvironment().withProperty("spring.jpa.hibernate.ddl-auto", "validate")
                .withProperty("courttime.documentos.storage", "s3")
                .withProperty("COURTTIME_DEMO_SYNTHETIC_DATA", "true")
                .withProperty("spring.datasource.url", "jdbc:postgresql://host/courttime_demo?sslmode=verify-full")
                .withProperty("spring.datasource.username", "usuario-sintetico")
                .withProperty("spring.datasource.password", "senha-sintetica-de-teste");
    }

    @Test
    void aceitaSomenteConfiguracaoDeDemonstracaoProtegida() {
        assertDoesNotThrow(() -> ConfiguracaoRender.validarAntesDoHibernate(ambiente())
                .postProcessBeanFactory(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"create", "create-drop", "update", "none"})
    void recusaAlteracaoAutomaticaDoSchemaAntesDoHibernate(String ddl) {
        MockEnvironment env = ambiente().withProperty("spring.jpa.hibernate.ddl-auto", ddl);
        assertThrows(IllegalStateException.class, () -> ConfiguracaoRender.validar(env));
    }

    @ParameterizedTest
    @ValueSource(strings = {"COURTTIME_POSTGRES_TEST", "COURTTIME_BOOTSTRAP_ADMIN_ENABLED"})
    void recusaTestesDestrutivosOuBootstrapPublico(String flag) {
        assertThrows(IllegalStateException.class, () -> ConfiguracaoRender.validar(
                ambiente().withProperty(flag, "true")));
    }

    @Test
    void recusaDiscoEfemeroEDadosNaoConfirmados() {
        assertThrows(IllegalStateException.class, () -> ConfiguracaoRender.validar(
                ambiente().withProperty("courttime.documentos.storage", "local")));
        assertThrows(IllegalStateException.class, () -> ConfiguracaoRender.validar(
                ambiente().withProperty("COURTTIME_DEMO_SYNTHETIC_DATA", "false")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"jdbc:postgresql://host/courttime_demo?sslmode=require",
            "jdbc:postgresql://host/courttime_test?sslmode=verify-full",
            "jdbc:postgresql://segredo@host/courttime_demo?sslmode=verify-full",
            "jdbc:postgresql://host/courttime_demo?password=segredo&sslmode=verify-full",
            "jdbc:postgresql://host/courttime_demo?sslmode=verify-full&sslmode=disable",
            "jdbc:postgresql://host/courttime_demo?sslmode=verify-full#segredo",
            "jdbc:postgresql://host com segredo/courttime_demo?sslmode=verify-full"})
    void recusaBancoOuTlsIncorretosSemRepetirDsn(String url) {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                ConfiguracaoRender.validar(ambiente().withProperty("spring.datasource.url", url)));
        assertFalse(ex.getMessage().contains("segredo"));
        assertNull(ex.getCause());
    }

    @Test
    void confereIdentidadeRealDoBancoAntesDosInicializadores() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        UsuarioRepository usuarios = mock(UsuarioRepository.class);
        when(jdbc.queryForObject("select current_database()", String.class)).thenReturn("courttime_test");
        assertThrows(IllegalStateException.class, () -> new ConfiguracaoRender(jdbc, usuarios).run(null));
        verifyNoInteractions(usuarios);
    }

    @Test
    void exigeAdministradorPreviamenteProvisionado() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        UsuarioRepository usuarios = mock(UsuarioRepository.class);
        when(jdbc.queryForObject("select current_database()", String.class)).thenReturn("courttime_demo");
        ConfiguracaoRender config = new ConfiguracaoRender(jdbc, usuarios);
        assertThrows(IllegalStateException.class, () -> config.run(null));
        when(usuarios.existsByPerfilAndAtivoTrue(Perfil.ADMINISTRADOR)).thenReturn(true);
        assertDoesNotThrow(() -> config.run(null));
    }
}
