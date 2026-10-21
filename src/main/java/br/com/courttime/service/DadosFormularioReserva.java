package br.com.courttime.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * O que vai escrito no formulário oficial. CPF e curso chegam digitados pelo
 * aluno no momento da geração e não são gravados em lugar nenhum.
 */
public record DadosFormularioReserva(
        String nomeSolicitante,
        String curso,
        String cpf,
        LocalDate dataUtilizacao,
        LocalTime horarioInicio,
        LocalTime horarioFim,
        LocalDate dataPreenchimento,
        List<Participante> participantes) {

    /** vinculo: texto escrito em "( ) Outro: ____"; nulo para Aluno ou sem vínculo. */
    public record Participante(String nomeCompleto, String rmOuRg, boolean aluno, String vinculo) {

        public Participante(String nomeCompleto, String rmOuRg, boolean aluno) {
            this(nomeCompleto, rmOuRg, aluno, null);
        }
    }
}
