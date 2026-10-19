package br.com.courttime.service;

import br.com.courttime.entity.ConferenciaDocumento;
import br.com.courttime.entity.ContribuicaoApm;
import br.com.courttime.entity.DocumentoReserva;
import br.com.courttime.entity.Perfil;
import br.com.courttime.entity.Quadra;
import br.com.courttime.entity.Reserva;
import br.com.courttime.entity.StatusReserva;
import br.com.courttime.entity.ResultadoConferenciaDocumento;
import br.com.courttime.entity.Usuario;
import br.com.courttime.repository.ContribuicaoApmRepository;
import br.com.courttime.repository.DocumentoReservaRepository;
import br.com.courttime.repository.QuadraRepository;
import br.com.courttime.repository.ReservaRepository;
import br.com.courttime.repository.UsuarioRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

@Service
public class ReservaService {

    private static final int TAMANHO_MAXIMO_FINALIDADE = 300;

    private final ReservaRepository reservaRepository;
    private final QuadraRepository quadraRepository;
    private final CalendarioDisponibilidadeService calendarioDisponibilidadeService;
    private final DocumentoReservaRepository documentoRepository;
    private final ParticipanteReservaService participanteService;
    private final ContribuicaoApmRepository contribuicaoRepository;
    private final UsuarioRepository usuarioRepository;

