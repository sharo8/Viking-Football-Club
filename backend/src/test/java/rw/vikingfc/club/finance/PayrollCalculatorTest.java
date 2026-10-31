package rw.vikingfc.club.finance;

import org.junit.jupiter.api.Test;
import rw.vikingfc.club.common.util.Money;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PayrollCalculatorTest {

    private record Player(String name, long pay, boolean exempt, long advance) {
    }

    private static final List<Player> SQUAD = List.of(
            new Player("Kwizera Jean-Marie", 240000, false, 0),
            new Player("Hitimana Mussa", 500000, false, 75000),
            new Player("Ishimwe Zaidu", 480000, false, 80000),
            new Player("Mariza Innocent", 650000, false, 0),
            new Player("Safari Gilbert", 800000, false, 150000),
            new Player("Ishimwe Habyarimana Patrick", 320000, true, 0),
            new Player("Kwizera James", 500000, false, 100000),
            new Player("Nkundineza Diogene", 1300000, false, 150000),
            new Player("Mwizerwa Shimeyi", 600000, false, 100000),
            new Player("Ntwali Ishimwe D'Amour", 400000, false, 150000),
            new Player("Kavatiri Dan", 900000, false, 75000),
            new Player("Kagenza Thierry David", 550000, false, 75000),
            new Player("Enock Musinguzi", 240000, false, 0),
            new Player("Mugisha Charles", 500000, false, 100000),
            new Player("Gikundiro Samuel", 400000, false, 100000),
            new Player("Mucyo Kevin", 1500000, false, 150000),
            new Player("Juvenal Martin", 240000, false, 0),
            new Player("Kubwimana Olivier", 1200000, false, 200000),
            new Player("Mugisha Lewis", 400000, false, 150000),
            new Player("Niyomukiza Faustin", 800000, false, 150000),
            new Player("Tangishaka Hussein", 400000, false, 80000),
            new Player("Tuyubahe Eric Fils", 400000, false, 75000),
            new Player("Kawaye Martin", 240000, false, 0),
            new Player("Longalonga Ushindi", 240000, false, 0),
            new Player("Onuchukwu Captain", 240000, false, 0)
    );

    @Test
    void workbookTotalsAndMussaAndDamour() {
        assertEquals(25, SQUAD.size());
        BigDecimal payroll = Money.zero();
        BigDecimal advances = Money.zero();
        BigDecimal envelopes = Money.zero();
        BigDecimal social = Money.zero();
        BigDecimal net = Money.zero();
        BigDecimal paid = Money.zero();
        int advancePlayers = 0;
        int envelopePlayers = 0;
        int exempt = 0;
        for (Player player : SQUAD) {
            PayrollCalculator.Result result = PayrollCalculator.calculate(
                    Money.of(player.pay), player.exempt, Money.of(player.advance), 8, 3, Money.of(12000));
            payroll = payroll.add(Money.of(player.pay));
            advances = advances.add(Money.of(player.advance));
            envelopes = envelopes.add(result.envelopeAmount());
            social = social.add(result.totalSocialFund());
            net = net.add(result.netBalanceDue());
            paid = paid.add(result.paidToDate());
            if (player.advance > 0) {
                advancePlayers++;
            }
            if (result.envelopeAmount().signum() > 0) {
                envelopePlayers++;
            }
            if (player.exempt) {
                exempt++;
            }
        }
        assertEquals(Money.of(14_040_000), payroll);
        assertEquals(Money.of(1_755_000), payroll.divide(BigDecimal.valueOf(8)));
        assertEquals(Money.of(561_600), payroll.divide(BigDecimal.valueOf(25)));
        assertEquals(Money.of(1_960_000), advances);
        assertEquals(17, advancePlayers);
        assertEquals(Money.of(3_041_000), envelopes);
        assertEquals(23, envelopePlayers);
        assertEquals(Money.of(576_000), social);
        assertEquals(1, exempt);
        assertEquals(Money.of(8_463_000), net);
        assertEquals(Money.of(5_001_000), paid);

        PayrollCalculator.Result mussa = PayrollCalculator.calculate(Money.of(500000), false, Money.of(75000), 8, 3, Money.of(12000));
        assertEquals(Money.of(62500), mussa.monthlyPay());
        assertEquals(Money.of(187500), mussa.threeMonthEntitlement());
        assertEquals(Money.of(12000), mussa.socialFundPhase1());
        assertEquals(Money.of(175500), mussa.net3Month());
        assertEquals(Money.of(100500), mussa.envelopeAmount());
        assertEquals(Money.of(312500), mussa.remaining5Months());
        assertEquals(Money.of(300500), mussa.remainingBalance());
        assertEquals(Money.of(175500), mussa.paidToDate());
        assertEquals("Advance + Envelope", mussa.paymentStatus());

        PayrollCalculator.Result damour = PayrollCalculator.calculate(Money.of(400000), false, Money.of(150000), 8, 3, Money.of(12000));
        assertEquals(Money.zero(), damour.envelopeAmount());
        assertEquals(Money.of(12000), damour.owedByPlayer());
        assertEquals(Money.of(226000), damour.netBalanceDue());
        assertEquals("Balance Owed to Club", damour.paymentStatus());

        PayrollCalculator.Result pazzo = PayrollCalculator.calculate(Money.of(320000), true, Money.zero(), 8, 3, Money.of(12000));
        assertEquals("SF Exempt", pazzo.paymentStatus());
        assertEquals(Money.zero(), pazzo.totalSocialFund());
    }
}
