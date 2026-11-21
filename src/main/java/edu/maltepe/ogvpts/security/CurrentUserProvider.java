package edu.maltepe.ogvpts.security;

import edu.maltepe.ogvpts.common.ApiException;
import edu.maltepe.ogvpts.model.enums.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * "Bu isteği kim yapıyor?" sorusunun cevabı.
 * Spring Security doğrulanmış token'ı SecurityContext içine koyar; buradan okuruz.
 */
@Component
public class CurrentUserProvider {

    public CurrentUser get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Jwt jwt)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Lütfen giriş yapın.");
        }
        List<String> roles = jwt.getClaimAsStringList("roles");
        UserRole role = UserRole.valueOf(roles.get(0));
        return new CurrentUser(Integer.valueOf(jwt.getSubject()), role, jwt.getClaimAsString("name"));
    }
}
