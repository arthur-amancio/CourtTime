package br.com.courttime.repository;

import br.com.courttime.entity.DisponibilidadeQuadra;
import br.com.courttime.entity.Quadra;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DisponibilidadeQuadraRepository extends JpaRepository<DisponibilidadeQuadra, Long> {

    List<DisponibilidadeQuadra> findByQuadraAndAtivaTrue(Quadra quadra);
}
