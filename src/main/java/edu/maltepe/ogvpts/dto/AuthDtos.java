package edu.maltepe.ogvpts.dto;

import edu.maltepe.ogvpts.model.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Giriş ve oturum bilgisiyle ilgili veri yapıları.
 *
 * Neden "record"? Java 16+ ile gelen record'lar, yalnızca veri taşıyan sınıflar için
 * kısa bir yazımdır: alanlar, constructor, getter, equals/hashCode otomatik üretilir.
 */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(
            @NotBlank(message = "E-posta zorunludur.") @Email(message = "Geçerli bir e-posta girin.") String email,
            @NotBlank(message = "Şifre zorunludur.") String password) {
    }

    /** Giriş yapan kullanıcının profili ve (öğrenciyse) grup bilgisi. */
    public record MeResponse(Integer id, String fullName, String email, UserRole role,
                             Integer groupId, String groupName, String advisorName) {
    }

    public record LoginResponse(String token, long expiresInHours, MeResponse user) {
    }

    public record ChangePasswordRequest(
            @NotBlank(message = "Mevcut şifre zorunludur.") String currentPassword,
            @NotBlank @Size(min = 8, message = "Yeni şifre en az 8 karakter olmalı.") String newPassword) {
    }
}
