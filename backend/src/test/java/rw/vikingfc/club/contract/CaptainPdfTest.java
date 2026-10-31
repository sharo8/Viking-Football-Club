package rw.vikingfc.club.contract;

import com.lowagie.text.pdf.PdfReader;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CaptainPdfTest {

    @Test
    void sheetRendersWithNoCaptainsTwoCaptainsAndFour() throws Exception {
        byte[] logo = CaptainPdf.class.getResourceAsStream("/brand/logo.png").readAllBytes();
        byte[] empty = CaptainPdf.render(sheet("en", List.of(), List.of(), logo));
        byte[] two = CaptainPdf.render(sheet("fr", List.of(
                slot(CaptainRank.CAPTAIN, "Mugisha Éric", null),
                slot(CaptainRank.VICE_CAPTAIN, "Ntwali", new byte[]{1, 2, 3})), List.of(), logo));
        byte[] four = CaptainPdf.render(sheet("en", List.of(
                slot(CaptainRank.CAPTAIN, "A Very Long Captain Name That Must Wrap Inside The Card", photo()),
                slot(CaptainRank.VICE_CAPTAIN, "Vice", null),
                slot(CaptainRank.THIRD_CAPTAIN, "Third", null),
                slot(CaptainRank.FOURTH_CAPTAIN, "Fourth", null)),
                List.of(new CaptainPdf.Row("Mugisha Éric", CaptainRank.CAPTAIN, Instant.parse("2026-09-01T08:00:00Z"), null, "Victor Jacker")),
                logo));
        assertTrue(empty[0] == '%' && empty[1] == 'P');
        assertEquals(1, pages(empty));
        assertEquals(1, pages(two));
        assertTrue(pages(four) >= 1);
        assertTrue(four.length > 2000 && two.length > 2000);
        assertEquals("Viking_FC_Captains_2026-2027.pdf", CaptainPdf.fileName("2026/2027"));
        assertEquals("Viking_FC_Captains_2026-2027.pdf", CaptainPdf.fileName("Season 2026/2027"));
    }

    @Test
    void durationAvoidsZeroDays() {
        Instant start = Instant.parse("2026-10-25T10:00:00Z");
        assertEquals("Same day", CaptainSpan.format(start, start.plusSeconds(3600), start.plusSeconds(7200), "en"));
        assertEquals("3 days", CaptainSpan.format(start, start.plusSeconds(3 * 86400L), start, "en"));
        assertEquals("2 weeks", CaptainSpan.format(start, start.plusSeconds(14 * 86400L), start, "en"));
        Instant month = java.time.LocalDate.of(2026, 10, 1).atStartOfDay(CaptainSpan.KIGALI).toInstant();
        Instant later = java.time.LocalDate.of(2026, 11, 5).atStartOfDay(CaptainSpan.KIGALI).toInstant();
        assertEquals("1 mois 4 jours", CaptainSpan.format(month, later, later, "fr"));
    }

    private static CaptainPdf.Sheet sheet(String lang, List<CaptainPdf.Slot> captains, List<CaptainPdf.Row> history, byte[] logo) {
        return new CaptainPdf.Sheet(lang, "2026/2027", Instant.parse("2026-10-25T12:00:00Z"), "Victor Jacker", captains, history, logo);
    }

    private static CaptainPdf.Slot slot(CaptainRank rank, String name, byte[] photo) {
        return new CaptainPdf.Slot(rank, name, "Known", 9, "Gardien", rank == CaptainRank.VICE_CAPTAIN ? null : 24,
                Instant.parse("2026-09-28T08:00:00Z"), photo);
    }

    private static byte[] photo() {
        return new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47};
    }

    private static int pages(byte[] pdf) throws Exception {
        PdfReader reader = new PdfReader(pdf);
        int pages = reader.getNumberOfPages();
        reader.close();
        return pages;
    }
}
