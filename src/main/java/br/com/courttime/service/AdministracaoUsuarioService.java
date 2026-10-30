package br.com.courttime.service;

import br.com.courttime.entity.AuditoriaUsuario;
import br.com.courttime.entity.OperacaoAuditoriaUsuario;
import br.com.courttime.entity.Perfil;
import br.com.courttime.entity.Usuario;
import br.com.courttime.repository.AuditoriaUsuarioRepository;
import br.com.courttime.repository.UsuarioRepository;
import br.com.courttime.security.Permissao;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class AdministracaoUsuarioService {

    private static final int TAMANHO_PAGINA = 10;
    private static final int TAMANHO_MINIMO_SENHA = 8;
    private static final int TAMANHO_MAXIMO_SENHA = 72;
    private static final Set<Perfil> PERFIS_CADASTRAVEIS = Set.of(
            Perfil.ALUNO, Perfil.SECRETARIA, Perfil.SEGURANCA);
    private static final Pattern EMAIL_VALIDO = Pattern.compile(
            "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final UsuarioRepository usuarioRepository;
    private final AuditoriaUsuarioRepository auditoriaRepository;
    private final PasswordEncoder passwordEncoder;

    public AdministracaoUsuarioService(
            UsuarioRepository usuarioRepository,
            AuditoriaUsuarioRepository auditoriaRepository,
            PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.auditoriaRepository = auditoriaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public Page<UsuarioResumo> listar(
            Usuario administrador,
            String termo,
            Perfil perfil,
            Boolean ativo,
            int pagina) {
        validarAdministradorAtual(administrador);
        String termoNormalizado = termo == null ? "" : termo.trim();
        int paginaSegura = Math.max(pagina, 0);
        return usuarioRepository.buscar(
                        termoNormalizado,
                        perfil,
                        ativo,
                        PageRequest.of(
                                paginaSegura,
                                TAMANHO_PAGINA,
                                Sort.by(Sort.Direction.ASC, "nome", "id")))
                .map(this::resumir);
    }

    @Transactional(readOnly = true)
    public UsuarioResumo obterParaEdicao(Long id, Usuario administrador) {
        validarAdministradorAtual(administrador);
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
        return resumir(usuario);
    }

    @Transactional
    public UsuarioResumo cadastrar(
            Usuario administrador,
            String nome,
            String email,
            Perfil perfil,
            String senhaInicial) {
        Usuario operador = obterAdministradorBloqueado(administrador);
        validarPerfilCadastravel(perfil);
        String nomeNormalizado = validarNome(nome);
        String emailNormalizado = validarEmail(email);
        validarSenha(senhaInicial);
        if (usuarioRepository.existsByEmailIgnoreCase(emailNormalizado)) {
            throw new IllegalStateException("Já existe uma conta cadastrada com esse email");
        }

        Usuario usuario = new Usuario();
        usuario.setNome(nomeNormalizado);
        usuario.setEmail(emailNormalizado);
        usuario.setPerfil(perfil);
        usuario.setSenha(passwordEncoder.encode(senhaInicial));
        usuario.setAtivo(true);

        try {
            usuario = usuarioRepository.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException exception) {
            throw new IllegalStateException(
                    "Já existe uma conta cadastrada com esse email", exception);
        }
        registrarAuditoria(
                operador,
                usuario,
                OperacaoAuditoriaUsuario.CADASTRO,
                null,
                perfil,
                null,
                true);
        return resumir(usuario);
    }

    @Transactional
    public UsuarioResumo editar(
            Long id,
            Usuario administrador,
            String nome,
            String email,
            Perfil perfil) {
        Usuario operador = obterAdministradorBloqueado(administrador);
        Usuario usuario = obterUsuarioBloqueado(id);
        String nomeNormalizado = validarNome(nome);
        String emailNormalizado = validarEmail(email);
        validarAlteracaoDePerfil(usuario, operador, perfil);
        if (usuarioRepository.existsByEmailIgnoreCaseAndIdNot(emailNormalizado, usuario.getId())) {
            throw new IllegalStateException("Já existe uma conta cadastrada com esse email");
        }

        Perfil perfilAnterior = usuario.getPerfil();
        boolean credencialAlterada = !usuario.getEmail().equals(emailNormalizado)
                || usuario.getPerfil() != perfil;
        usuario.setNome(nomeNormalizado);
        usuario.setEmail(emailNormalizado);
        usuario.setPerfil(perfil);
        if (credencialAlterada) {
            usuario.incrementarVersaoAcesso();
        }

        try {
            usuario = usuarioRepository.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException exception) {
            throw new IllegalStateException(
                    "Já existe uma conta cadastrada com esse email", exception);
        }
        registrarAuditoria(
                operador,
                usuario,
                OperacaoAuditoriaUsuario.EDICAO,
                perfilAnterior,
                perfil,
                usuario.isAtivo(),
                usuario.isAtivo());
        return resumir(usuario);
    }

    @Transactional
    public UsuarioResumo alternarEstado(Long id, Usuario administrador) {
        List<Usuario> administradoresAtivos = bloquearAdministradoresAtivos();
        Usuario operador = encontrarOperador(administrador, administradoresAtivos);
        Usuario usuario = obterUsuarioBloqueado(id);
        boolean novoEstado = !usuario.isAtivo();

        if (!novoEstado
                && usuario.getPerfil() == Perfil.ADMINISTRADOR
                && administradoresAtivos.size() <= 1) {
            throw new IllegalStateException("O último administrador ativo não pode ser desativado");
        }
        if (!novoEstado && Objects.equals(usuario.getId(), operador.getId())) {
            throw new IllegalStateException("O administrador não pode desativar a própria conta");
        }

        boolean estadoAnterior = usuario.isAtivo();
        usuario.setAtivo(novoEstado);
        usuario.incrementarVersaoAcesso();
        usuario = usuarioRepository.save(usuario);
        registrarAuditoria(
                operador,
                usuario,
                novoEstado
                        ? OperacaoAuditoriaUsuario.ATIVACAO
                        : OperacaoAuditoriaUsuario.DESATIVACAO,
                usuario.getPerfil(),
                usuario.getPerfil(),
                estadoAnterior,
                novoEstado);
        return resumir(usuario);
    }

    @Transactional
    public void redefinirSenha(Long id, Usuario administrador, String novaSenha) {
        Usuario operador = obterAdministradorBloqueado(administrador);
        Usuario usuario = obterUsuarioBloqueado(id);
        validarSenha(novaSenha);
        usuario.setSenha(passwordEncoder.encode(novaSenha));
        usuario.incrementarVersaoAcesso();
        usuarioRepository.save(usuario);
        registrarAuditoria(
                operador,
                usuario,
                OperacaoAuditoriaUsuario.REDEFINICAO_SENHA,
                usuario.getPerfil(),
                usuario.getPerfil(),
                usuario.isAtivo(),
                usuario.isAtivo());
    }

    public List<Perfil> perfisCadastraveis() {
        return List.of(Perfil.ALUNO, Perfil.SECRETARIA, Perfil.SEGURANCA);
    }

    private Usuario obterAdministradorBloqueado(Usuario informado) {
        return encontrarOperador(informado, bloquearAdministradoresAtivos());
    }

    private List<Usuario> bloquearAdministradoresAtivos() {
        return usuarioRepository.findAtivosPorPerfilForUpdate(Perfil.ADMINISTRADOR);
    }

    private Usuario encontrarOperador(
            Usuario informado, List<Usuario> administradoresAtivos) {
        if (informado == null || informado.getId() == null) {
            throw new AccessDeniedException("Usuário sem permissão para gerenciar contas");
        }
        return administradoresAtivos.stream()
                .filter(usuario -> Objects.equals(usuario.getId(), informado.getId()))
                .findFirst()
                .orElseThrow(() -> new AccessDeniedException(
                        "Usuário sem permissão para gerenciar contas"));
    }

    private void validarAdministradorAtual(Usuario informado) {
        if (informado == null || informado.getId() == null) {
            throw new AccessDeniedException("Usuário sem permissão para gerenciar contas");
        }
        usuarioRepository.findById(informado.getId())
                .filter(usuario -> Permissao.temPerfil(usuario, Perfil.ADMINISTRADOR))
                .orElseThrow(() -> new AccessDeniedException(
                        "Usuário sem permissão para gerenciar contas"));
    }

    private Usuario obterUsuarioBloqueado(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("O usuário é obrigatório");
        }
        return usuarioRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
    }

    private void validarPerfilCadastravel(Perfil perfil) {
        if (!PERFIS_CADASTRAVEIS.contains(perfil)) {
            throw new AccessDeniedException(
                    "Contas administrativas não podem ser criadas pelo formulário comum");
        }
    }

    private void validarAlteracaoDePerfil(
            Usuario usuario, Usuario operador, Perfil novoPerfil) {
        if (novoPerfil == null) {
            throw new IllegalArgumentException("O perfil é obrigatório");
        }
        if (usuario.getPerfil() == Perfil.ADMINISTRADOR) {
            if (novoPerfil != Perfil.ADMINISTRADOR) {
                if (Objects.equals(usuario.getId(), operador.getId())) {
                    throw new AccessDeniedException(
                            "O administrador não pode alterar o próprio perfil");
                }
                throw new AccessDeniedException(
                        "O perfil de uma conta administrativa não pode ser alterado por esta tela");
            }
            return;
        }
        validarPerfilCadastravel(novoPerfil);
    }

    private String validarNome(String nome) {
        String normalizado = nome == null ? "" : nome.trim().replaceAll("\\s+", " ");
        if (normalizado.isEmpty() || normalizado.length() > 150) {
            throw new IllegalArgumentException(
                    "O nome é obrigatório e deve possuir no máximo 150 caracteres");
        }
        return normalizado;
    }

    private String validarEmail(String email) {
        String normalizado = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (normalizado.length() > 254 || !EMAIL_VALIDO.matcher(normalizado).matches()) {
            throw new IllegalArgumentException("Informe um email válido");
        }
        return normalizado;
    }

    private void validarSenha(String senha) {
        if (senha == null
                || senha.length() < TAMANHO_MINIMO_SENHA
                || senha.length() > TAMANHO_MAXIMO_SENHA) {
            throw new IllegalArgumentException(
                    "A senha deve possuir entre 8 e 72 caracteres");
        }
    }

    private void registrarAuditoria(
            Usuario operador,
            Usuario usuarioAfetado,
            OperacaoAuditoriaUsuario operacao,
            Perfil perfilAnterior,
            Perfil perfilNovo,
            Boolean ativoAnterior,
            Boolean ativoNovo) {
        AuditoriaUsuario auditoria = new AuditoriaUsuario();
        auditoria.setOperador(operador);
        auditoria.setUsuarioAfetado(usuarioAfetado);
        auditoria.setOperacao(operacao);
        auditoria.setPerfilAnterior(perfilAnterior);
        auditoria.setPerfilNovo(perfilNovo);
        auditoria.setAtivoAnterior(ativoAnterior);
        auditoria.setAtivoNovo(ativoNovo);
        auditoriaRepository.save(auditoria);
    }

    private UsuarioResumo resumir(Usuario usuario) {
        return new UsuarioResumo(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getPerfil(),
                usuario.isAtivo());
    }

}
