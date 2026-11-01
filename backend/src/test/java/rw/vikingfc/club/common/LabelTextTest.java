package rw.vikingfc.club.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LabelTextTest {

    @Test
    void usesTheAppLanguageWhenThatTranslationExists() {
        assertEquals("Analyste", LabelText.pick("fr", "en", "Analyst", "Analyst", "Analyste", "Umusuzuma"));
    }

    @Test
    void fallsBackToTheLanguageTheNameWasWrittenIn() {
        assertEquals("Analyste", LabelText.pick("en", "fr", "Analyste", "", "", "Umusuzuma"));
        assertEquals("Analyste", LabelText.pick("rw", "fr", "Analyste", "", "", ""));
    }

    @Test
    void neverReturnsBlankWhenAMainNameExists() {
        assertEquals("Analyst", LabelText.pick("rw", "en", "Analyst", "Analyst", null, "  "));
    }
}
