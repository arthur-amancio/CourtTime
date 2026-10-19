package br.com.courttime.service;

import br.com.courttime.entity.AuditoriaUsuario;
import br.com.courttime.entity.OperacaoAuditoriaUsuario;
import br.com.courttime.entity.Perfil;
import br.com.courttime.entity.Quadra;
import br.com.courttime.entity.Reserva;
import br.com.courttime.entity.StatusReserva;
import br.com.courttime.entity.Usuario;
import br.com.courttime.repository.AuditoriaUsuarioRepository;
import br.com.courttime.repository.AjusteContribuicaoApmRepository;
import br.com.courttime.repository.BloqueioQuadraRepository;
import br.com.courttime.repository.ConferenciaDocumentoRepository;
import br.com.courttime.repository.ContribuicaoApmRepository;
import br.com.courttime.repository.DisponibilidadeQuadraRepository;
import br.com.courttime.repository.DocumentoReservaRepository;
import br.com.courttime.repository.ParticipanteReservaRepository;
import br.com.courttime.repository.QuadraRepository;
import br.com.courttime.repository.ReservaRepository;
import br.com.courttime.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "COURTTIME_POSTGRES_TEST", matches = "true")
class AdministracaoUsuarioPostgreSqlIntegrationTest {

    @Autowired
    private AdministracaoUsuarioService administracaoUsuarioService;
    @Autowired
    private UsuarioDetailsService usuarioDetailsService;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private AuditoriaUsuarioRepository auditoriaRepository;
    @Autowired
    private ReservaRepository reservaRepository;
    @Autowired
    private AjusteContribuicaoApmRepository ajusteContribuicaoRepository;
    @Autowired
    private ContribuicaoApmRepository contribuicaoRepository;
    @Autowired
    private ConferenciaDocumentoRepository conferenciaDocumentoRepository;
    @Autowired
    private DocumentoReservaRepository documentoReservaRepository;
    @Autowired
    private ParticipanteReservaRepository participanteReservaRepository;
    @Autowired
    private BloqueioQuadraRepository bloqueioRepository;
    @Autowired
    private DisponibilidadeQuadraRepository disponibilidadeRepository;
    @Autowired
    private QuadraRepository quadraRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Usuario administrador;
    private Quadra quadra;

    @BeforeEach
    void prepararBancoIsolado() {
        ajusteContribuicaoRepository.deleteAllInBatch();
        contribuicaoRepository.deleteAllInBatch();
        conferenciaDocumentoRepository.deleteAllInBatch();
        documentoReservaRepository.deleteAllInBatch();
        participanteReservaRepository.deleteAllInBatch();
        auditoriaRepository.deleteAllInBatch();
        reservaRepository.deleteAllInBatch();
        bloqueioRepository.deleteAllInBatch();
        disponibilidadeRepository.deleteAllInBatch();
        usuarioRepository.deleteAllInBatch();
        quadraRepository.deleteAllInBatch();

        administrador = novoUsuario(
                "Administrador de integração",
                "admin.integracao@teste.local",
                Perfil.ADMINISTRADOR,
                "senha-admin");
        quadra = new Quadra();
        quadra.setNome("Quadra de integração");
        quadra.setDescricao("Registro criado somente no PostgreSQL temporário de testes");
        quadra.setAtiva(true);
        quadra = quadraRepository.saveAndFlush(quadra);
    }

    @Test
    void schemaPossuiVersaoDeAcessoEAuditoriaSemColunasDeSenha() {
        List<String> colunasUsuario = jdbcTemplate.queryForList("""
                select column_name
                from information_schema.columns
                where table_schema = current_schema() and table_name = 'usuarios'
                """, String.class);
        List<String> colunasAuditoria = jdbcTemplate.queryForList("""
                select column_name
                from information_schema.columns
                where table_schema = current_schema() and table_name = 'auditorias_usuario'
                """, String.class);

        assertTrue(colunasUsuario.contains("versao_acesso"));
        assertTrue(colunasAuditoria.contains("operador_id"));
        assertTrue(colunasAuditoria.contains("usuario_afetado_id"));
        assertFalse(colunasAuditoria.stream().anyMatch(nome ->
                nome.contains("senha") || nome.contains("hash") || nome.contains("token")));
    }

    @Test
    void cadastroPersisteBCryptEAContaAutenticaComPerfilCorreto() {
        UsuarioResumo criado = administracaoUsuarioService.cadastrar(
                administrador,
                "  Ana   Souza  ",
                "  ANA@ESCOLA.TEST ",
                Perfil.ALUNO,
                "senha-segura");

        Usuario recuperado = usuarioRepository.findById(criado.id()).orElseThrow();
        var detalhes = usuarioDetailsService.loadUserByUsername("ana@escola.test");

        assertEquals("Ana Souza", recuperado.getNome());
        assertEquals("ana@escola.test", recuperado.getEmail());
        assertNotEquals("senha-segura", recuperado.getSenha());
        assertTrue(passwordEncoder.matches("senha-segura", recuperado.getSenha()));
        assertTrue(detalhes.getAuthorities().stream().anyMatch(
                autoridade -> autoridade.getAuthority().equals("ROLE_ALUNO")));
        assertEquals(1L, auditoriaRepository.count());
    }

