package br.com.courttime.controller;

import br.com.courttime.entity.DocumentoReserva;
import br.com.courttime.entity.Usuario;
import br.com.courttime.security.UsuarioAtual;
import br.com.courttime.service.ConferenciaDocumentoService;
import br.com.courttime.service.DocumentoReservaService;
import br.com.courttime.service.DownloadDocumento;
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
@RequestMapping("/secretaria/documentos")
public class SecretariaDocumentoController {

    private final ConferenciaDocumentoService conferenciaService;

    public SecretariaDocumentoController(
            ConferenciaDocumentoService conferenciaService) {
        this.conferenciaService = conferenciaService;
    }

    @GetMapping
    public String listar(@UsuarioAtual Usuario secretaria, Model model) {
        model.addAttribute("pendentes", conferenciaService.listarPendentes(secretaria));
        model.addAttribute("historico", conferenciaService.listarHistorico(secretaria));
        return "secretaria/documentos/lista";
    }

    @GetMapping("/{documentoId}")
    public String detalhar(
            @PathVariable Long documentoId,
            @UsuarioAtual Usuario secretaria,
            Model model) {
        try {
            DocumentoReserva documento = conferenciaService.obter(documentoId, secretaria);
            model.addAttribute("documento", documento);
            model.addAttribute(
                    "conferenciaPermitida",
                    conferenciaService.podeConferir(documento, secretaria));
            model.addAttribute(
                    "prazoReenvioEncerrado",
                    DocumentoReservaService.prazoDeEnvioEncerrado(documento.getReserva()));
            model.addAttribute(
                    "outrosSemVinculo",
                    conferenciaService.contarOutrosSemVinculo(documento, secretaria));
            return "secretaria/documentos/detalhe";
        } catch (IllegalArgumentException exception) {
            throw paginaNaoEncontrada();
        }
    }

    @GetMapping("/{documentoId}/baixar")
    public ResponseEntity<org.springframework.core.io.Resource> baixar(
            @PathVariable Long documentoId,
            @UsuarioAtual Usuario secretaria) {
        try {
            DownloadDocumento download = conferenciaService.baixar(documentoId, secretaria);
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
        } catch (IllegalArgumentException exception) {
            throw paginaNaoEncontrada();
        }
    }

    @PostMapping("/{documentoId}/validar")
    public String validar(
            @PathVariable Long documentoId,
            @RequestParam(defaultValue = "false") boolean assinaturaGovBrConferida,
            @UsuarioAtual Usuario secretaria,
            RedirectAttributes redirectAttributes) {
        try {
            conferenciaService.validar(documentoId, secretaria, assinaturaGovBrConferida);
            redirectAttributes.addFlashAttribute(
                    "mensagemSucesso", "Documento conferido e validado.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("mensagemErro", exception.getMessage());
        }
        return "redirect:/secretaria/documentos";
    }

    @PostMapping("/{documentoId}/devolver")
    public String devolver(
            @PathVariable Long documentoId,
            @RequestParam(required = false) String justificativa,
            @UsuarioAtual Usuario secretaria,
            RedirectAttributes redirectAttributes) {
        try {
            conferenciaService.devolver(documentoId, secretaria, justificativa);
            redirectAttributes.addFlashAttribute(
                    "mensagemSucesso", "Documento devolvido para correção.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("mensagemErro", exception.getMessage());
            return "redirect:/secretaria/documentos/" + documentoId;
        }
        return "redirect:/secretaria/documentos";
    }


    private ResponseStatusException paginaNaoEncontrada() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
}
