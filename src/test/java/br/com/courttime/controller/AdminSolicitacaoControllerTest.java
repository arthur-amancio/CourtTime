package br.com.courttime.controller;

import br.com.courttime.config.SecurityConfig;
import br.com.courttime.entity.CategoriaParticipante;
import br.com.courttime.entity.ConferenciaDocumento;
import br.com.courttime.entity.ContribuicaoApm;
import br.com.courttime.entity.DocumentoReserva;
import br.com.courttime.entity.ParticipanteReserva;
import br.com.courttime.entity.Perfil;
import br.com.courttime.entity.Quadra;
import br.com.courttime.entity.Reserva;
import br.com.courttime.entity.ResultadoConferenciaDocumento;
import br.com.courttime.entity.StatusReserva;
import br.com.courttime.entity.Usuario;
import br.com.courttime.repository.UsuarioRepository;
import br.com.courttime.service.AnaliseAdministrativaService;
import br.com.courttime.service.DetalheAnaliseAdministrativa;
import br.com.courttime.service.DownloadDocumento;
import br.com.courttime.service.ReservaService;
import br.com.courttime.service.ResumoParticipantes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
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

@WebMvcTest(AdminSolicitacaoController.class)
@Import(SecurityConfig.class)
class AdminSolicitacaoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnaliseAdministrativaService analiseService;
    @MockitoBean
    private ReservaService reservaService;
    @MockitoBean
    private UsuarioRepository usuarioRepository;

    private Usuario administrador;
    private Reserva reserva;
    private DocumentoReserva documento;

    @BeforeEach
    void setUp() {
        administrador = usuario(1L, "Administrador", Perfil.ADMINISTRADOR);
        Usuario aluno = usuario(2L, "Ana Aluna", Perfil.ALUNO);
        Quadra quadra = new Quadra();
        quadra.setId(3L);
        quadra.setNome("Quadra Poliesportiva");
        reserva = new Reserva();
        reserva.setId(20L);
        reserva.setSolicitante(aluno);
        reserva.setQuadra(quadra);
        reserva.setDataUtilizacao(LocalDate.of(2030, 10, 21));
        reserva.setHorarioInicio(LocalTime.of(8, 0));
        reserva.setHorarioFim(LocalTime.of(9, 0));
        reserva.setFinalidade("Treino da turma");
        reserva.setStatus(StatusReserva.PENDENTE_ANALISE);

        documento = new DocumentoReserva();
        documento.setId(30L);
        documento.setReserva(reserva);
        documento.setVersao(2);
        documento.setNomeOriginal("termo-assinado.pdf");
        documento.setTamanhoBytes(8);
        ConferenciaDocumento conferencia = new ConferenciaDocumento();
        conferencia.setDocumento(documento);
        conferencia.setResultado(ResultadoConferenciaDocumento.VALIDADO);
        ReflectionTestUtils.setField(documento, "conferencia", conferencia);
    }

    @Test
    void usuarioNaoAutenticadoEEnviadoAoLogin() throws Exception {
        mockMvc.perform(get("/admin/solicitacoes"))
                .andExpect(status().is3xxRedirection());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ALUNO", "SECRETARIA", "SEGURANCA"})
    void somenteAdministradorAcessaOuDecide(String perfil) throws Exception {
        mockMvc.perform(get("/admin/solicitacoes")
                        .with(user("usuario@escola.test").roles(perfil)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/admin/solicitacoes/20/aprovar")
                        .with(user("usuario@escola.test").roles(perfil))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void administradorVisualizaFilaEDetalheCompleto() throws Exception {
        prepararAdministrador();
        when(analiseService.listarPendentes(administrador)).thenReturn(List.of(reserva));
        when(analiseService.detalhar(20L, administrador)).thenReturn(detalhe());

        mockMvc.perform(get("/admin/solicitacoes")
                        .with(user(administrador.getEmail()).roles("ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Análise de solicitações")))
                .andExpect(content().string(containsString("Ana Aluna")))
                .andExpect(content().string(containsString("Treino da turma")));

        mockMvc.perform(get("/admin/solicitacoes/20")
                        .with(user(administrador.getEmail()).roles("ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("termo-assinado.pdf")))
                .andExpect(content().string(containsString("10 aluno(s)")))
                .andExpect(content().string(containsString(
                        "não valida criptograficamente a assinatura")))
                .andExpect(content().string(containsString("Aprovar e autorizar reserva")));
    }

    @Test
    void downloadPrivadoNaoExpoeChaveERecusaDocumentoDeOutraSolicitacao() throws Exception {
        prepararAdministrador();
        byte[] pdf = "%PDF-1.7".getBytes();
        when(analiseService.baixar(20L, 30L, administrador)).thenReturn(
                new DownloadDocumento(
                        "termo-assinado.pdf", pdf.length, new ByteArrayResource(pdf)));

        mockMvc.perform(get("/admin/solicitacoes/20/documentos/30/baixar")
                        .with(user(administrador.getEmail()).roles("ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(content().bytes(pdf));

        when(analiseService.baixar(20L, 99L, administrador)).thenThrow(
                new AccessDeniedException("Documento de outra solicitação"));
        mockMvc.perform(get("/admin/solicitacoes/20/documentos/99/baixar")
                        .with(user(administrador.getEmail()).roles("ADMINISTRADOR")))
                .andExpect(status().isNotFound());
    }

    @Test
    void aprovacaoExigeCsrfEUsaAdministradorDaSessaoSemMotivo() throws Exception {
        mockMvc.perform(post("/admin/solicitacoes/20/aprovar")
                        .with(user(administrador.getEmail()).roles("ADMINISTRADOR")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(reservaService);

        prepararAdministrador();
        mockMvc.perform(post("/admin/solicitacoes/20/aprovar")
                        .with(user(administrador.getEmail()).roles("ADMINISTRADOR"))
                        .with(csrf())
                        .param("administradorId", "999"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/solicitacoes"))
                .andExpect(flash().attribute(
                        "mensagemSucesso", "Solicitação aprovada e reserva autorizada."));

        verify(reservaService).aprovar(20L, administrador);
    }

    @Test
    void rejeicaoExigeMotivoValidadoNoBackend() throws Exception {
        prepararAdministrador();
        when(reservaService.rejeitar(20L, administrador, ""))
                .thenThrow(new IllegalArgumentException("O motivo da rejeição é obrigatório"));

        mockMvc.perform(post("/admin/solicitacoes/20/rejeitar")
                        .with(user(administrador.getEmail()).roles("ADMINISTRADOR"))
                        .with(csrf())
                        .param("motivo", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/solicitacoes/20"))
                .andExpect(flash().attribute(
                        "mensagemErro", containsString("obrigatório")));
    }

    private DetalheAnaliseAdministrativa detalhe() {
        ParticipanteReserva participante = new ParticipanteReserva();
        participante.setNomeCompleto("Ana Participante");
        participante.setRmOuRg("RM-123");
        participante.setCategoria(CategoriaParticipante.ALUNO);
        ContribuicaoApm contribuicao = new ContribuicaoApm();
        contribuicao.setValor(new BigDecimal("20.00"));
        contribuicao.setConferenciaDocumento(documento.getConferencia());
        contribuicao.setRegistradaPor(usuario(4L, "Secretaria", Perfil.SECRETARIA));
        ReflectionTestUtils.setField(
                contribuicao, "registradaEm", Instant.parse("2026-10-18T10:00:00Z"));
        return new DetalheAnaliseAdministrativa(
                reserva,
                List.of(participante),
                new ResumoParticipantes(10, 1, 11, true),
                documento,
                contribuicao);
    }

    private void prepararAdministrador() {
        when(usuarioRepository.findByEmailIgnoreCase(administrador.getEmail()))
                .thenReturn(Optional.of(administrador));
    }

    private Usuario usuario(Long id, String nome, Perfil perfil) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNome(nome);
        usuario.setEmail("usuario" + id + "@teste.local");
        usuario.setSenha("hash");
        usuario.setPerfil(perfil);
        usuario.setAtivo(true);
        return usuario;
    }
}
