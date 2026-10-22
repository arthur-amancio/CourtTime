package br.com.courttime.repository;

import br.com.courttime.entity.LocalPagamento;
import br.com.courttime.entity.PagamentoAdicional;
import br.com.courttime.entity.Reserva;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PagamentoAdicionalRepository extends JpaRepository<PagamentoAdicional, Long> {

    List<PagamentoAdicional> findByReservaOrderByRegistradoEmAscIdAsc(Reserva reserva);

    /** Serializa confirmações simultâneas do mesmo repasse. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PagamentoAdicional p where p.id = :id")
    Optional<PagamentoAdicional> findByIdForUpdate(@Param("id") Long id);

    /** R11: dinheiro entregue à segurança que a secretaria ainda não confirmou. */
    @Query("""
            select p from PagamentoAdicional p
            join fetch p.reserva r
            join fetch r.solicitante
            join fetch p.registradoPor
            where p.local = :local
              and p.repasseConfirmadoEm is null
            order by p.registradoEm asc, p.id asc
            """)
    List<PagamentoAdicional> listarAguardandoRepasse(@Param("local") LocalPagamento local);
}
