package br.com.courttime.storage;

import org.springframework.core.io.Resource;

public interface DocumentoStorage {

    void armazenar(String chave, byte[] conteudo);

    Resource carregar(String chave);

    void remover(String chave);
}
