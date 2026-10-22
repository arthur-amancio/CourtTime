package br.com.courttime.service;

import br.com.courttime.entity.CategoriaParticipante;
import br.com.courttime.entity.LocalPagamento;
import br.com.courttime.entity.PagamentoAdicional;
import br.com.courttime.entity.ParticipanteReserva;
import br.com.courttime.entity.Perfil;
import br.com.courttime.entity.PresencaParticipante;
import br.com.courttime.entity.Quadra;
import br.com.courttime.entity.Reserva;
import br.com.courttime.entity.StatusReserva;
import br.com.courttime.entity.Usuario;
import br.com.courttime.entity.VinculoParticipante;
import br.com.courttime.repository.AjusteContribuicaoApmRepository;
import br.com.courttime.repository.ConferenciaDocumentoRepository;
import br.com.courttime.repository.ContribuicaoApmRepository;
import br.com.courttime.repository.DocumentoReservaRepository;
import br.com.courttime.repository.PagamentoAdicionalRepository;
import br.com.courttime.repository.ParticipanteReservaRepository;
import br.com.courttime.repository.QuadraRepository;
import br.com.courttime.repository.ReservaRepository;
import br.com.courttime.repository.UsuarioRepository;
import br.com.courttime.support.BancoDeTeste;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** R10/R11: pagamentos e liberação persistidos no PostgreSQL temporário da CI. */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "COURTTIME_POSTGRES_TEST", matches = "true")
class PortariaPostgreSqlIntegrationTest {

    @Autowired private PortariaService portariaService;
    @Autowired private ConsultaSegurancaService consultaSegurancaService;
    @Autowired private DataSource dataSource;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private AjusteContribuicaoApmRepository ajusteRepository;
    @Autowired private ContribuicaoApmRepository contribuicaoRepository;
    @Autowired private ConferenciaDocumentoRepository conferenciaRepository;
    @Autowired private DocumentoReservaRepository documentoRepository;
    @Autowired private PagamentoAdicionalRepository pagamentoRepository;
    @Autowired private ParticipanteReservaRepository participanteRepository;
    @Autowired private ReservaRepository reservaRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private QuadraRepository quadraRepository;

    private Usuario seguranca;
    private Usuario secretaria;
    private Usuario segundaSecretaria;
    private Reserva reserva;
    private List<ParticipanteReserva> participantes;

    @BeforeEach
    void prepararReservaAprovadaDeHoje() {
        limparBanco();
        Quadra quadra = new Quadra();
        quadra.setNome("Quadra da portaria de teste");
        quadra.setDescricao("Dado sintético da suíte PostgreSQL");
        quadra.setAtiva(true);
        quadra = quadraRepository.saveAndFlush(quadra);

        Usuario aluno = novoUsuario("aluno.portaria@teste.local", Perfil.ALUNO);
        seguranca = novoUsuario("seguranca.portaria@teste.local", Perfil.SEGURANCA);
        secretaria = novoUsuario("secretaria.portaria@teste.local", Perfil.SECRETARIA);
        segundaSecretaria = novoUsuario("secretaria.dois.portaria@teste.local", Perfil.SECRETARIA);

        reserva = new Reserva();
        reserva.setQuadra(quadra);
        reserva.setSolicitante(aluno);
        reserva.setDataUtilizacao(LocalDate.now(CalendarioDisponibilidadeService.FUSO_HORARIO));
        reserva.setHorarioInicio(LocalTime.of(21, 0));
        reserva.setHorarioFim(LocalTime.of(22, 0));
        reserva.setFinalidade("Atividade sintética da portaria");
        reserva.setStatus(StatusReserva.APROVADA);
        reserva = reservaRepository.saveAndFlush(reserva);

        participantes = new ArrayList<>();
        for (int indice = 1; indice <= 10; indice++) {
            ParticipanteReserva participante = new ParticipanteReserva();
            participante.setReserva(reserva);
            participante.setNomeCompleto("Participante de teste " + indice);
            participante.setRmOuRg("PORTARIA-" + indice);
            participante.setCategoria(indice <= 8
                    ? CategoriaParticipante.ALUNO : CategoriaParticipante.OUTRO);
            if (indice > 8) participante.setVinculo(VinculoParticipante.PAI_OU_MAE);
            participante.registrarPresenca(PresencaParticipante.PRESENTE, seguranca,
                    java.time.Instant.now());
            participantes.add(participanteRepository.saveAndFlush(participante));
        }
    }

