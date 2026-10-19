package br.com.courttime.repository;

import br.com.courttime.entity.Usuario;
import br.com.courttime.entity.Perfil;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    boolean existsByPerfilAndAtivoTrue(Perfil perfil);

    @Query("""
            select u from Usuario u
            where (:termo = ''
                   or lower(u.nome) like lower(concat('%', :termo, '%'))
                   or lower(u.email) like lower(concat('%', :termo, '%')))
              and (:perfil is null or u.perfil = :perfil)
              and (:ativo is null or u.ativo = :ativo)
            """)
    Page<Usuario> buscar(
            @Param("termo") String termo,
            @Param("perfil") Perfil perfil,
            @Param("ativo") Boolean ativo,
            Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u where u.id = :id")
    Optional<Usuario> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u where u.perfil = :perfil and u.ativo = true order by u.id")
    List<Usuario> findAtivosPorPerfilForUpdate(@Param("perfil") Perfil perfil);
}
