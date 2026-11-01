package rw.vikingfc.club.report;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import rw.vikingfc.club.common.KigaliClock;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.List;

@Component
public class DirectorySheets {

    private static final Color RED = new Color(163, 40, 58);

    public byte[] excel(String title, String who, List<String> headers, List<List<String>> rows) {
        try (XSSFWorkbook book = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFSheet sheet = book.createSheet(title.length() > 31 ? title.substring(0, 31) : title);
            byte[] logo = logo();
            if (logo != null) {
                int picture = book.addPicture(logo, XSSFWorkbook.PICTURE_TYPE_PNG);
                var anchor = book.getCreationHelper().createClientAnchor();
                anchor.setCol1(0);
                anchor.setRow1(0);
                anchor.setCol2(1);
                anchor.setRow2(3);
                sheet.createDrawingPatriarch().createPicture(anchor, picture);
            }
            XSSFRow titleRow = sheet.createRow(3);
            titleRow.createCell(1).setCellValue(title);
            titleRow.createCell(3).setCellValue(KigaliClock.format(Instant.now(), "en") + " · " + who);
            XSSFCellStyle head = book.createCellStyle();
            head.setFillForegroundColor(new XSSFColor(new byte[] {(byte) 0xa3, 0x28, 0x3a}, null));
            head.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            XSSFFont font = book.createFont();
            font.setBold(true);
            font.setColor(new XSSFColor(new byte[] {(byte) 255, (byte) 255, (byte) 255}, null));
            head.setFont(font);
            XSSFRow header = sheet.createRow(5);
            for (int i = 0; i < headers.size(); i++) {
                var cell = header.createCell(i);
                cell.setCellValue(headers.get(i));
                cell.setCellStyle(head);
            }
            XSSFCellStyle zebra = book.createCellStyle();
            zebra.setFillForegroundColor(new XSSFColor(new byte[] {(byte) 248, (byte) 244, (byte) 244}, null));
            zebra.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            for (int r = 0; r < rows.size(); r++) {
                XSSFRow line = sheet.createRow(6 + r);
                List<String> values = rows.get(r);
                for (int c = 0; c < headers.size(); c++) {
                    var cell = line.createCell(c);
                    cell.setCellValue(c < values.size() && values.get(c) != null ? values.get(c) : "");
                    if (r % 2 == 1) cell.setCellStyle(zebra);
                }
            }
            sheet.createFreezePane(0, 6);
            sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(5, 5 + Math.max(rows.size(), 1), 0, Math.max(headers.size() - 1, 0)));
            for (int i = 0; i < headers.size(); i++) sheet.setColumnWidth(i, 22 * 256);
            book.write(out);
            return out.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("The workbook could not be built", ex);
        }
    }

    public byte[] pdf(String title, String who, List<String> headers, List<List<String>> rows, String lang) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4.rotate(), 36, 36, 72, 48);
            PdfWriter writer = PdfWriter.getInstance(document, out);
            document.open();
            Footer footer = new Footer(who, lang);
            writer.setPageEvent(footer);
            byte[] logo = logo();
            if (logo != null) {
                Image image = Image.getInstance(logo);
                image.scaleToFit(42, 42);
                image.setAbsolutePosition(36, document.getPageSize().getHeight() - 58);
                document.add(image);
            }
            Font titleFont = new Font(base(), 16, Font.BOLD, RED);
            Paragraph heading = new Paragraph(title, titleFont);
            heading.setSpacingAfter(12);
            heading.setIndentationLeft(52);
            document.add(heading);
            PdfPTable table = new PdfPTable(headers.size());
            table.setWidthPercentage(100);
            Font headFont = new Font(base(), 9, Font.BOLD, Color.WHITE);
            Font cellFont = new Font(base(), 9, Font.NORMAL, Color.BLACK);
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headFont));
                cell.setBackgroundColor(RED);
                cell.setPadding(6);
                cell.setBorderColor(new Color(230, 230, 230));
                table.addCell(cell);
            }
            for (int r = 0; r < rows.size(); r++) {
                List<String> values = rows.get(r);
                for (int c = 0; c < headers.size(); c++) {
                    String value = c < values.size() && values.get(c) != null && !values.get(c).isBlank() ? values.get(c) : "—";
                    PdfPCell cell = new PdfPCell(new Phrase(value, cellFont));
                    cell.setPadding(6);
                    cell.setBorderColor(new Color(230, 230, 230));
                    if (r % 2 == 1) cell.setBackgroundColor(new Color(248, 244, 244));
                    table.addCell(cell);
                }
            }
            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("The directory could not be built", ex);
        }
    }

    private static byte[] logo() {
        try (var in = DirectorySheets.class.getResourceAsStream("/brand/logo.png")) {
            return in == null ? null : in.readAllBytes();
        } catch (Exception ex) {
            return null;
        }
    }

    private static BaseFont base() {
        try {
            return BaseFont.createFont("C:/Windows/Fonts/arial.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        } catch (Exception ex) {
            try {
                return BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
            } catch (Exception nested) {
                throw new IllegalStateException(nested);
            }
        }
    }

    private static final class Footer extends PdfPageEventHelper {
        private final String who;
        private final String lang;
        private final BaseFont font = base();
        private PdfTemplate total;

        private Footer(String who, String lang) {
            this.who = who;
            this.lang = lang;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            if (total == null) {
                total = writer.getDirectContent().createTemplate(24, 12);
            }
            var canvas = writer.getDirectContent();
            canvas.beginText();
            canvas.setFontAndSize(font, 8);
            canvas.setColorFill(new Color(90, 90, 90));
            String left = ("fr".equals(lang) ? "Produit par " : "rw".equals(lang) ? "Byakozwe na " : "Generated by ") + who;
            canvas.showTextAligned(Element.ALIGN_LEFT, left, document.left(), 24, 0);
            String page = ("fr".equals(lang) ? "Page " : "rw".equals(lang) ? "Paji " : "Page ") + writer.getPageNumber() + " / ";
            canvas.showTextAligned(Element.ALIGN_RIGHT, page, document.right() - 20, 24, 0);
            canvas.endText();
            canvas.addTemplate(total, document.right() - 18, 24);
        }

        @Override
        public void onCloseDocument(PdfWriter writer, Document document) {
            if (total == null) {
                return;
            }
            total.beginText();
            total.setFontAndSize(font, 8);
            total.showText(String.valueOf(writer.getPageNumber() - 1));
            total.endText();
        }
    }
}
