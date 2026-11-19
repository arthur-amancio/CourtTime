package br.com.courttime.service;

import br.com.courttime.entity.CategoriaParticipante;
import br.com.courttime.entity.ParticipanteReserva;
import br.com.courttime.entity.Perfil;
import br.com.courttime.entity.PresencaParticipante;
import br.com.courttime.entity.Reserva;
import br.com.courttime.entity.StatusReserva;
import br.com.courttime.entity.Usuario;
import br.com.courttime.repository.ParticipanteReservaRepository;
import br.com.courttime.repository.ReservaRepository;
import br.com.courttime.security.Permissao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ConsultaSegurancaService {

    public static final String MENSAGEM_FORA_DO_DIA =
            "A presença só pode ser registrada no dia da reserva.";

    private final ReservaRepository reservaRepository;
    private final ParticipanteReservaRepository participanteRepository;

    public ConsultaSegurancaService(
            ReservaRepository reservaRepository,
            ParticipanteReservaRepository participanteRepository) {
        this.reservaRepository = reservaRepository;
        this.participanteRepository = participanteRepository;
    }

    @Transactional(readOnly = true)
    public List<ReservaAutorizadaResumo> listarAutorizadas(Usuario seguranca) {
        Permissao.exigir(seguranca, Perfil.SEGURANCA);
        LocalDate dataAtual = LocalDate.now(CalendarioDisponibilidadeService.FUSO_HORARIO);
        return reservaRepository.listarAutorizadasParaSeguranca(
                        StatusReserva.APROVADA, dataAtual)
                .stream()
                .map(this::resumir)
                .toList();
    }

    @Transactional(readOnly = true)
    public DetalheReservaAutorizada detalhar(Long reservaId, Usuario seguranca) {
        Permissao.exigir(seguranca, Perfil.SEGURANCA);
        if (reservaId == null) {
            throw new IllegalArgumentException("A reserva é obrigatória");
        }

        LocalDate dataAtual = LocalDate.now(CalendarioDisponibilidadeService.FUSO_HORARIO);
        Reserva reserva = reservaRepository.obterAutorizadaParaSeguranca(
                        reservaId, StatusReserva.APROVADA, dataAtual)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Reserva autorizada não encontrada"));
        List<ParticipanteConferencia> participantes = participanteRepository
                .findByReservaOrderByNomeCompletoAscIdAsc(reserva)
                .stream()
                .map(this::identificacao)
                .toList();
        // Proposta técnica (ainda a confirmar com a ETEC): presença só no dia da reserva.
        return new DetalheReservaAutorizada(
                resumir(reserva), participantes, reserva.getDataUtilizacao().equals(dataAtual));
    }

    /**
     * R08.1 e R08.3: a portaria marca presente ou ausente, e nada mais. Pagamento,
     * documento, horário e decisão continuam fora do alcance da segurança.
     */
    @Transactional
    public int registrarPresencas(
            Long reservaId, Usuario seguranca, Map<Long, PresencaParticipante> marcacoes) {
        Permissao.exigir(seguranca, Perfil.SEGURANCA);
        if (reservaId == null) {
            throw new IllegalArgumentException("A reserva é obrigatória");
        }
        if (marcacoes == null || marcacoes.isEmpty()) {
            throw new IllegalArgumentException("Marque a presença de pelo menos um participante");
        }
        // A trava da reserva serializa duas portarias marcando ao mesmo tempo.
        Reserva reserva = reservaRepository.findByIdForUpdate(reservaId)
                .filter(encontrada -> encontrada.getStatus() == StatusReserva.APROVADA)
                .orElseThrow(() -> new IllegalArgumentException("Reserva autorizada não encontrada"));
        LocalDate dataAtual = LocalDate.now(CalendarioDisponibilidadeService.FUSO_HORARIO);
        if (!reserva.getDataUtilizacao().equals(dataAtual)) {
            throw new IllegalStateException(MENSAGEM_FORA_DO_DIA);
        }

        Map<Long, ParticipanteReserva> participantes = new HashMap<>();
        participanteRepository.findByReservaOrderByNomeCompletoAscIdAsc(reserva)
                .forEach(participante -> participantes.put(participante.getId(), participante));
        if (!participantes.keySet().containsAll(marcacoes.keySet())) {
            throw new IllegalArgumentException("Participante não pertence a esta reserva");
        }
        Instant agora = Instant.now();
        int alteradas = 0;
        for (Map.Entry<Long, PresencaParticipante> marcacao : marcacoes.entrySet()) {
            ParticipanteReserva participante = participantes.get(marcacao.getKey());
            if (marcacao.getValue() != null && participante.getPresenca() != marcacao.getValue()) {
                participante.registrarPresenca(marcacao.getValue(), seguranca, agora);
                participanteRepository.save(participante);
                alteradas++;
            }
        }
        return alteradas;
    }

    private ReservaAutorizadaResumo resumir(Reserva reserva) {
        return new ReservaAutorizadaResumo(
                reserva.getId(),
                reserva.getDataUtilizacao(),
                reserva.getHorarioInicio(),
                reserva.getHorarioFim(),
                reserva.getQuadra().getNome(),
                reserva.getSolicitante().getNome(),
                participanteRepository.countByReserva(reserva));
    }

    private ParticipanteConferencia identificacao(ParticipanteReserva participante) {
        // R09.3: a portaria vê o vínculo de quem entra na categoria Outro.
        String categoria = participante.getCategoria() == CategoriaParticipante.ALUNO
                ? "Aluno"
                : "Outro (" + participante.getDescricaoVinculo() + ")";
        return new ParticipanteConferencia(
                participante.getId(),
                participante.getNomeCompleto(),
                participante.getRmOuRg(),
                categoria,
                participante.getPresenca());
    }
}
