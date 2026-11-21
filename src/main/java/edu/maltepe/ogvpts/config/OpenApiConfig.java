package edu.maltepe.ogvpts.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger arayüzü: http://localhost:8080/swagger-ui.html
 *
 * Kullanım: önce POST /api/auth/login ile giriş yap, dönen "token" değerini kopyala,
 * sağ üstteki "Authorize" düğmesine yapıştır. Sonra tüm uç noktaları deneyebilirsin.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ogvptsOpenApi() {
        String scheme = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("OGVPTS API")
                        .version("1.0")
                        .description("Öğrenci–Danışman Görüşme ve Proje Takip Sistemi REST API'si"))
                .addSecurityItem(new SecurityRequirement().addList(scheme))
                .components(new Components().addSecuritySchemes(scheme, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
    }
}
