package rw.vikingfc.club.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import rw.vikingfc.club.config.AppProperties;
import rw.vikingfc.club.user.User;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;

@Service
public class JwtService {

    private final AppProperties properties;
    private final Environment environment;
    private SecretKey key;

    public JwtService(AppProperties properties, Environment environment) {
        this.properties = properties;
        this.environment = environment;
    }

    @PostConstruct
    void init() {
        byte[] secret = properties.jwt().secret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 bytes");
        }
        boolean prod = Arrays.asList(environment.getActiveProfiles()).contains("prod");
        if (prod && properties.jwt().secret().contains("dev-only")) {
            throw new IllegalStateException("Set a private JWT_SECRET before starting the prod profile");
        }
        this.key = Keys.hmacShaKeyFor(secret);
    }

    public String accessToken(User user) {
        Instant now = Instant.now();
        Instant exp = now.plus(properties.jwt().accessMinutes(), ChronoUnit.MINUTES);
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("uid", user.getId())
                .claim("role", user.getRole().name())
                .claim("executive", user.getRole().isExecutive())
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key)
                .compact();
    }

    public String subject(String token) {
        return claims(token).getSubject();
    }

    public Claims claims(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
