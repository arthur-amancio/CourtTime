package br.com.courttime.controller;

import br.com.courttime.entity.Usuario;
import br.com.courttime.security.UsuarioAtual;
import br.com.courttime.service.AnaliseAdministrativaService;
import br.com.courttime.service.DetalheAnaliseAdministrativa;
import br.com.courttime.service.DownloadDocumento;
import br.com.courttime.service.ReservaService;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping("/admin/solicitacoes")
public class AdminSolicitacaoController {

    private final AnaliseAdministrativaService analiseService;
    private final ReservaService reservaService;

    public AdminSolicitacaoController(
            AnaliseAdministrativaService analiseService,
            ReservaService reservaService) {
        this.analiseService = analiseService;
        this.reservaService = reservaService;
    }

    @GetMapping
    public String listar(@UsuarioAtual Usuario administrador, Model model) {
        model.addAttribute("solicitacoes", analiseService.listarPendentes(administrador));
        model.addAttribute(
                "reservasAutorizadas",
                analiseService.listarAutorizadasAtuais(administrador));
        model.addAttribute(
                "solicitacoesEncerradas",
                analiseService.listarEncerradas(administrador));
        return "admin/solicitacoes/lista";
    }

    @GetMapping("/{id}")
    public String detalhar(
            @PathVariable Long id,
            @UsuarioAtual Usuario administrador,
            Model model) {
        try {
            DetalheAnaliseAdministrativa detalhe =
                    analiseService.detalhar(id, administrador);
            model.addAttribute("detalhe", detalhe);
            // Se o aluno pediu outro horário depois do cancelamento, a administração vê o vínculo.
            model.addAttribute(
                    "remarcacao",
                    reservaService.obterRemarcacao(detalhe.solicitacao()).orElse(null));
            return "admin/solicitacoes/detalhe";
        } catch (IllegalArgumentException exception) {
            throw paginaNaoEncontrada();
        }
    }

    @GetMapping("/{reservaId}/documentos/{documentoId}/baixar")
    public ResponseEntity<org.springframework.core.io.Resource> baixar(
            @PathVariable Long reservaId,
            @PathVariable Long documentoId,
            @UsuarioAtual Usuario administrador) {
        try {
            DownloadDocumento download = analiseService.baixar(
                    reservaId, documentoId, administrador);
            ContentDisposition disposicao = ContentDisposition.attachment()
                    .filename(download.nomeArquivo(), StandardCharsets.UTF_8)
                    .build();
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.noStore())
                    .header(HttpHeaders.CONTENT_DISPOSITION, disposicao.toString())
                    .header("X-Content-Type-Options", "nosniff")
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(download.tamanhoBytes())
                    .body(download.conteudo());
        } catch (IllegalArgumentException | AccessDeniedException exception) {
            throw paginaNaoEncontrada();
        }
    }

    @PostMapping("/{id}/aprovar")
    public String aprovar(
            @PathVariable Long id,
            @UsuarioAtual Usuario administrador,
            RedirectAttributes redirectAttributes) {
        try {
            reservaService.aprovar(id, administrador);
            redirectAttributes.addFlashAttribute(
                    "mensagemSucesso", "Solicitação aprovada e reserva autorizada.");
            return "redirect:/admin/solicitacoes";
        } catch (IllegalArgumentException | IllegalStateException | AccessDeniedException exception) {
            redirectAttributes.addFlashAttribute("mensagemErro", exception.getMessage());
            return redirecionarDetalhe(id);
        }
    }

    @PostMapping("/{id}/rejeitar")
    public String rejeitar(
            @PathVariable Long id,
            @RequestParam(required = false) String motivo,
            @UsuarioAtual Usuario administrador,
            RedirectAttributes redirectAttributes) {
        try {
            reservaService.rejeitar(
                    id, administrador, motivo);
            redirectAttributes.addFlashAttribute(
                    "mensagemSucesso", "Solicitação rejeitada.");
            return "redirect:/admin/solicitacoes";
        } catch (IllegalArgumentException | IllegalStateException | AccessDeniedException exception) {
            redirectAttributes.addFlashAttribute("mensagemErro", exception.getMessage());
            return redirecionarDetalhe(id);
        }
    }

    @PostMapping("/{id}/cancelar")
    public String cancelar(
            @PathVariable Long id,
            @RequestParam(required = false) String motivo,
            @UsuarioAtual Usuario administrador,
            RedirectAttributes redirectAttributes) {
        try {
            reservaService.cancelarInstitucionalmente(
                    id, administrador, motivo);
            redirectAttributes.addFlashAttribute(
                    "mensagemSucesso",
                    "Reserva cancelada por motivo institucional.");
            return "redirect:/admin/solicitacoes";
        } catch (IllegalArgumentException | IllegalStateException | AccessDeniedException exception) {
            redirectAttributes.addFlashAttribute("mensagemErro", exception.getMessage());
            return redirecionarDetalhe(id);
        }
    }


    private String redirecionarDetalhe(Long id) {
        return "redirect:/admin/solicitacoes/" + id;
    }

    private ResponseStatusException paginaNaoEncontrada() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
}
