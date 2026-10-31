package rw.vikingfc.club.attendance;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public final class AttendanceDtos {
    private AttendanceDtos() {
    }

    public record Mark(@NotNull Long playerId, @NotNull AttendanceMark status, Integer minutesLate, Boolean justified, String note) {
    }

    public record Bulk(@NotNull List<Mark> marks) {
    }
}
