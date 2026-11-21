package edu.maltepe.ogvpts.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * React'in derlenmiş halini (frontend/ → npm run build → src/main/resources/static)
 * Spring Boot üzerinden sunar. Böylece tek bir uygulama ile hem API hem arayüz çalışır.
 *
 * React Router adresleri (/randevular, /gorevler/5 …) sunucuda dosya olarak yoktur;
 * bu yüzden /api dışındaki bilinmeyen adreslerde index.html döneriz, sayfayı React çizer.
 */
@Configuration
public class SpaWebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requested = location.createRelative(resourcePath);
                        if (requested.exists() && requested.isReadable()) {
                            return requested;
                        }
                        if (resourcePath.startsWith("api/") || resourcePath.contains(".")) {
                            return null; // gerçek 404
                        }
                        Resource index = new ClassPathResource("/static/index.html");
                        return index.exists() ? index : null;
                    }
                });
    }
}
