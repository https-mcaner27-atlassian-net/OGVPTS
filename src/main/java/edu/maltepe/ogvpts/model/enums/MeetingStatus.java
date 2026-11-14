package edu.maltepe.ogvpts.model.enums;

/**
 * Görüşme durumu (meetings.status).
 * Veritabanında küçük harfle saklanır (ör. beklemede); dönüşümü MeetingStatusConverter yapar.
 */
public enum MeetingStatus {
    BEKLEMEDE, ONAYLANDI, REDDEDILDI, YENI_SAAT_ONERILDI, TAMAMLANDI, IPTAL
}
