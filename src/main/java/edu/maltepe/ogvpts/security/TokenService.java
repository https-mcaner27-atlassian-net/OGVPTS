package edu.maltepe.ogvpts.security;

import edu.maltepe.ogvpts.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** Giriş başarılı olduğunda kullanıcıya verilecek JWT'yi üretir. */
@Service
public class TokenService {

    private final JwtEncoder encoder;
    private final long expirationHours;

    public TokenService(JwtEncoder encoder, @Value("${ogvpts.jwt.expiration-hours}") long expirationHours) {
        this.encoder = encoder;
        this.expirationHours = expirationHours;
    }

    public String issue(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("ogvpts")
                .issuedAt(now)
                .expiresAt(now.plus(expirationHours, ChronoUnit.HOURS))
                .subject(String.valueOf(user.getId()))           // "sub": kullanıcının id'si
                .claim("name", user.getFullName())
                .claim("email", user.getEmail())
                .claim("roles", List.of(user.getRole().name()))  // ör. ["OGRENCI"]
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public long getExpirationHours() {
        return expirationHours;
    }
}
