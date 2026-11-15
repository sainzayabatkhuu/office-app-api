package com.sol.office.report.engine;

import com.sol.office.common.exception.BusinessException;
import com.sol.office.report.storage.FileStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Stores uploaded templates as {@code templates/<CODE>/v<N>/...}. Accepts a single .jrxml or a .zip with the
 * main report plus subreports and images. Each version gets its own directory, so history stays reproducible.
 */
@Component
@RequiredArgsConstructor
public class TemplateStore {

    private static final long MAX_UNZIPPED_BYTES = 50L * 1024 * 1024;
    private static final int MAX_ZIP_ENTRIES = 500;

    private final FileStorage storage;

    public record StoredTemplate(String mainKey, String versionPrefix, String originalFileName) {
    }

    public StoredTemplate store(String reportCode, int version, MultipartFile file, String mainTemplate) {
        String original = StringUtils.cleanPath(file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
        String prefix = "templates/" + reportCode + "/v" + version + "/";
        String lower = original.toLowerCase(Locale.ROOT);
        try (InputStream in = file.getInputStream()) {
            if (lower.endsWith(".jrxml")) {
                String key = prefix + safeName(original);
                storage.store(key, in);
                return new StoredTemplate(key, prefix, original);
            }
            if (lower.endsWith(".zip")) {
                List<String> jrxmls = unzip(in, prefix);
                String main = chooseMain(jrxmls, mainTemplate, prefix);
                return new StoredTemplate(main, prefix, original);
            }
        } catch (IOException e) {
            storage.deleteTree(prefix);
            throw new UncheckedIOException(e);
        } catch (RuntimeException e) {
            storage.deleteTree(prefix);
            throw e;
        }
        throw new BusinessException("INVALID_TEMPLATE_FILE", "Upload a .jrxml file or a .zip containing .jrxml files");
    }

    public void delete(StoredTemplate template) {
        storage.deleteTree(template.versionPrefix());
    }

    private List<String> unzip(InputStream in, String prefix) throws IOException {
        List<String> jrxmls = new ArrayList<>();
        long total = 0;
        int count = 0;
        try (ZipInputStream zip = new ZipInputStream(in)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory() || entry.getName().startsWith("__MACOSX/")) {
                    continue;
                }
                if (++count > MAX_ZIP_ENTRIES) {
                    throw new BusinessException("INVALID_TEMPLATE_FILE", "Archive has too many files");
                }
                // storage.localPath rejects entries escaping the prefix ("zip slip")
                String key = prefix + StringUtils.cleanPath(entry.getName());
                total += storage.store(key, new BoundedInputStream(zip, MAX_UNZIPPED_BYTES - total));
                if (key.toLowerCase(Locale.ROOT).endsWith(".jrxml")) {
                    jrxmls.add(key);
                }
            }
        }
        return jrxmls;
    }

    private static String chooseMain(List<String> jrxmls, String mainTemplate, String prefix) {
        if (StringUtils.hasText(mainTemplate)) {
            String key = prefix + StringUtils.cleanPath(mainTemplate);
            if (!jrxmls.contains(key)) {
                throw new BusinessException("INVALID_TEMPLATE_FILE", "Main template not found in archive: " + mainTemplate);
            }
            return key;
        }
        if (jrxmls.size() == 1) {
            return jrxmls.getFirst();
        }
        return jrxmls.stream().filter(k -> k.equals(prefix + "main.jrxml")).findFirst()
                .orElseThrow(() -> new BusinessException("INVALID_TEMPLATE_FILE",
                        "Archive contains " + jrxmls.size() + " .jrxml files; specify mainTemplate or name it main.jrxml"));
    }

    private static String safeName(String name) {
        String base = name.substring(name.lastIndexOf('/') + 1);
        return base.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    /** Stops zip bombs: fails once more than {@code limit} bytes have been read. */
    private static final class BoundedInputStream extends InputStream {
        private final InputStream delegate;
        private long remaining;

        BoundedInputStream(InputStream delegate, long limit) {
            this.delegate = delegate;
            this.remaining = limit;
        }

        @Override
        public int read() throws IOException {
            int b = delegate.read();
            if (b >= 0 && --remaining < 0) {
                throw tooLarge();
            }
            return b;
        }

        @Override
        public int read(byte[] buf, int off, int len) throws IOException {
            int n = delegate.read(buf, off, len);
            if (n > 0 && (remaining -= n) < 0) {
                throw tooLarge();
            }
            return n;
        }

        @Override
        public void close() {
            // the ZipInputStream is closed by the caller
        }

        private static BusinessException tooLarge() {
            return new BusinessException("INVALID_TEMPLATE_FILE", "Archive content exceeds " + MAX_UNZIPPED_BYTES / 1024 / 1024 + " MB");
        }
    }
}
