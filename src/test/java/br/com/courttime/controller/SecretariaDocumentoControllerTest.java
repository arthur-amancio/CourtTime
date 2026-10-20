package br.com.courttime.controller;

import br.com.courttime.config.SecurityConfig;
import br.com.courttime.entity.ConferenciaDocumento;
import br.com.courttime.entity.DocumentoReserva;
import br.com.courttime.entity.Perfil;
import br.com.courttime.entity.Reserva;
import br.com.courttime.entity.ResultadoConferenciaDocumento;
import br.com.courttime.entity.StatusReserva;
import br.com.courttime.entity.Usuario;
import br.com.courttime.repository.UsuarioRepository;
import br.com.courttime.service.ConferenciaDocumentoService;
import br.com.courttime.service.DownloadDocumento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
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

@WebMvcTest(SecretariaDocumentoController.class)
@Import(SecurityConfig.class)
class SecretariaDocumentoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConferenciaDocumentoService conferenciaService;
    @MockitoBean
    private UsuarioRepository usuarioRepository;

    private Usuario secretaria;
    private DocumentoReserva documento;
    private byte[] pdf;

    @BeforeEach
    void setUp() {
        secretaria = usuario(10L, "Secretaria da escola", Perfil.SECRETARIA);
        Usuario aluno = usuario(11L, "Ana Aluna", Perfil.ALUNO);
        Reserva reserva = new Reserva();
        reserva.setId(20L);
        reserva.setSolicitante(aluno);
        reserva.setStatus(StatusReserva.PENDENTE_DOCUMENTACAO);
        documento = new DocumentoReserva();
        documento.setId(30L);
        documento.setReserva(reserva);
        documento.setVersao(2);
        documento.setNomeOriginal("termo.pdf");
        documento.setChaveArmazenamento("123e4567-e89b-12d3-a456-426614174000.pdf");
        documento.setSha256("0".repeat(64));
        documento.setTamanhoBytes(8);
        documento.setEnviadoPor(aluno);
        ReflectionTestUtils.setField(documento, "enviadoEm", Instant.parse("2026-10-18T10:00:00Z"));
        pdf = "%PDF-1.7".getBytes(StandardCharsets.US_ASCII);
    }

    @Test
    void usuarioNaoAutenticadoEEnviadoAoLogin() throws Exception {
        mockMvc.perform(get("/secretaria/documentos"))
                .andExpect(status().is3xxRedirection());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ALUNO", "ADMINISTRADOR", "SEGURANCA"})
    void outrosPerfisNaoAcessamFilaNemDecidem(String perfil) throws Exception {
        mockMvc.perform(get("/secretaria/documentos")
                        .with(user("usuario@escola.test").roles(perfil)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/secretaria/documentos/30/validar")
                        .with(user("usuario@escola.test").roles(perfil))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void secretariaVisualizaFilaEHistoricoSemAcaoDeAprovacao() throws Exception {
        prepararSecretaria();
        ConferenciaDocumento conferencia = conferenciaDevolvida();
        when(conferenciaService.listarPendentes(secretaria)).thenReturn(List.of(documento));
        when(conferenciaService.listarHistorico(secretaria)).thenReturn(List.of(conferencia));

        mockMvc.perform(get("/secretaria/documentos")
                        .with(user(secretaria.getEmail()).roles("SECRETARIA")))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(content().string(containsString("Ana Aluna")))
                .andExpect(content().string(containsString(
                        "Documentos aguardando conferência")))
                .andExpect(content().string(containsString(
                        "Histórico de conferências documentais")))
                .andExpect(content().string(containsString("Devolvido para correção")))
                .andExpect(content().string(containsString("Secretaria da escola")));
    }

    @Test
    void secretariaConsultaDetalheEBaixaOriginalPrivado() throws Exception {
        prepararSecretaria();
        when(conferenciaService.obter(30L, secretaria)).thenReturn(documento);
        when(conferenciaService.podeConferir(documento, secretaria)).thenReturn(true);
        when(conferenciaService.baixar(30L, secretaria)).thenReturn(
                new DownloadDocumento("termo.pdf", pdf.length, new ByteArrayResource(pdf)));

        mockMvc.perform(get("/secretaria/documentos/30")
                        .with(user(secretaria.getEmail()).roles("SECRETARIA")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Conferir documento")))
                .andExpect(content().string(containsString("não altera o PDF original")))
                .andExpect(content().string(containsString(
                        "não aprova a utilização da quadra")))
                .andExpect(content().string(containsString("Validar documento")));

        mockMvc.perform(get("/secretaria/documentos/30/baixar")
                        .with(user(secretaria.getEmail()).roles("SECRETARIA")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(content().bytes(pdf));
    }

    @Test
    void versaoSubstituidaNaoExibeAcoesDeDecisao() throws Exception {
        prepararSecretaria();
        when(conferenciaService.obter(30L, secretaria)).thenReturn(documento);
        when(conferenciaService.podeConferir(documento, secretaria)).thenReturn(false);

        mockMvc.perform(get("/secretaria/documentos/30")
                        .with(user(secretaria.getEmail()).roles("SECRETARIA")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(
                        "não está disponível para uma nova conferência")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        containsString("Validar documento"))));
    }

    @Test
    void validacaoUsaOperadorAutenticadoEExigeCsrf() throws Exception {
        mockMvc.perform(post("/secretaria/documentos/30/validar")
                        .with(user(secretaria.getEmail()).roles("SECRETARIA")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(conferenciaService);

        prepararSecretaria();
        mockMvc.perform(post("/secretaria/documentos/30/validar")
                        .with(user(secretaria.getEmail()).roles("SECRETARIA"))
                        .with(csrf())
                        .param("secretariaId", "999"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/secretaria/documentos"))
                .andExpect(flash().attribute("mensagemSucesso", "Documento conferido e validado."));

        verify(conferenciaService).validar(30L, secretaria);
    }

    @Test
    void devolucaoExigeCsrfEJustificativaDoBackend() throws Exception {
        mockMvc.perform(post("/secretaria/documentos/30/devolver")
                        .with(user(secretaria.getEmail()).roles("SECRETARIA"))
                        .param("justificativa", "Falta a última página."))
                .andExpect(status().isForbidden());
        verifyNoInteractions(conferenciaService);

        prepararSecretaria();
        when(conferenciaService.devolver(30L, secretaria, ""))
                .thenThrow(new IllegalArgumentException("A justificativa da devolução é obrigatória"));
        mockMvc.perform(post("/secretaria/documentos/30/devolver")
                        .with(user(secretaria.getEmail()).roles("SECRETARIA"))
                        .with(csrf())
                        .param("justificativa", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/secretaria/documentos/30"))
                .andExpect(flash().attribute("mensagemErro", containsString("obrigatória")));
    }

    /**
     * O teste acima garante que o controller ENVIA a mensagem de erro. Este
     * garante que a pagina REALMENTE a mostra. Sem ele, a secretaria via a
     * tela recarregar sem explicacao nenhuma.
     */
    @Test
    void paginaDeConferenciaExibeOErroDevolvidoPeloBackend() throws Exception {
        prepararSecretaria();
        when(conferenciaService.obter(30L, secretaria)).thenReturn(documento);
        when(conferenciaService.podeConferir(documento, secretaria)).thenReturn(true);

        mockMvc.perform(get("/secretaria/documentos/30")
                        .with(user(secretaria.getEmail()).roles("SECRETARIA"))
                        .flashAttr("mensagemErro",
                                "A justificativa da devolução é obrigatória"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(
                        "A justificativa da devolução é obrigatória")));
    }

    @Test
    void documentoInexistenteRetornaNaoEncontrado() throws Exception {
        prepararSecretaria();
        when(conferenciaService.obter(99L, secretaria))
                .thenThrow(new IllegalArgumentException("Documento não encontrado"));

        mockMvc.perform(get("/secretaria/documentos/99")
                        .with(user(secretaria.getEmail()).roles("SECRETARIA")))
                .andExpect(status().isNotFound());
    }

    private ConferenciaDocumento conferenciaDevolvida() {
        ConferenciaDocumento conferencia = new ConferenciaDocumento();
        conferencia.setDocumento(documento);
        conferencia.setConferidoPor(secretaria);
        conferencia.setResultado(ResultadoConferenciaDocumento.DEVOLVIDO_CORRECAO);
        conferencia.setJustificativa("Falta a última página.");
        ReflectionTestUtils.setField(
                conferencia, "conferidoEm", Instant.parse("2026-10-18T11:00:00Z"));
        return conferencia;
    }

    private void prepararSecretaria() {
        when(usuarioRepository.findByEmailIgnoreCase(secretaria.getEmail()))
                .thenReturn(Optional.of(secretaria));
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
