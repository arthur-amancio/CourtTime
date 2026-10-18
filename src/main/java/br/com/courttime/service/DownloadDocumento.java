package br.com.courttime.service;

import org.springframework.core.io.Resource;

public record DownloadDocumento(String nomeArquivo, long tamanhoBytes, Resource conteudo) {
}
