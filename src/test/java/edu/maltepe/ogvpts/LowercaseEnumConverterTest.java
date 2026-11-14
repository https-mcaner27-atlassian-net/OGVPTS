package edu.maltepe.ogvpts;

import edu.maltepe.ogvpts.model.converter.MeetingStatusConverter;
import edu.maltepe.ogvpts.model.converter.UserRoleConverter;
import edu.maltepe.ogvpts.model.enums.MeetingStatus;
import edu.maltepe.ogvpts.model.enums.UserRole;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Veritabanı gerektirmeyen test: enum ↔ veritabanı değeri dönüşümü (Türkçe yerel ayarda da). */
class LowercaseEnumConverterTest {

    @Test
    void enumValuesMatchDatabaseValuesEvenWithTurkishLocale() {
        Locale original = Locale.getDefault();
        Locale.setDefault(Locale.forLanguageTag("tr-TR"));
        try {
            MeetingStatusConverter meeting = new MeetingStatusConverter();
            assertEquals("yeni_saat_onerildi", meeting.convertToDatabaseColumn(MeetingStatus.YENI_SAAT_ONERILDI));
            assertEquals(MeetingStatus.REDDEDILDI, meeting.convertToEntityAttribute("reddedildi"));

            UserRoleConverter role = new UserRoleConverter();
            assertEquals("ogrenci", role.convertToDatabaseColumn(UserRole.OGRENCI));
            assertEquals(UserRole.DANISMAN, role.convertToEntityAttribute("danisman"));
        } finally {
            Locale.setDefault(original);
        }
    }
}
