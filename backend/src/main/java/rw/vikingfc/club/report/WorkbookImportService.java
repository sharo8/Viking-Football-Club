package rw.vikingfc.club.report;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import rw.vikingfc.club.attendance.AttendanceDtos;
import rw.vikingfc.club.attendance.AttendanceMark;
import rw.vikingfc.club.attendance.AttendanceService;
import rw.vikingfc.club.common.util.ClubDates;
import rw.vikingfc.club.common.util.Money;
import rw.vikingfc.club.contract.PlayerContractRepository;
import rw.vikingfc.club.player.Player;
import rw.vikingfc.club.player.PlayerRepository;
import rw.vikingfc.club.sanction.Fine;
import rw.vikingfc.club.sanction.FineRepository;
import rw.vikingfc.club.sanction.FineSource;
import rw.vikingfc.club.sanction.FineStatus;
import rw.vikingfc.club.sanction.FineType;
import rw.vikingfc.club.settings.SettingsService;
import rw.vikingfc.club.training.SessionType;
import rw.vikingfc.club.training.TrainingSession;
import rw.vikingfc.club.training.TrainingSessionRepository;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class WorkbookImportService {

    private final PlayerRepository playerRepository;
    private final PlayerContractRepository contractRepository;
    private final TrainingSessionRepository sessionRepository;
    private final AttendanceService attendanceService;
    private final FineRepository fineRepository;
    private final SettingsService settingsService;
    private final DataFormatter formatter = new DataFormatter();

    public Map<String, Object> importWorkbook(MultipartFile file) {
        Map<String, Object> result = new LinkedHashMap<>();
        try (InputStream input = file.getInputStream(); Workbook book = WorkbookFactory.create(input)) {
            result.put("playersUpdated", importPlayers(book.getSheet("Player Register")));
            result.put("attendanceRows", importAttendance(book.getSheet("Attendance")));
            result.put("sanctions", importSanctions(book.getSheet("Sanctions")));
            result.put("sheets", book.getNumberOfSheets());
            return result;
        } catch (Exception ex) {
            throw new IllegalStateException("Could not read the workbook: " + ex.getMessage(), ex);
        }
    }

    private int importPlayers(Sheet sheet) {
        if (sheet == null) {
            return 0;
        }
        Map<String, Integer> columns = header(sheet.getRow(0));
        int updated = 0;
        var settings = settingsService.current();
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) {
                continue;
            }
            String name = text(row, columns, "name", "full name");
            if (name.isBlank()) {
                continue;
            }
            Player player = playerRepository.findByActiveTrueOrderByRegisterNoAsc().stream()
                    .filter(item -> item.getFullName().equalsIgnoreCase(name)).findFirst().orElse(null);
            if (player == null) {
                continue;
            }
            contractRepository.findByPlayerIdAndSeasonIdAndActiveTrue(player.getId(), settings.getSeason().getId())
                    .ifPresent(contract -> {
                        String pay = text(row, columns, "seasonal pay", "seasonalpay");
                        String advance = text(row, columns, "advance", "advance at signing");
                        String exempt = text(row, columns, "sf exempt", "social fund exempt");
                        if (!pay.isBlank()) {
                            contract.setSeasonalPay(Money.nz(new BigDecimal(pay.replace(",", ""))));
                        }
                        if (!advance.isBlank()) {
                            contract.setAdvanceAtSigning(Money.nz(new BigDecimal(advance.replace(",", ""))));
                        }
                        if (!exempt.isBlank()) {
                            contract.setSfExempt(exempt.equalsIgnoreCase("yes") || exempt.equalsIgnoreCase("true"));
                        }
                    });
            String jersey = text(row, columns, "jersey", "jersey number");
            if (!jersey.isBlank()) {
                player.setJerseyNumber((int) Double.parseDouble(jersey));
            }
            updated++;
        }
        return updated;
    }

    private int importAttendance(Sheet sheet) {
        if (sheet == null) {
            return 0;
        }
        Map<String, Integer> columns = header(sheet.getRow(0));
        Map<LocalDate, List<AttendanceDtos.Mark>> grouped = new LinkedHashMap<>();
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) {
                continue;
            }
            String name = text(row, columns, "player", "name", "known as");
            String mark = text(row, columns, "status", "mark");
            String dateText = text(row, columns, "date");
            if (name.isBlank() || mark.isBlank() || dateText.isBlank()) {
                continue;
            }
            Player player = playerRepository.findByActiveTrueOrderByRegisterNoAsc().stream()
                    .filter(item -> item.getFullName().equalsIgnoreCase(name) || item.getKnownAs().equalsIgnoreCase(name))
                    .findFirst().orElse(null);
            if (player == null) {
                continue;
            }
            LocalDate date = LocalDate.parse(dateText.substring(0, 10), DateTimeFormatter.ISO_LOCAL_DATE);
            AttendanceMark status = AttendanceMark.valueOf(mark.trim().toUpperCase(Locale.ENGLISH));
            String late = text(row, columns, "minutes late", "minuteslate");
            grouped.computeIfAbsent(date, key -> new ArrayList<>()).add(new AttendanceDtos.Mark(
                    player.getId(), status, late.isBlank() ? 0 : Integer.parseInt(late), status == AttendanceMark.E, null));
        }
        int rows = 0;
        var settings = settingsService.current();
        for (Map.Entry<LocalDate, List<AttendanceDtos.Mark>> entry : grouped.entrySet()) {
            TrainingSession session = sessionRepository.findBySessionDate(entry.getKey()).orElseGet(() -> {
                TrainingSession created = new TrainingSession();
                created.setSeason(settings.getSeason());
                created.setSessionDate(entry.getKey());
                created.setWeekday(entry.getKey().getDayOfWeek().name());
                created.setWeekNumber(ClubDates.weekNumber(settings.getFirstTrainingMonday(), entry.getKey()));
                created.setSessionType(SessionType.PITCH);
                return sessionRepository.save(created);
            });
            attendanceService.saveBulk(session.getId(), entry.getValue());
            rows += entry.getValue().size();
        }
        return rows;
    }

    private int importSanctions(Sheet sheet) {
        if (sheet == null) {
            return 0;
        }
        Map<String, Integer> columns = header(sheet.getRow(0));
        int created = 0;
        var settings = settingsService.current();
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) {
                continue;
            }
            String name = text(row, columns, "player", "name");
            String amount = text(row, columns, "amount");
            if (name.isBlank() || amount.isBlank()) {
                continue;
            }
            Player player = playerRepository.findByActiveTrueOrderByRegisterNoAsc().stream()
                    .filter(item -> item.getFullName().equalsIgnoreCase(name) || item.getKnownAs().equalsIgnoreCase(name))
                    .findFirst().orElse(null);
            if (player == null) {
                continue;
            }
            Fine fine = new Fine();
            fine.setPlayer(player);
            String type = text(row, columns, "type");
            fine.setFineType(type.isBlank() ? FineType.OTHER : FineType.valueOf(type.trim().toUpperCase(Locale.ENGLISH).replace(' ', '_')));
            fine.setSourceType(FineSource.MANUAL);
            String date = text(row, columns, "date");
            fine.setFineDate(date.isBlank() ? LocalDate.now() : LocalDate.parse(date.substring(0, 10)));
            fine.setWeekNumber(ClubDates.weekNumber(settings.getFirstTrainingMonday(), fine.getFineDate()));
            fine.setJustified(false);
            fine.setAmount(Money.nz(new BigDecimal(amount.replace(",", ""))));
            fine.setAmountPaid(Money.zero());
            fine.setReason(text(row, columns, "reason"));
            fine.setStatus(FineStatus.OUTSTANDING);
            fine.setAutoGenerated(false);
            fineRepository.save(fine);
            created++;
        }
        return created;
    }

    private Map<String, Integer> header(Row row) {
        Map<String, Integer> columns = new HashMap<>();
        if (row == null) {
            return columns;
        }
        for (Cell cell : row) {
            columns.put(formatter.formatCellValue(cell).trim().toLowerCase(Locale.ENGLISH), cell.getColumnIndex());
        }
        return columns;
    }

    private String text(Row row, Map<String, Integer> columns, String... names) {
        for (String name : names) {
            Integer index = columns.get(name);
            if (index != null && row.getCell(index) != null) {
                return formatter.formatCellValue(row.getCell(index)).trim();
            }
        }
        return "";
    }
}
