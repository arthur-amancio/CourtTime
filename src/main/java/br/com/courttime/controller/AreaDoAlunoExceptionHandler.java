package br.com.courttime.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.io.IOException;

/**
 * Nas telas do aluno, uma solicitação de outro aluno responde "página não
 * encontrada" (404), e não "proibido" (403). Assim ninguém descobre se o
 * número de outra solicitação existe.
 *
 * Os serviços lançam AccessDeniedException; esta classe faz a conversão em um
 * lugar só, em vez de um try/catch em cada método dos controllers abaixo.
 * As rotas de outros perfis continuam bloqueadas antes, pelo SecurityConfig.
 */
@ControllerAdvice(assignableTypes = {
        SolicitacaoReservaController.class,
        ParticipanteReservaController.class,
        DocumentoReservaController.class,
        FormularioOficialController.class
})
public class AreaDoAlunoExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public void solicitacaoDeOutroAluno(HttpServletResponse response) throws IOException {
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
    }
}
