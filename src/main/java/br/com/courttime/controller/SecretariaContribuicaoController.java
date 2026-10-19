package br.com.courttime.controller;

import br.com.courttime.entity.Usuario;
import br.com.courttime.repository.UsuarioRepository;
import br.com.courttime.service.ContribuicaoApmService;
import br.com.courttime.service.DetalheContribuicaoApm;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/secretaria/contribuicoes")
public class SecretariaContribuicaoController {

    private final ContribuicaoApmService contribuicaoService;
    private final UsuarioRepository usuarioRepository;

    public SecretariaContribuicaoController(
            ContribuicaoApmService contribuicaoService,
            UsuarioRepository usuarioRepository) {
        this.contribuicaoService = contribuicaoService;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping
    public String listar(Authentication authentication, Model model) {
        Usuario secretaria = obterUsuarioAutenticado(authentication);
        model.addAttribute("pendentes", contribuicaoService.listarPendentes(secretaria));
        model.addAttribute("registradas", contribuicaoService.listarRegistradas(secretaria));
        return "secretaria/contribuicoes/lista";
    }

    @GetMapping("/solicitacoes/{reservaId}")
    public String detalhar(
            @PathVariable Long reservaId,
            Authentication authentication,
            Model model) {
        Usuario secretaria = obterUsuarioAutenticado(authentication);
        try {
            model.addAttribute(
                    "detalhe",
                    contribuicaoService.obterPorReserva(reservaId, secretaria));
            return "secretaria/contribuicoes/detalhe";
        } catch (IllegalArgumentException exception) {
            throw paginaNaoEncontrada();
        }
    }

    @PostMapping("/solicitacoes/{reservaId}")
    public String registrar(
            @PathVariable Long reservaId,
            @RequestParam(required = false) String valor,
            @RequestParam(required = false) String observacoes,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        Usuario secretaria = obterUsuarioAutenticado(authentication);
        try {
            DetalheContribuicaoApm detalhe = contribuicaoService.registrar(
                    reservaId, secretaria, valor, observacoes);
            redirectAttributes.addFlashAttribute(
                    "mensagemSucesso", "Contribuição presencial registrada.");
            return redirecionarDetalhe(detalhe);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("mensagemErro", exception.getMessage());
            return "redirect:/secretaria/contribuicoes";
        }
    }

    @PostMapping("/{contribuicaoId}/ajustes")
    public String ajustar(
            @PathVariable Long contribuicaoId,
            @RequestParam(required = false) String novoValor,
            @RequestParam(required = false) String novasObservacoes,
            @RequestParam(required = false) String motivo,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        Usuario secretaria = obterUsuarioAutenticado(authentication);
        try {
            DetalheContribuicaoApm detalhe = contribuicaoService.ajustar(
                    contribuicaoId,
                    secretaria,
                    novoValor,
                    novasObservacoes,
                    motivo);
            redirectAttributes.addFlashAttribute(
                    "mensagemSucesso", "Ajuste registrado no histórico.");
            return redirecionarDetalhe(detalhe);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("mensagemErro", exception.getMessage());
            return "redirect:/secretaria/contribuicoes";
        }
    }

    private String redirecionarDetalhe(DetalheContribuicaoApm detalhe) {
        return "redirect:/secretaria/contribuicoes/solicitacoes/"
                + detalhe.contribuicao().getReserva().getId();
    }

    private Usuario obterUsuarioAutenticado(Authentication authentication) {
        return usuarioRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuário autenticado não encontrado"));
    }

    private ResponseStatusException paginaNaoEncontrada() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
}
