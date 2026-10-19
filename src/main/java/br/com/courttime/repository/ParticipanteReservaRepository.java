package br.com.courttime.repository;

import br.com.courttime.entity.CategoriaParticipante;
import br.com.courttime.entity.ParticipanteReserva;
import br.com.courttime.entity.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ParticipanteReservaRepository extends JpaRepository<ParticipanteReserva, Long> {

    List<ParticipanteReserva> findByReservaOrderByNomeCompletoAscIdAsc(Reserva reserva);

    Optional<ParticipanteReserva> findByIdAndReserva(Long id, Reserva reserva);

    boolean existsByReservaAndRmOuRgIgnoreCase(Reserva reserva, String rmOuRg);

    boolean existsByReservaAndRmOuRgIgnoreCaseAndIdNot(
            Reserva reserva, String rmOuRg, Long id);

    long countByReservaAndCategoria(Reserva reserva, CategoriaParticipante categoria);

    long countByReserva(Reserva reserva);
}
