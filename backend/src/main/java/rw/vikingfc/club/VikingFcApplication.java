package rw.vikingfc.club;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import rw.vikingfc.club.config.AppProperties;

import java.util.TimeZone;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@ConfigurationPropertiesScan(basePackageClasses = AppProperties.class)
public class VikingFcApplication {

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("Africa/Kigali"));
        SpringApplication.run(VikingFcApplication.class, args);
    }
}
