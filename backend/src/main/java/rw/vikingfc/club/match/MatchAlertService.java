package rw.vikingfc.club.match;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.vikingfc.club.lineup.LineupState;
import rw.vikingfc.club.lineup.LineupVersion;
import rw.vikingfc.club.lineup.LineupVersionRepository;
import rw.vikingfc.club.mail.ClubMailService;
import rw.vikingfc.club.marketing.NotificationService;
import rw.vikingfc.club.settings.SettingsService;
import rw.vikingfc.club.user.Role;
import rw.vikingfc.club.user.User;
import rw.vikingfc.club.user.UserRepository;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MatchAlertService {

    private static final List<Role> STAFF = List.of(
            Role.HEAD_COACH, Role.ASSISTANT_COACH, Role.GOALKEEPER_COACH, Role.ASSISTANT_MANAGER,
            Role.PRESIDENT, Role.GENERAL_MANAGER, Role.FINANCE_MANAGER);

    private final FootballMatchRepository matchRepository;
    private final LineupVersionRepository lineupRepository;
    private final AlertLogRepository alertLogRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final ClubMailService mailService;
    private final SettingsService settingsService;
    private final Clock clock;

    @Scheduled(cron = "0 */15 * * * *", zone = "Africa/Kigali")
    @Transactional
    public void scheduled() {
        run(Instant.now(clock));
    }

    @Transactional
    public int run(Instant asOf) {
        Instant now = asOf == null ? Instant.now(clock) : asOf;
        int sent = 0;
        for (FootballMatch match : matchRepository.findBySeasonIdOrderByKickoffAsc(settingsService.currentSeason().getId())) {
            sent += raise(match, now);
        }
        return sent;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> mine() {
        Instant now = Instant.now(clock);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (FootballMatch match : matchRepository.findBySeasonIdOrderByKickoffAsc(settingsService.currentSeason().getId())) {
            LineupVersion lineup = latest(match.getId());
            boolean published = stopped(match, lineup);
            long hours = Duration.between(now, match.getKickoff()).toHours();
            if (!published && hours <= 72 && hours >= 0 && match.getStatus() != MatchStatus.CANCELLED && match.getStatus() != MatchStatus.POSTPONED) {
                boolean overdue = hours < match.getAlertHours();
                rows.add(item("LINEUP", lineupCode(lineup, overdue), match.getOpponent().getName(),
                        "/admin/lineup?matchId=" + match.getId(), overdue));
            }
            if ((match.getStatus() == MatchStatus.SCHEDULED || match.getStatus() == MatchStatus.LINEUP_PUBLISHED)
                    && match.getKickoff().isBefore(now)) {
                rows.add(item("RESULT", "RESULT_MISSING", match.getOpponent().getName(),
                        "/admin/matches?matchId=" + match.getId(), true));
            }
        }
        return rows;
    }

    private int raise(FootballMatch match, Instant now) {
        LineupVersion lineup = latest(match.getId());
        boolean stop = stopped(match, lineup);
        List<String> windows = AlertWindows.due(match.getKickoff(), now, stop);
        if (windows.isEmpty()) {
            return 0;
        }
        String current = windows.get(windows.size() - 1);
        boolean overdue = Duration.between(now, match.getKickoff()).toHours() < match.getAlertHours();
        int sent = 0;
        for (User user : userRepository.findByRoleInAndActiveTrueAndDeletedAtIsNull(STAFF)) {
            for (String window : windows) {
                if (alertLogRepository.existsByMatchIdAndWindowKeyAndRecipientEmailAndChannel(match.getId(), window, user.getEmail(), "APP")) {
                    continue;
                }
                if (window.equals(current)) {
                    String title = lineupTitle(match, lineup, overdue);
                    notificationService.notify(user, "LINEUP_ALERT", title, match.getOpponent().getName(), "/admin/lineup?matchId=" + match.getId());
                    if (AlertWindows.email(window, overdue)) {
                        var mail = mailService.enqueue(user.getEmail(), title,
                                mailService.layout(title, "<p>" + match.getOpponent().getName() + "</p>"),
                                "lineup-alert", "en", "LINEUP_ALERT", String.valueOf(match.getId()));
                        mailService.dispatch(mail.getId());
                    }
                    sent++;
                }
                AlertLog log = new AlertLog();
                log.setMatch(match);
                log.setWindowKey(window);
                log.setRecipientEmail(user.getEmail());
                log.setChannel("APP");
                alertLogRepository.save(log);
            }
        }
        return sent;
    }

    private boolean stopped(FootballMatch match, LineupVersion lineup) {
        if (match.getStatus() == MatchStatus.CANCELLED || match.getStatus() == MatchStatus.POSTPONED || match.getStatus() == MatchStatus.FINISHED) {
            return true;
        }
        return lineup != null && (lineup.getStatus() == LineupState.PUBLISHED || lineup.getStatus() == LineupState.LOCKED || lineup.getStatus() == LineupState.FINISHED);
    }

    private String lineupCode(LineupVersion lineup, boolean overdue) {
        if (overdue) {
            return "LINEUP_NOW";
        }
        if (lineup == null) {
            return "NO_LINEUP";
        }
        if (lineup.getStatus() == LineupState.DRAFT || lineup.getStatus() == LineupState.CHANGES_REQUESTED) {
            return "LINEUP_DRAFT";
        }
        if (lineup.getStatus() == LineupState.SUBMITTED) {
            return "LINEUP_SUBMITTED";
        }
        if (lineup.getStatus() == LineupState.APPROVED) {
            return "LINEUP_APPROVED";
        }
        return "LINEUP";
    }

    private String lineupTitle(FootballMatch match, LineupVersion lineup, boolean overdue) {
        String opponent = match.getOpponent().getName();
        if (overdue) {
            return "Build lineup now vs " + opponent;
        }
        if (lineup == null || lineup.getStatus() == LineupState.DRAFT || lineup.getStatus() == LineupState.CHANGES_REQUESTED) {
            return lineup == null ? "No lineup yet vs " + opponent : "Lineup draft not submitted vs " + opponent;
        }
        if (lineup.getStatus() == LineupState.SUBMITTED) {
            return "Submitted, waiting for approval vs " + opponent;
        }
        if (lineup.getStatus() == LineupState.APPROVED) {
            return "Approved, ready to publish vs " + opponent;
        }
        return "Lineup vs " + opponent;
    }

    private LineupVersion latest(Long matchId) {
        return lineupRepository.findByMatchIdOrderByRevisionDesc(matchId).stream().findFirst().orElse(null);
    }

    private Map<String, Object> item(String kind, String code, String opponent, String link, boolean overdue) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("kind", kind);
        body.put("code", code);
        body.put("opponent", opponent);
        body.put("title", fallback(code, opponent));
        body.put("link", link);
        body.put("overdue", overdue);
        return body;
    }

    private String fallback(String code, String opponent) {
        return switch (code) {
            case "RESULT_MISSING" -> "Result missing for " + opponent;
            case "LINEUP_NOW" -> "Build lineup now vs " + opponent;
            case "NO_LINEUP" -> "No lineup yet vs " + opponent;
            case "LINEUP_DRAFT" -> "Lineup draft not submitted vs " + opponent;
            case "LINEUP_SUBMITTED" -> "Submitted, waiting for approval vs " + opponent;
            case "LINEUP_APPROVED" -> "Approved, ready to publish vs " + opponent;
            default -> "Lineup vs " + opponent;
        };
    }
}
