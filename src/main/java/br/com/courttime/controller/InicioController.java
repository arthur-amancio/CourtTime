package br.com.courttime.controller;

import br.com.courttime.entity.Usuario;
import br.com.courttime.repository.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class InicioController {

    private final UsuarioRepository usuarioRepository;

    public InicioController(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping("/inicio")
    public String inicio(Authentication authentication, Model model) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException("Usuário autenticado não encontrado"));

        model.addAttribute("nome", usuario.getNome());
        model.addAttribute("perfil", usuario.getPerfil());
        return "inicio";
    }
}
