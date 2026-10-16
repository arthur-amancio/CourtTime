package br.com.courttime.repository;

import br.com.courttime.entity.BloqueioQuadra;
import br.com.courttime.entity.Quadra;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface BloqueioQuadraRepository extends JpaRepository<BloqueioQuadra, Long> {

    List<BloqueioQuadra> findByQuadraAndDataAndAtivoTrueOrderByHorarioInicio(
            Quadra quadra, LocalDate data);
}
