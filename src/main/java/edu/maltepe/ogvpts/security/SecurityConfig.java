package edu.maltepe.ogvpts.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * Güvenlik ayarları (FR-01, FR-16, NFR-02).
 *
 * Nasıl çalışır?
 * 1. Kullanıcı POST /api/auth/login ile e-posta + şifre gönderir.
 * 2. Şifre BCrypt hash'iyle karşılaştırılır; doğruysa sunucu imzalı bir JWT döner.
 * 3. React her istekte "Authorization: Bearer <token>" başlığını ekler.
 * 4. Spring Security token'ın imzasını ve süresini doğrular; içindeki "roles"
 *    bilgisinden kullanıcının rolünü (ROLE_OGRENCI, ROLE_DANISMAN, ROLE_YONETICI) çıkarır.
 *
 * Sunucu oturum (session) tutmaz: her istek token ile kendini tanıtır (stateless).
 */
@Configuration
@EnableMethodSecurity // @PreAuthorize("hasRole('DANISMAN')") gibi metot düzeyi kontrolleri açar
public class SecurityConfig {

    @Value("${ogvpts.jwt.secret}")
    private String jwtSecret;

    @Value("${ogvpts.cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Token tabanlı API'de çerez kullanılmadığı için CSRF koruması gerekmez.
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Herkese açık uç noktalar
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers("/api/health/**").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // Yönetici ekranları yalnızca yönetici rolüne açık (ROL-03)
                        .requestMatchers("/api/admin/**").hasRole("YONETICI")
                        // Diğer tüm API çağrıları giriş gerektirir
                        .requestMatchers("/api/**").authenticated()
                        // /api dışı adresler: derlenmiş React dosyaları (index.html, js, css)
                        .anyRequest().permitAll())
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        return http.build();
    }

    /** Şifreler veritabanına BCrypt hash'i olarak yazılır; düz metin asla saklanmaz. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private SecretKey secretKey() {
        byte[] bytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("ogvpts.jwt.secret en az 32 karakter olmalı.");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    /** Token üretici (giriş sırasında kullanılır). HS256 = gizli anahtarla imzalama. */
    @Bean
    public JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey()));
    }

    /** Token doğrulayıcı (her istekte kullanılır). */
    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(secretKey()).macAlgorithm(MacAlgorithm.HS256).build();
    }

    /** Token içindeki "roles" listesini Spring'in yetki nesnelerine (ROLE_...) çevirir. */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    /** React geliştirme sunucusu (localhost:5173) farklı adreste çalıştığı için CORS izni. */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(allowedOrigins.split(",")).map(String::trim).toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setExposedHeaders(List.of("Content-Disposition"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
