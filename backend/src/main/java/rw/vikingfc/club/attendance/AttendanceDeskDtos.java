package rw.vikingfc.club.attendance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public final class AttendanceDeskDtos {
    private AttendanceDeskDtos() {
    }

    public record SessionWrite(
            @NotBlank String kind,
            @NotBlank String audience,
            @NotNull LocalDate date,
            LocalTime startTime,
            String place,
            Long matchId,
            String notes
    ) {
    }

    public record MarkWrite(
            @NotNull Long personId,
            @NotBlank String status,
            Integer minutesLate,
            String arrivalTime,
            String note
    ) {
    }

    public record MarksWrite(String reason, @NotNull List<MarkWrite> marks) {
    }

    public record ReasonWrite(@NotBlank String reason) {
    }

    public record JustificationWrite(
            @NotBlank String personType,
            @NotNull Long personId,
            Long sessionId,
            @NotBlank String reason,
            String evidenceName
    ) {
    }

    public record ImportRow(Long personId, @NotNull LocalDate date, @NotBlank String status, Integer minutesLate) {
    }

    public record ImportCommit(@NotBlank String audience, @NotBlank String kind, @NotNull List<ImportRow> rows) {
    }
}
