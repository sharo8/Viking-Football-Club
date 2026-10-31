package rw.vikingfc.club.player;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.vikingfc.club.mail.ClubMailService;
import rw.vikingfc.club.marketing.NotificationService;
import rw.vikingfc.club.user.Role;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BirthdayService {

    static final ZoneId KIGALI = ZoneId.of("Africa/Kigali");
    private static final List<Role> AUDIENCE = List.of(
            Role.PRESIDENT, Role.GENERAL_MANAGER, Role.FINANCE_MANAGER, Role.SOCIAL_MEDIA_MANAGER);

    private final PlayerRepository playerRepository;
    private final BirthdayNoticeRepository noticeRepository;
    private final NotificationService notificationService;
    private final ClubMailService mailService;
    private final Clock clock;

    @Scheduled(cron = "0 0 7 * * *", zone = "Africa/Kigali")
    public void morningRun() {
        run(LocalDate.now(clock.withZone(KIGALI)));
    }

    @Transactional
    public int run(LocalDate today) {
        int sent = 0;
        for (Player player : playerRepository.findByActiveTrueOrderByRegisterNoAsc()) {
            if (player.getDateOfBirth() == null || player.getDeletedAt() != null) {
                continue;
            }
            LocalDate celebration = celebration(player.getDateOfBirth(), today.getYear());
            long days = ChronoUnit.DAYS.between(today, celebration);
            String type = switch ((int) days) {
                case 7 -> "D7";
                case 3 -> "D3";
                case 1 -> "D1";
                case 0 -> "DAY";
                default -> null;
            };
            if (type == null || noticeRepository.existsByPlayerIdAndYearNumberAndNoticeType(player.getId(), today.getYear(), type)) {
                continue;
            }
            int age = Period.between(player.getDateOfBirth(), celebration).getYears();
            String when = switch (type) {
                case "D7" -> "Birthday in 7 days";
                case "D3" -> "Birthday in 3 days";
                case "D1" -> "Birthday tomorrow";
                default -> "It's " + player.getKnownAs() + "'s birthday today";
            };
            String link = "/admin/players?focus=" + player.getId();
            notificationService.notifyRoles(AUDIENCE, "BIRTHDAY_" + type, player.getKnownAs(), when + " · turns " + age, link);
            if ("DAY".equals(type)) {
                if (player.getEmail() == null || player.getEmail().isBlank()) {
                    notificationService.notifyRoles(List.of(Role.PRESIDENT, Role.GENERAL_MANAGER, Role.FINANCE_MANAGER),
                            "MISSING_EMAIL", "Missing email", "Missing email for " + player.getFullName(), link);
                } else {
                    String lang = player.getPreferredLanguage() == null ? "en" : player.getPreferredLanguage();
                    String title = switch (lang) {
                        case "fr" -> "Joyeux anniversaire " + player.getKnownAs();
                        case "rw" -> "Isabukuru nziza " + player.getKnownAs();
                        default -> "Happy Birthday " + player.getKnownAs();
                    };
                    var mail = mailService.enqueue(player.getEmail(), title,
                            mailService.layout(title, "<p>The whole of Viking FC wishes you a brilliant year.</p>"),
                            "birthday", lang, "PLAYER", String.valueOf(player.getId()));
                    mailService.dispatch(mail.getId());
                }
            }
            BirthdayNotice notice = new BirthdayNotice();
            notice.setPlayerId(player.getId());
            notice.setYearNumber(today.getYear());
            notice.setNoticeType(type);
            notice.setSentAt(Instant.now(clock));
            noticeRepository.save(notice);
            sent++;
        }
        log.info("Birthday job sent {} notices for {}", sent, today);
        return sent;
    }

    static LocalDate celebration(LocalDate birth, int year) {
        if (birth.getMonthValue() == 2 && birth.getDayOfMonth() == 29 && !LocalDate.of(year, 1, 1).isLeapYear()) {
            return LocalDate.of(year, 2, 28);
        }
        return birth.withYear(year);
    }
}
