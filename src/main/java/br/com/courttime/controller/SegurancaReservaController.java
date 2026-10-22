package br.com.courttime.controller;

import br.com.courttime.entity.CategoriaParticipante;
import br.com.courttime.entity.PresencaParticipante;
import br.com.courttime.entity.Usuario;
import br.com.courttime.entity.VinculoParticipante;
import br.com.courttime.security.UsuarioAtual;
import br.com.courttime.service.ConsultaSegurancaService;
import br.com.courttime.service.PortariaService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
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

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/seguranca/reservas")
public class SegurancaReservaController {

    private static final String PREFIXO_PRESENCA = "presenca-";

    private final ConsultaSegurancaService consultaSegurancaService;
    private final PortariaService portariaService;

    public SegurancaReservaController(
            ConsultaSegurancaService consultaSegurancaService,
            PortariaService portariaService) {
        this.consultaSegurancaService = consultaSegurancaService;
        this.portariaService = portariaService;
    }

    @GetMapping
    public String listar(
            @UsuarioAtual Usuario seguranca,
            Model model,
            HttpServletResponse response) {
        impedirCache(response);
        model.addAttribute(
                "reservas",
                consultaSegurancaService.listarAutorizadas(
                        seguranca));
        return "seguranca/reservas/lista";
    }

    @GetMapping("/{id}")
    public String detalhar(
            @PathVariable Long id,
            @UsuarioAtual Usuario seguranca,
            Model model,
            HttpServletResponse response) {
        impedirCache(response);
        try {
            model.addAttribute(
                    "detalhe",
                    consultaSegurancaService.detalhar(
                            id, seguranca));
            model.addAttribute("vinculos", VinculoParticipante.values());
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
            @UsuarioAtual Usuario seguranca,
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
                    id, seguranca, marcacoes);
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


    /** R10: pessoa que chegou e não estava na lista. Mesmos campos do cadastro do aluno. */
    @PostMapping("/{id}/pessoas")
    public String acrescentarPessoa(
            @PathVariable Long id,
            @RequestParam(required = false) String nomeCompleto,
            @RequestParam(required = false) String rmOuRg,
            @RequestParam(required = false) CategoriaParticipante categoria,
            @RequestParam(required = false) VinculoParticipante vinculo,
            @UsuarioAtual Usuario seguranca,
            RedirectAttributes redirectAttributes) {
        return executar(id, redirectAttributes, "Pessoa acrescentada e marcada como presente.",
                () -> portariaService.acrescentarPessoa(
                        id, seguranca, nomeCompleto, rmOuRg, categoria, vinculo));
    }

    /** R11: secretaria fechada; o valor a mais foi entregue à segurança. */
    @PostMapping("/{id}/pagamento")
    public String registrarPagamento(
            @PathVariable Long id,
            @UsuarioAtual Usuario seguranca,
            RedirectAttributes redirectAttributes) {
        return executar(id, redirectAttributes,
                "Valor recebido na portaria. Entregue o dinheiro à secretaria.",
                () -> portariaService.registrarPagamentoNaPortaria(id, seguranca));
    }

    /** R11: libera a quadra com pelo menos 10 presentes e nada a pagar. */
    @PostMapping("/{id}/liberar")
    public String liberar(
            @PathVariable Long id,
            @UsuarioAtual Usuario seguranca,
            RedirectAttributes redirectAttributes) {
        return executar(id, redirectAttributes, "Quadra liberada.",
                () -> portariaService.liberarQuadra(id, seguranca));
    }

    /** Executa a ação e volta para a conferência com a mensagem de sucesso ou de erro. */
    private String executar(
            Long id, RedirectAttributes redirectAttributes, String sucesso, Runnable acao) {
        try {
            acao.run();
            redirectAttributes.addFlashAttribute("mensagemSucesso", sucesso);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("mensagemErro", exception.getMessage());
        }
        return "redirect:/seguranca/reservas/" + id;
    }

    private void impedirCache(HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
    }
}
