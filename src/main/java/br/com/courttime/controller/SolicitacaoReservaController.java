package br.com.courttime.controller;

import br.com.courttime.controller.form.SolicitacaoReservaForm;
import br.com.courttime.entity.Quadra;
import br.com.courttime.entity.Reserva;
import br.com.courttime.entity.Usuario;
import br.com.courttime.security.UsuarioAtual;
import br.com.courttime.service.CalendarioDisponibilidadeService;
import br.com.courttime.service.ConsultaDisponibilidade;
import br.com.courttime.service.HorarioDisponibilidade;
import br.com.courttime.service.QuadraService;
import br.com.courttime.service.ReservaService;
import br.com.courttime.service.SituacaoSolicitacaoService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Controller
@RequestMapping("/solicitacoes")
public class SolicitacaoReservaController {

    private final ReservaService reservaService;
    private final QuadraService quadraService;
    private final CalendarioDisponibilidadeService calendarioDisponibilidadeService;
    private final SituacaoSolicitacaoService situacaoService;

    public SolicitacaoReservaController(
            ReservaService reservaService,
            QuadraService quadraService,
            CalendarioDisponibilidadeService calendarioDisponibilidadeService,
            SituacaoSolicitacaoService situacaoService) {
        this.reservaService = reservaService;
        this.quadraService = quadraService;
        this.calendarioDisponibilidadeService = calendarioDisponibilidadeService;
        this.situacaoService = situacaoService;
    }

    @GetMapping
    public String listar(@UsuarioAtual Usuario usuario, Model model) {
        model.addAttribute(
                "solicitacoes",
                situacaoService.descrever(
                        reservaService.listarSolicitacoesDoUsuario(usuario)));
        return "solicitacoes/lista";
    }

    @GetMapping("/nova")
    public String exibirFormulario(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @RequestParam(required = false) Long origem,
            @UsuarioAtual Usuario usuario,
            Model model,
            RedirectAttributes redirectAttributes) {
        LocalDate dataSelecionada = ajustarParaDataSolicitavel(data);
        SolicitacaoReservaForm form = new SolicitacaoReservaForm();
        form.setData(dataSelecionada);

        Reserva reservaCancelada = null;
        if (origem != null) {
            // Remarcação: só a partir de uma reserva cancelada do próprio aluno.
            try {
                reservaCancelada = reservaService.obterParaRemarcacao(origem, usuario);
            } catch (IllegalStateException exception) {
                redirectAttributes.addFlashAttribute("mensagemErro", exception.getMessage());
                return "redirect:/solicitacoes/" + origem;
            }
            form.setOrigemId(reservaCancelada.getId());
            form.setFinalidade(reservaCancelada.getFinalidade());
            form.setCopiarParticipantes(true);
        }

        model.addAttribute("form", form);
        prepararFormulario(model, dataSelecionada, reservaCancelada);
        return "solicitacoes/formulario";
    }

    @PostMapping
    public String criar(
            @Valid @ModelAttribute("form") SolicitacaoReservaForm form,
            BindingResult bindingResult,
            @UsuarioAtual Usuario usuario,
            Model model,
            RedirectAttributes redirectAttributes) {
        LocalDate dataSelecionada = ajustarParaDataSolicitavel(form.getData());
        Reserva reservaCancelada = obterReservaCanceladaDoFormulario(form, usuario);

        if (bindingResult.hasErrors()) {
            prepararFormulario(model, dataSelecionada, reservaCancelada);
            return "solicitacoes/formulario";
        }
        Quadra quadra = quadraService.obterQuadra();
        LocalTime horarioFim = form.getHorarioInicio().plusHours(1);

        try {
            Reserva reserva;
            if (reservaCancelada == null) {
                reserva = reservaService.criarSolicitacao(
                        usuario,
                        quadra,
                        form.getData(),
                        form.getHorarioInicio(),
                        horarioFim,
                        form.getFinalidade());
            } else {
                reserva = reservaService.criarRemarcacao(
                        usuario,
                        quadra,
                        reservaCancelada.getId(),
                        form.getData(),
                        form.getHorarioInicio(),
                        horarioFim,
                        form.getFinalidade(),
                        form.isCopiarParticipantes());
            }
            redirectAttributes.addFlashAttribute(
                    "mensagemSucesso",
                    reserva.isRascunho()
                            ? "Rascunho salvo. Cadastre ao menos dez participantes para registrar a solicitação e avançar para a documentação."
                            : "Solicitação registrada. A utilização da quadra ainda depende "
                                    + "da documentação e da aprovação da instituição.");
            return "redirect:/solicitacoes/" + reserva.getId();
        } catch (IllegalArgumentException | IllegalStateException exception) {
            bindingResult.reject("solicitacao.invalida", exception.getMessage());
            prepararFormulario(model, dataSelecionada, reservaCancelada);
            return "solicitacoes/formulario";
        }
    }

