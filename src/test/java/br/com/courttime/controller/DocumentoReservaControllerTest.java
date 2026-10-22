package br.com.courttime.controller;

import br.com.courttime.config.SecurityConfig;
import br.com.courttime.entity.ConferenciaDocumento;
import br.com.courttime.entity.DocumentoReserva;
import br.com.courttime.entity.Perfil;
import br.com.courttime.entity.Quadra;
import br.com.courttime.entity.Reserva;
import br.com.courttime.entity.ResultadoConferenciaDocumento;
import br.com.courttime.entity.StatusReserva;
import br.com.courttime.entity.Usuario;
import br.com.courttime.repository.UsuarioRepository;
import br.com.courttime.service.DocumentoReservaService;
import br.com.courttime.service.DownloadDocumento;
import br.com.courttime.service.ReservaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DocumentoReservaController.class)
@Import(SecurityConfig.class)
class DocumentoReservaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DocumentoReservaService documentoService;
    @MockitoBean
    private ReservaService reservaService;
    @MockitoBean
    private UsuarioRepository usuarioRepository;

    private Usuario aluno;
    private Reserva reserva;
    private byte[] pdf;

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
        quadra.setDescricao("Quadra esportiva da Etec.");
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
        pdf = "%PDF-1.7\nconteudo".getBytes(StandardCharsets.US_ASCII);
    }

    @Test
    void usuarioNaoAutenticadoEEnviadoAoLogin() throws Exception {
        mockMvc.perform(get("/solicitacoes/20/documentos"))
                .andExpect(status().is3xxRedirection());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMINISTRADOR", "SECRETARIA", "SEGURANCA"})
    void somenteAlunoAcessaDocumentosDaSolicitacao(String perfil) throws Exception {
        mockMvc.perform(get("/solicitacoes/20/documentos")
                        .with(user("usuario@escola.test").roles(perfil)))
                .andExpect(status().isForbidden());
    }

    @Test
    void alunoVisualizaHistoricoSemAlegacaoDeAssinaturaValidada() throws Exception {
        prepararAlunoEReserva();
        DocumentoReserva documento = documento(30L, 1, "termo externo.pdf");
        when(documentoService.listar(20L, aluno)).thenReturn(List.of(documento));

        mockMvc.perform(get("/solicitacoes/20/documentos")
                        .with(user(aluno.getEmail()).roles("ALUNO")))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(content().string(containsString("termo externo.pdf")))
                .andExpect(content().string(containsString("Aguardando conferência")))
                .andExpect(content().string(containsString(
                        "não comprova nem valida a assinatura")))
                .andExpect(content().string(containsString(
                        "Use o formulário oficial da Etec")))
                .andExpect(content().string(not(containsString("Assinatura validada"))));
    }

    @Test
    void alunoVisualizaDevolucaoEIdentificaVersaoSubstituida() throws Exception {
        prepararAlunoEReserva();
        DocumentoReserva versaoAnterior = documento(30L, 1, "termo inicial.pdf");
        DocumentoReserva versaoMaisRecente = documento(31L, 2, "termo corrigido.pdf");
        ConferenciaDocumento conferencia = new ConferenciaDocumento();
        conferencia.setDocumento(versaoAnterior);
        conferencia.setResultado(ResultadoConferenciaDocumento.DEVOLVIDO_CORRECAO);
        conferencia.setJustificativa("Falta a página final.");
        conferencia.setConferidoPor(aluno);
        ReflectionTestUtils.setField(versaoAnterior, "conferencia", conferencia);
        when(documentoService.listar(20L, aluno))
                .thenReturn(List.of(versaoMaisRecente, versaoAnterior));

        mockMvc.perform(get("/solicitacoes/20/documentos")
                        .with(user(aluno.getEmail()).roles("ALUNO")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Devolvido para correção")))
                .andExpect(content().string(containsString("Falta a página final.")))
                .andExpect(content().string(containsString("Aguardando conferência")));
    }

    @Test
    void versaoAntigaSemConferenciaEExibidaComoSubstituida() throws Exception {
        prepararAlunoEReserva();
        DocumentoReserva versaoAnterior = documento(30L, 1, "termo inicial.pdf");
        DocumentoReserva versaoMaisRecente = documento(31L, 2, "termo corrigido.pdf");
        when(documentoService.listar(20L, aluno))
                .thenReturn(List.of(versaoMaisRecente, versaoAnterior));

        mockMvc.perform(get("/solicitacoes/20/documentos")
                        .with(user(aluno.getEmail()).roles("ALUNO")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(
                        "Substituída por versão mais recente")));
    }

    @Test
    void solicitacaoAlheiaRetornaNaoEncontradoSemConsultarDocumentos() throws Exception {
        prepararAluno();
        when(reservaService.obterSolicitacaoDoUsuario(99L, aluno))
                .thenThrow(new AccessDeniedException("Solicitação de outro aluno"));

        mockMvc.perform(get("/solicitacoes/99/documentos")
                        .with(user(aluno.getEmail()).roles("ALUNO")))
                .andExpect(status().isNotFound());

        verifyNoInteractions(documentoService);
    }

    @Test
    void uploadValidoUsaAlunoAutenticadoEBytesOriginais() throws Exception {
        prepararAluno();
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo", "termo.pdf", "application/pdf", pdf);

        mockMvc.perform(multipart("/solicitacoes/20/documentos")
                        .file(arquivo)
                        .with(user(aluno.getEmail()).roles("ALUNO"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/solicitacoes/20/documentos"))
                .andExpect(flash().attribute("mensagemSucesso", containsString(
                        "aguardando conferência institucional")));

        verify(documentoService).enviar(
                20L, aluno, "termo.pdf", "application/pdf", pdf);
    }

    @Test
    void uploadSemCsrfERecusadoSemLerOuArmazenarArquivo() throws Exception {
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo", "termo.pdf", "application/pdf", pdf);

        mockMvc.perform(multipart("/solicitacoes/20/documentos")
                        .file(arquivo)
                        .with(user(aluno.getEmail()).roles("ALUNO")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(documentoService);
    }

    @Test
    void arquivoInvalidoMostraErroSemAlegarSucesso() throws Exception {
        prepararAluno();
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo", "falso.pdf", "application/pdf", "html".getBytes());
        when(documentoService.enviar(
                20L, aluno, "falso.pdf", "application/pdf", "html".getBytes()))
                .thenThrow(new IllegalArgumentException(
                        "O conteúdo enviado não corresponde a um arquivo PDF válido"));

        mockMvc.perform(multipart("/solicitacoes/20/documentos")
                        .file(arquivo)
                        .with(user(aluno.getEmail()).roles("ALUNO"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/solicitacoes/20/documentos"))
                .andExpect(flash().attribute("mensagemErro", containsString(
                        "não corresponde a um arquivo PDF válido")))
                .andExpect(flash().attributeCount(1));
    }

    @Test
    void tentativaDeUploadEmSolicitacaoAlheiaRetornaNaoEncontrado() throws Exception {
        prepararAluno();
        when(documentoService.enviar(99L, aluno, "termo.pdf", "application/pdf", pdf))
                .thenThrow(new AccessDeniedException("Solicitação alheia"));
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo", "termo.pdf", "application/pdf", pdf);

        mockMvc.perform(multipart("/solicitacoes/99/documentos")
                        .file(arquivo)
                        .with(user(aluno.getEmail()).roles("ALUNO"))
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void downloadAutorizadoForcaAnexoPdfPrivadoSemSniff() throws Exception {
        prepararAluno();
        when(documentoService.baixar(20L, 30L, aluno)).thenReturn(
                new DownloadDocumento(
                        "termo externo.pdf", pdf.length, new ByteArrayResource(pdf)));

        mockMvc.perform(get("/solicitacoes/20/documentos/30/baixar")
                        .with(user(aluno.getEmail()).roles("ALUNO")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(header().string("Content-Disposition", containsString("termo")))
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(content().bytes(pdf));
    }

    @Test
    void documentoAlheioRetornaNaoEncontradoSemCorpo() throws Exception {
        prepararAluno();
        when(documentoService.baixar(20L, 99L, aluno))
                .thenThrow(new AccessDeniedException("Documento de outra solicitação"));

        mockMvc.perform(get("/solicitacoes/20/documentos/99/baixar")
                        .with(user(aluno.getEmail()).roles("ALUNO")))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));
    }

    @Test
    void solicitacaoQueAvancouExibeHistoricoSemFormularioDeEnvio() throws Exception {
        reserva.setStatus(StatusReserva.PENDENTE_ANALISE);
        prepararAlunoEReserva();
        when(documentoService.listar(20L, aluno)).thenReturn(List.of());

        mockMvc.perform(get("/solicitacoes/20/documentos")
                        .with(user(aluno.getEmail()).roles("ALUNO")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(
                        "não são permitidos depois que a solicitação avança")))
                .andExpect(content().string(not(containsString("Enviar nova versão"))));

        verify(documentoService, never()).enviar(
                20L, aluno, "termo.pdf", "application/pdf", pdf);
    }

    @Test
    void documentoAguardandoConferenciaOcultaNovoUploadMesmoComStatusDocumental() throws Exception {
        prepararAlunoEReserva();
        when(documentoService.podeEnviar(20L, aluno)).thenReturn(false);
        when(documentoService.listar(20L, aluno))
                .thenReturn(List.of(documento(30L, 1, "termo.pdf")));

        mockMvc.perform(get("/solicitacoes/20/documentos")
                        .with(user(aluno.getEmail()).roles("ALUNO")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(
                        "só será liberado se a secretaria devolver")))
                .andExpect(content().string(not(containsString("Enviar nova versão"))));
    }

    @Test
    void r01TelaDeEnvioMostraOPrazoDoDiaAnterior() throws Exception {
        prepararAlunoEReserva();
        when(documentoService.listar(20L, aluno)).thenReturn(List.of());

        mockMvc.perform(get("/solicitacoes/20/documentos")
                        .with(user(aluno.getEmail()).roles("ALUNO")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Envie o PDF assinado até 20/10/2030.")))
                .andExpect(content().string(containsString("Enviar nova versão")));
    }

    @Test
    void r01NoDiaDaUtilizacaoMostraPrazoEncerradoSemFormulario() throws Exception {
        reserva.setDataUtilizacao(LocalDate.now(
                br.com.courttime.service.CalendarioDisponibilidadeService.FUSO_HORARIO));
        prepararAluno();
        when(reservaService.obterSolicitacaoDoUsuario(20L, aluno)).thenReturn(reserva);
        when(documentoService.podeEnviar(20L, aluno)).thenReturn(false);
        when(documentoService.listar(20L, aluno)).thenReturn(List.of());

        mockMvc.perform(get("/solicitacoes/20/documentos")
                        .with(user(aluno.getEmail()).roles("ALUNO")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Prazo de envio encerrado.")))
                .andExpect(content().string(containsString(
                        "O documento assinado deve ser entregue até o dia anterior ao uso")))
                .andExpect(content().string(not(containsString("só será liberado se a secretaria devolver"))))
                .andExpect(content().string(not(containsString("Enviar nova versão"))));
    }

    @Test
    void r01EnvioRecusadoNoDiaMostraOMotivo() throws Exception {
        prepararAluno();
        when(documentoService.enviar(20L, aluno, "termo.pdf", "application/pdf", pdf))
                .thenThrow(new IllegalStateException(DocumentoReservaService.MENSAGEM_PRAZO_ENCERRADO));

        mockMvc.perform(multipart("/solicitacoes/20/documentos")
                        .file(new MockMultipartFile("arquivo", "termo.pdf", "application/pdf", pdf))
                        .with(user(aluno.getEmail()).roles("ALUNO"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("mensagemErro",
                        "O documento assinado deve ser entregue até o dia anterior ao uso."));
    }

    private DocumentoReserva documento(Long id, int versao, String nome) {
        DocumentoReserva documento = new DocumentoReserva();
        documento.setId(id);
        documento.setReserva(reserva);
        documento.setVersao(versao);
        documento.setNomeOriginal(nome);
        documento.setChaveArmazenamento("123e4567-e89b-12d3-a456-426614174000.pdf");
        documento.setSha256("0".repeat(64));
        documento.setTamanhoBytes(pdf.length);
        documento.setEnviadoPor(aluno);
        ReflectionTestUtils.setField(documento, "enviadoEm", Instant.parse("2026-10-18T10:00:00Z"));
        return documento;
    }

    private void prepararAluno() {
        when(usuarioRepository.findByEmailIgnoreCase(aluno.getEmail()))
                .thenReturn(Optional.of(aluno));
    }

    private void prepararAlunoEReserva() {
        prepararAluno();
        when(reservaService.obterSolicitacaoDoUsuario(20L, aluno)).thenReturn(reserva);
        when(documentoService.podeEnviar(20L, aluno))
                .thenReturn(reserva.getStatus() == StatusReserva.PENDENTE_DOCUMENTACAO);
    }
}
