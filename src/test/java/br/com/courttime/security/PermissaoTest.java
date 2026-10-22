package br.com.courttime.security;

import br.com.courttime.entity.Perfil;
import br.com.courttime.entity.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PermissaoTest {

    @Test
    void contaAtivaComOPerfilPedidoPassa() {
        Usuario secretaria = usuario(1L, Perfil.SECRETARIA, true);

        assertTrue(Permissao.temPerfil(secretaria, Perfil.SECRETARIA));
        assertDoesNotThrow(() -> Permissao.exigir(secretaria, Perfil.SECRETARIA));
    }

    @Test
    void outroPerfilContaInativaContaNaoGravadaOuNulaSaoNegados() {
        assertFalse(Permissao.temPerfil(usuario(1L, Perfil.ALUNO, true), Perfil.SECRETARIA));
        assertFalse(Permissao.temPerfil(usuario(1L, Perfil.SECRETARIA, false), Perfil.SECRETARIA));
        assertFalse(Permissao.temPerfil(usuario(null, Perfil.SECRETARIA, true), Perfil.SECRETARIA));
        assertFalse(Permissao.temPerfil(null, Perfil.SECRETARIA));
        assertThrows(AccessDeniedException.class,
                () -> Permissao.exigir(usuario(1L, Perfil.ALUNO, true), Perfil.ADMINISTRADOR));
    }

    private Usuario usuario(Long id, Perfil perfil, boolean ativo) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setPerfil(perfil);
        usuario.setAtivo(ativo);
        return usuario;
    }
}
