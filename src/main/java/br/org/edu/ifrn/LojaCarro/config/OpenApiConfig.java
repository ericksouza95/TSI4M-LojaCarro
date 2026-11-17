package br.org.edu.ifrn.LojaCarro.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String ESQUEMA_JWT = "bearerAuth";

    // No Swagger UI, use o botão "Authorize" e cole o token devolvido por /api/auth/login
    @Bean
    public OpenAPI lojaCarroOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Loja de Carros - API")
                        .description("Catálogo de veículos, usuários e agendamento de test drives")
                        .version("1.0"))
                .components(new Components().addSecuritySchemes(ESQUEMA_JWT, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_JWT));
    }
}
