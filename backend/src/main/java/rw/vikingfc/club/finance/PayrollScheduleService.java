package rw.vikingfc.club.finance;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.vikingfc.club.common.util.Money;
import rw.vikingfc.club.contract.PlayerContract;
import rw.vikingfc.club.contract.PlayerContractRepository;
import rw.vikingfc.club.settings.ClubSettings;
import rw.vikingfc.club.settings.SettingsService;
import rw.vikingfc.club.staff.StaffContract;
import rw.vikingfc.club.staff.StaffContractRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PayrollScheduleService {

    private final PayrollInstalmentRepository instalmentRepository;
    private final PaymentRepository paymentRepository;
    private final PlayerContractRepository playerContractRepository;
    private final StaffContractRepository staffContractRepository;
    private final SettingsService settingsService;

    public void ensurePlayer(PlayerContract contract) {
        if (contract.getPlayer() == null || instalmentRepository.existsByPlayer_Id(contract.getPlayer().getId())) {
            return;
        }
        ClubSettings settings = settingsService.current();
        PayrollCalculator.Result result = PayrollCalculator.calculate(
                contract.getSeasonalPay(), contract.isSfExempt(), contract.getAdvanceAtSigning(),
                settings.getPlayerContractMonths(), settings.getPlayerFirstPhaseMonths(), contract.socialFundOr(settings.getPlayerSocialFund()));
        saveSchedule(contract.getPlayer(), null, settings, contract.getStartDate(), contract.getAdvanceAtSigning(), result);
    }

    public void ensureStaff(StaffContract contract) {
        if (contract.getStaff() == null || instalmentRepository.existsByStaff_Id(contract.getStaff().getId())) {
            return;
        }
        ClubSettings settings = settingsService.current();
        PayrollCalculator.Result result = PayrollCalculator.calculate(
                contract.getSeasonalPay(), contract.isSfExempt(), contract.getAdvanceAtSigning(),
                settings.getStaffContractMonths(), settings.getStaffFirstPhaseMonths(), settings.getStaffSocialFund());
        saveSchedule(null, contract.getStaff(), settings, contract.getStartDate(), contract.getAdvanceAtSigning(), result);
    }

    public void ensureAll() {
        if (instalmentRepository.count() > 0) {
            return;
        }
        ClubSettings settings = settingsService.current();
        Long seasonId = settings.getSeason().getId();
        for (PlayerContract contract : playerContractRepository.findBySeasonIdAndActiveTrue(seasonId)) {
            PayrollCalculator.Result result = PayrollCalculator.calculate(
                    contract.getSeasonalPay(), contract.isSfExempt(), contract.getAdvanceAtSigning(),
                    settings.getPlayerContractMonths(), settings.getPlayerFirstPhaseMonths(), contract.socialFundOr(settings.getPlayerSocialFund()));
            saveSchedule(contract.getPlayer(), null, settings, contract.getStartDate(), contract.getAdvanceAtSigning(), result);
        }
        for (StaffContract contract : staffContractRepository.findBySeasonIdAndActiveTrue(seasonId)) {
            PayrollCalculator.Result result = PayrollCalculator.calculate(
                    contract.getSeasonalPay(), contract.isSfExempt(), contract.getAdvanceAtSigning(),
                    settings.getStaffContractMonths(), settings.getStaffFirstPhaseMonths(), settings.getStaffSocialFund());
            saveSchedule(null, contract.getStaff(), settings, contract.getStartDate(), contract.getAdvanceAtSigning(), result);
        }
    }

    private void saveSchedule(rw.vikingfc.club.player.Player player, rw.vikingfc.club.staff.StaffMember staff,
                              ClubSettings settings, LocalDate start, BigDecimal advance, PayrollCalculator.Result result) {
        List<PayrollInstalment> rows = new ArrayList<>();
        if (Money.positive(advance)) {
            rows.add(row(player, staff, settings, InstalmentKind.ADVANCE_AT_SIGNING, start, advance, true));
        }
        if (Money.positive(result.envelopeAmount())) {
            rows.add(row(player, staff, settings, InstalmentKind.ENVELOPE_M1_M3, start, result.envelopeAmount(), true));
        }
        BigDecimal[] parts = PayrollCalculator.splitAcrossMonths(result.remainingBalance(), 5);
        InstalmentKind[] months = {InstalmentKind.MONTH_4, InstalmentKind.MONTH_5, InstalmentKind.MONTH_6,
                InstalmentKind.MONTH_7, InstalmentKind.MONTH_8};
        for (int i = 0; i < months.length; i++) {
            if (parts[i].signum() == 0) {
                continue;
            }
            rows.add(row(player, staff, settings, months[i], start.plusMonths(3L + i), parts[i], false));
        }
        for (PayrollInstalment instalment : rows) {
            instalmentRepository.save(instalment);
            if (instalment.getStatus() == InstalmentStatus.PAID) {
                Payment payment = new Payment();
                payment.setInstalment(instalment);
                payment.setAmount(instalment.getAmountPaid());
                payment.setMethod(PayMethod.CASH);
                payment.setPaidAt(start.atStartOfDay(ZoneId.of("Africa/Kigali")).toInstant());
                payment.setReceiptNumber(instalment.getReceiptNumber());
                payment.setReferenceNo("SIGNING");
                payment.setReversed(false);
                paymentRepository.save(payment);
            }
        }
    }

    private PayrollInstalment row(rw.vikingfc.club.player.Player player, rw.vikingfc.club.staff.StaffMember staff,
                                  ClubSettings settings, InstalmentKind kind, LocalDate due, BigDecimal amount, boolean paid) {
        PayrollInstalment instalment = new PayrollInstalment();
        instalment.setPlayer(player);
        instalment.setStaff(staff);
        instalment.setSeason(settings.getSeason());
        instalment.setKind(kind);
        instalment.setDueDate(due);
        instalment.setAmountDue(amount);
        instalment.setAmountPaid(paid ? amount : Money.zero());
        instalment.setAmountDeducted(Money.zero());
        instalment.setStatus(paid ? InstalmentStatus.PAID : InstalmentStatus.NOT_PAID);
        if (paid) {
            instalment.setPaymentDate(due);
            instalment.setMethod(PayMethod.CASH);
            instalment.setPaidBy("Signing");
            instalment.setReceiptNumber("SIGN-" + (player != null ? "P" + player.getRegisterNo() : "S" + staff.getId()) + "-" + kind.name());
        }
        return instalment;
    }

    public void refreshOverdue(LocalDate today) {
        for (PayrollInstalment instalment : instalmentRepository.findByStatusIn(
                List.of(InstalmentStatus.NOT_PAID, InstalmentStatus.PARTIALLY_PAID))) {
            if (instalment.getDueDate().isBefore(today) && instalment.cashRemaining().signum() > 0) {
                instalment.setStatus(InstalmentStatus.OVERDUE);
            }
        }
    }

}
