package br.com.courttime.controller;

import br.com.courttime.entity.Usuario;
import br.com.courttime.security.UsuarioAtual;
import br.com.courttime.service.FormularioOficialService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.support.RequestContextUtils;

import java.net.URI;

/**
 * Devolve o formulário oficial preenchido para download. É um POST para que o
 * CPF viaje no corpo da requisição, e não na URL ou no histórico do navegador.
 */
@Controller
@RequestMapping("/solicitacoes/{reservaId}/formulario-oficial")
public class FormularioOficialController {

    private final FormularioOficialService formularioService;

    public FormularioOficialController(
            FormularioOficialService formularioService) {
        this.formularioService = formularioService;
    }

    @PostMapping
    public ResponseEntity<byte[]> gerar(
            @PathVariable Long reservaId,
            @RequestParam(required = false) String cpf,
            @RequestParam(required = false) String curso,
            @UsuarioAtual Usuario usuario,
            HttpServletRequest request,
            HttpServletResponse response) {
        try {
            byte[] pdf = formularioService.gerar(reservaId, usuario, cpf, curso);
            ContentDisposition disposicao = ContentDisposition.attachment()
                    .filename("formulario-reserva-quadra-solicitacao-" + reservaId + ".pdf")
                    .build();
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.noStore())
                    .header(HttpHeaders.CONTENT_DISPOSITION, disposicao.toString())
                    .header("X-Content-Type-Options", "nosniff")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            // Volta para a tela de documentos com a mensagem, como os outros formulários.
            String destino = "/solicitacoes/" + reservaId + "/documentos";
            RequestContextUtils.getOutputFlashMap(request).put("mensagemErro", exception.getMessage());
            RequestContextUtils.saveOutputFlashMap(destino, request, response);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(request.getContextPath() + destino))
                    .build();
        }
    }
}
