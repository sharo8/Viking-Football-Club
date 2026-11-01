package rw.vikingfc.club.player;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import rw.vikingfc.club.audit.AuditService;
import rw.vikingfc.club.common.access.ClubAccess;
import rw.vikingfc.club.common.exception.BusinessException;
import rw.vikingfc.club.common.exception.NotFoundException;
import rw.vikingfc.club.common.util.ClubDates;
import rw.vikingfc.club.common.util.SecurityUtils;
import rw.vikingfc.club.contract.CaptainRank;
import rw.vikingfc.club.contract.ContractStatus;
import rw.vikingfc.club.contract.PlayerContract;
import rw.vikingfc.club.contract.PlayerContractRepository;
import rw.vikingfc.club.contract.TeamCaptainRepository;
import rw.vikingfc.club.finance.PayrollCalculator;
import rw.vikingfc.club.finance.PayrollScheduleService;
import rw.vikingfc.club.media.FileStorageService;
import rw.vikingfc.club.media.StoredFile;
import rw.vikingfc.club.settings.ClubSettings;
import rw.vikingfc.club.settings.SettingsService;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PlayerService {

    private final PlayerRepository playerRepository;
    private final PlayerPhotoRepository photoRepository;
    private final PlayerContractRepository contractRepository;
    private final TeamCaptainRepository captainRepository;
    private final SettingsService settingsService;
    private final FileStorageService fileStorageService;
    private final PlayerMapper playerMapper;
    private final AuditService auditService;
    private final PayrollScheduleService payrollScheduleService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<?> list() {
        return visible(playerRepository.findByActiveTrueOrderByRegisterNoAsc()).stream().map(this::present).toList();
    }

    @Transactional(readOnly = true)
    public Object get(Long id) {
        Player player = requireVisible(id);
        return present(player);
    }

    public Object create(PlayerDtos.Write request) {
        if (playerRepository.findByRegisterNoAndActiveTrue(request.registerNo()).isPresent()) {
            throw BusinessException.conflict("Register number " + request.registerNo() + " is already used");
        }
        Player player = new Player();
        apply(player, request);
        playerRepository.save(player);
        saveContract(player, request, true);
        auditService.record("CREATE", "Player", player.getId(), null, request.fullName(), null);
        return present(player);
    }

    public Object update(Long id, PlayerDtos.Write request) {
        Player player = requireVisible(id);
        apply(player, request);
        saveContract(player, request, false);
        auditService.record("UPDATE", "Player", id, null, request.fullName(), null);
        return present(player);
    }

    public PlayerDtos.Photo uploadPhoto(Long id, MultipartFile file, PhotoType type) {
        Player player = requireVisible(id);
        String previous = player.getPhotoUrl();
        StoredFile stored = fileStorageService.store(file, "players/" + id);
        if (previous != null && !previous.equals(stored.url())) {
            fileStorageService.delete(previous);
        }
        PlayerPhoto photo = new PlayerPhoto();
        photo.setPlayer(player);
        photo.setPhotoType(type == null ? PhotoType.PROFILE : type);
        photo.setUrl(stored.url());
        photo.setThumbnailUrl(stored.thumbnailUrl());
        photo.setUploadedBy(SecurityUtils.email());
        photo.setTakenOn(LocalDate.now(clock));
        photoRepository.save(photo);
        if (photo.getPhotoType() == PhotoType.PROFILE) {
            player.setPhotoUrl(stored.url());
        }
        return playerMapper.toPhoto(photo);
    }

    @Transactional(readOnly = true)
    public List<PlayerDtos.Photo> photos(Long id) {
        requireVisible(id);
        return photoRepository.findByPlayerIdOrderByCreatedAtDesc(id).stream().map(playerMapper::toPhoto).toList();
    }

    @Transactional(readOnly = true)
    public List<PlayerDtos.Public> newSignings() {
        return playerRepository.findByActiveTrueAndNewSigningTrueOrderBySignedOnDesc().stream()
                .filter(this::allowed)
                .map(player -> withRank(playerMapper.toPublic(player), rankOf(player)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PlayerDtos.Warning> warnings() {
        ClubSettings settings = settingsService.current();
        List<PlayerDtos.Warning> warnings = new ArrayList<>();
        for (Integer jersey : playerRepository.findDuplicateJerseys()) {
            warnings.add(new PlayerDtos.Warning("DUPLICATE_JERSEY",
                    "Jersey " + jersey + " is used by more than one player and still needs to be confirmed."));
        }
        for (Player player : playerRepository.findByActiveTrueOrderByRegisterNoAsc()) {
            contractRepository.findByPlayerIdAndSeasonIdAndActiveTrue(player.getId(), settings.getSeason().getId())
                    .ifPresent(contract -> {
                        PayrollCalculator.Result result = payroll(contract, settings);
                        if (result.owedByPlayer().signum() > 0) {
                            warnings.add(new PlayerDtos.Warning("ADVANCE_EXCEEDS_NET",
                                    player.getFullName() + " owes the club " + result.owedByPlayer() + " RWF."));
                        }
                    });
        }
        return warnings;
    }

    public Player require(Long id) {
        return playerRepository.findByIdAndActiveTrue(id).orElseThrow(() -> NotFoundException.of("Player", id));
    }

    private Player requireVisible(Long id) {
        Player player = require(id);
        if (!allowed(player)) {
            throw BusinessException.forbidden("This player is outside your area");
        }
        return player;
    }

    private List<Player> visible(List<Player> players) {
        return players.stream().filter(this::allowed).toList();
    }

    private boolean allowed(Player player) {
        return player.getStatus() != PlayerStatus.FORMER
                && !Boolean.TRUE.equals(player.getAnonymized())
                && (!ClubAccess.goalkeepersOnly() || player.getPosition() == Position.GOALKEEPER);
    }

    private void apply(Player player, PlayerDtos.Write request) {
        if (request.jerseyNumber() != null) {
            Long self = player.getId();
            playerRepository.findByActiveTrueOrderByRegisterNoAsc().stream()
                    .filter(other -> other.getDeletedAt() == null && other.getStatus() != PlayerStatus.FORMER)
                    .filter(other -> request.jerseyNumber().equals(other.getJerseyNumber()))
                    .filter(other -> self == null || !other.getId().equals(self))
                    .findFirst()
                    .ifPresent(owner -> {
                        throw BusinessException.conflict(
                                "Number " + request.jerseyNumber() + " is already assigned to " + owner.getFullName(),
                                "JERSEY_TAKEN", java.util.Map.of("owner", owner.getFullName()));
                    });
        }
        player.setRegisterNo(request.registerNo());
        player.setJerseyNumber(request.jerseyNumber());
        player.setFullName(request.fullName().trim());
        player.setKnownAs(request.knownAs() == null ? "" : request.knownAs().trim());
        if (request.position() != null) {
            player.setPosition(request.position());
            player.setLineName(settingsService.lineFor(request.position()));
        }
        player.setDateOfBirth(request.dateOfBirth());
        player.setNationality(request.nationality());
        player.setPhone(request.phone());
        player.setEmail(request.email());
        player.setPreferredFoot(request.preferredFoot());
        player.setHeightCm(request.heightCm());
        player.setWeightKg(request.weightKg());
        player.setStatus(request.status());
        player.setNewSigning(Boolean.TRUE.equals(request.newSigning()));
        player.setSignedOn(request.signedOn());
        player.setEmergencyContactName(request.emergencyContactName());
        player.setEmergencyContactPhone(request.emergencyContactPhone());
        if (request.emergencyContactRelationship() != null) {
            player.setEmergencyContactRelationship(request.emergencyContactRelationship());
        }
        if (request.emergencyContactEmail() != null) {
            player.setEmergencyContactEmail(request.emergencyContactEmail());
        }
        if (request.preferredLanguage() != null && !request.preferredLanguage().isBlank()) {
            player.setPreferredLanguage(request.preferredLanguage());
        }
        if (request.address() != null) {
            player.setAddress(request.address());
        }
        player.setNotes(request.notes());
        player.setActive(true);
    }

    private void saveContract(Player player, PlayerDtos.Write request, boolean creating) {
        ClubSettings settings = settingsService.current();
        PlayerContract contract = contractRepository
                .findByPlayerIdAndSeasonIdAndActiveTrue(player.getId(), settings.getSeason().getId())
                .orElseGet(PlayerContract::new);
        if (contract.getId() == null) {
            contract.setPlayer(player);
            contract.setSeason(settings.getSeason());
            contract.setStartDate(request.contractStart() != null ? request.contractStart() : settings.getFirstTrainingMonday());
            LocalDate fallbackEnd = contract.getStartDate().plusMonths(settings.getPlayerContractMonths()).minusDays(1);
            contract.setEndDate(request.contractEnd() != null ? request.contractEnd() : fallbackEnd);
            contract.setDurationYears(0);
            contract.setDurationMonths(settings.getPlayerContractMonths());
            contract.setReleaseClauseCurrency("RWF");
            contract.setActive(true);
        }
        if (request.contractStart() != null) {
            contract.setStartDate(request.contractStart());
        }
        if (request.contractEnd() != null) {
            contract.setEndDate(request.contractEnd());
        }
        contract.setSeasonalPay(request.seasonalPay());
        contract.setSfExempt(Boolean.TRUE.equals(request.sfExempt()));
        contract.setSocialFundAmount(Boolean.TRUE.equals(request.sfExempt()) ? java.math.BigDecimal.ZERO
                : request.socialFundAmount() != null ? request.socialFundAmount() : contract.getSocialFundAmount());
        contract.setAdvanceAtSigning(request.advanceAtSigning());
        contract.setReleaseClauseAmount(request.releaseClauseAmount());
        contract.setReleaseClauseActive(Boolean.TRUE.equals(request.releaseClauseActive()));
        contract.setMarketValue(request.marketValue());
        contract.setAgentName(request.agentName());
        contract.setAgentCommission(request.agentCommission());
        refreshStatus(contract);
        contractRepository.save(contract);
        if (creating) {
            payrollScheduleService.ensurePlayer(contract);
        }
        if (!creating) {
            auditService.record("UPDATE", "PlayerContract", contract.getId(), null, request.seasonalPay(), null);
        }
    }

    private Object present(Player player) {
        CaptainRank rank = rankOf(player);
        return switch (ClubAccess.playerView()) {
            case PUBLIC -> withRank(playerMapper.toPublic(player), rank);
            case OPERATIONAL -> operational(player, rank);
            case FULL -> full(player, rank);
        };
    }

    private PlayerDtos.Public withRank(PlayerDtos.Public view, CaptainRank rank) {
        return new PlayerDtos.Public(view.id(), view.registerNo(), view.jerseyNumber(), view.fullName(), view.knownAs(),
                view.position(), view.line(), view.photoUrl(), view.status(), view.newSigning(), view.signedOn(),
                view.nationality(), rank);
    }

    private PlayerDtos.Operational operational(Player player, CaptainRank rank) {
        return new PlayerDtos.Operational(player.getId(), player.getRegisterNo(), player.getJerseyNumber(),
                player.getFullName(), player.getKnownAs(), player.getPosition().getLabel(), player.getLineName(),
                player.getPhotoUrl(), player.getStatus(), player.isNewSigning(), player.getSignedOn(),
                player.getNationality(), player.getPhone(), player.getEmail(), player.getPreferredFoot(),
                player.getHeightCm(), player.getWeightKg(), player.getEmergencyContactName(),
                player.getEmergencyContactPhone(), player.getNotes(), rank);
    }

    private PlayerDtos.Full full(Player player, CaptainRank rank) {
        ClubSettings settings = settingsService.current();
        PlayerContract contract = contractRepository
                .findByPlayerIdAndSeasonIdAndActiveTrue(player.getId(), settings.getSeason().getId())
                .orElse(null);
        PayrollCalculator.Result payroll = contract == null ? null : payroll(contract, settings);
        String jerseyWarning = null;
        if (player.getJerseyNumber() != null && playerRepository.findDuplicateJerseys().contains(player.getJerseyNumber())) {
            jerseyWarning = "Jersey " + player.getJerseyNumber() + " is shared and needs confirmation";
        }
        return new PlayerDtos.Full(player.getId(), player.getRegisterNo(), player.getJerseyNumber(), player.getFullName(),
                player.getKnownAs(), player.getPosition().getLabel(), player.getLineName(), player.getPhotoUrl(),
                player.getStatus(), player.isNewSigning(), player.getSignedOn(), player.getNationality(), player.getPhone(),
                player.getEmail(), player.getPreferredFoot(), player.getHeightCm(), player.getWeightKg(),
                player.getEmergencyContactName(), player.getEmergencyContactPhone(), player.getNotes(), rank,
                contract == null ? null : contract.getId(),
                contract == null ? null : contract.getStartDate(),
                contract == null ? null : contract.getEndDate(),
                contract == null ? 0 : contract.getDurationMonths(),
                contract == null ? 0 : ClubDates.remainingMonths(LocalDate.now(clock), contract.getEndDate()),
                contract == null ? null : contract.getSeasonalPay(),
                contract != null && contract.isSfExempt(),
                contract == null ? null : contract.getAdvanceAtSigning(),
                contract == null ? null : contract.getReleaseClauseAmount(),
                contract == null ? null : contract.getReleaseClauseCurrency(),
                contract != null && contract.isReleaseClauseActive(),
                contract == null ? null : contract.getMarketValue(),
                contract == null ? null : contract.getAgentName(),
                contract == null ? null : contract.getAgentCommission(),
                payroll, jerseyWarning);
    }

    private PayrollCalculator.Result payroll(PlayerContract contract, ClubSettings settings) {
        return PayrollCalculator.calculate(contract.getSeasonalPay(), contract.isSfExempt(), contract.getAdvanceAtSigning(),
                settings.getPlayerContractMonths(), settings.getPlayerFirstPhaseMonths(), contract.socialFundOr(settings.getPlayerSocialFund()));
    }

    private void refreshStatus(PlayerContract contract) {
        LocalDate today = LocalDate.now(clock);
        contract.setRemainingMonths(ClubDates.remainingMonths(today, contract.getEndDate()));
        if (contract.getContractStatus() == ContractStatus.TERMINATED) {
            return;
        }
        if (contract.getEndDate().isBefore(today)) {
            contract.setContractStatus(ContractStatus.EXPIRED);
        } else if (contract.getRemainingMonths() <= 3) {
            contract.setContractStatus(ContractStatus.EXPIRING_SOON);
        } else {
            contract.setContractStatus(ContractStatus.ACTIVE);
        }
    }

    private CaptainRank rankOf(Player player) {
        return captainRepository.findBySeasonIdAndPlayerIdAndEndedAtIsNull(settingsService.currentSeason().getId(), player.getId())
                .map(captain -> captain.getRank())
                .orElse(null);
    }
}
