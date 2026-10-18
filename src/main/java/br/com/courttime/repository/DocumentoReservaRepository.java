package br.com.courttime.repository;

import br.com.courttime.entity.DocumentoReserva;
import br.com.courttime.entity.Reserva;
import br.com.courttime.entity.StatusReserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DocumentoReservaRepository extends JpaRepository<DocumentoReserva, Long> {

    List<DocumentoReserva> findByReservaOrderByVersaoDesc(Reserva reserva);

    Optional<DocumentoReserva> findByIdAndReserva(Long id, Reserva reserva);

    @Query("select d.reserva.id from DocumentoReserva d where d.id = :id")
    Optional<Long> obterReservaId(@Param("id") Long id);

    @Query("select coalesce(max(d.versao), 0) from DocumentoReserva d where d.reserva = :reserva")
    int obterMaiorVersao(@Param("reserva") Reserva reserva);

    @Query("""
            select d from DocumentoReserva d
            left join fetch d.conferencia c
            left join fetch c.conferidoPor
            where d.reserva = :reserva
              and d.versao = (
                select max(d2.versao) from DocumentoReserva d2 where d2.reserva = :reserva
              )
            """)
    Optional<DocumentoReserva> obterVersaoMaisRecenteComConferencia(
            @Param("reserva") Reserva reserva);

    @Query("""
            select d from DocumentoReserva d
            join fetch d.reserva r
            join fetch r.solicitante
            where d.versao = (
                select max(d2.versao) from DocumentoReserva d2 where d2.reserva = r
            )
              and r.status = :status
              and not exists (
                select c.id from ConferenciaDocumento c where c.documento = d
            )
            order by d.enviadoEm asc
            """)
    List<DocumentoReserva> listarVersoesMaisRecentesPendentes(
            @Param("status") StatusReserva status);

    @Query("""
            select d from DocumentoReserva d
            join fetch d.reserva r
            join fetch r.solicitante
            where d.id = :id
            """)
    Optional<DocumentoReserva> obterComSolicitacao(@Param("id") Long id);
}
