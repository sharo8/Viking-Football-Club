package rw.vikingfc.club.bonus;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.vikingfc.club.common.exception.BusinessException;
import rw.vikingfc.club.common.exception.NotFoundException;
import rw.vikingfc.club.common.util.Money;
import rw.vikingfc.club.common.util.SecurityUtils;
import rw.vikingfc.club.config.JpaConfig;
import rw.vikingfc.club.match.EventType;
import rw.vikingfc.club.match.FootballMatch;
import rw.vikingfc.club.match.MatchEvent;
import rw.vikingfc.club.match.MatchEventRepository;
import rw.vikingfc.club.match.MatchPlayerRecord;
import rw.vikingfc.club.match.MatchPlayerRecordRepository;
import rw.vikingfc.club.match.MatchStatus;
import rw.vikingfc.club.match.Participation;
import rw.vikingfc.club.player.Player;
import rw.vikingfc.club.player.PlayerRepository;
import rw.vikingfc.club.settings.SettingsService;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class BonusService {

    private final BonusRuleRepository ruleRepository;
    private final BonusEarnedRepository earnedRepository;
    private final MatchEventRepository eventRepository;
    private final MatchPlayerRecordRepository recordRepository;
    private final PlayerRepository playerRepository;
    private final SettingsService settingsService;
    private final EntityManager entityManager;
    private final Clock clock;

    public Map<String, Object> createRule(Long playerId, BonusType type, BigDecimal amount,
                                           LocalDate validFrom, LocalDate validTo, String description) {
        BonusRule rule = new BonusRule();
        rule.setSeason(settingsService.currentSeason());
        if (playerId != null) {
            rule.setPlayer(playerRepository.findByIdAndActiveTrue(playerId)
                    .orElseThrow(() -> NotFoundException.of("Player", playerId)));
        }
        rule.setBonusType(type);
        rule.setAmount(Money.nz(amount));
        rule.setValidFrom(validFrom);
        rule.setValidTo(validTo);
        rule.setRuleActive(true);
        rule.setDescription(description);
        return ruleView(ruleRepository.save(rule));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> rules() {
        Long seasonId = settingsService.currentSeason().getId();
        return ruleRepository.findBySeasonId(seasonId).stream().map(this::ruleView).toList();
    }

    public Map<String, Object> manual(Long playerId, BonusType type, BigDecimal amount, String description, LocalDate earnedOn) {
        Player player = playerRepository.findByIdAndActiveTrue(playerId)
                .orElseThrow(() -> NotFoundException.of("Player", playerId));
        BonusEarned earned = new BonusEarned();
        earned.setPlayer(player);
        earned.setBonusType(type);
        earned.setAmount(Money.nz(amount));
        earned.setStatus(BonusStatus.PENDING);
        earned.setManualEntry(true);
        earned.setDescription(description);
        earned.setEarnedOn(earnedOn == null ? LocalDate.now(clock) : earnedOn);
        earned.setSourceKey("manual-" + player.getId() + "-" + System.nanoTime());
        return earnedView(earnedRepository.save(earned));
    }

    public Map<String, Object> approve(Long id) {
        BonusEarned earned = load(id);
        if (earned.getStatus() == BonusStatus.CANCELLED || earned.getStatus() == BonusStatus.PAID) {
            throw BusinessException.conflict("This bonus cannot be approved from status " + earned.getStatus());
        }
        earned.setStatus(BonusStatus.APPROVED);
        earned.setApprovedBy(SecurityUtils.email());
        earned.setApprovedAt(Instant.now(clock));
        return earnedView(earned);
    }

    public Map<String, Object> pay(Long id) {
        BonusEarned earned = load(id);
        if (earned.getStatus() != BonusStatus.APPROVED && earned.getStatus() != BonusStatus.PENDING) {
            throw BusinessException.conflict("Only a pending or approved bonus can be marked paid");
        }
        earned.setStatus(BonusStatus.PAID);
        if (earned.getApprovedBy() == null) {
            earned.setApprovedBy(SecurityUtils.email());
            earned.setApprovedAt(Instant.now(clock));
        }
        return earnedView(earned);
    }

    public Map<String, Object> cancel(Long id) {
        BonusEarned earned = load(id);
        if (earned.getStatus() == BonusStatus.PAID) {
            throw BusinessException.conflict("A paid bonus cannot be cancelled. Reverse it in finance first.");
        }
        earned.setStatus(BonusStatus.CANCELLED);
        return earnedView(earned);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(BonusStatus status, Long playerId) {
        List<BonusEarned> rows = playerId != null
                ? earnedRepository.findByPlayerIdOrderByEarnedOnDesc(playerId)
                : status != null ? earnedRepository.findByStatus(status) : earnedRepository.findAll();
        if (status != null && playerId != null) {
            rows = rows.stream().filter(row -> row.getStatus() == status).toList();
        }
        return rows.stream().map(this::earnedView).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> liability() {
        List<BonusEarned> rows = earnedRepository.findAll();
        BigDecimal pending = sum(rows, BonusStatus.PENDING);
        BigDecimal approved = sum(rows, BonusStatus.APPROVED);
        BigDecimal paid = sum(rows, BonusStatus.PAID);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("pending", pending);
        body.put("approved", approved);
        body.put("paid", paid);
        body.put("unpaid", pending.add(approved));
        body.put("count", rows.size());
        return body;
    }

    public void sync(FootballMatch match) {
        List<BonusRule> rules = ruleRepository.findBySeasonId(match.getSeason().getId());
        List<BonusResolver.RuleView> views = rules.stream()
                .map(rule -> new BonusResolver.RuleView(
                        rule.getPlayer() == null ? null : rule.getPlayer().getId(),
                        rule.getBonusType(), rule.getAmount(), rule.getValidFrom(), rule.getValidTo(), rule.isRuleActive()))
                .toList();
        List<BonusEarned> existing = earnedRepository.findByMatchId(match.getId());
        Set<String> kept = existing.stream()
                .filter(row -> row.isManualEntry() || row.getStatus() != BonusStatus.PENDING)
                .map(BonusEarned::getSourceKey)
                .collect(Collectors.toSet());
        existing.stream()
                .filter(row -> !row.isManualEntry() && row.getStatus() == BonusStatus.PENDING)
                .forEach(earnedRepository::delete);
        entityManager.flush();
        LocalDate on = LocalDate.ofInstant(match.getKickoff(), JpaConfig.KIGALI);
        for (MatchEvent event : eventRepository.findByMatchIdOrderByEventMinuteAsc(match.getId())) {
            if (event.getEventType() == EventType.GOAL || event.getEventType() == EventType.PENALTY_GOAL) {
                award(match, event.getPlayer(), BonusType.GOAL, views, rules, on, "goal-" + event.getId(), kept,
                        "Goal bonus, minute " + event.getEventMinute());
                if (event.getRelatedPlayer() != null) {
                    award(match, event.getRelatedPlayer(), BonusType.ASSIST, views, rules, on, "assist-" + event.getId(), kept,
                            "Assist bonus, minute " + event.getEventMinute());
                }
            } else if (event.getEventType() == EventType.ASSIST) {
                award(match, event.getPlayer(), BonusType.ASSIST, views, rules, on, "assist-event-" + event.getId(), kept,
                        "Assist bonus");
            } else if (event.getEventType() == EventType.MAN_OF_THE_MATCH) {
                award(match, event.getPlayer(), BonusType.MAN_OF_THE_MATCH, views, rules, on, "motm-" + match.getId(), kept,
                        "Man of the match");
            }
        }
        if (match.getStatus() != MatchStatus.FINISHED) {
            return;
        }
        boolean win = match.getGoalsFor() > match.getGoalsAgainst();
        boolean draw = match.getGoalsFor() == match.getGoalsAgainst();
        boolean cleanSheet = match.getGoalsAgainst() == 0;
        for (MatchPlayerRecord record : recordRepository.findByMatchId(match.getId())) {
            if (record.isParticipated()) {
                award(match, record.getPlayer(), BonusType.APPEARANCE, views, rules, on,
                        "app-" + match.getId() + "-" + record.getPlayer().getId(), kept, "Appearance");
            }
            if (record.getParticipation() == Participation.STARTED) {
                award(match, record.getPlayer(), BonusType.STARTING_XI, views, rules, on,
                        "xi-" + match.getId() + "-" + record.getPlayer().getId(), kept, "Starting XI");
                if (cleanSheet && record.getPlayer().getPosition() == rw.vikingfc.club.player.Position.GOALKEEPER) {
                    award(match, record.getPlayer(), BonusType.CLEAN_SHEET, views, rules, on,
                            "cs-" + match.getId() + "-" + record.getPlayer().getId(), kept, "Clean sheet");
                }
            }
            if (record.isParticipated() && win) {
                award(match, record.getPlayer(), BonusType.MATCH_WIN, views, rules, on,
                        "win-" + match.getId() + "-" + record.getPlayer().getId(), kept, "Match win");
            }
            if (record.isParticipated() && draw) {
                award(match, record.getPlayer(), BonusType.MATCH_DRAW, views, rules, on,
                        "draw-" + match.getId() + "-" + record.getPlayer().getId(), kept, "Match draw");
            }
        }
    }

    private void award(FootballMatch match, Player player, BonusType type, List<BonusResolver.RuleView> views,
                       List<BonusRule> rules, LocalDate on, String sourceKey, Set<String> kept, String description) {
        if (player == null || kept.contains(sourceKey)) {
            return;
        }
        BigDecimal amount = BonusResolver.amount(views, player.getId(), type, on);
        if (!Money.positive(amount)) {
            return;
        }
        BonusEarned earned = new BonusEarned();
        earned.setPlayer(player);
        earned.setMatch(match);
        earned.setRule(matchingRule(rules, player.getId(), type));
        earned.setBonusType(type);
        earned.setAmount(amount);
        earned.setStatus(BonusStatus.PENDING);
        earned.setManualEntry(false);
        earned.setDescription(description);
        earned.setSourceKey(sourceKey);
        earned.setEarnedOn(on);
        earnedRepository.save(earned);
        kept.add(sourceKey);
    }

    private BonusRule matchingRule(List<BonusRule> rules, Long playerId, BonusType type) {
        BonusRule global = null;
        for (BonusRule rule : rules) {
            if (!rule.isRuleActive() || rule.getBonusType() != type) {
                continue;
            }
            if (rule.getPlayer() != null && rule.getPlayer().getId().equals(playerId)) {
                return rule;
            }
            if (rule.getPlayer() == null) {
                global = rule;
            }
        }
        return global;
    }

    private BonusEarned load(Long id) {
        return earnedRepository.findById(id).orElseThrow(() -> NotFoundException.of("Bonus", id));
    }

    private BigDecimal sum(List<BonusEarned> rows, BonusStatus status) {
        return rows.stream().filter(row -> row.getStatus() == status)
                .map(BonusEarned::getAmount).reduce(Money.zero(), BigDecimal::add);
    }

    private Map<String, Object> ruleView(BonusRule rule) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", rule.getId());
        body.put("playerId", rule.getPlayer() == null ? null : rule.getPlayer().getId());
        body.put("playerName", rule.getPlayer() == null ? null : rule.getPlayer().getKnownAs());
        body.put("type", rule.getBonusType());
        body.put("amount", rule.getAmount());
        body.put("validFrom", rule.getValidFrom());
        body.put("validTo", rule.getValidTo());
        body.put("active", rule.isRuleActive());
        body.put("description", rule.getDescription());
        body.put("customName", rule.getCustomName());
        return body;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> playerRules(Long playerId) {
        return ruleRepository.findByPlayerIdAndSeasonId(playerId, settingsService.currentSeason().getId()).stream().map(this::ruleView).toList();
    }

    public List<Map<String, Object>> replacePlayerRules(Long playerId, List<Map<String, Object>> rows) {
        if (!SecurityUtils.isExecutive()) {
            throw BusinessException.forbidden("Only an executive can set bonuses");
        }
        if (rows.size() > 3) {
            throw BusinessException.conflict("Maximum 3 bonuses per player", "BONUS_LIMIT", null);
        }
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (Map<String, Object> row : rows) {
            BonusType type = BonusType.valueOf(String.valueOf(row.get("type")));
            String custom = row.get("customName") == null ? "" : String.valueOf(row.get("customName")).trim();
            if (type == BonusType.CUSTOM && (custom.length() < 3 || custom.length() > 40)) {
                throw BusinessException.badRequest("Others needs a name of 3 to 40 characters");
            }
            String key = type == BonusType.CUSTOM ? "CUSTOM:" + custom.toLowerCase() : type.name();
            if (!seen.add(key)) {
                throw BusinessException.conflict("Two bonuses cannot use the same type", "BONUS_DUPLICATE", null);
            }
            BigDecimal amount = new BigDecimal(String.valueOf(row.get("amount")));
            if (amount.signum() <= 0) {
                throw BusinessException.badRequest("Bonus amount must be greater than 0");
            }
        }
        Long seasonId = settingsService.currentSeason().getId();
        ruleRepository.deleteAll(ruleRepository.findByPlayerIdAndSeasonId(playerId, seasonId));
        Player player = playerRepository.findByIdAndActiveTrue(playerId).orElseThrow(() -> NotFoundException.of("Player", playerId));
        for (Map<String, Object> row : rows) {
            BonusRule rule = new BonusRule();
            rule.setPlayer(player);
            rule.setSeason(settingsService.currentSeason());
            rule.setBonusType(BonusType.valueOf(String.valueOf(row.get("type"))));
            rule.setCustomName(row.get("customName") == null ? null : String.valueOf(row.get("customName")).trim());
            rule.setAmount(new BigDecimal(String.valueOf(row.get("amount"))));
            rule.setDescription(row.get("note") == null ? null : String.valueOf(row.get("note")));
            rule.setRuleActive(row.get("active") == null || Boolean.TRUE.equals(row.get("active")));
            ruleRepository.save(rule);
        }
        return playerRules(playerId);
    }

    private Map<String, Object> earnedView(BonusEarned earned) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", earned.getId());
        body.put("playerId", earned.getPlayer().getId());
        body.put("playerName", earned.getPlayer().getKnownAs());
        body.put("matchId", earned.getMatch() == null ? null : earned.getMatch().getId());
        body.put("type", earned.getBonusType());
        body.put("amount", earned.getAmount());
        body.put("status", earned.getStatus());
        body.put("manual", earned.isManualEntry());
        body.put("description", earned.getDescription());
        body.put("earnedOn", earned.getEarnedOn());
        body.put("approvedBy", earned.getApprovedBy());
        return body;
    }
}