    @AfterEach
    void limparRegistros() {
        limparBanco();
    }

    @Test
    void substituicaoDeDuasAusenciasPorDuasPessoasCustaZero() {
        consultaSegurancaService.registrarPresencas(reserva.getId(), seguranca, java.util.Map.of(
                participantes.get(0).getId(), PresencaParticipante.AUSENTE,
                participantes.get(1).getId(), PresencaParticipante.AUSENTE));
        acrescentar("Substituto A", "PORTARIA-X1");
        acrescentar("Substituto B", "PORTARIA-X2");

        ResumoPortaria resumo = portariaService.resumir(reserva);

        assertEquals(10, resumo.presentes());
        assertEquals(0, resumo.pessoasAPagar());
        assertEquals(new BigDecimal("0.00"), resumo.valorAPagar());
    }

    @Test
    void duasTentativasSimultaneasDePagamentoRegistramUmUnicoComplemento() throws Exception {
        acrescentar("Pessoa adicional A", "PORTARIA-Y1");
        acrescentar("Pessoa adicional B", "PORTARIA-Y2");
        CountDownLatch prontas = new CountDownLatch(2);
        CountDownLatch iniciar = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> primeira = executor.submit(() -> tentarRegistrarPagamento(secretaria, prontas, iniciar));
            Future<Boolean> segunda = executor.submit(() -> tentarRegistrarPagamento(segundaSecretaria, prontas, iniciar));
            prontas.await();
            iniciar.countDown();

            assertTrue(primeira.get() ^ segunda.get(), "uma tentativa deve registrar e a outra ver saldo zero");
        } finally {
            executor.shutdownNow();
        }

