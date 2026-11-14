package edu.maltepe.ogvpts.model.enums;

/**
 * Kullanıcı rolü (users.role).
 * Veritabanında küçük harfle saklanır (ör. ogrenci); dönüşümü UserRoleConverter yapar.
 */
public enum UserRole {
    OGRENCI, DANISMAN, YONETICI
}
