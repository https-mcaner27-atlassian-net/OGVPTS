package edu.maltepe.ogvpts.storage;

import edu.maltepe.ogvpts.common.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Teslim dosyalarını diskte saklar (FR-06, FR-14).
 *
 * Veritabanında yalnızca dosyanın adı, boyutu ve diskteki göreli yolu tutulur;
 * dosyanın kendisi ogvpts.upload-dir klasöründe durur:
 *   uploads/tasks/{görevId}/{rastgele-uuid}-{temizlenmiş-ad}
 */
@Service
public class FileStorageService {

    /** FR-06: en fazla 20 MB. */
    public static final long MAX_BYTES = 20L * 1024 * 1024;

    private final Path root;
    private final Set<String> allowedExtensions;

    public FileStorageService(@Value("${ogvpts.upload-dir}") String uploadDir,
                              @Value("${ogvpts.upload.allowed-extensions}") String allowed) {
        this.root = Path.of(uploadDir).toAbsolutePath().normalize();
        this.allowedExtensions = Arrays.stream(allowed.split(","))
                .map(s -> s.trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }

    public Set<String> getAllowedExtensions() {
        return allowedExtensions;
    }

    /** Dosyayı doğrular ve kaydeder; veritabanına yazılacak göreli yolu döner. */
    public String storeTaskFile(Integer taskId, MultipartFile file) {
        validate(file);
        String safeName = sanitize(file.getOriginalFilename());
        Path dir = root.resolve("tasks").resolve(String.valueOf(taskId));
        Path target = dir.resolve(UUID.randomUUID() + "-" + safeName).normalize();
        if (!target.startsWith(root)) {
            throw ApiException.badRequest("Geçersiz dosya adı.");
        }
        try (InputStream in = file.getInputStream()) {
            Files.createDirectories(dir);
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Dosya kaydedilemedi", e);
        }
        return root.relativize(target).toString().replace('\\', '/');
    }

    public Resource load(String relativePath) {
        Path p = root.resolve(relativePath).normalize();
        if (!p.startsWith(root) || !Files.isReadable(p)) {
            throw ApiException.notFound("Dosya");
        }
        return new PathResource(p);
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("Bir dosya seçmelisiniz.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw ApiException.badRequest("Dosya en fazla 20 MB olabilir.");
        }
        String ext = extension(file.getOriginalFilename());
        if (!allowedExtensions.contains(ext)) {
            throw ApiException.badRequest("İzin verilmeyen dosya türü. İzin verilenler: "
                    + String.join(", ", allowedExtensions.stream().sorted().toList()) + ".");
        }
    }

    static String extension(String name) {
        if (name == null || !name.contains(".")) {
            return "";
        }
        return name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    /** Yol karakterlerini ve tehlikeli işaretleri temizler; Türkçe harfler korunur. */
    static String sanitize(String name) {
        String base = name == null ? "dosya" : Path.of(name).getFileName().toString();
        base = base.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_").trim();
        if (base.isEmpty()) {
            base = "dosya";
        }
        return base.length() > 150 ? base.substring(base.length() - 150) : base;
    }
}
