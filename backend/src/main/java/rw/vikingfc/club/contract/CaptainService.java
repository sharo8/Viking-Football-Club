package rw.vikingfc.club.contract;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.vikingfc.club.audit.AuditService;
import rw.vikingfc.club.common.api.PageResponse;
import rw.vikingfc.club.common.exception.BusinessException;
import rw.vikingfc.club.common.exception.NotFoundException;
import rw.vikingfc.club.common.util.SecurityUtils;
import rw.vikingfc.club.config.AppProperties;
import rw.vikingfc.club.marketing.NotificationService;
import rw.vikingfc.club.match.MatchPlayerRecord;
import rw.vikingfc.club.match.MatchPlayerRecordRepository;
import rw.vikingfc.club.player.Player;
import rw.vikingfc.club.player.PlayerRepository;
import rw.vikingfc.club.player.PlayerStatus;
import rw.vikingfc.club.season.Season;
import rw.vikingfc.club.season.SeasonRepository;
import rw.vikingfc.club.settings.SettingsService;
import rw.vikingfc.club.user.Role;
import rw.vikingfc.club.user.UserRepository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CaptainService {

    private final TeamCaptainRepository captainRepository;
    private final PlayerRepository playerRepository;
    private final MatchPlayerRecordRepository records;
    private final SeasonRepository seasonRepository;
    private final SettingsService settingsService;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final AppProperties properties;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<CaptainDtos.View> list(Long seasonId) {
        Season season = season(seasonId);
        return captainRepository.findBySeasonIdAndEndedAtIsNullOrderByRankAsc(season.getId()).stream().map(this::view).toList();
    }

    @Transactional(readOnly = true)
    public CaptainDtos.Summary summary(Long seasonId) {
        Season season = season(seasonId);
        List<CaptainDtos.View> slots = list(season.getId());
        List<Integer> ages = slots.stream().map(CaptainDtos.View::age).filter(age -> age != null).toList();
        double average = ages.isEmpty() ? Double.NaN : ages.stream().mapToInt(Integer::intValue).average().orElse(0);
        return new CaptainDtos.Summary(
                slots.size(), 4,
                ages.isEmpty() ? null : Math.round(average * 10) / 10.0,
                ages.size(),
                slots.stream().mapToInt(row -> row.matches() == null ? 0 : row.matches()).sum(),
                slots.stream().mapToInt(row -> row.goals() == null ? 0 : row.goals()).sum(),
                slots.stream().mapToInt(row -> row.assists() == null ? 0 : row.assists()).sum(),
                season.getId(), season.getName(), !season.isCurrentSeason());
    }

    @Transactional(readOnly = true)
    public List<CaptainDtos.SeasonOption> seasons() {
        return seasonRepository.findAll().stream()
                .sorted(Comparator.comparing(Season::getStartDate).reversed())
                .map(season -> new CaptainDtos.SeasonOption(season.getId(), season.getName(), season.getCode(), season.isCurrentSeason()))
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<CaptainDtos.View> history(Long seasonId, CaptainRank rank, boolean currentOnly, String query,
                                                  String sort, String dir, int page, int size) {
        Season season = season(seasonId);
        String needle = query == null ? "" : query.trim().toLowerCase(java.util.Locale.ROOT);
        List<CaptainDtos.View> rows = new ArrayList<>(captainRepository.findBySeasonIdOrderByRankAsc(season.getId()).stream()
                .filter(row -> rank == null || row.getRank() == rank)
                .filter(row -> !currentOnly || row.getEndedAt() == null)
                .map(this::view)
                .filter(row -> needle.isBlank() || matches(row, needle))
                .sorted(historyOrder(sort, dir))
                .toList());
        int safe = Math.max(1, Math.min(size, 500));
        int safePage = Math.max(page, 0);
        int from = Math.min(safePage * safe, rows.size());
        int to = Math.min(from + safe, rows.size());
        int pages = rows.isEmpty() ? 0 : (int) Math.ceil(rows.size() / (double) safe);
        return new PageResponse<>(rows.subList(from, to), safePage, safe, rows.size(), pages);
    }

    public CaptainDtos.View assign(Long playerId, CaptainRank rank) {
        return assign(playerId, rank, null);
    }

    public CaptainDtos.View assign(Long playerId, CaptainRank rank, String note) {
        Season season = settingsService.currentSeason();
        Player player = playerRepository.findByIdAndActiveTrue(playerId)
                .orElseThrow(() -> NotFoundException.of("Player", playerId));
        if (player.getStatus() != PlayerStatus.ACTIVE || player.getDeletedAt() != null) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "Only an active player can wear the armband", "NOT_ELIGIBLE", null);
        }
        Long seasonId = season.getId();
        TeamCaptain existingPlayer = captainRepository.findBySeasonIdAndPlayerIdAndEndedAtIsNull(seasonId, playerId).orElse(null);
        TeamCaptain existingRank = captainRepository.findBySeasonIdAndRankAndEndedAtIsNull(seasonId, rank).orElse(null);
        boolean sameSeat = existingPlayer != null && existingRank != null && existingPlayer.getId().equals(existingRank.getId());
        int open = captainRepository.findBySeasonIdAndEndedAtIsNullOrderByRankAsc(seasonId).size();
        CaptainRules.assertCanAdd(open, existingPlayer != null || existingRank != null);
        Instant now = Instant.now(clock);
        if (sameSeat) {
            existingPlayer.setNote(blank(note) ? existingPlayer.getNote() : note.trim());
            existingPlayer.setAssignedBy(actor());
            auditService.record("CAPTAIN", "Player", playerId, null, rank.name(), null);
            return view(existingPlayer);
        }
        if (existingRank != null) {
            existingRank.setEndedAt(now);
        }
        if (existingPlayer != null) {
            existingPlayer.setEndedAt(now);
        }
        TeamCaptain captain = new TeamCaptain();
        captain.setSeason(season);
        captain.setPlayer(player);
        captain.setRank(rank);
        captain.setStartedAt(now);
        captain.setNote(blank(note) ? null : note.trim());
        captain.setAssignedBy(actor());
        captainRepository.save(captain);
        auditService.record("CAPTAIN", "Player", playerId, existingPlayer == null ? null : existingPlayer.getRank().name(), rank.name(), null);
        notificationService.notifyRoles(List.of(Role.PRESIDENT, Role.GENERAL_MANAGER, Role.FINANCE_MANAGER, Role.ASSISTANT_MANAGER,
                        Role.HEAD_COACH, Role.ASSISTANT_COACH, Role.GOALKEEPER_COACH),
                "CAPTAIN", player.getFullName() + " is now " + rankLabel(rank), season.getName(), "/admin/captains");
        return view(captain);
    }

    public void remove(Long id) {
        TeamCaptain captain = captainRepository.findById(id).orElseThrow(() -> NotFoundException.of("Captain", id));
        if (!captain.getSeason().isCurrentSeason()) {
            throw new BusinessException(HttpStatus.CONFLICT, "Past seasons are read-only", "SEASON_CLOSED", null);
        }
        captain.setEndedAt(Instant.now(clock));
        auditService.record("CAPTAIN_REMOVED", "Player", captain.getPlayer().getId(), captain.getRank().name(), null, null);
    }

    public void removeRank(CaptainRank rank) {
        TeamCaptain captain = captainRepository.findBySeasonIdAndRankAndEndedAtIsNull(settingsService.currentSeason().getId(), rank)
                .orElseThrow(() -> NotFoundException.of("Captain", rank.name()));
        remove(captain.getId());
    }

    @Transactional(readOnly = true)
    public byte[] export(Long seasonId, String lang) {
        Season season = season(seasonId);
        String language = language(lang);
        List<CaptainPdf.Slot> slots = new ArrayList<>();
        for (CaptainDtos.View holder : list(season.getId())) {
            slots.add(new CaptainPdf.Slot(holder.rank(), holder.fullName(), holder.knownAs(), holder.jerseyNumber(),
                    holder.position(), holder.age(), holder.startedAt(), photoBytes(holder.photoUrl())));
        }
        List<CaptainPdf.Row> history = captainRepository.findBySeasonIdOrderByRankAsc(season.getId()).stream()
                .map(this::view)
                .sorted(Comparator.comparing(CaptainDtos.View::startedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(row -> new CaptainPdf.Row(row.fullName(), row.rank(), row.startedAt(), row.endedAt(), row.assignedBy()))
                .toList();
        try {
            return CaptainPdf.render(new CaptainPdf.Sheet(language, season.getName(), Instant.now(clock), actor(),
                    slots, history, resource("/brand/logo.png")));
        } catch (RuntimeException ex) {
            throw BusinessException.badRequest("The captain sheet could not be built");
        }
    }

    public String exportName(Long seasonId) {
        return CaptainPdf.fileName(season(seasonId).getName());
    }

    private CaptainDtos.View view(TeamCaptain captain) {
        Player player = captain.getPlayer();
        Instant started = captain.getStartedAt() != null ? captain.getStartedAt() : captain.getCreatedAt();
        int[] stats = stats(player, captain.getSeason().getId(), started, captain.getEndedAt());
        Integer age = player.getDateOfBirth() == null ? null : Period.between(player.getDateOfBirth(), LocalDate.now(clock)).getYears();
        String positionLabel = player.getPosition() == null ? "" : player.getPosition().getLabel();
        String positionCode = player.getPosition() == null ? "" : player.getPosition().name();
        return new CaptainDtos.View(captain.getId(), player.getId(), player.getFullName(), player.getKnownAs(),
                player.getJerseyNumber(), player.getPhotoUrl(), positionLabel, positionCode,
                player.getStatus() == null ? "" : player.getStatus().name(), age, captain.getRank(), started, captain.getEndedAt(),
                stats[0], stats[1], stats[2], stats[3], captain.getNote(), captain.getAssignedBy(),
                captain.getSeason().getId(), captain.getSeason().getName());
    }

    private int[] stats(Player player, Long seasonId, Instant since, Instant until) {
        int matches = 0;
        int goals = 0;
        int assists = 0;
        int asCaptain = 0;
        for (MatchPlayerRecord record : records.findByPlayerId(player.getId())) {
            if (record.getMatch() == null || record.getMatch().getSeason() == null || !seasonId.equals(record.getMatch().getSeason().getId())) {
                continue;
            }
            matches++;
            goals += record.getGoals();
            assists += record.getAssists();
            Instant kickoff = record.getMatch().getKickoff();
            if (since != null && kickoff != null && !kickoff.isBefore(since) && (until == null || kickoff.isBefore(until))) {
                asCaptain++;
            }
        }
        return new int[]{matches, goals, assists, asCaptain};
    }

    private Season season(Long seasonId) {
        if (seasonId == null) {
            return settingsService.currentSeason();
        }
        return seasonRepository.findById(seasonId).orElseThrow(() -> NotFoundException.of("Season", seasonId));
    }

    private String actor() {
        String email = SecurityUtils.email();
        if (email == null || email.isBlank()) {
            return "";
        }
        return userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(email).map(user -> user.getFullName()).orElse(email);
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private boolean matches(CaptainDtos.View row, String needle) {
        String hay = (row.fullName() + " " + row.knownAs() + " " + row.jerseyNumber()).toLowerCase(java.util.Locale.ROOT);
        return hay.contains(needle);
    }

    private Comparator<CaptainDtos.View> historyOrder(String sort, String dir) {
        Comparator<CaptainDtos.View> order = switch (sort == null ? "startedAt" : sort) {
            case "player" -> Comparator.comparing(row -> row.fullName() == null ? "" : row.fullName(), String.CASE_INSENSITIVE_ORDER);
            case "rank" -> Comparator.comparing(row -> row.rank() == null ? 0 : row.rank().ordinal());
            case "endedAt" -> Comparator.comparing(CaptainDtos.View::endedAt, Comparator.nullsLast(Comparator.naturalOrder()));
            case "duration" -> Comparator.comparingLong(this::durationMillis);
            case "assignedBy" -> Comparator.comparing(row -> row.assignedBy() == null ? "" : row.assignedBy(), String.CASE_INSENSITIVE_ORDER);
            default -> Comparator.comparing(CaptainDtos.View::startedAt, Comparator.nullsLast(Comparator.naturalOrder()));
        };
        return "asc".equalsIgnoreCase(dir) ? order : order.reversed();
    }

    private long durationMillis(CaptainDtos.View row) {
        if (row.startedAt() == null) {
            return 0;
        }
        Instant end = row.endedAt() == null ? Instant.now(clock) : row.endedAt();
        return Math.max(0, end.toEpochMilli() - row.startedAt().toEpochMilli());
    }

    private String language(String requested) {
        if (requested != null && List.of("en", "fr", "rw").contains(requested)) {
            return requested;
        }
        String email = SecurityUtils.email();
        if (email == null) {
            return "en";
        }
        return userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(email)
                .map(user -> user.getPreferredLanguage() == null ? "en" : user.getPreferredLanguage())
                .orElse("en");
    }

    private byte[] resource(String path) {
        try (java.io.InputStream in = CaptainService.class.getResourceAsStream(path)) {
            return in == null ? new byte[0] : in.readAllBytes();
        } catch (java.io.IOException ex) {
            return new byte[0];
        }
    }

    private byte[] photoBytes(String url) {
        if (url == null || !url.startsWith("/files/")) {
            return null;
        }
        try {
            Path root = Path.of(properties.uploads().dir()).toAbsolutePath().normalize();
            Path file = root.resolve(url.substring("/files/".length())).normalize();
            if (!file.startsWith(root) || !Files.isRegularFile(file)) {
                return null;
            }
            return Files.readAllBytes(file);
        } catch (java.io.IOException ex) {
            return null;
        }
    }

    static String rankLabel(CaptainRank rank) {
        return switch (rank) {
            case CAPTAIN -> "Captain";
            case VICE_CAPTAIN -> "Vice-captain";
            case THIRD_CAPTAIN -> "Third captain";
            case FOURTH_CAPTAIN -> "Fourth captain";
        };
    }
}
