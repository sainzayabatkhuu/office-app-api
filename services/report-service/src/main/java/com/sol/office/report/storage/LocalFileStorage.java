package com.sol.office.report.storage;

import com.sol.office.common.exception.NotFoundException;
import com.sol.office.report.config.ReportProperties;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Component
public class LocalFileStorage implements FileStorage {

    private final Path root;

    public LocalFileStorage(ReportProperties properties) throws IOException {
        this.root = Path.of(properties.storage().baseDir()).toAbsolutePath().normalize();
        Files.createDirectories(root);
    }

    @Override
    public long store(String key, InputStream content) {
        Path target = localPath(key);
        try {
            Files.createDirectories(target.getParent());
            Path tmp = Files.createTempFile(target.getParent(), ".upload", ".tmp");
            long size = Files.copy(content, tmp, StandardCopyOption.REPLACE_EXISTING);
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            return size;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store " + key, e);
        }
    }

    @Override
    public Resource load(String key) {
        Path path = localPath(key);
        if (!Files.isRegularFile(path)) {
            throw new NotFoundException("File not found: " + key);
        }
        return new PathResource(path);
    }

    @Override
    public boolean exists(String key) {
        return Files.isRegularFile(localPath(key));
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(localPath(key));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to delete " + key, e);
        }
    }

    @Override
    public void deleteTree(String prefix) {
        try {
            FileSystemUtils.deleteRecursively(localPath(prefix));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to delete " + prefix, e);
        }
    }

    /** Resolves a key under the root, rejecting anything that escapes it ("../"). */
    @Override
    public Path localPath(String key) {
        Path resolved = root.resolve(key).normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("Invalid storage key: " + key);
        }
        return resolved;
    }
}
