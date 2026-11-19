package br.com.courttime.controller;

import br.com.courttime.config.SecurityConfig;
import br.com.courttime.entity.Perfil;
import br.com.courttime.entity.Quadra;
import br.com.courttime.entity.Reserva;
import br.com.courttime.entity.StatusReserva;
import br.com.courttime.entity.Usuario;
import br.com.courttime.repository.UsuarioRepository;
import br.com.courttime.service.CalendarioDisponibilidadeService;
import br.com.courttime.service.ConsultaDisponibilidade;
import br.com.courttime.service.HorarioDisponibilidade;
import br.com.courttime.service.QuadraService;
import br.com.courttime.service.ReservaService;
import br.com.courttime.service.SolicitacaoAluno;
import br.com.courttime.service.SituacaoSolicitacao;
import br.com.courttime.service.SituacaoSolicitacaoService;
import br.com.courttime.service.SituacaoHorario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SolicitacaoReservaController.class)
@Import(SecurityConfig.class)
class SolicitacaoReservaControllerTest {

    private static final LocalDate DATA = LocalDate.of(2030, 10, 21);
    private static final LocalTime INICIO = LocalTime.of(8, 0);
    private static final LocalTime FIM = LocalTime.of(9, 0);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReservaService reservaService;

    @MockitoBean
    private QuadraService quadraService;

    @MockitoBean
    private CalendarioDisponibilidadeService calendarioDisponibilidadeService;

    @MockitoBean
    private SituacaoSolicitacaoService situacaoService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    private Quadra quadra;
    private Usuario aluno;

    @BeforeEach
    void setUp() {
        quadra = new Quadra();
        quadra.setId(1L);
        quadra.setNome("Quadra Poliesportiva");
        quadra.setDescricao("Quadra esportiva da Etec.");
        quadra.setAtiva(true);

        aluno = new Usuario();
        aluno.setId(10L);
        aluno.setNome("Ana");
        aluno.setEmail("ana@escola.test");
        aluno.setSenha("hash-de-teste");
        aluno.setPerfil(Perfil.ALUNO);
        aluno.setAtivo(true);

        when(situacaoService.descrever(any(Reserva.class))).thenReturn(
                new SituacaoSolicitacao(
                        "Pendente de documentação",
                        "Revise os participantes e envie o PDF.",
                        false));
        // O @UsuarioAtual busca o usuário da sessão em toda requisição autenticada.
        when(usuarioRepository.findByEmailIgnoreCase(aluno.getEmail()))
                .thenReturn(Optional.of(aluno));
    }

    @Test
    void unauthenticatedUserIsRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/solicitacoes/nova"))
                .andExpect(status().is3xxRedirection());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMINISTRADOR", "SECRETARIA", "SEGURANCA"})
    void nonStudentProfilesCannotAccessRequests(String perfil) throws Exception {
        mockMvc.perform(get("/solicitacoes/nova")
                        .with(user("usuario@escola.test").roles(perfil)))
                .andExpect(status().isForbidden());
    }

