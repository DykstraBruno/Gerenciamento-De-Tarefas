package com.brunodykstra.taskapi.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("Task Management API")
                        .description("""
                                REST API for task management with JWT authentication.
                                
                                ## Features
                                - User registration and authentication (JWT)
                                - Full CRUD for tasks
                                - Filter by status and priority
                                - Full-text search
                                - Pagination and sorting
                                
                                ## How to use
                                1. Register a user via `POST /api/auth/register`
                                2. Login via `POST /api/auth/login` to get your JWT token
                                3. Click **Authorize** and enter: `Bearer <your_token>`
                                4. Use the task endpoints freely
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Bruno Dykstra")
                                .email("brunodykstra@gmail.com")
                                .url("https://github.com/DykstraBruno"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
