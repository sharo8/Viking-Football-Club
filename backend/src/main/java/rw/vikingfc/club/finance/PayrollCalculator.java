package rw.vikingfc.club.finance;

import rw.vikingfc.club.common.util.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Reproduces the Player Register sheet. All amounts are whole RWF.
 * paidToDate is the signing advance plus the envelope (the cash already handed over).
 */
public final class PayrollCalculator {

    private PayrollCalculator() {
    }

    public record Result(
            BigDecimal monthlyPay,
            BigDecimal threeMonthEntitlement,
            BigDecimal socialFundPhase1,
            BigDecimal net3Month,
            BigDecimal envelopeAmount,
            BigDecimal owedByPlayer,
            BigDecimal remaining5Months,
            BigDecimal socialFundPhase2,
            BigDecimal remainingBalance,
            BigDecimal netBalanceDue,
            BigDecimal totalSocialFund,
            BigDecimal paidToDate,
            BigDecimal percentSeasonPaid,
            String paymentStatus
    ) {
    }

    public static Result calculate(BigDecimal seasonalPay,
                                   boolean sfExempt,
                                   BigDecimal advanceAtSigning,
                                   int contractMonths,
                                   int firstPhaseMonths,
                                   BigDecimal socialFundPerPhase) {
        BigDecimal seasonal = Money.nz(seasonalPay);
        BigDecimal advance = Money.nz(advanceAtSigning);
        BigDecimal social = Money.nz(socialFundPerPhase);
        int months = contractMonths <= 0 ? 8 : contractMonths;
        int phase = firstPhaseMonths <= 0 ? 3 : firstPhaseMonths;

        BigDecimal monthly = seasonal.divide(BigDecimal.valueOf(months), 0, RoundingMode.HALF_UP);
        BigDecimal threeMonth = monthly.multiply(BigDecimal.valueOf(phase));
        BigDecimal social1 = sfExempt ? Money.zero() : social;
        BigDecimal net3 = threeMonth.subtract(social1);
        BigDecimal envelope = Money.maxZero(net3.subtract(advance));
        BigDecimal owed = Money.maxZero(advance.subtract(net3));
        BigDecimal remaining5 = seasonal.subtract(threeMonth);
        BigDecimal social2 = sfExempt ? Money.zero() : social;
        BigDecimal remainingBalance = remaining5.subtract(social2);
        BigDecimal netBalance = remainingBalance.subtract(owed);
        BigDecimal totalSocial = social1.add(social2);
        BigDecimal paid = advance.add(envelope);
        BigDecimal percent = seasonal.signum() == 0
                ? Money.zero()
                : paid.divide(seasonal, 4, RoundingMode.HALF_UP);

        String status;
        if (sfExempt) {
            status = "SF Exempt";
        } else if (owed.signum() > 0) {
            status = "Balance Owed to Club";
        } else if (advance.signum() > 0) {
            status = "Advance + Envelope";
        } else {
            status = "Envelope Only";
        }

        return new Result(
                monthly, threeMonth, social1, net3, envelope, owed, remaining5, social2,
                remainingBalance, netBalance, totalSocial, paid, percent, status
        );
    }

    public static BigDecimal[] splitAcrossMonths(BigDecimal amount, int months) {
        BigDecimal total = Money.nz(amount);
        int count = Math.max(months, 1);
        BigDecimal base = total.divide(BigDecimal.valueOf(count), 0, RoundingMode.DOWN);
        BigDecimal[] parts = new BigDecimal[count];
        BigDecimal allocated = Money.zero();
        for (int i = 0; i < count - 1; i++) {
            parts[i] = base;
            allocated = allocated.add(base);
        }
        parts[count - 1] = total.subtract(allocated);
        return parts;
    }
}
