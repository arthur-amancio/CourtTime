package br.com.courttime.controller;

import br.com.courttime.controller.form.BloqueioQuadraForm;
import br.com.courttime.controller.form.HorarioSemanalForm;
import br.com.courttime.entity.DiaSemana;
import br.com.courttime.entity.DisponibilidadeQuadra;
import br.com.courttime.entity.Usuario;
import br.com.courttime.repository.UsuarioRepository;
import br.com.courttime.service.AdministracaoQuadraService;
import br.com.courttime.service.CalendarioDisponibilidadeService;
import jakarta.validation.Valid;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminQuadraController {

    private final AdministracaoQuadraService administracaoService;
    private final CalendarioDisponibilidadeService calendarioService;
    private final UsuarioRepository usuarioRepository;

    public AdminQuadraController(
            AdministracaoQuadraService administracaoService,
            CalendarioDisponibilidadeService calendarioService,
            UsuarioRepository usuarioRepository) {
        this.administracaoService = administracaoService;
        this.calendarioService = calendarioService;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping
    public String painel(Authentication authentication, Model model) {
        model.addAttribute(
                "painel", administracaoService.carregarPainel(obterUsuario(authentication)));
        return "admin/painel";
    }

    @GetMapping("/horarios")
    public String listarHorarios(Authentication authentication, Model model) {
        model.addAttribute(
                "horarios", administracaoService.listarHorarios(obterUsuario(authentication)));
        return "admin/horarios/lista";
    }

    @GetMapping("/horarios/novo")
    public String novoHorario(Model model) {
        model.addAttribute("form", new HorarioSemanalForm());
        prepararFormularioHorario(model, null);
        return "admin/horarios/formulario";
    }

    @PostMapping("/horarios")
    public String cadastrarHorario(
            @Valid @ModelAttribute("form") HorarioSemanalForm form,
            BindingResult bindingResult,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            prepararFormularioHorario(model, null);
            return "admin/horarios/formulario";
        }
        try {
            administracaoService.cadastrarHorario(
                    obterUsuario(authentication),
                    form.getDiaSemana(),
                    form.getHorarioInicio(),
                    form.getHorarioFim());
            redirectAttributes.addFlashAttribute(
                    "mensagemSucesso", "Horário semanal cadastrado com sucesso.");
            return "redirect:/admin/horarios";
        } catch (IllegalArgumentException | IllegalStateException exception) {
            bindingResult.reject("horario.invalido", exception.getMessage());
            prepararFormularioHorario(model, null);
            return "admin/horarios/formulario";
        }
    }

    @GetMapping("/horarios/{id}/editar")
    public String editarHorario(
            @PathVariable Long id, Authentication authentication, Model model) {
        DisponibilidadeQuadra horario = administracaoService.obterHorario(
                id, obterUsuario(authentication));
        HorarioSemanalForm form = new HorarioSemanalForm();
        form.setDiaSemana(horario.getDiaSemana());
        form.setHorarioInicio(horario.getHorarioInicio());
        form.setHorarioFim(horario.getHorarioFim());
        model.addAttribute("form", form);
        prepararFormularioHorario(model, id);
        return "admin/horarios/formulario";
    }

    @PostMapping("/horarios/{id}")
    public String salvarEdicaoHorario(
            @PathVariable Long id,
            @Valid @ModelAttribute("form") HorarioSemanalForm form,
            BindingResult bindingResult,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            prepararFormularioHorario(model, id);
            return "admin/horarios/formulario";
        }
        try {
            administracaoService.editarHorario(
                    id,
                    obterUsuario(authentication),
                    form.getDiaSemana(),
                    form.getHorarioInicio(),
                    form.getHorarioFim());
            redirectAttributes.addFlashAttribute(
                    "mensagemSucesso", "Horário semanal atualizado com sucesso.");
            return "redirect:/admin/horarios";
        } catch (IllegalArgumentException | IllegalStateException exception) {
            bindingResult.reject("horario.invalido", exception.getMessage());
            prepararFormularioHorario(model, id);
            return "admin/horarios/formulario";
        }
    }

    @PostMapping("/horarios/{id}/alternar")
    public String alternarHorario(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            DisponibilidadeQuadra horario = administracaoService.alternarHorario(
                    id, obterUsuario(authentication));
            redirectAttributes.addFlashAttribute(
                    "mensagemSucesso",
                    horario.isAtiva() ? "Horário ativado com sucesso." : "Horário desativado.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("mensagemErro", exception.getMessage());
        }
        return "redirect:/admin/horarios";
    }

    @GetMapping("/bloqueios")
    public String listarBloqueios(Authentication authentication, Model model) {
        model.addAttribute(
                "bloqueios",
                administracaoService.listarBloqueiosFuturos(obterUsuario(authentication)));
        return "admin/bloqueios/lista";
    }

    @GetMapping("/bloqueios/novo")
    public String novoBloqueio(Model model) {
        BloqueioQuadraForm form = new BloqueioQuadraForm();
        form.setData(calendarioService.obterDataAtual());
        model.addAttribute("form", form);
        prepararFormularioBloqueio(model);
        return "admin/bloqueios/formulario";
    }

    @PostMapping("/bloqueios")
    public String cadastrarBloqueio(
            @Valid @ModelAttribute("form") BloqueioQuadraForm form,
            BindingResult bindingResult,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            prepararFormularioBloqueio(model);
            return "admin/bloqueios/formulario";
        }
        try {
            administracaoService.cadastrarBloqueio(
                    obterUsuario(authentication),
                    form.getData(),
                    form.getHorarioInicio(),
                    form.getHorarioFim(),
                    form.getMotivo());
            redirectAttributes.addFlashAttribute(
                    "mensagemSucesso", "Bloqueio cadastrado com sucesso.");
            return "redirect:/admin/bloqueios";
        } catch (IllegalArgumentException | IllegalStateException exception) {
            bindingResult.reject("bloqueio.invalido", exception.getMessage());
            prepararFormularioBloqueio(model);
            return "admin/bloqueios/formulario";
        }
    }

    @PostMapping("/bloqueios/{id}/desativar")
    public String desativarBloqueio(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            administracaoService.desativarBloqueio(id, obterUsuario(authentication));
            redirectAttributes.addFlashAttribute("mensagemSucesso", "Bloqueio desativado.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("mensagemErro", exception.getMessage());
        }
        return "redirect:/admin/bloqueios";
    }

    private Usuario obterUsuario(Authentication authentication) {
        return usuarioRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuário autenticado não encontrado"));
    }

    private void prepararFormularioHorario(Model model, Long horarioId) {
        model.addAttribute("diasSemana", DiaSemana.values());
        model.addAttribute("horarioId", horarioId);
        model.addAttribute("edicao", horarioId != null);
    }

    private void prepararFormularioBloqueio(Model model) {
        model.addAttribute("dataMinima", calendarioService.obterDataAtual());
    }
}
