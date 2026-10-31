package rw.vikingfc.club.player;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import rw.vikingfc.club.contract.CaptainRank;
import rw.vikingfc.club.finance.PayrollCalculator;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class PlayerDtos {
    private PlayerDtos() {
    }

    public record Write(
            @NotNull Integer registerNo,
            Integer jerseyNumber,
            @NotBlank String fullName,
            @NotBlank String knownAs,
            @NotNull Position position,
            LocalDate dateOfBirth,
            String nationality,
            String phone,
            String email,
            String preferredFoot,
            Integer heightCm,
            Integer weightKg,
            @NotNull PlayerStatus status,
            @NotNull Boolean newSigning,
            LocalDate signedOn,
            String emergencyContactName,
            String emergencyContactPhone,
            String notes,
            @NotNull BigDecimal seasonalPay,
            @NotNull Boolean sfExempt,
            @NotNull BigDecimal advanceAtSigning,
            BigDecimal releaseClauseAmount,
            Boolean releaseClauseActive,
            BigDecimal marketValue,
            String agentName,
            BigDecimal agentCommission,
            String preferredLanguage,
            String emergencyContactRelationship,
            String emergencyContactEmail,
            String address,
            Boolean confirmDuplicate,
            LocalDate contractStart,
            LocalDate contractEnd,
            BigDecimal socialFundAmount
    ) {
    }

    public record Public(
            Long id, Integer registerNo, Integer jerseyNumber, String fullName, String knownAs,
            String position, String line, String photoUrl, PlayerStatus status,
            boolean newSigning, LocalDate signedOn, String nationality, CaptainRank captainRank
    ) {
    }

    public record Operational(
            Long id, Integer registerNo, Integer jerseyNumber, String fullName, String knownAs,
            String position, String line, String photoUrl, PlayerStatus status,
            boolean newSigning, LocalDate signedOn, String nationality, String phone, String email,
            String preferredFoot, Integer heightCm, Integer weightKg,
            String emergencyContactName, String emergencyContactPhone, String notes, CaptainRank captainRank
    ) {
    }

    public record Full(
            Long id, Integer registerNo, Integer jerseyNumber, String fullName, String knownAs,
            String position, String line, String photoUrl, PlayerStatus status,
            boolean newSigning, LocalDate signedOn, String nationality, String phone, String email,
            String preferredFoot, Integer heightCm, Integer weightKg,
            String emergencyContactName, String emergencyContactPhone, String notes, CaptainRank captainRank,
            Long contractId, LocalDate contractStart, LocalDate contractEnd, int durationMonths, int remainingMonths,
            BigDecimal seasonalPay, boolean sfExempt, BigDecimal advanceAtSigning,
            BigDecimal releaseClauseAmount, String releaseClauseCurrency, boolean releaseClauseActive,
            BigDecimal marketValue, String agentName, BigDecimal agentCommission,
            PayrollCalculator.Result payroll, String jerseyWarning
    ) {
    }

    public record Warning(String code, String message) {
    }

    public record Photo(Long id, PhotoType type, String url, String thumbnailUrl, LocalDate takenOn) {
    }
}
