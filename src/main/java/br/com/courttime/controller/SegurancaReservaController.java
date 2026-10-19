package br.com.courttime.controller;

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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/seguranca/reservas")
public class SegurancaReservaController {

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

    private Usuario obterUsuarioAutenticado(Authentication authentication) {
        return usuarioRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuário autenticado não encontrado"));
    }

    private void impedirCache(HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
    }
}
