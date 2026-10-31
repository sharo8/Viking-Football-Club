package rw.vikingfc.club.marketing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContentPostAssetRepository extends JpaRepository<ContentPostAsset, Long> {
    List<ContentPostAsset> findByPostId(Long postId);
}
