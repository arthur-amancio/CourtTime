package br.com.courttime.service;

import br.com.courttime.entity.DocumentoReserva;
import br.com.courttime.entity.Perfil;
import br.com.courttime.entity.Reserva;
import br.com.courttime.entity.StatusReserva;
import br.com.courttime.entity.Usuario;
import br.com.courttime.repository.DocumentoReservaRepository;
import br.com.courttime.repository.ParticipanteReservaRepository;
import br.com.courttime.repository.ReservaRepository;
import br.com.courttime.storage.DocumentoStorage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentoReservaServiceTest {

    @Mock
    private DocumentoReservaRepository documentoRepository;
    @Mock
    private ParticipanteReservaRepository participanteRepository;
    @Mock
    private ReservaRepository reservaRepository;
    @Mock
    private DocumentoStorage documentoStorage;

    private DocumentoReservaService service;
    private Usuario aluno;
    private Reserva reserva;
    private byte[] pdf;

    @BeforeEach
    void setUp() {
        service = new DocumentoReservaService(
                documentoRepository, participanteRepository, reservaRepository, documentoStorage);
        aluno = usuario(10L, Perfil.ALUNO, true);
        reserva = reserva(20L, aluno, StatusReserva.PENDENTE_DOCUMENTACAO);
        pdf = "%PDF-1.7\nconteudo sintetico".getBytes(StandardCharsets.US_ASCII);
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void enviaNovaVersaoComNomeSeguroHashEChaveOpaca() {
        when(reservaRepository.findByIdAndSolicitanteIdForUpdate(20L, 10L))
                .thenReturn(Optional.of(reserva));
        when(participanteRepository.countByReserva(reserva)).thenReturn(10L);
        when(documentoRepository.obterMaiorVersao(reserva)).thenReturn(0);
        when(documentoRepository.saveAndFlush(any(DocumentoReserva.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DocumentoReserva documento = service.enviar(
                20L,
                aluno,
                "../../termo assinado.pdf",
                "application/pdf",
                pdf);

        assertEquals(1, documento.getVersao());
        assertEquals(reserva.getRevisaoParticipantes(), documento.getRevisaoParticipantes());
        assertEquals("termo assinado.pdf", documento.getNomeOriginal());
        assertEquals(pdf.length, documento.getTamanhoBytes());
        assertEquals(64, documento.getSha256().length());
        assertSame(aluno, documento.getEnviadoPor());
        assertSame(reserva, documento.getReserva());
        assertTrue(documento.getChaveArmazenamento().matches("[a-f0-9-]{36}\\.pdf"));
        assertNotEquals(documento.getNomeOriginal(), documento.getChaveArmazenamento());
        verify(documentoStorage).armazenar(documento.getChaveArmazenamento(), pdf);

        concluirTransacao(TransactionSynchronization.STATUS_COMMITTED);
        verify(documentoStorage, never()).remover(documento.getChaveArmazenamento());
    }

    @Test
    void rollbackRemoveSomenteArquivoDaNovaVersao() {
        when(reservaRepository.findByIdAndSolicitanteIdForUpdate(20L, 10L))
                .thenReturn(Optional.of(reserva));
        when(participanteRepository.countByReserva(reserva)).thenReturn(10L);
        when(documentoRepository.obterMaiorVersao(reserva)).thenReturn(0);
        when(documentoRepository.saveAndFlush(any(DocumentoReserva.class)))
                .thenThrow(new IllegalStateException("falha no banco"));

        assertThrows(IllegalStateException.class, () -> service.enviar(
                20L, aluno, "termo.pdf", "application/pdf", pdf));

        ArgumentCaptor<String> chave = ArgumentCaptor.forClass(String.class);
        verify(documentoStorage).armazenar(chave.capture(), any());
        concluirTransacao(TransactionSynchronization.STATUS_ROLLED_BACK);
        verify(documentoStorage).remover(chave.getValue());
    }

    @Test
    void recusaConteudoDisfarcadoVazioGrandeOuComExtensaoInvalidaAntesDoBanco() {
        assertThrows(IllegalArgumentException.class, () -> service.enviar(
                20L, aluno, "termo.pdf.exe", "application/pdf", pdf));
        assertThrows(IllegalArgumentException.class, () -> service.enviar(
                20L, aluno, "termo.pdf", "application/pdf", new byte[0]));
        assertThrows(IllegalArgumentException.class, () -> service.enviar(
                20L,
                aluno,
                "termo.pdf",
                "application/pdf",
                "<html>não é pdf</html>".getBytes(StandardCharsets.UTF_8)));
        byte[] grande = new byte[(int) DocumentoReservaService.TAMANHO_MAXIMO_BYTES + 1];
        System.arraycopy("%PDF-".getBytes(StandardCharsets.US_ASCII), 0, grande, 0, 5);
        assertThrows(IllegalArgumentException.class, () -> service.enviar(
                20L, aluno, "termo.pdf", "application/pdf", grande));

        verifyNoInteractions(reservaRepository, documentoRepository, documentoStorage);
    }

    @Test
    void recusaTipoDeclaradoIncompativelMasAceitaOctetStreamComAssinaturaPdf() {
        assertThrows(IllegalArgumentException.class, () -> service.enviar(
                20L, aluno, "termo.pdf", "text/html", pdf));

        when(reservaRepository.findByIdAndSolicitanteIdForUpdate(20L, 10L))
                .thenReturn(Optional.of(reserva));
        when(participanteRepository.countByReserva(reserva)).thenReturn(10L);
        when(documentoRepository.obterMaiorVersao(reserva)).thenReturn(0);
        when(documentoRepository.saveAndFlush(any(DocumentoReserva.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        DocumentoReserva documento = service.enviar(
                20L, aluno, "termo.pdf", "application/octet-stream", pdf);

        assertEquals(1, documento.getVersao());
    }

    @Test
    void recusaSolicitacaoAlheiaOuQueJaAvancouAntesDeArmazenar() {
        when(reservaRepository.findByIdAndSolicitanteIdForUpdate(20L, 10L))
                .thenReturn(Optional.empty());
        assertThrows(AccessDeniedException.class, () -> service.enviar(
                20L, aluno, "termo.pdf", "application/pdf", pdf));

        reserva.setStatus(StatusReserva.PENDENTE_ANALISE);
        when(reservaRepository.findByIdAndSolicitanteIdForUpdate(20L, 10L))
                .thenReturn(Optional.of(reserva));
        assertThrows(IllegalStateException.class, () -> service.enviar(
                20L, aluno, "termo.pdf", "application/pdf", pdf));

        verifyNoInteractions(documentoRepository, documentoStorage);
    }

    @Test
    void rascunhoNaoPodeEnviarPdfMesmoSeClienteForjarDezParticipantes() {
        reserva.setRascunho(true);
        when(reservaRepository.findByIdAndSolicitanteIdForUpdate(20L, 10L))
                .thenReturn(Optional.of(reserva));

        assertThrows(IllegalStateException.class, () -> service.enviar(
                20L, aluno, "termo.pdf", "application/pdf", pdf));
        verifyNoInteractions(documentoStorage);
    }

    @Test
    void envioExigeDezParticipantesDeQualquerCategoriaAntesDeArmazenar() {
        when(reservaRepository.findByIdAndSolicitanteIdForUpdate(20L, 10L))
                .thenReturn(Optional.of(reserva));
        when(participanteRepository.countByReserva(reserva)).thenReturn(9L);

        IllegalStateException erro = assertThrows(IllegalStateException.class,
                () -> service.enviar(20L, aluno, "termo.pdf", "application/pdf", pdf));

        assertTrue(erro.getMessage().contains("10 participantes"));
        verifyNoInteractions(documentoRepository, documentoStorage);
    }

    @Test
    void r01RecusaEnvioNoDiaDaUtilizacaoEDepoisDela() {
        when(reservaRepository.findByIdAndSolicitanteIdForUpdate(20L, 10L))
                .thenReturn(Optional.of(reserva));
        LocalDate hoje = LocalDate.now(CalendarioDisponibilidadeService.FUSO_HORARIO);

        for (LocalDate data : List.of(hoje, hoje.minusDays(1))) {
            reserva.setDataUtilizacao(data);
            IllegalStateException erro = assertThrows(IllegalStateException.class,
                    () -> service.enviar(20L, aluno, "termo.pdf", "application/pdf", pdf));
            assertEquals(DocumentoReservaService.MENSAGEM_PRAZO_ENCERRADO, erro.getMessage());
            assertTrue(DocumentoReservaService.prazoDeEnvioEncerrado(reserva));
        }
        verifyNoInteractions(documentoRepository, documentoStorage);
    }

    @Test
    void r01AceitaEnvioNoDiaAnteriorAoUso() {
        LocalDate amanha = LocalDate.now(CalendarioDisponibilidadeService.FUSO_HORARIO).plusDays(1);
        reserva.setDataUtilizacao(amanha);
        when(reservaRepository.findByIdAndSolicitanteIdForUpdate(20L, 10L))
                .thenReturn(Optional.of(reserva));
        when(participanteRepository.countByReserva(reserva)).thenReturn(10L);
        when(documentoRepository.obterMaiorVersao(reserva)).thenReturn(0);
        when(documentoRepository.saveAndFlush(any(DocumentoReserva.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DocumentoReserva documento = service.enviar(
                20L, aluno, "termo.pdf", "application/pdf", pdf);

        assertEquals(1, documento.getVersao());
        assertEquals(amanha.minusDays(1), DocumentoReservaService.prazoFinalDeEnvio(reserva));
        assertFalse(DocumentoReservaService.prazoDeEnvioEncerrado(reserva));
    }

    @Test
    void r01PodeEnviarFicaFalsoNoDiaDaUtilizacaoMesmoSemDocumento() {
        reserva.setDataUtilizacao(LocalDate.now(CalendarioDisponibilidadeService.FUSO_HORARIO));
        when(reservaRepository.findByIdAndSolicitante(20L, aluno))
                .thenReturn(Optional.of(reserva));

        assertFalse(service.podeEnviar(20L, aluno));
        verifyNoInteractions(documentoRepository);
    }

    @Test
    void listaEBaixaSomenteDocumentoAninhadoNaSolicitacaoDoAluno() {
        DocumentoReserva documento = documento(30L, reserva, "termo.pdf", "chave.pdf", pdf.length);
        ByteArrayResource recurso = new ByteArrayResource(pdf);
        when(reservaRepository.findByIdAndSolicitante(20L, aluno))
                .thenReturn(Optional.of(reserva));
        when(documentoRepository.findByReservaOrderByVersaoDesc(reserva))
                .thenReturn(List.of(documento));
        when(documentoRepository.findByIdAndReserva(30L, reserva))
                .thenReturn(Optional.of(documento));
        when(documentoStorage.carregar("chave.pdf")).thenReturn(recurso);

        assertEquals(List.of(documento), service.listar(20L, aluno));
        DownloadDocumento download = service.baixar(20L, 30L, aluno);
        assertEquals("termo.pdf", download.nomeArquivo());
        assertEquals(pdf.length, download.tamanhoBytes());
        assertSame(recurso, download.conteudo());
    }

    @Test
    void idDeDocumentoDeOutraSolicitacaoNaoChegaAoStorage() {
        when(reservaRepository.findByIdAndSolicitante(20L, aluno))
                .thenReturn(Optional.of(reserva));
        when(documentoRepository.findByIdAndReserva(99L, reserva))
                .thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class,
                () -> service.baixar(20L, 99L, aluno));

        verifyNoInteractions(documentoStorage);
    }

    @Test
    void recusaUsuarioInativoOuComOutroPerfil() {
        assertThrows(AccessDeniedException.class,
                () -> service.listar(20L, usuario(11L, Perfil.ALUNO, false)));
        assertThrows(AccessDeniedException.class,
                () -> service.listar(20L, usuario(12L, Perfil.SECRETARIA, true)));
        verifyNoInteractions(reservaRepository, documentoRepository, documentoStorage);
    }

    private void concluirTransacao(int status) {
        List<TransactionSynchronization> sincronizacoes =
                TransactionSynchronizationManager.getSynchronizations();
        TransactionSynchronizationManager.clearSynchronization();
        sincronizacoes.forEach(sincronizacao -> sincronizacao.afterCompletion(status));
    }

    private DocumentoReserva documento(
            Long id, Reserva reserva, String nome, String chave, long tamanho) {
        DocumentoReserva documento = new DocumentoReserva();
        documento.setId(id);
        documento.setReserva(reserva);
        documento.setVersao(1);
        documento.setNomeOriginal(nome);
        documento.setChaveArmazenamento(chave);
        documento.setSha256("0".repeat(64));
        documento.setTamanhoBytes(tamanho);
        documento.setEnviadoPor(aluno);
        return documento;
    }

    private Reserva reserva(Long id, Usuario solicitante, StatusReserva status) {
        Reserva reserva = new Reserva();
        reserva.setId(id);
        reserva.setSolicitante(solicitante);
        reserva.setStatus(status);
        return reserva;
    }

    private Usuario usuario(Long id, Perfil perfil, boolean ativo) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNome("Usuário");
        usuario.setEmail("usuario" + id + "@teste.local");
        usuario.setSenha("hash");
        usuario.setPerfil(perfil);
        usuario.setAtivo(ativo);
        return usuario;
    }
}
