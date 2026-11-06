package br.com.courttime.repository;

import br.com.courttime.entity.Quadra;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Acesso à quadra; a consulta explícita para atualização é o lock compartilhado por aprovação e
 * administração. Chamadas comuns de leitura não adquirem esse lock.
 */
public interface QuadraRepository extends JpaRepository<Quadra, Long> {

    /**
     * Exige transação do chamador; mantém o lock pessimista até seu término. Todos os caminhos
     * que aprovam ou restringem disponibilidade usam a mesma quadra.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select q from Quadra q where q.id = :id")
    Optional<Quadra> findByIdForUpdate(@Param("id") Long id);
}
