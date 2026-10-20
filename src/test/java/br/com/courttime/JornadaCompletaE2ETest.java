package br.com.courttime;

import br.com.courttime.entity.DiaSemana;
import br.com.courttime.entity.DisponibilidadeQuadra;
import br.com.courttime.entity.Perfil;
import br.com.courttime.entity.Quadra;
import br.com.courttime.entity.Reserva;
import br.com.courttime.entity.StatusReserva;
import br.com.courttime.entity.Usuario;
import br.com.courttime.repository.AjusteContribuicaoApmRepository;
import br.com.courttime.repository.AuditoriaUsuarioRepository;
import br.com.courttime.repository.BloqueioQuadraRepository;
import br.com.courttime.repository.ConferenciaDocumentoRepository;
import br.com.courttime.repository.ContribuicaoApmRepository;
import br.com.courttime.repository.DisponibilidadeQuadraRepository;
import br.com.courttime.repository.DocumentoReservaRepository;
import br.com.courttime.repository.ParticipanteReservaRepository;
import br.com.courttime.repository.QuadraRepository;
import br.com.courttime.repository.ReservaRepository;
import br.com.courttime.repository.UsuarioRepository;
import br.com.courttime.service.CalendarioDisponibilidadeService;
import br.com.courttime.support.BancoDeTeste;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Percorre a jornada completa do CourtTime por HTTP, com o servidor real e o
 * PostgreSQL do CI: aluno, secretaria, administração e segurança, cada um com
 * a sua própria sessão autenticada.
 *
 * Os testes que já existem cobrem cada serviço e cada tela isoladamente. O que
 * faltava era exercitar o caminho inteiro de ponta a ponta, com login de
 * verdade, token CSRF de verdade e upload de verdade.
 *
 * Não usa navegador: é o protocolo HTTP da aplicação. Nenhuma dependência nova
 * foi adicionada ao projeto.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        // Os PDFs do teste ficam sob target/, que o "mvn clean" remove, para
        // nao deixar arquivos espalhados no diretorio de trabalho.
        properties = "courttime.documentos.diretorio=target/documentos-jornada-e2e")
@EnabledIfEnvironmentVariable(named = "COURTTIME_POSTGRES_TEST", matches = "true")
class JornadaCompletaE2ETest {

    private static final LocalDate DATA = LocalDate.now(CalendarioDisponibilidadeService.FUSO_HORARIO)
            .plusDays(7);
    private static final LocalTime INICIO = LocalTime.of(8, 0);
    private static final String SENHA = "SenhaDeTeste123";
    private static final Pattern CSRF = Pattern.compile(
            "name=\"_csrf\"\\s+value=\"([^\"]+)\"");

    @LocalServerPort
    private int porta;

    /**
     * Cliente HTTP proprio: nao segue redirecionamento e nao lanca excecao em
     * 4xx. Sem isso, um POST que responde 302 seria seguido ate a pagina de
     * destino e o teste veria 200, escondendo o resultado real.
     */
    private final RestTemplate rest = clienteSemRedirecionamento();

    @Autowired private DataSource dataSource;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private QuadraRepository quadraRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ReservaRepository reservaRepository;
    @Autowired private DisponibilidadeQuadraRepository disponibilidadeQuadraRepository;
    @Autowired private BloqueioQuadraRepository bloqueioQuadraRepository;
    @Autowired private ParticipanteReservaRepository participanteReservaRepository;
    @Autowired private DocumentoReservaRepository documentoReservaRepository;
    @Autowired private ConferenciaDocumentoRepository conferenciaDocumentoRepository;
    @Autowired private ContribuicaoApmRepository contribuicaoApmRepository;
    @Autowired private AjusteContribuicaoApmRepository ajusteContribuicaoApmRepository;
    @Autowired private AuditoriaUsuarioRepository auditoriaUsuarioRepository;

    private Quadra quadra;

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

