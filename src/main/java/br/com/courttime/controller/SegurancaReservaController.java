package br.com.courttime.controller;

import br.com.courttime.entity.PresencaParticipante;
import br.com.courttime.entity.Usuario;
import br.com.courttime.repository.UsuarioRepository;
import br.com.courttime.service.ConsultaSegurancaService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
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

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/seguranca/reservas")
public class SegurancaReservaController {

    private static final String PREFIXO_PRESENCA = "presenca-";

    private final ConsultaSegurancaService consultaSegurancaService;
    private final UsuarioRepository usuarioRepository;

    public SegurancaReservaController(
            ConsultaSegurancaService consultaSegurancaService,
            UsuarioRepository usuarioRepository) {
        this.consultaSegurancaService = consultaSegurancaService;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping
    public String listar(
            Authentication authentication,
            Model model,
            HttpServletResponse response) {
        impedirCache(response);
        model.addAttribute(
                "reservas",
                consultaSegurancaService.listarAutorizadas(
                        obterUsuarioAutenticado(authentication)));
        return "seguranca/reservas/lista";
    }

    @GetMapping("/{id}")
    public String detalhar(
            @PathVariable Long id,
            Authentication authentication,
            Model model,
            HttpServletResponse response) {
        impedirCache(response);
        try {
            model.addAttribute(
                    "detalhe",
                    consultaSegurancaService.detalhar(
                            id, obterUsuarioAutenticado(authentication)));
            return "seguranca/reservas/detalhe";
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    /** R08.1: campos "presenca-{id do participante}" com PRESENTE ou AUSENTE. */
    @PostMapping("/{id}/presencas")
    public String registrarPresencas(
            @PathVariable Long id,
            @RequestParam Map<String, String> parametros,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        Map<Long, PresencaParticipante> marcacoes = new HashMap<>();
        try {
            parametros.forEach((nome, valor) -> {
                if (nome.startsWith(PREFIXO_PRESENCA) && valor != null && !valor.isBlank()) {
                    marcacoes.put(
                            Long.valueOf(nome.substring(PREFIXO_PRESENCA.length())),
                            PresencaParticipante.valueOf(valor));
                }
            });
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        try {
            int alteradas = consultaSegurancaService.registrarPresencas(
                    id, obterUsuarioAutenticado(authentication), marcacoes);
            redirectAttributes.addFlashAttribute(
                    "mensagemSucesso",
                    alteradas == 0 ? "Nenhuma marcação mudou." : "Presença registrada.");
        } catch (IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("mensagemErro", exception.getMessage());
        } catch (IllegalArgumentException exception) {
            if (marcacoes.isEmpty()) {
                redirectAttributes.addFlashAttribute("mensagemErro", exception.getMessage());
            } else {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
        }
        return "redirect:/seguranca/reservas/" + id;
    }

    private Usuario obterUsuarioAutenticado(Authentication authentication) {
        return usuarioRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuário autenticado não encontrado"));
    }

    private void impedirCache(HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
    }
}
