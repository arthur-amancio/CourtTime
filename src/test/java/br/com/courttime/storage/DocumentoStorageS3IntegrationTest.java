package br.com.courttime.storage;

import br.com.courttime.config.DocumentoS3Config;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

/** Protocolo real contra emulador S3 HTTPS local, nunca contra o bucket da demonstração. */
@EnabledIfEnvironmentVariable(named = "COURTTIME_S3_TEST", matches = "true")
class DocumentoStorageS3IntegrationTest {
    private static final String BUCKET = "courttime-s3-test";
    private static final byte[] PDF = "%PDF-1.4\nDocumento exclusivamente sintetico\n%%EOF"
            .getBytes(StandardCharsets.US_ASCII);
    private static String endpoint;

    @BeforeAll
    static void exigirEmuladorIsolado() {
        endpoint = System.getenv("AWS_ENDPOINT_URL_S3");
        URI uri = URI.create(endpoint);
        assertEquals("https", uri.getScheme());
        assertTrue("localhost".equals(uri.getHost()) || "127.0.0.1".equals(uri.getHost()));
        assertNull(uri.getUserInfo());
        assertNull(uri.getQuery());
        assertNull(uri.getFragment());
        assertEquals(BUCKET, System.getenv("COURTTIME_S3_TEST_BUCKET"));
    }

    private S3Client cliente() {
        return new DocumentoS3Config().documentoS3Client(endpoint, "us-east-2");
    }

    @Test
    void preservaBytesAoReabrirClienteERecusaDownloadAnonimo() throws Exception {
        String chave = UUID.randomUUID() + ".pdf";
        try (S3Client primeiro = cliente()) {
            new DocumentoStorageS3(primeiro, BUCKET).armazenar(chave, PDF);
        }
        try (S3Client segundo = cliente()) {
            DocumentoStorageS3 storage = new DocumentoStorageS3(segundo, BUCKET);
            try {
                assertArrayEquals(PDF, storage.carregar(chave).getContentAsByteArray());
                HttpResponse<Void> anonimo = HttpClient.newHttpClient().send(
                        HttpRequest.newBuilder(URI.create(endpoint + "/" + BUCKET + "/" + chave)).GET().build(),
                        HttpResponse.BodyHandlers.discarding());
                assertTrue(anonimo.statusCode() == 401 || anonimo.statusCode() == 403);
            } finally {
                storage.remover(chave);
            }
        }
    }

    @Test
    void duasGravacoesDaMesmaChaveNaoSobrescrevemODocumento() throws Exception {
        String chave = UUID.randomUUID() + ".pdf";
        try (S3Client client = cliente()) {
            DocumentoStorageS3 storage = new DocumentoStorageS3(client, BUCKET);
            var executor = Executors.newFixedThreadPool(2);
            CountDownLatch inicio = new CountDownLatch(1);
            try {
                var tarefa = (java.util.concurrent.Callable<Boolean>) () -> {
                    inicio.await();
                    try { storage.armazenar(chave, PDF); return true; }
                    catch (IllegalStateException exception) { return false; }
                };
                Future<Boolean> a = executor.submit(tarefa);
                Future<Boolean> b = executor.submit(tarefa);
                inicio.countDown();
                assertNotEquals(a.get(), b.get());
                assertArrayEquals(PDF, storage.carregar(chave).getContentAsByteArray());
            } finally {
                executor.shutdownNow();
                storage.remover(chave);
            }
        }
    }

    @Test
    void exclusaoControladaRemoveSomenteAChaveCriadaPeloTeste() {
        String chave = UUID.randomUUID() + ".pdf";
        try (S3Client client = cliente()) {
            DocumentoStorageS3 storage = new DocumentoStorageS3(client, BUCKET);
            storage.armazenar(chave, PDF);
            storage.remover(chave);
            assertThrows(IllegalStateException.class, () -> storage.carregar(chave));
            assertDoesNotThrow(() -> storage.remover(chave));
        }
    }
}
