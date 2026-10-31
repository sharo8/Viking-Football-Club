package rw.vikingfc.club.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI vikingOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Viking FC Club API")
                        .version("1.0.0")
                        .description("Club management for Viking FC, 3rd Division, Kigali. Season 2026/2027. "
                                + "Motto: ONE CLUB | ONE FAMILY | ONE MISSION. Money is RWF."))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme()
                                .name("bearerAuth")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