    /** A reserva cancelada informada no formulário, conferindo que é do aluno. */
    private Reserva obterReservaCanceladaDoFormulario(
            SolicitacaoReservaForm form, Usuario usuario) {
        if (form.getOrigemId() == null) {
            return null;
        }
        return reservaService.obterSolicitacaoDoUsuario(
                form.getOrigemId(), usuario);
    }

    @GetMapping("/{id}")
    public String detalhar(
            @PathVariable Long id, @UsuarioAtual Usuario usuario, Model model) {
        Reserva solicitacao = reservaService.obterSolicitacaoDoUsuario(id, usuario);
        model.addAttribute("solicitacao", solicitacao);
        model.addAttribute("situacao", situacaoService.descrever(solicitacao));
        // Vínculos de remarcação, nos dois sentidos.
        model.addAttribute("remarcacao",
                reservaService.obterRemarcacao(solicitacao).orElse(null));
        model.addAttribute("reservaRemarcada",
                reservaService.obterReservaRemarcada(solicitacao, usuario).orElse(null));
        return "solicitacoes/detalhe";
    }

    private void prepararFormulario(Model model, LocalDate data, Reserva reservaCancelada) {
        // Na remarcação, a navegação do calendário mantém a reserva de origem.
        model.addAttribute("reservaCancelada", reservaCancelada);
        model.addAttribute("rotaCalendario", reservaCancelada == null
                ? "/solicitacoes/nova"
                : "/solicitacoes/nova?origem=" + reservaCancelada.getId());
        Quadra quadra = quadraService.obterQuadra();
        ConsultaDisponibilidade consulta = calendarioDisponibilidadeService.consultar(quadra, data);
        List<HorarioDisponibilidade> horariosDisponiveis = consulta.horarios().stream()
                .filter(HorarioDisponibilidade::isDisponivel)
                .toList();

        model.addAttribute("quadra", quadra);
        model.addAttribute("consulta", consulta);
        model.addAttribute("horariosDisponiveis", horariosDisponiveis);
        LocalDate dataMinima = calendarioDisponibilidadeService.obterDataMinimaParaSolicitacao();
        model.addAttribute("dataMinima", dataMinima);
        model.addAttribute("calendario", CalendarioVisual.montar(
                data,
                calendarioDisponibilidadeService.obterDataAtual(),
                dataMinima,
                calendarioDisponibilidadeService.obterDiasComFuncionamento(quadra)));
    }

    /**
     * A tela de nova solicitação só mostra datas que ainda podem ser solicitadas.
     * Sem isso, o aluno conseguiria listar os horários de hoje e receberia um erro
     * apenas no envio do formulário.
     */
    private LocalDate ajustarParaDataSolicitavel(LocalDate data) {
        LocalDate dataMinima = calendarioDisponibilidadeService.obterDataMinimaParaSolicitacao();
        if (data == null || data.isBefore(dataMinima)) {
            return dataMinima;
        }
        return data;
    }

}
