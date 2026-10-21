package br.com.courttime.controller;

import br.com.courttime.config.SecurityConfig;
import br.com.courttime.entity.CategoriaParticipante;
import br.com.courttime.entity.ParticipanteReserva;
import br.com.courttime.entity.Perfil;
import br.com.courttime.entity.Quadra;
import br.com.courttime.entity.Reserva;
import br.com.courttime.entity.StatusReserva;
import br.com.courttime.entity.Usuario;
import br.com.courttime.entity.VinculoParticipante;
import br.com.courttime.repository.UsuarioRepository;
import br.com.courttime.service.ParticipanteReservaService;
import br.com.courttime.service.ReservaService;
import br.com.courttime.service.ResumoParticipantes;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ParticipanteReservaController.class)
@Import(SecurityConfig.class)
class ParticipanteReservaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ParticipanteReservaService participanteService;
    @MockitoBean
    private ReservaService reservaService;
    @MockitoBean
    private UsuarioRepository usuarioRepository;

    private Usuario aluno;
    private Reserva reserva;
    private ParticipanteReserva participante;

    @BeforeEach
    void setUp() {
        aluno = new Usuario();
        aluno.setId(10L);
        aluno.setNome("Ana Aluna");
        aluno.setEmail("ana@escola.test");
        aluno.setSenha("hash-de-teste");
        aluno.setPerfil(Perfil.ALUNO);
        aluno.setAtivo(true);

        Quadra quadra = new Quadra();
        quadra.setId(1L);
        quadra.setNome("Quadra Poliesportiva");
        quadra.setDescricao("Quadra esportiva da ETEC.");
        quadra.setAtiva(true);

        reserva = new Reserva();
        reserva.setId(20L);
        reserva.setSolicitante(aluno);
        reserva.setQuadra(quadra);
        reserva.setDataUtilizacao(LocalDate.of(2030, 10, 21));
        reserva.setHorarioInicio(LocalTime.of(8, 0));
        reserva.setHorarioFim(LocalTime.of(9, 0));
        reserva.setFinalidade("Treino da turma");
        reserva.setStatus(StatusReserva.PENDENTE_DOCUMENTACAO);

        participante = new ParticipanteReserva();
        participante.setId(30L);
        participante.setReserva(reserva);
        participante.setNomeCompleto("Beatriz Almeida");
        participante.setRmOuRg("RM-1234");
        participante.setCategoria(CategoriaParticipante.ALUNO);
    }

    @Test
    void usuarioNaoAutenticadoEEnviadoAoLogin() throws Exception {
        mockMvc.perform(get("/solicitacoes/20/participantes"))
                .andExpect(status().is3xxRedirection());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMINISTRADOR", "SECRETARIA", "SEGURANCA"})
    void somenteAlunoAcessaParticipantesDaSolicitacao(String perfil) throws Exception {
        mockMvc.perform(get("/solicitacoes/20/participantes")
                        .with(user("usuario@escola.test").roles(perfil)))
                .andExpect(status().isForbidden());
    }

    @Test
    void alunoRevisaListaPropriaComContagensSeparadasESemCache() throws Exception {
        prepararAlunoEReserva();
        when(participanteService.listar(20L, aluno)).thenReturn(List.of(participante));
        when(participanteService.resumir(20L, aluno))
                .thenReturn(new ResumoParticipantes(10, 2, 12, true));

        mockMvc.perform(get("/solicitacoes/20/participantes")
                        .with(user(aluno.getEmail()).roles("ALUNO")))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(content().string(containsString("Beatriz Almeida")))
                .andExpect(content().string(containsString("RM-1234")))
                .andExpect(content().string(containsString("Demais participantes")))
                .andExpect(content().string(containsString("Mínimo de alunos atendido")))
                .andExpect(content().string(containsString("Não há limite máximo de participantes")));
    }

    @Test
    void solicitacaoDeOutroAlunoRetornaNaoEncontrado() throws Exception {
        prepararAluno();
        when(reservaService.obterSolicitacaoDoUsuario(99L, aluno))
                .thenThrow(new AccessDeniedException("Solicitação de outro aluno"));

        mockMvc.perform(get("/solicitacoes/99/participantes")
                        .with(user(aluno.getEmail()).roles("ALUNO")))
                .andExpect(status().isNotFound());

        verifyNoInteractions(participanteService);
    }

    @Test
    void cadastroValidoUsaAlunoAutenticado() throws Exception {
        prepararAlunoEReserva();

        mockMvc.perform(post("/solicitacoes/20/participantes")
                        .with(user(aluno.getEmail()).roles("ALUNO"))
                        .with(csrf())
                        .param("nomeCompleto", "Beatriz Almeida")
                        .param("rmOuRg", "RM-1234")
                        .param("categoria", "ALUNO")
                        .param("solicitanteId", "999"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/solicitacoes/20/participantes"))
                .andExpect(flash().attribute("mensagemSucesso", "Participante adicionado à solicitação."));

        verify(participanteService).adicionar(
                20L, aluno, "Beatriz Almeida", "RM-1234", CategoriaParticipante.ALUNO, null);
    }

    @Test
    void cadastroInvalidoAssociaErrosAosCampos() throws Exception {
        prepararAlunoEReserva();

        mockMvc.perform(post("/solicitacoes/20/participantes")
                        .with(user(aluno.getEmail()).roles("ALUNO"))
                        .with(csrf())
                        .param("nomeCompleto", "")
                        .param("rmOuRg", "")
                        .param("categoria", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Revise os dados informados")))
                .andExpect(content().string(containsString("aria-invalid=\"true\"")))
                .andExpect(content().string(containsString("id=\"nomeCompleto-erro\"")))
                .andExpect(content().string(containsString("id=\"rmOuRg-erro\"")))
                .andExpect(content().string(containsString("id=\"categoria-erro\"")));

        verify(participanteService, never()).adicionar(
                20L, aluno, "", "", null, null);
    }

    @Test
    void r09FormularioOfereceOsQuatroVinculosEMostraARecusaDoServico() throws Exception {
        prepararAlunoEReserva();
        when(participanteService.adicionar(
                20L, aluno, "Carlos Exemplo", "RG-77", CategoriaParticipante.OUTRO, null))
                .thenThrow(new IllegalArgumentException(
                        ParticipanteReservaService.MENSAGEM_VINCULO_OBRIGATORIO));

        mockMvc.perform(get("/solicitacoes/20/participantes/novo")
                        .with(user(aluno.getEmail()).roles("ALUNO")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ex-aluno da ETEC")))
                .andExpect(content().string(containsString("Pai ou mãe de aluno")))
                .andExpect(content().string(containsString("Irmão ou irmã de aluno")))
                .andExpect(content().string(containsString("Professor da ETEC")))
                .andExpect(content().string(not(containsString("value=\"AMIGO\""))));

        mockMvc.perform(post("/solicitacoes/20/participantes")
                        .with(user(aluno.getEmail()).roles("ALUNO"))
                        .with(csrf())
                        .param("nomeCompleto", "Carlos Exemplo")
                        .param("rmOuRg", "RG-77")
                        .param("categoria", "OUTRO"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("escolha o vínculo")));
    }

    @Test
    void cadastroSemCsrfERecusadoSemAcionarServico() throws Exception {
        mockMvc.perform(post("/solicitacoes/20/participantes")
                        .with(user(aluno.getEmail()).roles("ALUNO"))
                        .param("nomeCompleto", "Beatriz Almeida")
                        .param("rmOuRg", "RM-1234")
                        .param("categoria", "ALUNO"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(participanteService);
    }

    @Test
    void edicaoCarregaParticipanteDaSolicitacaoEAtualizaDados() throws Exception {
        prepararAlunoEReserva();
        when(participanteService.obter(20L, 30L, aluno)).thenReturn(participante);

        mockMvc.perform(get("/solicitacoes/20/participantes/30/editar")
                        .with(user(aluno.getEmail()).roles("ALUNO")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Editar participante")))
                .andExpect(content().string(containsString("Beatriz Almeida")));

        mockMvc.perform(post("/solicitacoes/20/participantes/30")
                        .with(user(aluno.getEmail()).roles("ALUNO"))
                        .with(csrf())
                        .param("nomeCompleto", "Beatriz Souza")
                        .param("rmOuRg", "RG-5678")
                        .param("categoria", "OUTRO")
                        .param("vinculo", "EX_ALUNO"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/solicitacoes/20/participantes"));

        verify(participanteService).editar(
                20L, 30L, aluno, "Beatriz Souza", "RG-5678", CategoriaParticipante.OUTRO,
                VinculoParticipante.EX_ALUNO);
    }

    @Test
    void idDeParticipanteDeOutraSolicitacaoRetornaNaoEncontrado() throws Exception {
        prepararAlunoEReserva();
        when(participanteService.obter(20L, 99L, aluno))
                .thenThrow(new AccessDeniedException("Participante de outra solicitação"));

        mockMvc.perform(get("/solicitacoes/20/participantes/99/editar")
                        .with(user(aluno.getEmail()).roles("ALUNO")))
                .andExpect(status().isNotFound());
    }

    @Test
    void remocaoExigeCsrfEUsaAlunoAutenticado() throws Exception {
        mockMvc.perform(post("/solicitacoes/20/participantes/30/remover")
                        .with(user(aluno.getEmail()).roles("ALUNO")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(participanteService);

        prepararAluno();
        mockMvc.perform(post("/solicitacoes/20/participantes/30/remover")
                        .with(user(aluno.getEmail()).roles("ALUNO"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/solicitacoes/20/participantes"));

        verify(participanteService).remover(20L, 30L, aluno);
    }

    @Test
    void solicitacaoQueAvancouFicaSomenteParaConsulta() throws Exception {
        reserva.setStatus(StatusReserva.PENDENTE_ANALISE);
        prepararAlunoEReserva();
        when(participanteService.listar(20L, aluno)).thenReturn(List.of(participante));
        when(participanteService.resumir(20L, aluno))
                .thenReturn(new ResumoParticipantes(1, 0, 1, false));

        mockMvc.perform(get("/solicitacoes/20/participantes")
                        .with(user(aluno.getEmail()).roles("ALUNO")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("somente para consulta")))
                .andExpect(content().string(not(containsString("Adicionar participante"))))
                .andExpect(content().string(not(containsString(">Editar<"))))
                .andExpect(content().string(not(containsString(">Remover<"))));

        mockMvc.perform(get("/solicitacoes/20/participantes/novo")
                        .with(user(aluno.getEmail()).roles("ALUNO")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/solicitacoes/20/participantes"))
                .andExpect(flash().attribute("mensagemErro", containsString(
                        "só podem ser alterados")));
    }

    @Test
    void documentoAguardandoConferenciaOcultaEdicaoMesmoComStatusDocumental() throws Exception {
        prepararAlunoEReserva();
        when(participanteService.podeAlterar(20L, aluno)).thenReturn(false);
        when(participanteService.listar(20L, aluno)).thenReturn(List.of(participante));
        when(participanteService.resumir(20L, aluno))
                .thenReturn(new ResumoParticipantes(10, 0, 10, true));

        mockMvc.perform(get("/solicitacoes/20/participantes")
                        .with(user(aluno.getEmail()).roles("ALUNO")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(
                        "Alterações voltam a ser permitidas se a secretaria devolver")))
                .andExpect(content().string(not(containsString("Adicionar participante"))));
    }

    @Test
    void mutacaoRecusadaPeloServicoMostraErroSemAlegarSucesso() throws Exception {
        prepararAlunoEReserva();
        when(participanteService.adicionar(
                20L, aluno, "Beatriz Almeida", "RM-1234", CategoriaParticipante.ALUNO, null))
                .thenThrow(new IllegalStateException(
                        "Os participantes só podem ser alterados enquanto a documentação está pendente"));

        mockMvc.perform(post("/solicitacoes/20/participantes")
                        .with(user(aluno.getEmail()).roles("ALUNO"))
                        .with(csrf())
                        .param("nomeCompleto", "Beatriz Almeida")
                        .param("rmOuRg", "RM-1234")
                        .param("categoria", "ALUNO"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/solicitacoes/20/participantes"))
                .andExpect(flash().attribute("mensagemErro", containsString(
                        "só podem ser alterados")));
    }

    private void prepararAluno() {
        when(usuarioRepository.findByEmailIgnoreCase(aluno.getEmail()))
                .thenReturn(Optional.of(aluno));
    }

    private void prepararAlunoEReserva() {
        prepararAluno();
        when(reservaService.obterSolicitacaoDoUsuario(20L, aluno)).thenReturn(reserva);
        when(participanteService.podeAlterar(20L, aluno))
                .thenReturn(reserva.getStatus() == StatusReserva.PENDENTE_DOCUMENTACAO);
    }
}
