package br.com.courttime.controller;

import br.com.courttime.entity.Reserva;
import br.com.courttime.entity.Usuario;
import br.com.courttime.repository.UsuarioRepository;
import br.com.courttime.service.DocumentoReservaService;
import br.com.courttime.service.DownloadDocumento;
import br.com.courttime.service.ReservaService;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping("/solicitacoes/{reservaId}/documentos")
public class DocumentoReservaController {

    private final DocumentoReservaService documentoService;
    private final ReservaService reservaService;
    private final UsuarioRepository usuarioRepository;

    public DocumentoReservaController(
            DocumentoReservaService documentoService,
            ReservaService reservaService,
            UsuarioRepository usuarioRepository) {
        this.documentoService = documentoService;
        this.reservaService = reservaService;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping
    public String listar(
            @PathVariable Long reservaId,
            Authentication authentication,
            Model model) {
        Usuario usuario = obterUsuarioAutenticado(authentication);
        try {
            Reserva reserva = reservaService.obterSolicitacaoDoUsuario(reservaId, usuario);
            model.addAttribute("solicitacao", reserva);
            var documentos = documentoService.listar(reservaId, usuario);
            model.addAttribute("documentos", documentos);
            model.addAttribute(
                    "versaoMaisRecente",
                    documentos.isEmpty() ? 0 : documentos.get(0).getVersao());
            model.addAttribute(
                    "envioPermitido",
                    documentoService.podeEnviar(reservaId, usuario));
            model.addAttribute("prazoEnvio", DocumentoReservaService.prazoFinalDeEnvio(reserva));
            model.addAttribute(
                    "prazoEncerrado", DocumentoReservaService.prazoDeEnvioEncerrado(reserva));
            return "solicitacoes/documentos/lista";
        } catch (AccessDeniedException exception) {
            throw paginaNaoEncontrada();
        }
    }

    @PostMapping
    public String enviar(
            @PathVariable Long reservaId,
            @RequestParam("arquivo") MultipartFile arquivo,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        Usuario usuario = obterUsuarioAutenticado(authentication);
        try {
            documentoService.enviar(
                    reservaId,
                    usuario,
                    arquivo.getOriginalFilename(),
                    arquivo.getContentType(),
                    arquivo.getBytes());
            redirectAttributes.addFlashAttribute(
                    "mensagemSucesso",
                    "PDF enviado como nova versão e aguardando conferência institucional.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("mensagemErro", exception.getMessage());
        } catch (AccessDeniedException exception) {
            throw paginaNaoEncontrada();
        } catch (IOException exception) {
            redirectAttributes.addFlashAttribute(
                    "mensagemErro", "Não foi possível ler o arquivo enviado.");
        }
        return redirecionarLista(reservaId);
    }

    @GetMapping("/{documentoId}/baixar")
    public ResponseEntity<org.springframework.core.io.Resource> baixar(
            @PathVariable Long reservaId,
            @PathVariable Long documentoId,
            Authentication authentication) {
        Usuario usuario = obterUsuarioAutenticado(authentication);
        try {
            DownloadDocumento download = documentoService.baixar(
                    reservaId, documentoId, usuario);
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
        } catch (AccessDeniedException exception) {
            throw paginaNaoEncontrada();
        }
    }

    private Usuario obterUsuarioAutenticado(Authentication authentication) {
        return usuarioRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuário autenticado não encontrado"));
    }

    private String redirecionarLista(Long reservaId) {
        return "redirect:/solicitacoes/" + reservaId + "/documentos";
    }

    private ResponseStatusException paginaNaoEncontrada() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
}
