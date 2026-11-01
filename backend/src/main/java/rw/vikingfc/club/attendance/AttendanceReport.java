package rw.vikingfc.club.attendance;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import rw.vikingfc.club.sanction.Fine;
import rw.vikingfc.club.sanction.FineStatus;
import rw.vikingfc.club.sanction.FineType;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class AttendanceReport {

    private static final ZoneId KIGALI = ZoneId.of("Africa/Kigali");
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
    private static final byte[] RED = new byte[]{(byte) 0xA3, (byte) 0x28, (byte) 0x3A};

    public byte[] general(String format, String lang, String title, String who, String period,
                          List<String> headers, List<List<Object>> people, List<Fine> fines) {
        return general(format, lang, title, who, period, headers, people, fines, "both");
    }

    public byte[] general(String format, String lang, String title, String who, String period,
                          List<String> headers, List<List<Object>> people, List<Fine> fines, String mode) {
        List<FineLine> lines = lines(lang, fines);
        return pack(format, lang, title, who, period, headers, people, lines, mode);
    }

    public byte[] finesPack(String format, String lang, String who, String period, List<FineLine> lines) {
        return pack(format, lang, word(lang, "fines"), who, period, List.of(), List.of(), lines, "fines");
    }

    public byte[] person(String format, String lang, String who, Map<String, Object> summary, List<Map<String, Object>> records) {
        return "pdf".equals(format) ? personPdf(lang, who, summary, records) : personExcel(lang, who, summary, records);
    }

    private byte[] pack(String format, String lang, String title, String who, String period,
                       List<String> headers, List<List<Object>> people, List<FineLine> lines, String mode) {
        if ("csv".equals(format)) {
            return "fines".equals(mode) ? finesCsv(lines) : csvGrid(headers, people);
        }
        return "pdf".equals(format) ? pdf(lang, title, who, period, headers, people, lines, mode)
                : excel(lang, title, who, period, headers, people, lines, mode);
    }

    private byte[] pdf(String lang, String title, String who, String period, List<String> headers,
                       List<List<Object>> people, List<FineLine> lines, String mode) {
        try {
            registerFont();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4.rotate(), 28, 28, 78, 40);
            PdfWriter writer = PdfWriter.getInstance(document, out);
            boolean grid = !"fines".equals(mode);
            boolean finesOn = !"attendance".equals(mode);
            writer.setPageEvent(masthead(lang, grid && finesOn ? word(lang, "attendance") + " + " + word(lang, "fines") : grid ? word(lang, "attendance") : word(lang, "fines"), who, period));
            document.open();
            if (grid && !headers.isEmpty()) {
                document.add(heading(word(lang, "attendance")));
                document.add(gridTable(headers, people));
            }
            if (finesOn) {
                if (grid && !headers.isEmpty()) {
                    document.newPage();
                }
                document.add(heading(word(lang, "fines")));
                document.add(lines.isEmpty() ? new Paragraph(word(lang, "noFines"), font(10, Color.DARK_GRAY)) : fineTable(lang, lines));
            }
            document.add(new Paragraph(" "));
            document.add(new Paragraph(word(lang, "signatures"), font(11, new Color(0xA3, 0x28, 0x3A))));
            document.close();
            return out.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Could not build the attendance PDF", exception);
        }
    }

    private byte[] excel(String lang, String title, String who, String period, List<String> headers,
                         List<List<Object>> people, List<FineLine> lines, String mode) {
        try (XSSFWorkbook book = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Styles styles = new Styles(book);
            int headerRow = 5;
            if (!"fines".equals(mode) && !headers.isEmpty()) {
            Sheet summary = book.createSheet(word(lang, "sheetSummary"));
            writeBanner(book, summary, styles, title, who, period, headers.size());
            writeHeader(summary, styles, headerRow, headers);
            int rowIndex = headerRow + 1;
            for (int i = 0; i < people.size(); i++) {
                Row row = summary.createRow(rowIndex++);
                List<Object> values = people.get(i);
                for (int column = 0; column < values.size(); column++) {
                    writeCell(row, column, values.get(column), styles, i % 2 == 1, column == 2);
                }
            }
            finish(summary, headerRow, rowIndex - 1, headers.size());
            }
            if (!"attendance".equals(mode)) {
            Sheet fines = book.createSheet(word(lang, "sheetFines"));
            String[] fineHeaders = {word(lang, "person"), word(lang, "rule"), word(lang, "source"), word(lang, "date"),
                    word(lang, "amount"), word(lang, "paid"), word(lang, "balance"), word(lang, "status")};
            writeBanner(book, fines, styles, word(lang, "fines"), who, period, fineHeaders.length);
            writeHeader(fines, styles, headerRow, List.of(fineHeaders));
            int fineRow = headerRow + 1;
            long amount = 0;
            long paid = 0;
            long balance = 0;
            for (FineLine line : lines) {
                Row row = fines.createRow(fineRow++);
                writeCell(row, 0, line.person(), styles, false, false);
                writeCell(row, 1, line.rule(), styles, false, false);
                writeCell(row, 2, line.source(), styles, false, false);
                writeCell(row, 3, line.date(), styles, false, false);
                writeCell(row, 4, line.amount(), styles, false, false);
                writeCell(row, 5, line.paid(), styles, false, false);
                writeCell(row, 6, line.balance(), styles, false, false);
                writeCell(row, 7, line.status(), styles, false, false);
                amount += line.amount();
                paid += line.paid();
                balance += line.balance();
            }
            Row total = fines.createRow(fineRow);
            writeCell(total, 0, word(lang, "total"), styles, false, false);
            writeCell(total, 4, amount, styles, false, false);
            writeCell(total, 5, paid, styles, false, false);
            writeCell(total, 6, balance, styles, false, false);
            finish(fines, headerRow, fineRow, fineHeaders.length);
            }
            Sheet info = book.createSheet(word(lang, "sheetInfo"));
            info.createRow(0).createCell(0).setCellValue(title);
            info.createRow(1).createCell(0).setCellValue(word(lang, "period") + ": " + period);
            info.createRow(2).createCell(0).setCellValue(word(lang, "by") + " " + who);
            info.createRow(3).createCell(0).setCellValue(stamp());
            info.setColumnWidth(0, 80 * 256);
            book.write(out);
            return out.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Could not build the attendance workbook", exception);
        }
    }

    private byte[] csvGrid(List<String> headers, List<List<Object>> people) {
        StringBuilder csv = new StringBuilder();
        csv.append(String.join(",", headers)).append('\n');
        for (List<Object> person : people) {
            List<String> cells = new ArrayList<>();
            for (Object value : person) {
                cells.add("\"" + String.valueOf(value).replace("\"", "\"\"") + "\"");
            }
            csv.append(String.join(",", cells)).append('\n');
        }
        return csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private byte[] finesCsv(List<FineLine> lines) {
        StringBuilder csv = new StringBuilder("\uFEFFPerson,Rule,Source,Date,Amount,Paid,Balance,Status\n");
        for (FineLine line : lines) {
            csv.append(String.join(",", List.of(line.person(), line.rule(), line.source(), line.date(),
                    String.valueOf(line.amount()), String.valueOf(line.paid()), String.valueOf(line.balance()), line.status()))).append('\n');
        }
        return csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private byte[] personPdf(String lang, String who, Map<String, Object> summary, List<Map<String, Object>> records) {
        try {
            registerFont();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 36, 36, 78, 40);
            PdfWriter writer = PdfWriter.getInstance(document, out);
            String name = String.valueOf(((Map<?, ?>) summary.get("person")).get("name"));
            writer.setPageEvent(masthead(lang, name, who, word(lang, "since") + " " + text(summary.get("since"))));
            document.open();
            document.add(pairTable(personPairs(lang, summary)));
            document.add(heading(word(lang, "fines")));
            List<FineLine> lines = personFines(lang, summary);
            document.add(lines.isEmpty() ? new Paragraph(word(lang, "noFines"), font(10, Color.DARK_GRAY)) : fineTable(lang, lines));
            document.add(heading(word(lang, "records")));
            document.add(recordTable(lang, records));
            document.close();
            return out.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Could not build the player PDF", exception);
        }
    }

    private byte[] personExcel(String lang, String who, Map<String, Object> summary, List<Map<String, Object>> records) {
        try (XSSFWorkbook book = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Styles styles = new Styles(book);
            String name = String.valueOf(((Map<?, ?>) summary.get("person")).get("name"));
            Sheet sheet = book.createSheet(word(lang, "sheetSummary"));
            writeBanner(book, sheet, styles, name, who, word(lang, "since") + " " + text(summary.get("since")), 2);
            int rowIndex = 5;
            for (String[] pair : personPairs(lang, summary)) {
                Row row = sheet.createRow(rowIndex++);
                writeCell(row, 0, pair[0], styles, false, false);
                Object value = pair.length > 2 ? number(pair[2]) : pair[1];
                writeCell(row, 1, value, styles, false, "rate".equals(pair.length > 3 ? pair[3] : ""));
            }
            finish(sheet, 5, rowIndex - 1, 2);
            Sheet fines = book.createSheet(word(lang, "sheetFines"));
            String[] headers = {word(lang, "person"), word(lang, "rule"), word(lang, "source"), word(lang, "date"), word(lang, "amount"), word(lang, "paid"), word(lang, "balance"), word(lang, "status")};
            writeBanner(book, fines, styles, word(lang, "fines"), who, name, headers.length);
            writeHeader(fines, styles, 5, List.of(headers));
            int fineRow = 6;
            for (FineLine line : personFines(lang, summary)) {
                Row row = fines.createRow(fineRow++);
                writeCell(row, 0, line.person(), styles, false, false);
                writeCell(row, 1, line.rule(), styles, false, false);
                writeCell(row, 2, line.source(), styles, false, false);
                writeCell(row, 3, line.date(), styles, false, false);
                writeCell(row, 4, line.amount(), styles, false, false);
                writeCell(row, 5, line.paid(), styles, false, false);
                writeCell(row, 6, line.balance(), styles, false, false);
                writeCell(row, 7, line.status(), styles, false, false);
            }
            finish(fines, 5, Math.max(fineRow - 1, 5), headers.length);
            book.write(out);
            return out.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Could not build the player workbook", exception);
        }
    }

    private List<String[]> personPairs(String lang, Map<String, Object> summary) {
        List<String[]> rows = new ArrayList<>();
        rows.add(textPair(word(lang, "since"), summary.get("since")));
        rows.add(numberPair(word(lang, "sessions"), summary.get("sessions")));
        rows.add(numberPair(word(lang, "present"), summary.get("present")));
        rows.add(numberPair(word(lang, "late"), summary.get("late")));
        rows.add(numberPair(word(lang, "absent"), summary.get("absent")));
        rows.add(numberPair(word(lang, "excused"), summary.get("excused")));
        rows.add(ratePair(word(lang, "rate"), summary.get("rate")));
        rows.add(ratePair(word(lang, "punctuality"), summary.get("punctuality")));
        rows.add(numberPair(word(lang, "streak"), summary.get("streak")));
        rows.add(textPair(word(lang, "bestMonth"), summary.get("bestMonth")));
        rows.add(moneyPair(word(lang, "finesTotal"), summary.get("finesTotal")));
        rows.add(moneyPair(word(lang, "finesPaid"), summary.get("finesPaid")));
        rows.add(moneyPair(word(lang, "finesOpen"), summary.get("finesOutstanding")));
        return rows;
    }

    private PdfPTable gridTable(List<String> headers, List<List<Object>> people) throws Exception {
        PdfPTable table = new PdfPTable(headers.size());
        table.setWidthPercentage(100);
        table.setHeaderRows(1);
        table.setSplitLate(false);
        float[] widths = new float[headers.size()];
        for (int column = 0; column < headers.size(); column++) {
            widths[column] = column == 0 ? 2.6f : column == 1 ? 1.8f : column == headers.size() - 1 ? 1.3f : 0.85f;
        }
        table.setWidths(widths);
        for (String header : headers) {
            table.addCell(headCell(header));
        }
        for (List<Object> person : people) {
            for (int column = 0; column < person.size(); column++) {
                Object value = person.get(column);
                String shown = column == 0 || column == 1 ? clip(text(value), 22)
                        : column == 2 && value instanceof Number number ? number + "%"
                        : column == person.size() - 1 && value instanceof Number money ? rwf(money.longValue())
                        : text(value);
                PdfPCell cell = bodyCell(shown, column >= 2);
                paintMark(cell, shown);
                table.addCell(cell);
            }
        }
        return table;
    }

    private PdfPTable fineTable(String lang, List<FineLine> lines) {
        PdfPTable table = new PdfPTable(new float[]{2.2f, 1.4f, 2.2f, 1.1f, 1.2f, 1.1f, 1.2f, 1.2f});
        table.setWidthPercentage(100);
        table.setHeaderRows(1);
        table.setSplitLate(false);
        for (String header : List.of(word(lang, "person"), word(lang, "rule"), word(lang, "source"), word(lang, "date"),
                word(lang, "amount"), word(lang, "paid"), word(lang, "balance"), word(lang, "status"))) {
            table.addCell(headCell(header));
        }
        String current = "";
        long subAmount = 0;
        long subPaid = 0;
        long subBalance = 0;
        int count = 0;
        long amount = 0;
        long paid = 0;
        long balance = 0;
        for (FineLine line : lines) {
            if (!current.isEmpty() && !current.equals(line.person()) && count > 1) {
                addSubtotal(table, word(lang, "subtotal") + " · " + current, subAmount, subPaid, subBalance);
            }
            if (!current.equals(line.person())) {
                current = line.person();
                subAmount = 0;
                subPaid = 0;
                subBalance = 0;
                count = 0;
            }
            table.addCell(bodyCell(line.person(), false));
            table.addCell(bodyCell(line.rule(), false));
            table.addCell(bodyCell(line.source(), false));
            table.addCell(bodyCell(line.date(), false));
            table.addCell(bodyCell(rwf(line.amount()), true));
            table.addCell(bodyCell(rwf(line.paid()), true));
            table.addCell(bodyCell(rwf(line.balance()), true));
            PdfPCell status = bodyCell(statusLabel(line.status()), false);
            status.setNoWrap(true);
            table.addCell(status);
            subAmount += line.amount();
            subPaid += line.paid();
            subBalance += line.balance();
            amount += line.amount();
            paid += line.paid();
            balance += line.balance();
            count++;
        }
        if (count > 1) {
            addSubtotal(table, word(lang, "subtotal") + " · " + current, subAmount, subPaid, subBalance);
        }
        addSubtotal(table, word(lang, "total"), amount, paid, balance);
        return table;
    }

    private void addSubtotal(PdfPTable table, String label, long amount, long paid, long balance) {
        PdfPCell name = bodyCell(label, false);
        name.setColspan(4);
        name.setBackgroundColor(new Color(0xF6, 0xEE, 0xF0));
        table.addCell(name);
        for (long value : new long[]{amount, paid, balance}) {
            PdfPCell cell = bodyCell(rwf(value), true);
            cell.setBackgroundColor(new Color(0xF6, 0xEE, 0xF0));
            table.addCell(cell);
        }
        PdfPCell blank = bodyCell("", false);
        blank.setBackgroundColor(new Color(0xF6, 0xEE, 0xF0));
        table.addCell(blank);
    }

    private PdfPTable recordTable(String lang, List<Map<String, Object>> records) {
        String[] headers = {word(lang, "date"), word(lang, "kind"), word(lang, "status"), word(lang, "fine"), word(lang, "recorded")};
        PdfPTable table = new PdfPTable(headers.length);
        table.setWidthPercentage(100);
        table.setHeaderRows(1);
        for (String header : headers) {
            table.addCell(headCell(header));
        }
        for (Map<String, Object> record : records) {
            table.addCell(bodyCell(text(record.get("date")), false));
            table.addCell(bodyCell(text(record.get("kind")), false));
            PdfPCell mark = bodyCell(text(record.get("status")), true);
            paintMark(mark, text(record.get("status")));
            table.addCell(mark);
            Object fine = record.get("fine");
            table.addCell(bodyCell(fine instanceof Number number && number.longValue() > 0 ? rwf(number.longValue()) : "—", true));
            table.addCell(bodyCell(text(record.get("recordedBy")), false));
        }
        return table;
    }

    private PdfPTable pairTable(List<String[]> pairs) {
        PdfPTable table = new PdfPTable(new float[]{1.4f, 1f});
        table.setWidthPercentage(100);
        for (String[] pair : pairs) {
            table.addCell(bodyCell(pair[0], false));
            table.addCell(bodyCell(pair[1], true));
        }
        return table;
    }

    private List<FineLine> lines(String lang, List<Fine> fines) {
        List<FineLine> lines = new ArrayList<>();
        for (Fine fine : fines) {
            if (fine.getStatus() == FineStatus.WAIVED) {
                continue;
            }
            String person = fine.getPlayer() != null ? fine.getPlayer().getFullName()
                    : fine.getStaff() != null ? fine.getStaff().getFullName() : "—";
            String source = fine.getTrainingSession() != null
                    ? word(lang, "training") + " " + fine.getTrainingSession().getSessionDate()
                    : fine.getMatch() != null && fine.getMatch().getOpponent() != null
                    ? word(lang, "match") + " " + fine.getMatch().getOpponent().getName()
                    : word(lang, "manual");
            lines.add(new FineLine(person, ruleName(lang, fine.getFineType()), source,
                    fine.getFineDate() == null ? "" : fine.getFineDate().toString(),
                    money(fine.getAmount()), money(fine.getAmountPaid()), money(fine.balance()), statusLabel(fine.getStatus().name())));
        }
        lines.sort(Comparator.comparing(FineLine::person).thenComparing(FineLine::date));
        return lines;
    }

    private List<FineLine> personFines(String lang, Map<String, Object> summary) {
        List<FineLine> lines = new ArrayList<>();
        Object raw = summary.get("fines");
        if (!(raw instanceof List<?> list)) {
            return lines;
        }
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> fine) || "WAIVED".equals(String.valueOf(fine.get("status")))) {
                continue;
            }
            String person = String.valueOf(((Map<?, ?>) summary.get("person")).get("name"));
            lines.add(new FineLine(person, ruleLabel(lang, text(fine.get("rule"))), sourceLabel(lang, text(fine.get("source"))), text(fine.get("date")),
                    money(fine.get("amount")), money(fine.get("paid")),
                    Math.max(0, money(fine.get("amount")) - money(fine.get("paid"))), statusLabel(text(fine.get("status")))));
        }
        return lines;
    }

    private PdfPageEventHelper masthead(String lang, String title, String who, String period) {
        byte[] mark = logo();
        String when = stamp();
        return new PdfPageEventHelper() {
            @Override
            public void onEndPage(PdfWriter writer, Document document) {
                float top = document.getPageSize().getHeight();
                try {
                    if (mark.length > 0) {
                        Image image = Image.getInstance(mark);
                        image.scaleToFit(54, 54);
                        image.setAbsolutePosition(document.left(), top - 66);
                        writer.getDirectContent().addImage(image);
                    }
                } catch (Exception ignored) {
                    // A missing crest must not stop the report.
                }
                float textX = document.left() + (mark.length > 0 ? 62 : 0);
                ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_LEFT,
                        new Phrase("VIKING FC", font(11, new Color(0xA3, 0x28, 0x3A))), textX, top - 28, 0);
                ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_LEFT,
                        new Phrase(title, font(14, Color.BLACK)), textX, top - 44, 0);
                ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_LEFT,
                        new Phrase(period + "  ·  " + who + "  ·  " + when, font(8, Color.DARK_GRAY)), textX, top - 58, 0);
                PdfContentByte line = writer.getDirectContent();
                line.setColorStroke(new Color(0xA3, 0x28, 0x3A));
                line.setLineWidth(1.4f);
                line.moveTo(document.left(), top - 72);
                line.lineTo(document.right(), top - 72);
                line.stroke();
                Phrase page = new Phrase("Viking FC  ·  " + word(lang, "page") + " " + writer.getPageNumber(), font(8, Color.GRAY));
                ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_RIGHT, page, document.right(), 22, 0);
            }
        };
    }

    private Paragraph heading(String text) {
        Paragraph paragraph = new Paragraph(text, font(13, new Color(0xA3, 0x28, 0x3A)));
        paragraph.setSpacingBefore(14);
        paragraph.setSpacingAfter(6);
        return paragraph;
    }

    private void writeBanner(XSSFWorkbook book, Sheet sheet, Styles styles, String title, String who, String period, int columns) throws Exception {
        for (int index = 0; index < 4; index++) {
            sheet.createRow(index).setHeightInPoints(20);
        }
        byte[] mark = logo();
        if (mark.length > 0) {
            int picture = book.addPicture(mark, XSSFWorkbook.PICTURE_TYPE_PNG);
            XSSFDrawing drawing = (XSSFDrawing) sheet.createDrawingPatriarch();
            drawing.createPicture(new XSSFClientAnchor(0, 0, 0, 0, 0, 0, 2, 4), picture);
        }
        Row titleRow = sheet.getRow(0);
        Cell titleCell = titleRow.createCell(2);
        titleCell.setCellValue("VIKING FC  ·  " + title);
        titleCell.setCellStyle(styles.title);
        sheet.getRow(1).createCell(2).setCellValue(period);
        sheet.getRow(2).createCell(2).setCellValue(who);
        sheet.getRow(3).createCell(2).setCellValue(stamp());
        if (columns > 3) {
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 2, Math.max(2, columns - 1)));
        }
    }

    private void writeHeader(Sheet sheet, Styles styles, int index, List<String> headers) {
        Row row = sheet.createRow(index);
        row.setHeightInPoints(22);
        for (int column = 0; column < headers.size(); column++) {
            Cell cell = row.createCell(column);
            cell.setCellValue(headers.get(column));
            cell.setCellStyle(styles.header);
        }
    }

    private void writeCell(Row row, int column, Object value, Styles styles, boolean zebra, boolean percent) {
        Cell cell = row.createCell(column);
        String mark = value == null ? "" : String.valueOf(value);
        if ("P".equals(mark) || "L".equals(mark) || "A".equals(mark) || "E".equals(mark)) {
            cell.setCellValue(mark);
            cell.setCellStyle(styles.marks.get(mark));
            return;
        }
        if (percent && value instanceof Number number) {
            cell.setCellValue(number.doubleValue() / 100d);
            cell.setCellStyle(styles.percent);
            return;
        }
        if (value instanceof Number number) {
            cell.setCellValue(number.doubleValue());
            cell.setCellStyle(zebra ? styles.moneyZebra : styles.money);
            return;
        }
        cell.setCellValue(mark);
        cell.setCellStyle(zebra ? styles.textZebra : styles.text);
    }

    private void finish(Sheet sheet, int headerRow, int lastRow, int columns) {
        sheet.createFreezePane(1, headerRow + 1);
        if (lastRow >= headerRow && columns > 0) {
            sheet.setAutoFilter(new CellRangeAddress(headerRow, Math.max(headerRow, lastRow), 0, columns - 1));
        }
        for (int column = 0; column < columns; column++) {
            sheet.autoSizeColumn(column);
            int width = sheet.getColumnWidth(column);
            sheet.setColumnWidth(column, Math.min(Math.max(width + 512, 12 * 256), 40 * 256));
        }
        PrintSetup setup = sheet.getPrintSetup();
        setup.setLandscape(true);
        setup.setFitWidth((short) 1);
        setup.setFitHeight((short) 0);
        sheet.setFitToPage(true);
        sheet.setRepeatingRows(new CellRangeAddress(headerRow, headerRow, 0, Math.max(0, columns - 1)));
        sheet.setColumnWidth(0, Math.max(sheet.getColumnWidth(0), 16 * 256));
        sheet.setColumnWidth(1, Math.max(sheet.getColumnWidth(1), 16 * 256));
    }

    private static void paintMark(PdfPCell cell, String mark) {
        if ("P".equals(mark)) {
            cell.setBackgroundColor(new Color(0xE5, 0xF6, 0xDF));
        } else if ("L".equals(mark)) {
            cell.setBackgroundColor(new Color(0xF8, 0xE7, 0xC4));
        } else if ("A".equals(mark)) {
            cell.setBackgroundColor(new Color(0xA3, 0x28, 0x3A));
            cell.setPhrase(new Phrase(mark, font(8, Color.WHITE)));
        } else if ("E".equals(mark)) {
            cell.setBackgroundColor(new Color(0xD9, 0xE8, 0xFB));
        }
    }

    private static PdfPCell headCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font(8, Color.WHITE)));
        cell.setBackgroundColor(new Color(0xA3, 0x28, 0x3A));
        cell.setPadding(5);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        return cell;
    }

    private static PdfPCell bodyCell(String text, boolean right) {
        PdfPCell cell = new PdfPCell(new Phrase(text == null || text.isBlank() ? "—" : text, font(8, Color.BLACK)));
        cell.setPadding(4);
        cell.setHorizontalAlignment(right ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    private static Font font(float size, Color color) {
        Font font = FontFactory.getFont("Viking", "Cp1252", true, size);
        if ("unknown".equals(font.getFamilyname())) {
            font = FontFactory.getFont(FontFactory.HELVETICA, size);
        }
        font.setColor(color);
        return font;
    }

    private static void registerFont() {
        Path arial = Path.of("C:/Windows/Fonts/arial.ttf");
        if (Files.exists(arial)) {
            FontFactory.register(arial.toString(), "Viking");
        }
    }

    private static byte[] logo() {
        try (var in = AttendanceReport.class.getResourceAsStream("/brand/logo.png")) {
            return in == null ? new byte[0] : in.readAllBytes();
        } catch (Exception exception) {
            return new byte[0];
        }
    }

    private static String statusLabel(String status) {
        return switch (status) {
            case "OUTSTANDING" -> "Outstanding";
            case "PARTIALLY_PAID" -> "Partial";
            case "PAID" -> "Paid";
            case "WAIVED" -> "Waived";
            default -> status == null || status.isBlank() ? "—" : status;
        };
    }

    private static String sourceLabel(String lang, String raw) {
        if (raw.startsWith("Training ")) {
            return word(lang, "training") + raw.substring("Training".length());
        }
        if (raw.startsWith("Match vs ")) {
            return word(lang, "match") + " " + raw.substring("Match vs ".length());
        }
        if ("Manual".equals(raw)) {
            return word(lang, "manual");
        }
        return raw.isBlank() ? "—" : raw;
    }

    private static String stamp() {
        return ZonedDateTime.now(KIGALI).format(STAMP) + " Kigali";
    }

    private static String rwf(long amount) {
        return "RWF " + NumberFormat.getIntegerInstance(Locale.US).format(amount);
    }

    private static long money(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal.longValue();
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return 0;
    }

    private static Object number(String raw) {
        try {
            return raw.contains(".") ? Double.parseDouble(raw) : Long.parseLong(raw);
        } catch (NumberFormatException exception) {
            return raw;
        }
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static String clip(String value, int max) {
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, max - 1) + "…";
    }

    private static String[] textPair(String label, Object value) {
        return new String[]{label, text(value).isBlank() ? "—" : text(value)};
    }

    private static String[] numberPair(String label, Object value) {
        return new String[]{label, text(value), text(value), "number"};
    }

    private static String[] ratePair(String label, Object value) {
        return new String[]{label, text(value) + "%", text(value), "rate"};
    }

    private static String[] moneyPair(String label, Object value) {
        long amount = money(value);
        return new String[]{label, rwf(amount), String.valueOf(amount), "money"};
    }

    private static String ruleName(String lang, FineType type) {
        if (type == FineType.TRAINING_LATE) {
            return ruleLabel(lang, "6");
        }
        if (type == FineType.TRAINING_ABSENCE) {
            return ruleLabel(lang, "7");
        }
        if (type == FineType.MATCH_LATE || type == FineType.MATCH_ABSENCE) {
            return ruleLabel(lang, "8");
        }
        return type == null ? "" : type.name();
    }

    private static String ruleLabel(String lang, String rule) {
        String name = switch (rule) {
            case "6" -> "fr".equals(lang) ? "Retard à l'entraînement" : "rw".equals(lang) ? "Gukererwa imyitozo" : "Late to training";
            case "7" -> "fr".equals(lang) ? "Absence à l'entraînement" : "rw".equals(lang) ? "Kutitabira imyitozo" : "Absence from training";
            case "8" -> "fr".equals(lang) ? "Retard ou absence au match" : "rw".equals(lang) ? "Gukererwa cyangwa kutitabira umukino" : "Match lateness or absence";
            default -> "";
        };
        String prefix = "fr".equals(lang) ? "Règle " : "rw".equals(lang) ? "Itegeko " : "Rule ";
        return rule.isBlank() ? name : prefix + rule + (name.isBlank() ? "" : " · " + name);
    }

    private static String word(String lang, String key) {
        Map<String, String> en = Map.ofEntries(
                Map.entry("attendance", "Attendance"), Map.entry("fines", "Fines"), Map.entry("noFines", "No fines in this period."),
                Map.entry("signatures", "Team Manager ____________________          Coach ____________________"),
                Map.entry("page", "page"), Map.entry("person", "Person"), Map.entry("rule", "Rule"),
                Map.entry("source", "Source"), Map.entry("date", "Date"), Map.entry("amount", "Amount (RWF)"),
                Map.entry("paid", "Paid (RWF)"), Map.entry("balance", "Balance (RWF)"), Map.entry("status", "Status"),
                Map.entry("total", "Total"), Map.entry("subtotal", "Subtotal"), Map.entry("training", "Training"),
                Map.entry("match", "Match vs"), Map.entry("manual", "Manual"), Map.entry("period", "Period"),
                Map.entry("by", "Generated by"), Map.entry("sheetSummary", "Summary"), Map.entry("sheetFines", "Fines"),
                Map.entry("sheetInfo", "Info"), Map.entry("since", "Since"), Map.entry("sessions", "Sessions"),
                Map.entry("present", "Present"), Map.entry("late", "Late"), Map.entry("absent", "Absent"),
                Map.entry("excused", "Excused"), Map.entry("rate", "Attendance rate"), Map.entry("punctuality", "Punctuality"),
                Map.entry("streak", "Current streak"), Map.entry("bestMonth", "Best month"),
                Map.entry("finesTotal", "Fines total (RWF)"), Map.entry("finesPaid", "Fines paid (RWF)"),
                Map.entry("finesOpen", "Fines outstanding (RWF)"), Map.entry("records", "Records"),
                Map.entry("kind", "Kind"), Map.entry("fine", "Fine"), Map.entry("recorded", "Recorded by"));
        Map<String, String> fr = new LinkedHashMap<>(en);
        fr.put("attendance", "Présences");
        fr.put("fines", "Amendes");
        fr.put("noFines", "Aucune amende sur cette période.");
        fr.put("signatures", "Team Manager ____________________          Entraîneur ____________________");
        fr.put("page", "page");
        fr.put("person", "Personne");
        fr.put("rule", "Règle");
        fr.put("source", "Origine");
        fr.put("date", "Date");
        fr.put("amount", "Montant (RWF)");
        fr.put("paid", "Payé (RWF)");
        fr.put("balance", "Solde (RWF)");
        fr.put("status", "Statut");
        fr.put("total", "Total");
        fr.put("subtotal", "Sous-total");
        fr.put("training", "Entraînement");
        fr.put("match", "Match contre");
        fr.put("manual", "Manuel");
        fr.put("period", "Période");
        fr.put("by", "Établi par");
        fr.put("since", "Depuis");
        fr.put("sessions", "Séances");
        fr.put("present", "Présent");
        fr.put("late", "En retard");
        fr.put("absent", "Absent");
        fr.put("excused", "Excusé");
        fr.put("rate", "Taux de présence");
        fr.put("punctuality", "Ponctualité");
        fr.put("streak", "Série en cours");
        fr.put("bestMonth", "Meilleur mois");
        fr.put("finesTotal", "Amendes émises (RWF)");
        fr.put("finesPaid", "Amendes payées (RWF)");
        fr.put("finesOpen", "Amendes dues (RWF)");
        fr.put("records", "Pointages");
        fr.put("kind", "Type");
        fr.put("fine", "Amende");
        fr.put("recorded", "Saisi par");
        Map<String, String> rw = new LinkedHashMap<>(en);
        rw.put("attendance", "Kwitabira");
        rw.put("fines", "Amande");
        rw.put("noFines", "Nta hazabu muri iki gihe.");
        rw.put("person", "Umuntu");
        rw.put("rule", "Itegeko");
        rw.put("source", "Inkomoko");
        rw.put("date", "Itariki");
        rw.put("amount", "Amafaranga (RWF)");
        rw.put("paid", "Yishyuwe (RWF)");
        rw.put("balance", "Isigaye (RWF)");
        rw.put("status", "Imiterere");
        rw.put("total", "Igiteranyo");
        rw.put("subtotal", "Igiteranyo gito");
        rw.put("training", "Imyitozo");
        rw.put("match", "Umukino na");
        rw.put("manual", "Intoki");
        rw.put("period", "Igihe");
        rw.put("by", "Byakozwe na");
        rw.put("since", "Kuva");
        rw.put("sessions", "Amasomo");
        rw.put("present", "Yari ahari");
        rw.put("late", "Yakererewe");
        rw.put("absent", "Ntiyaje");
        rw.put("excused", "Yemerewe");
        rw.put("rate", "Igipimo cyo kwitabira");
        rw.put("punctuality", "Kwitwararika");
        rw.put("records", "Ibibaruwe");
        Map<String, String> chosen = "fr".equals(lang) ? fr : "rw".equals(lang) ? rw : en;
        return chosen.getOrDefault(key, en.getOrDefault(key, key));
    }

    public record FineLine(String person, String rule, String source, String date, long amount, long paid, long balance, String status) {
    }

    private static final class Styles {
        private final XSSFCellStyle title;
        private final XSSFCellStyle header;
        private final XSSFCellStyle text;
        private final XSSFCellStyle textZebra;
        private final XSSFCellStyle money;
        private final XSSFCellStyle moneyZebra;
        private final XSSFCellStyle percent;
        private final Map<String, XSSFCellStyle> marks = new LinkedHashMap<>();

        private Styles(XSSFWorkbook book) {
            XSSFFont white = book.createFont();
            white.setBold(true);
            white.setColor(IndexedColors.WHITE.getIndex());
            XSSFFont bold = book.createFont();
            bold.setBold(true);
            bold.setFontHeightInPoints((short) 14);
            title = book.createCellStyle();
            title.setFont(bold);
            header = fill(book, RED);
            header.setFont(white);
            header.setAlignment(HorizontalAlignment.CENTER);
            text = book.createCellStyle();
            text.setVerticalAlignment(VerticalAlignment.CENTER);
            textZebra = fill(book, new byte[]{(byte) 0xFB, (byte) 0xF6, (byte) 0xF7});
            money = book.createCellStyle();
            money.setDataFormat(book.createDataFormat().getFormat("#,##0"));
            money.setAlignment(HorizontalAlignment.RIGHT);
            moneyZebra = fill(book, new byte[]{(byte) 0xFB, (byte) 0xF6, (byte) 0xF7});
            moneyZebra.setDataFormat(book.createDataFormat().getFormat("#,##0"));
            moneyZebra.setAlignment(HorizontalAlignment.RIGHT);
            percent = book.createCellStyle();
            percent.setDataFormat(book.createDataFormat().getFormat("0.0%"));
            percent.setAlignment(HorizontalAlignment.RIGHT);
            marks.put("P", mark(book, new byte[]{(byte) 0xE5, (byte) 0xF6, (byte) 0xDF}));
            marks.put("L", mark(book, new byte[]{(byte) 0xF8, (byte) 0xE7, (byte) 0xC4}));
            marks.put("A", mark(book, RED));
            marks.get("A").setFont(white);
            marks.put("E", mark(book, new byte[]{(byte) 0xD9, (byte) 0xE8, (byte) 0xFB}));
        }

        private static XSSFCellStyle fill(XSSFWorkbook book, byte[] rgb) {
            XSSFCellStyle style = book.createCellStyle();
            style.setFillForegroundColor(new XSSFColor(rgb, null));
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            style.setVerticalAlignment(VerticalAlignment.CENTER);
            return style;
        }

        private static XSSFCellStyle mark(XSSFWorkbook book, byte[] rgb) {
            XSSFCellStyle style = fill(book, rgb);
            style.setAlignment(HorizontalAlignment.CENTER);
            return style;
        }
    }
}
