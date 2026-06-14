package az.fitnest.catalog.configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

import java.time.Clock;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Value(value = "${springdoc.server-url:}")
    private String serverUrl;

    @Bean
    public OpenAPI customOpenAPI() {
        OpenAPI openAPI = new OpenAPI()
                .info(new Info()
                        .title("Catalog Backend API")
                        .version("1.0.0")
                        .description("Fitnest Catalog Backend endpoints")
                        .contact(new Contact()
                                .name("FitNest Team")
                                .email("support@fitnest.az")))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Enter your JWT token")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));

        if (this.serverUrl != null && !this.serverUrl.isEmpty()) {
            openAPI.servers(List.of(new Server().url(this.serverUrl).description("API Server")));
        }
        return openAPI;
    }

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
