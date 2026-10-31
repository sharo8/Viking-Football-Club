package rw.vikingfc.club.settings;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.vikingfc.club.audit.AuditService;
import rw.vikingfc.club.common.exception.NotFoundException;
import rw.vikingfc.club.player.Position;
import rw.vikingfc.club.season.Season;
import rw.vikingfc.club.season.SeasonRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SettingsService {

    private final ClubSettingsRepository settingsRepository;
    private final SeasonRepository seasonRepository;
    private final PositionLineMappingRepository mappingRepository;
    private final SalaryBandRepository salaryBandRepository;
    private final StaffRoleDepartmentRepository staffRoleRepository;
    private final FineTypeConfigRepository fineTypeConfigRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public ClubSettings current() {
        Season season = seasonRepository.findByCurrentSeasonTrue()
                .orElseThrow(() -> new NotFoundException("No current season is configured"));
        return settingsRepository.findBySeasonId(season.getId())
                .orElseThrow(() -> new NotFoundException("Club settings are missing for " + season.getCode()));
    }

    @Transactional(readOnly = true)
    public Season currentSeason() {
        return current().getSeason();
    }

    @Transactional(readOnly = true)
    public SettingsDtos.View view() {
        return SettingsDtos.View.of(current());
    }

    public SettingsDtos.View update(SettingsDtos.Update request) {
        ClubSettings settings = current();
        SettingsDtos.Update before = SettingsDtos.Update.from(settings);
        request.apply(settings);
        auditService.record("UPDATE", "ClubSettings", settings.getId(), before, request, null);
        return SettingsDtos.View.of(settings);
    }

    @Transactional(readOnly = true)
    public String lineFor(Position position) {
        return mappingRepository.findByPositionCode(position.name())
                .map(PositionLineMapping::getLineName)
                .orElse(position.getLabel());
    }

    @Transactional(readOnly = true)
    public List<PositionLineMapping> mappings() {
        return mappingRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<SalaryBand> bands() {
        return salaryBandRepository.findAllByOrderBySortOrderAsc();
    }

    @Transactional(readOnly = true)
    public List<StaffRoleDepartment> staffRoles() {
        return staffRoleRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<FineTypeConfig> fineTypes() {
        return fineTypeConfigRepository.findAll();
    }

    public FineTypeConfig updateFineType(Long id, SettingsDtos.FineTypeUpdate request) {
        FineTypeConfig config = fineTypeConfigRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("Fine type", id));
        config.setLabel(request.label());
        config.setDefaultAmount(request.defaultAmount());
        config.setLateAfterMinutes(request.lateAfterMinutes());
        config.setActive(request.active());
        auditService.record("UPDATE", "FineTypeConfig", id, null, request, null);
        return config;
    }

    public String departmentFor(String roleName) {
        return staffRoleRepository.findByRoleNameIgnoreCase(roleName)
                .map(StaffRoleDepartment::getDepartment)
                .orElse("Support");
    }
}