    public ReservaService(
            ReservaRepository reservaRepository,
            QuadraRepository quadraRepository,
            CalendarioDisponibilidadeService calendarioDisponibilidadeService,
            DocumentoReservaRepository documentoRepository,
            ParticipanteReservaService participanteService,
            ContribuicaoApmRepository contribuicaoRepository,
            UsuarioRepository usuarioRepository) {
        this.reservaRepository = reservaRepository;
        this.quadraRepository = quadraRepository;
        this.calendarioDisponibilidadeService = calendarioDisponibilidadeService;
        this.documentoRepository = documentoRepository;
        this.participanteService = participanteService;
        this.contribuicaoRepository = contribuicaoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public Reserva criarSolicitacao(
            Usuario solicitante,
            Quadra quadra,
            LocalDate data,
            LocalTime horarioInicio,
            LocalTime horarioFim,
            String finalidade) {
        validarUsuarioAtivoComPerfil(solicitante, Perfil.ALUNO);
        Objects.requireNonNull(quadra, "A quadra é obrigatória");
        Objects.requireNonNull(data, "A data é obrigatória");
        if (data.isBefore(calendarioDisponibilidadeService.obterDataMinimaParaSolicitacao())) {
            throw new IllegalArgumentException(
                    "A solicitação deve ser feita com pelo menos um dia de antecedência. "
                            + "Escolha uma data a partir de amanhã");
        }
        validarPeriodo(horarioInicio, horarioFim);
        String finalidadeNormalizada = validarFinalidade(finalidade);

        if (!calendarioDisponibilidadeService.periodoEstaDisponivel(
                quadra, data, horarioInicio, horarioFim)) {
            throw new IllegalStateException("O período selecionado não está disponível");
        }

        Reserva reserva = new Reserva();
        reserva.setQuadra(quadra);
        reserva.setSolicitante(solicitante);
        reserva.setDataUtilizacao(data);
        reserva.setHorarioInicio(horarioInicio);
        reserva.setHorarioFim(horarioFim);
        reserva.setFinalidade(finalidadeNormalizada);
        reserva.setStatus(StatusReserva.PENDENTE_DOCUMENTACAO);
        return reservaRepository.save(reserva);
    }

    @Transactional(readOnly = true)
    public List<Reserva> listarSolicitacoesDoUsuario(Usuario solicitante) {
        validarUsuarioAtivoComPerfil(solicitante, Perfil.ALUNO);
        return reservaRepository.findBySolicitanteOrderByCriadaEmDesc(solicitante);
    }

    @Transactional(readOnly = true)
    public Reserva obterSolicitacaoDoUsuario(Long reservaId, Usuario solicitante) {
        validarUsuarioAtivoComPerfil(solicitante, Perfil.ALUNO);
        if (reservaId == null) {
            throw new IllegalArgumentException("A solicitação é obrigatória");
        }
        return reservaRepository.findByIdAndSolicitante(reservaId, solicitante)
                .orElseThrow(() -> new AccessDeniedException(
                        "A solicitação não existe ou não pertence ao usuário autenticado"));
    }

    @Transactional
    public Reserva aprovar(Long reservaId, Usuario administrador) {
        Usuario administradorAtual = obterAdministradorAtual(administrador);
        Reserva reserva = obterParaAnalise(reservaId);
        impedirAnaliseDaPropriaSolicitacao(reserva, administradorAtual);
        validarPendente(reserva);
        validarPeriodoAindaNaoIniciado(reserva);
        validarElegibilidade(reserva);

        Quadra quadraBloqueada = quadraRepository.findByIdForUpdate(reserva.getQuadra().getId())
                .orElseThrow(() -> new IllegalStateException("Quadra não encontrada"));

        if (!calendarioDisponibilidadeService.periodoEstaDisponivel(
                quadraBloqueada,
                reserva.getDataUtilizacao(),
                reserva.getHorarioInicio(),
                reserva.getHorarioFim())) {
            throw new IllegalStateException(
                    "A reserva não pode ser aprovada porque o período não está mais disponível");
        }

        reserva.setStatus(StatusReserva.APROVADA);
        reserva.setAnalisadaPor(administradorAtual);
        reserva.setAnalisadaEm(Instant.now());
        reserva.setMotivoRejeicao(null);
        return reservaRepository.save(reserva);
    }

    @Transactional
    public Reserva rejeitar(Long reservaId, Usuario administrador, String motivo) {
        Usuario administradorAtual = obterAdministradorAtual(administrador);
        Reserva reserva = obterParaAnalise(reservaId);
        impedirAnaliseDaPropriaSolicitacao(reserva, administradorAtual);
        validarPendente(reserva);

        String motivoNormalizado = motivo == null ? "" : motivo.trim();
        if (motivoNormalizado.isEmpty() || motivoNormalizado.length() > 500) {
            throw new IllegalArgumentException(
                    "O motivo da rejeição é obrigatório e deve possuir no máximo 500 caracteres");
        }

        reserva.setStatus(StatusReserva.REJEITADA);
        reserva.setAnalisadaPor(administradorAtual);
        reserva.setAnalisadaEm(Instant.now());
        reserva.setMotivoRejeicao(motivoNormalizado);
        return reservaRepository.save(reserva);
    }

    @Transactional
    public Reserva cancelarInstitucionalmente(
            Long reservaId, Usuario administrador, String motivo) {
        Usuario administradorAtual = obterAdministradorAtual(administrador);
        String motivoNormalizado = validarMotivoCancelamento(motivo);
        Reserva reserva = obterParaAnalise(reservaId);
        impedirAnaliseDaPropriaSolicitacao(reserva, administradorAtual);

        if (reserva.getStatus() != StatusReserva.APROVADA) {
            throw new IllegalStateException(
                    "Somente reservas aprovadas podem ser canceladas pela administração");
        }
        if (reserva.getDataUtilizacao()
                .isBefore(calendarioDisponibilidadeService.obterDataAtual())) {
            throw new IllegalStateException(
                    "Reservas com data de utilização passada não podem ser canceladas");
        }

        reserva.setStatus(StatusReserva.CANCELADA);
        reserva.setMotivoCancelamentoInstitucional(motivoNormalizado);
        reserva.setCanceladaPor(administradorAtual);
        reserva.setCanceladaEm(Instant.now());
        return reservaRepository.save(reserva);
    }

    private Reserva obterParaAnalise(Long reservaId) {
        if (reservaId == null) {
            throw new IllegalArgumentException("A reserva é obrigatória");
        }
        return reservaRepository.findByIdForUpdate(reservaId)
                .orElseThrow(() -> new IllegalArgumentException("Reserva não encontrada"));
    }

    private Usuario obterAdministradorAtual(Usuario administrador) {
        if (administrador == null || administrador.getId() == null) {
            throw new AccessDeniedException("Usuário sem permissão para esta operação");
        }
        return usuarioRepository.findByIdForUpdate(administrador.getId())
                .filter(Usuario::isAtivo)
                .filter(usuario -> usuario.getPerfil() == Perfil.ADMINISTRADOR)
                .orElseThrow(() -> new AccessDeniedException(
                        "Usuário sem permissão para esta operação"));
    }

    private void validarElegibilidade(Reserva reserva) {
        DocumentoReserva documento = documentoRepository
                .obterVersaoMaisRecenteComConferencia(reserva)
                .orElseThrow(() -> new IllegalStateException(
                        "A versão mais recente do documento ainda não foi conferida"));
        ConferenciaDocumento conferencia = documento.getConferencia();
        if (conferencia == null
                || conferencia.getResultado() != ResultadoConferenciaDocumento.VALIDADO) {
            throw new IllegalStateException(
                    "A versão mais recente do documento ainda não foi validada");
        }
        participanteService.validarDocumentoAtual(reserva, documento);
        ContribuicaoApm contribuicao = contribuicaoRepository.obterPorReservaId(reserva.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "A contribuição presencial ainda não foi confirmada"));
        if (!mesmaConferencia(contribuicao.getConferenciaDocumento(), conferencia)) {
            throw new IllegalStateException(
                    "A contribuição não corresponde ao documento mais recente validado");
        }
    }

