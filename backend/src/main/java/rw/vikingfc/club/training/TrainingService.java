package rw.vikingfc.club.training;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.vikingfc.club.common.exception.BusinessException;
import rw.vikingfc.club.common.exception.NotFoundException;
import rw.vikingfc.club.common.util.ClubDates;
import rw.vikingfc.club.settings.SettingsService;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class TrainingService {

    private final TrainingSessionRepository sessionRepository;
    private final SettingsService settingsService;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list() {
        return sessionRepository.findBySeasonIdOrderBySessionDateAsc(settingsService.currentSeason().getId())
                .stream().map(this::view).toList();
    }

    public Map<String, Object> create(SessionWrite request) {
        if (sessionRepository.countBySeasonId(settingsService.currentSeason().getId()) >= 80) {
            throw BusinessException.conflict("A season holds at most 80 training sessions");
        }
        var settings = settingsService.current();
        TrainingSession session = new TrainingSession();
        session.setSeason(settings.getSeason());
        session.setSessionDate(request.date());
        session.setWeekday(request.date().getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH));
        session.setWeekNumber(ClubDates.weekNumber(settings.getFirstTrainingMonday(), request.date()));
        session.setSessionType(request.type());
        session.setStartTime(request.startTime());
        session.setLocation(request.location());
        session.setNotes(request.notes());
        return view(sessionRepository.save(session));
    }

    public TrainingSession load(Long id) {
        return sessionRepository.findById(id).orElseThrow(() -> NotFoundException.of("Training session", id));
    }

    private Map<String, Object> view(TrainingSession session) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", session.getId());
        body.put("date", session.getSessionDate());
        body.put("weekday", session.getWeekday());
        body.put("weekNumber", session.getWeekNumber());
        body.put("type", session.getSessionType());
        body.put("startTime", session.getStartTime());
        body.put("location", session.getLocation());
        return body;
    }

    public record SessionWrite(@NotNull LocalDate date, @NotNull SessionType type, LocalTime startTime,
                               String location, String notes) {
    }
}
