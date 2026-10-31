package rw.vikingfc.club.contract;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfGState;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;

public final class CaptainPdf {

    private static final Color RED = new Color(0xA3, 0x28, 0x3A);
    private static final Color INK = new Color(0x16, 0x16, 0x16);
    private static final Color MUTED = new Color(0x5C, 0x5C, 0x5C);
    private static final Color LINE = new Color(0xE6, 0xE6, 0xE6);
    private static final Color ZEBR = new Color(0xF7, 0xF4, 0xF5);
    private static final Color GRAY = new Color(0x2A, 0x2A, 0x2A);

    private CaptainPdf() {
    }

    public record Sheet(String lang, String seasonName, Instant generatedAt, String generatedBy,
                        List<Slot> captains, List<Row> history, byte[] logo) {
    }

    public record Slot(CaptainRank rank, String fullName, String knownAs, Integer jersey, String position,
                       Integer age, Instant since, byte[] photo) {
    }

    public record Row(String player, CaptainRank rank, Instant start, Instant end, String changedBy) {
    }

    public static String fileName(String seasonName) {
        String raw = seasonName == null ? "" : seasonName;
        java.util.regex.Matcher years = java.util.regex.Pattern.compile("(\\d{4})\\s*/\\s*(\\d{4})").matcher(raw);
        String token = years.find() ? years.group(1) + "-" + years.group(2) : raw.replace('/', '-').replaceAll("[^A-Za-z0-9._-]", "");
        if (token.isBlank()) {
            token = "season";
        }
        return "Viking_FC_Captains_" + token + ".pdf";
    }

