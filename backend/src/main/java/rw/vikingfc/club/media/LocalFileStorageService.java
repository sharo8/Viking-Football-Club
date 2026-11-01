package rw.vikingfc.club.media;

import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import rw.vikingfc.club.common.exception.BusinessException;
import rw.vikingfc.club.config.AppProperties;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class LocalFileStorageService implements FileStorageService {

    private static final Set<String> ALLOWED = Set.of("jpg", "jpeg", "png", "webp");
    private final Path root;

    public LocalFileStorageService(AppProperties properties) {
        this.root = Path.of(properties.uploads().dir()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException ex) {
            throw new IllegalStateException("Upload directory could not be created: " + root, ex);
        }
    }

    @Override
    public StoredFile store(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.badRequest("Choose an image file");
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            throw BusinessException.badRequest("File is larger than 5 MB");
        }
        String extension = sniff(file);
        if (!ALLOWED.contains(extension)) {
            throw BusinessException.badRequest("Only jpg, png and webp images are accepted");
        }
        String name = UUID.randomUUID() + "." + extension;
        Path directory = root.resolve(folder).normalize();
        if (!directory.startsWith(root)) {
            throw BusinessException.badRequest("Invalid upload folder");
        }
        try {
            Files.createDirectories(directory);
            Path target = directory.resolve(name);
            file.transferTo(target);
            String thumbName = "thumb-" + name;
            Path thumb = directory.resolve(thumbName);
            Path sized = directory.resolve("sized-" + name);
            try {
                Thumbnails.of(target.toFile()).size(800, 800).outputQuality(0.9).toFile(sized.toFile());
                Files.move(sized, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ignored) {
                Files.deleteIfExists(sized);
            }
            writeThumbnail(target, thumb, extension);
            String url = "/files/" + folder + "/" + name;
            String thumbnail = Files.exists(thumb) ? "/files/" + folder + "/" + thumbName : url;
            return new StoredFile(url, thumbnail, file.getOriginalFilename());
        } catch (IOException ex) {
            throw BusinessException.badRequest("The image could not be stored");
        }
    }

    @Override
    public void delete(String publicUrl) {
        if (publicUrl == null || !publicUrl.startsWith("/files/")) {
            return;
        }
        Path file = root.resolve(publicUrl.substring("/files/".length())).normalize();
        if (!file.startsWith(root)) {
            return;
        }
        try {
            Files.deleteIfExists(file);
            Files.deleteIfExists(file.resolveSibling("thumb-" + file.getFileName()));
        } catch (IOException ignored) {
            // Replacing a photo should still succeed when the old file is already gone.
        }
    }

    private void writeThumbnail(Path source, Path thumb, String extension) {
        try {
            Thumbnails.of(source.toFile()).size(256, 256).toFile(thumb.toFile());
        } catch (IOException ignored) {
            try (InputStream in = Files.newInputStream(source)) {
                Files.copy(in, thumb);
            } catch (IOException copyFailure) {
                // The original file is still available.
            }
        }
    }

    private String sniff(MultipartFile file) {
        try {
            byte[] head = file.getInputStream().readNBytes(12);
            if (head.length >= 3 && (head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xD8 && (head[2] & 0xFF) == 0xFF) {
                return "jpg";
            }
            if (head.length >= 4 && head[0] == (byte) 0x89 && head[1] == 0x50 && head[2] == 0x4E && head[3] == 0x47) {
                return "png";
            }
            if (head.length >= 12 && head[0] == 'R' && head[1] == 'I' && head[2] == 'F' && head[3] == 'F'
                    && head[8] == 'W' && head[9] == 'E' && head[10] == 'B' && head[11] == 'P') {
                return "webp";
            }
            return "";
        } catch (IOException ex) {
            return "";
        }
    }

    private String extension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}
