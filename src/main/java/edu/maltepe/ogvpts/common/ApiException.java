package edu.maltepe.ogvpts.common;

import org.springframework.http.HttpStatus;

/**
 * Uygulamadaki tüm "beklenen" hataların ortak atası.
 * Fırlatıldığında {@link GlobalExceptionHandler} bunu yakalar ve
 * istemciye uygun HTTP durum kodu + Türkçe açıklama içeren JSON döner.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    /** Ön yüzün hatayı ayırt edebilmesi için kısa kod (ör. ADVISOR_QUOTA_FULL). */
    private final String code;

    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    /** 404 – kayıt bulunamadı. */
    public static ApiException notFound(String what) {
        return new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", what + " bulunamadı.");
    }

    /** 403 – kullanıcının bu kayda erişim yetkisi yok (FR-16). */
    public static ApiException forbidden(String message) {
        return new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", message);
    }

    /** 400 – istek kurallara uymuyor (eksik/hatalı veri). */
    public static ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message);
    }

    /** 409 – bir iş kuralı ihlal edildi (ör. BR-02 çakışan randevu). */
    public static ApiException conflict(String code, String message) {
        return new ApiException(HttpStatus.CONFLICT, code, message);
    }
}
