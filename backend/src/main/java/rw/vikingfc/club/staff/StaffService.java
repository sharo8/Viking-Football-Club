package rw.vikingfc.club.staff;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import rw.vikingfc.club.media.FileStorageService;
import rw.vikingfc.club.media.StoredFile;
import rw.vikingfc.club.common.exception.BusinessException;
import rw.vikingfc.club.common.RwandaPhone;
import rw.vikingfc.club.common.exception.NotFoundException;
import rw.vikingfc.club.user.Role;
import rw.vikingfc.club.user.RolePolicy;
import rw.vikingfc.club.user.UserRepository;
import rw.vikingfc.club.common.util.Money;
import rw.vikingfc.club.contract.ContractStatus;
import rw.vikingfc.club.finance.PayrollCalculator;
import rw.vikingfc.club.finance.PayrollScheduleService;
import rw.vikingfc.club.settings.ClubSettings;
import rw.vikingfc.club.settings.SettingsService;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    private final FileStorageService storage;
    private final StaffAccountService accounts;
    private final UserRepository users;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list() {
        ClubSettings settings = settingsService.current();
        return staffRepository.findByActiveTrueOrderByFullNameAsc().stream()
                .filter(staff -> staff.getListedIn() == null || !"ADMINISTRATION".equalsIgnoreCase(staff.getListedIn()))
                .map(staff -> view(staff, contractRepository.findByStaffIdAndSeasonIdAndActiveTrue(staff.getId(), settings.getSeason().getId()).orElse(null), settings))
                .toList();
    }

    public Map<String, Object> create(StaffWrite request) {
        ClubSettings settings = settingsService.current();
        StaffMember staff = new StaffMember();
        String code = RolePolicy.codeOf(request.roleName());
        if ("ADMINISTRATION".equals(RolePolicy.category(code))) {
            throw BusinessException.badRequest("Administration roles are managed on the Administration page", "ROLE_CATEGORY_MISMATCH");
        }
        staff.setFullName(request.fullName().trim());
        staff.setRoleName(request.roleName().trim());
        staff.setListedIn("STAFF");
        staff.setDepartment(settingsService.departmentFor(request.roleName()));
        staff.setPhone(RwandaPhone.store(request.phone()));
        staff.setEmail(request.email());
        staff.setDateOfBirth(request.dateOfBirth());
        staff.setJobTitle(request.jobTitle());
        staff.setStatus("ACTIVE");
        staff.setActive(true);
        staffRepository.save(staff);
        StaffContract contract = new StaffContract();
        contract.setStaff(staff);
        contract.setSeason(settings.getSeason());
        contract.setStartDate(request.contractStart() == null ? settings.getFirstTrainingMonday() : request.contractStart());
        contract.setEndDate(request.contractEnd() == null
                ? settings.getFirstTrainingMonday().plusMonths(settings.getStaffContractMonths()).minusDays(1)
                : request.contractEnd());
        contract.setDurationMonths(settings.getStaffContractMonths());
        contract.setRemainingMonths(settings.getStaffContractMonths());
        contract.setSeasonalPay(Money.nz(request.seasonalPay()));
        contract.setSfExempt(Boolean.TRUE.equals(request.sfExempt()));
        contract.setAdvanceAtSigning(Money.nz(request.advanceAtSigning()));
        contract.setContractStatus(ContractStatus.ACTIVE);
        contract.setActive(true);
        contractRepository.save(contract);
        payrollScheduleService.ensureStaff(contract);
        openAccount(staff, request.account());
        return view(staff, contract, settings);
    }

    public Map<String, Object> get(Long id) {
        StaffMember staff = load(id);
        ClubSettings settings = settingsService.current();
        StaffContract contract = contractRepository.findByStaffIdAndSeasonIdAndActiveTrue(staff.getId(), settings.getSeason().getId()).orElse(null);
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
        body.put("dateOfBirth", staff.getDateOfBirth());
        body.put("jobTitle", staff.getJobTitle());
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
            body.put("contractEnd", contract.getEndDate());
            body.put("contractStart", contract.getStartDate());
        }
        rw.vikingfc.club.user.User linked = staff.getUser();
        boolean live = linked != null && linked.getDeletedAt() == null;
        body.put("account", StaffAccountRules.status(live, live && linked.isActive(), live && linked.isMustChangePassword(), live && linked.getLastLoginAt() != null));
        body.put("otpRequired", rw.vikingfc.club.user.RolePolicy.otpRequired(rw.vikingfc.club.user.RolePolicy.codeOf(staff.getRoleName())));
        return body;
    }

    public Map<String, Object> update(Long id, StaffWrite request) {
        StaffMember staff = load(id);
        String code = RolePolicy.codeOf(request.roleName());
        if ("ADMINISTRATION".equals(RolePolicy.category(code))) {
            throw BusinessException.badRequest("Administration roles are managed on the Administration page", "ROLE_CATEGORY_MISMATCH");
        }
        staff.setFullName(request.fullName().trim());
        staff.setRoleName(request.roleName().trim());
        staff.setDepartment(settingsService.departmentFor(request.roleName()));
        staff.setPhone(RwandaPhone.store(request.phone()));
        staff.setEmail(request.email());
        if (request.dateOfBirth() != null) staff.setDateOfBirth(request.dateOfBirth());
        if (request.jobTitle() != null) staff.setJobTitle(request.jobTitle());
        if (staff.getUser() != null && staff.getUser().getDeletedAt() == null) {
            StaffAccountRules.staffCategory(code);
            Role role;
            try {
                role = Role.valueOf(code);
            } catch (IllegalArgumentException ex) {
                throw BusinessException.badRequest("Staff accounts use a staff role", "ROLE_CATEGORY_MISMATCH");
            }
            StaffAccountRules.headCoachSeat(code, users.countByRoleAndActiveTrueAndDeletedAtIsNull(role), code.equals(staff.getUser().getRoleCode()));
            staff.getUser().setFullName(staff.getFullName());
            staff.getUser().setRole(role);
            staff.getUser().setRoleCode(code);
        }
        ClubSettings settings = settingsService.current();
        StaffContract contract = contractRepository.findByStaffIdAndSeasonIdAndActiveTrue(staff.getId(), settings.getSeason().getId()).orElse(null);
        if (contract != null) {
            if (request.contractStart() != null) contract.setStartDate(request.contractStart());
            if (request.contractEnd() != null) contract.setEndDate(request.contractEnd());
        }
        openAccount(staff, request.account());
        return view(staff, contract, settings);
    }

    private void openAccount(StaffMember staff, AccountRequest account) {
        if (account == null || !Boolean.TRUE.equals(account.enabled())) {
            return;
        }
        if (staff.getUser() != null && staff.getUser().getDeletedAt() == null) {
            return;
        }
        accounts.open(staff, account.email() == null || account.email().isBlank() ? staff.getEmail() : account.email(),
                account.sendInvitation() == null || account.sendInvitation(), account.language());
    }

    public Map<String, Object> photo(Long id, MultipartFile file) {
        StaffMember staff = load(id);
        String previous = staff.getPhotoUrl();
        StoredFile stored = storage.store(file, "people");
        if (previous != null && !previous.equals(stored.url())) {
            storage.delete(previous);
        }
        staff.setPhotoUrl(stored.url());
        if (staff.getUser() != null) {
            staff.getUser().setPhotoUrl(stored.url());
        }
        ClubSettings settings = settingsService.current();
        StaffContract contract = contractRepository
                .findByStaffIdAndSeasonIdAndActiveTrue(staff.getId(), settings.getSeason().getId())
                .orElse(null);
        return view(staff, contract, settings);
    }

    public List<List<String>> table(String q) {
        String needle = q == null ? "" : q.toLowerCase();
        return list().stream().map(row -> (Map<String, Object>) row).filter(row -> {
            String blob = (str(row.get("fullName")) + " " + str(row.get("role")) + " " + str(row.get("email"))).toLowerCase();
            return needle.isBlank() || blob.contains(needle);
        }).map(row -> List.of(str(row.get("fullName")), str(row.get("role")), str(row.get("department")), str(row.get("phone")), str(row.get("email")), str(row.get("status")))).toList();
    }

    private static String str(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    public void archive(Long id) {
        StaffMember staff = load(id);
        staff.setActive(false);
        staff.setStatus("ARCHIVED");
        staff.setDeletedAt(java.time.Instant.now());
    }

    public StaffMember load(Long id) {
        return staffRepository.findById(id).filter(StaffMember::isActive)
                .orElseThrow(() -> NotFoundException.of("Staff member", id));
    }

    public record AccountRequest(Boolean enabled, String email, Boolean sendInvitation, String language) {
    }

    public record StaffWrite(@NotBlank String fullName, @NotBlank String roleName, String department,
                             String phone, String email, @NotNull BigDecimal seasonalPay,
                             Boolean sfExempt, BigDecimal advanceAtSigning,
                             LocalDate dateOfBirth, LocalDate contractStart, LocalDate contractEnd,
                             String jobTitle, AccountRequest account) {
    }
}
