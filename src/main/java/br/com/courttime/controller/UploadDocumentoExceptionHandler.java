package br.com.courttime.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.nio.charset.StandardCharsets;

@ControllerAdvice
public class UploadDocumentoExceptionHandler {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<String> tratarArquivoMuitoGrande() {
        MediaType textoUtf8 = new MediaType(
                MediaType.TEXT_PLAIN, StandardCharsets.UTF_8);
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .cacheControl(CacheControl.noStore())
                .contentType(textoUtf8)
                .body("O arquivo PDF deve possuir no máximo 10 MB.");
    }
}
