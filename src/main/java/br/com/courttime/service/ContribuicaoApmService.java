package br.com.courttime.service;

import br.com.courttime.entity.AjusteContribuicaoApm;
import br.com.courttime.entity.ConferenciaDocumento;
import br.com.courttime.entity.ContribuicaoApm;
import br.com.courttime.entity.DocumentoReserva;
import br.com.courttime.entity.Perfil;
import br.com.courttime.entity.Reserva;
import br.com.courttime.entity.ResultadoConferenciaDocumento;
import br.com.courttime.entity.StatusReserva;
import br.com.courttime.entity.Usuario;
import br.com.courttime.repository.AjusteContribuicaoApmRepository;
import br.com.courttime.repository.ContribuicaoApmRepository;
import br.com.courttime.repository.DocumentoReservaRepository;
import br.com.courttime.repository.ReservaRepository;
import br.com.courttime.repository.UsuarioRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Service
public class ContribuicaoApmService {

    private static final BigDecimal VALOR_MAXIMO = new BigDecimal("9999999999.99");
    private static final int TAMANHO_MAXIMO_TEXTO = 500;

    private final ContribuicaoApmRepository contribuicaoRepository;
    private final AjusteContribuicaoApmRepository ajusteRepository;
    private final DocumentoReservaRepository documentoRepository;
    private final ReservaRepository reservaRepository;
    private final ParticipanteReservaService participanteService;
    private final UsuarioRepository usuarioRepository;