    @BeforeEach
    void prepararCenario() {
        BancoDeTeste.exigirBancoDedicado(dataSource);
        ajusteContribuicaoApmRepository.deleteAllInBatch();
        contribuicaoApmRepository.deleteAllInBatch();
        conferenciaDocumentoRepository.deleteAllInBatch();
        documentoReservaRepository.deleteAllInBatch();
        participanteReservaRepository.deleteAllInBatch();
        auditoriaUsuarioRepository.deleteAllInBatch();
        reservaRepository.deleteAllInBatch();
        bloqueioQuadraRepository.deleteAllInBatch();
        disponibilidadeQuadraRepository.deleteAllInBatch();
        usuarioRepository.deleteAllInBatch();
        quadraRepository.deleteAllInBatch();

        quadra = new Quadra();
        quadra.setNome("Quadra de demonstração");
        quadra.setDescricao("Dados fictícios, somente no PostgreSQL temporário do CI");
        quadra.setAtiva(true);
        quadra = quadraRepository.saveAndFlush(quadra);

        criarUsuario("aluno.demo@exemplo.local", Perfil.ALUNO);
        criarUsuario("outro.aluno@exemplo.local", Perfil.ALUNO);
        criarUsuario("secretaria.demo@exemplo.local", Perfil.SECRETARIA);
        criarUsuario("admin.demo@exemplo.local", Perfil.ADMINISTRADOR);
        criarUsuario("seguranca.demo@exemplo.local", Perfil.SEGURANCA);

        DisponibilidadeQuadra grade = new DisponibilidadeQuadra();
        grade.setQuadra(quadra);
        grade.setDiaSemana(DiaSemana.from(DATA.getDayOfWeek()));
        grade.setHorarioInicio(LocalTime.of(8, 0));
        grade.setHorarioFim(LocalTime.of(12, 0));
        grade.setAtiva(true);
        disponibilidadeQuadraRepository.saveAndFlush(grade);
    }