    public static byte[] render(Sheet sheet) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 36, 36, 36, 46);
            PdfWriter writer = PdfWriter.getInstance(document, out);
            BaseFont regular = font("Inter-Regular.ttf");
            BaseFont bold = font("Inter-SemiBold.ttf");
            Font body = new Font(regular, 9, Font.NORMAL, INK);
            Font small = new Font(regular, 8, Font.NORMAL, MUTED);
            Font strong = new Font(bold, 11, Font.NORMAL, INK);
            Font white = new Font(bold, 9, Font.NORMAL, Color.WHITE);
            Font head = new Font(bold, 8, Font.NORMAL, Color.WHITE);
            Locale locale = locale(sheet.lang());
            DateTimeFormatter date = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale).withZone(CaptainSpan.KIGALI);
            DateTimeFormatter stamp = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withLocale(locale).withZone(CaptainSpan.KIGALI);
            Brand brand = new Brand(sheet, regular, date, stamp);
            writer.setPageEvent(brand);
            document.open();
            document.add(header(sheet, bold, regular, stamp));
            document.add(rule());
            document.add(new Paragraph(text(sheet.lang(), "captains"), new Font(bold, 13, Font.NORMAL, RED)));
            document.add(cards(sheet, bold, regular, white, body, small, date));
            document.add(new Paragraph(text(sheet.lang(), "summary"), new Font(bold, 13, Font.NORMAL, RED)));
            document.add(summary(sheet, head, body, date));
            document.add(new Paragraph(" "));
            document.add(new Paragraph(text(sheet.lang(), "history"), new Font(bold, 13, Font.NORMAL, RED)));
            document.add(history(sheet, head, body, date));
            document.close();
            return out.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("The captain sheet could not be built", ex);
        }
    }

    private static PdfPTable header(Sheet sheet, BaseFont bold, BaseFont regular, DateTimeFormatter stamp) throws Exception {
        PdfPTable table = new PdfPTable(new float[]{70, 280, 180});
        table.setWidthPercentage(100);
        PdfPCell logo = new PdfPCell();
        logo.setBorder(Rectangle.NO_BORDER);
        logo.setVerticalAlignment(Element.ALIGN_MIDDLE);
        if (sheet.logo() != null && sheet.logo().length > 0) {
            Image image = Image.getInstance(sheet.logo());
            image.scaleToFit(64, 64);
            logo.addElement(image);
        }
        table.addCell(logo);
        PdfPCell name = new PdfPCell();
        name.setBorder(Rectangle.NO_BORDER);
        name.setVerticalAlignment(Element.ALIGN_MIDDLE);
        name.addElement(new Paragraph("VIKING FC", new Font(bold, 18, Font.NORMAL, RED)));
        name.addElement(new Paragraph("ONE CLUB  |  ONE FAMILY  |  ONE MISSION", new Font(regular, 8, Font.NORMAL, MUTED)));
        table.addCell(name);
        PdfPCell title = new PdfPCell();
        title.setBorder(Rectangle.NO_BORDER);
        title.setHorizontalAlignment(Element.ALIGN_RIGHT);
        title.setVerticalAlignment(Element.ALIGN_MIDDLE);
        Paragraph heading = new Paragraph(text(sheet.lang(), "title"), new Font(bold, 13, Font.NORMAL, INK));
        heading.setAlignment(Element.ALIGN_RIGHT);
        Paragraph season = new Paragraph(sheet.seasonName() == null ? "" : sheet.seasonName(), new Font(regular, 10, Font.NORMAL, RED));
        season.setAlignment(Element.ALIGN_RIGHT);
        Paragraph when = new Paragraph(sheet.generatedAt() == null ? "" : stamp.format(sheet.generatedAt()), new Font(regular, 8, Font.NORMAL, MUTED));
        when.setAlignment(Element.ALIGN_RIGHT);
        title.addElement(heading);
        title.addElement(season);
        title.addElement(when);
        table.addCell(title);
        table.setSpacingAfter(6);
        return table;
    }

    private static PdfPTable rule() {
        PdfPTable line = new PdfPTable(1);
        line.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell();
        cell.setFixedHeight(2.5f);
        cell.setBackgroundColor(RED);
        cell.setBorder(Rectangle.NO_BORDER);
        line.addCell(cell);
        line.setSpacingAfter(12);
        return line;
    }

    private static PdfPTable cards(Sheet sheet, BaseFont bold, BaseFont regular, Font white, Font body, Font small, DateTimeFormatter date) throws Exception {
        PdfPTable grid = new PdfPTable(2);
        grid.setWidthPercentage(100);
        grid.setSpacingBefore(8);
        grid.setSpacingAfter(8);
        for (CaptainRank rank : CaptainRank.values()) {
            Slot slot = sheet.captains().stream().filter(item -> item.rank() == rank).findFirst().orElse(null);
            grid.addCell(card(sheet.lang(), rank, slot, bold, regular, white, body, small, date));
        }
        return grid;
    }

    private static PdfPCell card(String lang, CaptainRank rank, Slot slot, BaseFont bold, BaseFont regular, Font white, Font body, Font small, DateTimeFormatter date) throws Exception {
        PdfPTable card = new PdfPTable(1);
        card.setWidthPercentage(100);
        PdfPCell banner = new PdfPCell(new Phrase(rank(lang, rank), white));
        banner.setBackgroundColor(rank == CaptainRank.CAPTAIN ? RED : GRAY);
        banner.setBorder(Rectangle.NO_BORDER);
        banner.setPadding(7);
        card.addCell(banner);
        PdfPTable bodyTable = new PdfPTable(new float[]{58, 180});
        bodyTable.setWidthPercentage(100);
        PdfPCell photo = new PdfPCell(portrait(slot == null ? null : slot.photo(), slot == null ? rank(lang, rank) : slot.fullName()), true);
        photo.setBorder(Rectangle.NO_BORDER);
        photo.setPadding(8);
        bodyTable.addCell(photo);
        PdfPCell facts = new PdfPCell();
        facts.setBorder(Rectangle.NO_BORDER);
        facts.setPadding(8);
        if (slot == null || slot.fullName() == null || slot.fullName().isBlank()) {
            facts.addElement(new Paragraph(text(lang, "free"), new Font(bold, 12, Font.NORMAL, INK)));
        } else {
            facts.addElement(new Paragraph(slot.fullName(), new Font(bold, 11, Font.NORMAL, INK)));
            if (slot.knownAs() != null && !slot.knownAs().isBlank()) {
                facts.addElement(new Paragraph(slot.knownAs(), small));
            }
            String meta = "#" + (slot.jersey() == null ? "–" : slot.jersey()) + "  ·  " + blank(slot.position());
            if (slot.age() != null) {
                meta += "  ·  " + slot.age();
            }
            facts.addElement(new Paragraph(meta, body));
            String since = slot.since() == null ? "–" : date.format(slot.since());
            facts.addElement(new Paragraph(text(lang, "since") + " " + since, new Font(regular, 8, Font.NORMAL, RED)));
        }
        bodyTable.addCell(facts);
        PdfPCell wrap = new PdfPCell(bodyTable);
        wrap.setBorder(Rectangle.NO_BORDER);
        card.addCell(wrap);
        PdfPCell shell = new PdfPCell(card);
        shell.setPadding(4);
        shell.setBorderColor(LINE);
        shell.setBorderWidth(0.6f);
        return shell;
    }

    private static PdfPTable summary(Sheet sheet, Font head, Font body, DateTimeFormatter date) {
        PdfPTable table = new PdfPTable(new float[]{90, 160, 50, 120, 80});
        table.setWidthPercentage(100);
        table.setSpacingBefore(6);
        table.setHeaderRows(1);
        for (String key : List.of("rank", "player", "jersey", "position", "since")) {
            table.addCell(head(text(sheet.lang(), key), head));
        }
        int index = 0;
        for (CaptainRank rank : CaptainRank.values()) {
            Slot slot = sheet.captains().stream().filter(item -> item.rank() == rank).findFirst().orElse(null);
            Color bg = index++ % 2 == 0 ? Color.WHITE : ZEBR;
            table.addCell(cell(rank(sheet.lang(), rank), body, bg));
            table.addCell(cell(slot == null ? text(sheet.lang(), "free") : slot.fullName(), body, bg));
            table.addCell(cell(slot == null || slot.jersey() == null ? "–" : String.valueOf(slot.jersey()), body, bg));
            table.addCell(cell(slot == null ? "–" : blank(slot.position()), body, bg));
            table.addCell(cell(slot == null || slot.since() == null ? "–" : date.format(slot.since()), body, bg));
        }
        return table;
    }

    private static PdfPTable history(Sheet sheet, Font head, Font body, DateTimeFormatter date) {
        PdfPTable table = new PdfPTable(new float[]{130, 90, 70, 70, 80, 80});
        table.setWidthPercentage(100);
        table.setSpacingBefore(6);
        table.setHeaderRows(1);
        table.setSplitRows(false);
        for (String key : List.of("player", "rank", "start", "end", "duration", "by")) {
            table.addCell(head(text(sheet.lang(), key), head));
        }
        if (sheet.history() == null || sheet.history().isEmpty()) {
            PdfPCell empty = cell(text(sheet.lang(), "none"), body, Color.WHITE);
            empty.setColspan(6);
            table.addCell(empty);
            return table;
        }
        int index = 0;
        for (Row row : sheet.history()) {
            Color bg = index++ % 2 == 0 ? Color.WHITE : ZEBR;
            table.addCell(cell(row.player(), body, bg));
            table.addCell(cell(rank(sheet.lang(), row.rank()), body, bg));
            table.addCell(cell(row.start() == null ? "–" : date.format(row.start()), body, bg));
            table.addCell(cell(row.end() == null ? text(sheet.lang(), "current") : date.format(row.end()), body, bg));
            table.addCell(cell(CaptainSpan.format(row.start(), row.end(), sheet.generatedAt(), sheet.lang()), body, bg));
            table.addCell(cell(blank(row.changedBy()), body, bg));
        }
        return table;
    }

    private static Image portrait(byte[] photo, String name) throws Exception {
        BufferedImage canvas = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = canvas.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setColor(new Color(0xA3, 0x28, 0x3A));
        graphics.fillOval(0, 0, 128, 128);
        BufferedImage source = read(photo);
        if (source != null) {
            graphics.setClip(new Ellipse2D.Float(3, 3, 122, 122));
            int side = Math.min(source.getWidth(), source.getHeight());
            int x = Math.max(0, (source.getWidth() - side) / 2);
            int y = Math.max(0, (source.getHeight() - side) / 2);
            graphics.drawImage(source, 3, 3, 125, 125, x, y, x + side, y + side, null);
        } else {
            graphics.setColor(Color.WHITE);
            graphics.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 42));
            String letters = initials(name);
            int width = graphics.getFontMetrics().stringWidth(letters);
            graphics.drawString(letters, (128 - width) / 2, 78);
        }
        graphics.setClip(null);
        graphics.setStroke(new BasicStroke(4f));
        graphics.setColor(Color.WHITE);
        graphics.drawOval(2, 2, 123, 123);
        graphics.dispose();
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(canvas, "png", png);
        Image image = Image.getInstance(png.toByteArray());
        image.scaleAbsolute(52, 52);
        return image;
    }

    private static BufferedImage read(byte[] photo) {
        if (photo == null || photo.length == 0) {
            return null;
        }
        try {
            return ImageIO.read(new ByteArrayInputStream(photo));
        } catch (Exception ex) {
            return null;
        }
    }

    private static String initials(String name) {
        if (name == null || name.isBlank()) {
            return "VF";
        }
        StringBuilder letters = new StringBuilder();
        for (String part : name.trim().split("\\s+")) {
            if (!part.isBlank()) {
                letters.append(Character.toUpperCase(part.charAt(0)));
            }
            if (letters.length() == 2) {
                break;
            }
        }
        return letters.isEmpty() ? "VF" : letters.toString();
    }

    private static PdfPCell head(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(RED);
        cell.setPadding(6);
        cell.setBorderColor(RED);
        return cell;
    }

    private static PdfPCell cell(String text, Font font, Color background) {
        PdfPCell cell = new PdfPCell(new Phrase(text == null || text.isBlank() ? "–" : text, font));
        cell.setBackgroundColor(background);
        cell.setPadding(6);
        cell.setBorderColor(LINE);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    private static BaseFont font(String file) throws Exception {
        try (var in = CaptainPdf.class.getResourceAsStream("/fonts/" + file)) {
            if (in == null) {
                throw new IllegalStateException("Missing font " + file);
            }
            byte[] bytes = in.readAllBytes();
            return BaseFont.createFont(file, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, bytes, null);
        }
    }

    private static String blank(String value) {
        return value == null || value.isBlank() ? "–" : value;
    }

    private static Locale locale(String lang) {
        if ("fr".equals(lang)) {
            return Locale.FRANCE;
        }
        if ("rw".equals(lang)) {
            return Locale.forLanguageTag("rw");
        }
        return Locale.UK;
    }

    static String rank(String lang, CaptainRank rank) {
        boolean fr = "fr".equals(lang);
        boolean rw = "rw".equals(lang);
        if (rank == null) {
            return "–";
        }
        return switch (rank) {
            case CAPTAIN -> fr ? "Capitaine" : rw ? "Kapiteni" : "Captain";
            case VICE_CAPTAIN -> fr ? "Vice-capitaine" : rw ? "Visi kapiteni" : "Vice-captain";
            case THIRD_CAPTAIN -> fr ? "Troisième capitaine" : rw ? "Kapiteni wa gatatu" : "Third captain";
            case FOURTH_CAPTAIN -> fr ? "Quatrième capitaine" : rw ? "Kapiteni wa kane" : "Fourth captain";
        };
    }

    private static String text(String lang, String key) {
        boolean fr = "fr".equals(lang);
        boolean rw = "rw".equals(lang);
        return switch (key) {
            case "title" -> fr ? "Capitaines du club" : rw ? "Abakapiteni b'ikipe" : "Club Captains";
            case "captains" -> fr ? "Capitaines" : rw ? "Abakapiteni" : "Captains";
            case "summary" -> fr ? "Résumé" : rw ? "Incamake" : "Leadership summary";
            case "history" -> fr ? "Historique" : rw ? "Amateka" : "History";
            case "rank" -> fr ? "Rang" : rw ? "Urwego" : "Rank";
            case "player" -> fr ? "Joueur" : rw ? "Umukinnyi" : "Player";
            case "jersey" -> fr ? "Numéro" : rw ? "Nimero" : "Jersey";
            case "position" -> fr ? "Poste" : rw ? "Umwanya" : "Position";
            case "since" -> fr ? "Depuis" : rw ? "Kuva" : "Since";
            case "start" -> fr ? "Début" : rw ? "Itangira" : "Start";
            case "end" -> fr ? "Fin" : rw ? "Irangira" : "End";
            case "duration" -> fr ? "Durée" : rw ? "Igihe" : "Duration";
            case "by" -> fr ? "Modifié par" : rw ? "Byahinduwe na" : "Changed by";
            case "current" -> fr ? "En cours" : rw ? "Ubu" : "Current";
            case "free" -> fr ? "Libre" : rw ? "Yubusa" : "Free";
            case "none" -> fr ? "Aucun changement de capitanat" : rw ? "Nta mpinduka z'ubukapiteni" : "No captaincy changes yet";
            case "confidential" -> fr ? "Confidentiel : usage interne" : rw ? "Ibanga: bikoreshwa imbere" : "Confidential: internal use";
            case "page" -> fr ? "Page" : rw ? "Paji" : "Page";
            case "of" -> fr ? "de" : rw ? "kuri" : "of";
            default -> key;
        };
    }

    private static final class Brand extends PdfPageEventHelper {
        private final Sheet sheet;
        private final BaseFont font;
        private final DateTimeFormatter date;
        private PdfTemplate total;

        private Brand(Sheet sheet, BaseFont font, DateTimeFormatter date, DateTimeFormatter stamp) {
            this.sheet = sheet;
            this.font = font;
            this.date = date;
        }

        @Override
        public void onOpenDocument(PdfWriter writer, Document document) {
            total = writer.getDirectContent().createTemplate(24, 12);
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            if (writer.getPageNumber() == 1 && sheet.logo() != null && sheet.logo().length > 0) {
                try {
                    Image mark = Image.getInstance(sheet.logo());
                    mark.scaleToFit(220, 220);
                    mark.setAbsolutePosition((document.getPageSize().getWidth() - mark.getScaledWidth()) / 2, 280);
                    PdfContentByte under = writer.getDirectContentUnder();
                    under.saveState();
                    PdfGState state = new PdfGState();
                    state.setFillOpacity(0.06f);
                    under.setGState(state);
                    under.addImage(mark);
                    under.restoreState();
                } catch (Exception ignored) {
                    // A missing logo must not stop the sheet.
                }
            }
            PdfContentByte canvas = writer.getDirectContent();
            float y = 24;
            Font foot = new Font(font, 8, Font.NORMAL, MUTED);
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, new Phrase("Viking FC", foot), document.left(), y, 0);
            ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER, new Phrase(text(sheet.lang(), "confidential"), foot), document.getPageSize().getWidth() / 2, y, 0);
            String prefix = text(sheet.lang(), "page") + " " + writer.getPageNumber() + " " + text(sheet.lang(), "of") + " ";
            float width = font.getWidthPoint(prefix, 8);
            ColumnText.showTextAligned(canvas, Element.ALIGN_RIGHT, new Phrase(prefix, foot), document.right() - 18, y, 0);
            canvas.addTemplate(total, document.right() - 16, y);
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, new Phrase(blank(sheet.generatedBy()), foot), document.left(), 12, 0);
        }

        @Override
        public void onCloseDocument(PdfWriter writer, Document document) {
            total.beginText();
            total.setFontAndSize(font, 8);
            total.setColorFill(MUTED);
            int pages = Math.max(1, writer.getPageNumber() - 1);
            total.showText(String.valueOf(pages));
            total.endText();
        }
    }
}
