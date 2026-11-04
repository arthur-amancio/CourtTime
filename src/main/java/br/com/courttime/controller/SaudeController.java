package br.com.courttime.controller;

import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SaudeController {

    private final ApplicationAvailability disponibilidade;

    public SaudeController(ApplicationAvailability disponibilidade) {
        this.disponibilidade = disponibilidade;
    }

    @GetMapping(value = "/healthz", produces = "text/plain")
    public ResponseEntity<String> verificar() {
        boolean pronto = disponibilidade.getReadinessState() == ReadinessState.ACCEPTING_TRAFFIC;
        return ResponseEntity.status(pronto ? 200 : 503)
                .cacheControl(CacheControl.noStore()).body(pronto ? "ok" : "indisponível");
    }
}