    public ContribuicaoApmService(
            ContribuicaoApmRepository contribuicaoRepository,
            AjusteContribuicaoApmRepository ajusteRepository,
            DocumentoReservaRepository documentoRepository,
            ReservaRepository reservaRepository,
            ParticipanteReservaService participanteService,
            UsuarioRepository usuarioRepository) {
        this.contribuicaoRepository = contribuicaoRepository;
        this.ajusteRepository = ajusteRepository;
        this.documentoRepository = documentoRepository;
        this.reservaRepository = reservaRepository;
        this.participanteService = participanteService;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<Reserva> listarPendentes(Usuario secretaria) {
        obterSecretariaAtual(secretaria);
        return contribuicaoRepository.listarPendentes(
                StatusReserva.PENDENTE_DOCUMENTACAO,
                ResultadoConferenciaDocumento.VALIDADO);
    }

    @Transactional(readOnly = true)
    public List<ContribuicaoApm> listarRegistradas(Usuario secretaria) {
        obterSecretariaAtual(secretaria);
        return contribuicaoRepository.listarRegistradas();
    }

    @Transactional(readOnly = true)
    public DetalheContribuicaoApm obterPorReserva(Long reservaId, Usuario secretaria) {
        obterSecretariaAtual(secretaria);
        validarId(reservaId, "A solicitação é obrigatória");
        ContribuicaoApm contribuicao = contribuicaoRepository.obterPorReservaId(reservaId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Contribuição não encontrada para a solicitação"));
        return detalhar(contribuicao);
    }

    @Transactional
    public DetalheContribuicaoApm registrar(
            Long reservaId,
            Usuario secretaria,
            String valor,
            String observacoes) {
        Usuario operadora = obterSecretariaAtual(secretaria);
        validarId(reservaId, "A solicitação é obrigatória");
        BigDecimal valorValidado = validarValor(valor);
        String observacoesNormalizadas = normalizarTextoOpcional(
                observacoes, "As observações devem possuir no máximo 500 caracteres");

        Reserva reserva = reservaRepository.findByIdForUpdate(reservaId)
                .orElseThrow(() -> new IllegalArgumentException("Solicitação não encontrada"));
        if (reserva.getStatus() != StatusReserva.PENDENTE_DOCUMENTACAO) {
            throw new IllegalStateException(
                    "A contribuição só pode ser registrada enquanto a documentação está pendente");
        }
        DocumentoReserva documento = documentoRepository
                .obterVersaoMaisRecenteComConferencia(reserva)
                .orElseThrow(() -> new IllegalStateException(
                        "A versão mais recente do documento ainda não foi validada"));
        ConferenciaDocumento conferencia = documento.getConferencia();
        if (conferencia == null
                || conferencia.getResultado() != ResultadoConferenciaDocumento.VALIDADO) {
            throw new IllegalStateException(
                    "A versão mais recente do documento ainda não foi validada");
        }
        participanteService.validarDocumentoAtual(reserva, documento);
        if (contribuicaoRepository.existsByReserva(reserva)) {
            throw new IllegalStateException(
                    "Esta solicitação já possui uma contribuição registrada");
        }

        ContribuicaoApm contribuicao = new ContribuicaoApm();
        contribuicao.setReserva(reserva);
        contribuicao.setConferenciaDocumento(conferencia);
        contribuicao.setValor(valorValidado);
        contribuicao.setObservacoes(observacoesNormalizadas);
        contribuicao.setRegistradaPor(operadora);
        contribuicao = contribuicaoRepository.saveAndFlush(contribuicao);
        reserva.setStatus(StatusReserva.PENDENTE_ANALISE);
        reservaRepository.save(reserva);
        return new DetalheContribuicaoApm(contribuicao, List.of());
    }

    @Transactional
    public DetalheContribuicaoApm ajustar(
            Long contribuicaoId,
            Usuario secretaria,
            String novoValor,
            String novasObservacoes,
            String motivo) {
        Usuario operadora = obterSecretariaAtual(secretaria);
        validarId(contribuicaoId, "A contribuição é obrigatória");
        BigDecimal valorValidado = validarValor(novoValor);
        String observacoesNormalizadas = normalizarTextoOpcional(
                novasObservacoes,
                "As observações devem possuir no máximo 500 caracteres");
        String motivoNormalizado = normalizarTextoObrigatorio(
                motivo,
                "O motivo do ajuste é obrigatório e deve possuir no máximo 500 caracteres");

        ContribuicaoApm contribuicao = contribuicaoRepository.findByIdForUpdate(contribuicaoId)
                .orElseThrow(() -> new IllegalArgumentException("Contribuição não encontrada"));
        if (contribuicao.getValor().compareTo(valorValidado) == 0
                && Objects.equals(contribuicao.getObservacoes(), observacoesNormalizadas)) {
            throw new IllegalArgumentException(
                    "Informe um novo valor ou novas observações para registrar o ajuste");
        }

        AjusteContribuicaoApm ajuste = new AjusteContribuicaoApm();
        ajuste.setContribuicao(contribuicao);
        ajuste.setValorAnterior(contribuicao.getValor());
        ajuste.setValorNovo(valorValidado);
        ajuste.setObservacoesAnteriores(contribuicao.getObservacoes());
        ajuste.setObservacoesNovas(observacoesNormalizadas);
        ajuste.setMotivo(motivoNormalizado);
        ajuste.setAjustadaPor(operadora);
        ajusteRepository.save(ajuste);

        contribuicao.setValor(valorValidado);
        contribuicao.setObservacoes(observacoesNormalizadas);
        contribuicaoRepository.saveAndFlush(contribuicao);
        return detalhar(contribuicao);
    }

    private DetalheContribuicaoApm detalhar(ContribuicaoApm contribuicao) {
        return new DetalheContribuicaoApm(
                contribuicao,
                ajusteRepository.listarHistorico(contribuicao));
    }

    private Usuario obterSecretariaAtual(Usuario secretaria) {
        if (secretaria == null || secretaria.getId() == null) {
            throw acessoNegado();
        }
        return usuarioRepository.findById(secretaria.getId())
                .filter(Usuario::isAtivo)
                .filter(usuario -> usuario.getPerfil() == Perfil.SECRETARIA)
                .orElseThrow(this::acessoNegado);
    }

    private AccessDeniedException acessoNegado() {
        return new AccessDeniedException(
                "Usuário sem permissão para registrar contribuições");
    }

    private BigDecimal validarValor(String valor) {
        String normalizado = valor == null ? "" : valor.trim();
        if (normalizado.contains(",") && normalizado.contains(".")) {
            throw valorInvalido();
        }
        normalizado = normalizado.replace(',', '.');
        try {
            BigDecimal convertido = new BigDecimal(normalizado);
            if (convertido.scale() > 2
                    || convertido.signum() <= 0
                    || convertido.compareTo(VALOR_MAXIMO) > 0) {
                throw valorInvalido();
            }
            return convertido.setScale(2);
        } catch (NumberFormatException exception) {
            throw valorInvalido();
        }
    }

    private IllegalArgumentException valorInvalido() {
        return new IllegalArgumentException(
                "Informe um valor maior que zero, com no máximo duas casas decimais");
    }

    private String normalizarTextoOpcional(String texto, String mensagem) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String normalizado = texto.trim();
        if (normalizado.length() > TAMANHO_MAXIMO_TEXTO) {
            throw new IllegalArgumentException(mensagem);
        }
        return normalizado;
    }

    private String normalizarTextoObrigatorio(String texto, String mensagem) {
        String normalizado = normalizarTextoOpcional(texto, mensagem);
        if (normalizado == null) {
            throw new IllegalArgumentException(mensagem);
        }
        return normalizado;
    }

    private void validarId(Long id, String mensagem) {
        if (id == null) {
            throw new IllegalArgumentException(mensagem);
        }
    }
}