    @Test
    void jornadaCompletaDosQuatroPerfis() {
        // ---------- ALUNO ----------
        Sessao aluno = entrar("aluno.demo@exemplo.local");
        assertTrue(aluno.get("/inicio").getBody().contains("Início"),
                "o aluno deve chegar ao início depois do login");
        assertTrue(aluno.get("/quadra").getStatusCode().is2xxSuccessful(),
                "o aluno deve conseguir consultar a quadra");

        Long reservaId = criarSolicitacao(aluno);
        assertEquals(StatusReserva.PENDENTE_DOCUMENTACAO,
                reservaRepository.findById(reservaId).orElseThrow().getStatus());

        for (int i = 1; i <= 10; i++) {
            MultiValueMap<String, String> p = new LinkedMultiValueMap<>();
            p.add("nomeCompleto", "Participante Fictício " + i);
            p.add("rmOuRg", "RM-" + (1000 + i));
            p.add("categoria", "ALUNO");
            assertEquals(HttpStatus.FOUND,
                    aluno.post("/solicitacoes/" + reservaId + "/participantes", p).getStatusCode(),
                    "cada participante deve ser aceito");
        }
        assertEquals(10, participanteReservaRepository.count());

        // ---------- FORMULÁRIO OFICIAL PREENCHIDO ----------
        assertTrue(aluno.get("/solicitacoes/" + reservaId + "/documentos").getBody()
                .contains("Gerar formulário preenchido"));
        MultiValueMap<String, String> dadosDoTermo = new LinkedMultiValueMap<>();
        dadosDoTermo.add("cpf", "123.456.789-09");
        dadosDoTermo.add("curso", "Técnico em Informática para Internet");
        ResponseEntity<String> formulario = aluno.postComTokenDe(
                "/solicitacoes/" + reservaId + "/documentos",
                "/solicitacoes/" + reservaId + "/formulario-oficial", dadosDoTermo);
        assertEquals(HttpStatus.OK, formulario.getStatusCode());
        assertEquals(MediaType.APPLICATION_PDF, formulario.getHeaders().getContentType());
        assertTrue(formulario.getBody().startsWith("%PDF"), "o servidor devolve um PDF");

        enviarPdf(aluno, reservaId);
        assertEquals(1, documentoReservaRepository.count(), "o PDF deve ter sido registrado");

        assertTrue(aluno.get("/solicitacoes/" + reservaId).getBody()
                        .contains("Documento aguardando conferência"),
                "depois do envio, o aluno deve ver que o documento aguarda a secretaria");

        // ---------- IDOR: outro aluno não acessa a solicitação alheia ----------
        Sessao outroAluno = entrar("outro.aluno@exemplo.local");
        assertEquals(HttpStatus.NOT_FOUND,
                outroAluno.get("/solicitacoes/" + reservaId).getStatusCode(),
                "um aluno não pode abrir a solicitação de outro");

        // ---------- CSRF ausente é recusado ----------
        MultiValueMap<String, String> semToken = new LinkedMultiValueMap<>();
        semToken.add("nomeCompleto", "Sem Token");
        semToken.add("rmOuRg", "RM-9999");
        semToken.add("categoria", "ALUNO");
        assertEquals(HttpStatus.FORBIDDEN,
                aluno.postSemCsrf("/solicitacoes/" + reservaId + "/participantes", semToken)
                        .getStatusCode(),
                "POST sem token CSRF deve ser recusado");
        assertEquals(10, participanteReservaRepository.count(),
                "a recusa por CSRF nao pode ter criado o participante");

        // ---------- perfil errado não entra na área da secretaria ----------
        assertEquals(HttpStatus.FORBIDDEN, aluno.get("/secretaria/documentos").getStatusCode(),
                "aluno não acessa a área da secretaria");
        assertEquals(HttpStatus.FORBIDDEN, aluno.get("/admin/solicitacoes").getStatusCode(),
                "aluno não acessa a administração");

        // ---------- SECRETARIA ----------
        Sessao secretaria = entrar("secretaria.demo@exemplo.local");
        Long documentoId = documentoReservaRepository.findAll().get(0).getId();
        assertTrue(secretaria.get("/secretaria/documentos").getBody().contains("conferência"),
                "a secretaria deve ver a fila documental");
        assertTrue(secretaria.get("/secretaria/documentos/" + documentoId + "/baixar")
                        .getStatusCode().is2xxSuccessful(),
                "a secretaria deve conseguir baixar o PDF");

        assertEquals(HttpStatus.FOUND,
                secretaria.post("/secretaria/documentos/" + documentoId + "/validar",
                        new LinkedMultiValueMap<>()).getStatusCode());
        assertEquals(1, conferenciaDocumentoRepository.count());

        MultiValueMap<String, String> contribuicao = new LinkedMultiValueMap<>();
        contribuicao.add("valor", "20.00");
        contribuicao.add("observacoes", "Recebimento fictício de demonstração");
        assertEquals(HttpStatus.FOUND,
                secretaria.post("/secretaria/contribuicoes/solicitacoes/" + reservaId, contribuicao)
                        .getStatusCode());
        assertEquals(1, contribuicaoApmRepository.count());
        assertEquals(StatusReserva.PENDENTE_ANALISE,
                reservaRepository.findById(reservaId).orElseThrow().getStatus(),
                "registrar a contribuição deve liberar a análise administrativa");

        // a secretaria não aprova reservas
        assertEquals(HttpStatus.FORBIDDEN,
                secretaria.postComTokenDe("/inicio",
                        "/admin/solicitacoes/" + reservaId + "/aprovar",
                        new LinkedMultiValueMap<>()).getStatusCode(),
                "a secretaria não pode aprovar, mesmo com token CSRF válido");
        assertEquals(StatusReserva.PENDENTE_ANALISE,
                reservaRepository.findById(reservaId).orElseThrow().getStatus(),
                "a tentativa da secretaria nao pode ter mudado o estado da reserva");

        // ---------- ADMINISTRACAO ----------
        Sessao admin = entrar("admin.demo@exemplo.local");
        String detalhe = admin.get("/admin/solicitacoes/" + reservaId).getBody();
        assertTrue(detalhe.contains("Participante Fictício 1"),
                "a administração deve ver os participantes");
        assertTrue(detalhe.contains("R$"), "a administração deve ver a contribuição registrada");

        assertEquals(HttpStatus.FOUND,
                admin.post("/admin/solicitacoes/" + reservaId + "/aprovar",
                        new LinkedMultiValueMap<>()).getStatusCode());
        assertEquals(StatusReserva.APROVADA,
                reservaRepository.findById(reservaId).orElseThrow().getStatus());

        // o horário aprovado deixa de ser oferecido como período disponível
        assertFalse(aluno.get("/solicitacoes/nova?data=" + DATA).getBody()
                        .contains("value=\"" + INICIO + "\""),
                "o período aprovado não deve mais ser oferecido ao aluno");

        // ---------- SEGURANCA ----------
        Sessao seguranca = entrar("seguranca.demo@exemplo.local");
        String lista = seguranca.get("/seguranca/reservas").getBody();
        assertTrue(lista.contains("Reservas autorizadas"));
        String conferencia = seguranca.get("/seguranca/reservas/" + reservaId).getBody();
        assertTrue(conferencia.contains("Participante Fictício 1"),
                "a segurança deve ver os participantes para conferir a entrada");
        assertTrue(conferencia.contains("RM-1001"));
        assertFalse(conferencia.contains("R$"),
                "a segurança não deve ver valores financeiros");
        assertFalse(conferencia.contains("aluno.demo@exemplo.local"),
                "a segurança não deve ver o email do solicitante");
        assertEquals(HttpStatus.FORBIDDEN,
                seguranca.get("/solicitacoes/" + reservaId + "/documentos/"
                        + documentoId + "/baixar").getStatusCode(),
                "a segurança não acessa os PDFs privados");

        // ---------- CANCELAMENTO INSTITUCIONAL ----------
        MultiValueMap<String, String> motivo = new LinkedMultiValueMap<>();
        motivo.add("motivo", "Interdição da quadra para manutenção");
        assertEquals(HttpStatus.FOUND,
                admin.post("/admin/solicitacoes/" + reservaId + "/cancelar", motivo).getStatusCode());

        Reserva cancelada = reservaRepository.findById(reservaId).orElseThrow();
        assertEquals(StatusReserva.CANCELADA, cancelada.getStatus());
        assertEquals("Interdição da quadra para manutenção",
                cancelada.getMotivoCancelamentoInstitucional());
        assertNotNull(cancelada.getAnalisadaEm(), "o histórico da aprovação deve ser preservado");
        assertEquals(1, documentoReservaRepository.count(), "o PDF deve continuar registrado");
        assertEquals(1, contribuicaoApmRepository.count(), "a contribuição deve continuar registrada");
        assertEquals(10, participanteReservaRepository.count());

        assertEquals(HttpStatus.NOT_FOUND,
                seguranca.get("/seguranca/reservas/" + reservaId).getStatusCode(),
                "a reserva cancelada nao deve mais ser encontrada pela segurança");
        assertTrue(aluno.get("/solicitacoes/" + reservaId).getBody()
                        .contains("Interdição da quadra para manutenção"),
                "o aluno deve ver o motivo do cancelamento");
    }

