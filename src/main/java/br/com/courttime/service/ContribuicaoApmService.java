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
import br.com.courttime.security.Permissao;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class ContribuicaoApmService {

    /** R03: R$ 2,00 por participante, de qualquer categoria (Aluno ou Outro). */
    public static final BigDecimal VALOR_POR_PARTICIPANTE = new BigDecimal("2.00");

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
    public List<ContribuicaoPendente> listarPendentes(Usuario secretaria) {
        obterSecretariaAtual(secretaria);
        return contribuicaoRepository.listarPendentes(
                        StatusReserva.PENDENTE_DOCUMENTACAO,
                        ResultadoConferenciaDocumento.VALIDADO)
                .stream()
                .map(reserva -> pendente(reserva, contribuicaoDaReservaCancelada(reserva).orElse(null)))
                .toList();
    }

    private ContribuicaoPendente pendente(Reserva reserva, ContribuicaoApm origem) {
        long participantes = participanteService.contarParticipantes(reserva);
        if (origem == null) {
            return new ContribuicaoPendente(reserva, participantes);
        }
        return new ContribuicaoPendente(reserva, participantes,
                origem.getReserva().getId(), origem.getValorEfetivamentePago());
    }

    /**
     * R04.1: na remarcação, o pagamento da reserva cancelada vira crédito.
     * Cada cancelada só pode ser remarcada uma vez, então o crédito é usado uma
     * única vez (R04.4).
     */
    private Optional<ContribuicaoApm> contribuicaoDaReservaCancelada(Reserva reserva) {
        Reserva origem = reserva.getRemarcacaoDe();
        if (origem == null) {
            return Optional.empty();
        }
        return contribuicaoRepository.obterPorReservaId(origem.getId());
    }

    /** R03.1: valor devido = R$ 2,00 × total de participantes. */
    public static BigDecimal calcularValorDevido(long participantes) {
        return VALOR_POR_PARTICIPANTE.multiply(BigDecimal.valueOf(participantes));
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
            String observacoes) {
        Usuario operadora = obterSecretariaAtual(secretaria);
        validarId(reservaId, "A solicitação é obrigatória");
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

        // A lista está travada desde o envio, então a contagem atual é a do
        // documento conferido (R03.1). A secretaria não digita o valor (R03.2).
        ContribuicaoApm origem = contribuicaoDaReservaCancelada(reserva).orElse(null);
        ContribuicaoPendente calculo = pendente(reserva, origem);
        if (calculo.valorDevido().signum() <= 0) {
            throw new IllegalStateException("A solicitação não possui participantes cadastrados");
        }

        ContribuicaoApm contribuicao = new ContribuicaoApm();
        contribuicao.setReserva(reserva);
        contribuicao.setConferenciaDocumento(conferencia);
        contribuicao.setValorDevido(calculo.valorDevido());
        // R04.2 e R04.3: com crédito, registra só a diferença; se o crédito cobre
        // tudo, o valor recebido é zero e fica registrado apenas o uso do crédito.
        contribuicao.setValor(calculo.valorACobrar());
        if (calculo.temCredito()) {
            contribuicao.setCreditoAplicado(calculo.creditoAplicado());
            contribuicao.setContribuicaoOrigem(origem);
        }
        contribuicao.setObservacoes(observacoesNormalizadas);
        contribuicao.setRegistradaPor(operadora);
        contribuicao = contribuicaoRepository.saveAndFlush(contribuicao);
        reserva.setStatus(StatusReserva.PENDENTE_ANALISE);
        reservaRepository.save(reserva);
        return detalhar(contribuicao);
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
        List<AjusteContribuicaoApm> ajustes = ajusteRepository.listarHistorico(contribuicao);
        ContribuicaoApm origem = contribuicao.getContribuicaoOrigem();
        if (origem == null) {
            return new DetalheContribuicaoApm(contribuicao, ajustes);
        }
        // D2: o excedente só é mostrado, sem destino, até a decisão da ETEC.
        BigDecimal excedente = origem.getValorEfetivamentePago()
                .subtract(contribuicao.getCreditoAplicado());
        return new DetalheContribuicaoApm(
                contribuicao, ajustes, origem.getReserva().getId(), excedente.max(BigDecimal.ZERO));
    }

    private Usuario obterSecretariaAtual(Usuario secretaria) {
        if (secretaria == null || secretaria.getId() == null) {
            throw acessoNegado();
        }
        return usuarioRepository.findById(secretaria.getId())
                .filter(usuario -> Permissao.temPerfil(usuario, Perfil.SECRETARIA))
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
