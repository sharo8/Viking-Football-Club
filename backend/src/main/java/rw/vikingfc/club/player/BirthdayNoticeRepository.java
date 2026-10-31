package rw.vikingfc.club.player;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BirthdayNoticeRepository extends JpaRepository<BirthdayNotice, Long> {
    boolean existsByPlayerIdAndYearNumberAndNoticeType(Long playerId, int yearNumber, String noticeType);
}
