package br.com.courttime.repository;

import br.com.courttime.entity.Quadra;
import br.com.courttime.entity.Reserva;
import br.com.courttime.entity.StatusReserva;
import br.com.courttime.entity.Usuario;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    List<Reserva> findBySolicitanteOrderByCriadaEmDesc(Usuario solicitante);

    Optional<Reserva> findByIdAndSolicitante(Long id, Usuario solicitante);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select r from Reserva r
            join fetch r.solicitante
            where r.id = :id and r.solicitante.id = :solicitanteId
            """)
    Optional<Reserva> findByIdAndSolicitanteIdForUpdate(
            @Param("id") Long id, @Param("solicitanteId") Long solicitanteId);

    List<Reserva> findByQuadraAndDataUtilizacaoAndStatusOrderByHorarioInicio(
            Quadra quadra, LocalDate dataUtilizacao, StatusReserva status);

    List<Reserva> findByQuadraAndStatusAndDataUtilizacaoGreaterThanEqualOrderByDataUtilizacaoAscHorarioInicioAsc(
            Quadra quadra, StatusReserva status, LocalDate dataUtilizacao);

    long countByStatus(StatusReserva status);

    boolean existsByRemarcacaoDe(Reserva reservaCancelada);

    Optional<Reserva> findByRemarcacaoDe(Reserva reservaCancelada);

    /** Reserva cancelada que a solicitação informada remarca, do mesmo aluno. */
    @Query("""
            select r.remarcacaoDe from Reserva r
            where r.id = :id and r.solicitante = :solicitante
            """)
    Optional<Reserva> obterReservaRemarcada(
            @Param("id") Long id, @Param("solicitante") Usuario solicitante);

    @Query("""
            select r from Reserva r
            join fetch r.quadra
            join fetch r.solicitante
            where r.status = :status
            order by r.criadaEm asc, r.id asc
            """)
    List<Reserva> listarParaAnaliseAdministrativa(@Param("status") StatusReserva status);

    @Query("""
            select r from Reserva r
            join fetch r.quadra
            join fetch r.solicitante
            left join fetch r.canceladaPor
            where r.id = :id
            """)
    Optional<Reserva> obterParaConsultaAdministrativa(@Param("id") Long id);

    @Query("""
            select r from Reserva r
            join fetch r.quadra
            join fetch r.solicitante
            where r.status = :status
              and r.dataUtilizacao >= :dataAtual
            order by r.dataUtilizacao asc, r.horarioInicio asc, r.id asc
            """)
    List<Reserva> listarAutorizadasParaSeguranca(
            @Param("status") StatusReserva status,
            @Param("dataAtual") LocalDate dataAtual);

    @Query("""
            select r from Reserva r
            join fetch r.quadra
            join fetch r.solicitante
            where r.status = :status
              and r.dataUtilizacao >= :dataAtual
            order by r.dataUtilizacao asc, r.horarioInicio asc, r.id asc
            """)
    List<Reserva> listarAutorizadasParaAdministracao(
            @Param("status") StatusReserva status,
            @Param("dataAtual") LocalDate dataAtual);

    @Query("""
            select r from Reserva r
            join fetch r.quadra
            join fetch r.solicitante
            where r.id = :id
              and r.status = :status
              and r.dataUtilizacao >= :dataAtual
            """)
    Optional<Reserva> obterAutorizadaParaSeguranca(
            @Param("id") Long id,
            @Param("status") StatusReserva status,
            @Param("dataAtual") LocalDate dataAtual);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select r from Reserva r
            join fetch r.quadra
            join fetch r.solicitante
            where r.id = :id
            """)
    Optional<Reserva> findByIdForUpdate(@Param("id") Long id);
}
