package br.com.courttime.service;

import br.com.courttime.entity.AjusteContribuicaoApm;
import br.com.courttime.entity.ContribuicaoApm;

import java.util.List;

public record DetalheContribuicaoApm(
        ContribuicaoApm contribuicao,
        List<AjusteContribuicaoApm> ajustes) {

    public DetalheContribuicaoApm {
        ajustes = List.copyOf(ajustes);
    }
}
