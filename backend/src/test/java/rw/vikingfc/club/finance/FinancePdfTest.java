package rw.vikingfc.club.finance;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinancePdfTest {

    @Test
    void salariesShowMoneySeparatorsSubtotalAndPageNumber() throws Exception {
        byte[] pdf = FinancePdf.render("SALARIES", "PLAYERS", List.of(
                new String[]{"Person", "Seasonal pay", "Paid to date"},
                new String[]{"Enock Musinguzi", "240000", "78000"}
        ), new byte[0], "Victor Jacker");
        String text = text(pdf);
        assertTrue(text.contains("Salaries report"));
        assertTrue(text.contains("Players"));
        assertTrue(text.contains("RWF 240,000"));
        assertTrue(text.contains("RWF 78,000"));
        assertTrue(text.contains("Subtotal"));
        assertTrue(text.contains("Page 1"));
        assertTrue(text.contains("Confidential: contains salary data"));
        assertFalse(text.contains("FULL"));
        assertFalse(text.contains("240000"));
    }

    @Test
    void emptyBonusesExplainWhy() throws Exception {
        byte[] pdf = FinancePdf.render("BONUSES", "ALL", List.<String[]>of(new String[]{"Person", "Amount"}), new byte[0], "Victor Jacker");
        String text = text(pdf);
        assertTrue(text.contains("Bonuses report"));
        assertTrue(text.contains("All people"));
        assertTrue(text.contains("No bonuses recorded for this period"));
        assertFalse(text.contains("ALL"));
    }

    private static String text(byte[] pdf) throws Exception {
        PdfReader reader = new PdfReader(new java.io.ByteArrayInputStream(pdf));
        try {
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            StringBuilder out = new StringBuilder();
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                out.append(extractor.getTextFromPage(page)).append('\n');
            }
            return out.toString();
        } finally {
            reader.close();
        }
    }
}
