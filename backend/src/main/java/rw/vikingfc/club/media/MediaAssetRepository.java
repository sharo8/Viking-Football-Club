package rw.vikingfc.club.media;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MediaAssetRepository extends JpaRepository<MediaAsset, Long> {
    List<MediaAsset> findAllByOrderByCreatedAtDesc();
}
