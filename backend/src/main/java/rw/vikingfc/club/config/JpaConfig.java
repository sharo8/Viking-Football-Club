package rw.vikingfc.club.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import rw.vikingfc.club.common.util.SecurityUtils;

import java.time.Clock;
import java.time.ZoneId;
import java.util.Optional;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaConfig {

    public static final ZoneId KIGALI = ZoneId.of("Africa/Kigali");

    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> Optional.of(SecurityUtils.email());
    }

    @Bean
    public Clock clock() {
        return Clock.system(KIGALI);
    }
}
