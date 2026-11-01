package rw.vikingfc.club.player;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.vikingfc.club.audit.AuditService;
import rw.vikingfc.club.common.api.PageResponse;
import rw.vikingfc.club.common.RwandaPhone;
import rw.vikingfc.club.common.exception.BusinessException;
import rw.vikingfc.club.common.util.SecurityUtils;
import rw.vikingfc.club.contract.PlayerContractRepository;
import rw.vikingfc.club.contract.TeamCaptainRepository;
import rw.vikingfc.club.mail.ClubMailService;
import rw.vikingfc.club.match.MatchPlayerRecordRepository;
import rw.vikingfc.club.sanction.FineRepository;
import rw.vikingfc.club.settings.SettingsService;
import rw.vikingfc.club.user.Role;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Transactional
public class PlayerOfficeService {

    private final PlayerRepository playerRepository;
    private final PlayerContractRepository contractRepository;
    private final TeamCaptainRepository captainRepository;
    private final MatchPlayerRecordRepository recordRepository;
    private final SettingsService settingsService;
    private final ClubMailService mailService;
    private final AuditService auditService;
    private final PlayerService playerService;
    private final FineRepository fineRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> directory(String q, String line, String position, String status,
                                                       String birthday, boolean missing, boolean archived, boolean expiring,
                                                       String sort, String dir, int page, int size) {
        List<Map<String, Object>> rows = new java.util.ArrayList<>(cards(archived).stream()
                .filter(row -> matches(row, q, line, position, status, birthday, missing, expiring)).toList());
        rows.sort(comparator(sort, dir));
        int safeSize = Math.max(1, Math.min(size, 100));
        int safePage = Math.max(page, 0);
        int from = Math.min(safePage * safeSize, rows.size());
        int to = Math.min(from + safeSize, rows.size());
        int pages = rows.isEmpty() ? 0 : (int) Math.ceil(rows.size() / (double) safeSize);
        return new PageResponse<>(rows.subList(from, to), safePage, safeSize, rows.size(), pages);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> analytics() {
        List<Player> players = playerRepository.findByActiveTrueOrderByRegisterNoAsc().stream()
                .filter(player -> player.getDeletedAt() == null).toList();
        List<Integer> ages = players.stream().map(this::age).filter(age -> age != null).sorted().toList();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("count", players.size());
        body.put("averageAge", ages.isEmpty() ? null : ages.stream().mapToInt(Integer::intValue).average().orElse(0));
        body.put("medianAge", ages.isEmpty() ? null : ages.get(ages.size() / 2));
        body.put("youngest", ages.isEmpty() ? null : ages.get(0));
        body.put("oldest", ages.isEmpty() ? null : ages.get(ages.size() - 1));
        body.put("ageBands", bands(ages));
        body.put("byPosition", count(players.stream().map(player -> player.getPosition().name()).toList()));
        body.put("byLine", count(players.stream().map(Player::getLineName).toList()));
        body.put("byStatus", count(players.stream().map(player -> player.getStatus().name()).toList()));
        body.put("byNationality", count(players.stream().map(player -> player.getNationality() == null ? "Unknown" : player.getNationality()).toList()));
        body.put("byFoot", count(players.stream().map(player -> player.getPreferredFoot() == null || player.getPreferredFoot().isBlank() ? "Unknown" : player.getPreferredFoot()).toList()));
        body.put("withDateOfBirth", ages.size());
        body.put("missingDateOfBirth", players.size() - ages.size());
        body.put("goalkeepers", players.stream().filter(player -> player.getPosition() == Position.GOALKEEPER).count());
        body.put("averageByPosition", averageBy(players, player -> player.getPosition().name()));
        body.put("averageByLine", averageBy(players, Player::getLineName));
        body.put("depth", depth(players));
        body.put("ageByLine", ageByLine(players));
        body.put("birthdaysByMonth", birthdaysByMonth(players));
        body.put("contractQuarters", contractQuarters());
        body.put("upcomingBirthdays", upcoming(30).stream().limit(8).toList());
        body.put("completeness", completeness(players));
        if (SecurityUtils.isExecutive()) {
            body.put("payByLine", payByLine());
        }
        return body;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> counts(String q, String status, String birthday, boolean missing) {
        List<Map<String, Object>> rows = cards(false).stream()
                .filter(row -> matches(row, q, "", "", status, birthday, missing, false)).toList();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("all", rows.size());
        for (String lineName : List.of("Goalkeepers", "Defenders", "Midfielders", "Attackers")) {
            body.put(lineName, rows.stream().filter(row -> lineName.equals(row.get("line"))).count());
        }
        body.put("missing", cards(false).stream().filter(this::isMissing).count());
        body.put("expiring", cards(false).stream().filter(row -> Boolean.TRUE.equals(row.get("expiringSoon"))).count());
        return body;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> jerseyAvailability(Integer number, Long excludeId) {
        List<Player> squad = playerRepository.findByActiveTrueOrderByRegisterNoAsc().stream()
                .filter(player -> player.getDeletedAt() == null && player.getStatus() != PlayerStatus.FORMER).toList();
        Player owner = squad.stream()
                .filter(player -> number != null && number.equals(player.getJerseyNumber()))
                .filter(player -> excludeId == null || !player.getId().equals(excludeId))
                .findFirst().orElse(null);
        java.util.Set<Integer> used = squad.stream().map(Player::getJerseyNumber).filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        List<Integer> free = new ArrayList<>();
        for (int candidate = 1; candidate <= 99 && free.size() < 5; candidate++) {
            if (!used.contains(candidate) || (owner == null && number != null && candidate == number)) {
                if (!used.contains(candidate)) {
                    free.add(candidate);
                }
            }
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("number", number);
        body.put("taken", owner != null);
        body.put("owner", owner == null ? null : owner.getFullName());
        body.put("ownerId", owner == null ? null : owner.getId());
        body.put("suggestions", free);
        return body;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> upcoming(int days) {
        LocalDate today = LocalDate.now(clock);
        return cards(false).stream().filter(row -> {
            Object next = row.get("daysUntilBirthday");
            return next instanceof Number number && number.intValue() >= 0 && number.intValue() <= days;
        }).sorted(Comparator.comparingInt(row -> ((Number) row.get("daysUntilBirthday")).intValue())).toList();
    }

    public Map<String, Object> message(Long playerId, String subject, String body, String language) {
        assertCanMessage();
        Player player = playerRepository.findByIdAndActiveTrue(playerId).orElseThrow(() -> BusinessException.badRequest("Player not found"));
        if (player.getEmail() == null || player.getEmail().isBlank()) {
            throw BusinessException.badRequest("This player has no email address", "MISSING_EMAIL");
        }
        String html = mailService.layout(subject, "<div>" + body + "</div>");
        var queued = mailService.enqueue(player.getEmail(), subject, html, "player-message", language, "PLAYER", String.valueOf(playerId));
        mailService.dispatch(queued.getId());
        auditService.record("EMAIL", "Player", playerId, null, subject, null);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", queued.getId());
        result.put("status", queued.getStatus());
        result.put("note", mailService.deliveryNote());
        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> messages(Long playerId) {
        return mailService.related("PLAYER", String.valueOf(playerId));
    }

    public String archive(Long id) {
        Player player = playerRepository.findByIdAndActiveTrue(id).orElseThrow(() -> BusinessException.badRequest("Player not found"));
        boolean history = !recordRepository.findByPlayerId(id).isEmpty();
        player.setActive(false);
        player.setDeletedAt(Instant.now(clock));
        auditService.record("ARCHIVE", "Player", id, null, player.getFullName(), null);
        return history
                ? "Archived. Match history was kept, so the player was not erased."
                : "Player archived.";
    }

    public int runBirthdays(BirthdayService birthdayService) {
        if (!SecurityUtils.isExecutive()) {
            throw BusinessException.forbidden("Only an executive can run the birthday job");
        }
        return birthdayService.run(LocalDate.now(clock));
    }

    private boolean canSeeEmergency() {
        return SecurityUtils.isExecutive()
                || SecurityUtils.hasRole(Role.ASSISTANT_MANAGER.name())
                || SecurityUtils.hasRole(Role.HEAD_COACH.name())
                || SecurityUtils.hasRole(Role.DOCTOR.name());
    }

    private void assertCanMessage() {
        boolean allowed = SecurityUtils.isExecutive()
                || SecurityUtils.hasRole(Role.ASSISTANT_MANAGER.name())
                || SecurityUtils.hasRole(Role.HEAD_COACH.name());
        if (!allowed) {
            throw BusinessException.forbidden("You cannot send official mail");
        }
    }

    private List<Map<String, Object>> cards(boolean archived) {
        LocalDate today = LocalDate.now(clock);
        Long seasonId = settingsService.currentSeason().getId();
        boolean executive = SecurityUtils.isExecutive();
        List<Map<String, Object>> rows = new ArrayList<>();
        java.util.Set<Integer> shared = new java.util.HashSet<>(playerRepository.findDuplicateJerseys());
        for (Player player : playerRepository.findAll()) {
            if (player.getStatus() == PlayerStatus.FORMER || Boolean.TRUE.equals(player.getAnonymized())) {
                continue;
            }
            boolean gone = player.getDeletedAt() != null || !player.isActive();
            if (gone != archived) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", player.getId());
            row.put("jersey", player.getJerseyNumber());
            row.put("fullName", player.getFullName());
            row.put("knownAs", player.getKnownAs());
            row.put("position", player.getPosition().name());
            row.put("line", player.getLineName());
            row.put("photoUrl", player.getPhotoUrl());
            row.put("status", player.getStatus().name());
            row.put("nationality", player.getNationality());
            row.put("phone", player.getPhone());
            row.put("email", player.getEmail());
            row.put("dateOfBirth", player.getDateOfBirth());
            row.put("age", age(player));
            row.put("preferredLanguage", player.getPreferredLanguage());
            row.put("address", player.getAddress());
            row.put("foot", player.getPreferredFoot());
            row.put("heightCm", player.getHeightCm());
            row.put("weightKg", player.getWeightKg());
            row.put("version", player.getVersion());
            row.put("duplicate", player.getJerseyNumber() != null && shared.contains(player.getJerseyNumber()));
            row.put("registerNo", player.getRegisterNo());
            if (canSeeEmergency()) {
                row.put("emergencyName", player.getEmergencyContactName());
                row.put("emergencyPhone", player.getEmergencyContactPhone());
                row.put("emergencyRelationship", player.getEmergencyContactRelationship());
                row.put("emergencyEmail", player.getEmergencyContactEmail());
            }
            captainRepository.findBySeasonIdAndPlayerIdAndEndedAtIsNull(seasonId, player.getId())
                    .ifPresent(captain -> row.put("captain", captain.getRank().name()));
            contractRepository.findByPlayerIdAndSeasonIdAndActiveTrue(player.getId(), seasonId).ifPresent(contract -> {
                row.put("contractEnd", contract.getEndDate());
                row.put("expiringSoon", contract.getEndDate() != null && !contract.getEndDate().isAfter(today.plusMonths(3)));
                if (executive) {
                    row.put("seasonalPay", contract.getSeasonalPay());
                    row.put("releaseClause", contract.getReleaseClauseAmount());
                    row.put("advance", contract.getAdvanceAtSigning());
                    row.put("marketValue", contract.getMarketValue());
                    row.put("sfExempt", contract.isSfExempt());
                    row.put("contractStart", contract.getStartDate());
                }
            });
            int missingCount = missingFields(player);
            row.put("missingCount", missingCount);
            row.put("profileComplete", missingCount == 0);
            if (player.getDateOfBirth() != null) {
                LocalDate celebration = BirthdayService.celebration(player.getDateOfBirth(), today.getYear());
                if (celebration.isBefore(today)) {
                    celebration = BirthdayService.celebration(player.getDateOfBirth(), today.getYear() + 1);
                }
                row.put("daysUntilBirthday", ChronoUnit.DAYS.between(today, celebration));
            }
            rows.add(row);
        }
        return rows;
    }

    private int missingFields(Player player) {
        int count = 0;
        if (blank(player.getPhotoUrl())) count++;
        if (blank(player.getFullName())) count++;
        if (player.getDateOfBirth() == null) count++;
        if (player.getPosition() == null) count++;
        if (player.getJerseyNumber() == null) count++;
        if (blank(player.getNationality())) count++;
        if (player.getPreferredFoot() == null) count++;
        if (player.getHeightCm() == null) count++;
        if (player.getWeightKg() == null) count++;
        if (player.getStatus() == null) count++;
        if (blank(player.getPhone())) count++;
        if (blank(player.getEmail())) count++;
        if (blank(player.getPreferredLanguage())) count++;
        if (blank(player.getAddress())) count++;
        if (blank(player.getEmergencyContactName())) count++;
        if (blank(player.getEmergencyContactRelationship())) count++;
        if (blank(player.getEmergencyContactPhone())) count++;
        return count;
    }

    private boolean matches(Map<String, Object> row, String q, String line, String position, String status, String birthday, boolean missing, boolean expiring) {
        if (q != null && !q.isBlank()) {
            String hay = (String.valueOf(row.get("fullName")) + " " + row.get("knownAs") + " " + row.get("jersey") + " " + row.get("email")).toLowerCase(Locale.ROOT);
            if (!hay.contains(q.toLowerCase(Locale.ROOT))) {
                return false;
            }
        }
        if (line != null && !line.isBlank() && !line.equalsIgnoreCase(String.valueOf(row.get("line")))) {
            return false;
        }
        if (position != null && !position.isBlank() && !position.equalsIgnoreCase(String.valueOf(row.get("position")))) {
            return false;
        }
        if (status != null && !status.isBlank() && !status.equalsIgnoreCase(String.valueOf(row.get("status")))) {
            return false;
        }
        if (birthday != null && !birthday.isBlank()) {
            Object days = row.get("daysUntilBirthday");
            int value = days instanceof Number number ? number.intValue() : 999;
            boolean ok = switch (birthday) {
                case "today" -> value == 0;
                case "week" -> value <= 7;
                case "month" -> value <= 31;
                case "30" -> value <= 30;
                default -> true;
            };
            if (!ok) {
                return false;
            }
        }
        if (missing && !isMissing(row)) {
            return false;
        }
        if (expiring && !Boolean.TRUE.equals(row.get("expiringSoon"))) {
            return false;
        }
        return true;
    }

    private boolean isMissing(Map<String, Object> row) {
        return row.get("dateOfBirth") == null || blank(row.get("email")) || blank(row.get("phone")) || blank(row.get("emergencyName"));
    }

    private boolean blank(Object value) {
        return value == null || String.valueOf(value).isBlank();
    }

    private Comparator<Map<String, Object>> comparator(String sort, String dir) {
        boolean desc = "desc".equalsIgnoreCase(dir);
        return (left, right) -> {
            int result = switch (sort == null ? "jersey" : sort) {
                case "name" -> String.valueOf(left.get("fullName")).compareToIgnoreCase(String.valueOf(right.get("fullName")));
                case "position" -> String.valueOf(left.get("position")).compareTo(String.valueOf(right.get("position")));
                case "age" -> Integer.compare(number(left.get("age")), number(right.get("age")));
                case "contract" -> String.valueOf(left.get("contractEnd")).compareTo(String.valueOf(right.get("contractEnd")));
                case "jersey", "jerseyNumber" -> Integer.compare(jerseyKey(left.get("jersey")), jerseyKey(right.get("jersey")));
                default -> Integer.compare(jerseyKey(left.get("jersey")), jerseyKey(right.get("jersey")));
            };
            if (result == 0) {
                result = String.valueOf(left.get("fullName")).compareToIgnoreCase(String.valueOf(right.get("fullName")));
            }
            return desc ? -result : result;
        };
    }

    private int jerseyKey(Object value) {
        return value instanceof Number number ? number.intValue() : Integer.MAX_VALUE;
    }

    private int number(Object value) {
        return value instanceof Number n ? n.intValue() : Integer.MAX_VALUE;
    }

    private LocalDate dateOrNull(Object value) {
        String raw = text(value);
        if (raw.isBlank()) {
            return null;
        }
        return LocalDate.parse(raw.substring(0, Math.min(10, raw.length())));
    }

    public Object save(Map<String, Object> body) {
        return save(body, false);
    }

    public Object save(Map<String, Object> body, boolean hasPhoto) {
        assertCanEdit();
        Long id = body.get("id") == null || String.valueOf(body.get("id")).isBlank() ? null : Long.valueOf(String.valueOf(body.get("id")));
        boolean creating = id == null;
        boolean importing = Boolean.TRUE.equals(body.get("importing"));
        if (creating && !SecurityUtils.isExecutive()) {
            throw BusinessException.forbidden("Only an executive can add a player, because the contract is required");
        }
        boolean executive = SecurityUtils.isExecutive();
        List<Map<String, Object>> issues = PlayerProfileRules.issues(body, creating, executive, hasPhoto || !creating || importing, LocalDate.now(clock));
        List<Map<String, Object>> blocking = PlayerProfileRules.blocking(issues, creating);
        if (!blocking.isEmpty()) {
            throw new BusinessException(org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY,
                    "Complete the player profile", "VALIDATION", Map.of("errors", blocking));
        }
        String fullName = text(body.get("fullName"));
        String knownAs = text(body.get("knownAs"));
        if (knownAs.isBlank() && !fullName.isBlank()) {
            knownAs = fullName.split(" ")[0];
        }
        Integer jersey = intOrNull(body.get("jersey"));
        String positionName = text(body.get("position"));
        LocalDate dob = text(body.get("dateOfBirth")).isBlank() ? null : LocalDate.parse(text(body.get("dateOfBirth")).substring(0, 10));
        Position position = positionName.isBlank() ? null : Position.valueOf(positionName);
        String email = text(body.get("email"));
        String phone = normalizePhone(text(body.get("phone")));
        if (!text(body.get("phone")).isBlank()) {
            RwandaPhone.require(phone);
        }
        String emergencyPhone = text(body.get("emergencyPhone"));
        if (!emergencyPhone.isBlank()) {
            RwandaPhone.require(emergencyPhone);
        }
        if (jersey != null) {
            Map<String, Object> availability = jerseyAvailability(jersey, id);
            if (Boolean.TRUE.equals(availability.get("taken"))) {
                throw BusinessException.conflict("Number " + jersey + " is already assigned to " + availability.get("owner"),
                        "JERSEY_TAKEN", availability);
            }
        }
        boolean confirm = Boolean.TRUE.equals(body.get("confirmDuplicate"));
        if (!confirm) {
            for (Player other : playerRepository.findByActiveTrueOrderByRegisterNoAsc()) {
                if (id != null && other.getId().equals(id)) {
                    continue;
                }
                boolean sameEmail = !email.isBlank() && email.equalsIgnoreCase(other.getEmail());
                boolean samePhone = !phone.isBlank() && phone.equals(normalizePhone(other.getPhone() == null ? "" : other.getPhone()));
                boolean sameIdentity = dob != null && dob.equals(other.getDateOfBirth()) && fullName.equalsIgnoreCase(other.getFullName());
                boolean fuzzy = !fullName.isBlank() && normalizeName(fullName).equals(normalizeName(other.getFullName())) && !fullName.equalsIgnoreCase(other.getFullName());
                if (sameEmail || samePhone || sameIdentity || fuzzy) {
                    throw BusinessException.conflict("A similar player is already on the squad: " + other.getFullName(),
                            "PLAYER_EXISTS", Map.of("playerId", other.getId(), "fullName", other.getFullName(), "fuzzy", fuzzy));
                }
            }
        }
        Player existing = id == null ? null : playerRepository.findById(id).orElseThrow(() -> BusinessException.badRequest("Player not found"));
        if (existing != null && body.get("version") != null && existing.getVersion() != null
                && !existing.getVersion().equals(Long.valueOf(String.valueOf(body.get("version"))))) {
            throw BusinessException.conflict("This player was updated by someone else. Reload and try again.", "VERSION", null);
        }
        int registerNo = existing != null ? existing.getRegisterNo() : nextRegister();
        if (existing != null) {
            if (jersey == null) jersey = existing.getJerseyNumber();
            if (position == null) position = existing.getPosition();
            if (fullName.isBlank()) fullName = existing.getFullName();
            if (knownAs.isBlank()) knownAs = existing.getKnownAs() == null ? "" : existing.getKnownAs();
            if (dob == null) dob = existing.getDateOfBirth();
            if (phone.isBlank()) phone = existing.getPhone() == null ? "" : existing.getPhone();
            if (email.isBlank()) email = existing.getEmail() == null ? "" : existing.getEmail();
        }
        var contract = existing == null ? null : contractRepository.findByPlayerIdAndSeasonIdAndActiveTrue(existing.getId(), settingsService.currentSeason().getId()).orElse(null);
        BigDecimal pay = executive && !text(body.get("seasonalPay")).isBlank() ? money(body.get("seasonalPay")) : contract == null ? BigDecimal.ZERO : contract.getSeasonalPay();
        BigDecimal advance = executive && !text(body.get("advance")).isBlank() ? money(body.get("advance")) : contract == null ? BigDecimal.ZERO : contract.getAdvanceAtSigning();
        boolean exempt = body.containsKey("sfExempt") && executive ? flag(body.get("sfExempt")) : contract != null && contract.isSfExempt();
        String nationality = text(body.get("nationality"));
        if (nationality.isBlank()) nationality = existing != null && existing.getNationality() != null ? existing.getNationality() : "Rwanda";
        String foot = text(body.get("foot"));
        if (foot.isBlank() && existing != null) foot = existing.getPreferredFoot() == null ? "" : existing.getPreferredFoot();
        Integer height = intOrNull(body.get("heightCm"));
        if (height == null && existing != null) height = existing.getHeightCm();
        Integer weight = intOrNull(body.get("weightKg"));
        if (weight == null && existing != null) weight = existing.getWeightKg();
        PlayerStatus status = text(body.get("status")).isBlank()
                ? (existing != null && existing.getStatus() != null ? existing.getStatus() : PlayerStatus.ACTIVE)
                : PlayerStatus.valueOf(text(body.get("status")));
        PlayerDtos.Write write = new PlayerDtos.Write(registerNo, jersey, fullName, knownAs, position, dob,
                nationality,
                phone, email, foot, height, weight,
                status,
                false, existing == null ? LocalDate.now(clock) : existing.getSignedOn(),
                kept(text(body.get("emergencyName")), existing == null ? "" : existing.getEmergencyContactName()),
                kept(RwandaPhone.normalize(emergencyPhone), existing == null ? "" : existing.getEmergencyContactPhone()),
                kept(text(body.get("notes")), existing == null ? "" : existing.getNotes()),
                pay, exempt, advance,
                executive && !text(body.get("releaseClause")).isBlank() ? money(body.get("releaseClause")) : contract == null ? null : contract.getReleaseClauseAmount(),
                false, executive && !text(body.get("marketValue")).isBlank() ? money(body.get("marketValue")) : contract == null ? null : contract.getMarketValue(),
                contract == null ? null : contract.getAgentName(), contract == null ? null : contract.getAgentCommission(),
                text(body.get("preferredLanguage")).isBlank() ? (existing != null && existing.getPreferredLanguage() != null ? existing.getPreferredLanguage() : "en") : text(body.get("preferredLanguage")),
                kept(text(body.get("emergencyRelationship")), existing == null ? "" : existing.getEmergencyContactRelationship()),
                kept(text(body.get("emergencyEmail")), existing == null ? "" : existing.getEmergencyContactEmail()),
                kept(text(body.get("address")), existing == null ? "" : existing.getAddress()), confirm,
                executive ? dateOrNull(body.get("contractStart")) : null,
                executive ? dateOrNull(body.get("contractEnd")) : null,
                !executive ? null : Boolean.TRUE.equals(body.get("sfExempt")) ? BigDecimal.ZERO
                        : text(body.get("socialFund")).isBlank() ? null : money(body.get("socialFund")));
        return existing == null ? playerService.create(write) : playerService.update(id, write);
    }

    public String restore(Long id) {
        assertCanEdit();
        Player player = playerRepository.findById(id).orElseThrow(() -> BusinessException.badRequest("Player not found"));
        player.setActive(true);
        player.setDeletedAt(null);
        auditService.record("RESTORE", "Player", id, null, player.getFullName(), null);
        return "Player restored";
    }

    public String destroy(Long id) {
        if (!SecurityUtils.isExecutive()) {
            throw BusinessException.forbidden("Only an executive can erase a player");
        }
        boolean history = !recordRepository.findByPlayerId(id).isEmpty() || !fineRepository.findByPlayerIdOrderByFineDateAsc(id).isEmpty();
        if (history) {
            throw BusinessException.conflict("This player has matches or fines. Archive them instead.", "HAS_HISTORY", null);
        }
        Player player = playerRepository.findById(id).orElseThrow(() -> BusinessException.badRequest("Player not found"));
        player.setActive(false);
        player.setDeletedAt(Instant.now(clock));
        auditService.record("DELETE", "Player", id, null, player.getFullName(), null);
        return "Player removed";
    }

    public Map<String, Object> importCsv(String csv, boolean confirm) {
        assertCanEdit();
        String[] lines = csv.split("\\R");
        int ready = 0;
        List<String> errors = new ArrayList<>();
        for (int index = 1; index < lines.length; index++) {
            if (lines[index].isBlank()) {
                continue;
            }
            String[] cells = lines[index].split(",");
            if (cells.length < 4) {
                errors.add("Row " + (index + 1) + " is incomplete");
                continue;
            }
            if (!confirm) {
                ready++;
                continue;
            }
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("fullName", cells[0].replace("\"", "").trim());
            body.put("knownAs", cells.length > 1 ? cells[1].replace("\"", "").trim() : "");
            body.put("jersey", cells.length > 2 ? cells[2].replace("\"", "").trim() : "");
            body.put("position", cells.length > 3 ? cells[3].replace("\"", "").trim() : "");
            body.put("dateOfBirth", cells.length > 4 ? cells[4].replace("\"", "").trim() : "");
            body.put("nationality", cells.length > 5 ? cells[5].replace("\"", "").trim() : "Rwanda");
            body.put("phone", cells.length > 6 ? cells[6].replace("\"", "").trim() : "");
            body.put("email", cells.length > 7 ? cells[7].replace("\"", "").trim() : "");
            body.put("confirmDuplicate", true);
            try {
                save(body);
                ready++;
            } catch (RuntimeException ex) {
                errors.add("Row " + (index + 1) + ": " + ex.getMessage());
            }
        }
        return Map.of("ready", ready, "errors", errors, "confirmed", confirm);
    }

    public List<List<String>> exportRows() {
        return cards(false).stream().map(row -> List.of(
                cell(row.get("fullName")), cell(row.get("jersey")), cell(row.get("position")),
                cell(row.get("nationality")), cell(row.get("phone")), cell(row.get("status"))
        )).toList();
    }

    public String exportCsv() {
        StringBuilder csv = new StringBuilder("fullName,knownAs,jersey,position,dateOfBirth,nationality,phone,email,status\n");
        for (Map<String, Object> row : cards(false)) {
            csv.append(cell(row.get("fullName"))).append(',').append(cell(row.get("knownAs"))).append(',')
                    .append(cell(row.get("jersey"))).append(',').append(cell(row.get("position"))).append(',')
                    .append(cell(row.get("dateOfBirth"))).append(',').append(cell(row.get("nationality"))).append(',')
                    .append(cell(row.get("phone"))).append(',').append(cell(row.get("email"))).append(',')
                    .append(cell(row.get("status"))).append('\n');
        }
        return csv.toString();
    }

    private void assertCanEdit() {
        if (!SecurityUtils.isExecutive() && !SecurityUtils.hasRole(Role.ASSISTANT_MANAGER.name())) {
            throw BusinessException.forbidden("You cannot edit players");
        }
    }

    private List<Map<String, Object>> averageBy(List<Player> players, Function<Player, String> key) {
        Map<String, List<Integer>> grouped = new LinkedHashMap<>();
        for (Player player : players) {
            Integer years = age(player);
            if (years == null) {
                continue;
            }
            grouped.computeIfAbsent(key.apply(player), ignored -> new ArrayList<>()).add(years);
        }
        return grouped.entrySet().stream().map(entry -> {
            double average = entry.getValue().stream().mapToInt(Integer::intValue).average().orElse(0);
            return Map.<String, Object>of("name", entry.getKey(), "age", Math.round(average * 10) / 10.0, "count", entry.getValue().size());
        }).toList();
    }

    private List<Map<String, Object>> depth(List<Player> players) {
        Map<Position, Integer> minimum = Map.of(Position.GOALKEEPER, 2, Position.CENTER_BACK, 2, Position.LEFT_BACK, 1,
                Position.RIGHT_BACK, 1, Position.DEFENSIVE_MIDFIELDER, 1, Position.CENTRAL_MIDFIELDER, 2,
                Position.OFFENSIVE_MIDFIELDER, 1, Position.WINGER, 2, Position.FORWARD, 2);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Position position : Position.values()) {
            long count = players.stream().filter(player -> player.getPosition() == position).count();
            int floor = minimum.getOrDefault(position, 1);
            rows.add(Map.of("name", position.name(), "count", count, "minimum", floor, "thin", count < floor));
        }
        return rows;
    }

    private List<Map<String, Object>> ageByLine(List<Player> players) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (String line : List.of("Goalkeepers", "Defenders", "Midfielders", "Attackers")) {
            int[] bands = new int[6];
            for (Player player : players) {
                if (!line.equals(player.getLineName()) || age(player) == null) {
                    continue;
                }
                int years = age(player);
                int index = years <= 19 ? 0 : years <= 22 ? 1 : years <= 25 ? 2 : years <= 28 ? 3 : years <= 31 ? 4 : 5;
                bands[index]++;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", line);
            row.put("17-19", bands[0]);
            row.put("20-22", bands[1]);
            row.put("23-25", bands[2]);
            row.put("26-28", bands[3]);
            row.put("29-31", bands[4]);
            row.put("32+", bands[5]);
            rows.add(row);
        }
        return rows;
    }

    private List<Map<String, Object>> contractQuarters() {
        int year = LocalDate.now(clock).getYear();
        int[] counts = new int[4];
        for (var contract : contractRepository.findBySeasonIdAndActiveTrue(settingsService.currentSeason().getId())) {
            if (contract.getEndDate() == null || contract.getEndDate().getYear() != year) {
                continue;
            }
            counts[(contract.getEndDate().getMonthValue() - 1) / 3]++;
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int quarter = 0; quarter < 4; quarter++) {
            rows.add(Map.of("name", "Q" + (quarter + 1), "count", counts[quarter]));
        }
        return rows;
    }

    private List<Map<String, Object>> birthdaysByMonth(List<Player> players) {
        int[] months = new int[12];
        for (Player player : players) {
            if (player.getDateOfBirth() != null) {
                months[player.getDateOfBirth().getMonthValue() - 1]++;
            }
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        String[] names = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        for (int i = 0; i < 12; i++) {
            rows.add(Map.of("name", names[i], "count", months[i]));
        }
        return rows;
    }

    private Map<String, Object> completeness(List<Player> players) {
        long complete = players.stream().filter(player -> player.getDateOfBirth() != null
                && player.getEmail() != null && !player.getEmail().isBlank()
                && player.getPhone() != null && !player.getPhone().isBlank()
                && player.getEmergencyContactName() != null && !player.getEmergencyContactName().isBlank()).count();
        int size = players.size();
        return Map.of("complete", complete, "squad", size, "percent", size == 0 ? 0 : Math.round(complete * 1000.0 / size) / 10.0);
    }

    private List<Map<String, Object>> payByLine() {
        return cards(false).stream()
                .filter(row -> row.get("seasonalPay") instanceof BigDecimal)
                .collect(java.util.stream.Collectors.groupingBy(row -> String.valueOf(row.get("line")),
                        java.util.stream.Collectors.reducing(BigDecimal.ZERO, row -> (BigDecimal) row.get("seasonalPay"), BigDecimal::add)))
                .entrySet().stream().map(entry -> Map.<String, Object>of("name", entry.getKey(), "amount", entry.getValue())).toList();
    }

    private int nextRegister() {
        return playerRepository.findAll().stream().map(Player::getRegisterNo).filter(java.util.Objects::nonNull).max(Integer::compareTo).orElse(0) + 1;
    }

    private String normalizePhone(String raw) {
        String digits = raw.replaceAll("[^0-9+]", "");
        if (digits.isBlank()) {
            return "";
        }
        if (digits.startsWith("0")) {
            return "+250" + digits.substring(1);
        }
        if (digits.startsWith("+")) {
            return digits;
        }
        if (digits.startsWith("250")) {
            return "+" + digits;
        }
        return "+250" + digits;
    }

    private String normalizeName(String name) {
        return name.toLowerCase(Locale.ROOT).replace("-", " ").replaceAll("\\s+", " ").trim();
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private Integer intOrNull(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.valueOf(String.valueOf(value));
    }

    private BigDecimal money(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(String.valueOf(value));
    }

    private Map<String, String> err(String field, String code, String message) {
        return Map.of("field", field, "code", code, "message", message);
    }

    private boolean flag(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        String text = String.valueOf(value).toLowerCase(Locale.ROOT);
        return text.equals("true") || text.equals("yes") || text.equals("oui") || text.equals("1");
    }

    private String kept(String incoming, String current) {
        if (!incoming.isBlank()) {
            return incoming;
        }
        return current == null ? "" : current;
    }

    private String cell(Object value) {
        String text = value == null ? "" : String.valueOf(value).replace("\"", "\"\"");
        return "\"" + text + "\"";
    }

    private Integer age(Player player) {
        if (player.getDateOfBirth() == null) {
            return null;
        }
        return Period.between(player.getDateOfBirth(), LocalDate.now(clock)).getYears();
    }

    private List<Map<String, Object>> bands(List<Integer> ages) {
        int[] limits = {19, 22, 25, 28, 31, 200};
        String[] names = {"17-19", "20-22", "23-25", "26-28", "29-31", "32+"};
        List<Map<String, Object>> rows = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < limits.length; i++) {
            int end = limits[i];
            int count = 0;
            for (Integer age : ages) {
                if (age >= (i == 0 ? 0 : limits[i - 1] + 1) && age <= end) {
                    count++;
                }
            }
            rows.add(Map.of("name", names[i], "count", count));
            start = end;
        }
        return rows;
    }

    private List<Map<String, Object>> count(List<String> values) {
        Map<String, Integer> totals = new LinkedHashMap<>();
        for (String value : values) {
            totals.merge(value, 1, Integer::sum);
        }
        return totals.entrySet().stream().map(entry -> Map.<String, Object>of("name", entry.getKey(), "count", entry.getValue())).toList();
    }
}
