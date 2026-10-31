package rw.vikingfc.club.contract;

import jakarta.validation.constraints.NotNull;

public final class CaptainDtos {
    private CaptainDtos() {
    }

    public record Assign(@NotNull Long playerId, @NotNull CaptainRank rank) {
    }

    public record View(Long id, Long playerId, String fullName, String knownAs, Integer jerseyNumber,
                       String photoUrl, String position, String positionCode, String status, Integer age,
                       CaptainRank rank, java.time.Instant startedAt, java.time.Instant endedAt,
                       Integer matches, Integer goals, Integer assists, Integer matchesAsCaptain,
                       String note, String assignedBy, Long seasonId, String seasonName) {
    }

    public record Summary(int assigned, int slots, Double averageAge, int withDateOfBirth,
                          int matches, int goals, int assists, Long seasonId, String seasonName, boolean readOnly) {
    }

    public record SeasonOption(Long id, String name, String code, boolean current) {
    }
}
