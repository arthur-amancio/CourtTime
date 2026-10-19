package br.com.courttime.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DocumentoStorageLocalTest {

    private static final String CHAVE = "123e4567-e89b-12d3-a456-426614174000.pdf";

    @TempDir
    Path diretorio;

    @Test
    void armazenaCarregaERemoveArquivoNaRaizPrivada() throws Exception {
        DocumentoStorageLocal storage = new DocumentoStorageLocal(diretorio.toString());
        byte[] conteudo = "%PDF-1.7\nteste".getBytes();

        storage.armazenar(CHAVE, conteudo);

        assertArrayEquals(conteudo, storage.carregar(CHAVE).getInputStream().readAllBytes());
        storage.remover(CHAVE);
        assertFalse(Files.exists(diretorio.resolve(CHAVE)));
    }

    @Test
    void recusaTraversalChaveArbitrariaESobrescrita() {
        DocumentoStorageLocal storage = new DocumentoStorageLocal(diretorio.toString());
        byte[] conteudo = "%PDF-1.7".getBytes();

        assertThrows(IllegalArgumentException.class,
                () -> storage.armazenar("../../application.properties", conteudo));
        assertThrows(IllegalArgumentException.class,
                () -> storage.carregar("/tmp/arquivo.pdf"));

        storage.armazenar(CHAVE, conteudo);
        assertThrows(IllegalStateException.class, () -> storage.armazenar(CHAVE, conteudo));
    }

    @Test
    void recusaLinkSimbolicoMesmoComChaveValida() throws Exception {
        DocumentoStorageLocal storage = new DocumentoStorageLocal(diretorio.toString());
        Path externo = Files.createTempFile("documento-externo-", ".pdf");
        try {
            Files.createSymbolicLink(diretorio.resolve(CHAVE), externo);
            assertThrows(IllegalStateException.class, () -> storage.carregar(CHAVE));
        } finally {
            Files.deleteIfExists(externo);
        }
    }
}
