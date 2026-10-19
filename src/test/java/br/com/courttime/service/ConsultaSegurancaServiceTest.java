package br.com.courttime.service;

import br.com.courttime.entity.CategoriaParticipante;
import br.com.courttime.entity.ParticipanteReserva;
import br.com.courttime.entity.Perfil;
import br.com.courttime.entity.Quadra;
import br.com.courttime.entity.Reserva;
import br.com.courttime.entity.StatusReserva;
import br.com.courttime.entity.Usuario;
import br.com.courttime.repository.ParticipanteReservaRepository;
import br.com.courttime.repository.ReservaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultaSegurancaServiceTest {

    @Mock
    private ReservaRepository reservaRepository;
    @Mock
    private ParticipanteReservaRepository participanteRepository;

    private ConsultaSegurancaService service;
    private Usuario seguranca;
    private Reserva reserva;

    @BeforeEach
    void setUp() {
        service = new ConsultaSegurancaService(
                reservaRepository, participanteRepository);
        seguranca = usuario(1L, "Agente da segurança", Perfil.SEGURANCA, true);
        reserva = reservaAutorizada();
    }

    @Test
    void listaSomenteAutorizadasDeHojeEmDianteComDadosMinimos() {
        LocalDate hoje = LocalDate.now(CalendarioDisponibilidadeService.FUSO_HORARIO);
        when(reservaRepository.listarAutorizadasParaSeguranca(
                StatusReserva.APROVADA, hoje)).thenReturn(List.of(reserva));
        when(participanteRepository.countByReserva(reserva)).thenReturn(11L);

        List<ReservaAutorizadaResumo> resultado = service.listarAutorizadas(seguranca);

        assertEquals(1, resultado.size());
        ReservaAutorizadaResumo resumo = resultado.get(0);
        assertEquals(20L, resumo.id());
        assertEquals("Quadra Poliesportiva", resumo.quadra());
        assertEquals("Ana Aluna", resumo.solicitante());
        assertEquals(11L, resumo.quantidadeParticipantes());
        verify(reservaRepository).listarAutorizadasParaSeguranca(
                StatusReserva.APROVADA, hoje);
    }

    @Test
    void detalheTrazSomenteIdentificacaoNecessariaDosParticipantes() {
        LocalDate hoje = LocalDate.now(CalendarioDisponibilidadeService.FUSO_HORARIO);
        ParticipanteReserva participante = participante(
                "Carlos Participante", "RM-123", CategoriaParticipante.ALUNO);
        when(reservaRepository.obterAutorizadaParaSeguranca(
                20L, StatusReserva.APROVADA, hoje)).thenReturn(Optional.of(reserva));
        when(participanteRepository.findByReservaOrderByNomeCompletoAscIdAsc(reserva))
                .thenReturn(List.of(participante));
        when(participanteRepository.countByReserva(reserva)).thenReturn(1L);

        DetalheReservaAutorizada detalhe = service.detalhar(20L, seguranca);

        assertEquals("Carlos Participante", detalhe.participantes().get(0).nomeCompleto());
        assertEquals("RM-123", detalhe.participantes().get(0).rmOuRg());
        assertEquals("Aluno", detalhe.participantes().get(0).categoria());
    }

    @Test
    void reservaPendenteCanceladaOuAntigaNaoPodeSerConsultada() {
        LocalDate hoje = LocalDate.now(CalendarioDisponibilidadeService.FUSO_HORARIO);
        when(reservaRepository.obterAutorizadaParaSeguranca(
                99L, StatusReserva.APROVADA, hoje)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.detalhar(99L, seguranca));

        assertEquals("Reserva autorizada não encontrada", exception.getMessage());
        verify(reservaRepository).obterAutorizadaParaSeguranca(
                99L, StatusReserva.APROVADA, hoje);
        verifyNoInteractions(participanteRepository);
    }

    @Test
    void backendRecusaOutrosPerfisEContaInativa() {
        assertThrows(
                AccessDeniedException.class,
                () -> service.listarAutorizadas(
                        usuario(2L, "Administrador", Perfil.ADMINISTRADOR, true)));
        assertThrows(
                AccessDeniedException.class,
                () -> service.listarAutorizadas(
                        usuario(3L, "Segurança inativa", Perfil.SEGURANCA, false)));

        verifyNoInteractions(reservaRepository, participanteRepository);
    }

    private Reserva reservaAutorizada() {
        Quadra quadra = new Quadra();
        quadra.setId(3L);
        quadra.setNome("Quadra Poliesportiva");
        Reserva nova = new Reserva();
        nova.setId(20L);
        nova.setQuadra(quadra);
        nova.setSolicitante(usuario(4L, "Ana Aluna", Perfil.ALUNO, true));
        nova.setDataUtilizacao(LocalDate.of(2030, 10, 21));
        nova.setHorarioInicio(LocalTime.of(8, 0));
        nova.setHorarioFim(LocalTime.of(9, 0));
        nova.setFinalidade("Treino da turma");
        nova.setStatus(StatusReserva.APROVADA);
        return nova;
    }

    private ParticipanteReserva participante(
            String nome, String documento, CategoriaParticipante categoria) {
        ParticipanteReserva participante = new ParticipanteReserva();
        participante.setReserva(reserva);
        participante.setNomeCompleto(nome);
        participante.setRmOuRg(documento);
        participante.setCategoria(categoria);
        return participante;
    }

    private Usuario usuario(
            Long id, String nome, Perfil perfil, boolean ativo) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNome(nome);
        usuario.setEmail("usuario" + id + "@teste.local");
        usuario.setSenha("hash");
        usuario.setPerfil(perfil);
        usuario.setAtivo(ativo);
        return usuario;
    }
}
