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
import br.com.courttime.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "COURTTIME_POSTGRES_TEST", matches = "true")
class AdministracaoQuadraPostgreSqlIntegrationTest {

    @Autowired
    private AdministracaoQuadraService administracaoService;
    @Autowired
    private CalendarioDisponibilidadeService calendarioService;
    @Autowired
    private DisponibilidadeQuadraRepository disponibilidadeRepository;
    @Autowired
    private BloqueioQuadraRepository bloqueioRepository;
    @Autowired
    private ReservaRepository reservaRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private QuadraRepository quadraRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Quadra quadra;
    private Usuario administrador;
    private Usuario aluno;
    private LocalDate dataFutura;
    private DiaSemana diaSemana;

    @BeforeEach
    void prepararBancoIsolado() {
        reservaRepository.deleteAllInBatch();
        bloqueioRepository.deleteAllInBatch();
        disponibilidadeRepository.deleteAllInBatch();
        usuarioRepository.deleteAllInBatch();
        quadraRepository.deleteAllInBatch();

        quadra = new Quadra();
        quadra.setNome("Quadra de integração");
        quadra.setDescricao("Registro criado somente no PostgreSQL temporário de testes");
        quadra.setAtiva(true);
        quadra = quadraRepository.saveAndFlush(quadra);
        administrador = salvarUsuario("admin.integracao@teste.local", Perfil.ADMINISTRADOR);
        aluno = salvarUsuario("aluno.integracao@teste.local", Perfil.ALUNO);
        dataFutura = calendarioService.obterDataAtual().plusDays(7);
        diaSemana = DiaSemana.from(dataFutura.getDayOfWeek());
    }

    @Test
    void postgresCriaTabelasDeDisponibilidadeEBloqueio() {
        List<String> tabelas = jdbcTemplate.queryForList("""
                select table_name
                from information_schema.tables
                where table_schema = current_schema()
                  and table_name in ('disponibilidades_quadra', 'bloqueios_quadra')
                order by table_name
                """, String.class);

        assertEquals(List.of("bloqueios_quadra", "disponibilidades_quadra"), tabelas);
    }

    @Test
    void horarioCadastradoPermaneceNoPostgresEApareceNoCalendario() {
        DisponibilidadeQuadra criada = administracaoService.cadastrarHorario(
                administrador,
                diaSemana,
                LocalTime.of(8, 15),
                LocalTime.of(10, 15));

        DisponibilidadeQuadra recuperada = disponibilidadeRepository.findById(criada.getId())
                .orElseThrow();
        ConsultaDisponibilidade consulta = calendarioService.consultar(quadra, dataFutura);

        assertEquals(LocalTime.of(8, 15), recuperada.getHorarioInicio());
        assertEquals(LocalTime.of(10, 15), recuperada.getHorarioFim());
        assertEquals(2, consulta.horarios().size());
        assertTrue(consulta.horarios().stream().allMatch(HorarioDisponibilidade::isDisponivel));
    }

    @Test
    void edicaoEDesativacaoPersistemSemExcluirRegistro() {
        DisponibilidadeQuadra criada = administracaoService.cadastrarHorario(
                administrador,
                diaSemana,
                LocalTime.of(8, 0),
                LocalTime.of(10, 0));

        administracaoService.editarHorario(
                criada.getId(),
                administrador,
                diaSemana,
                LocalTime.of(9, 0),
                LocalTime.of(11, 0));
        administracaoService.alternarHorario(criada.getId(), administrador);
        DisponibilidadeQuadra recuperada = disponibilidadeRepository.findById(criada.getId())
                .orElseThrow();
        assertEquals(LocalTime.of(9, 0), recuperada.getHorarioInicio());
        assertEquals(LocalTime.of(11, 0), recuperada.getHorarioFim());
        assertFalse(recuperada.isAtiva());
    }

