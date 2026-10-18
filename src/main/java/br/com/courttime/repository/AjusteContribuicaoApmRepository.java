package br.com.courttime.repository;

import br.com.courttime.entity.AjusteContribuicaoApm;
import br.com.courttime.entity.ContribuicaoApm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AjusteContribuicaoApmRepository
        extends JpaRepository<AjusteContribuicaoApm, Long> {

    @Query("""
            select a from AjusteContribuicaoApm a
            join fetch a.ajustadaPor
            where a.contribuicao = :contribuicao
            order by a.ajustadaEm desc, a.id desc
            """)
    List<AjusteContribuicaoApm> listarHistorico(
            @Param("contribuicao") ContribuicaoApm contribuicao);
}
