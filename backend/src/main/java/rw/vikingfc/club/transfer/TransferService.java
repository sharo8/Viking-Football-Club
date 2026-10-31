package rw.vikingfc.club.transfer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.vikingfc.club.common.exception.BusinessException;
import rw.vikingfc.club.common.exception.NotFoundException;
import rw.vikingfc.club.common.util.Money;
import rw.vikingfc.club.contract.ContractStatus;
import rw.vikingfc.club.contract.PlayerContract;
import rw.vikingfc.club.contract.PlayerContractRepository;
import rw.vikingfc.club.finance.PayrollScheduleService;
import rw.vikingfc.club.player.Player;
import rw.vikingfc.club.player.PlayerRepository;
import rw.vikingfc.club.player.PlayerStatus;
import rw.vikingfc.club.player.Position;
import rw.vikingfc.club.settings.SettingsService;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class TransferService {

    private final TransferRepository transferRepository;
    private final PlayerRepository playerRepository;
    private final PlayerContractRepository contractRepository;
    private final SettingsService settingsService;
    private final PayrollScheduleService payrollScheduleService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list() {
        return transferRepository.findAll().stream().map(this::view).toList();
    }

    public Map<String, Object> create(TransferWrite request) {
        Transfer transfer = new Transfer();
        transfer.setPlayerName(request.playerName().trim());
        transfer.setTransferType(request.type());
        transfer.setFromClub(request.fromClub());
        transfer.setToClub(request.toClub() == null ? "Viking FC" : request.toClub());
        transfer.setFee(Money.nz(request.fee()));
        transfer.setAgentFee(Money.nz(request.agentFee()));
        transfer.setTransferDate(request.transferDate() == null ? LocalDate.now(clock) : request.transferDate());
        transfer.setContractMonths(request.contractMonths());
        transfer.setSeasonalPay(request.seasonalPay() == null ? null : Money.nz(request.seasonalPay()));
        transfer.setStatus(TransferStatus.NEGOTIATING);
        transfer.setNotes(request.notes());
        if (request.playerId() != null) {
            transfer.setPlayer(playerRepository.findByIdAndActiveTrue(request.playerId())
                    .orElseThrow(() -> NotFoundException.of("Player", request.playerId())));
        }
        return view(transferRepository.save(transfer));
    }

    public Map<String, Object> complete(Long id, Position position, Integer jerseyNumber) {
        Transfer transfer = transferRepository.findById(id).orElseThrow(() -> NotFoundException.of("Transfer", id));
        if (transfer.getStatus() == TransferStatus.COMPLETED) {
            throw BusinessException.conflict("This transfer is already completed");
        }
        boolean incoming = transfer.getTransferType() == TransferType.INCOMING
                || transfer.getTransferType() == TransferType.LOAN_IN
                || transfer.getTransferType() == TransferType.FREE_TRANSFER && transfer.getPlayer() == null;
        if (incoming && transfer.getPlayer() == null) {
            if (position == null) {
                throw BusinessException.unprocessable("An incoming transfer needs a position");
            }
            Player player = new Player();
            int next = playerRepository.findByActiveTrueOrderByRegisterNoAsc().stream()
                    .map(Player::getRegisterNo).max(Integer::compareTo).orElse(0) + 1;
            player.setRegisterNo(next);
            player.setJerseyNumber(jerseyNumber);
            player.setFullName(transfer.getPlayerName());
            player.setKnownAs(transfer.getPlayerName());
            player.setPosition(position);
            player.setLineName(settingsService.lineFor(position));
            player.setNationality("Rwanda");
            player.setStatus(transfer.getTransferType() == TransferType.LOAN_IN ? PlayerStatus.LOANED : PlayerStatus.ACTIVE);
            player.setNewSigning(true);
            player.setSignedOn(transfer.getTransferDate());
            player.setActive(true);
            playerRepository.save(player);
            var settings = settingsService.current();
            PlayerContract contract = new PlayerContract();
            contract.setPlayer(player);
            contract.setSeason(settings.getSeason());
            contract.setStartDate(transfer.getTransferDate());
            int months = transfer.getContractMonths() == null ? settings.getPlayerContractMonths() : transfer.getContractMonths();
            contract.setEndDate(transfer.getTransferDate().plusMonths(months).minusDays(1));
            contract.setDurationYears(months / 12);
            contract.setDurationMonths(months);
            contract.setRemainingMonths(months);
            contract.setSeasonalPay(transfer.getSeasonalPay() == null ? Money.zero() : transfer.getSeasonalPay());
            contract.setSfExempt(false);
            contract.setAdvanceAtSigning(Money.zero());
            contract.setReleaseClauseCurrency("RWF");
            contract.setContractStatus(ContractStatus.ACTIVE);
            contract.setActive(true);
            contractRepository.save(contract);
            payrollScheduleService.ensurePlayer(contract);
            transfer.setPlayer(player);
        }
        if (!incoming && transfer.getPlayer() != null) {
            transfer.getPlayer().setStatus(transfer.getTransferType() == TransferType.LOAN_OUT
                    ? PlayerStatus.LOANED : PlayerStatus.TRANSFERRED);
            transfer.getPlayer().setNewSigning(false);
        }
        transfer.setStatus(TransferStatus.COMPLETED);
        return view(transfer);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> totals() {
        BigDecimal income = Money.zero();
        BigDecimal expense = Money.zero();
        for (Transfer transfer : transferRepository.findAll()) {
            if (transfer.getStatus() != TransferStatus.COMPLETED) {
                continue;
            }
            if (transfer.getTransferType() == TransferType.OUTGOING || transfer.getTransferType() == TransferType.LOAN_OUT) {
                income = income.add(transfer.getFee());
            } else {
                expense = expense.add(transfer.getFee()).add(transfer.getAgentFee());
            }
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("income", income);
        body.put("expense", expense);
        return body;
    }

    private Map<String, Object> view(Transfer transfer) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", transfer.getId());
        body.put("playerId", transfer.getPlayer() == null ? null : transfer.getPlayer().getId());
        body.put("playerName", transfer.getPlayerName());
        body.put("type", transfer.getTransferType());
        body.put("fromClub", transfer.getFromClub());
        body.put("toClub", transfer.getToClub());
        body.put("fee", transfer.getFee());
        body.put("agentFee", transfer.getAgentFee());
        body.put("date", transfer.getTransferDate());
        body.put("contractMonths", transfer.getContractMonths());
        body.put("seasonalPay", transfer.getSeasonalPay());
        body.put("status", transfer.getStatus());
        body.put("newSigning", transfer.getPlayer() != null && transfer.getPlayer().isNewSigning());
        return body;
    }

    public record TransferWrite(@NotBlank String playerName, Long playerId, @NotNull TransferType type,
                                String fromClub, String toClub, BigDecimal fee, BigDecimal agentFee,
                                LocalDate transferDate, Integer contractMonths, BigDecimal seasonalPay, String notes) {
    }
}
