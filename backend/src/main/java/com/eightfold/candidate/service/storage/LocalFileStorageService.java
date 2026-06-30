package com.eightfold.candidate.service.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class LocalFileStorageService {

    private final Path root;

    public LocalFileStorageService(@Value("${app.storage.path:./storage}") String storagePath) throws IOException {
        this.root = Path.of(storagePath).toAbsolutePath().normalize();
        Files.createDirectories(root);
    }

    public String store(UUID candidateId, String folder, String filename, InputStream content) throws IOException {
        String safeName = filename.replaceAll("[^a-zA-Z0-9._-]", "_");
        Path dir = root.resolve(candidateId.toString()).resolve(folder);
        Files.createDirectories(dir);
        Path target = dir.resolve(UUID.randomUUID() + "_" + safeName);
        Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
        return target.toString();
    }

    public InputStream retrieve(String storagePath) throws IOException {
        Path path = Path.of(storagePath).normalize();
        if (!path.startsWith(root) || !Files.exists(path)) {
            throw new IOException("File not found: " + storagePath);
        }
        return Files.newInputStream(path);
    }

    public boolean exists(String storagePath) {
        return storagePath != null && Files.exists(Path.of(storagePath));
    }
}
