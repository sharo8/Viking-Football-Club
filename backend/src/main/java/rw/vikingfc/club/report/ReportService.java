package rw.vikingfc.club.report;

import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.vikingfc.club.attendance.AttendanceRecord;
import rw.vikingfc.club.attendance.AttendanceRecordRepository;
import rw.vikingfc.club.contract.PlayerContract;
import rw.vikingfc.club.contract.PlayerContractRepository;
import rw.vikingfc.club.finance.FinanceDtos;
import rw.vikingfc.club.finance.FinanceService;
import rw.vikingfc.club.finance.PayrollCalculator;
import rw.vikingfc.club.player.Player;
import rw.vikingfc.club.player.PlayerRepository;
import rw.vikingfc.club.sanction.Fine;
import rw.vikingfc.club.sanction.FineRepository;
import rw.vikingfc.club.settings.ClubSettings;
import rw.vikingfc.club.settings.SettingsService;
import rw.vikingfc.club.staff.StaffContract;
import rw.vikingfc.club.staff.StaffContractRepository;
import rw.vikingfc.club.staff.StaffMember;
import rw.vikingfc.club.staff.StaffMemberRepository;

import java.io.ByteArrayOutputStream;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final FinanceService financeService;
    private final SettingsService settingsService;
    private final PlayerRepository playerRepository;
    private final PlayerContractRepository contractRepository;
    private final StaffMemberRepository staffRepository;
    private final StaffContractRepository staffContractRepository;
    private final AttendanceRecordRepository attendanceRepository;
    private final FineRepository fineRepository;

    public byte[] workbook() {
        FinanceDtos.Summary summary = financeService.summary();
        ClubSettings settings = settingsService.current();
        try (XSSFWorkbook book = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFSheet dashboard = book.createSheet("Dashboard");
            String[][] kpis = {
                    {"Players", String.valueOf(summary.players())},
                    {"Season payroll", summary.seasonPayroll().toPlainString()},
                    {"Monthly wage bill", summary.monthlyWageBill().toPlainString()},
                    {"Average seasonal pay", summary.averageSeasonalPay().toPlainString()},
                    {"Median seasonal pay", summary.medianSeasonalPay().toPlainString()},
                    {"Advances", summary.advances().toPlainString()},
                    {"Players with an advance", String.valueOf(summary.playersWithAdvance())},
                    {"Envelopes", summary.envelopes().toPlainString()},
                    {"Social fund", summary.socialFund().toPlainString()},
                    {"Social fund exempt", String.valueOf(summary.socialFundExempt())},
                    {"Net balance due", summary.netBalanceDue().toPlainString()},
                    {"Paid to date", summary.paidToDate().toPlainString()}
            };
            for (int i = 0; i < kpis.length; i++) {
                Row row = dashboard.createRow(i);
                row.createCell(0).setCellValue(kpis[i][0]);
                row.createCell(1).setCellValue(kpis[i][1]);
            }
            XSSFSheet register = book.createSheet("Player Register");
            header(register, "No", "Jersey", "Name", "Known As", "Position", "Seasonal Pay", "SF Exempt", "Advance",
                    "Envelope", "Net balance due", "Paid to date", "Status");
            int line = 1;
            for (Player player : playerRepository.findByActiveTrueOrderByRegisterNoAsc()) {
                PlayerContract contract = contractRepository
                        .findByPlayerIdAndSeasonIdAndActiveTrue(player.getId(), settings.getSeason().getId()).orElse(null);
                Row row = register.createRow(line++);
                row.createCell(0).setCellValue(player.getRegisterNo());
                row.createCell(1).setCellValue(player.getJerseyNumber() == null ? "" : String.valueOf(player.getJerseyNumber()));
                row.createCell(2).setCellValue(player.getFullName());
                row.createCell(3).setCellValue(player.getKnownAs());
                row.createCell(4).setCellValue(player.getPosition().getLabel());
                if (contract != null) {
                    PayrollCalculator.Result result = PayrollCalculator.calculate(contract.getSeasonalPay(), contract.isSfExempt(),
                            contract.getAdvanceAtSigning(), settings.getPlayerContractMonths(), settings.getPlayerFirstPhaseMonths(),
                            contract.socialFundOr(settings.getPlayerSocialFund()));
                    row.createCell(5).setCellValue(result.paidToDate() == null ? 0 : contract.getSeasonalPay().doubleValue());
                    row.createCell(6).setCellValue(contract.isSfExempt() ? "Yes" : "No");
                    row.createCell(7).setCellValue(contract.getAdvanceAtSigning().doubleValue());
                    row.createCell(8).setCellValue(result.envelopeAmount().doubleValue());
                    row.createCell(9).setCellValue(result.netBalanceDue().doubleValue());
                    row.createCell(10).setCellValue(result.paidToDate().doubleValue());
                    row.createCell(11).setCellValue(result.paymentStatus());
                }
            }
            XSSFSheet staff = book.createSheet("Staff Register");
            header(staff, "Name", "Role", "Department", "Seasonal Pay");
            int staffLine = 1;
            for (StaffMember member : staffRepository.findByActiveTrueOrderByFullNameAsc()) {
                StaffContract contract = staffContractRepository
                        .findByStaffIdAndSeasonIdAndActiveTrue(member.getId(), settings.getSeason().getId()).orElse(null);
                Row row = staff.createRow(staffLine++);
                row.createCell(0).setCellValue(member.getFullName());
                row.createCell(1).setCellValue(member.getRoleName());
                row.createCell(2).setCellValue(member.getDepartment());
                row.createCell(3).setCellValue(contract == null ? 0 : contract.getSeasonalPay().doubleValue());
            }
            XSSFSheet attendance = book.createSheet("Attendance");
            header(attendance, "Date", "Week", "Player", "Mark");
            int attendanceLine = 1;
            for (AttendanceRecord record : attendanceRepository.findBySession_Season_Id(settings.getSeason().getId())) {
                Row row = attendance.createRow(attendanceLine++);
                row.createCell(0).setCellValue(record.getSession().getSessionDate().toString());
                row.createCell(1).setCellValue(record.getSession().getWeekNumber());
                row.createCell(2).setCellValue(record.getPlayer().getKnownAs());
                row.createCell(3).setCellValue(record.getStatus().name());
            }
            XSSFSheet sanctions = book.createSheet("Sanctions");
            header(sanctions, "Date", "Player", "Type", "Amount", "Status");
            int fineLine = 1;
            for (Fine fine : fineRepository.findAll()) {
                Row row = sanctions.createRow(fineLine++);
                row.createCell(0).setCellValue(fine.getFineDate().toString());
                row.createCell(1).setCellValue(fine.getPlayer() == null ? "" : fine.getPlayer().getKnownAs());
                row.createCell(2).setCellValue(fine.getFineType().name());
                row.createCell(3).setCellValue(fine.getAmount().doubleValue());
                row.createCell(4).setCellValue(fine.getStatus().name());
            }
            XSSFSheet summarySheet = book.createSheet("Summary Tables");
            header(summarySheet, "Line", "Players", "Seasonal pay");
            int summaryLine = 1;
            for (FinanceDtos.LineRow rowData : summary.payrollByLine()) {
                Row row = summarySheet.createRow(summaryLine++);
                row.createCell(0).setCellValue(rowData.line());
                row.createCell(1).setCellValue(rowData.players());
                row.createCell(2).setCellValue(rowData.seasonalPay().doubleValue());
            }
            book.write(out);
            return out.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Could not build the workbook", ex);
        }
    }

    public byte[] payslip(Long playerId) {
        var statement = financeService.statement(playerId);
        StringBuilder text = new StringBuilder();
        text.append("Viking FC payslip — ").append(statement.fullName()).append(" (").append(statement.knownAs()).append(")\n");
        text.append("Seasonal pay status: ").append(statement.payroll().paymentStatus()).append("\n");
        text.append("Monthly pay: ").append(statement.payroll().monthlyPay()).append(" RWF\n");
        text.append("Paid to date: ").append(statement.payroll().paidToDate()).append(" RWF\n");
        text.append("Net balance due: ").append(statement.payroll().netBalanceDue()).append(" RWF\n");
        statement.instalments().forEach(instalment -> text.append(instalment.kind()).append(" due ")
                .append(instalment.dueDate()).append(" — ").append(instalment.amountDue())
                .append(" paid ").append(instalment.amountPaid()).append(" (").append(instalment.status()).append(")\n"));
        return pdf("Player statement", text.toString());
    }

    public byte[] textReport(String title, String body) {
        return pdf(title, body);
    }

    private byte[] pdf(String title, String body) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();
            document.add(new Paragraph(title));
            document.add(new Paragraph(" "));
            for (String line : body.split("\n")) {
                document.add(new Paragraph(line));
            }
            document.close();
            return out.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Could not build the PDF", ex);
        }
    }

    private void header(XSSFSheet sheet, String... titles) {
        Row row = sheet.createRow(0);
        for (int i = 0; i < titles.length; i++) {
            row.createCell(i).setCellValue(titles[i]);
        }
    }
}
