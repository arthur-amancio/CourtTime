package br.com.courttime.service;

import br.com.courttime.entity.CategoriaParticipante;
import br.com.courttime.entity.DocumentoReserva;
import br.com.courttime.entity.ParticipanteReserva;
import br.com.courttime.entity.Perfil;
import br.com.courttime.entity.Reserva;
import br.com.courttime.entity.StatusReserva;
import br.com.courttime.entity.Usuario;
import br.com.courttime.repository.ContribuicaoApmRepository;
import br.com.courttime.repository.DocumentoReservaRepository;
import br.com.courttime.repository.ParticipanteReservaRepository;
import br.com.courttime.repository.ReservaRepository;
import br.com.courttime.storage.DocumentoStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnaliseAdministrativaServiceTest {

    @Mock
    private ReservaRepository reservaRepository;
    @Mock
    private ParticipanteReservaRepository participanteRepository;
    @Mock
    private DocumentoReservaRepository documentoRepository;
    @Mock
    private ContribuicaoApmRepository contribuicaoRepository;
    @Mock
    private DocumentoStorage documentoStorage;

    private AnaliseAdministrativaService service;
    private Usuario administrador;
    private Reserva reserva;

    @BeforeEach
    void setUp() {
        service = new AnaliseAdministrativaService(
                reservaRepository,
                participanteRepository,
                documentoRepository,
                contribuicaoRepository,
                documentoStorage);
        administrador = usuario(1L, Perfil.ADMINISTRADOR);
        reserva = new Reserva();
        reserva.setId(20L);
        reserva.setStatus(StatusReserva.PENDENTE_ANALISE);
    }

    @Test
    void filaConsultaSomenteEstadoProntoParaAnalise() {
        when(reservaRepository.listarParaAnaliseAdministrativa(
                StatusReserva.PENDENTE_ANALISE)).thenReturn(List.of(reserva));

        assertEquals(List.of(reserva), service.listarPendentes(administrador));
    }

    @Test
    void listaSomenteReservasAutorizadasAtuaisOuFuturas() {
        LocalDate hoje = LocalDate.now(CalendarioDisponibilidadeService.FUSO_HORARIO);
        reserva.setStatus(StatusReserva.APROVADA);
        when(reservaRepository.listarAutorizadas(
                StatusReserva.APROVADA, hoje)).thenReturn(List.of(reserva));

        assertEquals(List.of(reserva), service.listarAutorizadasAtuais(administrador));
        verify(reservaRepository).listarAutorizadas(
                StatusReserva.APROVADA, hoje);
    }

    @Test
    void listaRejeitadasECanceladasParaConsultaDaAdministracao() {
        reserva.setStatus(StatusReserva.REJEITADA);
        when(reservaRepository.listarEncerradasParaAdministracao(
                List.of(StatusReserva.REJEITADA, StatusReserva.CANCELADA)))
                .thenReturn(List.of(reserva));

        assertEquals(List.of(reserva), service.listarEncerradas(administrador));
    }

    @Test
    void encerradasExigemAdministradorAtivo() {
        Usuario secretaria = usuario(9L, Perfil.SECRETARIA);

        assertThrows(AccessDeniedException.class, () -> service.listarEncerradas(secretaria));
        verifyNoInteractions(reservaRepository);
    }

    @Test
    void detalheResumeSeparadamenteAlunosEDemaisParticipantes() {
        ParticipanteReserva aluno = participante(CategoriaParticipante.ALUNO);
        ParticipanteReserva outro = participante(CategoriaParticipante.OUTRO);
        when(reservaRepository.obterParaConsultaAdministrativa(20L))
                .thenReturn(Optional.of(reserva));
        when(participanteRepository.listarComPresenca(reserva))
                .thenReturn(List.of(aluno, outro));

        DetalheAnaliseAdministrativa detalhe = service.detalhar(20L, administrador);

        assertEquals(1, detalhe.resumoParticipantes().quantidadeAlunos());
        assertEquals(1, detalhe.resumoParticipantes().quantidadeOutros());
        assertEquals(2, detalhe.resumoParticipantes().total());
    }

    @Test
    void downloadExigeDocumentoDaSolicitacaoInformada() {
        when(reservaRepository.obterParaConsultaAdministrativa(20L))
                .thenReturn(Optional.of(reserva));
        when(documentoRepository.findByIdAndReserva(99L, reserva))
                .thenReturn(Optional.empty());

        assertThrows(
                AccessDeniedException.class,
                () -> service.baixar(20L, 99L, administrador));
    }

    @Test
    void downloadCarregaSomenteChavePrivadaDoDocumentoAutorizado() {
        byte[] pdf = "%PDF-1.7".getBytes();
        DocumentoReserva documento = new DocumentoReserva();
        documento.setNomeOriginal("termo.pdf");
        documento.setTamanhoBytes(pdf.length);
        documento.setChaveArmazenamento("chave-privada.pdf");
        when(reservaRepository.obterParaConsultaAdministrativa(20L))
                .thenReturn(Optional.of(reserva));
        when(documentoRepository.findByIdAndReserva(30L, reserva))
                .thenReturn(Optional.of(documento));
        when(documentoStorage.carregar("chave-privada.pdf"))
                .thenReturn(new ByteArrayResource(pdf));

        DownloadDocumento download = service.baixar(20L, 30L, administrador);

        assertEquals("termo.pdf", download.nomeArquivo());
        verify(documentoStorage).carregar("chave-privada.pdf");
    }

    @Test
    void perfisNaoAdministrativosSaoRecusadosNoBackend() {
        assertThrows(
                AccessDeniedException.class,
                () -> service.listarPendentes(usuario(2L, Perfil.SECRETARIA)));
        assertThrows(
                AccessDeniedException.class,
                () -> service.listarAutorizadasAtuais(usuario(3L, Perfil.SEGURANCA)));
    }

    private ParticipanteReserva participante(CategoriaParticipante categoria) {
        ParticipanteReserva participante = new ParticipanteReserva();
        participante.setCategoria(categoria);
        return participante;
    }

    private Usuario usuario(Long id, Perfil perfil) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNome("Usuário");
        usuario.setEmail("usuario" + id + "@teste.local");
        usuario.setSenha("hash");
        usuario.setPerfil(perfil);
        usuario.setAtivo(true);
        return usuario;
    }
}
