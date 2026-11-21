package edu.maltepe.ogvpts.dto;

import edu.maltepe.ogvpts.model.User;
import edu.maltepe.ogvpts.model.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public final class UserDtos {

    private UserDtos() {
    }

    /** Kullanıcının dışarıya gösterilen özeti (şifre hash'i asla dışarı çıkmaz). */
    public record UserSummary(Integer id, String fullName, String email, UserRole role) {
        public static UserSummary of(User u) {
            return u == null ? null : new UserSummary(u.getId(), u.getFullName(), u.getEmail(), u.getRole());
        }
    }

    public record UserAdminView(Integer id, String fullName, String email, UserRole role,
                                LocalDateTime createdAt, String groupName) {
    }

    public record CreateUserRequest(
            @NotBlank(message = "Ad soyad zorunludur.") @Size(max = 150) String fullName,
            @NotBlank(message = "E-posta zorunludur.") @Email(message = "Geçerli bir e-posta girin.") @Size(max = 255) String email,
            @NotNull(message = "Rol seçilmelidir.") UserRole role,
            @NotBlank @Size(min = 8, message = "Şifre en az 8 karakter olmalı.") String password) {
    }

    public record ResetPasswordRequest(
            @NotBlank @Size(min = 8, message = "Şifre en az 8 karakter olmalı.") String password) {
    }
}
