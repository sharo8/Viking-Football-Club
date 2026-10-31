package rw.vikingfc.club.settings;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public final class SettingsDtos {
    private SettingsDtos() {
    }

    public record View(
            Long id,
            String seasonCode,
            String clubName,
            String motto,
            String competitionName,
            String currencyCode,
            int playerContractMonths,
            int playerFirstPhaseMonths,
            BigDecimal playerSocialFund,
            int staffContractMonths,
            int staffFirstPhaseMonths,
            BigDecimal staffSocialFund,
            int attendanceOnTrackPct,
            int attendanceWatchPct,
            BigDecimal trainingLateFine,
            int trainingLateMinutes,
            BigDecimal trainingAbsenceFine,
            BigDecimal matchLateFine,
            int matchLateMinutes,
            BigDecimal matchAbsenceFine,
            String rule6Text,
            String rule7Text,
            java.time.LocalDate firstTrainingMonday,
            int maxSubstitutes,
            int matchLengthMinutes
    ) {
        public static View of(ClubSettings settings) {
            return new View(
                    settings.getId(), settings.getSeason().getCode(), settings.getClubName(), settings.getMotto(),
                    settings.getCompetitionName(), settings.getCurrencyCode(),
                    settings.getPlayerContractMonths(), settings.getPlayerFirstPhaseMonths(), settings.getPlayerSocialFund(),
                    settings.getStaffContractMonths(), settings.getStaffFirstPhaseMonths(), settings.getStaffSocialFund(),
                    settings.getAttendanceOnTrackPct(), settings.getAttendanceWatchPct(),
                    settings.getTrainingLateFine(), settings.getTrainingLateMinutes(), settings.getTrainingAbsenceFine(),
                    settings.getMatchLateFine(), settings.getMatchLateMinutes(), settings.getMatchAbsenceFine(),
                    settings.getRule6Text(), settings.getRule7Text(), settings.getFirstTrainingMonday(),
                    settings.getMaxSubstitutes(), settings.getMatchLengthMinutes());
        }
    }

    public record Update(
            @NotBlank String competitionName,
            @NotNull Integer playerContractMonths,
            @NotNull Integer playerFirstPhaseMonths,
            @NotNull BigDecimal playerSocialFund,
            @NotNull Integer staffContractMonths,
            @NotNull Integer staffFirstPhaseMonths,
            @NotNull BigDecimal staffSocialFund,
            @NotNull Integer attendanceOnTrackPct,
            @NotNull Integer attendanceWatchPct,
            @NotNull BigDecimal trainingLateFine,
            @NotNull Integer trainingLateMinutes,
            @NotNull BigDecimal trainingAbsenceFine,
            @NotNull BigDecimal matchLateFine,
            @NotNull Integer matchLateMinutes,
            @NotNull BigDecimal matchAbsenceFine,
            @NotBlank String rule6Text,
            @NotBlank String rule7Text,
            @NotNull Integer maxSubstitutes,
            @NotNull Integer matchLengthMinutes
    ) {
        public static Update from(ClubSettings settings) {
            return new Update(
                    settings.getCompetitionName(),
                    settings.getPlayerContractMonths(),
                    settings.getPlayerFirstPhaseMonths(),
                    settings.getPlayerSocialFund(),
                    settings.getStaffContractMonths(),
                    settings.getStaffFirstPhaseMonths(),
                    settings.getStaffSocialFund(),
                    settings.getAttendanceOnTrackPct(),
                    settings.getAttendanceWatchPct(),
                    settings.getTrainingLateFine(),
                    settings.getTrainingLateMinutes(),
                    settings.getTrainingAbsenceFine(),
                    settings.getMatchLateFine(),
                    settings.getMatchLateMinutes(),
                    settings.getMatchAbsenceFine(),
                    settings.getRule6Text(),
                    settings.getRule7Text(),
                    settings.getMaxSubstitutes(),
                    settings.getMatchLengthMinutes()
            );
        }

        public void apply(ClubSettings settings) {
            settings.setCompetitionName(competitionName);
            settings.setPlayerContractMonths(playerContractMonths);
            settings.setPlayerFirstPhaseMonths(playerFirstPhaseMonths);
            settings.setPlayerSocialFund(playerSocialFund);
            settings.setStaffContractMonths(staffContractMonths);
            settings.setStaffFirstPhaseMonths(staffFirstPhaseMonths);
            settings.setStaffSocialFund(staffSocialFund);
            settings.setAttendanceOnTrackPct(attendanceOnTrackPct);
            settings.setAttendanceWatchPct(attendanceWatchPct);
            settings.setTrainingLateFine(trainingLateFine);
            settings.setTrainingLateMinutes(trainingLateMinutes);
            settings.setTrainingAbsenceFine(trainingAbsenceFine);
            settings.setMatchLateFine(matchLateFine);
            settings.setMatchLateMinutes(matchLateMinutes);
            settings.setMatchAbsenceFine(matchAbsenceFine);
            settings.setRule6Text(rule6Text);
            settings.setRule7Text(rule7Text);
            settings.setMaxSubstitutes(maxSubstitutes);
            settings.setMatchLengthMinutes(matchLengthMinutes);
        }
    }

    public record FineTypeUpdate(
            @NotBlank String label,
            @NotNull BigDecimal defaultAmount,
            Integer lateAfterMinutes,
            @NotNull Boolean active
    ) {
    }
}