    @Test
    void bloqueioPersistidoAlteraConsultaDoCalendarioEDesativacaoRestauraHorario() {
        administracaoService.cadastrarHorario(
                administrador,
                diaSemana,
                LocalTime.of(8, 0),
                LocalTime.of(10, 0));
        BloqueioQuadra bloqueio = administracaoService.cadastrarBloqueio(
                administrador,
                dataFutura,
                LocalTime.of(8, 30),
                LocalTime.of(8, 45),
                "Manutenção preventiva");

        ConsultaDisponibilidade comBloqueio = calendarioService.consultar(quadra, dataFutura);
        assertEquals(SituacaoHorario.BLOQUEADO, comBloqueio.horarios().get(0).situacao());
        assertEquals(SituacaoHorario.DISPONIVEL, comBloqueio.horarios().get(1).situacao());

        administracaoService.desativarBloqueio(bloqueio.getId(), administrador);
        BloqueioQuadra recuperado = bloqueioRepository.findById(bloqueio.getId()).orElseThrow();
        ConsultaDisponibilidade semBloqueio = calendarioService.consultar(quadra, dataFutura);

        assertFalse(recuperado.isAtivo());
        assertTrue(semBloqueio.horarios().stream().allMatch(HorarioDisponibilidade::isDisponivel));
    }

    @Test
    void reservaAprovadaImpedeDesativarHorarioECadastrarBloqueioConflitante() {
        DisponibilidadeQuadra horario = administracaoService.cadastrarHorario(
                administrador,
                diaSemana,
                LocalTime.of(8, 0),
                LocalTime.of(10, 0));
        salvarReservaAprovada();

        assertThrows(
                IllegalStateException.class,
                () -> administracaoService.alternarHorario(horario.getId(), administrador));
        assertThrows(
                IllegalStateException.class,
                () -> administracaoService.cadastrarBloqueio(
                        administrador,
                        dataFutura,
                        LocalTime.of(8, 30),
                        LocalTime.of(9, 30),
                        "Evento institucional"));

        assertTrue(disponibilidadeRepository.findById(horario.getId()).orElseThrow().isAtiva());
        assertTrue(bloqueioRepository.findAll().isEmpty());
    }

    @Test
    void postgresRecusaEdicaoQueDesalinhaReservaAprovadaDaGrade() {
        DisponibilidadeQuadra horario = administracaoService.cadastrarHorario(
                administrador, diaSemana, LocalTime.of(8, 0), LocalTime.of(11, 0));
        salvarReservaAprovada();

        assertThrows(IllegalStateException.class, () -> administracaoService.editarHorario(
                horario.getId(), administrador, diaSemana,
                LocalTime.of(7, 30), LocalTime.of(11, 30)));

        DisponibilidadeQuadra persistido = disponibilidadeRepository.findById(horario.getId()).orElseThrow();
        assertEquals(LocalTime.of(8, 0), persistido.getHorarioInicio());
        assertTrue(calendarioService.consultar(quadra, dataFutura).horarios().stream()
                .anyMatch(slot -> slot.horarioInicio().equals(LocalTime.of(8, 0))
                        && slot.situacao() == SituacaoHorario.RESERVADO));
    }

    private Usuario salvarUsuario(String email, Perfil perfil) {
        Usuario usuario = new Usuario();
        usuario.setNome("Usuário de integração");
        usuario.setEmail(email);
        usuario.setSenha("hash-de-teste");
        usuario.setPerfil(perfil);
        usuario.setAtivo(true);
        return usuarioRepository.saveAndFlush(usuario);
    }

    private void salvarReservaAprovada() {
        Reserva reserva = new Reserva();
        reserva.setQuadra(quadra);
        reserva.setSolicitante(aluno);
        reserva.setDataUtilizacao(dataFutura);
        reserva.setHorarioInicio(LocalTime.of(8, 0));
        reserva.setHorarioFim(LocalTime.of(9, 0));
        reserva.setFinalidade("Atividade esportiva de integração");
        reserva.setStatus(StatusReserva.APROVADA);
        reserva.setAnalisadaPor(administrador);
        reservaRepository.saveAndFlush(reserva);
    }
}