    @Test
    void solicitacaoParaHojeEhRecusadaPelaRegraDeAntecedencia() {
        Sessao aluno = entrar("aluno.demo@exemplo.local");
        LocalDate hoje = LocalDate.now(CalendarioDisponibilidadeService.FUSO_HORARIO);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("data", hoje.toString());
        form.add("horarioInicio", INICIO.toString());
        form.add("finalidade", "Tentativa no próprio dia");
        ResponseEntity<String> resposta = aluno.post("/solicitacoes", form);

        assertEquals(HttpStatus.OK, resposta.getStatusCode(),
                "o formulário é reexibido com erro, não redireciona");
        assertTrue(resposta.getBody().contains("pelo menos um dia de anteced"),
                "a recusa deve ser pela regra de antecedência, não por outro motivo");
        assertEquals(0, reservaRepository.count(), "nenhuma reserva deve ser criada");
    }

    @Test
    void usuarioDesativadoPerdeAcessoNaRequisicaoSeguinte() {
        Sessao aluno = entrar("aluno.demo@exemplo.local");
        assertTrue(aluno.get("/inicio").getStatusCode().is2xxSuccessful());

        Usuario conta = usuarioRepository.findByEmailIgnoreCase("aluno.demo@exemplo.local")
                .orElseThrow();
        conta.setAtivo(false);
        usuarioRepository.saveAndFlush(conta);

        ResponseEntity<String> depois = aluno.get("/inicio");
        assertEquals(HttpStatus.FOUND, depois.getStatusCode(),
                "a sessão deve ser encerrada na requisição seguinte");
        assertTrue(depois.getHeaders().getLocation().toString().contains("/login"));
    }

    // ------------------------------------------------------------------

