package br.com.courttime.repository;

import br.com.courttime.entity.AuditoriaUsuario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditoriaUsuarioRepository extends JpaRepository<AuditoriaUsuario, Long> {
}
