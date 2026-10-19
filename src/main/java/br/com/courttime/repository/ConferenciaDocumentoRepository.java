package br.com.courttime.repository;

import br.com.courttime.entity.ConferenciaDocumento;
import br.com.courttime.entity.DocumentoReserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ConferenciaDocumentoRepository
        extends JpaRepository<ConferenciaDocumento, Long> {

    boolean existsByDocumento(DocumentoReserva documento);

    Optional<ConferenciaDocumento> findByDocumento(DocumentoReserva documento);

    @Query("""
            select c from ConferenciaDocumento c
            join fetch c.documento d
            join fetch d.reserva r
            join fetch r.solicitante
            join fetch c.conferidoPor
            order by c.conferidoEm desc
            """)
    List<ConferenciaDocumento> listarHistorico();
}
