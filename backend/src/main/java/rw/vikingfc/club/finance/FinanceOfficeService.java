package rw.vikingfc.club.finance;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.vikingfc.club.audit.AuditLog;
import rw.vikingfc.club.audit.AuditLogRepository;
import rw.vikingfc.club.audit.AuditService;
import rw.vikingfc.club.bonus.BonusEarned;
import rw.vikingfc.club.bonus.BonusEarnedRepository;
import rw.vikingfc.club.bonus.BonusStatus;
import rw.vikingfc.club.common.exception.BusinessException;
import rw.vikingfc.club.common.exception.NotFoundException;
import rw.vikingfc.club.common.util.Money;
import rw.vikingfc.club.common.util.SecurityUtils;
import rw.vikingfc.club.contract.PlayerContractRepository;
import rw.vikingfc.club.sanction.Fine;
import rw.vikingfc.club.sanction.FinePayment;
import rw.vikingfc.club.sanction.FinePaymentRepository;
import rw.vikingfc.club.sanction.FineRepository;
import rw.vikingfc.club.sanction.FineStatus;
import rw.vikingfc.club.sanction.PayDeduction;
import rw.vikingfc.club.sanction.PayDeductionRepository;
import rw.vikingfc.club.settings.ClubSettings;
import rw.vikingfc.club.settings.SettingsService;
import rw.vikingfc.club.user.UserRepository;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
@Transactional
public class FinanceOfficeService {

    private static final ZoneId KIGALI = ZoneId.of("Africa/Kigali");
    private static final List<String> CHARTS = List.of(
            "income-expenses", "paid-vs-owed", "player-ranking", "staff-by-role", "admin-by-person",
            "salary-histogram", "bonuses-by-period", "top-bonus-earners", "expense-by-category",
            "expense-by-month", "cost-per-match", "training-spend", "fines-by-month", "cash-trend",
            "budget-vs-actual", "social-by-month");

