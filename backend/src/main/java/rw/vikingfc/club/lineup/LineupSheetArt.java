package rw.vikingfc.club.lineup;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
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
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

public final class LineupSheetArt {

    public record Token(String name, String jersey, int x, int y, byte[] photo, boolean captain, boolean keeper) {
        public Token(String name, String jersey, int x, int y) {
            this(name, jersey, x, y, null, false, false);
        }

        public Token(String name, String jersey, int x, int y, byte[] photo) {
            this(name, jersey, x, y, photo, false, false);
        }
    }

    private LineupSheetArt() {
    }

    public static byte[] png(String title, String detail, List<Token> tokens, byte[] logo) {
        return png(title, detail, tokens, logo, List.of());
    }

    public static byte[] png(String title, String detail, List<Token> tokens, byte[] logo, List<String> bench) {
        return png(title, detail, "4-3-3", tokens, bench.stream().map(name -> new Token(name, "", 0, 0)).toList(), logo, null, "photo", 1080, 1350);
    }

    public static byte[] png(String title, String detail, String formation, List<Token> tokens, List<Token> bench,
                             byte[] logo, byte[] opponent, String template, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D pen = image.createGraphics();
        pen.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        pen.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        boolean kit = "kit".equals(template);
        boolean classic = "classic".equals(template);
        pen.setColor(classic ? new Color(0xF7, 0xF4, 0xEF) : kit ? new Color(0x07, 0x18, 0x32) : new Color(0x12, 0x12, 0x14));
        pen.fillRect(0, 0, width, height);
        pen.setColor(new Color(0xA3, 0x28, 0x3A));
        pen.fillRect(0, 0, width, 148);
        drawCrest(pen, logo, 36, 24, 100, 100);
        drawCrest(pen, opponent, width - 136, 24, 100, 100);
        pen.setColor(Color.WHITE);
        pen.setFont(pen.getFont().deriveFont(java.awt.Font.BOLD, 28f));
        pen.drawString("MATCHDAY LINEUP", 156, 62);
        pen.setFont(pen.getFont().deriveFont(java.awt.Font.PLAIN, 22f));
        pen.drawString(fit(title, 36), 156, 98);
        pen.setFont(pen.getFont().deriveFont(16f));
        pen.drawString(fit(detail, 64), 156, 126);

        int pitchX = 70;
        int pitchY = 180;
        int pitchW = width - 140;
        int pitchH = height - 390;
        drawPitch(pen, pitchX, pitchY, pitchW, pitchH, kit);
        int diameter = Math.max(64, Math.min(108, pitchW / 10));
        for (Token token : tokens) {
            int cx = pitchX + token.x() * pitchW / 100;
            int cy = pitchY + token.y() * pitchH / 100;
            if (kit) {
                drawShirt(pen, cx, cy, diameter, token.keeper());
            } else {
                drawFace(pen, cx, cy - 8, diameter, token);
            }
            drawPlate(pen, cx, cy + diameter / 2 + 6, fit(shortName(token.name()), 14), classic);
            if (token.captain()) {
                pen.setColor(new Color(0xC4, 0x84, 0x1D));
                pen.fillOval(cx - diameter / 2 - 4, cy - diameter / 2 - 8, 22, 22);
                pen.setColor(Color.WHITE);
                pen.setFont(pen.getFont().deriveFont(java.awt.Font.BOLD, 12f));
                pen.drawString("C", cx - diameter / 2 + 2, cy - diameter / 2 + 8);
            }
        }
        pen.setColor(new Color(0xA3, 0x28, 0x3A));
        pen.fillRoundRect(pitchX + 16, pitchY + pitchH - 48, 92, 32, 16, 16);
        pen.setColor(Color.WHITE);
        pen.setFont(pen.getFont().deriveFont(java.awt.Font.BOLD, 16f));
        pen.drawString(formation == null ? "" : formation, pitchX + 28, pitchY + pitchH - 27);

        int benchY = pitchY + pitchH + 28;
        pen.setColor(classic ? new Color(0x22, 0x22, 0x22) : Color.WHITE);
        pen.setFont(pen.getFont().deriveFont(java.awt.Font.BOLD, 16f));
        pen.drawString("BENCH", 70, benchY);
        int index = 0;
        for (Token token : bench) {
            int cx = 90 + (index % 9) * ((width - 160) / 9);
            int cy = benchY + 36 + (index / 9) * 70;
            drawFace(pen, cx, cy, 44, token);
            pen.setColor(classic ? new Color(0x22, 0x22, 0x22) : Color.WHITE);
            pen.setFont(pen.getFont().deriveFont(12f));
            pen.drawString(fit(shortName(token.name()), 10), cx - 28, cy + 36);
            index++;
        }
        pen.setFont(pen.getFont().deriveFont(14f));
        pen.drawString("Viking FC  ·  Together we rise", 70, height - 28);
        pen.dispose();
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("The matchday graphic could not be built", ex);
        }
    }

    public static byte[] pdf(String opponent, String when, String venue, String formation, List<Token> tokens) {
        return pdf(opponent, when, venue, formation, tokens, List.of(), "en");
    }

    public static byte[] pdf(String opponent, String when, String venue, String formation, List<Token> tokens, List<String> bench, String lang) {
        List<Token> benchTokens = bench == null ? List.of() : bench.stream().map(name -> new Token(name, "", 0, 0)).toList();
        return pdf(opponent, when, venue, "", formation, null, tokens, benchTokens, lang, "Viking FC");
    }

    public static byte[] pdf(String opponent, String when, String venue, String competition, String formation, Integer squadSize,
                             List<Token> tokens, List<Token> bench, String lang, String generatedBy) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 36, 36, 86, 56);
            PdfWriter writer = PdfWriter.getInstance(document, out);
            Font title = face(16, true, Color.WHITE);
            Font text = face(10, false, new Color(0x22, 0x22, 0x22));
            Font head = face(9, true, Color.WHITE);
            Font small = face(8, false, new Color(0x55, 0x55, 0x55));
            document.open();
            PdfTemplate total = writer.getDirectContent().createTemplate(24, 12);
            String confidential = "fr".equals(lang) ? "Confidentiel — usage interne" : "rw".equals(lang) ? "Ibanga — ikoreshwa imbere" : "Confidential — internal use";
            String heading = "fr".equals(lang) ? "FICHE DE MATCH" : "rw".equals(lang) ? "IFISHI Y'UMUKINO" : "MATCH SHEET";
            writer.setPageEvent(new PdfPageEventHelper() {
                @Override
                public void onEndPage(PdfWriter pageWriter, Document page) {
                    PdfContentByte canvas = pageWriter.getDirectContent();
                    canvas.setColorFill(new Color(0xA3, 0x28, 0x3A));
                    canvas.rectangle(0, page.getPageSize().getHeight() - 64, page.getPageSize().getWidth(), 64);
                    canvas.fill();
                    ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, new Phrase("VIKING FC", title), 36, page.getPageSize().getHeight() - 38, 0);
                    ColumnText.showTextAligned(canvas, Element.ALIGN_RIGHT, new Phrase(heading, title), page.getPageSize().getWidth() - 36, page.getPageSize().getHeight() - 38, 0);
                    ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, new Phrase("Viking FC  ·  " + confidential, small), 36, 28, 0);
                    ColumnText.showTextAligned(canvas, Element.ALIGN_RIGHT,
                            new Phrase(pageWriter.getPageNumber() + " / ", small), page.getPageSize().getWidth() - 52, 28, 0);
                    canvas.addTemplate(total, page.getPageSize().getWidth() - 48, 26);
                }

                @Override
                public void onCloseDocument(PdfWriter pageWriter, Document page) {
                    ColumnText.showTextAligned(total, Element.ALIGN_LEFT, new Phrase(String.valueOf(pageWriter.getPageNumber() - 1), small), 0, 0, 0);
                }
            });
            byte[] logo = null;
            try (var stream = LineupSheetArt.class.getResourceAsStream("/brand/logo.png")) {
                if (stream != null) {
                    logo = stream.readAllBytes();
                }
            }
            if (logo != null) {
                Image crest = Image.getInstance(logo);
                crest.scaleToFit(42, 42);
                crest.setAbsolutePosition(36, PageSize.A4.getHeight() - 56);
                writer.getDirectContent().addImage(crest);
            }
            String side = "";
            document.add(new Paragraph("Viking FC  vs  " + opponent, face(16, true, new Color(0xA3, 0x28, 0x3A))));
            document.add(new Paragraph(join(competition, when, venue, formation, squadSize == null ? null : "Squad " + squadSize) + side, text));
            document.add(new Paragraph(generatedBy == null ? "" : generatedBy, small));

            float pitchW = 311f;
            float pitchH = 420f;
            float left = (PageSize.A4.getWidth() - pitchW) / 2f;
            float top = writer.getVerticalPosition(true) - 8;
            drawPdfPitch(writer.getDirectContent(), left, top - pitchH, pitchW, pitchH, tokens, text);
            Paragraph spacer = new Paragraph(" ");
            spacer.setSpacingBefore(pitchH + 12);
            document.add(spacer);

            String xi = "fr".equals(lang) ? "Onze de départ" : "rw".equals(lang) ? "Abatangira" : "Starting XI";
            String benchLabel = "fr".equals(lang) ? "Remplaçants" : "rw".equals(lang) ? "Abasimbura" : "Substitutes";
            document.add(table(xi, new String[] {"No.", "Player", "Slot"}, tokens, text, head));
            document.add(table(benchLabel, new String[] {"No.", "Player", "Slot"}, bench, text, head));

            PdfPTable signs = new PdfPTable(2);
            signs.setWidthPercentage(100);
            signs.setSpacingBefore(18);
            signs.addCell(sign("Head coach", text));
            signs.addCell(sign("General Manager", text));
            document.add(signs);
            document.close();
            return out.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("The match sheet could not be built", ex);
        }
    }

    private static void drawPdfPitch(PdfContentByte canvas, float x, float y, float w, float h, List<Token> tokens, Font font) {
        canvas.saveState();
        canvas.setColorFill(new Color(0x1F, 0x6B, 0x3A));
        canvas.roundRectangle(x, y, w, h, 8);
        canvas.fill();
        canvas.setColorStroke(Color.WHITE);
        canvas.setLineWidth(0.8f);
        canvas.rectangle(x + 10, y + 10, w - 20, h - 20);
        canvas.stroke();
        canvas.moveTo(x + 10, y + h / 2);
        canvas.lineTo(x + w - 10, y + h / 2);
        canvas.stroke();
        canvas.circle(x + w / 2, y + h / 2, 28);
        canvas.stroke();
        for (Token token : tokens) {
            float cx = x + token.x() * w / 100f;
            float cy = y + h - token.y() * h / 100f;
            canvas.setColorFill(token.keeper() ? new Color(0x1D, 0x4E, 0x89) : new Color(0xA3, 0x28, 0x3A));
            canvas.circle(cx, cy, 9);
            canvas.fill();
            ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER, new Phrase(fit(shortName(token.name()), 12), font), cx, cy - 18, 0);
        }
        canvas.restoreState();
    }

    private static PdfPTable table(String title, String[] headers, List<Token> rows, Font text, Font head) throws Exception {
        PdfPTable table = new PdfPTable(new float[] {1.1f, 4f, 1.4f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(12);
        table.setHeaderRows(2);
        PdfPCell banner = new PdfPCell(new Phrase(title, head));
        banner.setColspan(3);
        banner.setBackgroundColor(new Color(0xA3, 0x28, 0x3A));
        banner.setPadding(6);
        banner.setBorderColor(new Color(0xE6, 0xE6, 0xE6));
        table.addCell(banner);
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, head));
            cell.setBackgroundColor(new Color(0xA3, 0x28, 0x3A));
            cell.setPadding(5);
            cell.setBorderColor(new Color(0xE6, 0xE6, 0xE6));
            table.addCell(cell);
        }
        int index = 0;
        for (Token token : rows) {
            Color zebra = index++ % 2 == 0 ? new Color(0xFA, 0xF7, 0xF7) : Color.WHITE;
            cell(table, token.jersey() == null ? "" : token.jersey(), text, zebra);
            cell(table, (token.name() == null ? "" : token.name()) + (token.captain() ? "  (C)" : ""), text, zebra);
            cell(table, "", text, zebra);
        }
        return table;
    }

    private static void cell(PdfPTable table, String value, Font font, Color background) {
        PdfPCell cell = new PdfPCell(new Phrase(value, font));
        cell.setBackgroundColor(background);
        cell.setPadding(5);
        cell.setBorderColor(new Color(0xE4, 0xE4, 0xE4));
        table.addCell(cell);
    }

    private static PdfPCell sign(String label, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(label + "\n\n\n", font));
        cell.setPadding(10);
        cell.setFixedHeight(72);
        cell.setBorderColor(new Color(0xE4, 0xE4, 0xE4));
        return cell;
    }

    private static void drawPitch(Graphics2D pen, int x, int y, int w, int h, boolean kit) {
        pen.setColor(kit ? new Color(0x0E, 0x3B, 0x2E) : new Color(0x1F, 0x6B, 0x3A));
        pen.fill(new RoundRectangle2D.Float(x, y, w, h, 18, 18));
        if (!kit) {
            pen.setColor(new Color(0x25, 0x7A, 0x42));
            for (int band = 0; band < 8; band += 2) {
                pen.fillRect(x, y + band * h / 8, w, h / 8);
            }
        }
        pen.setColor(Color.WHITE);
        pen.setStroke(new BasicStroke(3f));
        pen.drawRoundRect(x + 16, y + 16, w - 32, h - 32, 8, 8);
        pen.drawLine(x + 16, y + h / 2, x + w - 16, y + h / 2);
        pen.drawOval(x + w / 2 - 70, y + h / 2 - 70, 140, 140);
        pen.drawRect(x + w / 2 - 110, y + 16, 220, 90);
        pen.drawRect(x + w / 2 - 110, y + h - 106, 220, 90);
    }

    private static void drawFace(Graphics2D pen, int cx, int cy, int diameter, Token token) {
        int left = cx - diameter / 2;
        int top = cy - diameter / 2;
        pen.setColor(new Color(0xA3, 0x28, 0x3A));
        pen.fillOval(left, top, diameter, diameter);
        if (token.photo() != null && token.photo().length > 0) {
            try {
                BufferedImage face = ImageIO.read(new ByteArrayInputStream(token.photo()));
                if (face != null) {
                    java.awt.Shape clip = pen.getClip();
                    pen.setClip(new Ellipse2D.Float(left + 2, top + 2, diameter - 4, diameter - 4));
                    pen.drawImage(face, left + 2, top + 2, diameter - 4, diameter - 4, null);
                    pen.setClip(clip);
                }
            } catch (Exception ignored) {
                initials(pen, cx, cy, token.name());
            }
        } else {
            initials(pen, cx, cy, token.name());
        }
        pen.setColor(new Color(0x11, 0x11, 0x11));
        pen.fillOval(cx + diameter / 2 - 22, cy - diameter / 2 - 2, 22, 22);
        pen.setColor(Color.WHITE);
        pen.setFont(pen.getFont().deriveFont(java.awt.Font.BOLD, 11f));
        String jersey = token.jersey() == null ? "" : token.jersey();
        pen.drawString(jersey, cx + diameter / 2 - 16, cy - diameter / 2 + 14);
    }

    private static void drawShirt(Graphics2D pen, int cx, int cy, int diameter, boolean keeper) {
        pen.setColor(keeper ? new Color(0x1D, 0x4E, 0x89) : new Color(0xA3, 0x28, 0x3A));
        pen.fillRoundRect(cx - diameter / 3, cy - diameter / 5, diameter * 2 / 3, diameter * 3 / 5, 10, 10);
        pen.fillRoundRect(cx - diameter / 2, cy - diameter / 8, diameter / 5, diameter / 4, 6, 6);
        pen.fillRoundRect(cx + diameter / 3, cy - diameter / 8, diameter / 5, diameter / 4, 6, 6);
    }

    private static void drawPlate(Graphics2D pen, int cx, int y, String name, boolean classic) {
        int width = Math.max(72, name.length() * 8 + 16);
        pen.setColor(new Color(0, 0, 0, 180));
        pen.fillRoundRect(cx - width / 2, y, width, 22, 11, 11);
        pen.setColor(Color.WHITE);
        pen.setFont(pen.getFont().deriveFont(java.awt.Font.BOLD, 12f));
        pen.drawString(name, cx - width / 2 + 8, y + 15);
    }

    private static void initials(Graphics2D pen, int cx, int cy, String name) {
        pen.setColor(Color.WHITE);
        pen.setFont(pen.getFont().deriveFont(java.awt.Font.BOLD, 16f));
        String letters = shortName(name);
        letters = letters.isEmpty() ? "?" : letters.substring(0, 1).toUpperCase();
        pen.drawString(letters, cx - 5, cy + 6);
    }

    private static void drawCrest(Graphics2D pen, byte[] bytes, int x, int y, int w, int h) {
        if (bytes == null || bytes.length == 0) {
            return;
        }
        try {
            BufferedImage crest = ImageIO.read(new ByteArrayInputStream(bytes));
            if (crest != null) {
                pen.drawImage(crest, x, y, w, h, null);
            }
        } catch (Exception ignored) {
            // The title still identifies the clubs when a crest cannot be read.
        }
    }

    private static String shortName(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }
        String trimmed = name.trim();
        int cut = trimmed.indexOf('·');
        if (cut > 0) {
            trimmed = trimmed.substring(0, cut).trim();
        }
        return trimmed.length() <= 14 ? trimmed : trimmed.substring(0, 14);
    }

    private static String join(String... parts) {
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part == null || part.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append("  ·  ");
            }
            builder.append(part);
        }
        return builder.toString();
    }

    private static Font face(float size, boolean bold, Color color) {
        try (var stream = LineupSheetArt.class.getResourceAsStream(bold ? "/fonts/Inter-SemiBold.ttf" : "/fonts/Inter-Regular.ttf")) {
            byte[] bytes = stream == null ? new byte[0] : stream.readAllBytes();
            if (bytes.length > 0) {
                BaseFont base = BaseFont.createFont(bold ? "Inter-SemiBold.ttf" : "Inter-Regular.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, bytes, null);
                return new Font(base, size, Font.NORMAL, color);
            }
        } catch (Exception ignored) {
            // Helvetica remains the fallback when the embedded font cannot be read.
        }
        return new Font(Font.HELVETICA, size, bold ? Font.BOLD : Font.NORMAL, color);
    }

    private static String fit(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
