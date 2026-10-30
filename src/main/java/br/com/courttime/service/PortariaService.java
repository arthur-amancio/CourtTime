package br.com.courttime.service;

import br.com.courttime.entity.CategoriaParticipante;
import br.com.courttime.entity.LocalPagamento;
import br.com.courttime.entity.PagamentoAdicional;
import br.com.courttime.entity.ParticipanteReserva;
import br.com.courttime.entity.Perfil;
import br.com.courttime.entity.PresencaParticipante;
import br.com.courttime.entity.Reserva;
import br.com.courttime.entity.StatusReserva;
import br.com.courttime.entity.Usuario;
import br.com.courttime.entity.VinculoParticipante;
import br.com.courttime.repository.PagamentoAdicionalRepository;
import br.com.courttime.repository.ParticipanteReservaRepository;
import br.com.courttime.repository.ReservaRepository;
import br.com.courttime.security.Permissao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Regras do dia da reserva na portaria (decisões do grupo a partir da conversa
 * com o Leandro, 22/10/2026):
 *
 * R10 - a segurança pode acrescentar pessoas que não estavam na lista. Trocar
 *       pessoas (sai uma, entra outra) não cobra nada; só paga quem passa do
 *       total já pago, R$ 2,00 por pessoa.
 * R11 - a quadra só é liberada com pelo menos 10 pessoas presentes, de qualquer
 *       categoria, e sem valor pendente. O valor a mais é pago na secretaria
 *       ou, se ela estiver fechada, entregue à segurança por acordo; depois a
 *       secretaria confirma o repasse.
 */
@Service
public class PortariaService {

    public static final int MINIMO_DE_PRESENTES = 10;
    public static final String MENSAGEM_FORA_DO_DIA =
            "Esta ação só pode ser feita no dia da reserva.";
    public static final String MENSAGEM_RESERVA_LIBERADA =
            "A conferência foi encerrada porque a quadra já foi liberada.";

    private final ReservaRepository reservaRepository;
    private final ParticipanteReservaRepository participanteRepository;
    private final ParticipanteReservaService participanteService;
    private final PagamentoAdicionalRepository pagamentoRepository;

    public PortariaService(
            ReservaRepository reservaRepository,
            ParticipanteReservaRepository participanteRepository,
            ParticipanteReservaService participanteService,
            PagamentoAdicionalRepository pagamentoRepository) {
        this.reservaRepository = reservaRepository;
        this.participanteRepository = participanteRepository;
        this.participanteService = participanteService;
        this.pagamentoRepository = pagamentoRepository;
    }

    /** R10: quem passa do total já pago; troca de pessoas dá zero. */
    public static long calcularPessoasAPagar(long presentes, long pagos) {
        return Math.max(0, presentes - pagos);
    }

    public static BigDecimal calcularValor(long pessoas) {
        return ContribuicaoApmService.VALOR_POR_PARTICIPANTE.multiply(BigDecimal.valueOf(pessoas));
    }

    @Transactional(readOnly = true)
    public ResumoPortaria resumir(Reserva reserva) {
        return resumir(reserva, participanteRepository.findByReservaOrderByNomeCompletoAscIdAsc(reserva));
    }

    // A consulta de segurança já carregou esta lista para identificar os participantes.
    ResumoPortaria resumir(Reserva reserva, List<ParticipanteReserva> participantes) {
        List<PagamentoAdicional> pagamentos =
                pagamentoRepository.findByReservaOrderByRegistradoEmAscIdAsc(reserva);

        long presentes = participantes.stream()
                .filter(participante -> participante.getPresenca() == PresencaParticipante.PRESENTE)
                .count();
        long daLista = participantes.stream()
                .filter(participante -> !participante.isAdicionadoNaPortaria())
                .count();
        long pagosAMais = pagamentos.stream()
                .mapToLong(PagamentoAdicional::getQuantidadePessoas)
                .sum();
        long pessoasAPagar = calcularPessoasAPagar(presentes, daLista + pagosAMais);
        return new ResumoPortaria(
                presentes,
                pessoasAPagar,
                calcularValor(pessoasAPagar),
                reserva.getLiberadaEm());
    }

    /** R10: a segurança acrescenta quem chegou e não estava na lista. */
    @Transactional
    public ParticipanteReserva acrescentarPessoa(
            Long reservaId,
            Usuario seguranca,
            String nomeCompleto,
            String rmOuRg,
            CategoriaParticipante categoria,
            VinculoParticipante vinculo) {
        Permissao.exigir(seguranca, Perfil.SEGURANCA);
        Reserva reserva = obterReservaDeHoje(reservaId);
        exigirNaoLiberada(reserva);
        return participanteService.acrescentarNaPortaria(
                reserva, seguranca, nomeCompleto, rmOuRg, categoria, vinculo);
    }

    /** R11: secretaria fechada; os alunos entregam o valor à segurança. */
    @Transactional
    public PagamentoAdicional registrarPagamentoNaPortaria(Long reservaId, Usuario seguranca) {
        Permissao.exigir(seguranca, Perfil.SEGURANCA);
        return registrarPagamento(obterReservaDeHoje(reservaId), seguranca, LocalPagamento.PORTARIA);
    }

