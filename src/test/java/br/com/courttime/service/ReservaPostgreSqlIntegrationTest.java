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
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "COURTTIME_POSTGRES_TEST", matches = "true")
class ReservaPostgreSqlIntegrationTest {

    private static final LocalDate DATA = LocalDate.now(CalendarioDisponibilidadeService.FUSO_HORARIO)
            .plusDays(7);
    private static final LocalTime INICIO = LocalTime.of(8, 0);
    private static final LocalTime FIM = LocalTime.of(9, 0);

    @Autowired
    private ReservaService reservaService;

    @Autowired
    private AdministracaoQuadraService administracaoQuadraService;

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private QuadraRepository quadraRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private DisponibilidadeQuadraRepository disponibilidadeQuadraRepository;

    @Autowired
    private BloqueioQuadraRepository bloqueioQuadraRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Quadra quadra;
    private Usuario aluno;
    private Usuario administrador;

    @BeforeEach
    void prepararBanco() {
        reservaRepository.deleteAllInBatch();
        bloqueioQuadraRepository.deleteAllInBatch();
        disponibilidadeQuadraRepository.deleteAllInBatch();
        usuarioRepository.deleteAllInBatch();
        quadraRepository.deleteAllInBatch();

        quadra = novaQuadra();
        aluno = novoUsuario("aluno@teste.local", Perfil.ALUNO);
        administrador = novoUsuario("administrador@teste.local", Perfil.ADMINISTRADOR);
        disponibilidadeQuadraRepository.saveAndFlush(disponibilidade(8, 0, 10, 0));
    }

    @Test
    void postgresCreatesReservationTableAndDeclaredIndexes() {
        Long tabelaExiste = jdbcTemplate.queryForObject("""
                select count(*)
                from information_schema.tables
                where table_schema = current_schema() and table_name = 'reservas'
                """, Long.class);
        List<String> indices = jdbcTemplate.queryForList("""
                select indexname
                from pg_indexes
                where schemaname = current_schema() and tablename = 'reservas'
                """, String.class);

        assertEquals(1L, tabelaExiste);
        assertTrue(indices.contains("idx_reservas_quadra_data_status"));
        assertTrue(indices.contains("idx_reservas_solicitante"));
    }

    @Test
    void persistsRecoversAndQueriesStudentRequest() {
        Reserva criada = reservaService.criarSolicitacao(
                aluno, quadra, DATA, INICIO, FIM, "  Treino da turma  ");

        Reserva recuperada = reservaRepository.findById(criada.getId()).orElseThrow();
        List<Reserva> solicitacoes = reservaService.listarSolicitacoesDoUsuario(aluno);

        assertEquals(StatusReserva.PENDENTE_DOCUMENTACAO, recuperada.getStatus());
        assertEquals("Treino da turma", recuperada.getFinalidade());
        assertEquals(aluno.getId(), recuperada.getSolicitante().getId());
        assertNotNull(recuperada.getCriadaEm());
        assertEquals(List.of(criada.getId()), solicitacoes.stream().map(Reserva::getId).toList());
        assertTrue(reservaRepository.findByIdAndSolicitante(criada.getId(), aluno).isPresent());
        assertTrue(reservaRepository.findByIdAndSolicitante(criada.getId(), administrador).isEmpty());
    }

    @Test
    void postgresRejectsRequestWithSecondOffsetOutsideGeneratedGrid() {
        LocalTime inicioDesalinhado = LocalTime.of(8, 0, 30);
        LocalTime fimDesalinhado = LocalTime.of(9, 0, 30);

        assertThrows(IllegalStateException.class, () -> reservaService.criarSolicitacao(
                aluno,
                quadra,
                DATA,
                inicioDesalinhado,
                fimDesalinhado,
                "Período adulterado fora da grade"));

        assertEquals(0L, reservaRepository.count());
    }