    @Test
    void authenticatedStudentCanAccessRequestForm() throws Exception {
        prepararFormularioDisponivel();

        mockMvc.perform(get("/solicitacoes/nova")
                        .param("data", DATA.toString())
                        .with(user(aluno.getEmail()).roles("ALUNO")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Nova solicitação")))
                .andExpect(content().string(containsString("08:00 – 09:00")))
                .andExpect(content().string(containsString("solicitação preliminar")));
    }

    @Test
    void validRequestUsesAuthenticatedStudentAndShowsAccurateFeedback() throws Exception {
        prepararFormularioDisponivel();
        when(usuarioRepository.findByEmailIgnoreCase(aluno.getEmail()))
                .thenReturn(Optional.of(aluno));
        Reserva reserva = reserva(30L, aluno);
        when(reservaService.criarSolicitacao(
                aluno, quadra, DATA, INICIO, FIM, "Treino da turma"))
                .thenReturn(reserva);

        mockMvc.perform(post("/solicitacoes")
                        .with(user(aluno.getEmail()).roles("ALUNO"))
                        .with(csrf())
                        .param("data", DATA.toString())
                        .param("horarioInicio", INICIO.toString())
                        .param("finalidade", "Treino da turma"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/solicitacoes/30"))
                .andExpect(flash().attribute("mensagemSucesso", containsString(
                        "ainda depende da documentação e da aprovação")));
    }

    @Test
    void requestCannotSelectStudentThroughRequestParameter() throws Exception {
        prepararFormularioDisponivel();
        when(usuarioRepository.findByEmailIgnoreCase(aluno.getEmail()))
                .thenReturn(Optional.of(aluno));
        when(reservaService.criarSolicitacao(
                eq(aluno), eq(quadra), eq(DATA), eq(INICIO), eq(FIM), any()))
                .thenReturn(reserva(30L, aluno));

        mockMvc.perform(post("/solicitacoes")
                        .with(user(aluno.getEmail()).roles("ALUNO"))
                        .with(csrf())
                        .param("solicitanteId", "999")
                        .param("data", DATA.toString())
                        .param("horarioInicio", INICIO.toString())
                        .param("finalidade", "Treino"))
                .andExpect(status().is3xxRedirection());

        verify(reservaService).criarSolicitacao(
                aluno, quadra, DATA, INICIO, FIM, "Treino");
    }

    @Test
    void requestWithoutCsrfIsRejected() throws Exception {
        mockMvc.perform(post("/solicitacoes")
                        .with(user(aluno.getEmail()).roles("ALUNO"))
                        .param("data", DATA.toString())
                        .param("horarioInicio", INICIO.toString())
                        .param("finalidade", "Treino"))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidDateReturnsFormValidationError() throws Exception {
        prepararFormularioDisponivel();

        mockMvc.perform(post("/solicitacoes")
                        .with(user(aluno.getEmail()).roles("ALUNO"))
                        .with(csrf())
                        .param("data", "data-invalida")
                        .param("horarioInicio", INICIO.toString())
                        .param("finalidade", "Treino"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Revise os dados informados")))
                .andExpect(content().string(containsString("Informe uma data válida")))
                .andExpect(content().string(not(containsString("Failed to convert"))));
    }

    @Test
    void invalidTimeReturnsFormValidationError() throws Exception {
        prepararFormularioDisponivel();

        mockMvc.perform(post("/solicitacoes")
                        .with(user(aluno.getEmail()).roles("ALUNO"))
                        .with(csrf())
                        .param("data", DATA.toString())
                        .param("horarioInicio", "horario-invalido")
                        .param("finalidade", "Treino"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Revise os dados informados")))
                .andExpect(content().string(containsString("Informe um horário válido")))
                .andExpect(content().string(not(containsString("Failed to convert"))));
    }

    @Test
    void unavailablePeriodReturnsClearError() throws Exception {
        prepararFormularioDisponivel();
        when(usuarioRepository.findByEmailIgnoreCase(aluno.getEmail()))
                .thenReturn(Optional.of(aluno));
        when(reservaService.criarSolicitacao(any(), any(), any(), any(), any(), any()))
                .thenThrow(new IllegalStateException("O período selecionado não está disponível"));

        mockMvc.perform(post("/solicitacoes")
                        .with(user(aluno.getEmail()).roles("ALUNO"))
                        .with(csrf())
                        .param("data", DATA.toString())
                        .param("horarioInicio", INICIO.toString())
                        .param("finalidade", "Treino"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(
                        "O período selecionado não está disponível")));
    }

    @Test
    void blankOrExcessivePurposeIsRejected() throws Exception {
        prepararFormularioDisponivel();

        mockMvc.perform(post("/solicitacoes")
                        .with(user(aluno.getEmail()).roles("ALUNO"))
                        .with(csrf())
                        .param("data", DATA.toString())
                        .param("horarioInicio", INICIO.toString())
                        .param("finalidade", " "))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Informe a finalidade")));

        mockMvc.perform(post("/solicitacoes")
                        .with(user(aluno.getEmail()).roles("ALUNO"))
                        .with(csrf())
                        .param("data", DATA.toString())
                        .param("horarioInicio", INICIO.toString())
                        .param("finalidade", "a".repeat(301)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("no máximo 300 caracteres")));
    }

    @Test
    void listContainsOnlyRequestsReturnedForAuthenticatedStudent() throws Exception {
        when(usuarioRepository.findByEmailIgnoreCase(aluno.getEmail()))
                .thenReturn(Optional.of(aluno));
        Reserva solicitacao = reserva(30L, aluno);
        when(reservaService.listarSolicitacoesDoUsuario(aluno))
                .thenReturn(List.of(solicitacao));
        when(situacaoService.descrever(List.of(solicitacao))).thenReturn(List.of(
                new SolicitacaoAluno(
                        solicitacao,
                        new SituacaoSolicitacao(
                                "Pendente de documentação", "Envie o PDF.", false))));

        mockMvc.perform(get("/solicitacoes")
                        .with(user(aluno.getEmail()).roles("ALUNO")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Treino da turma")))
                .andExpect(content().string(containsString("Pendente de documentação")));
    }

    @Test
    void studentCannotAccessAnotherStudentsRequest() throws Exception {
        when(usuarioRepository.findByEmailIgnoreCase(aluno.getEmail()))
                .thenReturn(Optional.of(aluno));
        when(reservaService.obterSolicitacaoDoUsuario(99L, aluno))
                .thenThrow(new AccessDeniedException("Solicitação de outro aluno"));

        mockMvc.perform(get("/solicitacoes/99")
                        .with(user(aluno.getEmail()).roles("ALUNO")))
                .andExpect(status().isNotFound());
    }

    @Test
    void pendingRequestDetailDoesNotClaimApproval() throws Exception {
        when(usuarioRepository.findByEmailIgnoreCase(aluno.getEmail()))
                .thenReturn(Optional.of(aluno));
        when(reservaService.obterSolicitacaoDoUsuario(30L, aluno))
                .thenReturn(reserva(30L, aluno));

        mockMvc.perform(get("/solicitacoes/30")
                        .with(user(aluno.getEmail()).roles("ALUNO")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Pendente de documentação")))
                .andExpect(content().string(containsString("não representa autorização imediata")))
                .andExpect(content().string(containsString("Revisar participantes")))
                .andExpect(content().string(containsString(
                        "/solicitacoes/30/participantes")))
                .andExpect(content().string(containsString("Documentos enviados")))
                .andExpect(content().string(containsString(
                        "/solicitacoes/30/documentos")));
    }

    @Test
    void alunoVisualizaMotivoDoCancelamentoSemContatoFicticio() throws Exception {
        when(usuarioRepository.findByEmailIgnoreCase(aluno.getEmail()))
                .thenReturn(Optional.of(aluno));
        Reserva cancelada = reserva(30L, aluno);
        cancelada.setStatus(StatusReserva.CANCELADA);
        cancelada.setMotivoCancelamentoInstitucional("Interdição da quadra");
        when(reservaService.obterSolicitacaoDoUsuario(30L, aluno))
                .thenReturn(cancelada);
        when(situacaoService.descrever(cancelada)).thenReturn(
                new SituacaoSolicitacao(
                        "Cancelada pela administração",
                        "A reserva deixou de autorizar a utilização da quadra.",
                        false));

        mockMvc.perform(get("/solicitacoes/30")
                        .with(user(aluno.getEmail()).roles("ALUNO")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(
                        "Cancelada pela administração")))
                .andExpect(content().string(containsString("Interdição da quadra")))
                .andExpect(content().string(containsString("Solicitar outro horário")))
                .andExpect(content().string(containsString("/solicitacoes/nova?origem=30")));
    }

    // ---------- Remarcação após cancelamento ----------

    @Test
    void canceladaJaRemarcadaMostraLinkParaANovaEmVezDoBotao() throws Exception {
        when(usuarioRepository.findByEmailIgnoreCase(aluno.getEmail()))
                .thenReturn(Optional.of(aluno));
        Reserva cancelada = reservaCancelada(30L, aluno);
        Reserva nova = reserva(31L, aluno);
        when(reservaService.obterSolicitacaoDoUsuario(30L, aluno)).thenReturn(cancelada);
        when(reservaService.obterRemarcacao(cancelada)).thenReturn(Optional.of(nova));

        mockMvc.perform(get("/solicitacoes/30")
                        .with(user(aluno.getEmail()).roles("ALUNO")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/solicitacoes/31")))
                .andExpect(content().string(not(containsString("Solicitar outro horário"))));
    }

    @Test
    void remarcacaoMostraDeQualReservaCanceladaVeio() throws Exception {
        when(usuarioRepository.findByEmailIgnoreCase(aluno.getEmail()))
                .thenReturn(Optional.of(aluno));
        Reserva nova = reserva(31L, aluno);
        Reserva cancelada = reservaCancelada(30L, aluno);
        when(reservaService.obterSolicitacaoDoUsuario(31L, aluno)).thenReturn(nova);
        when(reservaService.obterReservaRemarcada(nova, aluno)).thenReturn(Optional.of(cancelada));

        mockMvc.perform(get("/solicitacoes/31")
                        .with(user(aluno.getEmail()).roles("ALUNO")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Esta solicitação remarca a reserva de")))
                .andExpect(content().string(containsString("/solicitacoes/30")));
    }

    @Test
    void formularioDeRemarcacaoTrazAvisoFinalidadeECaixaDeParticipantes() throws Exception {
        prepararFormularioDisponivel();
        when(usuarioRepository.findByEmailIgnoreCase(aluno.getEmail()))
                .thenReturn(Optional.of(aluno));
        Reserva cancelada = reservaCancelada(30L, aluno);
        when(reservaService.obterParaRemarcacao(30L, aluno)).thenReturn(cancelada);

        mockMvc.perform(get("/solicitacoes/nova")
                        .param("origem", "30")
                        .with(user(aluno.getEmail()).roles("ALUNO")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Remarcação de reserva cancelada")))
                .andExpect(content().string(containsString("foi cancelada pela instituição")))
                .andExpect(content().string(containsString("name=\"origemId\"")))
                .andExpect(content().string(containsString("Copiar os participantes da reserva cancelada")))
                .andExpect(content().string(containsString("Treino da turma")))
                .andExpect(content().string(containsString("name=\"origem\"")));
    }

    @Test
    void remarcacaoDeReservaDeOutroAlunoRespondeNaoEncontrado() throws Exception {
        when(usuarioRepository.findByEmailIgnoreCase(aluno.getEmail()))
                .thenReturn(Optional.of(aluno));
        when(reservaService.obterParaRemarcacao(99L, aluno))
                .thenThrow(new AccessDeniedException("não pertence"));

        mockMvc.perform(get("/solicitacoes/nova")
                        .param("origem", "99")
                        .with(user(aluno.getEmail()).roles("ALUNO")))
                .andExpect(status().isNotFound());
    }

    @Test
    void remarcacaoDeReservaNaoCanceladaVoltaAoDetalheComOMotivo() throws Exception {
        when(usuarioRepository.findByEmailIgnoreCase(aluno.getEmail()))
                .thenReturn(Optional.of(aluno));
        when(reservaService.obterParaRemarcacao(30L, aluno))
                .thenThrow(new IllegalStateException("Esta reserva cancelada já possui uma nova solicitação"));

        mockMvc.perform(get("/solicitacoes/nova")
                        .param("origem", "30")
                        .with(user(aluno.getEmail()).roles("ALUNO")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/solicitacoes/30"))
                .andExpect(flash().attribute("mensagemErro", containsString("já possui")));
    }

    @Test
    void envioDaRemarcacaoUsaOAlunoAutenticadoEAOpcaoDeCopia() throws Exception {
        prepararFormularioDisponivel();
        when(usuarioRepository.findByEmailIgnoreCase(aluno.getEmail()))
                .thenReturn(Optional.of(aluno));
        Reserva cancelada = reservaCancelada(30L, aluno);
        when(reservaService.obterSolicitacaoDoUsuario(30L, aluno)).thenReturn(cancelada);
        when(reservaService.criarRemarcacao(
                aluno, quadra, 30L, DATA, INICIO, FIM, "Treino remarcado", false))
                .thenReturn(reserva(31L, aluno));

        mockMvc.perform(post("/solicitacoes")
                        .with(user(aluno.getEmail()).roles("ALUNO"))
                        .with(csrf())
                        .param("data", DATA.toString())
                        .param("horarioInicio", INICIO.toString())
                        .param("finalidade", "Treino remarcado")
                        .param("origemId", "30")
                        .param("_copiarParticipantes", "on"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/solicitacoes/31"));
        verify(reservaService, never()).criarSolicitacao(any(), any(), any(), any(), any(), any());
    }

    @Test
    void envioDeRemarcacaoComReservaDeOutroAlunoRespondeNaoEncontrado() throws Exception {
        prepararFormularioDisponivel();
        when(usuarioRepository.findByEmailIgnoreCase(aluno.getEmail()))
                .thenReturn(Optional.of(aluno));
        when(reservaService.obterSolicitacaoDoUsuario(99L, aluno))
                .thenThrow(new AccessDeniedException("não pertence"));

        mockMvc.perform(post("/solicitacoes")
                        .with(user(aluno.getEmail()).roles("ALUNO"))
                        .with(csrf())
                        .param("data", DATA.toString())
                        .param("horarioInicio", INICIO.toString())
                        .param("finalidade", "Treino")
                        .param("origemId", "99"))
                .andExpect(status().isNotFound());
        verify(reservaService, never()).criarRemarcacao(
                any(), any(), any(), any(), any(), any(), any(), org.mockito.ArgumentMatchers.anyBoolean());
    }

    @Test
    void conflitoNaRemarcacaoReexibeFormularioComOAviso() throws Exception {
        prepararFormularioDisponivel();
        when(usuarioRepository.findByEmailIgnoreCase(aluno.getEmail()))
                .thenReturn(Optional.of(aluno));
        Reserva cancelada = reservaCancelada(30L, aluno);
        when(reservaService.obterSolicitacaoDoUsuario(30L, aluno)).thenReturn(cancelada);
        when(reservaService.criarRemarcacao(
                aluno, quadra, 30L, DATA, INICIO, FIM, "Treino", true))
                .thenThrow(new IllegalStateException("O período selecionado não está disponível"));

        mockMvc.perform(post("/solicitacoes")
                        .with(user(aluno.getEmail()).roles("ALUNO"))
                        .with(csrf())
                        .param("data", DATA.toString())
                        .param("horarioInicio", INICIO.toString())
                        .param("finalidade", "Treino")
                        .param("origemId", "30")
                        .param("copiarParticipantes", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("O período selecionado não está disponível")))
                .andExpect(content().string(containsString("Remarcação de reserva cancelada")));
    }

    private void prepararFormularioDisponivel() {
        HorarioDisponibilidade horario = new HorarioDisponibilidade(
                INICIO, FIM, SituacaoHorario.DISPONIVEL);
        when(quadraService.obterQuadra()).thenReturn(quadra);
        when(calendarioDisponibilidadeService.obterDataMinimaParaSolicitacao())
                .thenReturn(DATA);
        when(calendarioDisponibilidadeService.consultar(quadra, DATA))
                .thenReturn(new ConsultaDisponibilidade(DATA, true, List.of(horario)));
    }

    private Reserva reservaCancelada(Long id, Usuario solicitante) {
        Reserva reserva = reserva(id, solicitante);
        reserva.setStatus(StatusReserva.CANCELADA);
        reserva.setMotivoCancelamentoInstitucional("Interdição da quadra");
        return reserva;
    }

    private Reserva reserva(Long id, Usuario solicitante) {
        Reserva reserva = new Reserva();
        reserva.setId(id);
        reserva.setQuadra(quadra);
        reserva.setSolicitante(solicitante);
        reserva.setDataUtilizacao(DATA);
        reserva.setHorarioInicio(INICIO);
        reserva.setHorarioFim(FIM);
        reserva.setFinalidade("Treino da turma");
        reserva.setStatus(StatusReserva.PENDENTE_DOCUMENTACAO);
        return reserva;
    }
}
