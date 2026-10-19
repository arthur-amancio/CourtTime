package br.com.courttime.service;

import br.com.courttime.entity.ConferenciaDocumento;
import br.com.courttime.entity.ContribuicaoApm;
import br.com.courttime.entity.DocumentoReserva;
import br.com.courttime.entity.Reserva;
import br.com.courttime.entity.ResultadoConferenciaDocumento;
import br.com.courttime.entity.StatusReserva;
import br.com.courttime.repository.ContribuicaoApmRepository;
import br.com.courttime.repository.DocumentoReservaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SituacaoSolicitacaoServiceTest {

    @Mock
    private DocumentoReservaRepository documentoRepository;
    @Mock
    private ContribuicaoApmRepository contribuicaoRepository;

    private SituacaoSolicitacaoService service;
    private Reserva reserva;

    @BeforeEach
    void setUp() {
        service = new SituacaoSolicitacaoService(
                documentoRepository, contribuicaoRepository);
        reserva = new Reserva();
        reserva.setId(10L);
        reserva.setStatus(StatusReserva.PENDENTE_DOCUMENTACAO);
    }

    @Test
    void apresentaPendenciaQuandoAindaNaoHaPdf() {
        when(documentoRepository.obterVersaoMaisRecenteComConferencia(reserva))
                .thenReturn(Optional.empty());

        SituacaoSolicitacao situacao = service.descrever(reserva);

        assertEquals("Pendente de documentação", situacao.titulo());
        assertFalse(situacao.autorizada());
    }

    @Test
    void diferenciaConferenciaDevolucaoEContribuicao() {
        DocumentoReserva documento = documento(null);
        when(documentoRepository.obterVersaoMaisRecenteComConferencia(reserva))
                .thenReturn(Optional.of(documento));
        assertEquals("Documento aguardando conferência", service.descrever(reserva).titulo());

        ConferenciaDocumento conferencia = conferencia(
                ResultadoConferenciaDocumento.DEVOLVIDO_CORRECAO);
        ReflectionTestUtils.setField(documento, "conferencia", conferencia);
        assertEquals("Documento devolvido para correção", service.descrever(reserva).titulo());

        conferencia.setResultado(ResultadoConferenciaDocumento.VALIDADO);
        when(contribuicaoRepository.obterPorReservaId(reserva.getId()))
                .thenReturn(Optional.empty());
        assertEquals("Contribuição pendente", service.descrever(reserva).titulo());

        ReflectionTestUtils.setField(conferencia, "id", 30L);
        ContribuicaoApm contribuicao = new ContribuicaoApm();
        contribuicao.setConferenciaDocumento(conferencia);
        when(contribuicaoRepository.obterPorReservaId(reserva.getId()))
                .thenReturn(Optional.of(contribuicao));
        assertEquals("Pronta para análise administrativa", service.descrever(reserva).titulo());
    }

    @Test
    void naoApresentaContribuicaoAntigaComoProntidaoAtual() {
        ConferenciaDocumento conferenciaAtual = conferencia(
                ResultadoConferenciaDocumento.VALIDADO);
        ReflectionTestUtils.setField(conferenciaAtual, "id", 30L);
        DocumentoReserva documento = documento(conferenciaAtual);
        when(documentoRepository.obterVersaoMaisRecenteComConferencia(reserva))
                .thenReturn(Optional.of(documento));

        ConferenciaDocumento conferenciaAntiga = conferencia(
                ResultadoConferenciaDocumento.VALIDADO);
        ReflectionTestUtils.setField(conferenciaAntiga, "id", 29L);
        ContribuicaoApm contribuicao = new ContribuicaoApm();
        contribuicao.setConferenciaDocumento(conferenciaAntiga);
        when(contribuicaoRepository.obterPorReservaId(reserva.getId()))
                .thenReturn(Optional.of(contribuicao));

        assertEquals("Contribuição precisa de revisão", service.descrever(reserva).titulo());
    }

    @Test
    void estadosPersistidosFinaisTemPrioridadeSobreEtapasDocumentais() {
        reserva.setStatus(StatusReserva.PENDENTE_ANALISE);
        assertEquals("Pronta para análise administrativa", service.descrever(reserva).titulo());

        reserva.setStatus(StatusReserva.APROVADA);
        SituacaoSolicitacao aprovada = service.descrever(reserva);
        assertEquals("Aprovada", aprovada.titulo());
        assertTrue(aprovada.autorizada());

        reserva.setStatus(StatusReserva.REJEITADA);
        assertEquals("Rejeitada", service.descrever(reserva).titulo());

        reserva.setStatus(StatusReserva.CANCELADA);
        SituacaoSolicitacao cancelada = service.descrever(reserva);
        assertEquals("Cancelada pela administração", cancelada.titulo());
        assertFalse(cancelada.autorizada());
    }

    private DocumentoReserva documento(ConferenciaDocumento conferencia) {
        DocumentoReserva documento = new DocumentoReserva();
        documento.setReserva(reserva);
        ReflectionTestUtils.setField(documento, "conferencia", conferencia);
        return documento;
    }

    private ConferenciaDocumento conferencia(ResultadoConferenciaDocumento resultado) {
        ConferenciaDocumento conferencia = new ConferenciaDocumento();
        conferencia.setResultado(resultado);
        return conferencia;
    }
}
