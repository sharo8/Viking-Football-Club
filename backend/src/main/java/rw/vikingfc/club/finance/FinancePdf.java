package rw.vikingfc.club.finance;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Branded finance PDF. The header is drawn in the top margin so the logo never covers the title.
 */
public final class FinancePdf {

    private static final Color RED = new Color(163, 40, 58);
    private static final Color ZEBR = new Color(248, 244, 245);
    private static final Color LINE = new Color(226, 220, 222);
    private static final ZoneId KIGALI = ZoneId.of("Africa/Kigali");

    private FinancePdf() {
    }

    public static byte[] render(String what, String who, List<String[]> rows, byte[] logo, String generatedBy) {
        boolean wide = "SALARIES".equals(what) || "FULL".equals(what) || "SOCIAL".equals(what);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(wide ? PageSize.A4.rotate() : PageSize.A4, 42, 42, 92, 46);
            PdfWriter writer = PdfWriter.getInstance(document, out);
            Fonts fonts = fonts();
            String title = titleOf(what);
            String subtitle = subtitleOf(who);
            String when = ZonedDateTime.now(KIGALI).format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.ENGLISH)) + " Kigali";
            writer.setPageEvent(new Header(logo, title, subtitle, generatedBy, when, fonts));
            document.open();
            document.add(new Phrase(title, fonts.title));
            document.add(new Phrase("\n" + subtitle + "\n\n", fonts.body));
            List<List<String[]>> tables = split(rows);
            if (tables.isEmpty()) {
                document.add(note(emptyMessage(what), fonts));
            }
            for (List<String[]> table : tables) {
                if (table.isEmpty()) continue;
                String[] header = table.get(0);
                document.add(new Phrase(sectionTitle(header), fonts.section));
                document.add(new Phrase("\n"));
                PdfPTable pdf = new PdfPTable(header.length);
                pdf.setWidthPercentage(100);
                pdf.setHeaderRows(1);
                pdf.setSplitLate(false);
                for (String cell : header) {
                    pdf.addCell(cell(display(cell, header, true), fonts.header, RED, Color.WHITE, Element.ALIGN_LEFT));
                }
                List<String[]> body = table.subList(1, table.size());
                if (body.isEmpty()) {
                    PdfPCell empty = cell(emptyMessage(what), fonts.body, Color.WHITE, new Color(60, 50, 52), Element.ALIGN_LEFT);
                    empty.setColspan(header.length);
                    empty.setPadding(10);
                    pdf.addCell(empty);
                }
                BigDecimal[] totals = new BigDecimal[header.length];
                int index = 0;
                for (String[] line : body) {
                    Color background = index++ % 2 == 0 ? Color.WHITE : ZEBR;
                    for (int column = 0; column < header.length; column++) {
                        String raw = column < line.length ? line[column] : "";
                        boolean money = moneyColumn(header[column]) && raw != null && raw.matches("-?\\d+");
                        if (money) {
                            totals[column] = (totals[column] == null ? BigDecimal.ZERO : totals[column]).add(new BigDecimal(raw));
                        }
                        int align = money ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT;
                        pdf.addCell(cell(display(raw, new String[]{header[column]}, false), fonts.body, background, new Color(32, 28, 29), align));
                    }
                }
                if (!body.isEmpty() && hasMoney(header)) {
                    for (int column = 0; column < header.length; column++) {
                        String value = column == 0 ? "Subtotal" : totals[column] == null ? "" : "RWF " + new DecimalFormat("#,##0").format(totals[column]);
                        pdf.addCell(cell(value, fonts.header, new Color(243, 232, 234), RED, column == 0 ? Element.ALIGN_LEFT : Element.ALIGN_RIGHT));
                    }
                }
                document.add(pdf);
                document.add(new Phrase("\n"));
            }
            document.add(new Phrase("\nPresident ________________    General Manager ________________    Finance Manager ________________\n", fonts.body));
            document.close();
            return out.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Could not build the finance PDF", ex);
        }
    }

    public static String fileName(String what, String who) {
        String month = ZonedDateTime.now(KIGALI).format(DateTimeFormatter.ofPattern("yyyy-MM"));
        return "VIKING-FC_" + filePart(what) + "_" + filePart(who) + "_" + month + ".pdf";
    }

    private static List<List<String[]>> split(List<String[]> rows) {
        List<List<String[]>> tables = new ArrayList<>();
        List<String[]> current = null;
        for (String[] line : rows) {
            if (isHeader(line)) {
                current = new ArrayList<>();
                tables.add(current);
            }
            if (current != null) current.add(line);
        }
        return tables;
    }

    private static boolean isHeader(String[] line) {
        if (line.length == 0) return false;
        for (String cell : line) {
            if (cell != null && cell.matches("-?\\d+")) return false;
        }
        return true;
    }

    private static String display(String raw, String[] headerHint, boolean header) {
        if (raw == null) return "";
        if (!header && raw.matches("-?\\d+") && headerHint.length == 1 && moneyColumn(headerHint[0])) {
            return "RWF " + new DecimalFormat("#,##0").format(new BigDecimal(raw));
        }
        return switch (raw.toLowerCase(Locale.ROOT)) {
            case "players" -> "Players";
            case "staff" -> "Staff";
            case "administration" -> "Administration";
            default -> raw;
        };
    }

    private static boolean moneyColumn(String header) {
        String value = header == null ? "" : header.toLowerCase(Locale.ROOT);
        return value.contains("pay") || value.contains("paid") || value.contains("due") || value.contains("fund")
                || value.contains("fine") || value.contains("bonus") || value.contains("amount") || value.contains("payable");
    }

    private static boolean hasMoney(String[] header) {
        for (String cell : header) if (moneyColumn(cell)) return true;
        return false;
    }

    private static String sectionTitle(String[] header) {
        if (header.length > 0 && "Person".equals(header[0])) return "Payroll";
        return header.length > 0 ? header[0] : "Report";
    }

    private static String titleOf(String what) {
        return switch (what) {
            case "SALARIES" -> "Salaries report";
            case "BONUSES" -> "Bonuses report";
            case "EXPENSES" -> "Expenses report";
            case "FINES" -> "Fines and deductions report";
            case "SOCIAL" -> "Social fund report";
            case "RECEIPTS" -> "Receipts register";
            default -> "Full finance report";
        };
    }

    private static String subtitleOf(String who) {
        String scope = switch (who) {
            case "PLAYERS" -> "Players";
            case "STAFF" -> "Staff";
            case "ADMINISTRATION" -> "Administration";
            default -> "All people";
        };
        String month = ZonedDateTime.now(KIGALI).format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH));
        return scope + " · " + month;
    }

    private static String emptyMessage(String what) {
        if ("BONUSES".equals(what)) {
            return "No bonuses recorded for this period. Bonus rates are set per player; earned bonuses appear after matches are finalized.";
        }
        return "No rows recorded for this period.";
    }

    private static String filePart(String value) {
        return switch (value) {
            case "SALARIES" -> "Salaries";
            case "BONUSES" -> "Bonuses";
            case "EXPENSES" -> "Expenses";
            case "FINES" -> "Fines";
            case "SOCIAL" -> "SocialFund";
            case "RECEIPTS" -> "Receipts";
            case "PLAYERS" -> "Players";
            case "STAFF" -> "Staff";
            case "ADMINISTRATION" -> "Administration";
            case "ALL" -> "All";
            default -> "Full";
        };
    }

    private static PdfPTable note(String text, Fonts fonts) {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        PdfPCell cell = cell(text, fonts.body, Color.WHITE, new Color(60, 50, 52), Element.ALIGN_LEFT);
        cell.setPadding(12);
        table.addCell(cell);
        return table;
    }

    private static PdfPCell cell(String text, Font font, Color background, Color color, int align) {
        Font painted = new Font(font.getBaseFont(), font.getSize(), font.getStyle(), color);
        PdfPCell cell = new PdfPCell(new Phrase(text == null ? "" : text, painted));
        cell.setBackgroundColor(background);
        cell.setBorderColor(LINE);
        cell.setBorderWidth(0.5f);
        cell.setPadding(4);
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setMinimumHeight(18);
        return cell;
    }

    private static Fonts fonts() {
        try {
            BaseFont base = BaseFont.createFont("C:/Windows/Fonts/arial.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            return new Fonts(
                    new Font(base, 16, Font.BOLD, RED),
                    new Font(base, 12, Font.BOLD, RED),
                    new Font(base, 8, Font.BOLD, Color.WHITE),
                    new Font(base, 8, Font.NORMAL, new Color(32, 28, 29)),
                    new Font(base, 8, Font.NORMAL, new Color(90, 80, 84))
            );
        } catch (Exception ex) {
            return new Fonts(
                    new Font(Font.HELVETICA, 16, Font.BOLD, RED),
                    new Font(Font.HELVETICA, 12, Font.BOLD, RED),
                    new Font(Font.HELVETICA, 8, Font.BOLD, Color.WHITE),
                    new Font(Font.HELVETICA, 8, Font.NORMAL, new Color(32, 28, 29)),
                    new Font(Font.HELVETICA, 8, Font.NORMAL, new Color(90, 80, 84))
            );
        }
    }

    private record Fonts(Font title, Font section, Font header, Font body, Font muted) {
    }

    private static final class Header extends PdfPageEventHelper {
        private final byte[] logo;
        private final String title;
        private final String subtitle;
        private final String generatedBy;
        private final String when;
        private final Fonts fonts;

        private Header(byte[] logo, String title, String subtitle, String generatedBy, String when, Fonts fonts) {
            this.logo = logo;
            this.title = title;
            this.subtitle = subtitle;
            this.generatedBy = generatedBy == null ? "" : generatedBy;
            this.when = when;
            this.fonts = fonts;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            Rectangle page = document.getPageSize();
            float top = page.getTop() - 18;
            try {
                if (logo != null && logo.length > 0) {
                    Image image = Image.getInstance(logo);
                    image.scaleToFit(54, 51);
                    image.setAbsolutePosition(page.getLeft() + 42, top - 48);
                    writer.getDirectContent().addImage(image);
                }
            } catch (Exception ignored) {
                // A missing logo leaves the title box intact.
            }
            com.lowagie.text.pdf.ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_LEFT,
                    new Phrase("VIKING FC", fonts.title), page.getLeft() + 108, top - 16, 0);
            com.lowagie.text.pdf.ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_LEFT,
                    new Phrase(title, fonts.section), page.getLeft() + 108, top - 32, 0);
            com.lowagie.text.pdf.ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_LEFT,
                    new Phrase(subtitle, fonts.muted), page.getLeft() + 108, top - 44, 0);
            com.lowagie.text.pdf.ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_RIGHT,
                    new Phrase(when, fonts.muted), page.getRight() - 42, top - 16, 0);
            com.lowagie.text.pdf.ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_RIGHT,
                    new Phrase(generatedBy, fonts.muted), page.getRight() - 42, top - 30, 0);
            writer.getDirectContent().setColorStroke(RED);
            writer.getDirectContent().setLineWidth(1.2f);
            writer.getDirectContent().moveTo(page.getLeft() + 42, top - 56);
            writer.getDirectContent().lineTo(page.getRight() - 42, top - 56);
            writer.getDirectContent().stroke();
            com.lowagie.text.pdf.ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_LEFT,
                    new Phrase("VIKING FC  ·  Confidential: contains salary data", fonts.muted), page.getLeft() + 42, page.getBottom() + 22, 0);
            com.lowagie.text.pdf.ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_RIGHT,
                    new Phrase("Page " + writer.getPageNumber(), fonts.muted), page.getRight() - 42, page.getBottom() + 22, 0);
        }
    }
}
