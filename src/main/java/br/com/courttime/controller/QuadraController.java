package br.com.courttime.controller;

import br.com.courttime.entity.Quadra;
import br.com.courttime.service.CalendarioDisponibilidadeService;
import br.com.courttime.service.ConsultaDisponibilidade;
import br.com.courttime.service.QuadraService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Controller
public class QuadraController {

    private final QuadraService quadraService;
    private final CalendarioDisponibilidadeService calendarioDisponibilidadeService;

    public QuadraController(
            QuadraService quadraService,
            CalendarioDisponibilidadeService calendarioDisponibilidadeService) {
        this.quadraService = quadraService;
        this.calendarioDisponibilidadeService = calendarioDisponibilidadeService;
    }

    @GetMapping("/quadra")
    public String consultarQuadra(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            Model model) {
        Quadra quadra = quadraService.obterQuadra();
        LocalDate dataSelecionada = data == null
                ? calendarioDisponibilidadeService.obterDataAtual()
                : data;
        ConsultaDisponibilidade consulta = calendarioDisponibilidadeService
                .consultar(quadra, dataSelecionada);

        model.addAttribute("quadra", quadra);
        model.addAttribute("consulta", consulta);
        model.addAttribute("dataAnterior", dataSelecionada.minusDays(1));
        model.addAttribute("dataSeguinte", dataSelecionada.plusDays(1));
        model.addAttribute("calendario", CalendarioVisual.montar(
                dataSelecionada,
                calendarioDisponibilidadeService.obterDataAtual(),
                null,
                calendarioDisponibilidadeService.obterDiasComFuncionamento(quadra)));
        return "quadra";
    }
}
