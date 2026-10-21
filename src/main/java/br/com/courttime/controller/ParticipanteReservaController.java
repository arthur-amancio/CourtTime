package br.com.courttime.controller;

import br.com.courttime.controller.form.ParticipanteReservaForm;
import br.com.courttime.entity.ParticipanteReserva;
import br.com.courttime.entity.Reserva;
import br.com.courttime.entity.Usuario;
import br.com.courttime.entity.VinculoParticipante;
import br.com.courttime.repository.UsuarioRepository;
import br.com.courttime.service.ParticipanteReservaService;
import br.com.courttime.service.ReservaService;
import jakarta.validation.Valid;
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
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/solicitacoes/{reservaId}/participantes")
public class ParticipanteReservaController {

    private final ParticipanteReservaService participanteService;
    private final ReservaService reservaService;
    private final UsuarioRepository usuarioRepository;

    public ParticipanteReservaController(
            ParticipanteReservaService participanteService,
            ReservaService reservaService,
            UsuarioRepository usuarioRepository) {
        this.participanteService = participanteService;
        this.reservaService = reservaService;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping
    public String listar(
            @PathVariable Long reservaId,
            Authentication authentication,
            Model model) {
        Usuario usuario = obterUsuarioAutenticado(authentication);
        try {
            Reserva reserva = reservaService.obterSolicitacaoDoUsuario(reservaId, usuario);
            model.addAttribute("solicitacao", reserva);
            model.addAttribute("participantes", participanteService.listar(reservaId, usuario));
            model.addAttribute("resumo", participanteService.resumir(reservaId, usuario));
            model.addAttribute(
                    "edicaoPermitida",
                    participanteService.podeAlterar(reservaId, usuario));
            return "solicitacoes/participantes/lista";
        } catch (AccessDeniedException exception) {
            throw paginaNaoEncontrada();
        }
    }

    @GetMapping("/novo")
    public String exibirCadastro(
            @PathVariable Long reservaId,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {
        Usuario usuario = obterUsuarioAutenticado(authentication);
        Reserva reserva = obterSolicitacao(reservaId, usuario);
        if (!participanteService.podeAlterar(reservaId, usuario)) {
            return redirecionarEdicaoBloqueada(reservaId, redirectAttributes);
        }
        prepararFormulario(
                model, reserva, new ParticipanteReservaForm(), null, "Adicionar participante");
        return "solicitacoes/participantes/formulario";
    }

    @PostMapping
    public String adicionar(
            @PathVariable Long reservaId,
            @Valid @ModelAttribute("form") ParticipanteReservaForm form,
            BindingResult bindingResult,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {
        Usuario usuario = obterUsuarioAutenticado(authentication);
        Reserva reserva = obterSolicitacao(reservaId, usuario);
        if (!participanteService.podeAlterar(reservaId, usuario)) {
            return redirecionarEdicaoBloqueada(reservaId, redirectAttributes);
        }
        if (bindingResult.hasErrors()) {
            prepararFormulario(model, reserva, form, null, "Adicionar participante");
            return "solicitacoes/participantes/formulario";
        }
        try {
            participanteService.adicionar(
                    reservaId,
                    usuario,
                    form.getNomeCompleto(),
                    form.getRmOuRg(),
                    form.getCategoria(),
                    form.getVinculo());
            redirectAttributes.addFlashAttribute(
                    "mensagemSucesso", "Participante adicionado à solicitação.");
            return redirecionarLista(reservaId);
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("participante.invalido", exception.getMessage());
            prepararFormulario(model, reserva, form, null, "Adicionar participante");
            return "solicitacoes/participantes/formulario";
        } catch (IllegalStateException exception) {
            return redirecionarErro(reservaId, exception.getMessage(), redirectAttributes);
        } catch (AccessDeniedException exception) {
            throw paginaNaoEncontrada();
        }
    }

    @GetMapping("/{participanteId}/editar")
    public String exibirEdicao(
            @PathVariable Long reservaId,
            @PathVariable Long participanteId,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {
        Usuario usuario = obterUsuarioAutenticado(authentication);
        Reserva reserva = obterSolicitacao(reservaId, usuario);
        if (!participanteService.podeAlterar(reservaId, usuario)) {
            return redirecionarEdicaoBloqueada(reservaId, redirectAttributes);
        }
        try {
            ParticipanteReserva participante = participanteService.obter(
                    reservaId, participanteId, usuario);
            ParticipanteReservaForm form = new ParticipanteReservaForm();
            form.setNomeCompleto(participante.getNomeCompleto());
            form.setRmOuRg(participante.getRmOuRg());
            form.setCategoria(participante.getCategoria());
            form.setVinculo(participante.getVinculo());
            prepararFormulario(model, reserva, form, participanteId, "Editar participante");
            return "solicitacoes/participantes/formulario";
        } catch (AccessDeniedException exception) {
            throw paginaNaoEncontrada();
        }
    }

    @PostMapping("/{participanteId}")
    public String editar(
            @PathVariable Long reservaId,
            @PathVariable Long participanteId,
            @Valid @ModelAttribute("form") ParticipanteReservaForm form,
            BindingResult bindingResult,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {
        Usuario usuario = obterUsuarioAutenticado(authentication);
        Reserva reserva = obterSolicitacao(reservaId, usuario);
        if (!participanteService.podeAlterar(reservaId, usuario)) {
            return redirecionarEdicaoBloqueada(reservaId, redirectAttributes);
        }
        if (bindingResult.hasErrors()) {
            prepararFormulario(model, reserva, form, participanteId, "Editar participante");
            return "solicitacoes/participantes/formulario";
        }
        try {
            participanteService.editar(
                    reservaId,
                    participanteId,
                    usuario,
                    form.getNomeCompleto(),
                    form.getRmOuRg(),
                    form.getCategoria(),
                    form.getVinculo());
            redirectAttributes.addFlashAttribute(
                    "mensagemSucesso", "Participante atualizado.");
            return redirecionarLista(reservaId);
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("participante.invalido", exception.getMessage());
            prepararFormulario(model, reserva, form, participanteId, "Editar participante");
            return "solicitacoes/participantes/formulario";
        } catch (IllegalStateException exception) {
            return redirecionarErro(reservaId, exception.getMessage(), redirectAttributes);
        } catch (AccessDeniedException exception) {
            throw paginaNaoEncontrada();
        }
    }

    @PostMapping("/{participanteId}/remover")
    public String remover(
            @PathVariable Long reservaId,
            @PathVariable Long participanteId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        Usuario usuario = obterUsuarioAutenticado(authentication);
        try {
            participanteService.remover(reservaId, participanteId, usuario);
            redirectAttributes.addFlashAttribute(
                    "mensagemSucesso", "Participante removido da solicitação.");
            return redirecionarLista(reservaId);
        } catch (IllegalStateException exception) {
            return redirecionarErro(reservaId, exception.getMessage(), redirectAttributes);
        } catch (AccessDeniedException exception) {
            throw paginaNaoEncontrada();
        }
    }

    private void prepararFormulario(
            Model model,
            Reserva reserva,
            ParticipanteReservaForm form,
            Long participanteId,
            String titulo) {
        model.addAttribute("vinculos", VinculoParticipante.values());
        model.addAttribute("solicitacao", reserva);
        model.addAttribute("form", form);
        model.addAttribute("participanteId", participanteId);
        model.addAttribute("titulo", titulo);
        String base = "/solicitacoes/" + reserva.getId() + "/participantes";
        model.addAttribute(
                "acaoFormulario", participanteId == null ? base : base + "/" + participanteId);
    }

    private Reserva obterSolicitacao(Long reservaId, Usuario usuario) {
        try {
            return reservaService.obterSolicitacaoDoUsuario(reservaId, usuario);
        } catch (AccessDeniedException exception) {
            throw paginaNaoEncontrada();
        }
    }

    private Usuario obterUsuarioAutenticado(Authentication authentication) {
        return usuarioRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuário autenticado não encontrado"));
    }

    private String redirecionarEdicaoBloqueada(
            Long reservaId, RedirectAttributes redirectAttributes) {
        return redirecionarErro(
                reservaId,
                "Os participantes só podem ser alterados antes do envio ou após a devolução do documento.",
                redirectAttributes);
    }

    private String redirecionarErro(
            Long reservaId, String mensagem, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("mensagemErro", mensagem);
        return redirecionarLista(reservaId);
    }

    private String redirecionarLista(Long reservaId) {
        return "redirect:/solicitacoes/" + reservaId + "/participantes";
    }

    private ResponseStatusException paginaNaoEncontrada() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
}