    private final FinanceService financeService;
    private final FinanceExpenseRepository expenseRepository;
    private final ClubReceiptRepository receiptRepository;
    private final FinanceReportRunRepository reportRunRepository;
    private final PaymentRepository paymentRepository;
    private final PayrollInstalmentRepository instalmentRepository;
    private final FineRepository fineRepository;
    private final FinePaymentRepository finePaymentRepository;
    private final PayDeductionRepository payDeductionRepository;
    private final BonusEarnedRepository bonusEarnedRepository;
    private final PlayerContractRepository playerContractRepository;
    private final AuditLogRepository auditLogRepository;
    private final AuditService auditService;
    private final UserRepository userRepository;
    private final SettingsService settingsService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public Map<String, Object> overview() {
        Map<String, Object> roster = financeService.roster();
        Map<String, Object> players = group(roster, "players");
        Map<String, Object> staff = group(roster, "staff");
        Map<String, Object> administration = group(roster, "administration");
        players.put("sparkline", sparkline(true));
        staff.put("sparkline", sparkline(false));
        administration.put("sparkline", List.of());
        ClubSettings settings = settingsService.current();
        Map<String, Object> kpis = new LinkedHashMap<>();
        kpis.put("cashBalance", cashBalance());
        kpis.put("seasonPayroll", money(players.get("seasonPayroll")).add(money(staff.get("seasonPayroll"))).add(money(administration.get("seasonPayroll"))));
        kpis.put("paid", money(players.get("paid")).add(money(staff.get("paid"))).add(money(administration.get("paid"))));
        kpis.put("netDue", money(players.get("netDue")).add(money(staff.get("netDue"))).add(money(administration.get("netDue"))));
        kpis.put("expenses", expenseTotal());
        kpis.put("bonusesPaid", bonusesPaid());
        kpis.put("finesReceivable", finesReceivable());
        kpis.put("socialBalance", money(players.get("social")).add(money(staff.get("social"))).add(money(administration.get("social"))).subtract(socialSpent()));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("season", roster.get("season"));
        body.put("contractMonths", settings.getPlayerContractMonths());
        body.put("firstPhaseMonths", settings.getPlayerFirstPhaseMonths());
        body.put("kpis", kpis);
        body.put("groups", Map.of("players", players, "staff", staff, "administration", administration));
        body.put("payNotSet", roster.get("payNotSet"));
        return body;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> chart(String chartId, String group, String groupBy) {
        if (!CHARTS.contains(chartId)) {
            throw NotFoundException.of("Chart", chartId);
        }
        Map<String, Object> roster = financeService.roster();
        List<Map<String, Object>> points = switch (chartId) {
            case "income-expenses" -> incomeExpenses();
            case "paid-vs-owed" -> paidVersusOwed(roster);
            case "player-ranking" -> ranking(people(roster, group == null || group.isBlank() ? "players" : group));
            case "staff-by-role" -> byKey(people(roster, "staff"), "role");
            case "admin-by-person" -> ranking(people(roster, "administration"));
            case "salary-histogram" -> histogram(people(roster, group == null || group.isBlank() ? "players" : group));
            case "bonuses-by-period" -> bonusesByPeriod();
            case "top-bonus-earners" -> topBonuses();
            case "expense-by-category" -> expensesBy("category");
            case "expense-by-month" -> expensesByMonth();
            case "cost-per-match" -> expensesBy("linkedLabel");
            case "training-spend" -> trainingSpend();
            case "fines-by-month" -> finesByMonth();
            case "cash-trend" -> cashTrend();
            case "budget-vs-actual" -> List.of();
            case "social-by-month" -> socialByMonth(roster);
            default -> List.of();
        };
        if ("YEAR".equalsIgnoreCase(groupBy)) {
            points = collapseYears(points);
        }
        List<String> series = seriesOf(chartId);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", chartId);
        body.put("series", series);
        body.put("points", points);
        body.put("empty", points.isEmpty() || points.stream().allMatch(this::blankPoint));
        return body;
    }

    public Map<String, Object> saveExpense(FinanceDtos.ExpenseRequest request) {
        BigDecimal amount = Money.nz(request.amount());
        if (amount.signum() <= 0) {
            throw BusinessException.unprocessable("Amount must be greater than zero", "AMOUNT");
        }
        if (request.expenseDate().isAfter(LocalDate.now(clock))) {
            throw BusinessException.unprocessable("An expense date cannot be in the future", "FUTURE_DATE");
        }
        String category = request.category().trim().toLowerCase();
        String custom = blank(request.customLabel());
        boolean other = category.contains("other") || category.contains("autre") || category.contains("ibindi");
        if (other && (custom.length() < 3 || custom.length() > 80)) {
            throw BusinessException.unprocessable("Name the expense when the category is Other", "CUSTOM_LABEL");
        }
        FinanceExpense expense = new FinanceExpense();
        expense.setCategory(request.category().trim());
        expense.setCustomLabel(custom);
        expense.setSubcategory(blank(request.subcategory()));
        expense.setDescription(custom.isBlank() ? request.description().trim() : custom);
        expense.setAmount(amount);
        expense.setExpenseDate(request.expenseDate());
        expense.setVendorName(blank(request.vendorName()));
        expense.setVendorPhone(blank(request.vendorPhone()));
        expense.setMethod(blank(request.method()));
        expense.setNote(blank(request.note()));
        expense.setLinkedLabel(blank(request.linkedLabel()));
        String status = request.status() == null || request.status().isBlank() ? "PAID" : request.status().trim().toUpperCase();
        expense.setStatus(status);
        expense.setReversed(false);
        expenseRepository.save(expense);
        auditService.record("CREATE", "FinanceExpense", expense.getId(), null, amount, null);
        return expenseRow(expense);
    }

    public Map<String, Object> reverseExpense(Long id, String reason) {
        if (reason == null || reason.isBlank()) {
            throw BusinessException.unprocessable("A reversal reason is required", "REASON");
        }
        FinanceExpense expense = expenseRepository.findById(id).orElseThrow(() -> NotFoundException.of("Expense", id));
        if (expense.isReversed()) {
            throw BusinessException.conflict("This expense is already reversed", "REVERSED", null);
        }
        expense.setReversed(true);
        expense.setReverseReason(reason.trim());
        expense.setStatus("REVERSED");
        auditService.record("REVERSE", "FinanceExpense", id, expense.getAmount(), reason, null);
        return expenseRow(expense);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> expenses() {
        return expenseRepository.findAll().stream()
                .sorted(Comparator.comparing(FinanceExpense::getExpenseDate).reversed())
                .map(this::expenseRow)
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> dossier(String type, Long id) {
        Map<String, Object> roster = financeService.roster();
        Map<String, Object> person = findPerson(roster, type, id);
        List<FinanceDtos.InstalmentView> instalments = "PLAYER".equalsIgnoreCase(type)
                ? instalmentRepository.findByPlayerIdOrderByDueDateAsc(id).stream().map(financeService::viewInstalment).toList()
                : instalmentRepository.findByStaffIdOrderByDueDateAsc(id).stream().map(financeService::viewInstalment).toList();
        List<Map<String, Object>> payments = new ArrayList<>();
        for (FinanceDtos.InstalmentView instalment : instalments) {
            for (Payment payment : paymentRepository.findByInstalmentIdOrderByPaidAtAsc(instalment.id())) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", payment.getId());
                row.put("paidAt", payment.getPaidAt());
                row.put("amount", payment.getAmount());
                row.put("method", payment.getMethod());
                row.put("reference", payment.getReferenceNo());
                row.put("receipt", payment.getReceiptNumber());
                row.put("reversed", payment.isReversed());
                String author = payment.getPaidByUser() == null ? payment.getCreatedBy() : payment.getPaidByUser().getFullName();
                row.put("recordedBy", author == null || author.isBlank() || "system".equalsIgnoreCase(author) ? "Imported" : author);
                payments.add(row);
            }
        }
        List<Map<String, Object>> bonuses = "PLAYER".equalsIgnoreCase(type)
                ? bonusEarnedRepository.findByPlayerIdOrderByEarnedOnDesc(id).stream().map(this::bonusRow).toList()
                : List.of();
        String entity = "PLAYER".equalsIgnoreCase(type) ? "PlayerContract" : "StaffContract";
        Long contractId = contractId(type, id);
        List<Map<String, Object>> history = contractId == null ? List.of() : auditLogRepository
                .findTop40ByEntityNameAndEntityIdOrderByCreatedAtDesc(entity, String.valueOf(contractId)).stream()
                .map(this::historyRow).toList();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("person", person);
        body.put("instalments", instalments);
        body.put("payments", payments);
        body.put("bonuses", bonuses);
        body.put("receipts", receiptRepository.findByPersonTypeAndPersonIdOrderByCreatedAtDesc(type.toUpperCase(), id).stream().map(this::receiptRow).toList());
        body.put("history", history);
        if ("PLAYER".equalsIgnoreCase(type)) {
            try {
                body.put("statement", financeService.statement(id));
            } catch (NotFoundException ex) {
                body.put("statement", null);
            }
        }
        return body;
    }

    public Map<String, Object> receipt(FinanceDtos.ReceiptRequest request) {
        if (Money.nz(request.amount()).signum() <= 0) {
            throw BusinessException.unprocessable("Amount must be greater than zero", "AMOUNT");
        }
        String year = String.valueOf(LocalDate.now(clock).getYear());
        long sequence = receiptRepository.countByReceiptNumberStartingWith("VFC-REC-" + year) + 1;
        ClubReceipt receipt = new ClubReceipt();
        receipt.setReceiptNumber("VFC-REC-" + year + "-" + String.format("%06d", sequence));
        receipt.setPersonType(request.personType() == null ? "PLAYER" : request.personType());
        receipt.setPersonId(request.personId() == null ? 0L : request.personId());
        receipt.setPersonName(request.personName() == null ? "" : request.personName());
        receipt.setPaymentId(request.paymentId());
        receipt.setAmount(Money.nz(request.amount()));
        receipt.setFinanceSigner(signerName());
        receipt.setRecipientSigner(request.personName());
        receipt.setSignedFinanceAt(Instant.now(clock));
        receipt.setSignedRecipientAt(Instant.now(clock));
        receipt.setStatus("COMPLETED");
        receiptRepository.save(receipt);
        auditService.record("RECEIPT", "ClubReceipt", receipt.getId(), null, receipt.getReceiptNumber(), null);
        return receiptRow(receipt);
    }

    @Transactional(readOnly = true)
    public byte[] receiptPdf(Long id) {
        ClubReceipt receipt = receiptRepository.findById(id).orElseThrow(() -> NotFoundException.of("Receipt", id));
        return renderReceipt(receipt);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> verify(String number) {
        ClubReceipt receipt = receiptRepository.findByReceiptNumber(number).orElseThrow(() -> NotFoundException.of("Receipt", number));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("number", receipt.getReceiptNumber());
        body.put("status", receipt.getStatus());
        body.put("signedAt", receipt.getSignedFinanceAt());
        body.put("amount", receipt.getAmount());
        return body;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> reports() {
        return reportRunRepository.findTop30ByOrderByCreatedAtDesc().stream().map(run -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", run.getId());
            row.put("what", run.getReportWhat());
            row.put("who", run.getReportWho());
            row.put("period", run.getPeriodLabel());
            row.put("split", run.getSplitMode());
            row.put("level", run.getDetailLevel());
            row.put("format", run.getFormat());
            row.put("bytes", run.getByteSize());
            row.put("at", run.getCreatedAt());
            row.put("by", display(run.getCreatedBy()));
            return row;
        }).toList();
    }

    public ExportFile export(FinanceDtos.ExportRequest request) {
        String what = upper(request.what(), "FULL");
        String who = upper(request.who(), "ALL");
        String format = upper(request.format(), "XLSX");
        Map<String, Object> roster = financeService.roster();
        List<String[]> tables = tables(what, who, request.person(), roster);
        byte[] bytes = switch (format) {
            case "PDF" -> FinancePdf.render(what, who, tables, logo(), signerName());
            case "CSV" -> csv(tables);
            default -> xlsx(what, tables);
        };
        FinanceReportRun run = new FinanceReportRun();
        run.setReportWhat(what);
        run.setReportWho(who);
        run.setPeriodLabel(upper(request.period(), "SEASON"));
        run.setSplitMode(upper(request.split(), "SINGLE"));
        run.setDetailLevel(upper(request.level(), "DETAILS"));
        run.setFormat(format.equals("PDF") || format.equals("CSV") ? format : "XLSX");
        run.setByteSize(bytes.length);
        reportRunRepository.save(run);
        auditService.record("EXPORT", "FinanceReport", run.getId(), null, what + " " + format, null);
        String extension = format.equals("PDF") ? "pdf" : format.equals("CSV") ? "csv" : "xlsx";
        String month = java.time.ZonedDateTime.now(java.time.ZoneId.of("Africa/Kigali")).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));
        String type = switch (extension) {
            case "pdf" -> "application/pdf";
            case "csv" -> "text/csv";
            default -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        };
        return new ExportFile(bytes, "VIKING-FC_" + what + "_" + who + "_" + month + "." + extension, type);
    }

    public record ExportFile(byte[] bytes, String filename, String type) {
    }

    private List<String[]> tables(String what, String who, String personName, Map<String, Object> roster) {
        List<String[]> rows = new ArrayList<>();
        List<Map<String, Object>> people = selected(roster, who).stream()
                .filter(person -> personName == null || personName.isBlank() || personName.equals(String.valueOf(person.get("fullName"))))
                .toList();
        if ("SALARIES".equals(what) || "FULL".equals(what) || "SOCIAL".equals(what)) {
            rows.add(new String[]{"Person", "Group", "Role", "Seasonal pay", "Paid to date", "Net balance due", "Social fund", "Fines", "Net payable"});
            BigDecimal seasonal = Money.zero();
            BigDecimal paid = Money.zero();
            BigDecimal net = Money.zero();
            for (Map<String, Object> person : people) {
                rows.add(new String[]{
                        String.valueOf(person.get("fullName")),
                        String.valueOf(person.get("group")),
                        String.valueOf(person.get("role")),
                        plain(person.get("seasonal")),
                        plain(person.get("paid")),
                        plain(person.get("netDue")),
                        plain(person.get("social")),
                        plain(person.get("fines")),
                        plain(person.get("netPayable"))
                });
                seasonal = seasonal.add(money(person.get("seasonal")));
                paid = paid.add(money(person.get("paid")));
                net = net.add(money(person.get("netDue")));
            }
            rows.add(new String[]{"Total", "", "", plain(seasonal), plain(paid), plain(net), "", "", ""});
        }
        if ("BONUSES".equals(what) || "FULL".equals(what)) {
            rows.add(new String[]{"Person", "Type", "Amount", "Status", "Earned"});
            for (BonusEarned bonus : bonusEarnedRepository.findAll()) {
                rows.add(new String[]{bonus.getPlayer().getFullName(), String.valueOf(bonus.getBonusType()), plain(bonus.getAmount()), String.valueOf(bonus.getStatus()), String.valueOf(bonus.getEarnedOn())});
            }
        }
        if ("EXPENSES".equals(what) || "FULL".equals(what)) {
            rows.add(new String[]{"Date", "Category", "Description", "Vendor", "Amount", "Status"});
            for (FinanceExpense expense : expenseRepository.findAll()) {
                rows.add(new String[]{String.valueOf(expense.getExpenseDate()), expense.getCategory(), expense.getDescription(), String.valueOf(expense.getVendorName()), plain(expense.getAmount()), expense.getStatus()});
            }
        }
        if ("FINES".equals(what) || "FULL".equals(what)) {
            rows.add(new String[]{"Person", "Date", "Amount", "Paid", "Balance", "Status"});
            for (Fine fine : fineRepository.findAll()) {
                String name = fine.getPlayer() == null ? (fine.getStaff() == null ? "" : fine.getStaff().getFullName()) : fine.getPlayer().getFullName();
                rows.add(new String[]{name, String.valueOf(fine.getFineDate()), plain(fine.getAmount()), plain(fine.getAmountPaid()), plain(fine.balance()), String.valueOf(fine.getStatus())});
            }
        }
        if ("RECEIPTS".equals(what) || "FULL".equals(what)) {
            rows.add(new String[]{"Number", "Person", "Amount", "Status", "Signed"});
            for (ClubReceipt receipt : receiptRepository.findAll()) {
                rows.add(new String[]{receipt.getReceiptNumber(), receipt.getPersonName(), plain(receipt.getAmount()), receipt.getStatus(), String.valueOf(receipt.getSignedFinanceAt())});
            }
        }
        if (rows.isEmpty()) {
            rows.add(new String[]{"Info", "No rows for this export"});
        }
        return rows;
    }

    private byte[] pdf(String what, String who, List<String[]> tables) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 48, 36);
            PdfWriter writer = PdfWriter.getInstance(document, out);
            document.open();
            byte[] logo = logo();
            if (logo.length > 0) {
                Image image = Image.getInstance(logo);
                image.scaleToFit(78, 42);
                document.add(image);
            }
            Font title = new Font(Font.HELVETICA, 16, Font.BOLD, new Color(163, 40, 58));
            document.add(new Phrase("Viking FC  ·  " + what + "  ·  " + who, title));
            document.add(new Phrase("\n" + Instant.now(clock).atZone(KIGALI).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) + " Africa/Kigali\n\n"));
            for (String[] line : tables) {
                PdfPTable table = new PdfPTable(line.length);
                table.setWidthPercentage(100);
                boolean header = !line[0].isBlank() && tables.indexOf(line) >= 0 && looksHeader(line);
                for (String cell : line) {
                    PdfPCell pdfCell = new PdfPCell(new Phrase(cell == null ? "" : cell, new Font(Font.HELVETICA, 8)));
                    if (header) {
                        pdfCell.setBackgroundColor(new Color(163, 40, 58));
                        pdfCell.setPhrase(new Phrase(cell, new Font(Font.HELVETICA, 8, Font.BOLD, Color.WHITE)));
                    }
                    table.addCell(pdfCell);
                }
                document.add(table);
                document.add(new Phrase(" "));
            }
            drawQr(writer, document, what + " " + who);
            document.close();
            return out.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Could not build the PDF", ex);
        }
    }

    private byte[] xlsx(String what, List<String[]> tables) {
        try (XSSFWorkbook book = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFSheet sheet = book.createSheet(what.substring(0, Math.min(what.length(), 31)));
            byte[] logo = logo();
            int start = 0;
            if (logo.length > 0) {
                int picture = book.addPicture(logo, XSSFWorkbook.PICTURE_TYPE_PNG);
                CreationHelper helper = book.getCreationHelper();
                ClientAnchor anchor = helper.createClientAnchor();
                anchor.setCol1(0);
                anchor.setRow1(0);
                anchor.setCol2(2);
                anchor.setRow2(2);
                sheet.createDrawingPatriarch().createPicture(anchor, picture);
                sheet.createRow(0).setHeightInPoints(36);
                start = 3;
            }
            CellStyle header = book.createCellStyle();
            header.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.ROSE.getIndex());
            int rowIndex = start;
            for (String[] line : tables) {
                Row row = sheet.createRow(rowIndex++);
                for (int column = 0; column < line.length; column++) {
                    String value = line[column] == null ? "" : line[column];
                    if (value.matches("-?\\d+")) {
                        row.createCell(column).setCellValue(Long.parseLong(value));
                    } else {
                        row.createCell(column).setCellValue(value);
                    }
                }
            }
            if (rowIndex > start + 1) {
                sheet.createFreezePane(1, start + 1);
                sheet.setAutoFilter(new CellRangeAddress(start, rowIndex - 1, 0, Math.max(0, tables.get(0).length - 1)));
            }
            book.write(out);
            return out.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Could not build the workbook", ex);
        }
    }

    private byte[] csv(List<String[]> tables) {
        StringBuilder builder = new StringBuilder("\uFEFF");
        for (String[] line : tables) {
            for (int i = 0; i < line.length; i++) {
                if (i > 0) builder.append(',');
                builder.append('"').append(line[i] == null ? "" : line[i].replace("\"", "\"\"")).append('"');
            }
            builder.append("\n");
        }
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] renderReceipt(ClubReceipt receipt) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A5.rotate(), 24, 24, 24, 28);
            PdfWriter writer = PdfWriter.getInstance(document, out);
            document.open();
            com.lowagie.text.pdf.BaseFont base;
            try {
                base = com.lowagie.text.pdf.BaseFont.createFont("C:/Windows/Fonts/arial.ttf", com.lowagie.text.pdf.BaseFont.IDENTITY_H, com.lowagie.text.pdf.BaseFont.EMBEDDED);
            } catch (Exception ex) {
                base = com.lowagie.text.pdf.BaseFont.createFont(com.lowagie.text.pdf.BaseFont.HELVETICA, com.lowagie.text.pdf.BaseFont.WINANSI, false);
            }
            Font club = new Font(base, 14, Font.BOLD, new Color(163, 40, 58));
            Font small = new Font(base, 8, Font.NORMAL, Color.DARK_GRAY);
            Font white = new Font(base, 11, Font.BOLD, Color.WHITE);
            Font amount = new Font(base, 18, Font.BOLD, Color.BLACK);
            PdfPTable head = new PdfPTable(new float[] { 1.1f, 3f, 2.2f });
            head.setWidthPercentage(100);
            PdfPCell logoCell = new PdfPCell();
            logoCell.setBorder(PdfPCell.BOX);
            logoCell.setFixedHeight(58);
            byte[] logo = logo();
            if (logo.length > 0) {
                Image image = Image.getInstance(logo);
                image.scaleToFit(52, 52);
                logoCell.addElement(image);
            }
            head.addCell(logoCell);
            PdfPCell name = new PdfPCell();
            name.setBorder(PdfPCell.NO_BORDER);
            name.addElement(new com.lowagie.text.Paragraph("VIKING FC", club));
            name.addElement(new com.lowagie.text.Paragraph("Football Club · Kigali, Rwanda", small));
            head.addCell(name);
            PdfPCell meta = new PdfPCell();
            meta.setBorder(PdfPCell.NO_BORDER);
            meta.addElement(new com.lowagie.text.Paragraph(receipt.getReceiptNumber(), new Font(base, 9, Font.BOLD, Color.BLACK)));
            meta.addElement(new com.lowagie.text.Paragraph(receipt.getSignedFinanceAt().atZone(KIGALI).format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")) + " Kigali", small));
            meta.addElement(new com.lowagie.text.Paragraph(String.valueOf(receipt.getStatus()), small));
            head.addCell(meta);
            document.add(head);
            PdfPTable title = new PdfPTable(1);
            title.setWidthPercentage(100);
            title.setSpacingBefore(8);
            PdfPCell bar = new PdfPCell(new Phrase("PAYMENT RECEIPT / REÇU DE PAIEMENT", white));
            bar.setBackgroundColor(new Color(163, 40, 58));
            bar.setPadding(8);
            bar.setHorizontalAlignment(Element.ALIGN_CENTER);
            title.addCell(bar);
            document.add(title);
            document.add(new com.lowagie.text.Paragraph(receipt.getPersonName(), new Font(base, 12, Font.BOLD, Color.BLACK)));
            document.add(new com.lowagie.text.Paragraph("RWF " + new java.text.DecimalFormat("#,##0").format(receipt.getAmount()), amount));
            document.add(new com.lowagie.text.Paragraph(words(receipt.getAmount().longValue()), small));
            PdfPTable signs = new PdfPTable(2);
            signs.setWidthPercentage(100);
            signs.setSpacingBefore(12);
            signs.addCell(signBox("FINANCE MANAGER", receipt.getFinanceSigner(), base));
            signs.addCell(signBox("RECIPIENT", receipt.getRecipientSigner(), base));
            document.add(signs);
            document.add(new com.lowagie.text.Paragraph("A signature drawn on screen is an internal proof. Large amounts should also keep a signed paper copy.", small));
            drawQr(writer, document, receipt.getReceiptNumber());
            document.close();
            return out.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Could not build the receipt", ex);
        }
    }

    private PdfPCell signBox(String role, String name, com.lowagie.text.pdf.BaseFont base) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(8);
        cell.setFixedHeight(72);
        cell.addElement(new com.lowagie.text.Paragraph(role, new Font(base, 8, Font.BOLD, new Color(163, 40, 58))));
        cell.addElement(new com.lowagie.text.Paragraph(safe(name), new Font(base, 10, Font.NORMAL, Color.BLACK)));
        cell.addElement(new com.lowagie.text.Paragraph(" ", new Font(base, 16)));
        return cell;
    }

    private String words(long amount) {
        return english(amount) + " Rwandan francs / " + french(amount) + " francs rwandais";
    }

    private String english(long value) {
        if (value == 0) return "zero";
        if (value < 0) return "minus " + english(-value);
        String[] small = { "", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen" };
        String[] tens = { "", "", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety" };
        if (value < 20) return small[(int) value];
        if (value < 100) return tens[(int) (value / 10)] + (value % 10 == 0 ? "" : "-" + small[(int) (value % 10)]);
        if (value < 1000) return small[(int) (value / 100)] + " hundred" + (value % 100 == 0 ? "" : " " + english(value % 100));
        if (value < 1_000_000) return english(value / 1000) + " thousand" + (value % 1000 == 0 ? "" : " " + english(value % 1000));
        return english(value / 1_000_000) + " million" + (value % 1_000_000 == 0 ? "" : " " + english(value % 1_000_000));
    }

    private String french(long value) {
        if (value == 0) return "zéro";
        if (value < 0) return "moins " + french(-value);
        String[] small = { "", "un", "deux", "trois", "quatre", "cinq", "six", "sept", "huit", "neuf", "dix", "onze", "douze", "treize", "quatorze", "quinze", "seize", "dix-sept", "dix-huit", "dix-neuf" };
        String[] tens = { "", "", "vingt", "trente", "quarante", "cinquante", "soixante", "soixante", "quatre-vingt", "quatre-vingt" };
        if (value < 20) return small[(int) value];
        if (value < 70) return tens[(int) (value / 10)] + (value % 10 == 0 ? "" : "-" + small[(int) (value % 10)]);
        if (value < 80) return "soixante-" + small[(int) (value - 60)];
        if (value < 100) return "quatre-vingt" + (value == 80 ? "s" : "-" + small[(int) (value - 80)]);
        if (value < 1000) return (value / 100 == 1 ? "cent" : small[(int) (value / 100)] + " cent") + (value % 100 == 0 ? "" : " " + french(value % 100));
        if (value < 1_000_000) return french(value / 1000) + " mille" + (value % 1000 == 0 ? "" : " " + french(value % 1000));
        return french(value / 1_000_000) + " million" + (value % 1_000_000 == 0 ? "" : " " + french(value % 1_000_000));
    }

    private void ParagraphTitle(Document document, String text, Font font) throws Exception {
        com.lowagie.text.Paragraph paragraph = new com.lowagie.text.Paragraph(text, font);
        paragraph.setAlignment(Element.ALIGN_CENTER);
        document.add(paragraph);
    }

    private void drawQr(PdfWriter writer, Document document, String payload) {
        try {
            BitMatrix matrix = new QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, 21, 21);
            PdfContentByte canvas = writer.getDirectContent();
            float cell = 3.0f;
            float x0 = document.right() - (matrix.getWidth() * cell) - 8;
            float y0 = document.bottom() + 8;
            canvas.setColorFill(Color.BLACK);
            for (int x = 0; x < matrix.getWidth(); x++) {
                for (int y = 0; y < matrix.getHeight(); y++) {
                    if (matrix.get(x, y)) {
                        canvas.rectangle(x0 + x * cell, y0 + (matrix.getHeight() - 1 - y) * cell, cell, cell);
                    }
                }
            }
            canvas.fill();
        } catch (Exception ignored) {
            // The receipt stays valid when the code cannot be drawn.
        }
    }

    private List<Map<String, Object>> incomeExpenses() {
        TreeMap<String, BigDecimal> income = new TreeMap<>();
        TreeMap<String, BigDecimal> expenses = new TreeMap<>();
        for (FinePayment payment : finePaymentRepository.findAll()) {
            income.merge(month(payment.getPaidAt()), Money.nz(payment.getAmount()), BigDecimal::add);
        }
        for (FinanceExpense expense : expenseRepository.findAll()) {
            if (!expense.isReversed()) {
                expenses.merge(month(expense.getExpenseDate()), expense.getAmount(), BigDecimal::add);
            }
        }
        TreeMap<String, Boolean> keys = new TreeMap<>();
        income.keySet().forEach(key -> keys.put(key, true));
        expenses.keySet().forEach(key -> keys.put(key, true));
        List<Map<String, Object>> points = new ArrayList<>();
        keys.keySet().forEach(key -> points.add(point(key, "income", income.getOrDefault(key, Money.zero()), "expenses", expenses.getOrDefault(key, Money.zero()))));
        return points;
    }

    private List<Map<String, Object>> paidVersusOwed(Map<String, Object> roster) {
        List<Map<String, Object>> points = new ArrayList<>();
        for (String key : List.of("players", "staff", "administration")) {
            Map<String, Object> block = group(roster, key);
            points.add(point(key, "paid", money(block.get("paid")), "owed", money(block.get("netDue"))));
        }
        return points;
    }

    private List<Map<String, Object>> ranking(List<Map<String, Object>> people) {
        return people.stream()
                .sorted(Comparator.comparing((Map<String, Object> row) -> money(row.get("seasonal"))).reversed())
                .map(row -> point(String.valueOf(row.get("fullName")), "pay", money(row.get("seasonal"))))
                .toList();
    }

    private List<Map<String, Object>> byKey(List<Map<String, Object>> people, String key) {
        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        for (Map<String, Object> person : people) {
            String label = String.valueOf(person.get(key) == null ? "" : person.get(key));
            if (label.isBlank()) continue;
            totals.merge(label, money(person.get("seasonal")), BigDecimal::add);
        }
        List<Map<String, Object>> points = new ArrayList<>();
        totals.forEach((label, amount) -> points.add(point(label, "pay", amount)));
        return points;
    }

    private List<Map<String, Object>> histogram(List<Map<String, Object>> people) {
        List<BigDecimal> pays = people.stream().map(row -> money(row.get("seasonal"))).filter(amount -> amount.signum() > 0).sorted().toList();
        if (pays.isEmpty()) return List.of();
        BigDecimal min = pays.get(0);
        BigDecimal max = pays.get(pays.size() - 1);
        int buckets = Math.min(6, pays.size());
        BigDecimal width = max.subtract(min).divide(BigDecimal.valueOf(buckets), 0, RoundingMode.UP);
        if (width.signum() == 0) width = BigDecimal.ONE;
        int[] counts = new int[buckets];
        for (BigDecimal pay : pays) {
            int index = pay.subtract(min).divide(width, 0, RoundingMode.DOWN).intValue();
            counts[Math.min(buckets - 1, Math.max(0, index))]++;
        }
        List<Map<String, Object>> points = new ArrayList<>();
        for (int i = 0; i < buckets; i++) {
            BigDecimal from = min.add(width.multiply(BigDecimal.valueOf(i)));
            points.add(point(from.toPlainString(), "players", BigDecimal.valueOf(counts[i])));
        }
        return points;
    }

    private List<Map<String, Object>> bonusesByPeriod() {
        TreeMap<String, BigDecimal> accrued = new TreeMap<>();
        TreeMap<String, BigDecimal> paid = new TreeMap<>();
        for (BonusEarned bonus : bonusEarnedRepository.findAll()) {
            if (bonus.getStatus() == BonusStatus.CANCELLED) continue;
            String key = bonus.getEarnedOn() == null ? month(bonus.getCreatedAt()) : bonus.getEarnedOn().toString().substring(0, 7);
            accrued.merge(key, Money.nz(bonus.getAmount()), BigDecimal::add);
            if (bonus.getStatus() == BonusStatus.PAID) {
                paid.merge(key, Money.nz(bonus.getAmount()), BigDecimal::add);
            }
        }
        List<Map<String, Object>> points = new ArrayList<>();
        accrued.forEach((key, amount) -> points.add(point(key, "accrued", amount, "paid", paid.getOrDefault(key, Money.zero()))));
        return points;
    }

    private List<Map<String, Object>> topBonuses() {
        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        for (BonusEarned bonus : bonusEarnedRepository.findAll()) {
            if (bonus.getStatus() == BonusStatus.CANCELLED || bonus.getPlayer() == null) continue;
            totals.merge(bonus.getPlayer().getFullName(), Money.nz(bonus.getAmount()), BigDecimal::add);
        }
        return totals.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .limit(8)
                .map(entry -> point(entry.getKey(), "bonus", entry.getValue()))
                .toList();
    }

    private List<Map<String, Object>> expensesBy(String field) {
        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        for (FinanceExpense expense : expenseRepository.findAll()) {
            if (expense.isReversed()) continue;
            String label = "category".equals(field) ? expense.getCategory() : expense.getLinkedLabel();
            if (label == null || label.isBlank()) continue;
            totals.merge(label, expense.getAmount(), BigDecimal::add);
        }
        List<Map<String, Object>> points = new ArrayList<>();
        totals.forEach((label, amount) -> points.add(point(label, "amount", amount)));
        return points;
    }

    private List<Map<String, Object>> expensesByMonth() {
        Map<String, Map<String, BigDecimal>> months = new TreeMap<>();
        for (FinanceExpense expense : expenseRepository.findAll()) {
            if (expense.isReversed()) continue;
            months.computeIfAbsent(month(expense.getExpenseDate()), key -> new LinkedHashMap<>())
                    .merge(expense.getCategory(), expense.getAmount(), BigDecimal::add);
        }
        List<Map<String, Object>> points = new ArrayList<>();
        months.forEach((label, categories) -> {
            Map<String, Object> row = point(label);
            categories.forEach(row::put);
            points.add(row);
        });
        return points;
    }

    private List<Map<String, Object>> trainingSpend() {
        TreeMap<String, BigDecimal> months = new TreeMap<>();
        for (FinanceExpense expense : expenseRepository.findAll()) {
            if (expense.isReversed()) continue;
            String category = expense.getCategory() == null ? "" : expense.getCategory().toLowerCase();
            if (!category.contains("train") && !category.contains("ground")) continue;
            months.merge(month(expense.getExpenseDate()), expense.getAmount(), BigDecimal::add);
        }
        List<Map<String, Object>> points = new ArrayList<>();
        months.forEach((label, amount) -> points.add(point(label, "amount", amount)));
        return points;
    }

    private List<Map<String, Object>> finesByMonth() {
        TreeMap<String, BigDecimal> issued = new TreeMap<>();
        TreeMap<String, BigDecimal> collected = new TreeMap<>();
        TreeMap<String, BigDecimal> deducted = new TreeMap<>();
        for (Fine fine : fineRepository.findAll()) {
            if (fine.getStatus() == FineStatus.WAIVED) continue;
            issued.merge(month(fine.getFineDate()), Money.nz(fine.getAmount()), BigDecimal::add);
        }
        for (FinePayment payment : finePaymentRepository.findAll()) {
            collected.merge(month(payment.getPaidAt()), Money.nz(payment.getAmount()), BigDecimal::add);
        }
        for (PayDeduction deduction : payDeductionRepository.findAll()) {
            deducted.merge(month(deduction.getCreatedAt()), Money.nz(deduction.getAmount()), BigDecimal::add);
        }
        TreeMap<String, Boolean> keys = new TreeMap<>();
        issued.keySet().forEach(key -> keys.put(key, true));
        collected.keySet().forEach(key -> keys.put(key, true));
        deducted.keySet().forEach(key -> keys.put(key, true));
        List<Map<String, Object>> points = new ArrayList<>();
        keys.forEach((key, ignored) -> points.add(point(key, "issued", issued.getOrDefault(key, Money.zero()), "collected", collected.getOrDefault(key, Money.zero()), "deducted", deducted.getOrDefault(key, Money.zero()))));
        return points;
    }

    private List<Map<String, Object>> cashTrend() {
        TreeMap<String, BigDecimal> net = new TreeMap<>();
        for (FinePayment payment : finePaymentRepository.findAll()) {
            net.merge(month(payment.getPaidAt()), Money.nz(payment.getAmount()), BigDecimal::add);
        }
        for (Payment payment : paymentRepository.findAll()) {
            if (!payment.isReversed()) {
                net.merge(month(payment.getPaidAt()), Money.nz(payment.getAmount()).negate(), BigDecimal::add);
            }
        }
        for (FinanceExpense expense : expenseRepository.findAll()) {
            if (!expense.isReversed() && "PAID".equals(expense.getStatus())) {
                net.merge(month(expense.getExpenseDate()), expense.getAmount().negate(), BigDecimal::add);
            }
        }
        BigDecimal running = Money.zero();
        List<Map<String, Object>> points = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : net.entrySet()) {
            running = running.add(entry.getValue());
            points.add(point(entry.getKey(), "balance", running));
        }
        return points;
    }

    private List<Map<String, Object>> socialByMonth(Map<String, Object> roster) {
        ClubSettings settings = settingsService.current();
        int months = Math.max(1, settings.getPlayerContractMonths());
        BigDecimal social = money(group(roster, "players").get("social"))
                .add(money(group(roster, "staff").get("social")))
                .add(money(group(roster, "administration").get("social")));
        if (social.signum() == 0) return List.of();
        BigDecimal each = social.divide(BigDecimal.valueOf(months), 0, RoundingMode.DOWN);
        BigDecimal spent = socialSpent();
        List<Map<String, Object>> points = new ArrayList<>();
        BigDecimal allocated = Money.zero();
        for (int month = 1; month <= months; month++) {
            BigDecimal due = month == months ? social.subtract(allocated) : each;
            allocated = allocated.add(due);
            points.add(point("M" + month, "due", due, "collected", month == 1 ? spent : Money.zero()));
        }
        return points;
    }

    private List<Map<String, Object>> sparkline(boolean players) {
        TreeMap<String, BigDecimal> months = new TreeMap<>();
        for (Payment payment : paymentRepository.findAll()) {
            if (payment.isReversed() || payment.getInstalment() == null) continue;
            boolean playerPayment = payment.getInstalment().getPlayer() != null;
            if (playerPayment != players) continue;
            months.merge(month(payment.getPaidAt()), Money.nz(payment.getAmount()), BigDecimal::add);
        }
        List<Map<String, Object>> points = new ArrayList<>();
        months.forEach((label, amount) -> points.add(point(label, "amount", amount)));
        return points;
    }

    private BigDecimal cashBalance() {
        BigDecimal balance = Money.zero();
        for (FinePayment payment : finePaymentRepository.findAll()) balance = balance.add(Money.nz(payment.getAmount()));
        for (Payment payment : paymentRepository.findAll()) {
            if (!payment.isReversed()) balance = balance.subtract(Money.nz(payment.getAmount()));
        }
        for (FinanceExpense expense : expenseRepository.findAll()) {
            if (!expense.isReversed() && "PAID".equals(expense.getStatus())) balance = balance.subtract(expense.getAmount());
        }
        return balance;
    }

    private BigDecimal expenseTotal() {
        return expenseRepository.findAll().stream().filter(expense -> !expense.isReversed()).map(FinanceExpense::getAmount).reduce(Money.zero(), BigDecimal::add);
    }

    private BigDecimal socialSpent() {
        return expenseRepository.findAll().stream()
                .filter(expense -> !expense.isReversed())
                .filter(expense -> expense.getCategory() != null && expense.getCategory().toLowerCase().contains("social"))
                .map(FinanceExpense::getAmount)
                .reduce(Money.zero(), BigDecimal::add);
    }

    private BigDecimal bonusesPaid() {
        return bonusEarnedRepository.findByStatus(BonusStatus.PAID).stream().map(bonus -> Money.nz(bonus.getAmount())).reduce(Money.zero(), BigDecimal::add);
    }

    private BigDecimal finesReceivable() {
        return fineRepository.findAll().stream()
                .filter(fine -> fine.getStatus() != FineStatus.WAIVED)
                .map(fine -> Money.maxZero(fine.balance()))
                .reduce(Money.zero(), BigDecimal::add);
    }

    private Map<String, Object> expenseRow(FinanceExpense expense) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", expense.getId());
        row.put("category", expense.getCategory());
        row.put("subcategory", expense.getSubcategory());
        row.put("description", expense.getDescription());
        row.put("customLabel", expense.getCustomLabel());
        row.put("amount", expense.getAmount());
        row.put("date", expense.getExpenseDate());
        row.put("vendor", expense.getVendorName());
        row.put("phone", expense.getVendorPhone());
        row.put("method", expense.getMethod());
        row.put("status", expense.getStatus());
        row.put("note", expense.getNote());
        row.put("linked", expense.getLinkedLabel());
        row.put("reversed", expense.isReversed());
        row.put("updatedAt", expense.getUpdatedAt());
        row.put("updatedBy", display(expense.getUpdatedBy()));
        return row;
    }

    private Map<String, Object> bonusRow(BonusEarned bonus) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", bonus.getId());
        row.put("type", bonus.getBonusType());
        row.put("amount", bonus.getAmount());
        row.put("status", bonus.getStatus());
        row.put("earnedOn", bonus.getEarnedOn());
        return row;
    }

    private Map<String, Object> receiptRow(ClubReceipt receipt) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", receipt.getId());
        row.put("number", receipt.getReceiptNumber());
        row.put("person", receipt.getPersonName());
        row.put("amount", receipt.getAmount());
        row.put("status", receipt.getStatus());
        row.put("financeSigner", receipt.getFinanceSigner());
        row.put("recipientSigner", receipt.getRecipientSigner());
        row.put("signedAt", receipt.getSignedFinanceAt());
        return row;
    }

    private Map<String, Object> historyRow(AuditLog log) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("at", log.getCreatedAt());
        row.put("actor", log.getActorName() == null || log.getActorName().isBlank() ? display(log.getActorEmail()) : log.getActorName());
        row.put("action", log.getAction());
        String before = log.getBeforeJson() == null ? "" : log.getBeforeJson();
        String after = log.getAfterJson() == null ? "" : log.getAfterJson();
        String reason = "";
        if ("UPDATE_PAY".equals(log.getAction()) && after.matches("-?\\d+(\\.\\d+)?\\s+.+")) {
            int split = after.indexOf(' ');
            reason = after.substring(split + 1);
            after = after.substring(0, split);
        }
        row.put("field", "UPDATE_PAY".equals(log.getAction()) ? "Seasonal pay" : log.getEntityName());
        row.put("before", before);
        row.put("after", after);
        row.put("reason", reason);
        return row;
    }

    private Map<String, Object> findPerson(Map<String, Object> roster, String type, Long id) {
        for (String key : List.of("players", "staff", "administration")) {
            for (Map<String, Object> person : people(roster, key)) {
                if (String.valueOf(person.get("personType")).equalsIgnoreCase(type) && String.valueOf(person.get("id")).equals(String.valueOf(id))) {
                    return person;
                }
            }
        }
        Object unset = roster.get("payNotSet");
        if (unset instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> person && String.valueOf(person.get("id")).equals(String.valueOf(id))) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> row = (Map<String, Object>) person;
                    return row;
                }
            }
        }
        throw NotFoundException.of("Person", id);
    }

    private Long contractId(String type, Long id) {
        ClubSettings settings = settingsService.current();
        if ("PLAYER".equalsIgnoreCase(type)) {
            return playerContractRepository.findByPlayerIdAndSeasonIdAndActiveTrue(id, settings.getSeason().getId()).map(contract -> contract.getId()).orElse(null);
        }
        return null;
    }

    private String signerName() {
        return userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(SecurityUtils.email()).map(user -> user.getFullName()).orElse(SecurityUtils.email());
    }

    private String display(String email) {
        if (email == null || email.isBlank()) return "";
        return userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(email).map(user -> user.getFullName()).orElse(email);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> group(Map<String, Object> roster, String key) {
        Object value = roster.get(key);
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : new LinkedHashMap<>();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> people(Map<String, Object> roster, String key) {
        if ("all".equalsIgnoreCase(key)) {
            List<Map<String, Object>> rows = new ArrayList<>();
            rows.addAll(people(roster, "players"));
            rows.addAll(people(roster, "staff"));
            rows.addAll(people(roster, "administration"));
            return rows;
        }
        Object value = group(roster, key).get("people");
        return value instanceof List<?> list ? (List<Map<String, Object>>) list : List.of();
    }

    private List<Map<String, Object>> selected(Map<String, Object> roster, String who) {
        return switch (who) {
            case "PLAYERS" -> people(roster, "players");
            case "STAFF" -> people(roster, "staff");
            case "ADMINISTRATION" -> people(roster, "administration");
            default -> people(roster, "all");
        };
    }

    private List<Map<String, Object>> collapseYears(List<Map<String, Object>> points) {
        Map<String, Map<String, Object>> years = new LinkedHashMap<>();
        List<Map<String, Object>> kept = new ArrayList<>();
        for (Map<String, Object> point : points) {
            String label = String.valueOf(point.get("label"));
            if (!label.matches("\\d{4}-\\d{2}")) {
                kept.add(point);
                continue;
            }
            String year = label.substring(0, 4);
            Map<String, Object> bucket = years.computeIfAbsent(year, key -> point(key));
            point.forEach((key, value) -> {
                if (!"label".equals(key) && value instanceof Number) {
                    bucket.put(key, money(bucket.get(key)).add(money(value)));
                }
            });
        }
        if (years.isEmpty()) return points;
        kept.addAll(years.values());
        return kept;
    }

    private List<String> seriesOf(String chartId) {
        return switch (chartId) {
            case "income-expenses" -> List.of("income", "expenses");
            case "paid-vs-owed" -> List.of("paid", "owed");
            case "bonuses-by-period" -> List.of("accrued", "paid");
            case "fines-by-month" -> List.of("issued", "collected", "deducted");
            case "social-by-month" -> List.of("due", "collected");
            case "salary-histogram" -> List.of("players");
            case "cash-trend", "training-spend", "expense-by-category", "cost-per-match" -> List.of("amount", "balance");
            default -> List.of("pay", "bonus");
        };
    }

    private boolean blankPoint(Map<String, Object> point) {
        return point.entrySet().stream().allMatch(entry -> "label".equals(entry.getKey()) || money(entry.getValue()).signum() == 0);
    }

    private boolean looksHeader(String[] line) {
        for (String cell : line) {
            if (cell != null && cell.matches("-?\\d+")) return false;
        }
        return true;
    }

    private Map<String, Object> point(String label, Object... pairs) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("label", label);
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            row.put(String.valueOf(pairs[i]), pairs[i + 1]);
        }
        return row;
    }

    private static BigDecimal money(Object value) {
        if (value instanceof BigDecimal decimal) return decimal;
        if (value instanceof Number number) return BigDecimal.valueOf(number.longValue());
        return Money.zero();
    }

    private static String plain(Object value) {
        return money(value).toPlainString();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String blank(String value) {
        return value == null ? "" : value.trim();
    }

    private static String upper(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim().toUpperCase();
    }

    private static String month(LocalDate date) {
        return date == null ? "" : date.format(DateTimeFormatter.ofPattern("yyyy-MM"));
    }

    private static String month(Instant instant) {
        return instant == null ? "" : instant.atZone(KIGALI).format(DateTimeFormatter.ofPattern("yyyy-MM"));
    }

    private static byte[] logo() {
        try {
            return new ClassPathResource("brand/logo.png").getContentAsByteArray();
        } catch (Exception ex) {
            return new byte[0];
        }
    }
}
