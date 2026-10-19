package br.com.courttime.service;

import br.com.courttime.entity.Perfil;

public record UsuarioResumo(Long id, String nome, String email, Perfil perfil, boolean ativo) {
}