        List<PagamentoAdicional> pagamentos = pagamentoRepository
                .findByReservaOrderByRegistradoEmAscIdAsc(reserva);
        assertEquals(1, pagamentos.size());
        assertEquals(new BigDecimal("4.00"), pagamentos.get(0).getValor());
        assertEquals(LocalPagamento.SECRETARIA, pagamentos.get(0).getLocal());
        assertEquals(0, portariaService.resumir(reserva).pessoasAPagar());
    }

    @Test
    void recebimentoNaPortariaAguardaERegistraRepasseDaSecretaria() {
        acrescentar("Pessoa adicional A", "PORTARIA-Z1");
        PagamentoAdicional pagamento = portariaService.registrarPagamentoNaPortaria(
                reserva.getId(), seguranca);

        assertEquals(new BigDecimal("2.00"), pagamento.getValor());
        assertEquals(seguranca.getId(), pagamento.getRegistradoPor().getId());
        assertEquals(1, pagamentoRepository.listarAguardandoRepasse(LocalPagamento.PORTARIA).size());

        portariaService.confirmarRepasse(pagamento.getId(), secretaria);

        PagamentoAdicional persistido = pagamentoRepository.findById(pagamento.getId()).orElseThrow();
        assertFalse(persistido.aguardaRepasse());
        assertEquals(secretaria.getId(), persistido.getRepasseConfirmadoPor().getId());
        assertNotNull(persistido.getRepasseConfirmadoEm());
        assertTrue(pagamentoRepository.listarAguardandoRepasse(LocalPagamento.PORTARIA).isEmpty());
    }

    @Test
    void duasConfirmacoesSimultaneasDoMesmoRepasseRegistramUmaSo() throws Exception {
        acrescentar("Pessoa adicional para repasse", "PORTARIA-R1");
        PagamentoAdicional pagamento = portariaService.registrarPagamentoNaPortaria(
                reserva.getId(), seguranca);
        CountDownLatch prontas = new CountDownLatch(2);
        CountDownLatch iniciar = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> primeira = executor.submit(
                    () -> tentarConfirmarRepasse(pagamento.getId(), secretaria, prontas, iniciar));
            Future<Boolean> segunda = executor.submit(
                    () -> tentarConfirmarRepasse(pagamento.getId(), segundaSecretaria, prontas, iniciar));
            prontas.await();
            iniciar.countDown();

            assertTrue(primeira.get() ^ segunda.get(),
                    "uma confirmação deve ser gravada; a outra deve ver repasse já confirmado");
        } finally {
            executor.shutdownNow();
        }

        PagamentoAdicional persistido = pagamentoRepository.findById(pagamento.getId()).orElseThrow();
        assertFalse(persistido.aguardaRepasse());
        assertNotNull(persistido.getRepasseConfirmadoPor());
        assertNotNull(persistido.getRepasseConfirmadoEm());
        assertTrue(pagamentoRepository.listarAguardandoRepasse(LocalPagamento.PORTARIA).isEmpty());
    }

    @Test
    void segurancaNaoPodeAlterarParticipantesDepoisDaLiberacao() {
        reserva.registrarLiberacao(seguranca, java.time.Instant.now());
        reservaRepository.saveAndFlush(reserva);

        assertThrows(IllegalStateException.class,
                () -> portariaService.acrescentarPessoa(reserva.getId(), seguranca,
                        "Pessoa tardia", "PORTARIA-TARDIA", CategoriaParticipante.OUTRO,
                        VinculoParticipante.PAI_OU_MAE));
        assertThrows(IllegalStateException.class,
                () -> consultaSegurancaService.registrarPresencas(reserva.getId(), seguranca,
                        java.util.Map.of(participantes.get(0).getId(), PresencaParticipante.AUSENTE)));
        assertEquals(10, participanteRepository.countByReserva(reserva));
        assertEquals(PresencaParticipante.PRESENTE,
                participanteRepository.findById(participantes.get(0).getId()).orElseThrow().getPresenca());
    }

    private void acrescentar(String nome, String identificador) {
        portariaService.acrescentarPessoa(reserva.getId(), seguranca, nome, identificador,
                CategoriaParticipante.OUTRO, VinculoParticipante.PAI_OU_MAE);
    }

    private boolean tentarRegistrarPagamento(
            Usuario operador, CountDownLatch prontas, CountDownLatch iniciar) throws Exception {
        prontas.countDown();
        iniciar.await();
        try {
            portariaService.registrarPagamentoNaSecretaria(reserva.getId(), operador);
            return true;
        } catch (IllegalStateException esperado) {
            return false;
        }
    }

    private boolean tentarConfirmarRepasse(
            Long pagamentoId, Usuario operador, CountDownLatch prontas, CountDownLatch iniciar)
            throws Exception {
        prontas.countDown();
        iniciar.await();
        try {
            portariaService.confirmarRepasse(pagamentoId, operador);
            return true;
        } catch (IllegalStateException esperado) {
            return false;
        }
    }

    private Usuario novoUsuario(String email, Perfil perfil) {
        Usuario usuario = new Usuario();
        usuario.setNome("Usuário sintético " + email);
        usuario.setEmail(email);
        usuario.setSenha("hash-sintetico");
        usuario.setPerfil(perfil);
        usuario.setAtivo(true);
        return usuarioRepository.saveAndFlush(usuario);
    }

    private void limparBanco() {
        BancoDeTeste.exigirBancoDedicado(dataSource);
        pagamentoRepository.deleteAllInBatch();
        ajusteRepository.deleteAllInBatch();
        contribuicaoRepository.deleteAllInBatch();
        conferenciaRepository.deleteAllInBatch();
        documentoRepository.deleteAllInBatch();
        participanteRepository.deleteAllInBatch();
        reservaRepository.deleteAllInBatch();
        jdbcTemplate.execute("delete from bloqueios_quadra");
        jdbcTemplate.execute("delete from disponibilidades_quadra");
        usuarioRepository.deleteAllInBatch();
        quadraRepository.deleteAllInBatch();
    }
}
