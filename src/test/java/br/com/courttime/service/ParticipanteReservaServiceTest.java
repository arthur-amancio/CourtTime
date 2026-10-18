package br.com.courttime.service;

import br.com.courttime.entity.CategoriaParticipante;
import br.com.courttime.entity.DocumentoReserva;
import br.com.courttime.entity.ParticipanteReserva;
import br.com.courttime.entity.Perfil;
import br.com.courttime.entity.Reserva;
import br.com.courttime.entity.StatusReserva;
import br.com.courttime.entity.Usuario;
import br.com.courttime.repository.ParticipanteReservaRepository;
import br.com.courttime.repository.DocumentoReservaRepository;
import br.com.courttime.repository.ReservaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParticipanteReservaServiceTest {

    @Mock
    private ParticipanteReservaRepository participanteRepository;

    @Mock
    private ReservaRepository reservaRepository;
    @Mock
    private DocumentoReservaRepository documentoRepository;

    private ParticipanteReservaService service;
    private Usuario aluno;
    private Reserva reserva;

    @BeforeEach
    void setUp() {
        service = new ParticipanteReservaService(
                participanteRepository, reservaRepository, documentoRepository);
        aluno = usuario(10L, Perfil.ALUNO, true);
        reserva = reserva(20L, aluno, StatusReserva.PENDENTE_DOCUMENTACAO);
    }

    @Test
    void adicionaParticipanteNormalizadoAposBloquearSolicitacaoDoAluno() {
        when(reservaRepository.findByIdAndSolicitanteIdForUpdate(20L, 10L))
                .thenReturn(Optional.of(reserva));
        when(participanteRepository.save(any(ParticipanteReserva.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ParticipanteReserva participante = service.adicionar(
                20L,
                aluno,
                "  Ana   Souza  ",
                "  12.345.678-X  ",
                CategoriaParticipante.ALUNO);

        assertSame(reserva, participante.getReserva());
        assertEquals("Ana Souza", participante.getNomeCompleto());
        assertEquals("12.345.678-X", participante.getRmOuRg());
        assertEquals(CategoriaParticipante.ALUNO, participante.getCategoria());
        assertEquals(1, reserva.getRevisaoParticipantes());
        verify(reservaRepository).findByIdAndSolicitanteIdForUpdate(20L, 10L);
        verify(reservaRepository).save(reserva);
    }

    @Test
    void recusaMutacaoQuandoDocumentacaoNaoEstaPendente() {
        reserva.setStatus(StatusReserva.PENDENTE_ANALISE);
        when(reservaRepository.findByIdAndSolicitanteIdForUpdate(20L, 10L))
                .thenReturn(Optional.of(reserva));

        assertThrows(IllegalStateException.class, () -> service.adicionar(
                20L, aluno, "Ana Souza", "12345", CategoriaParticipante.ALUNO));

        verify(participanteRepository, never()).save(any());
    }

    @Test
    void recusaSolicitacaoQueNaoPertenceAoAlunoSemConsultarParticipante() {
        when(reservaRepository.findByIdAndSolicitanteIdForUpdate(20L, 10L))
                .thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () -> service.remover(20L, 30L, aluno));

        verify(participanteRepository, never()).findByIdAndReserva(any(), any());
    }

    @Test
    void buscaAninhadaImpedeEditarParticipanteDeOutraSolicitacao() {
        when(reservaRepository.findByIdAndSolicitanteIdForUpdate(20L, 10L))
                .thenReturn(Optional.of(reserva));
        when(participanteRepository.findByIdAndReserva(30L, reserva))
                .thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () -> service.editar(
                20L,
                30L,
                aluno,
                "Ana Souza",
                "12345",
                CategoriaParticipante.ALUNO));

        verify(participanteRepository, never()).save(any());
    }

    @Test
    void editaERemoveSomenteParticipanteDaSolicitacaoBloqueada() {
        ParticipanteReserva participante = participante(
                30L, reserva, "Nome anterior", "111", CategoriaParticipante.OUTRO);
        when(reservaRepository.findByIdAndSolicitanteIdForUpdate(20L, 10L))
                .thenReturn(Optional.of(reserva));
        when(participanteRepository.findByIdAndReserva(30L, reserva))
                .thenReturn(Optional.of(participante));
        when(participanteRepository.save(participante)).thenReturn(participante);

        ParticipanteReserva editado = service.editar(
                20L,
                30L,
                aluno,
                "Ana Souza",
                "222",
                CategoriaParticipante.ALUNO);
        service.remover(20L, 30L, aluno);

        assertEquals("Ana Souza", editado.getNomeCompleto());
        assertEquals("222", editado.getRmOuRg());
        assertEquals(CategoriaParticipante.ALUNO, editado.getCategoria());
        assertEquals(2, reserva.getRevisaoParticipantes());
        verify(participanteRepository).delete(participante);
    }

    @Test
    void recusaIdentificacaoDuplicadaDepoisDaNormalizacao() {
        when(reservaRepository.findByIdAndSolicitanteIdForUpdate(20L, 10L))
                .thenReturn(Optional.of(reserva));
        when(participanteRepository.existsByReservaAndRmOuRgIgnoreCase(
                reserva, "RM 123")).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.adicionar(
                        20L, aluno, "Ana Souza", "  RM   123 ", CategoriaParticipante.ALUNO));

        assertEquals("Já existe um participante com este RM ou RG na solicitação",
                exception.getMessage());
        assertEquals(0, reserva.getRevisaoParticipantes());
        verify(participanteRepository, never()).save(any());
    }

    @Test
    void listaEObtemParticipantesSomenteDepoisDeValidarProprietario() {
        ParticipanteReserva participante = participante(
                30L, reserva, "Ana Souza", "123", CategoriaParticipante.ALUNO);
        when(reservaRepository.findByIdAndSolicitante(20L, aluno))
                .thenReturn(Optional.of(reserva));
        when(participanteRepository.findByReservaOrderByNomeCompletoAscIdAsc(reserva))
                .thenReturn(List.of(participante));
        when(participanteRepository.findByIdAndReserva(30L, reserva))
                .thenReturn(Optional.of(participante));

        assertEquals(List.of(participante), service.listar(20L, aluno));
        assertSame(participante, service.obter(20L, 30L, aluno));
    }

    @Test
    void resumoSeparaCategoriasEExigeDezAlunos() {
        List<ParticipanteReserva> noveAlunosEOutro = participantes(
                reserva, 9, 1);
        List<ParticipanteReserva> dezAlunosEDoisOutros = participantes(
                reserva, 10, 2);
        when(reservaRepository.findByIdAndSolicitante(20L, aluno))
                .thenReturn(Optional.of(reserva));
        when(participanteRepository.findByReservaOrderByNomeCompletoAscIdAsc(reserva))
                .thenReturn(noveAlunosEOutro, dezAlunosEDoisOutros);

        ResumoParticipantes abaixoDoMinimo = service.resumir(20L, aluno);
        ResumoParticipantes minimoAtendido = service.resumir(20L, aluno);

        assertEquals(9, abaixoDoMinimo.quantidadeAlunos());
        assertEquals(1, abaixoDoMinimo.quantidadeOutros());
        assertEquals(10, abaixoDoMinimo.total());
        assertFalse(abaixoDoMinimo.minimoDeAlunosAtendido());
        assertEquals(10, minimoAtendido.quantidadeAlunos());
        assertEquals(2, minimoAtendido.quantidadeOutros());
        assertEquals(12, minimoAtendido.total());
        assertTrue(minimoAtendido.minimoDeAlunosAtendido());
    }

    @Test
    void documentoExigeMesmaRevisaoEDezAlunos() {
        DocumentoReserva documento = new DocumentoReserva();
        documento.setReserva(reserva);
        documento.setRevisaoParticipantes(0);
        when(participanteRepository.countByReservaAndCategoria(
                reserva, CategoriaParticipante.ALUNO)).thenReturn(9L, 10L);

        assertThrows(IllegalStateException.class,
                () -> service.validarDocumentoAtual(reserva, documento));
        service.validarDocumentoAtual(reserva, documento);

        reserva.incrementarRevisaoParticipantes();
        assertThrows(IllegalStateException.class,
                () -> service.validarDocumentoAtual(reserva, documento));
    }

    @Test
    void recusaPerfilInativoCamposInvalidosECategoriaAusente() {
        Usuario inativo = usuario(11L, Perfil.ALUNO, false);
        Usuario administrador = usuario(12L, Perfil.ADMINISTRADOR, true);

        assertThrows(AccessDeniedException.class, () -> service.listar(20L, inativo));
        assertThrows(AccessDeniedException.class, () -> service.listar(20L, administrador));

        when(reservaRepository.findByIdAndSolicitanteIdForUpdate(20L, 10L))
                .thenReturn(Optional.of(reserva));
        assertThrows(IllegalArgumentException.class, () -> service.adicionar(
                20L, aluno, " ", "123", CategoriaParticipante.ALUNO));
        assertThrows(IllegalArgumentException.class, () -> service.adicionar(
                20L, aluno, "Ana Souza", " ", CategoriaParticipante.ALUNO));
        assertThrows(IllegalArgumentException.class, () -> service.adicionar(
                20L, aluno, "Ana Souza", "123", null));
        verify(participanteRepository, never()).save(any());
    }

    private List<ParticipanteReserva> participantes(
            Reserva reserva, int quantidadeAlunos, int quantidadeOutros) {
        List<ParticipanteReserva> participantes = new ArrayList<>();
        for (int indice = 0; indice < quantidadeAlunos; indice++) {
            participantes.add(participante(
                    (long) indice,
                    reserva,
                    "Aluno " + indice,
                    "RM" + indice,
                    CategoriaParticipante.ALUNO));
        }
        for (int indice = 0; indice < quantidadeOutros; indice++) {
            participantes.add(participante(
                    100L + indice,
                    reserva,
                    "Outro " + indice,
                    "RG" + indice,
                    CategoriaParticipante.OUTRO));
        }
        return participantes;
    }

    private ParticipanteReserva participante(
            Long id,
            Reserva reserva,
            String nome,
            String rmOuRg,
            CategoriaParticipante categoria) {
        ParticipanteReserva participante = new ParticipanteReserva();
        participante.setId(id);
        participante.setReserva(reserva);
        participante.setNomeCompleto(nome);
        participante.setRmOuRg(rmOuRg);
        participante.setCategoria(categoria);
        return participante;
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
