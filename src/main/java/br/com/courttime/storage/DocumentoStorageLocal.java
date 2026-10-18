package br.com.courttime.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermissions;

@Component
public class DocumentoStorageLocal implements DocumentoStorage {

    private final Path diretorio;

    public DocumentoStorageLocal(
            @Value("${courttime.documentos.diretorio:./data/documentos}") String diretorio) {
        this.diretorio = Path.of(diretorio).toAbsolutePath().normalize();
    }

    @Override
    public void armazenar(String chave, byte[] conteudo) {
        Path destino = resolverChave(chave);
        if (Files.exists(destino, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalStateException("A chave do documento já está em uso");
        }
        Path temporario = null;
        try {
            Files.createDirectories(diretorio);
            restringirPermissoes(diretorio, "rwx------");
            temporario = Files.createTempFile(diretorio, "envio-", ".tmp");
            restringirPermissoes(temporario, "rw-------");
            Files.write(temporario, conteudo, StandardOpenOption.TRUNCATE_EXISTING);
            try {
                Files.move(
                        temporario,
                        destino,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException exception) {
                Files.move(temporario, destino);
            }
        } catch (IOException exception) {
            removerTemporario(temporario);
            throw new IllegalStateException("Não foi possível armazenar o documento", exception);
        }
    }

    @Override
    public Resource carregar(String chave) {
        Path arquivo = resolverChave(chave);
        if (Files.isSymbolicLink(arquivo)
                || !Files.isRegularFile(arquivo, LinkOption.NOFOLLOW_LINKS)
                || !Files.isReadable(arquivo)) {
            throw new IllegalStateException("O arquivo do documento não está disponível");
        }
        return new FileSystemResource(arquivo);
    }

    @Override
    public void remover(String chave) {
        try {
            Files.deleteIfExists(resolverChave(chave));
        } catch (IOException exception) {
            throw new IllegalStateException("Não foi possível remover o arquivo incompleto", exception);
        }
    }

    private Path resolverChave(String chave) {
        if (chave == null || !chave.matches("[a-f0-9-]{36}\\.pdf")) {
            throw new IllegalArgumentException("Chave de armazenamento inválida");
        }
        Path arquivo = diretorio.resolve(chave).normalize();
        if (!arquivo.getParent().equals(diretorio)) {
            throw new IllegalArgumentException("Chave de armazenamento inválida");
        }
        return arquivo;
    }

    private void removerTemporario(Path temporario) {
        if (temporario == null) {
            return;
        }
        try {
            Files.deleteIfExists(temporario);
        } catch (IOException ignored) {
            // O erro original de armazenamento deve ser preservado.
        }
    }

    private void restringirPermissoes(Path caminho, String permissoes) throws IOException {
        if (Files.getFileAttributeView(
                caminho, PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS) != null) {
            Files.setPosixFilePermissions(
                    caminho, PosixFilePermissions.fromString(permissoes));
        }
    }
}
