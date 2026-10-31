package rw.vikingfc.club.marketing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContentPostRepository extends JpaRepository<ContentPost, Long> {
    List<ContentPost> findAllByOrderByScheduledAtAsc();
}
