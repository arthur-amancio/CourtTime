package br.com.courttime.repository;

import br.com.courttime.entity.BloqueioSemanalQuadra;
import br.com.courttime.entity.DiaSemana;
import br.com.courttime.entity.Quadra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

/** Consulta regras, e não uma tabela de ocorrências semanais previamente geradas. */
public interface BloqueioSemanalQuadraRepository extends JpaRepository<BloqueioSemanalQuadra, Long> {
    /** Inclui regras desativadas e vencidas para a consulta administrativa. */
    List<BloqueioSemanalQuadra> findByQuadraOrderByVigenciaInicioDescHorarioInicioAsc(Quadra quadra);

    /** Filtra no banco a quadra, o dia e os limites inclusivos da vigência. */
    @Query("""
            select b from BloqueioSemanalQuadra b
            where b.quadra = :quadra and b.ativo = true and b.diaSemana = :dia
              and b.vigenciaInicio <= :data and b.vigenciaFim >= :data
            order by b.horarioInicio
            """)
    List<BloqueioSemanalQuadra> listarAtivosNaData(
            @Param("quadra") Quadra quadra, @Param("dia") DiaSemana dia, @Param("data") LocalDate data);
}