    /** R11: os alunos pagam o valor a mais na secretaria. */
    @Transactional
    public PagamentoAdicional registrarPagamentoNaSecretaria(Long reservaId, Usuario secretaria) {
        Permissao.exigir(secretaria, Perfil.SECRETARIA);
        return registrarPagamento(obterReservaDeHoje(reservaId), secretaria, LocalPagamento.SECRETARIA);
    }

    /** R11: com 10 presentes e nada pendente, a segurança libera a quadra. */
    @Transactional
    public void liberarQuadra(Long reservaId, Usuario seguranca) {
        Permissao.exigir(seguranca, Perfil.SEGURANCA);
        Reserva reserva = obterReservaDeHoje(reservaId);
        exigirNaoLiberada(reserva);
        ResumoPortaria resumo = resumir(reserva);
        if (!resumo.minimoAtingido()) {
            throw new IllegalStateException("Quadra não liberada: " + resumo.presentes()
                    + " pessoas presentes. São necessárias pelo menos "
                    + MINIMO_DE_PRESENTES + ".");
        }
        if (resumo.temValorAPagar()) {
            throw new IllegalStateException("Quadra não liberada: falta pagar o valor de "
                    + resumo.pessoasAPagar() + " pessoa(s) a mais. Registre o pagamento"
                    + " ou marque como ausente quem não vai entrar.");
        }
        reserva.registrarLiberacao(seguranca, Instant.now());
        reservaRepository.save(reserva);
    }

    /** R11: reservas de hoje com pessoas a mais ainda sem pagamento. */
    @Transactional(readOnly = true)
    public List<ValorAMaisPendente> listarValoresPendentes(Usuario secretaria) {
        Permissao.exigir(secretaria, Perfil.SECRETARIA);
        return reservaRepository.listarAutorizadas(StatusReserva.APROVADA, hoje())
                .stream()
                .filter(reserva -> reserva.getDataUtilizacao().equals(hoje()))
                .map(reserva -> {
                    ResumoPortaria resumo = resumir(reserva);
                    return new ValorAMaisPendente(
                            reserva, resumo.pessoasAPagar(), resumo.valorAPagar());
                })
                .filter(pendente -> pendente.pessoasAPagar() > 0)
                .toList();
    }

    /** R11: dinheiro que a segurança recebeu e a secretaria ainda não confirmou. */
    @Transactional(readOnly = true)
    public List<PagamentoAdicional> listarAguardandoRepasse(Usuario secretaria) {
        Permissao.exigir(secretaria, Perfil.SECRETARIA);
        return pagamentoRepository.listarAguardandoRepasse(LocalPagamento.PORTARIA);
    }

    @Transactional
    public void confirmarRepasse(Long pagamentoId, Usuario secretaria) {
        Permissao.exigir(secretaria, Perfil.SECRETARIA);
        if (pagamentoId == null) {
            throw new IllegalArgumentException("O pagamento é obrigatório");
        }
        PagamentoAdicional pagamento = pagamentoRepository.findByIdForUpdate(pagamentoId)
                .orElseThrow(() -> new IllegalArgumentException("Pagamento não encontrado"));
        if (!pagamento.aguardaRepasse()) {
            throw new IllegalStateException("Este pagamento não aguarda repasse.");
        }
        pagamento.confirmarRepasse(secretaria, Instant.now());
        pagamentoRepository.save(pagamento);
    }

    private PagamentoAdicional registrarPagamento(
            Reserva reserva, Usuario usuario, LocalPagamento local) {
        exigirNaoLiberada(reserva);
        ResumoPortaria resumo = resumir(reserva);
        if (!resumo.temValorAPagar()) {
            throw new IllegalStateException("Não há valor a mais para pagar nesta reserva.");
        }
        // O valor é calculado pelo sistema; ninguém digita (como em R03.2).
        PagamentoAdicional pagamento = new PagamentoAdicional();
        pagamento.setReserva(reserva);
        pagamento.setQuantidadePessoas((int) resumo.pessoasAPagar());
        pagamento.setValor(resumo.valorAPagar());
        pagamento.setLocal(local);
        pagamento.setRegistradoPor(usuario);
        pagamento.setRegistradoEm(Instant.now());
        return pagamentoRepository.save(pagamento);
    }

    /** Reserva aprovada de hoje, travada para duas portarias não agirem ao mesmo tempo. */
    private Reserva obterReservaDeHoje(Long reservaId) {
        if (reservaId == null) {
            throw new IllegalArgumentException("A reserva é obrigatória");
        }
        Reserva reserva = reservaRepository.findByIdForUpdate(reservaId)
                .filter(encontrada -> encontrada.getStatus() == StatusReserva.APROVADA)
                .orElseThrow(() -> new IllegalArgumentException("Reserva autorizada não encontrada"));
        if (!reserva.getDataUtilizacao().equals(hoje())) {
            throw new IllegalStateException(MENSAGEM_FORA_DO_DIA);
        }
        return reserva;
    }

    private LocalDate hoje() {
        return LocalDate.now(CalendarioDisponibilidadeService.FUSO_HORARIO);
    }

    private void exigirNaoLiberada(Reserva reserva) {
        if (reserva.isLiberada()) {
            throw new IllegalStateException(MENSAGEM_RESERVA_LIBERADA);
        }
    }
}
