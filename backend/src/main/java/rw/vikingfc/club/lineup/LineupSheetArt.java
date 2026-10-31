package rw.vikingfc.club.lineup;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;

public final class LineupSheetArt {

    public record Token(String name, String jersey, int x, int y) {
    }

    private LineupSheetArt() {
    }

    public static byte[] png(String title, String detail, List<Token> tokens, byte[] logo) {
        BufferedImage image = new BufferedImage(1080, 1350, BufferedImage.TYPE_INT_RGB);
        Graphics2D pen = image.createGraphics();
        pen.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        pen.setColor(new Color(0x16, 0x16, 0x16));
        pen.fillRect(0, 0, 1080, 1350);
        pen.setColor(new Color(0xA3, 0x28, 0x3A));
        pen.fillRect(0, 0, 1080, 150);
        pen.setColor(Color.WHITE);
        pen.setFont(pen.getFont().deriveFont(42f));
        pen.drawString("VIKING FC", 40, 70);
        pen.setFont(pen.getFont().deriveFont(28f));
        pen.drawString(fit(title, 42), 40, 115);
        if (logo != null && logo.length > 0) {
            try {
                BufferedImage crest = ImageIO.read(new java.io.ByteArrayInputStream(logo));
                if (crest != null) {
                    pen.drawImage(crest, 920, 25, 120, 110, null);
                }
            } catch (Exception ignored) {
                // A missing crest still leaves the graphic usable.
            }
        }
        int pitchX = 80;
        int pitchY = 190;
        int pitchW = 920;
        int pitchH = 1000;
        pen.setColor(new Color(0x1F, 0x6B, 0x3A));
        pen.fillRoundRect(pitchX, pitchY, pitchW, pitchH, 24, 24);
        pen.setColor(new Color(0x25, 0x7A, 0x42));
        for (int band = 0; band < 10; band += 2) {
            pen.fillRect(pitchX, pitchY + band * pitchH / 10, pitchW, pitchH / 10);
        }
        pen.setColor(Color.WHITE);
        pen.setStroke(new BasicStroke(3f));
        pen.drawRoundRect(pitchX + 16, pitchY + 16, pitchW - 32, pitchH - 32, 8, 8);
        pen.drawOval(pitchX + pitchW / 2 - 80, pitchY + pitchH / 2 - 80, 160, 160);
        for (Token token : tokens) {
            int cx = pitchX + token.x() * pitchW / 100;
            int cy = pitchY + token.y() * pitchH / 100;
            pen.setColor(new Color(0xA3, 0x28, 0x3A));
            pen.fillOval(cx - 36, cy - 36, 72, 72);
            pen.setColor(Color.WHITE);
            pen.setFont(pen.getFont().deriveFont(22f));
            String jersey = token.jersey() == null ? "" : token.jersey();
            pen.drawString(jersey, cx - 8 * jersey.length(), cy + 8);
            pen.setFont(pen.getFont().deriveFont(18f));
            pen.drawString(fit(token.name(), 16), cx - 40, cy + 56);
        }
        pen.setColor(Color.WHITE);
        pen.setFont(pen.getFont().deriveFont(22f));
        pen.drawString(fit(detail, 70), 40, 1280);
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
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(document, out);
            document.open();
            Font title = new Font(Font.HELVETICA, 18, Font.BOLD, new Color(0xA3, 0x28, 0x3A));
            document.add(new Paragraph("VIKING FC", title));
            document.add(new Paragraph("Match sheet · " + opponent));
            document.add(new Paragraph(when + " · " + venue + " · " + formation));
            PdfPTable table = new PdfPTable(new float[] {1, 4});
            table.setWidthPercentage(100);
            table.setSpacingBefore(16);
            header(table, "No");
            header(table, "Player");
            for (Token token : tokens) {
                table.addCell(new Phrase(token.jersey() == null ? "" : token.jersey()));
                table.addCell(new Phrase(token.name() == null ? "" : token.name()));
            }
            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("The match sheet could not be built", ex);
        }
    }

    private static void header(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text));
        cell.setBackgroundColor(new Color(0xA3, 0x28, 0x3A));
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell.setPadding(6);
        table.addCell(cell);
    }

    private static String fit(String value, int max) {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, max);
    }
}