    private boolean mesmaConferencia(
            ConferenciaDocumento contribuicao, ConferenciaDocumento documento) {
        if (contribuicao == documento) {
            return true;
        }
        return contribuicao != null
                && contribuicao.getId() != null
                && Objects.equals(contribuicao.getId(), documento.getId());
    }

    private void validarUsuarioAtivoComPerfil(Usuario usuario, Perfil perfilEsperado) {
        if (usuario == null || !usuario.isAtivo() || usuario.getPerfil() != perfilEsperado) {
            throw new AccessDeniedException("Usuário sem permissão para esta operação");
        }
    }

    private void impedirAnaliseDaPropriaSolicitacao(Reserva reserva, Usuario administrador) {
        if (Objects.equals(reserva.getSolicitante().getId(), administrador.getId())) {
            throw new AccessDeniedException("O usuário não pode analisar a própria solicitação");
        }
    }

    /**
     * Uma solicitação pode ficar parada na fila e chegar à análise quando o
     * período de utilização já começou ou já terminou. Nesse caso a autorização
     * não teria efeito, então a aprovação é recusada.
     *
     * A aprovação no próprio dia continua permitida enquanto o horário não
     * começou: o termo da ETEC exige um dia de antecedência para a entrega da
     * documentação assinada, e não para a decisão da direção.
     *
     * A rejeição continua permitida em qualquer data, para que a administração
     * consiga encerrar uma solicitação vencida com um motivo registrado.
     */
    private void validarPeriodoAindaNaoIniciado(Reserva reserva) {
        LocalDateTime inicioDaUtilizacao = LocalDateTime.of(
                reserva.getDataUtilizacao(), reserva.getHorarioInicio());
        if (!inicioDaUtilizacao.isAfter(
                calendarioDisponibilidadeService.obterDataHoraAtual())) {
            throw new IllegalStateException(
                    "A reserva não pode ser aprovada porque o horário de utilização "
                            + "já começou ou já passou");
        }
    }

    private void validarPendente(Reserva reserva) {
        if (reserva.getStatus() != StatusReserva.PENDENTE_ANALISE) {
            throw new IllegalStateException(
                    "Somente solicitações com documentação conferida podem ser analisadas");
        }
    }

    private void validarPeriodo(LocalTime horarioInicio, LocalTime horarioFim) {
        if (horarioInicio == null || horarioFim == null
                || !horarioInicio.isBefore(horarioFim)
                || !Duration.between(horarioInicio, horarioFim).equals(Duration.ofHours(1))) {
            throw new IllegalArgumentException("A solicitação deve possuir duração de uma hora");
        }
    }

    private String validarFinalidade(String finalidade) {
        String finalidadeNormalizada = finalidade == null ? "" : finalidade.trim();
        if (finalidadeNormalizada.isEmpty()
                || finalidadeNormalizada.length() > TAMANHO_MAXIMO_FINALIDADE) {
            throw new IllegalArgumentException(
                    "A finalidade é obrigatória e deve possuir no máximo 300 caracteres");
        }
        return finalidadeNormalizada;
    }

    private String validarMotivoCancelamento(String motivo) {
        String motivoNormalizado = motivo == null ? "" : motivo.trim();
        if (motivoNormalizado.isEmpty() || motivoNormalizado.length() > 500) {
            throw new IllegalArgumentException(
                    "O motivo do cancelamento é obrigatório e deve possuir no máximo 500 caracteres");
        }
        return motivoNormalizado;
    }
}
