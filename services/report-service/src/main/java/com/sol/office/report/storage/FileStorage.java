package com.sol.office.report.storage;

import org.springframework.core.io.Resource;

import java.io.InputStream;
import java.nio.file.Path;

/**
 * Where templates and generated outputs live. Keys are relative, '/'-separated paths.
 * The local-disk implementation needs a shared volume when running several instances; an S3/MinIO
 * implementation can replace it without touching callers (except {@link #localPath}, which Jasper needs
 * for subreports and images).
 */
public interface FileStorage {

    long store(String key, InputStream content);

    Resource load(String key);

    boolean exists(String key);

    void delete(String key);

    /** Deletes a key prefix recursively (e.g. a whole template version). */
    void deleteTree(String prefix);

    /** Local file system path of a key. */
    Path localPath(String key);
}