    private Long criarSolicitacao(Sessao aluno) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("data", DATA.toString());
        form.add("horarioInicio", INICIO.toString());
        form.add("finalidade", "Treino da turma — demonstração");
        ResponseEntity<String> resposta = aluno.post("/solicitacoes", form);
        assertEquals(HttpStatus.FOUND, resposta.getStatusCode(),
                "a solicitação válida deve redirecionar para o detalhe");
        List<Reserva> reservas = reservaRepository.findAll();
        assertEquals(1, reservas.size());
        return reservas.get(0).getId();
    }

    private void enviarPdf(Sessao aluno, Long reservaId) {
        byte[] pdf = "%PDF-1.4\n% documento sintetico de teste\n%%EOF\n"
                .getBytes(StandardCharsets.UTF_8);
        MultiValueMap<String, Object> corpo = new LinkedMultiValueMap<>();
        corpo.add("arquivo", new ByteArrayResource(pdf) {
            @Override
            public String getFilename() {
                return "termo-ficticio.pdf";
            }
        });
        corpo.add("_csrf", aluno.csrf("/solicitacoes/" + reservaId + "/documentos"));

        HttpHeaders cabecalhos = aluno.cabecalhos();
        cabecalhos.setContentType(MediaType.MULTIPART_FORM_DATA);
        ResponseEntity<String> resposta = rest.exchange(
                url("/solicitacoes/" + reservaId + "/documentos"), HttpMethod.POST,
                new HttpEntity<>(corpo, cabecalhos), String.class);
        assertEquals(HttpStatus.FOUND, resposta.getStatusCode(), "o envio do PDF deve ser aceito");
    }

    private void criarUsuario(String email, Perfil perfil) {
        Usuario usuario = new Usuario();
        usuario.setNome("Conta fictícia " + perfil.name().toLowerCase());
        usuario.setEmail(email);
        usuario.setSenha(passwordEncoder.encode(SENHA));
        usuario.setPerfil(perfil);
        usuario.setAtivo(true);
        usuarioRepository.saveAndFlush(usuario);
    }

    private Sessao entrar(String email) {
        Sessao sessao = new Sessao();
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("email", email);
        form.add("password", SENHA);
        ResponseEntity<String> resposta = sessao.post("/login", form);
        assertEquals(HttpStatus.FOUND, resposta.getStatusCode(), "o login deve redirecionar");
        assertTrue(resposta.getHeaders().getLocation().toString().contains("/inicio"),
                "o login válido leva ao início, e não de volta ao /login");
        return sessao;
    }

    private String url(String caminho) {
        return "http://localhost:" + porta + caminho;
    }

    /** Guarda o cookie de sessão e o token CSRF, como um navegador faria. */
    private class Sessao {

        private String cookie;

        ResponseEntity<String> get(String caminho) {
            return rest.exchange(url(caminho), HttpMethod.GET,
                    new HttpEntity<>(cabecalhos()), String.class);
        }

        ResponseEntity<String> post(String caminho, MultiValueMap<String, String> form) {
            MultiValueMap<String, String> comToken = new LinkedMultiValueMap<>(form);
            comToken.add("_csrf", csrf(caminhoDeOrigem(caminho)));
            return enviar(caminho, comToken);
        }

        ResponseEntity<String> postSemCsrf(String caminho, MultiValueMap<String, String> form) {
            return enviar(caminho, form);
        }

        /**
         * POST com token valido obtido de outra pagina. Serve para provar que a
         * recusa veio da autorizacao por perfil, e nao da falta de token: quem
         * nao pode abrir a pagina de destino tambem nao conseguiria o token
         * dela.
         */
        ResponseEntity<String> postComTokenDe(String paginaDoToken, String destino,
                MultiValueMap<String, String> form) {
            MultiValueMap<String, String> comToken = new LinkedMultiValueMap<>(form);
            comToken.add("_csrf", csrf(paginaDoToken));
            return enviar(destino, comToken);
        }

        private ResponseEntity<String> enviar(String caminho, MultiValueMap<String, String> form) {
            HttpHeaders cabecalhos = cabecalhos();
            cabecalhos.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            ResponseEntity<String> resposta = rest.exchange(url(caminho), HttpMethod.POST,
                    new HttpEntity<>(form, cabecalhos), String.class);
            guardarCookie(resposta);
            return resposta;
        }

        /** Abre uma página que contenha o formulário e extrai o token. */
        String csrf(String caminho) {
            ResponseEntity<String> pagina = get(caminho);
            guardarCookie(pagina);
            Matcher m = CSRF.matcher(pagina.getBody() == null ? "" : pagina.getBody());
            assertTrue(m.find(), "não encontrei o token CSRF em " + caminho);
            return m.group(1);
        }

        private String caminhoDeOrigem(String destino) {
            if (destino.equals("/login")) {
                return "/login";
            }
            if (destino.equals("/solicitacoes")) {
                return "/solicitacoes/nova?data=" + DATA;
            }
            if (destino.endsWith("/participantes")) {
                return destino + "/novo";
            }
            if (destino.startsWith("/secretaria/documentos/")) {
                return "/secretaria/documentos/" + destino.split("/")[3];
            }
            if (destino.startsWith("/secretaria/contribuicoes/solicitacoes/")) {
                return "/secretaria/contribuicoes";
            }
            if (destino.startsWith("/admin/solicitacoes/")) {
                return "/admin/solicitacoes/" + destino.split("/")[3];
            }
            return destino;
        }

        HttpHeaders cabecalhos() {
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
    }
}
