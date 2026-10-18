package br.com.courttime.repository;

import br.com.courttime.entity.DocumentoReserva;
import br.com.courttime.entity.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DocumentoReservaRepository extends JpaRepository<DocumentoReserva, Long> {

    List<DocumentoReserva> findByReservaOrderByVersaoDesc(Reserva reserva);

    Optional<DocumentoReserva> findByIdAndReserva(Long id, Reserva reserva);

    @Query("select coalesce(max(d.versao), 0) from DocumentoReserva d where d.reserva = :reserva")
    int obterMaiorVersao(@Param("reserva") Reserva reserva);
}
