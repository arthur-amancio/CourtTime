package br.com.courttime.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca o parâmetro do controller que recebe o usuário da sessão, já carregado
 * do banco. Exemplo: {@code public String listar(@UsuarioAtual Usuario aluno)}.
 * Quem preenche é o {@link UsuarioAtualResolver}.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface UsuarioAtual {
}
