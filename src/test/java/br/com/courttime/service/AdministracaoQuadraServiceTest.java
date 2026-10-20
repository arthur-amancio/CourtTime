package br.com.courttime.service;

import br.com.courttime.entity.BloqueioQuadra;
import br.com.courttime.entity.DiaSemana;
import br.com.courttime.entity.DisponibilidadeQuadra;
import br.com.courttime.entity.Perfil;
import br.com.courttime.entity.Quadra;
import br.com.courttime.entity.Reserva;
import br.com.courttime.entity.StatusReserva;
import br.com.courttime.entity.Usuario;
import br.com.courttime.repository.BloqueioQuadraRepository;
import br.com.courttime.repository.DisponibilidadeQuadraRepository;
import br.com.courttime.repository.QuadraRepository;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdministracaoQuadraServiceTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 10, 16);
    private static final LocalDate DATA_FUTURA = LocalDate.of(2026, 10, 19);

    @Mock
    private QuadraService quadraService;
    @Mock
    private QuadraRepository quadraRepository;
    @Mock
    private DisponibilidadeQuadraRepository disponibilidadeRepository;
    @Mock
    private BloqueioQuadraRepository bloqueioRepository;
    @Mock
    private ReservaRepository reservaRepository;
    @Mock
    private CalendarioDisponibilidadeService calendarioService;

    private AdministracaoQuadraService service;
    private Quadra quadra;
    private Usuario administrador;

    @BeforeEach
    void setUp() {
        service = new AdministracaoQuadraService(
                quadraService,
                quadraRepository,
                disponibilidadeRepository,
                bloqueioRepository,
                reservaRepository,
                calendarioService);
        quadra = new Quadra();
        quadra.setId(1L);
        quadra.setNome("Quadra");
        quadra.setDescricao("Quadra de teste");
        quadra.setAtiva(true);
        administrador = usuario(10L, Perfil.ADMINISTRADOR, true);
    }

    @Test
    void horariosDaAdministracaoSaoExibidosDeSegundaADomingoEmOrdemDeInicio() {
        DisponibilidadeQuadra domingo = horario(1L, DiaSemana.DOMINGO, 8, 0, 9, 0, true);
        DisponibilidadeQuadra quarta = horario(2L, DiaSemana.QUARTA, 12, 0, 13, 0, true);
        DisponibilidadeQuadra segundaTarde = horario(3L, DiaSemana.SEGUNDA, 15, 0, 16, 0, true);
        DisponibilidadeQuadra segundaManha = horario(4L, DiaSemana.SEGUNDA, 8, 0, 9, 0, true);
        DisponibilidadeQuadra terca = horario(5L, DiaSemana.TERCA, 10, 0, 11, 0, true);

        when(quadraService.obterQuadra()).thenReturn(quadra);
        when(disponibilidadeRepository.findByQuadraOrderByDiaSemanaAscHorarioInicioAsc(quadra))
                .thenReturn(List.of(domingo, quarta, segundaTarde, segundaManha, terca));

        assertEquals(
                List.of(segundaManha, segundaTarde, terca, quarta, domingo),
                service.listarHorarios(administrador));
    }

    @Test
    void painelAdministrativoUsaAMesmaOrdenacaoSemModificarOPersistido() {
        DisponibilidadeQuadra domingo = horario(1L, DiaSemana.DOMINGO, 8, 0, 9, 0, true);
        DisponibilidadeQuadra segunda = horario(2L, DiaSemana.SEGUNDA, 8, 0, 9, 0, true);

        when(quadraService.obterQuadra()).thenReturn(quadra);
        when(calendarioService.obterDataAtual()).thenReturn(HOJE);
        when(disponibilidadeRepository.findByQuadraOrderByDiaSemanaAscHorarioInicioAsc(quadra))
                .thenReturn(List.of(domingo, segunda));
        when(bloqueioRepository.findByQuadraAndDataGreaterThanEqualOrderByDataAscHorarioInicioAsc(
                quadra, HOJE)).thenReturn(List.of());

        PainelAdministrativo painel = service.carregarPainel(administrador);

        assertEquals(List.of(segunda, domingo), painel.horarios());
    }

    @Test
    void administradorCadastraHorarioComMinutosSemExigirHoraCheia() {
        prepararBloqueioDaQuadra();
        when(disponibilidadeRepository.findByQuadraAndDiaSemanaOrderByHorarioInicioAsc(
                quadra, DiaSemana.SEGUNDA)).thenReturn(List.of());
        when(disponibilidadeRepository.save(any(DisponibilidadeQuadra.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DisponibilidadeQuadra criado = service.cadastrarHorario(
                administrador,
                DiaSemana.SEGUNDA,
                LocalTime.of(8, 15),
                LocalTime.of(10, 15));

        assertEquals(LocalTime.of(8, 15), criado.getHorarioInicio());
        assertEquals(LocalTime.of(10, 15), criado.getHorarioFim());
        assertTrue(criado.isAtiva());
        verify(disponibilidadeRepository).save(criado);
    }

    @Test
    void horarioSobrepostoOuDuplicadoERecusado() {
        prepararBloqueioDaQuadra();
        DisponibilidadeQuadra existente = horario(
                20L, DiaSemana.SEGUNDA, 8, 0, 10, 0, true);
        when(disponibilidadeRepository.findByQuadraAndDiaSemanaOrderByHorarioInicioAsc(
                quadra, DiaSemana.SEGUNDA)).thenReturn(List.of(existente));

        assertThrows(IllegalStateException.class, () -> service.cadastrarHorario(
                administrador,
                DiaSemana.SEGUNDA,
                LocalTime.of(9, 30),
                LocalTime.of(11, 0)));
        assertThrows(IllegalStateException.class, () -> service.cadastrarHorario(
                administrador,
                DiaSemana.SEGUNDA,
                LocalTime.of(8, 0),
                LocalTime.of(10, 0)));
        verify(disponibilidadeRepository, never()).save(any());
    }

    @Test
    void horariosContiguosSaoPermitidos() {
        prepararBloqueioDaQuadra();
        DisponibilidadeQuadra existente = horario(
                20L, DiaSemana.SEGUNDA, 8, 0, 10, 0, true);
        when(disponibilidadeRepository.findByQuadraAndDiaSemanaOrderByHorarioInicioAsc(
                quadra, DiaSemana.SEGUNDA)).thenReturn(List.of(existente));
        when(disponibilidadeRepository.save(any(DisponibilidadeQuadra.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DisponibilidadeQuadra criado = service.cadastrarHorario(
                administrador,
                DiaSemana.SEGUNDA,
                LocalTime.of(10, 0),
                LocalTime.of(12, 0));

        assertEquals(LocalTime.NOON, criado.getHorarioFim());
    }

    @Test
    void desativacaoQueDeixariaReservaAprovadaSemHorarioERecusada() {
        prepararBloqueioDaQuadra();
        DisponibilidadeQuadra horario = horario(
                20L, DiaSemana.SEGUNDA, 8, 0, 10, 0, true);
        Reserva aprovada = reservaAprovada(8, 0, 9, 0);
        when(disponibilidadeRepository.findByIdComQuadra(20L)).thenReturn(Optional.of(horario));
        when(disponibilidadeRepository.findByQuadraAndAtivaTrue(quadra)).thenReturn(List.of());
        when(calendarioService.obterDataAtual()).thenReturn(HOJE);
        when(reservaRepository
                .findByQuadraAndStatusAndDataUtilizacaoGreaterThanEqualOrderByDataUtilizacaoAscHorarioInicioAsc(
                        quadra, StatusReserva.APROVADA, HOJE)).thenReturn(List.of(aprovada));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.alternarHorario(20L, administrador));

        assertTrue(exception.getMessage().contains("reserva aprovada"));
        verify(disponibilidadeRepository, never()).save(any());
    }

    @Test
    void edicaoMantemReservaAprovadaQuandoOutroHorarioContinuaCobrindoPeriodo() {
        prepararBloqueioDaQuadra();
        DisponibilidadeQuadra editado = horario(
                20L, DiaSemana.SEGUNDA, 8, 0, 10, 0, true);
        DisponibilidadeQuadra cobertura = horario(
                21L, DiaSemana.SEGUNDA, 7, 0, 10, 0, true);
        Reserva aprovada = reservaAprovada(8, 0, 9, 0);
        when(disponibilidadeRepository.findByIdComQuadra(20L)).thenReturn(Optional.of(editado));
        when(disponibilidadeRepository.findByQuadraAndDiaSemanaOrderByHorarioInicioAsc(
                quadra, DiaSemana.TERCA)).thenReturn(List.of());
        when(disponibilidadeRepository.findByQuadraAndAtivaTrue(quadra))
                .thenReturn(List.of(editado, cobertura));
        when(calendarioService.obterDataAtual()).thenReturn(HOJE);
        when(reservaRepository
                .findByQuadraAndStatusAndDataUtilizacaoGreaterThanEqualOrderByDataUtilizacaoAscHorarioInicioAsc(
                        quadra, StatusReserva.APROVADA, HOJE)).thenReturn(List.of(aprovada));
        when(disponibilidadeRepository.save(editado)).thenReturn(editado);

        DisponibilidadeQuadra resultado = service.editarHorario(
                20L,
                administrador,
                DiaSemana.TERCA,
                LocalTime.of(14, 0),
                LocalTime.of(16, 0));

        assertEquals(DiaSemana.TERCA, resultado.getDiaSemana());
    }

    @Test
    void edicaoQueDesalinhaReservaAprovadaDaGradeERecusada() {
        prepararBloqueioDaQuadra();
        DisponibilidadeQuadra editado = horario(20L, DiaSemana.SEGUNDA, 8, 0, 11, 0, true);
        Reserva aprovada = reservaAprovada(9, 0, 10, 0);
        when(disponibilidadeRepository.findByIdComQuadra(20L)).thenReturn(Optional.of(editado));
        when(disponibilidadeRepository.findByQuadraAndDiaSemanaOrderByHorarioInicioAsc(
                quadra, DiaSemana.SEGUNDA)).thenReturn(List.of(editado));
        when(disponibilidadeRepository.findByQuadraAndAtivaTrue(quadra)).thenReturn(List.of(editado));
        when(calendarioService.obterDataAtual()).thenReturn(HOJE);
        when(reservaRepository
                .findByQuadraAndStatusAndDataUtilizacaoGreaterThanEqualOrderByDataUtilizacaoAscHorarioInicioAsc(
                        quadra, StatusReserva.APROVADA, HOJE)).thenReturn(List.of(aprovada));

        assertThrows(IllegalStateException.class, () -> service.editarHorario(
                20L, administrador, DiaSemana.SEGUNDA,
                LocalTime.of(8, 30), LocalTime.of(11, 30)));
        verify(disponibilidadeRepository, never()).save(any());
    }

    @Test
    void bloqueioValidoEGravadoComMotivoNormalizado() {
        prepararBloqueioDaQuadra();
        when(calendarioService.obterDataAtual()).thenReturn(HOJE);
        when(bloqueioRepository.findByQuadraAndDataOrderByHorarioInicio(
                quadra, DATA_FUTURA)).thenReturn(List.of());
        when(reservaRepository.findByQuadraAndDataUtilizacaoAndStatusOrderByHorarioInicio(
                quadra, DATA_FUTURA, StatusReserva.APROVADA)).thenReturn(List.of());
        when(bloqueioRepository.save(any(BloqueioQuadra.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BloqueioQuadra criado = service.cadastrarBloqueio(
                administrador,
                DATA_FUTURA,
                LocalTime.of(8, 30),
                LocalTime.of(9, 15),
                "  Manutenção preventiva  ");

        assertEquals("Manutenção preventiva", criado.getMotivo());
        assertTrue(criado.isAtivo());
    }

    @Test
    void bloqueioSobrepostoAOutroBloqueioAtivoERecusado() {
        prepararBloqueioDaQuadra();
        when(calendarioService.obterDataAtual()).thenReturn(HOJE);
        BloqueioQuadra existente = bloqueio(8, 0, 9, 0, true);
        when(bloqueioRepository.findByQuadraAndDataOrderByHorarioInicio(
                quadra, DATA_FUTURA)).thenReturn(List.of(existente));

        assertThrows(IllegalStateException.class, () -> service.cadastrarBloqueio(
                administrador,
                DATA_FUTURA,
                LocalTime.of(8, 30),
                LocalTime.of(10, 0),
                "Evento"));
        verify(bloqueioRepository, never()).save(any());
    }

    @Test
    void bloqueioQueAtingeReservaAprovadaERecusado() {
        prepararBloqueioDaQuadra();
        when(calendarioService.obterDataAtual()).thenReturn(HOJE);
        when(bloqueioRepository.findByQuadraAndDataOrderByHorarioInicio(
                quadra, DATA_FUTURA)).thenReturn(List.of());
        when(reservaRepository.findByQuadraAndDataUtilizacaoAndStatusOrderByHorarioInicio(
                quadra, DATA_FUTURA, StatusReserva.APROVADA))
                .thenReturn(List.of(reservaAprovada(8, 0, 9, 0)));

        assertThrows(IllegalStateException.class, () -> service.cadastrarBloqueio(
                administrador,
                DATA_FUTURA,
                LocalTime.of(8, 30),
                LocalTime.of(9, 30),
                "Evento"));
        verify(bloqueioRepository, never()).save(any());
    }

    @Test
    void desativarBloqueioPreservaRegistroPersistido() {
        prepararBloqueioDaQuadra();
        BloqueioQuadra bloqueio = bloqueio(8, 0, 9, 0, true);
        bloqueio.setId(30L);
        when(bloqueioRepository.findById(30L)).thenReturn(Optional.of(bloqueio));
        when(bloqueioRepository.save(bloqueio)).thenReturn(bloqueio);

        BloqueioQuadra resultado = service.desativarBloqueio(30L, administrador);

        assertFalse(resultado.isAtivo());
        verify(bloqueioRepository).save(bloqueio);
    }

    @Test
    void usuarioSemPerfilAdministrativoNaoPodeAlterarConfiguracao() {
        Usuario aluno = usuario(11L, Perfil.ALUNO, true);

        assertThrows(AccessDeniedException.class, () -> service.cadastrarHorario(
                aluno,
                DiaSemana.SEGUNDA,
                LocalTime.of(8, 0),
                LocalTime.of(10, 0)));
        verify(quadraRepository, never()).findByIdForUpdate(any());
    }

    private void prepararBloqueioDaQuadra() {
        when(quadraService.obterQuadra()).thenReturn(quadra);
        when(quadraRepository.findByIdForUpdate(quadra.getId())).thenReturn(Optional.of(quadra));
    }

    private Usuario usuario(Long id, Perfil perfil, boolean ativo) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNome("Usuário");
        usuario.setEmail("usuario" + id + "@escola.test");
        usuario.setSenha("hash");
        usuario.setPerfil(perfil);
        usuario.setAtivo(ativo);
        return usuario;
    }

    private DisponibilidadeQuadra horario(
            Long id,
            DiaSemana dia,
            int horaInicio,
            int minutoInicio,
            int horaFim,
            int minutoFim,
            boolean ativo) {
        DisponibilidadeQuadra horario = new DisponibilidadeQuadra();
        horario.setId(id);
        horario.setQuadra(quadra);
        horario.setDiaSemana(dia);
        horario.setHorarioInicio(LocalTime.of(horaInicio, minutoInicio));
        horario.setHorarioFim(LocalTime.of(horaFim, minutoFim));
        horario.setAtiva(ativo);
        return horario;
    }

    private BloqueioQuadra bloqueio(
            int horaInicio, int minutoInicio, int horaFim, int minutoFim, boolean ativo) {
        BloqueioQuadra bloqueio = new BloqueioQuadra();
        bloqueio.setQuadra(quadra);
        bloqueio.setData(DATA_FUTURA);
        bloqueio.setHorarioInicio(LocalTime.of(horaInicio, minutoInicio));
        bloqueio.setHorarioFim(LocalTime.of(horaFim, minutoFim));
        bloqueio.setMotivo("Motivo");
        bloqueio.setAtivo(ativo);
        return bloqueio;
    }

    private Reserva reservaAprovada(
            int horaInicio, int minutoInicio, int horaFim, int minutoFim) {
        Reserva reserva = new Reserva();
        reserva.setQuadra(quadra);
        reserva.setDataUtilizacao(DATA_FUTURA);
        reserva.setHorarioInicio(LocalTime.of(horaInicio, minutoInicio));
        reserva.setHorarioFim(LocalTime.of(horaFim, minutoFim));
        reserva.setStatus(StatusReserva.APROVADA);
        return reserva;
    }
}
