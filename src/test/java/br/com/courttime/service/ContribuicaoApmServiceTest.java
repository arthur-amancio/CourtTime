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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
    private ParticipanteReservaService participanteService;
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
                participanteService,
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
        when(participanteService.contarParticipantes(reserva)).thenReturn(11L);

        List<ContribuicaoPendente> pendentes = service.listarPendentes(secretaria);
        assertEquals(List.of(new ContribuicaoPendente(reserva, 11L)), pendentes);
        assertEquals(new BigDecimal("22.00"), pendentes.get(0).valorDevido());
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
    void r03RegistraValorCalculadoVinculadoAConferenciaExataEAvancaParaAnalise() {
        prepararRegistro();
        // 10 alunos e 1 participante Outro: 11 × R$ 2,00.
        when(participanteService.contarParticipantes(reserva)).thenReturn(11L);
        when(contribuicaoRepository.saveAndFlush(any(ContribuicaoApm.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DetalheContribuicaoApm detalhe = service.registrar(
                20L, secretaria, "  Recebido em espécie.  ");

        ContribuicaoApm contribuicao = detalhe.contribuicao();
        assertSame(reserva, contribuicao.getReserva());
        assertSame(conferencia, contribuicao.getConferenciaDocumento());
        assertSame(secretaria, contribuicao.getRegistradaPor());
        assertEquals(new BigDecimal("22.00"), contribuicao.getValorDevido());
        assertEquals(new BigDecimal("22.00"), contribuicao.getValor());
        assertEquals(11L, detalhe.participantesCobrados());
        assertEquals("Recebido em espécie.", contribuicao.getObservacoes());
        assertEquals(StatusReserva.PENDENTE_ANALISE, reserva.getStatus());
        assertEquals(List.of(), detalhe.ajustes());
        verify(reservaRepository).save(reserva);
    }

    @Test
    void exigeUltimaVersaoValidadaEStatusPendente() {
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(secretaria));
        when(reservaRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(reserva));
        when(documentoRepository.obterVersaoMaisRecenteComConferencia(reserva))
                .thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class,
                () -> service.registrar(20L, secretaria, null));

        conferencia.setResultado(ResultadoConferenciaDocumento.DEVOLVIDO_CORRECAO);
        when(documentoRepository.obterVersaoMaisRecenteComConferencia(reserva))
                .thenReturn(Optional.of(documento));
        assertThrows(IllegalStateException.class,
                () -> service.registrar(20L, secretaria, null));

        reserva.setStatus(StatusReserva.PENDENTE_ANALISE);
        assertThrows(IllegalStateException.class,
                () -> service.registrar(20L, secretaria, null));
        verify(contribuicaoRepository, never()).saveAndFlush(any());
    }

    @Test
    void impedeSegundaContribuicaoParaMesmaReserva() {
        prepararRegistro();
        when(contribuicaoRepository.existsByReserva(reserva)).thenReturn(true);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> service.registrar(20L, secretaria, null));

        assertEquals("Esta solicitação já possui uma contribuição registrada",
                exception.getMessage());
        verify(contribuicaoRepository, never()).saveAndFlush(any());
    }

    @Test
    void recusaParticipantesIncompativeisComDocumento() {
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(secretaria));
        when(reservaRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(reserva));
        when(documentoRepository.obterVersaoMaisRecenteComConferencia(reserva))
                .thenReturn(Optional.of(documento));
        org.mockito.Mockito.doThrow(new IllegalStateException("participantes inválidos"))
                .when(participanteService).validarDocumentoAtual(reserva, documento);

        assertThrows(IllegalStateException.class,
                () -> service.registrar(20L, secretaria, null));
        verify(contribuicaoRepository, never()).saveAndFlush(any());
    }

    @Test
    void validaObservacoesAntesDeBloquearReserva() {
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(secretaria));

        assertThrows(IllegalArgumentException.class,
                () -> service.registrar(20L, secretaria, "x".repeat(501)));
        verifyNoInteractions(reservaRepository, documentoRepository);
    }

    @ParameterizedTest
    @CsvSource({
            // participantes, crédito, valor cobrado, crédito aplicado
            "15, 22.00, 8.00, 22.00",   // crédito menor: paga a diferença
            "11, 22.00, 0.00, 22.00",   // crédito igual: nenhuma cobrança nova
            "10, 22.00, 0.00, 20.00"    // crédito maior: sobra R$ 2,00 (D2)
    })
    void r04RemarcacaoUsaOCreditoDaCanceladaSemFicarNegativo(
            long participantes, String credito, String cobrado, String aplicado) {
        Reserva cancelada = new Reserva();
        cancelada.setId(5L);
        cancelada.setStatus(StatusReserva.CANCELADA);
        reserva.setRemarcacaoDe(cancelada);
        ContribuicaoApm origem = new ContribuicaoApm();
        origem.setId(41L);
        origem.setReserva(cancelada);
        origem.setValor(new BigDecimal(credito));
        prepararRegistro();
        when(participanteService.contarParticipantes(reserva)).thenReturn(participantes);
        when(contribuicaoRepository.obterPorReservaId(5L)).thenReturn(Optional.of(origem));
        when(contribuicaoRepository.saveAndFlush(any(ContribuicaoApm.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DetalheContribuicaoApm detalhe = service.registrar(20L, secretaria, null);

        ContribuicaoApm nova = detalhe.contribuicao();
        assertEquals(ContribuicaoApmService.calcularValorDevido(participantes), nova.getValorDevido());
        assertEquals(new BigDecimal(cobrado), nova.getValor());
        assertEquals(new BigDecimal(aplicado), nova.getCreditoAplicado());
        assertSame(origem, nova.getContribuicaoOrigem());
        assertEquals(5L, detalhe.reservaOrigemId());
        assertEquals(new BigDecimal(credito).subtract(new BigDecimal(aplicado)), detalhe.saldoExcedente());
        assertEquals(StatusReserva.PENDENTE_ANALISE, reserva.getStatus(), "R04.5: o crédito não aprova");
    }

    @Test
    void r04CreditoContaOQueACanceladaJaTinhaAproveitado() {
        ContribuicaoApm origem = new ContribuicaoApm();
        origem.setValor(new BigDecimal("8.00"));
        origem.setCreditoAplicado(new BigDecimal("22.00"));

        assertEquals(new BigDecimal("30.00"), origem.getValorEfetivamentePago());
    }

    @Test
    void r03CalculaDoisReaisPorParticipante() {
        assertEquals(new BigDecimal("20.00"), ContribuicaoApmService.calcularValorDevido(10));
        assertEquals(new BigDecimal("22.00"), ContribuicaoApmService.calcularValorDevido(11));
        assertEquals(new BigDecimal("62.00"), ContribuicaoApmService.calcularValorDevido(31));
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
