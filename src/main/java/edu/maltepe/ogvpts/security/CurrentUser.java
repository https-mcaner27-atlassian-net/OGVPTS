package edu.maltepe.ogvpts.security;

import edu.maltepe.ogvpts.model.enums.UserRole;

/**
 * İsteği yapan kullanıcının token'dan okunan kimliği.
 * Veritabanına gitmeden id ve rol bilgisine ulaşmak için kullanılır.
 */
public record CurrentUser(Integer id, UserRole role, String name) {

    public boolean isStudent() {
        return role == UserRole.OGRENCI;
    }

    public boolean isAdvisor() {
        return role == UserRole.DANISMAN;
    }

    public boolean isAdmin() {
        return role == UserRole.YONETICI;
    }
}
