package br.com.courttime.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;
import java.time.Duration;

/**
 * Cria o cliente S3 somente quando esse armazenamento é selecionado. Exige endpoint HTTPS e
 * credenciais do ambiente; não torna objetos públicos nem os grava no Git.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "courttime.documentos.storage", havingValue = "s3")
public class DocumentoS3Config {

    @Bean(destroyMethod = "close")
    public S3Client documentoS3Client(
            @Value("${AWS_ENDPOINT_URL_S3:}") String endpoint,
            @Value("${AWS_REGION:us-east-2}") String regiao) {
        URI uri;
        try {
            uri = URI.create(endpoint);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("Configure um endpoint S3 HTTPS válido");
        }
        if (!"https".equals(uri.getScheme()) || uri.getHost() == null
                || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null) {
            throw new IllegalStateException("Configure um endpoint S3 HTTPS sem credenciais na URL");
        }
        return S3Client.builder()
                .endpointOverride(uri)
                .region(Region.of(regiao))
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true).chunkedEncodingEnabled(false).build())
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
                .httpClientBuilder(UrlConnectionHttpClient.builder()
                        .connectionTimeout(Duration.ofSeconds(5))
                        .socketTimeout(Duration.ofSeconds(20)))
                .overrideConfiguration(config -> config
                        .apiCallTimeout(Duration.ofSeconds(30))
                        .apiCallAttemptTimeout(Duration.ofSeconds(20)))
                .build();
    }
}