    @Test
    void emailDuplicadoCaseInsensitiveERecusado() {
        administracaoUsuarioService.cadastrar(
                administrador,
                "Ana Souza",
                "ana@escola.test",
                Perfil.ALUNO,
                "senha-segura");

        assertThrows(IllegalStateException.class, () -> administracaoUsuarioService.cadastrar(
                administrador,
                "Outra pessoa",
                "ANA@ESCOLA.TEST",
                Perfil.SECRETARIA,
                "outra-senha"));

        assertEquals(2L, usuarioRepository.count());
    }

    @Test
    void buscaFiltrosEPaginacaoConsultamUsuariosPersistidos() {
        administracaoUsuarioService.cadastrar(
                administrador, "Ana Souza", "ana@escola.test", Perfil.ALUNO, "senha-segura");
        administracaoUsuarioService.cadastrar(
                administrador, "Anabela Lima", "anabela@escola.test",
                Perfil.SECRETARIA, "senha-segura");
        administracaoUsuarioService.cadastrar(
                administrador, "Carlos Pereira", "carlos@escola.test",
                Perfil.SEGURANCA, "senha-segura");

        Page<UsuarioResumo> filtrados = administracaoUsuarioService.listar(
                administrador, "ANA", Perfil.ALUNO, true, 0);
        Page<Usuario> primeiraPagina = usuarioRepository.buscar(
                "",
                null,
                true,
                PageRequest.of(0, 2, Sort.by("nome")));

        assertEquals(1L, filtrados.getTotalElements());
        assertEquals("ana@escola.test", filtrados.getContent().get(0).email());
        assertEquals(4L, primeiraPagina.getTotalElements());
        assertEquals(2, primeiraPagina.getTotalPages());
        assertEquals(2, primeiraPagina.getContent().size());
    }

    @Test
    void edicaoERedefinicaoPersistemEAuditoriaNaoGuardaCredencial() {
        UsuarioResumo criado = administracaoUsuarioService.cadastrar(
                administrador,
                "Ana Souza",
                "ana@escola.test",
                Perfil.ALUNO,
                "senha-antiga");

        administracaoUsuarioService.editar(
                criado.id(),
                administrador,
                "Ana Pereira",
                "ana.pereira@escola.test",
                Perfil.SECRETARIA);
        administracaoUsuarioService.redefinirSenha(
                criado.id(), administrador, "senha-nova-segura");

        Usuario recuperado = usuarioRepository.findById(criado.id()).orElseThrow();
        List<AuditoriaUsuario> auditorias = auditoriaRepository.findAll();

        assertEquals("Ana Pereira", recuperado.getNome());
        assertEquals(Perfil.SECRETARIA, recuperado.getPerfil());
        assertEquals(2L, recuperado.getVersaoAcesso());
        assertTrue(passwordEncoder.matches("senha-nova-segura", recuperado.getSenha()));
        assertEquals(3, auditorias.size());
        assertTrue(auditorias.stream().anyMatch(item ->
                item.getOperacao() == OperacaoAuditoriaUsuario.REDEFINICAO_SENHA));
    }

    @Test
    void desativacaoPreservaReservaHistoricaEBloqueiaNovaAutenticacao() {
        UsuarioResumo criado = administracaoUsuarioService.cadastrar(
                administrador,
                "Ana Souza",
                "ana@escola.test",
                Perfil.ALUNO,
                "senha-segura");
        Usuario aluno = usuarioRepository.findById(criado.id()).orElseThrow();
        Reserva reserva = new Reserva();
        reserva.setQuadra(quadra);
        reserva.setSolicitante(aluno);
        reserva.setDataUtilizacao(LocalDate.now().plusDays(7));
        reserva.setHorarioInicio(LocalTime.of(8, 0));
        reserva.setHorarioFim(LocalTime.of(9, 0));
        reserva.setFinalidade("Atividade de integração");
        reserva.setStatus(StatusReserva.PENDENTE_DOCUMENTACAO);
        reserva = reservaRepository.saveAndFlush(reserva);

        administracaoUsuarioService.alternarEstado(aluno.getId(), administrador);

        Usuario desativado = usuarioRepository.findById(aluno.getId()).orElseThrow();
        assertFalse(desativado.isAtivo());
        Long solicitantePreservado = jdbcTemplate.queryForObject(
                "select solicitante_id from reservas where id = ?",
                Long.class,
                reserva.getId());
        assertEquals(aluno.getId(), solicitantePreservado);
        assertThrows(UsernameNotFoundException.class,
                () -> usuarioDetailsService.loadUserByUsername(aluno.getEmail()));
        assertTrue(auditoriaRepository.findAll().stream().anyMatch(item ->
                item.getOperacao() == OperacaoAuditoriaUsuario.DESATIVACAO));
    }

    private Usuario novoUsuario(
            String nome, String email, Perfil perfil, String senha) {
        Usuario usuario = new Usuario();
        usuario.setNome(nome);
        usuario.setEmail(email);
        usuario.setSenha(passwordEncoder.encode(senha));
        usuario.setPerfil(perfil);
        usuario.setAtivo(true);
        return usuarioRepository.saveAndFlush(usuario);
    }
}
