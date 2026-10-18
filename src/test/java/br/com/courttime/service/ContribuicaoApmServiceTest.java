package br.com.courttime.service;

import br.com.courttime.entity.AjusteContribuicaoApm;
import br.com.courttime.entity.ConferenciaDocumento;
import br.com.courttime.entity.ContribuicaoApm;
import br.com.courttime.entity.DocumentoReserva;
import br.com.courttime.entity.Perfil;
import br.com.courttime.entity.Reserva;
import br.com.courttime.entity.ResultadoConferenciaDocumento;
import br.com.courttime.entity.StatusReserva;
import br.com.courttime.entity.Usuario;
import br.com.courttime.repository.AjusteContribuicaoApmRepository;
import br.com.courttime.repository.ContribuicaoApmRepository;
import br.com.courttime.repository.DocumentoReservaRepository;
import br.com.courttime.repository.ReservaRepository;
import br.com.courttime.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContribuicaoApmServiceTest {

    @Mock
    private ContribuicaoApmRepository contribuicaoRepository;
    @Mock
    private AjusteContribuicaoApmRepository ajusteRepository;
    @Mock
    private DocumentoReservaRepository documentoRepository;
    @Mock
    private ReservaRepository reservaRepository;
    @Mock
    private UsuarioRepository usuarioRepository;

    private ContribuicaoApmService service;
    private Usuario secretaria;
    private Reserva reserva;
    private DocumentoReserva documento;
    private ConferenciaDocumento conferencia;

    @BeforeEach
    void setUp() {
        service = new ContribuicaoApmService(
                contribuicaoRepository,
                ajusteRepository,
                documentoRepository,
                reservaRepository,
                usuarioRepository);
        secretaria = usuario(10L, Perfil.SECRETARIA, true);
        reserva = new Reserva();
        reserva.setId(20L);
        reserva.setSolicitante(usuario(11L, Perfil.ALUNO, true));
        reserva.setStatus(StatusReserva.PENDENTE_DOCUMENTACAO);
        documento = new DocumentoReserva();
        documento.setId(30L);
        documento.setReserva(reserva);
        documento.setVersao(1);
        conferencia = new ConferenciaDocumento();
        conferencia.setDocumento(documento);
        conferencia.setResultado(ResultadoConferenciaDocumento.VALIDADO);
        conferencia.setConferidoPor(secretaria);
        ReflectionTestUtils.setField(documento, "conferencia", conferencia);
    }

    @Test
    void listaPendenciasRegistradasEDetalheSomenteParaSecretariaAtual() {
        ContribuicaoApm contribuicao = contribuicao("20.00", "Recebido presencialmente");
        AjusteContribuicaoApm ajuste = new AjusteContribuicaoApm();
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(secretaria));
        when(contribuicaoRepository.listarPendentes(
                StatusReserva.PENDENTE_DOCUMENTACAO,
                ResultadoConferenciaDocumento.VALIDADO)).thenReturn(List.of(reserva));
        when(contribuicaoRepository.listarRegistradas()).thenReturn(List.of(contribuicao));
        when(contribuicaoRepository.obterPorReservaId(20L))
                .thenReturn(Optional.of(contribuicao));
        when(ajusteRepository.listarHistorico(contribuicao)).thenReturn(List.of(ajuste));

        assertEquals(List.of(reserva), service.listarPendentes(secretaria));
        assertEquals(List.of(contribuicao), service.listarRegistradas(secretaria));
        DetalheContribuicaoApm detalhe = service.obterPorReserva(20L, secretaria);
        assertSame(contribuicao, detalhe.contribuicao());
        assertEquals(List.of(ajuste), detalhe.ajustes());
    }

    @Test
    void recusaPerfilInativoOuDiferenteMesmoSeObjetoRecebidoParecerValido() {
        Usuario inativa = usuario(10L, Perfil.SECRETARIA, false);
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(inativa));
        assertThrows(AccessDeniedException.class,
                () -> service.listarPendentes(secretaria));

        Usuario aluno = usuario(12L, Perfil.ALUNO, true);
        when(usuarioRepository.findById(12L)).thenReturn(Optional.of(aluno));
        assertThrows(AccessDeniedException.class,
                () -> service.listarRegistradas(aluno));
        verifyNoInteractions(contribuicaoRepository);
    }

    @Test
    void registraValorRealVinculadoAConferenciaExataSemAlterarStatus() {
        prepararRegistro();
        when(contribuicaoRepository.saveAndFlush(any(ContribuicaoApm.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DetalheContribuicaoApm detalhe = service.registrar(
                20L, secretaria, " 20,50 ", "  Recebido em espécie.  ");

        ContribuicaoApm contribuicao = detalhe.contribuicao();
        assertSame(reserva, contribuicao.getReserva());
        assertSame(conferencia, contribuicao.getConferenciaDocumento());
        assertSame(secretaria, contribuicao.getRegistradaPor());
        assertEquals(new BigDecimal("20.50"), contribuicao.getValor());
        assertEquals("Recebido em espécie.", contribuicao.getObservacoes());
        assertEquals(StatusReserva.PENDENTE_DOCUMENTACAO, reserva.getStatus());
        assertEquals(List.of(), detalhe.ajustes());
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void exigeUltimaVersaoValidadaEStatusPendente() {
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(secretaria));
        when(reservaRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(reserva));
        when(documentoRepository.obterVersaoMaisRecenteComConferencia(reserva))
                .thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class,
                () -> service.registrar(20L, secretaria, "20.00", null));

        conferencia.setResultado(ResultadoConferenciaDocumento.DEVOLVIDO_CORRECAO);
        when(documentoRepository.obterVersaoMaisRecenteComConferencia(reserva))
                .thenReturn(Optional.of(documento));
        assertThrows(IllegalStateException.class,
                () -> service.registrar(20L, secretaria, "20.00", null));

        reserva.setStatus(StatusReserva.PENDENTE_ANALISE);
        assertThrows(IllegalStateException.class,
                () -> service.registrar(20L, secretaria, "20.00", null));
        verify(contribuicaoRepository, never()).saveAndFlush(any());
    }

    @Test
    void impedeSegundaContribuicaoParaMesmaReserva() {
        prepararRegistro();
        when(contribuicaoRepository.existsByReserva(reserva)).thenReturn(true);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> service.registrar(20L, secretaria, "20.00", null));

        assertEquals("Esta solicitação já possui uma contribuição registrada",
                exception.getMessage());
        verify(contribuicaoRepository, never()).saveAndFlush(any());
    }

    @Test
    void validaValorEObservacoesAntesDeBloquearReserva() {
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(secretaria));

        for (String valor : List.of("", "0", "-1", "2.001", "abc", "1.2.3")) {
            assertThrows(IllegalArgumentException.class,
                    () -> service.registrar(20L, secretaria, valor, null));
        }
        assertThrows(IllegalArgumentException.class,
                () -> service.registrar(20L, secretaria, "20.00", "x".repeat(501)));
        verifyNoInteractions(reservaRepository, documentoRepository);
    }

    @Test
    void ajustePreservaAntesEDepoisComMotivoOperadorEAtualizaEstadoCorrente() {
        ContribuicaoApm contribuicao = contribuicao("20.00", "Observação antiga");
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(secretaria));
        when(contribuicaoRepository.findByIdForUpdate(40L))
                .thenReturn(Optional.of(contribuicao));
        when(contribuicaoRepository.saveAndFlush(contribuicao)).thenReturn(contribuicao);
        when(ajusteRepository.listarHistorico(contribuicao))
                .thenAnswer(invocation -> List.of());

        service.ajustar(
                40L, secretaria, "25.50", "  Observação nova  ", "  Correção do valor.  ");

        ArgumentCaptor<AjusteContribuicaoApm> captor = ArgumentCaptor.forClass(
                AjusteContribuicaoApm.class);
        verify(ajusteRepository).save(captor.capture());
        AjusteContribuicaoApm ajuste = captor.getValue();
        assertSame(contribuicao, ajuste.getContribuicao());
        assertEquals(new BigDecimal("20.00"), ajuste.getValorAnterior());
        assertEquals(new BigDecimal("25.50"), ajuste.getValorNovo());
        assertEquals("Observação antiga", ajuste.getObservacoesAnteriores());
        assertEquals("Observação nova", ajuste.getObservacoesNovas());
        assertEquals("Correção do valor.", ajuste.getMotivo());
        assertSame(secretaria, ajuste.getAjustadaPor());
        assertEquals(new BigDecimal("25.50"), contribuicao.getValor());
        assertEquals("Observação nova", contribuicao.getObservacoes());
    }

    @Test
    void ajusteExigeMotivoEMudancaEfetiva() {
        ContribuicaoApm contribuicao = contribuicao("20.00", null);
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(secretaria));

        assertThrows(IllegalArgumentException.class,
                () -> service.ajustar(40L, secretaria, "21.00", null, "  "));
        verifyNoInteractions(contribuicaoRepository);

        when(contribuicaoRepository.findByIdForUpdate(40L))
                .thenReturn(Optional.of(contribuicao));
        assertThrows(IllegalArgumentException.class,
                () -> service.ajustar(40L, secretaria, "20.00", "", "Conferência"));
        verify(ajusteRepository, never()).save(any());
        verify(contribuicaoRepository, never()).saveAndFlush(any());
    }

    private void prepararRegistro() {
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(secretaria));
        when(reservaRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(reserva));
        when(documentoRepository.obterVersaoMaisRecenteComConferencia(reserva))
                .thenReturn(Optional.of(documento));
        when(contribuicaoRepository.existsByReserva(reserva)).thenReturn(false);
    }

    private ContribuicaoApm contribuicao(String valor, String observacoes) {
        ContribuicaoApm contribuicao = new ContribuicaoApm();
        contribuicao.setId(40L);
        contribuicao.setReserva(reserva);
        contribuicao.setConferenciaDocumento(conferencia);
        contribuicao.setValor(new BigDecimal(valor));
        contribuicao.setObservacoes(observacoes);
        contribuicao.setRegistradaPor(secretaria);
        return contribuicao;
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
