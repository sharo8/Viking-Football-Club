package rw.vikingfc.club.player;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlayerPhotoRepository extends JpaRepository<PlayerPhoto, Long> {
    List<PlayerPhoto> findByPlayerIdOrderByCreatedAtDesc(Long playerId);
}
