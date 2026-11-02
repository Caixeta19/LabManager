package com.example.lab_manager.config;

import com.example.lab_manager.dto.error.ErrorResponseDTO;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI (RF23): esquema {@code bearerAuth} aplicado a toda a API (as rotas públicas o removem com
 * {@code @SecurityRequirements}) e o corpo padrão de erro anexado a toda resposta 4xx/5xx documentada.
 */
@Configuration
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI labManagerOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("LabManager API")
                        .version("v1")
                        .description("Reserva de laboratórios, check-in via QR Code e empréstimo de equipamentos."))
                .components(new Components().addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Token obtido em POST /auth/login")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }

    @Bean
    public OpenApiCustomizer errorResponseSchemaCustomizer() {
        return openApi -> {
            ModelConverters.getInstance().readAll(ErrorResponseDTO.class)
                    .forEach((name, schema) -> openApi.getComponents().addSchemas(name, schema));
            Schema<?> errorRef = new Schema<>().$ref("#/components/schemas/ErrorResponseDTO");

            if (openApi.getPaths() == null) {
                return;
            }
            openApi.getPaths().values().forEach(path -> path.readOperations().forEach(operation -> {
                if (operation.getResponses() == null) {
                    return;
                }
                operation.getResponses().forEach((code, response) -> {
                    if ((code.startsWith("4") || code.startsWith("5")) && response.getContent() == null) {
                        response.setContent(new Content().addMediaType(
                                org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
                                new MediaType().schema(errorRef)));
                    }
                });
            }));
        };
    }
}
