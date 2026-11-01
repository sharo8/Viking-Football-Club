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
import rw.vikingfc.club.sanction.Fine;
import rw.vikingfc.club.sanction.FineRepository;
import rw.vikingfc.club.sanction.FineStatus;
import rw.vikingfc.club.settings.ClubSettings;
import rw.vikingfc.club.settings.SalaryBand;
import rw.vikingfc.club.settings.SettingsService;
import rw.vikingfc.club.contract.ContractStatus;
import rw.vikingfc.club.staff.StaffContract;
import rw.vikingfc.club.staff.StaffContractRepository;
import rw.vikingfc.club.staff.StaffMember;
import rw.vikingfc.club.staff.StaffMemberRepository;
import rw.vikingfc.club.user.User;
import rw.vikingfc.club.user.UserRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class FinanceService {

    private final PayrollInstalmentRepository instalmentRepository;
    private final PaymentRepository paymentRepository;
    private final PlayerContractRepository playerContractRepository;
    private final StaffContractRepository staffContractRepository;
    private final PlayerRepository playerRepository;
    private final StaffMemberRepository staffMemberRepository;
    private final SettingsService settingsService;
    private final UserRepository userRepository;
    private final FineRepository fineRepository;
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
            throw BusinessException.conflict("Payment exceeds the amount still due on this instalment", "OVERPAYMENT", null);
        }
        if (request.method() == PayMethod.MOBILE_MONEY && (request.reference() == null || request.reference().isBlank())) {
            throw BusinessException.unprocessable("Mobile money needs a reference", "REFERENCE_REQUIRED");
        }
        LocalDate paidOn = request.paidOn() == null ? LocalDate.now(clock) : request.paidOn();
        if (paidOn.isAfter(LocalDate.now(clock))) {
            throw BusinessException.unprocessable("A payment date cannot be in the future", "FUTURE_DATE");
        }
        instalment.setAmountPaid(Money.nz(instalment.getAmountPaid()).add(amount));
        instalment.setMethod(request.method());
        instalment.setReferenceNo(request.reference());
        instalment.setPaymentDate(paidOn);
        instalment.setPaidBy(SecurityUtils.email());
        instalment.setReceiptNumber(nextReceipt());
        refreshStatus(instalment);
        Payment payment = new Payment();
        payment.setInstalment(instalment);
        payment.setAmount(amount);
        payment.setMethod(request.method());
        payment.setReferenceNo(request.reference());
        payment.setPaidAt(paidOn.atStartOfDay(java.time.ZoneId.of("Africa/Kigali")).toInstant());
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
            if (contract.getPlayer() == null || !contract.getPlayer().isActive() || !onPayroll(contract.getSeasonalPay())) {
                continue;
            }
            PayrollCalculator.Result result = PayrollCalculator.calculate(contract.getSeasonalPay(), contract.isSfExempt(),
                    contract.getAdvanceAtSigning(), settings.getPlayerContractMonths(), settings.getPlayerFirstPhaseMonths(),
                    contract.socialFundOr(settings.getPlayerSocialFund()));
            players.add(new Row(contract.getPlayer().getFullName(), contract.getPlayer().getLineName(),
                    contract.getPlayer().getPosition().getLabel(), contract.getSeasonalPay(), contract.isSfExempt(), result));
        }
        List<Row> staff = new ArrayList<>();
        for (StaffContract contract : staffContractRepository.findBySeasonIdAndActiveTrue(settings.getSeason().getId())) {
            if (contract.getStaff() == null || !contract.getStaff().isActive() || !onPayroll(contract.getSeasonalPay())) {
                continue;
            }
            PayrollCalculator.Result result = PayrollCalculator.calculate(contract.getSeasonalPay(), contract.isSfExempt(),
                    contract.getAdvanceAtSigning(), settings.getStaffContractMonths(), settings.getStaffFirstPhaseMonths(),
                    settings.getStaffSocialFund());
            staff.add(new Row(contract.getStaff().getFullName(), contract.getStaff().getDepartment(),
                    contract.getStaff().getRoleName(), contract.getSeasonalPay(), contract.isSfExempt(), result));
        }
        return build(players, staff, settingsService.bands());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> roster() {
        ClubSettings settings = settingsService.current();
        Map<Long, BigDecimal> fines = new LinkedHashMap<>();
        for (Fine fine : fineRepository.findAll()) {
            if (fine.getPlayer() == null || fine.getStatus() == FineStatus.WAIVED) {
                continue;
            }
            fines.merge(fine.getPlayer().getId(), Money.maxZero(fine.balance()), BigDecimal::add);
        }
        Map<String, String> names = names();
        List<Map<String, Object>> players = new ArrayList<>();
        List<Map<String, Object>> unset = new ArrayList<>();
        Set<Long> seenPlayers = new HashSet<>();
        for (PlayerContract contract : playerContractRepository.findBySeasonIdAndActiveTrue(settings.getSeason().getId())) {
            Player player = contract.getPlayer();
            if (player == null || !player.isActive()) {
                continue;
            }
            seenPlayers.add(player.getId());
            PayrollCalculator.Result result = PayrollCalculator.calculate(contract.getSeasonalPay(), contract.isSfExempt(),
                    contract.getAdvanceAtSigning(), settings.getPlayerContractMonths(), settings.getPlayerFirstPhaseMonths(),
                    contract.socialFundOr(settings.getPlayerSocialFund()));
            BigDecimal fine = fines.getOrDefault(player.getId(), Money.zero());
            String role = player.getPosition() == null ? "" : player.getPosition().getLabel();
            Map<String, Object> row = person("PLAYER", player.getId(), player.getFullName(), player.getPhotoUrl(), player.getJerseyNumber(),
                    role, player.getLineName(), contract.getSeasonalPay(), result, fine);
            row.put("signingAdvance", contract.getAdvanceAtSigning());
            row.put("sfExempt", contract.isSfExempt());
            stamp(row, contract.getUpdatedAt(), names.getOrDefault(lower(contract.getUpdatedBy()), contract.getUpdatedBy()), "players");
            if (onPayroll(contract.getSeasonalPay())) {
                players.add(row);
            } else {
                unset.add(row);
            }
        }
        for (Player player : playerRepository.findByActiveTrueOrderByRegisterNoAsc()) {
            if (seenPlayers.contains(player.getId())) {
                continue;
            }
            String role = player.getPosition() == null ? "" : player.getPosition().getLabel();
            PayrollCalculator.Result result = PayrollCalculator.calculate(Money.zero(), false, Money.zero(),
                    settings.getPlayerContractMonths(), settings.getPlayerFirstPhaseMonths(), settings.getPlayerSocialFund());
            Map<String, Object> row = person("PLAYER", player.getId(), player.getFullName(), player.getPhotoUrl(), player.getJerseyNumber(),
                    role, player.getLineName(), Money.zero(), result, fines.getOrDefault(player.getId(), Money.zero()));
            stamp(row, player.getUpdatedAt(), names.getOrDefault(lower(player.getUpdatedBy()), player.getUpdatedBy()), "players");
            unset.add(row);
        }
        List<Map<String, Object>> staffRows = new ArrayList<>();
        List<Map<String, Object>> administration = new ArrayList<>();
        Set<Long> seenStaff = new HashSet<>();
        for (StaffContract contract : staffContractRepository.findBySeasonIdAndActiveTrue(settings.getSeason().getId())) {
            StaffMember member = contract.getStaff();
            if (member == null || !member.isActive()) {
                continue;
            }
            seenStaff.add(member.getId());
            PayrollCalculator.Result result = PayrollCalculator.calculate(contract.getSeasonalPay(), contract.isSfExempt(),
                    contract.getAdvanceAtSigning(), settings.getStaffContractMonths(), settings.getStaffFirstPhaseMonths(),
                    settings.getStaffSocialFund());
            String group = "ADMINISTRATION".equalsIgnoreCase(member.getListedIn()) ? "administration" : "staff";
            Map<String, Object> row = person("STAFF", member.getId(), member.getFullName(), member.getPhotoUrl(), null,
                    member.getRoleName(), member.getDepartment(), contract.getSeasonalPay(), result, Money.zero());
            row.put("signingAdvance", contract.getAdvanceAtSigning());
            row.put("sfExempt", contract.isSfExempt());
            stamp(row, contract.getUpdatedAt(), names.getOrDefault(lower(contract.getUpdatedBy()), contract.getUpdatedBy()), group);
            if (!onPayroll(contract.getSeasonalPay())) {
                unset.add(row);
            } else if ("administration".equals(group)) {
                administration.add(row);
            } else {
                staffRows.add(row);
            }
        }
        for (StaffMember member : staffMemberRepository.findByActiveTrueOrderByFullNameAsc()) {
            if (seenStaff.contains(member.getId())) {
                continue;
            }
            String group = "ADMINISTRATION".equalsIgnoreCase(member.getListedIn()) ? "administration" : "staff";
            PayrollCalculator.Result result = PayrollCalculator.calculate(Money.zero(), false, Money.zero(),
                    settings.getStaffContractMonths(), settings.getStaffFirstPhaseMonths(), settings.getStaffSocialFund());
            Map<String, Object> row = person("STAFF", member.getId(), member.getFullName(), member.getPhotoUrl(), null,
                    member.getRoleName(), member.getDepartment(), Money.zero(), result, Money.zero());
            stamp(row, member.getUpdatedAt(), names.getOrDefault(lower(member.getUpdatedBy()), member.getUpdatedBy()), group);
            unset.add(row);
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("season", settings.getSeason().getName());
        body.put("players", pack(players));
        body.put("staff", pack(staffRows));
        body.put("administration", pack(administration));
        body.put("payNotSet", unset);
        return body;
    }

    public Map<String, Object> updatePay(String type, Long id, BigDecimal seasonalPay, BigDecimal advance, boolean sfExempt, String reason) {
        BigDecimal seasonal = Money.nz(seasonalPay);
        BigDecimal signing = Money.nz(advance);
        if (reason == null || reason.isBlank()) {
            throw BusinessException.unprocessable("A reason for the change is required", "REASON");
        }
        if (seasonal.signum() <= 0) {
            throw BusinessException.unprocessable("Seasonal pay must be greater than zero", "AMOUNT");
        }
        if (signing.signum() < 0 || signing.compareTo(seasonal) > 0) {
            throw BusinessException.unprocessable("The advance must be between zero and the seasonal pay", "ADVANCE");
        }
        ClubSettings settings = settingsService.current();
        if ("PLAYER".equalsIgnoreCase(type)) {
            Player player = playerRepository.findById(id).orElseThrow(() -> NotFoundException.of("Player", id));
            PlayerContract contract = playerContractRepository.findByPlayerIdAndSeasonIdAndActiveTrue(id, settings.getSeason().getId())
                    .orElseGet(() -> newPlayerContract(player, settings));
            PayrollCalculator.Result current = PayrollCalculator.calculate(contract.getSeasonalPay(), contract.isSfExempt(),
                    contract.getAdvanceAtSigning(), settings.getPlayerContractMonths(), settings.getPlayerFirstPhaseMonths(),
                    contract.socialFundOr(settings.getPlayerSocialFund()));
            if (seasonal.compareTo(current.paidToDate()) < 0) {
                throw BusinessException.unprocessable("Seasonal pay cannot go below what was already paid", "BELOW_PAID");
            }
            refuseSocialFund(seasonal, sfExempt, settings.getPlayerContractMonths(), settings.getPlayerFirstPhaseMonths(), settings.getPlayerSocialFund());
            BigDecimal before = contract.getSeasonalPay();
            contract.setSeasonalPay(seasonal);
            contract.setAdvanceAtSigning(signing);
            contract.setSfExempt(sfExempt);
            playerContractRepository.save(contract);
            auditService.record("UPDATE_PAY", "PlayerContract", contract.getId(), before, seasonal + " " + reason.trim(), null);
            return Map.of("id", id, "seasonal", seasonal);
        }
        StaffMember member = staffMemberRepository.findById(id).orElseThrow(() -> NotFoundException.of("Staff", id));
        StaffContract contract = staffContractRepository.findByStaffIdAndSeasonIdAndActiveTrue(id, settings.getSeason().getId())
                .orElseGet(() -> newStaffContract(member, settings));
        PayrollCalculator.Result current = PayrollCalculator.calculate(contract.getSeasonalPay(), contract.isSfExempt(),
                contract.getAdvanceAtSigning(), settings.getStaffContractMonths(), settings.getStaffFirstPhaseMonths(),
                settings.getStaffSocialFund());
        if (seasonal.compareTo(current.paidToDate()) < 0) {
            throw BusinessException.unprocessable("Seasonal pay cannot go below what was already paid", "BELOW_PAID");
        }
        refuseSocialFund(seasonal, sfExempt, settings.getStaffContractMonths(), settings.getStaffFirstPhaseMonths(), settings.getStaffSocialFund());
        BigDecimal before = contract.getSeasonalPay();
        contract.setSeasonalPay(seasonal);
        contract.setAdvanceAtSigning(signing);
        contract.setSfExempt(sfExempt);
        staffContractRepository.save(contract);
        auditService.record("UPDATE_PAY", "StaffContract", contract.getId(), before, seasonal + " " + reason.trim(), null);
        return Map.of("id", id, "seasonal", seasonal);
    }

    private void refuseSocialFund(BigDecimal seasonal, boolean exempt, int months, int phaseMonths, BigDecimal fund) {
        if (exempt || fund == null) return;
        BigDecimal monthly = seasonal.divide(BigDecimal.valueOf(Math.max(months, 1)), 0, RoundingMode.HALF_UP);
        BigDecimal three = monthly.multiply(BigDecimal.valueOf(phaseMonths));
        if (three.compareTo(fund) < 0) {
            DecimalFormat format = new DecimalFormat("#,##0");
            throw BusinessException.unprocessable(
                    "Monthly pay RWF " + format.format(monthly) + " gives RWF " + format.format(three)
                            + " for " + phaseMonths + " months, below the social fund RWF " + format.format(fund)
                            + ": raise the salary, mark the person social-fund exempt, or change the social fund in Settings",
                    "SOCIAL_FUND");
        }
    }

    private PlayerContract newPlayerContract(Player player, ClubSettings settings) {
        PlayerContract contract = new PlayerContract();
        contract.setPlayer(player);
        contract.setSeason(settings.getSeason());
        contract.setStartDate(settings.getSeason().getStartDate());
        contract.setEndDate(settings.getSeason().getEndDate());
        contract.setDurationYears(1);
        contract.setDurationMonths(settings.getPlayerContractMonths());
        contract.setRemainingMonths(settings.getPlayerContractMonths());
        contract.setSeasonalPay(Money.zero());
        contract.setAdvanceAtSigning(Money.zero());
        contract.setSfExempt(false);
        contract.setReleaseClauseCurrency("RWF");
        contract.setReleaseClauseActive(false);
        contract.setContractStatus(ContractStatus.ACTIVE);
        contract.setActive(true);
        return contract;
    }

    private StaffContract newStaffContract(StaffMember member, ClubSettings settings) {
        StaffContract contract = new StaffContract();
        contract.setStaff(member);
        contract.setSeason(settings.getSeason());
        contract.setStartDate(settings.getSeason().getStartDate());
        contract.setEndDate(settings.getSeason().getEndDate());
        contract.setDurationMonths(settings.getStaffContractMonths());
        contract.setRemainingMonths(settings.getStaffContractMonths());
        contract.setSeasonalPay(Money.zero());
        contract.setAdvanceAtSigning(Money.zero());
        contract.setSfExempt(false);
        contract.setContractStatus(ContractStatus.ACTIVE);
        contract.setActive(true);
        return contract;
    }

    private static boolean onPayroll(BigDecimal seasonal) {
        return Money.nz(seasonal).signum() > 0;
    }

    private Map<String, String> names() {
        Map<String, String> names = new HashMap<>();
        for (User user : userRepository.findAll()) {
            if (user.getEmail() != null && user.getFullName() != null) {
                names.put(user.getEmail().toLowerCase(), user.getFullName());
            }
        }
        return names;
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase();
    }

    private static void stamp(Map<String, Object> row, Instant updatedAt, String updatedBy, String group) {
        row.put("updatedAt", updatedAt);
        row.put("updatedBy", updatedBy == null ? "" : updatedBy);
        row.put("group", group);
    }

    private Map<String, Object> person(String type, Long id, String name, String photo, Integer jersey, String role, String line,
                                       BigDecimal seasonal, PayrollCalculator.Result result, BigDecimal fines) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", id);
        row.put("personType", type);
        row.put("fullName", name);
        row.put("photoUrl", photo);
        row.put("jerseyNumber", jersey);
        row.put("role", role);
        row.put("line", line);
        row.put("seasonal", Money.nz(seasonal));
        row.put("monthly", result.monthlyPay());
        row.put("advance", Money.maxZero(result.paidToDate().subtract(result.envelopeAmount())));
        row.put("signingAdvance", Money.zero());
        row.put("sfExempt", false);
        row.put("envelope", result.envelopeAmount());
        row.put("social", result.totalSocialFund());
        row.put("paid", result.paidToDate());
        row.put("percent", result.percentSeasonPaid().multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP));
        row.put("status", result.paymentStatus());
        row.put("fines", fines);
        row.put("netDue", result.netBalanceDue());
        row.put("netPayable", result.netBalanceDue().subtract(Money.nz(fines)));
        return row;
    }

    private Map<String, Object> pack(List<Map<String, Object>> people) {
        people.sort(Comparator.comparing(row -> String.valueOf(row.get("fullName"))));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("headcount", people.size());
        body.put("seasonPayroll", sumOf(people, "seasonal"));
        body.put("monthly", sumOf(people, "monthly"));
        body.put("average", people.isEmpty() ? Money.zero() : sumOf(people, "seasonal").divide(BigDecimal.valueOf(people.size()), 0, RoundingMode.HALF_UP));
        body.put("advances", sumOf(people, "advance"));
        body.put("envelopes", sumOf(people, "envelope"));
        body.put("social", sumOf(people, "social"));
        body.put("paid", sumOf(people, "paid"));
        body.put("netDue", sumOf(people, "netDue"));
        body.put("fines", sumOf(people, "fines"));
        body.put("netPayable", sumOf(people, "netPayable"));
        BigDecimal seasonal = sumOf(people, "seasonal");
        body.put("percent", seasonal.signum() == 0 ? Money.zero() : sumOf(people, "paid").multiply(BigDecimal.valueOf(100)).divide(seasonal, 1, RoundingMode.HALF_UP));
        body.put("people", people);
        return body;
    }

    private static BigDecimal sumOf(List<Map<String, Object>> rows, String key) {
        return rows.stream().map(row -> row.get(key) instanceof BigDecimal value ? value : Money.zero()).reduce(Money.zero(), BigDecimal::add);
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

    public FinanceDtos.InstalmentView viewInstalment(PayrollInstalment instalment) {
        return view(instalment);
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
