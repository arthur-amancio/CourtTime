package br.com.courttime.repository;

import br.com.courttime.entity.CategoriaParticipante;
import br.com.courttime.entity.ParticipanteReserva;
import br.com.courttime.entity.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ParticipanteReservaRepository extends JpaRepository<ParticipanteReserva, Long> {

    List<ParticipanteReserva> findByReservaOrderByNomeCompletoAscIdAsc(Reserva reserva);

    /** R08.4: lista da administração, já com quem marcou a presença. */
    @Query("""
            select p from ParticipanteReserva p
            left join fetch p.presencaRegistradaPor
            where p.reserva = :reserva
            order by p.nomeCompleto asc, p.id asc
            """)
    List<ParticipanteReserva> listarComPresenca(@Param("reserva") Reserva reserva);

    Optional<ParticipanteReserva> findByIdAndReserva(Long id, Reserva reserva);

    boolean existsByReservaAndRmOuRgIgnoreCase(Reserva reserva, String rmOuRg);

    boolean existsByReservaAndRmOuRgIgnoreCaseAndIdNot(
            Reserva reserva, String rmOuRg, Long id);

    long countByReservaAndCategoria(Reserva reserva, CategoriaParticipante categoria);

    long countByReserva(Reserva reserva);

    long countByReservaAndCategoriaAndVinculoIsNull(Reserva reserva, CategoriaParticipante categoria);
}
