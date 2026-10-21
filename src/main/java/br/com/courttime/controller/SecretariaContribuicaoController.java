package br.com.courttime.controller;

import br.com.courttime.entity.Usuario;
import br.com.courttime.security.UsuarioAtual;
import br.com.courttime.service.ContribuicaoApmService;
import br.com.courttime.service.DetalheContribuicaoApm;
import org.springframework.http.HttpStatus;
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

    public SecretariaContribuicaoController(
            ContribuicaoApmService contribuicaoService) {
        this.contribuicaoService = contribuicaoService;
    }

    @GetMapping
    public String listar(@UsuarioAtual Usuario secretaria, Model model) {
        model.addAttribute("pendentes", contribuicaoService.listarPendentes(secretaria));
        model.addAttribute("registradas", contribuicaoService.listarRegistradas(secretaria));
        return "secretaria/contribuicoes/lista";
    }

    @GetMapping("/solicitacoes/{reservaId}")
    public String detalhar(
            @PathVariable Long reservaId,
            @UsuarioAtual Usuario secretaria,
            Model model) {
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
            @RequestParam(required = false) String observacoes,
            @UsuarioAtual Usuario secretaria,
            RedirectAttributes redirectAttributes) {
        try {
            DetalheContribuicaoApm detalhe = contribuicaoService.registrar(
                    reservaId, secretaria, observacoes);
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
            @UsuarioAtual Usuario secretaria,
            RedirectAttributes redirectAttributes) {
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


    private ResponseStatusException paginaNaoEncontrada() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
}
