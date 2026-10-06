package br.com.courttime.controller;

import br.com.courttime.entity.Quadra;
import br.com.courttime.service.QuadraService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class QuadraController {

    private final QuadraService quadraService;

    public QuadraController(QuadraService quadraService) {
        this.quadraService = quadraService;
    }

    @GetMapping("/quadra")
    public String consultarQuadra(Model model) {
        Quadra quadra = quadraService.obterQuadra();
        model.addAttribute("quadra", quadra);
        model.addAttribute("disponibilidades", quadraService.obterDisponibilidadesAtivas(quadra));
        return "quadra";
    }
}
