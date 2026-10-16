package br.com.courttime.controller;

import br.com.courttime.controller.form.SolicitacaoReservaForm;
import br.com.courttime.entity.Quadra;
import br.com.courttime.entity.Reserva;
import br.com.courttime.entity.Usuario;
import br.com.courttime.repository.UsuarioRepository;
import br.com.courttime.service.CalendarioDisponibilidadeService;
import br.com.courttime.service.ConsultaDisponibilidade;
import br.com.courttime.service.HorarioDisponibilidade;
import br.com.courttime.service.QuadraService;
import br.com.courttime.service.ReservaService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
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
    private final UsuarioRepository usuarioRepository;

    public SolicitacaoReservaController(
            ReservaService reservaService,
            QuadraService quadraService,
            CalendarioDisponibilidadeService calendarioDisponibilidadeService,
            UsuarioRepository usuarioRepository) {
        this.reservaService = reservaService;
        this.quadraService = quadraService;
        this.calendarioDisponibilidadeService = calendarioDisponibilidadeService;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping
    public String listar(Authentication authentication, Model model) {
        Usuario usuario = obterUsuarioAutenticado(authentication);
        model.addAttribute("solicitacoes", reservaService.listarSolicitacoesDoUsuario(usuario));
        return "solicitacoes/lista";
    }

    @GetMapping("/nova")
    public String exibirFormulario(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            Model model) {
        LocalDate dataSelecionada = data == null
                ? calendarioDisponibilidadeService.obterDataAtual()
                : data;
        SolicitacaoReservaForm form = new SolicitacaoReservaForm();
        form.setData(dataSelecionada);
        model.addAttribute("form", form);
        prepararFormulario(model, dataSelecionada);
        return "solicitacoes/formulario";
    }

    @PostMapping
    public String criar(
            @Valid @ModelAttribute("form") SolicitacaoReservaForm form,
            BindingResult bindingResult,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {
        LocalDate dataSelecionada = form.getData() == null
                ? calendarioDisponibilidadeService.obterDataAtual()
                : form.getData();

        if (bindingResult.hasErrors()) {
            prepararFormulario(model, dataSelecionada);
            return "solicitacoes/formulario";
        }

        Usuario usuario = obterUsuarioAutenticado(authentication);
        Quadra quadra = quadraService.obterQuadra();
        LocalTime horarioFim = form.getHorarioInicio().plusHours(1);

        try {
            Reserva reserva = reservaService.criarSolicitacao(
                    usuario,
                    quadra,
                    form.getData(),
                    form.getHorarioInicio(),
                    horarioFim,
                    form.getFinalidade());
            redirectAttributes.addFlashAttribute(
                    "mensagemSucesso",
                    "Solicitação registrada. A utilização da quadra ainda depende "
                            + "da documentação e da aprovação da instituição.");
            return "redirect:/solicitacoes/" + reserva.getId();
        } catch (IllegalArgumentException | IllegalStateException exception) {
            bindingResult.reject("solicitacao.invalida", exception.getMessage());
            prepararFormulario(model, dataSelecionada);
            return "solicitacoes/formulario";
        }
    }

    @GetMapping("/{id}")
    public String detalhar(
            @PathVariable Long id, Authentication authentication, Model model) {
        Usuario usuario = obterUsuarioAutenticado(authentication);
        try {
            model.addAttribute(
                    "solicitacao", reservaService.obterSolicitacaoDoUsuario(id, usuario));
        } catch (AccessDeniedException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return "solicitacoes/detalhe";
    }

    private void prepararFormulario(Model model, LocalDate data) {
        Quadra quadra = quadraService.obterQuadra();
        ConsultaDisponibilidade consulta = calendarioDisponibilidadeService.consultar(quadra, data);
        List<HorarioDisponibilidade> horariosDisponiveis = consulta.horarios().stream()
                .filter(HorarioDisponibilidade::isDisponivel)
                .toList();

        model.addAttribute("quadra", quadra);
        model.addAttribute("consulta", consulta);
        model.addAttribute("horariosDisponiveis", horariosDisponiveis);
        model.addAttribute("dataMinima", calendarioDisponibilidadeService.obterDataAtual());
    }

    private Usuario obterUsuarioAutenticado(Authentication authentication) {
        return usuarioRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuário autenticado não encontrado"));
    }
}
