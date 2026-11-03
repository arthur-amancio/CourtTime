package br.com.courttime.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DocumentoStorageS3Test {
    private static final String CHAVE = "123e4567-e89b-12d3-a456-426614174000.pdf";
    private static final byte[] PDF = "%PDF-1.7\nsintético".getBytes(StandardCharsets.UTF_8);
    private S3Client client;
    private DocumentoStorageS3 storage;

    @BeforeEach
    void preparar() {
        client = mock(S3Client.class);
        storage = new DocumentoStorageS3(client, "courttime-documentos-test");
    }

    @Test
    void enviaPdfSemAclPublicaEComPreCondicaoContraSobrescrita() throws Exception {
        storage.armazenar(CHAVE, PDF);
        ArgumentCaptor<PutObjectRequest> req = ArgumentCaptor.forClass(PutObjectRequest.class);
        ArgumentCaptor<RequestBody> body = ArgumentCaptor.forClass(RequestBody.class);
        verify(client).putObject(req.capture(), body.capture());
        assertEquals("*", req.getValue().ifNoneMatch());
        assertNull(req.getValue().acl());
        assertEquals("application/pdf", req.getValue().contentType());
        assertEquals("private, no-store", req.getValue().cacheControl());
        assertEquals(CHAVE, req.getValue().key());
        assertArrayEquals(PDF, body.getValue().contentStreamProvider().newStream().readAllBytes());
    }

    @Test
    void recuperaBytesSemRetornarUrlDoBucket() throws Exception {
        when(client.getObject(any(GetObjectRequest.class))).thenReturn(stream(PDF, (long) PDF.length));
        assertArrayEquals(PDF, storage.carregar(CHAVE).getContentAsByteArray());
    }

    @Test
    void recusaSobrescritaSemRemoverArquivoAnterior() {
        when(client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(S3Exception.builder().statusCode(412).build());
        assertThrows(IllegalStateException.class, () -> storage.armazenar(CHAVE, PDF));
        verify(client, never()).deleteObject(any(DeleteObjectRequest.class));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "../../arquivo.pdf", "/arquivo.pdf", "arquivo.pdf", "https://host/arquivo.pdf"})
    void rejeitaChaveInvalidaAntesDeAcessarRede(String chave) {
        assertThrows(IllegalArgumentException.class, () -> storage.armazenar(chave, PDF));
        assertThrows(IllegalArgumentException.class, () -> storage.carregar(chave));
        assertThrows(IllegalArgumentException.class, () -> storage.remover(chave));
        verifyNoInteractions(client);
    }

    @Test
    void recusaConteudoInvalidoVazioOuMaiorQueDezMb() {
        for (byte[] invalido : new byte[][]{null, new byte[0], "nao-pdf".getBytes(), new byte[10 * 1024 * 1024 + 1]}) {
            assertThrows(IllegalArgumentException.class, () -> storage.armazenar(CHAVE, invalido));
        }
        verifyNoInteractions(client);
    }

    @Test
    void recusaDownloadGrandeOuIncompleto() {
        when(client.getObject(any(GetObjectRequest.class)))
                .thenReturn(stream(PDF, 10L * 1024 * 1024 + 1), stream(PDF, (long) PDF.length + 1));
        assertThrows(IllegalStateException.class, () -> storage.carregar(CHAVE));
        assertThrows(IllegalStateException.class, () -> storage.carregar(CHAVE));
    }

    @Test
    void exclusaoAusenteEIdempotente() {
        when(client.deleteObject(any(DeleteObjectRequest.class)))
                .thenThrow(S3Exception.builder().statusCode(404).build());
        assertDoesNotThrow(() -> storage.remover(CHAVE));
    }

    @Test
    void falhaDoProvedorNaoExpoeMensagemOuCredencial() {
        when(client.getObject(any(GetObjectRequest.class))).thenThrow(S3Exception.builder()
                .statusCode(403).message("credencial-sintetica-nao-deve-aparecer").build());
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> storage.carregar(CHAVE));
        assertFalse(ex.getMessage().contains("credencial-sintetica"));
        assertNull(ex.getCause());
    }

    private ResponseInputStream<GetObjectResponse> stream(byte[] bytes, Long tamanho) {
        return new ResponseInputStream<>(GetObjectResponse.builder().contentLength(tamanho).build(),
                AbortableInputStream.create(new ByteArrayInputStream(bytes)));
    }
}