    @Test
    void approvalPersistsTransitionAndApprovedReservationQuery() {
        Reserva pendente = reservaPendente(aluno, INICIO, FIM);

        Reserva aprovada = reservaService.aprovar(pendente.getId(), administrador);
        Reserva recuperada = reservaRepository.findById(aprovada.getId()).orElseThrow();
        List<Reserva> aprovadas = reservaRepository
                .findByQuadraAndDataUtilizacaoAndStatusOrderByHorarioInicio(
                        quadra, DATA, StatusReserva.APROVADA);

        assertEquals(StatusReserva.APROVADA, recuperada.getStatus());
        assertEquals(administrador.getId(), recuperada.getAnalisadaPor().getId());
        assertNotNull(recuperada.getAnalisadaEm());
        assertEquals(List.of(aprovada.getId()), aprovadas.stream().map(Reserva::getId).toList());
    }

    @Test
    void conflictingApprovalRollsBackAllStateChanges() {
        Reserva pendente = reservaPendente(aluno, INICIO, FIM);
        bloqueioQuadraRepository.saveAndFlush(bloqueio(8, 30, 8, 45));

        assertThrows(IllegalStateException.class,
                () -> reservaService.aprovar(pendente.getId(), administrador));

        Reserva recuperada = reservaRepository.findById(pendente.getId()).orElseThrow();
        assertEquals(StatusReserva.PENDENTE_ANALISE, recuperada.getStatus());
        assertNull(recuperada.getAnalisadaPor());
        assertNull(recuperada.getAnalisadaEm());
    }

    @Test
    void pessimisticCourtLockAllowsOnlyOneConcurrentApprovalForSamePeriod() throws Exception {
        Reserva primeira = reservaPendente(aluno, INICIO, FIM);
        Usuario outroAluno = novoUsuario("outro.aluno@teste.local", Perfil.ALUNO);
        Reserva segunda = reservaPendente(outroAluno, INICIO, FIM);
        CountDownLatch prontas = new CountDownLatch(2);
        CountDownLatch iniciar = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> resultadoPrimeira = executor.submit(
                    tentativaDeAprovacao(primeira.getId(), prontas, iniciar));
            Future<Boolean> resultadoSegunda = executor.submit(
                    tentativaDeAprovacao(segunda.getId(), prontas, iniciar));

            assertTrue(prontas.await(5, TimeUnit.SECONDS));
            iniciar.countDown();

            List<Boolean> resultados = List.of(
                    resultadoPrimeira.get(10, TimeUnit.SECONDS),
                    resultadoSegunda.get(10, TimeUnit.SECONDS));
            assertEquals(1L, resultados.stream().filter(Boolean::booleanValue).count());
            assertEquals(1L, resultados.stream().filter(resultado -> !resultado).count());
        } finally {
            executor.shutdownNow();
        }

        List<Reserva> aprovadas = reservaRepository
                .findByQuadraAndDataUtilizacaoAndStatusOrderByHorarioInicio(
                        quadra, DATA, StatusReserva.APROVADA);
        long pendentes = reservaRepository.findAll().stream()
                .filter(reserva -> reserva.getStatus() == StatusReserva.PENDENTE_ANALISE)
                .count();

