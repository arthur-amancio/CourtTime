package br.com.courttime;

import br.com.courttime.entity.Perfil;
import br.com.courttime.entity.Usuario;
import br.com.courttime.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prova, com o servidor real, o que o MockMvc nao consegue: que o Tomcat
 * encaminha o erro para a pagina em portugues mantendo a sessao do usuario.
 * E o cenario do Ato 5 da banca: a seguranca abre uma reserva que nao esta
 * mais disponivel.
 *
 * Cria uma unica conta ficticia com email aleatorio e, ao final, apaga
 * somente essa conta. Nao limpa tabelas.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnabledIfEnvironmentVariable(named = "COURTTIME_POSTGRES_TEST", matches = "true")
class PaginaDeErroServidorTest {

    private static final String SENHA = "SenhaDeTeste123";
    private static final Pattern CSRF = Pattern.compile("name=\"_csrf\"\\s+value=\"([^\"]+)\"");

    @LocalServerPort
    private int porta;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private final RestTemplate rest = clienteSemRedirecionamento();
    private Usuario seguranca;
    private String cookie;

    @BeforeEach
    void criarContaFicticia() {
        Usuario usuario = new Usuario();
        usuario.setNome("Portaria fictícia");
        usuario.setEmail("erro-" + UUID.randomUUID() + "@exemplo.local");
        usuario.setSenha(passwordEncoder.encode(SENHA));
        usuario.setPerfil(Perfil.SEGURANCA);
        usuario.setAtivo(true);
        seguranca = usuarioRepository.saveAndFlush(usuario);
    }

    @AfterEach
    void apagarSomenteAContaCriada() {
        if (seguranca != null && seguranca.getId() != null) {
            usuarioRepository.deleteById(seguranca.getId());
        }
    }

    @Test
    void reservaInexistenteMostraPaginaEmPortuguesComASessaoAtiva() {
        entrar();

        ResponseEntity<String> resposta = get("/seguranca/reservas/999999999");

        assertEquals(HttpStatus.NOT_FOUND, resposta.getStatusCode());
        assertTrue(resposta.getBody().contains("Não encontrado"), resposta.getBody());
        assertTrue(resposta.getBody().contains("Voltar ao início"));
        assertFalse(resposta.getBody().contains("Whitelabel"));
    }

    @Test
    void areaDeOutroPerfilMostraAcessoNaoPermitido() {
        entrar();

        ResponseEntity<String> resposta = get("/admin/solicitacoes");

        assertEquals(HttpStatus.FORBIDDEN, resposta.getStatusCode());
        assertTrue(resposta.getBody().contains("Acesso não permitido"), resposta.getBody());
        assertFalse(resposta.getBody().contains("Whitelabel"));
    }

    private void entrar() {
        ResponseEntity<String> paginaLogin = get("/login");
        guardarCookie(paginaLogin);
        Matcher token = CSRF.matcher(paginaLogin.getBody());
        assertTrue(token.find(), "token CSRF ausente na página de login");

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("email", seguranca.getEmail());
        form.add("password", SENHA);
        form.add("_csrf", token.group(1));
        HttpHeaders cabecalhos = cabecalhos();
        cabecalhos.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        ResponseEntity<String> resposta = rest.exchange(url("/login"), HttpMethod.POST,
                new HttpEntity<>(form, cabecalhos), String.class);
        guardarCookie(resposta);
        assertEquals(HttpStatus.FOUND, resposta.getStatusCode());
        assertTrue(resposta.getHeaders().getLocation().toString().contains("/inicio"),
                "o login deve levar ao início");
    }

    /** Pede HTML, como o navegador; sem isso o Spring responderia em JSON. */
    private ResponseEntity<String> get(String caminho) {
        HttpHeaders cabecalhos = cabecalhos();
        cabecalhos.setAccept(List.of(MediaType.TEXT_HTML));
        return rest.exchange(url(caminho), HttpMethod.GET, new HttpEntity<>(cabecalhos), String.class);
    }

    private HttpHeaders cabecalhos() {
        HttpHeaders cabecalhos = new HttpHeaders();
        if (cookie != null) {
            cabecalhos.add(HttpHeaders.COOKIE, cookie);
        }
        return cabecalhos;
    }

    private void guardarCookie(ResponseEntity<String> resposta) {
        List<String> cookies = resposta.getHeaders().get(HttpHeaders.SET_COOKIE);
        if (cookies == null) {
            return;
        }
        for (String bruto : cookies) {
            if (bruto.startsWith("JSESSIONID=")) {
                cookie = bruto.split(";", 2)[0];
            }
        }
    }

    private String url(String caminho) {
        return "http://localhost:" + porta + caminho;
    }

    private static RestTemplate clienteSemRedirecionamento() {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory() {
            @Override
            protected void prepareConnection(HttpURLConnection conexao, String metodo)
                    throws IOException {
                super.prepareConnection(conexao, metodo);
                conexao.setInstanceFollowRedirects(false);
            }
        };
        RestTemplate template = new RestTemplate(fabrica);
        template.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse resposta) {
                return false;
            }
        });
        return template;
    }
}
