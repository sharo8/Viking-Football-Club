package rw.vikingfc.club.medical;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.vikingfc.club.common.access.ClubAccess;
import rw.vikingfc.club.common.exception.NotFoundException;
import rw.vikingfc.club.common.util.SecurityUtils;
import rw.vikingfc.club.marketing.NotificationService;
import rw.vikingfc.club.player.Player;
import rw.vikingfc.club.player.PlayerRepository;
import rw.vikingfc.club.player.PlayerStatus;
import rw.vikingfc.club.user.Role;
import rw.vikingfc.club.user.User;
import rw.vikingfc.club.user.UserRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class MedicalService {

    private final InjuryRepository injuryRepository;
    private final MedicalCheckRepository checkRepository;
    private final PlayerRepository playerRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final Clock clock;

    public Map<String, Object> register(InjuryWrite request) {
        Player player = playerRepository.findByIdAndActiveTrue(request.playerId())
                .orElseThrow(() -> NotFoundException.of("Player", request.playerId()));
        Injury injury = new Injury();
        injury.setPlayer(player);
        injury.setInjuryType(request.injuryType());
        injury.setDiagnosis(request.diagnosis());
        injury.setBodyPart(request.bodyPart());
        injury.setSide(request.side());
        injury.setSeverity(request.severity());
        injury.setCause(request.cause());
        injury.setInjuryDate(request.injuryDate());
        injury.setExpectedReturnDate(request.expectedReturnDate());
        injury.setStatus(request.status() == null ? InjuryStatus.INJURED : request.status());
        injury.setTreatmentPlan(request.treatmentPlan());
        injury.setConfidentialNotes(request.confidentialNotes());
        injury.setDoctor(currentDoctor());
        injuryRepository.save(injury);
        if (injury.getStatus() != InjuryStatus.CLEARED) {
            player.setStatus(PlayerStatus.INJURED);
        }
        notificationService.notifyRoles(List.of(Role.HEAD_COACH, Role.ASSISTANT_COACH, Role.GOALKEEPER_COACH),
                "INJURY", "Injury update", player.getKnownAs() + " — " + injury.getStatus(), "/medical");
        return detail(injury);
    }

    public Map<String, Object> updateStatus(Long id, InjuryStatus status, LocalDate expectedReturn, String note) {
        Injury injury = injuryRepository.findById(id).orElseThrow(() -> NotFoundException.of("Injury", id));
        injury.setStatus(status);
        if (expectedReturn != null) {
            injury.setExpectedReturnDate(expectedReturn);
        }
        if (status == InjuryStatus.CLEARED) {
            injury.setClearedOn(LocalDate.now(clock));
            injury.setClearedBy(SecurityUtils.email());
            boolean stillOut = injuryRepository.existsByPlayerIdAndStatusNot(injury.getPlayer().getId(), InjuryStatus.CLEARED);
            if (!stillOut) {
                injury.getPlayer().setStatus(PlayerStatus.ACTIVE);
            }
            notificationService.notifyRoles(List.of(Role.HEAD_COACH), "INJURY", "Player cleared",
                    injury.getPlayer().getKnownAs() + " is available", "/medical");
        } else {
            injury.getPlayer().setStatus(PlayerStatus.INJURED);
        }
        if (note != null && !note.isBlank()) {
            MedicalCheck check = new MedicalCheck();
            check.setInjury(injury);
            check.setCheckDate(LocalDate.now(clock));
            check.setNote(note);
            check.setStatusAfter(status);
            check.setDoctor(currentDoctor());
            checkRepository.save(check);
        }
        return detail(injury);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> dashboard() {
        LocalDate today = LocalDate.now(clock);
        List<Injury> open = injuryRepository.findByStatusNot(InjuryStatus.CLEARED);
        List<Map<String, Object>> injured = open.stream().map(injury -> ClubAccess.medicalDetail() ? detail(injury) : availabilityRow(injury)).toList();
        long overdue = open.stream().filter(injury -> injury.getExpectedReturnDate() != null && injury.getExpectedReturnDate().isBefore(today)).count();
        long dueThisWeek = open.stream().filter(injury -> injury.getExpectedReturnDate() != null
                && !injury.getExpectedReturnDate().isBefore(today)
                && !injury.getExpectedReturnDate().isAfter(today.plusDays(7))).count();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("unavailable", open.size());
        body.put("overdueForReview", overdue);
        body.put("recoveriesDueThisWeek", dueThisWeek);
        body.put("notCleared", injured);
        body.put("fit", playerRepository.findByActiveTrueOrderByRegisterNoAsc().stream()
                .filter(player -> player.getStatus() != PlayerStatus.INJURED)
                .map(player -> player.getKnownAs()).toList());
        return body;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> availability(LocalDate date) {
        LocalDate on = date == null ? LocalDate.now(clock) : date;
        return playerRepository.findByActiveTrueOrderByRegisterNoAsc().stream().map(player -> {
            Injury open = injuryRepository.findByPlayerIdOrderByInjuryDateDesc(player.getId()).stream()
                    .filter(injury -> injury.getStatus() != InjuryStatus.CLEARED)
                    .findFirst().orElse(null);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("playerId", player.getId());
            row.put("name", player.getKnownAs());
            row.put("jersey", player.getJerseyNumber());
            row.put("position", player.getPosition().getLabel());
            row.put("photoUrl", player.getPhotoUrl());
            if (open == null || (open.getExpectedReturnDate() != null && open.getExpectedReturnDate().isBefore(on) && open.getStatus() == InjuryStatus.CLEARED)) {
                row.put("availability", "FIT");
                row.put("expectedReturn", null);
            } else {
                row.put("availability", open.getStatus().name());
                row.put("expectedReturn", open.getExpectedReturnDate());
            }
            return row;
        }).toList();
    }

    private Map<String, Object> detail(Injury injury) {
        Map<String, Object> body = availabilityRow(injury);
        if (ClubAccess.medicalDetail()) {
            body.put("diagnosis", injury.getDiagnosis());
            body.put("treatmentPlan", injury.getTreatmentPlan());
            body.put("confidentialNotes", injury.getConfidentialNotes());
            body.put("severity", injury.getSeverity());
            body.put("cause", injury.getCause());
        }
        return body;
    }

    private Map<String, Object> availabilityRow(Injury injury) {
        LocalDate today = LocalDate.now(clock);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", injury.getId());
        body.put("playerId", injury.getPlayer().getId());
        body.put("name", injury.getPlayer().getKnownAs());
        body.put("jersey", injury.getPlayer().getJerseyNumber());
        body.put("position", injury.getPlayer().getPosition().getLabel());
        body.put("photoUrl", injury.getPlayer().getPhotoUrl());
        body.put("injuryType", injury.getInjuryType());
        body.put("bodyPart", injury.getBodyPart());
        body.put("status", injury.getStatus());
        body.put("injuryDate", injury.getInjuryDate());
        body.put("expectedReturn", injury.getExpectedReturnDate());
        body.put("daysOut", ChronoUnit.DAYS.between(injury.getInjuryDate(), today));
        body.put("overdue", injury.getExpectedReturnDate() != null && injury.getExpectedReturnDate().isBefore(today)
                && injury.getStatus() != InjuryStatus.CLEARED);
        return body;
    }

    private User currentDoctor() {
        var principal = SecurityUtils.principal();
        if (principal == null) {
            return null;
        }
        return userRepository.findById(principal.getId()).orElse(null);
    }

    public record InjuryWrite(@NotNull Long playerId, @NotBlank String injuryType, String diagnosis, String bodyPart,
                              String side, @NotNull InjurySeverity severity, @NotNull InjuryCause cause,
                              @NotNull LocalDate injuryDate, LocalDate expectedReturnDate, InjuryStatus status,
                              String treatmentPlan, String confidentialNotes) {
    }
}
