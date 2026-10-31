package rw.vikingfc.club;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DemoPasswordTest {

    @Test
    void demoPasswordMatchesStoredHash() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
        assertTrue(encoder.matches(
                "ChangeMe@2026",
                "$2b$12$ChKg4MblupIMc4qjBScQBeniyT6Gb.YO6YaJglxXZHBVt.XhpDftq"));
    }
}
