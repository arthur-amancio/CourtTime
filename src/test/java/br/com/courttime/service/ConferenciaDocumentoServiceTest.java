package br.com.courttime.service;

import br.com.courttime.entity.ConferenciaDocumento;
import br.com.courttime.entity.DocumentoReserva;
import br.com.courttime.entity.Perfil;
import br.com.courttime.entity.Reserva;
import br.com.courttime.entity.ResultadoConferenciaDocumento;
import br.com.courttime.entity.StatusReserva;
import br.com.courttime.entity.Usuario;
import br.com.courttime.repository.ConferenciaDocumentoRepository;
import br.com.courttime.repository.DocumentoReservaRepository;
import br.com.courttime.repository.ReservaRepository;
import br.com.courttime.storage.DocumentoStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConferenciaDocumentoServiceTest {

    @Mock
    private DocumentoReservaRepository documentoRepository;
    @Mock
    private ConferenciaDocumentoRepository conferenciaRepository;
    @Mock
    private ReservaRepository reservaRepository;
    @Mock
    private ParticipanteReservaService participanteService;
    @Mock
    private DocumentoStorage documentoStorage;

    private ConferenciaDocumentoService service;
    private Usuario secretaria;
    private Reserva reserva;
    private DocumentoReserva documento;

    @BeforeEach
    void setUp() {
        service = new ConferenciaDocumentoService(
                documentoRepository,
                conferenciaRepository,
                reservaRepository,
                participanteService,
                documentoStorage);
        secretaria = usuario(10L, Perfil.SECRETARIA, true);
        Usuario aluno = usuario(11L, Perfil.ALUNO, true);
        reserva = new Reserva();
        reserva.setId(20L);
        reserva.setSolicitante(aluno);
        reserva.setStatus(StatusReserva.PENDENTE_DOCUMENTACAO);
        documento = documento(30L, reserva, 2);
    }

    @Test
    void listaFilaEHistoricoSomenteParaSecretariaAtiva() {
        ConferenciaDocumento conferencia = new ConferenciaDocumento();
        when(documentoRepository.listarVersoesMaisRecentesPendentes(
                StatusReserva.PENDENTE_DOCUMENTACAO)).thenReturn(List.of(documento));
        when(conferenciaRepository.listarHistorico()).thenReturn(List.of(conferencia));

        assertEquals(List.of(documento), service.listarPendentes(secretaria));
        assertEquals(List.of(conferencia), service.listarHistorico(secretaria));

        assertThrows(AccessDeniedException.class,
                () -> service.listarPendentes(usuario(12L, Perfil.ALUNO, true)));
        assertThrows(AccessDeniedException.class,
                () -> service.listarPendentes(usuario(13L, Perfil.SECRETARIA, false)));
    }

    @Test
    void validaVersaoMaisRecenteSemAprovarOuAlterarReserva() {
        prepararRegistro();
        when(conferenciaRepository.saveAndFlush(any(ConferenciaDocumento.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ConferenciaDocumento conferencia = service.validar(30L, secretaria);

        assertSame(documento, conferencia.getDocumento());
        assertSame(secretaria, conferencia.getConferidoPor());
        assertEquals(ResultadoConferenciaDocumento.VALIDADO, conferencia.getResultado());
        assertNull(conferencia.getJustificativa());
        assertEquals(StatusReserva.PENDENTE_DOCUMENTACAO, reserva.getStatus());
        verify(reservaRepository, never()).save(any());
        verifyNoInteractions(documentoStorage);
    }

    @Test
    void permiteAcaoSomenteNaVersaoAtualPendenteESemDecisao() {
        when(documentoRepository.obterMaiorVersao(reserva)).thenReturn(2);

        assertTrue(service.podeConferir(documento, secretaria));

        when(documentoRepository.obterMaiorVersao(reserva)).thenReturn(3);
        assertFalse(service.podeConferir(documento, secretaria));

        reserva.setStatus(StatusReserva.PENDENTE_ANALISE);
        assertFalse(service.podeConferir(documento, secretaria));
    }

    @Test
    void devolucaoExigeJustificativaENormalizaTexto() {
        assertThrows(IllegalArgumentException.class,
                () -> service.devolver(30L, secretaria, "  "));
        assertThrows(IllegalArgumentException.class,
                () -> service.devolver(30L, secretaria, "a".repeat(501)));
        verifyNoInteractions(documentoRepository, conferenciaRepository, reservaRepository);

        prepararRegistro();
        when(conferenciaRepository.saveAndFlush(any(ConferenciaDocumento.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        ConferenciaDocumento conferencia = service.devolver(
                30L, secretaria, "  Falta a página final.  ");

        assertEquals(ResultadoConferenciaDocumento.DEVOLVIDO_CORRECAO,
                conferencia.getResultado());
        assertEquals("Falta a página final.", conferencia.getJustificativa());
    }

    @Test
    void validacaoExigeParticipantesIntegresMasDevolucaoContinuaPermitida() {
        prepararRegistro();
        org.mockito.Mockito.doThrow(new IllegalStateException("participantes inválidos"))
                .when(participanteService).validarDocumentoAtual(reserva, documento);

        assertThrows(IllegalStateException.class,
                () -> service.validar(30L, secretaria));

        when(conferenciaRepository.saveAndFlush(any(ConferenciaDocumento.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        ConferenciaDocumento devolvida = service.devolver(
                30L, secretaria, "Lista de alunos incompleta");
        assertEquals(ResultadoConferenciaDocumento.DEVOLVIDO_CORRECAO,
                devolvida.getResultado());
    }

    @Test
    void recusaVersaoAntigaDecisaoDuplicadaEStatusIncompativel() {
        when(documentoRepository.obterReservaId(30L)).thenReturn(Optional.of(20L));
        when(documentoRepository.obterComSolicitacao(30L)).thenReturn(Optional.of(documento));
        when(reservaRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(reserva));
        when(documentoRepository.obterMaiorVersao(reserva)).thenReturn(3);
        assertThrows(IllegalStateException.class, () -> service.validar(30L, secretaria));

        when(documentoRepository.obterMaiorVersao(reserva)).thenReturn(2);
        when(conferenciaRepository.existsByDocumento(documento)).thenReturn(true);
        assertThrows(IllegalStateException.class, () -> service.validar(30L, secretaria));

        reserva.setStatus(StatusReserva.PENDENTE_ANALISE);
        assertThrows(IllegalStateException.class, () -> service.validar(30L, secretaria));
        verify(conferenciaRepository, never()).saveAndFlush(any());
    }

    @Test
    void downloadDaSecretariaUsaChaveInternaSemAlterarArquivo() {
        byte[] pdf = "%PDF-1.7".getBytes();
        ByteArrayResource recurso = new ByteArrayResource(pdf);
        when(documentoRepository.obterComSolicitacao(30L)).thenReturn(Optional.of(documento));
        when(documentoStorage.carregar(documento.getChaveArmazenamento()))
                .thenReturn(recurso);

        DownloadDocumento download = service.baixar(30L, secretaria);

        assertEquals("termo.pdf", download.nomeArquivo());
        assertSame(recurso, download.conteudo());
        verify(documentoStorage, never()).armazenar(any(), any());
        verify(documentoStorage, never()).remover(any());
    }

    private void prepararRegistro() {
        when(documentoRepository.obterReservaId(30L)).thenReturn(Optional.of(20L));
        when(documentoRepository.obterComSolicitacao(30L)).thenReturn(Optional.of(documento));
        when(reservaRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(reserva));
        when(documentoRepository.obterMaiorVersao(reserva)).thenReturn(2);
        when(conferenciaRepository.existsByDocumento(documento)).thenReturn(false);
    }

    private DocumentoReserva documento(Long id, Reserva reserva, int versao) {
        DocumentoReserva documento = new DocumentoReserva();
        documento.setId(id);
        documento.setReserva(reserva);
        documento.setVersao(versao);
        documento.setNomeOriginal("termo.pdf");
        documento.setChaveArmazenamento("123e4567-e89b-12d3-a456-426614174000.pdf");
        documento.setSha256("0".repeat(64));
        documento.setTamanhoBytes(8);
        documento.setEnviadoPor(reserva.getSolicitante());
        return documento;
    }

    private Usuario usuario(Long id, Perfil perfil, boolean ativo) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNome("Usuário " + id);
        usuario.setEmail("usuario" + id + "@teste.local");
        usuario.setSenha("hash");
        usuario.setPerfil(perfil);
        usuario.setAtivo(ativo);
        return usuario;
    }
}
