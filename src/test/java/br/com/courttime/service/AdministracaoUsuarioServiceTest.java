package br.com.courttime.service;

import br.com.courttime.entity.AuditoriaUsuario;
import br.com.courttime.entity.OperacaoAuditoriaUsuario;
import br.com.courttime.entity.Perfil;
import br.com.courttime.entity.Usuario;
import br.com.courttime.repository.AuditoriaUsuarioRepository;
import br.com.courttime.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdministracaoUsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private AuditoriaUsuarioRepository auditoriaRepository;

    private AdministracaoUsuarioService service;
    private BCryptPasswordEncoder passwordEncoder;
    private Usuario administrador;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        service = new AdministracaoUsuarioService(
                usuarioRepository, auditoriaRepository, passwordEncoder);
        administrador = usuario(1L, "admin@escola.test", Perfil.ADMINISTRADOR, true);
    }

    @Test
    void cadastraUsuarioComEmailNormalizadoESenhaBCrypt() {
        prepararOperador();
        when(usuarioRepository.existsByEmailIgnoreCase("aluno@escola.test")).thenReturn(false);
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario usuario = invocation.getArgument(0);
            usuario.setId(20L);
            return usuario;
        });

        UsuarioResumo criado = service.cadastrar(
                administrador,
                "  Ana   Souza  ",
                "  ALUNO@ESCOLA.TEST ",
                Perfil.ALUNO,
                "senha-segura");

        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).saveAndFlush(usuarioCaptor.capture());
        Usuario persistido = usuarioCaptor.getValue();
        assertEquals("Ana Souza", criado.nome());
        assertEquals("aluno@escola.test", criado.email());
        assertNotEquals("senha-segura", persistido.getSenha());
        assertTrue(passwordEncoder.matches("senha-segura", persistido.getSenha()));

        ArgumentCaptor<AuditoriaUsuario> auditoriaCaptor =
                ArgumentCaptor.forClass(AuditoriaUsuario.class);
        verify(auditoriaRepository).save(auditoriaCaptor.capture());
        assertEquals(OperacaoAuditoriaUsuario.CADASTRO,
                auditoriaCaptor.getValue().getOperacao());
    }

    @Test
    void emailDuplicadoERecusadoSemGravarUsuario() {
        prepararOperador();
        when(usuarioRepository.existsByEmailIgnoreCase("aluno@escola.test")).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> service.cadastrar(
                administrador,
                "Ana Souza",
                "aluno@escola.test",
                Perfil.ALUNO,
                "senha-segura"));

        verify(usuarioRepository, never()).saveAndFlush(any());
        verify(auditoriaRepository, never()).save(any());
    }

    @Test
    void conflitoConcorrenteNoIndiceUnicoETraduzidoSemAuditoriaIncorreta() {
        prepararOperador();
        when(usuarioRepository.existsByEmailIgnoreCase("aluno@escola.test"))
                .thenReturn(false);
        when(usuarioRepository.saveAndFlush(any(Usuario.class)))
                .thenThrow(new DataIntegrityViolationException("email único"));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.cadastrar(
                        administrador,
                        "Ana Souza",
                        "aluno@escola.test",
                        Perfil.ALUNO,
                        "senha-segura"));

        assertTrue(exception.getMessage().contains("Já existe uma conta"));
        verify(auditoriaRepository, never()).save(any());
    }

    @Test
    void formularioComumNaoCriaAdministrador() {
        prepararOperador();

        assertThrows(AccessDeniedException.class, () -> service.cadastrar(
                administrador,
                "Outro administrador",
                "outro.admin@escola.test",
                Perfil.ADMINISTRADOR,
                "senha-segura"));

        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void edicaoValidaAlteraPerfilEInvalidaSessoesAntigas() {
        prepararOperador();
        Usuario secretaria = usuario(20L, "secretaria@escola.test", Perfil.SECRETARIA, true);
        when(usuarioRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(secretaria));
        when(usuarioRepository.existsByEmailIgnoreCaseAndIdNot(
                "atendimento@escola.test", 20L)).thenReturn(false);
        when(usuarioRepository.saveAndFlush(secretaria)).thenReturn(secretaria);

        UsuarioResumo editado = service.editar(
                20L,
                administrador,
                "Atendimento",
                "ATENDIMENTO@ESCOLA.TEST",
                Perfil.SEGURANCA);

        assertEquals(Perfil.SEGURANCA, editado.perfil());
        assertEquals(1L, secretaria.getVersaoAcesso());
        verify(auditoriaRepository).save(any(AuditoriaUsuario.class));
    }

    @Test
    void administradorNaoPodeAlterarOProprioPerfil() {
        prepararOperador();
        when(usuarioRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(administrador));

        assertThrows(AccessDeniedException.class, () -> service.editar(
                1L,
                administrador,
                "Administrador",
                "admin@escola.test",
                Perfil.ALUNO));

        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void perfilDeAdministradorNaoPodeSerRebaixadoPorOutroOperador() {
        Usuario segundoAdmin = usuario(2L, "segundo@escola.test", Perfil.ADMINISTRADOR, true);
        when(usuarioRepository.findAtivosPorPerfilForUpdate(Perfil.ADMINISTRADOR))
                .thenReturn(List.of(administrador, segundoAdmin));
        when(usuarioRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(administrador));

        assertThrows(AccessDeniedException.class, () -> service.editar(
                1L,
                segundoAdmin,
                "Administrador",
                "admin@escola.test",
                Perfil.ALUNO));

        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void edicaoComEmailInvalidoERecusada() {
        prepararOperador();
        Usuario aluno = usuario(20L, "aluno@escola.test", Perfil.ALUNO, true);
        when(usuarioRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(aluno));

        assertThrows(IllegalArgumentException.class, () -> service.editar(
                20L,
                administrador,
                "Aluno",
                "email-invalido",
                Perfil.ALUNO));

        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void administradorNaoPodeDesativarAPropriaConta() {
        Usuario segundoAdmin = usuario(2L, "segundo@escola.test", Perfil.ADMINISTRADOR, true);
        when(usuarioRepository.findAtivosPorPerfilForUpdate(Perfil.ADMINISTRADOR))
                .thenReturn(List.of(administrador, segundoAdmin));
        when(usuarioRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(administrador));

        assertThrows(IllegalStateException.class,
                () -> service.alternarEstado(1L, administrador));

        assertTrue(administrador.isAtivo());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void ultimoAdministradorAtivoNaoPodeSerDesativado() {
        when(usuarioRepository.findAtivosPorPerfilForUpdate(Perfil.ADMINISTRADOR))
                .thenReturn(List.of(administrador));
        when(usuarioRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(administrador));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.alternarEstado(1L, administrador));
        assertTrue(exception.getMessage().contains("último administrador"));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void administradorDesativaOutraContaSemExcluirRegistro() {
        prepararOperador();
        Usuario aluno = usuario(20L, "aluno@escola.test", Perfil.ALUNO, true);
        when(usuarioRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(aluno));
        when(usuarioRepository.save(aluno)).thenReturn(aluno);

        UsuarioResumo resultado = service.alternarEstado(20L, administrador);

        assertFalse(resultado.ativo());
        assertEquals(1L, aluno.getVersaoAcesso());
        verify(usuarioRepository).save(aluno);
        verify(usuarioRepository, never()).delete(any());
        verify(auditoriaRepository).save(any(AuditoriaUsuario.class));
    }

    @Test
    void redefinicaoDeSenhaUsaBCryptEInvalidaSessaoAnterior() {
        prepararOperador();
        Usuario aluno = usuario(20L, "aluno@escola.test", Perfil.ALUNO, true);
        aluno.setSenha(passwordEncoder.encode("senha-antiga"));
        when(usuarioRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(aluno));

        service.redefinirSenha(20L, administrador, "senha-nova-segura");

        assertTrue(passwordEncoder.matches("senha-nova-segura", aluno.getSenha()));
        assertFalse(passwordEncoder.matches("senha-antiga", aluno.getSenha()));
        assertEquals(1L, aluno.getVersaoAcesso());
        verify(auditoriaRepository).save(any(AuditoriaUsuario.class));
    }

    @Test
    void usuarioInativoOuNaoAdministradorNaoPodeGerenciarContas() {
        Usuario secretaria = usuario(30L, "secretaria@escola.test", Perfil.SECRETARIA, true);
        when(usuarioRepository.findAtivosPorPerfilForUpdate(Perfil.ADMINISTRADOR))
                .thenReturn(List.of(administrador));

        assertThrows(AccessDeniedException.class, () -> service.cadastrar(
                secretaria,
                "Aluno",
                "aluno@escola.test",
                Perfil.ALUNO,
                "senha-segura"));
    }

    private void prepararOperador() {
        when(usuarioRepository.findAtivosPorPerfilForUpdate(Perfil.ADMINISTRADOR))
                .thenReturn(List.of(administrador));
    }

    private Usuario usuario(Long id, String email, Perfil perfil, boolean ativo) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNome("Usuário");
        usuario.setEmail(email);
        usuario.setSenha("hash");
        usuario.setPerfil(perfil);
        usuario.setAtivo(ativo);
        return usuario;
    }
}
