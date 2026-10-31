package rw.vikingfc.club.finance;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public final class FinanceDtos {
    private FinanceDtos() {
    }

    public record PayRequest(@NotNull BigDecimal amount, @NotNull PayMethod method, String reference) {
    }

    public record ReverseRequest(@NotNull String reason) {
    }

    public record InstalmentView(Long id, String name, InstalmentKind kind, LocalDate dueDate, BigDecimal amountDue,
                                 BigDecimal amountPaid, BigDecimal amountDeducted, BigDecimal cashRemaining,
                                 InstalmentStatus status, String receiptNumber) {
    }

    public record Statement(Long playerId, String fullName, String knownAs, PayrollCalculator.Result payroll,
                            List<InstalmentView> instalments) {
    }

    public record LineRow(String line, int players, BigDecimal seasonalPay) {
    }

    public record PositionRow(String position, int players, BigDecimal seasonalPay, BigDecimal lowest, BigDecimal highest) {
    }

    public record BandRow(String band, int players, BigDecimal seasonalPay) {
    }

    public record CashOut(BigDecimal atSigning, BigDecimal envelope, BigDecimal balance) {
    }

    public record MonthRow(int month, BigDecimal wageBill, BigDecimal cumulative, BigDecimal percentOfPayroll) {
    }

    public record Earner(String name, String position, BigDecimal seasonalPay) {
    }

    public record Summary(
            int players,
            BigDecimal seasonPayroll,
            BigDecimal monthlyWageBill,
            BigDecimal averageSeasonalPay,
            BigDecimal medianSeasonalPay,
            BigDecimal advances,
            long playersWithAdvance,
            BigDecimal envelopes,
            BigDecimal envelopesPercentOfPayroll,
            long playersWithEnvelope,
            BigDecimal socialFund,
            long socialFundExempt,
            BigDecimal netBalanceDue,
            BigDecimal paidToDate,
            BigDecimal paidPercent,
            BigDecimal workbookCheck,
            boolean workbookCheckMatchesPayroll,
            int staff,
            BigDecimal staffSeasonalPay,
            BigDecimal staffAdvances,
            BigDecimal staffEnvelopes,
            BigDecimal staffNetBalanceDue,
            BigDecimal staffPaidToDate,
            BigDecimal clubSeasonalPay,
            List<LineRow> payrollByLine,
            List<PositionRow> payrollByPosition,
            List<BandRow> salaryBands,
            Map<String, Long> paymentStatus,
            CashOut cashOutByPhase,
            List<MonthRow> monthlySchedule,
            List<Earner> topEarners
    ) {
    }
}
