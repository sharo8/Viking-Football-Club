package rw.vikingfc.club.finance;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.vikingfc.club.audit.AuditService;
import rw.vikingfc.club.common.exception.BusinessException;
import rw.vikingfc.club.common.exception.NotFoundException;
import rw.vikingfc.club.common.util.Money;
import rw.vikingfc.club.common.util.SecurityUtils;
import rw.vikingfc.club.contract.PlayerContract;
import rw.vikingfc.club.contract.PlayerContractRepository;
import rw.vikingfc.club.player.Player;
import rw.vikingfc.club.player.PlayerRepository;
import rw.vikingfc.club.settings.ClubSettings;
import rw.vikingfc.club.settings.SalaryBand;
import rw.vikingfc.club.settings.SettingsService;
import rw.vikingfc.club.staff.StaffContract;
import rw.vikingfc.club.staff.StaffContractRepository;
import rw.vikingfc.club.user.UserRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class FinanceService {

    private final PayrollInstalmentRepository instalmentRepository;
    private final PaymentRepository paymentRepository;
    private final PlayerContractRepository playerContractRepository;
    private final StaffContractRepository staffContractRepository;
    private final PlayerRepository playerRepository;
    private final SettingsService settingsService;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final Clock clock;

    public FinanceDtos.InstalmentView record(Long instalmentId, FinanceDtos.PayRequest request) {
        PayrollInstalment instalment = instalmentRepository.findById(instalmentId)
                .orElseThrow(() -> NotFoundException.of("Instalment", instalmentId));
        BigDecimal amount = Money.nz(request.amount());
        if (amount.signum() <= 0) {
            throw BusinessException.unprocessable("Payment amount must be greater than zero");
        }
        if (amount.compareTo(instalment.cashRemaining()) > 0) {
            throw BusinessException.conflict("Payment exceeds the amount still due on this instalment");
        }
        instalment.setAmountPaid(Money.nz(instalment.getAmountPaid()).add(amount));
        instalment.setMethod(request.method());
        instalment.setReferenceNo(request.reference());
        instalment.setPaymentDate(LocalDate.now(clock));
        instalment.setPaidBy(SecurityUtils.email());
        instalment.setReceiptNumber(nextReceipt());
        refreshStatus(instalment);
        Payment payment = new Payment();
        payment.setInstalment(instalment);
        payment.setAmount(amount);
        payment.setMethod(request.method());
        payment.setReferenceNo(request.reference());
        payment.setPaidAt(Instant.now(clock));
        payment.setReceiptNumber(instalment.getReceiptNumber());
        payment.setReversed(false);
        userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(SecurityUtils.email()).ifPresent(payment::setPaidByUser);
        paymentRepository.save(payment);
        auditService.record("PAY", "PayrollInstalment", instalmentId, null, amount, null);
        return view(instalment);
    }

    public FinanceDtos.InstalmentView reverse(Long paymentId, String reason) {
        Payment payment = paymentRepository.findById(paymentId).orElseThrow(() -> NotFoundException.of("Payment", paymentId));
        if (payment.isReversed()) {
            throw BusinessException.conflict("This payment is already reversed");
        }
        if (reason == null || reason.isBlank()) {
            throw BusinessException.unprocessable("A reversal reason is required");
        }
        PayrollInstalment instalment = payment.getInstalment();
        instalment.setAmountPaid(Money.maxZero(Money.nz(instalment.getAmountPaid()).subtract(payment.getAmount())));
        refreshStatus(instalment);
        payment.setReversed(true);
        payment.setReversedAt(Instant.now(clock));
        payment.setReverseReason(reason);
        payment.setReversedBy(SecurityUtils.email());
        auditService.record("REVERSE", "Payment", paymentId, payment.getAmount(), reason, null);
        return view(instalment);
    }

    @Transactional(readOnly = true)
    public FinanceDtos.Statement statement(Long playerId) {
        Player player = playerRepository.findByIdAndActiveTrue(playerId).orElseThrow(() -> NotFoundException.of("Player", playerId));
        ClubSettings settings = settingsService.current();
        PlayerContract contract = playerContractRepository
                .findByPlayerIdAndSeasonIdAndActiveTrue(playerId, settings.getSeason().getId())
                .orElseThrow(() -> new NotFoundException("No active contract for " + player.getFullName()));
        PayrollCalculator.Result payroll = PayrollCalculator.calculate(contract.getSeasonalPay(), contract.isSfExempt(),
                contract.getAdvanceAtSigning(), settings.getPlayerContractMonths(), settings.getPlayerFirstPhaseMonths(),
                contract.socialFundOr(settings.getPlayerSocialFund()));
        List<FinanceDtos.InstalmentView> rows = instalmentRepository.findByPlayerIdOrderByDueDateAsc(playerId).stream()
                .map(this::view).toList();
        return new FinanceDtos.Statement(player.getId(), player.getFullName(), player.getKnownAs(), payroll, rows);
    }

    @Transactional(readOnly = true)
    public List<FinanceDtos.InstalmentView> unpaid() {
        return instalmentRepository.findByStatusIn(List.of(InstalmentStatus.NOT_PAID, InstalmentStatus.PARTIALLY_PAID, InstalmentStatus.OVERDUE))
                .stream().map(this::view).toList();
    }

    @Transactional(readOnly = true)
    public List<FinanceDtos.InstalmentView> overdue() {
        return instalmentRepository.findByStatusIn(List.of(InstalmentStatus.OVERDUE)).stream().map(this::view).toList();
    }

    @Transactional(readOnly = true)
    public FinanceDtos.Summary summary() {
        ClubSettings settings = settingsService.current();
        List<Row> players = new ArrayList<>();
        for (PlayerContract contract : playerContractRepository.findBySeasonIdAndActiveTrue(settings.getSeason().getId())) {
            PayrollCalculator.Result result = PayrollCalculator.calculate(contract.getSeasonalPay(), contract.isSfExempt(),
                    contract.getAdvanceAtSigning(), settings.getPlayerContractMonths(), settings.getPlayerFirstPhaseMonths(),
                    contract.socialFundOr(settings.getPlayerSocialFund()));
            players.add(new Row(contract.getPlayer().getFullName(), contract.getPlayer().getLineName(),
                    contract.getPlayer().getPosition().getLabel(), contract.getSeasonalPay(), contract.isSfExempt(), result));
        }
        List<Row> staff = new ArrayList<>();
        for (StaffContract contract : staffContractRepository.findBySeasonIdAndActiveTrue(settings.getSeason().getId())) {
            PayrollCalculator.Result result = PayrollCalculator.calculate(contract.getSeasonalPay(), contract.isSfExempt(),
                    contract.getAdvanceAtSigning(), settings.getStaffContractMonths(), settings.getStaffFirstPhaseMonths(),
                    settings.getStaffSocialFund());
            staff.add(new Row(contract.getStaff().getFullName(), contract.getStaff().getDepartment(),
                    contract.getStaff().getRoleName(), contract.getSeasonalPay(), contract.isSfExempt(), result));
        }
        return build(players, staff, settingsService.bands());
    }

    private FinanceDtos.Summary build(List<Row> players, List<Row> staff, List<SalaryBand> bands) {
        Totals playerTotals = totals(players);
        Totals staffTotals = totals(staff);
        BigDecimal check = playerTotals.paid.add(playerTotals.net).add(playerTotals.social);
        Map<String, Totals> byLine = grouped(players, Row::line);
        Map<String, List<Row>> byPosition = new LinkedHashMap<>();
        for (Row row : players) {
            byPosition.computeIfAbsent(row.position, key -> new ArrayList<>()).add(row);
        }
        List<FinanceDtos.PositionRow> positions = byPosition.entrySet().stream().map(entry -> {
            List<BigDecimal> pays = entry.getValue().stream().map(row -> row.seasonal).sorted().toList();
            return new FinanceDtos.PositionRow(entry.getKey(), entry.getValue().size(), sum(entry.getValue(), row -> row.seasonal),
                    pays.get(0), pays.get(pays.size() - 1));
        }).toList();
        List<FinanceDtos.BandRow> bandRows = bands.stream().map(band -> {
            List<Row> inBand = players.stream().filter(row ->
                    row.seasonal.compareTo(band.getMinAmount()) >= 0 && row.seasonal.compareTo(band.getMaxAmount()) <= 0).toList();
            return new FinanceDtos.BandRow(band.getLabel(), inBand.size(), sum(inBand, row -> row.seasonal));
        }).toList();
        Map<String, Long> statusCounts = new LinkedHashMap<>();
        for (Row row : players) {
            statusCounts.merge(row.result.paymentStatus(), 1L, Long::sum);
        }
        List<FinanceDtos.MonthRow> months = new ArrayList<>();
        BigDecimal monthly = sum(players, row -> row.result.monthlyPay());
        BigDecimal cumulative = Money.zero();
        for (int month = 1; month <= 8; month++) {
            cumulative = cumulative.add(monthly);
            BigDecimal percent = playerTotals.seasonal.signum() == 0 ? Money.zero()
                    : cumulative.multiply(BigDecimal.valueOf(100)).divide(playerTotals.seasonal, 1, RoundingMode.HALF_UP);
            months.add(new FinanceDtos.MonthRow(month, monthly, cumulative, percent));
        }
        List<FinanceDtos.Earner> top = players.stream()
                .sorted(Comparator.comparing((Row row) -> row.seasonal).reversed().thenComparing(row -> row.name))
                .limit(10)
                .map(row -> new FinanceDtos.Earner(row.name, row.position, row.seasonal))
                .toList();
        return new FinanceDtos.Summary(
                players.size(),
                playerTotals.seasonal,
                monthly,
                average(playerTotals.seasonal, players.size()),
                median(players.stream().map(row -> row.seasonal).sorted().toList()),
                playerTotals.advance,
                players.stream().filter(row -> Money.positive(row.result.paidToDate().subtract(row.result.envelopeAmount()))).count(),
                playerTotals.envelope,
                percent(playerTotals.envelope, playerTotals.seasonal),
                players.stream().filter(row -> Money.positive(row.result.envelopeAmount())).count(),
                playerTotals.social,
                players.stream().filter(Row::exempt).count(),
                playerTotals.net,
                playerTotals.paid,
                percent(playerTotals.paid, playerTotals.seasonal),
                check,
                check.compareTo(playerTotals.seasonal) == 0,
                staff.size(),
                staffTotals.seasonal,
                staffTotals.advance,
                staffTotals.envelope,
                staffTotals.net,
                staffTotals.paid,
                playerTotals.seasonal.add(staffTotals.seasonal),
                byLine.entrySet().stream().map(entry -> new FinanceDtos.LineRow(entry.getKey(), entry.getValue().count, entry.getValue().seasonal)).toList(),
                positions,
                bandRows,
                statusCounts,
                new FinanceDtos.CashOut(playerTotals.advance, playerTotals.envelope, playerTotals.net),
                months,
                top
        );
    }

    private FinanceDtos.InstalmentView view(PayrollInstalment instalment) {
        String name = instalment.getPlayer() != null ? instalment.getPlayer().getFullName() : instalment.getStaff().getFullName();
        return new FinanceDtos.InstalmentView(instalment.getId(), name, instalment.getKind(), instalment.getDueDate(),
                instalment.getAmountDue(), instalment.getAmountPaid(), instalment.getAmountDeducted(), instalment.cashRemaining(),
                instalment.getStatus(), instalment.getReceiptNumber());
    }

    private void refreshStatus(PayrollInstalment instalment) {
        BigDecimal remaining = instalment.cashRemaining();
        if (remaining.signum() <= 0) {
            instalment.setStatus(InstalmentStatus.PAID);
        } else if (Money.positive(instalment.getAmountPaid()) || Money.positive(instalment.getAmountDeducted())) {
            instalment.setStatus(instalment.getDueDate().isBefore(LocalDate.now(clock)) ? InstalmentStatus.OVERDUE : InstalmentStatus.PARTIALLY_PAID);
        } else {
            instalment.setStatus(instalment.getDueDate().isBefore(LocalDate.now(clock)) ? InstalmentStatus.OVERDUE : InstalmentStatus.NOT_PAID);
        }
    }

    private String nextReceipt() {
        long count = paymentRepository.count() + 1;
        return "RCP-" + LocalDate.now(clock).getYear() + "-" + String.format("%04d", count);
    }

    private static BigDecimal sum(List<Row> rows, java.util.function.Function<Row, BigDecimal> getter) {
        return rows.stream().map(getter).reduce(Money.zero(), BigDecimal::add);
    }

    private static BigDecimal average(BigDecimal total, int count) {
        if (count == 0) {
            return Money.zero();
        }
        return total.divide(BigDecimal.valueOf(count), 0, RoundingMode.HALF_UP);
    }

    private static BigDecimal median(List<BigDecimal> sorted) {
        if (sorted.isEmpty()) {
            return Money.zero();
        }
        return sorted.get(sorted.size() / 2);
    }

    private static BigDecimal percent(BigDecimal part, BigDecimal whole) {
        if (whole.signum() == 0) {
            return Money.zero();
        }
        return part.multiply(BigDecimal.valueOf(100)).divide(whole, 1, RoundingMode.HALF_UP);
    }

    private static Totals totals(List<Row> rows) {
        Totals totals = new Totals();
        totals.count = rows.size();
        totals.seasonal = sum(rows, row -> row.seasonal);
        totals.advance = sum(rows, row -> row.result.paidToDate().subtract(row.result.envelopeAmount()));
        totals.envelope = sum(rows, row -> row.result.envelopeAmount());
        totals.social = sum(rows, row -> row.result.totalSocialFund());
        totals.net = sum(rows, row -> row.result.netBalanceDue());
        totals.paid = sum(rows, row -> row.result.paidToDate());
        return totals;
    }

    private static Map<String, Totals> grouped(List<Row> rows, java.util.function.Function<Row, String> key) {
        Map<String, Totals> map = new LinkedHashMap<>();
        for (Row row : rows) {
            Totals totals = map.computeIfAbsent(key.apply(row), ignored -> new Totals());
            totals.count++;
            totals.seasonal = totals.seasonal.add(row.seasonal);
        }
        return map;
    }

    private record Row(String name, String line, String position, BigDecimal seasonal, boolean exempt, PayrollCalculator.Result result) {
        private BigDecimal advance(Row ignored) {
            return result.paidToDate().subtract(result.envelopeAmount());
        }
    }

    private static final class Totals {
        private int count;
        private BigDecimal seasonal = Money.zero();
        private BigDecimal advance = Money.zero();
        private BigDecimal envelope = Money.zero();
        private BigDecimal social = Money.zero();
        private BigDecimal net = Money.zero();
        private BigDecimal paid = Money.zero();
    }
}
