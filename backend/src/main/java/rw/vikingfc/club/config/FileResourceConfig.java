package rw.vikingfc.club.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class FileResourceConfig implements WebMvcConfigurer {

    private final Path root;

    public FileResourceConfig(AppProperties properties) {
        this.root = Path.of(properties.uploads().dir()).toAbsolutePath().normalize();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/files/**")
                .addResourceLocations(root.toUri().toString())
                .setCachePeriod(86_400);
    }
}
