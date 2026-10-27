package com.tuannt.api.configs;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Created by tuannt7 on 27/10/2026
 * <p>
 * Served at /v3/api-docs (JSON) and /docs (Swagger UI). Declaring the bearer scheme lets the UI
 * send a token from POST /v1/auth/login to the endpoints that need one.
 */
@Configuration
public class OpenApiConfig {
    private static final String BEARER = "bearer";

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("nttuan.dev API")
                        .version("1")
                        .description("Backend for https://nttuan.dev. Errors are always { errorCode, errorMessage }; "
                                + "timestamps are epoch millis."))
                .servers(List.of(new Server().url("https://api.nttuan.dev"), new Server().url("http://localhost:8089")))
                .components(new Components().addSecuritySchemes(BEARER,
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }
}
