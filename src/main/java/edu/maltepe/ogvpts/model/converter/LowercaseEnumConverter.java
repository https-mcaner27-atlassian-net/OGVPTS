package edu.maltepe.ogvpts.model.converter;

import jakarta.persistence.AttributeConverter;

import java.util.Locale;

/**
 * Java enum sabitlerini (OGRENCI) veritabanındaki küçük harfli değerlere (ogrenci) çevirir.
 * Locale.ROOT kullanılır; Türkçe yerel ayarda "I" harfinin "ı"ya dönüşmesi engellenir.
 */
public abstract class LowercaseEnumConverter<E extends Enum<E>> implements AttributeConverter<E, String> {

    private final Class<E> enumType;

    protected LowercaseEnumConverter(Class<E> enumType) {
        this.enumType = enumType;
    }

    @Override
    public String convertToDatabaseColumn(E value) {
        return value == null ? null : value.name().toLowerCase(Locale.ROOT);
    }

    @Override
    public E convertToEntityAttribute(String dbValue) {
        return dbValue == null ? null : Enum.valueOf(enumType, dbValue.toUpperCase(Locale.ROOT));
    }
}
