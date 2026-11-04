package br.com.courttime.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** O bucket é privado; somente controllers já autorizados entregam os bytes. */
@Component
@ConditionalOnProperty(name = "courttime.documentos.storage", havingValue = "s3")
public class DocumentoStorageS3 implements DocumentoStorage {

    private static final Logger LOG = LoggerFactory.getLogger(DocumentoStorageS3.class);
    private static final int LIMITE = 10 * 1024 * 1024;
    private final S3Client client;
    private final String bucket;

    public DocumentoStorageS3(S3Client client, @Value("${COURTTIME_S3_BUCKET:}") String bucket) {
        if (!bucket.matches("[a-z0-9][a-z0-9.-]{1,61}[a-z0-9]")) {
            throw new IllegalStateException("Configure o nome do bucket privado de documentos");
        }
        this.client = client;
        this.bucket = bucket;
    }

    @Override
    public void armazenar(String chave, byte[] conteudo) {
        validarChave(chave);
        validarConteudo(conteudo);
        try {
            client.putObject(PutObjectRequest.builder().bucket(bucket).key(chave)
                            .contentType("application/pdf").cacheControl("private, no-store")
                            .ifNoneMatch("*").build(), RequestBody.fromBytes(conteudo));
        } catch (S3Exception exception) {
            if (exception.statusCode() == 412 || exception.statusCode() == 409) {
                throw new IllegalStateException("A chave do documento já está em uso");
            }
            throw falha("armazenar", exception);
        } catch (SdkException exception) {
            throw falha("armazenar", exception);
        }
    }

    @Override
    public Resource carregar(String chave) {
        validarChave(chave);
        try (ResponseInputStream<GetObjectResponse> stream = client.getObject(
                GetObjectRequest.builder().bucket(bucket).key(chave).build())) {
            Long tamanho = stream.response().contentLength();
            if (tamanho != null && tamanho > LIMITE) {
                throw new IllegalStateException("O arquivo do documento excede o tamanho permitido");
            }
            byte[] conteudo = stream.readNBytes(LIMITE + 1);
            validarConteudo(conteudo);
            if (tamanho != null && tamanho != conteudo.length) {
                throw new IllegalStateException("O arquivo do documento está incompleto");
            }
            return new ByteArrayResource(conteudo);
        } catch (SdkException exception) {
            throw falha("carregar", exception);
        } catch (IOException exception) {
            // Não transportar mensagens do provedor, URLs ou cabeçalhos para logs/telas.
            throw new IllegalStateException("Não foi possível carregar o documento remoto");
        }
    }

    @Override
    public void remover(String chave) {
        validarChave(chave);
        try {
            client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(chave).build());
        } catch (S3Exception exception) {
            if (exception.statusCode() != 404) {
                throw falha("remover", exception);
            }
        } catch (SdkException exception) {
            throw falha("remover", exception);
        }
    }

    private void validarChave(String chave) {
        if (chave == null || !chave.matches("[a-f0-9-]{36}\\.pdf")) {
            throw new IllegalArgumentException("Chave de armazenamento inválida");
        }
    }

    private void validarConteudo(byte[] conteudo) {
        byte[] assinatura = "%PDF-".getBytes(StandardCharsets.US_ASCII);
        if (conteudo == null || conteudo.length < assinatura.length || conteudo.length > LIMITE) {
            throw new IllegalArgumentException("O PDF deve possuir conteúdo e no máximo 10 MB");
        }
        for (int i = 0; i < assinatura.length; i++) {
            if (conteudo[i] != assinatura[i]) {
                throw new IllegalArgumentException("O conteúdo não corresponde a um PDF");
            }
        }
    }

    private IllegalStateException falha(String operacao, SdkException exception) {
        int status = exception instanceof S3Exception s3 ? s3.statusCode() : 0;
        LOG.warn("Falha no armazenamento remoto: operação={}, status={}", operacao, status);
        return new IllegalStateException("Não foi possível " + operacao + " o documento remoto");
    }
}