        assertEquals(1, aprovadas.size());
        assertEquals(1, pendentes);
    }

    @Test
    void approvalAndConflictingBlockAreSerializedByCourtLock() throws Exception {
        Reserva pendente = reservaPendente(aluno, INICIO, FIM);

        List<Boolean> resultados = executarConcorrentemente(
                () -> tentarAprovar(pendente.getId()),
                () -> tentarCadastrarBloqueio());

        Reserva persistida = reservaRepository.findById(pendente.getId()).orElseThrow();
        long bloqueiosAtivos = bloqueioQuadraRepository.findAll().stream()
                .filter(BloqueioQuadra::isAtivo)
                .count();
        assertEquals(1L, resultados.stream().filter(Boolean::booleanValue).count());
        assertTrue((persistida.getStatus() == StatusReserva.APROVADA) ^ (bloqueiosAtivos == 1));
    }

    @Test
    void approvalAndGridChangingEditCannotBothPersist() throws Exception {
        Reserva pendente = reservaPendente(aluno, INICIO, FIM);
        DisponibilidadeQuadra horario = disponibilidadeQuadraRepository.findAll().get(0);

        List<Boolean> resultados = executarConcorrentemente(
                () -> tentarAprovar(pendente.getId()),
                () -> tentarEditarHorario(horario.getId()));

        Reserva reservaPersistida = reservaRepository.findById(pendente.getId()).orElseThrow();
        DisponibilidadeQuadra horarioPersistido = disponibilidadeQuadraRepository
                .findById(horario.getId()).orElseThrow();
        assertEquals(1L, resultados.stream().filter(Boolean::booleanValue).count());
        assertTrue((reservaPersistida.getStatus() == StatusReserva.APROVADA
                && horarioPersistido.getHorarioInicio().equals(LocalTime.of(8, 0)))
                || (reservaPersistida.getStatus() == StatusReserva.PENDENTE_ANALISE
                && horarioPersistido.getHorarioInicio().equals(LocalTime.of(8, 30))));
    }

    @Test
    void concurrentOverlappingWeeklySchedulesCannotBothPersist() throws Exception {
        disponibilidadeQuadraRepository.deleteAllInBatch();

        List<Boolean> resultados = executarConcorrentemente(
                () -> tentarCadastrarHorario(LocalTime.of(8, 0), LocalTime.of(10, 0)),
                () -> tentarCadastrarHorario(LocalTime.of(9, 0), LocalTime.of(11, 0)));

        assertEquals(1L, resultados.stream().filter(Boolean::booleanValue).count());
        assertEquals(1L, disponibilidadeQuadraRepository.count());
    }

    private Callable<Boolean> tentativaDeAprovacao(
            Long reservaId, CountDownLatch prontas, CountDownLatch iniciar) {
        return () -> {
            prontas.countDown();
            if (!iniciar.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("As aprovações não iniciaram de forma coordenada");
            }
            Usuario adminDaTransacao = usuarioRepository.findById(administrador.getId()).orElseThrow();
            try {
                reservaService.aprovar(reservaId, adminDaTransacao);
                return true;
            } catch (IllegalStateException conflitoEsperado) {
                return false;
            }
        };
    }

    private List<Boolean> executarConcorrentemente(
            Callable<Boolean> primeiraOperacao,
            Callable<Boolean> segundaOperacao) throws Exception {
        CountDownLatch prontas = new CountDownLatch(2);
        CountDownLatch iniciar = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> primeira = executor.submit(
                    operacaoSincronizada(primeiraOperacao, prontas, iniciar));
            Future<Boolean> segunda = executor.submit(
                    operacaoSincronizada(segundaOperacao, prontas, iniciar));
            assertTrue(prontas.await(5, TimeUnit.SECONDS));
            iniciar.countDown();
            return List.of(
                    primeira.get(10, TimeUnit.SECONDS),
                    segunda.get(10, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
    }

    private Callable<Boolean> operacaoSincronizada(
            Callable<Boolean> operacao,
            CountDownLatch prontas,
            CountDownLatch iniciar) {
        return () -> {
            prontas.countDown();
            if (!iniciar.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("As operações não iniciaram de forma coordenada");
            }
            return operacao.call();
        };
    }

    private boolean tentarAprovar(Long reservaId) {
        Usuario adminDaTransacao = usuarioRepository.findById(administrador.getId()).orElseThrow();
        try {
            reservaService.aprovar(reservaId, adminDaTransacao);
            return true;
        } catch (IllegalStateException conflitoEsperado) {
            return false;
        }
    }

    private boolean tentarCadastrarBloqueio() {
        Usuario adminDaTransacao = usuarioRepository.findById(administrador.getId()).orElseThrow();
        try {
            administracaoQuadraService.cadastrarBloqueio(
                    adminDaTransacao,
                    DATA,
                    LocalTime.of(8, 30),
                    LocalTime.of(8, 45),
                    "Bloqueio concorrente de teste");
            return true;
        } catch (IllegalStateException conflitoEsperado) {
            return false;
        }
    }

    private boolean tentarEditarHorario(Long horarioId) {
        Usuario adminDaTransacao = usuarioRepository.findById(administrador.getId()).orElseThrow();
        try {
            administracaoQuadraService.editarHorario(
                    horarioId,
                    adminDaTransacao,
                    DiaSemana.from(DATA.getDayOfWeek()),
                    LocalTime.of(8, 30),
                    LocalTime.of(10, 30));
            return true;
        } catch (IllegalStateException conflitoEsperado) {
            return false;
        }
    }

    private boolean tentarCadastrarHorario(LocalTime inicio, LocalTime fim) {
        Usuario adminDaTransacao = usuarioRepository.findById(administrador.getId()).orElseThrow();
        try {
            administracaoQuadraService.cadastrarHorario(
                    adminDaTransacao,
                    DiaSemana.from(DATA.getDayOfWeek()),
                    inicio,
                    fim);
            return true;
        } catch (IllegalStateException conflitoEsperado) {
            return false;
        }
    }

    private Quadra novaQuadra() {
        Quadra nova = new Quadra();
        nova.setNome("Quadra de teste");
        nova.setDescricao("Criada somente no PostgreSQL temporário do CI");
        nova.setAtiva(true);
        return quadraRepository.saveAndFlush(nova);
    }

    private Usuario novoUsuario(String email, Perfil perfil) {
        Usuario usuario = new Usuario();
        usuario.setNome("Usuário de teste");
        usuario.setEmail(email);
        usuario.setSenha("hash-de-teste");
        usuario.setPerfil(perfil);
        usuario.setAtivo(true);
        return usuarioRepository.saveAndFlush(usuario);
    }

    private DisponibilidadeQuadra disponibilidade(
            int horaInicio, int minutoInicio, int horaFim, int minutoFim) {
        DisponibilidadeQuadra disponibilidade = new DisponibilidadeQuadra();
        disponibilidade.setQuadra(quadra);
        disponibilidade.setDiaSemana(DiaSemana.from(DATA.getDayOfWeek()));
        disponibilidade.setHorarioInicio(LocalTime.of(horaInicio, minutoInicio));
        disponibilidade.setHorarioFim(LocalTime.of(horaFim, minutoFim));
        disponibilidade.setAtiva(true);
        return disponibilidade;
    }

    private Reserva reservaPendente(
            Usuario solicitante, LocalTime horarioInicio, LocalTime horarioFim) {
        Reserva reserva = new Reserva();
        reserva.setQuadra(quadra);
        reserva.setSolicitante(solicitante);
        reserva.setDataUtilizacao(DATA);
        reserva.setHorarioInicio(horarioInicio);
        reserva.setHorarioFim(horarioFim);
        reserva.setFinalidade("Atividade esportiva de teste");
        reserva.setStatus(StatusReserva.PENDENTE_ANALISE);
        return reservaRepository.saveAndFlush(reserva);
    }

    private BloqueioQuadra bloqueio(
            int horaInicio, int minutoInicio, int horaFim, int minutoFim) {
        BloqueioQuadra bloqueio = new BloqueioQuadra();
        bloqueio.setQuadra(quadra);
        bloqueio.setData(DATA);
        bloqueio.setHorarioInicio(LocalTime.of(horaInicio, minutoInicio));
        bloqueio.setHorarioFim(LocalTime.of(horaFim, minutoFim));
        bloqueio.setMotivo("Bloqueio de teste");
        bloqueio.setAtivo(true);
        return bloqueio;
    }
}
