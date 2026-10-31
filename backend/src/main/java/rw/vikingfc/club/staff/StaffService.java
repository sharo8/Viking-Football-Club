package rw.vikingfc.club.staff;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.vikingfc.club.common.exception.NotFoundException;
import rw.vikingfc.club.common.util.Money;
import rw.vikingfc.club.contract.ContractStatus;
import rw.vikingfc.club.finance.PayrollCalculator;
import rw.vikingfc.club.finance.PayrollScheduleService;
import rw.vikingfc.club.settings.ClubSettings;
import rw.vikingfc.club.settings.SettingsService;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class StaffService {

    private final StaffMemberRepository staffRepository;
    private final StaffContractRepository contractRepository;
    private final SettingsService settingsService;
    private final PayrollScheduleService payrollScheduleService;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list() {
        ClubSettings settings = settingsService.current();
        return staffRepository.findByActiveTrueOrderByFullNameAsc().stream()
                .map(staff -> view(staff, contractRepository.findByStaffIdAndSeasonIdAndActiveTrue(staff.getId(), settings.getSeason().getId()).orElse(null), settings))
                .toList();
    }

    public Map<String, Object> create(StaffWrite request) {
        ClubSettings settings = settingsService.current();
        StaffMember staff = new StaffMember();
        staff.setFullName(request.fullName().trim());
        staff.setRoleName(request.roleName().trim());
        staff.setDepartment(request.department() == null || request.department().isBlank()
                ? settingsService.departmentFor(request.roleName()) : request.department());
        staff.setPhone(request.phone());
        staff.setEmail(request.email());
        staff.setStatus("ACTIVE");
        staff.setActive(true);
        staffRepository.save(staff);
        StaffContract contract = new StaffContract();
        contract.setStaff(staff);
        contract.setSeason(settings.getSeason());
        contract.setStartDate(settings.getFirstTrainingMonday());
        contract.setEndDate(settings.getFirstTrainingMonday().plusMonths(settings.getStaffContractMonths()).minusDays(1));
        contract.setDurationMonths(settings.getStaffContractMonths());
        contract.setRemainingMonths(settings.getStaffContractMonths());
        contract.setSeasonalPay(Money.nz(request.seasonalPay()));
        contract.setSfExempt(Boolean.TRUE.equals(request.sfExempt()));
        contract.setAdvanceAtSigning(Money.nz(request.advanceAtSigning()));
        contract.setContractStatus(ContractStatus.ACTIVE);
        contract.setActive(true);
        contractRepository.save(contract);
        payrollScheduleService.ensureStaff(contract);
        return view(staff, contract, settings);
    }

    private Map<String, Object> view(StaffMember staff, StaffContract contract, ClubSettings settings) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", staff.getId());
        body.put("fullName", staff.getFullName());
        body.put("role", staff.getRoleName());
        body.put("department", staff.getDepartment());
        body.put("phone", staff.getPhone());
        body.put("email", staff.getEmail());
        body.put("photoUrl", staff.getPhotoUrl());
        body.put("status", staff.getStatus());
        if (contract != null) {
            PayrollCalculator.Result result = PayrollCalculator.calculate(
                    contract.getSeasonalPay(), contract.isSfExempt(), contract.getAdvanceAtSigning(),
                    settings.getStaffContractMonths(), settings.getStaffFirstPhaseMonths(), settings.getStaffSocialFund());
            body.put("seasonalPay", contract.getSeasonalPay());
            body.put("monthlyPay", result.monthlyPay());
            body.put("advanceAtSigning", contract.getAdvanceAtSigning());
            body.put("envelope", result.envelopeAmount());
            body.put("netBalanceDue", result.netBalanceDue());
            body.put("paidToDate", result.paidToDate());
            body.put("paymentStatus", result.paymentStatus());
        }
        return body;
    }

    public StaffMember load(Long id) {
        return staffRepository.findById(id).filter(StaffMember::isActive)
                .orElseThrow(() -> NotFoundException.of("Staff member", id));
    }

    public record StaffWrite(@NotBlank String fullName, @NotBlank String roleName, String department,
                             String phone, String email, @NotNull BigDecimal seasonalPay,
                             Boolean sfExempt, BigDecimal advanceAtSigning) {
    }
}
