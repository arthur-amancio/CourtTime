package br.com.courttime.repository;

import br.com.courttime.entity.DisponibilidadeQuadra;
import br.com.courttime.entity.DiaSemana;
import br.com.courttime.entity.Quadra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DisponibilidadeQuadraRepository extends JpaRepository<DisponibilidadeQuadra, Long> {

    List<DisponibilidadeQuadra> findByQuadraAndAtivaTrue(Quadra quadra);

    List<DisponibilidadeQuadra> findByQuadraOrderByDiaSemanaAscHorarioInicioAsc(Quadra quadra);

    List<DisponibilidadeQuadra> findByQuadraAndDiaSemanaOrderByHorarioInicioAsc(
            Quadra quadra, DiaSemana diaSemana);

    List<DisponibilidadeQuadra> findByQuadraAndDiaSemanaAndAtivaTrueOrderByHorarioInicio(
            Quadra quadra, DiaSemana diaSemana);

    @Query("select d from DisponibilidadeQuadra d join fetch d.quadra where d.id = :id")
    java.util.Optional<DisponibilidadeQuadra> findByIdComQuadra(@Param("id") Long id);
}
