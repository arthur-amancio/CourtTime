package br.com.courttime.repository;

import br.com.courttime.entity.Quadra;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface QuadraRepository extends JpaRepository<Quadra, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select q from Quadra q where q.id = :id")
    Optional<Quadra> findByIdForUpdate(@Param("id") Long id);
}
