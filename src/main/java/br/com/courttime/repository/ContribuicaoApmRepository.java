package br.com.courttime.repository;

import br.com.courttime.entity.ContribuicaoApm;
import br.com.courttime.entity.Reserva;
import br.com.courttime.entity.ResultadoConferenciaDocumento;
import br.com.courttime.entity.StatusReserva;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ContribuicaoApmRepository extends JpaRepository<ContribuicaoApm, Long> {

    boolean existsByReserva(Reserva reserva);

    @Query("""
            select r from Reserva r
            join fetch r.solicitante
            where r.status = :status
              and not exists (
                select ca.id from ContribuicaoApm ca where ca.reserva = r
              )
              and exists (
                select cd.id from ConferenciaDocumento cd
                join cd.documento d
                where d.reserva = r
                  and cd.resultado = :resultado
                  and d.versao = (
                    select max(d2.versao) from DocumentoReserva d2 where d2.reserva = r
                  )
              )
            order by r.criadaEm asc, r.id asc
            """)
    List<Reserva> listarPendentes(
            @Param("status") StatusReserva status,
            @Param("resultado") ResultadoConferenciaDocumento resultado);

    @Query("""
            select ca from ContribuicaoApm ca
            join fetch ca.reserva r
            join fetch r.solicitante
            join fetch ca.conferenciaDocumento cd
            join fetch cd.documento
            join fetch ca.registradaPor
            order by ca.registradaEm desc, ca.id desc
            """)
    List<ContribuicaoApm> listarRegistradas();

    @Query("""
            select ca from ContribuicaoApm ca
            join fetch ca.reserva r
            join fetch r.solicitante
            join fetch ca.conferenciaDocumento cd
            join fetch cd.documento
            join fetch ca.registradaPor
            where r.id = :reservaId
            """)
    Optional<ContribuicaoApm> obterPorReservaId(@Param("reservaId") Long reservaId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select ca from ContribuicaoApm ca
            join fetch ca.reserva r
            join fetch r.solicitante
            join fetch ca.conferenciaDocumento cd
            join fetch cd.documento
            join fetch ca.registradaPor
            where ca.id = :id
            """)
    Optional<ContribuicaoApm> findByIdForUpdate(@Param("id") Long id);
}
