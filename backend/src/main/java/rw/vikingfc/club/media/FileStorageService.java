package rw.vikingfc.club.media;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    StoredFile store(MultipartFile file, String folder);

    void delete(String publicUrl);
}
