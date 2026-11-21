package edu.maltepe.ogvpts.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tüm controller'lardan fırlayan hataları tek bir yerde JSON'a çevirir.
 *
 * Yanıt biçimi (RFC 9457 "Problem Details"):
 * <pre>
 * { "status": 409, "title": "Conflict", "detail": "Bu saatte onaylı başka bir görüşme var.",
 *   "code": "SLOT_CONFLICT" }
 * </pre>
 * React tarafı "detail" alanını kullanıcıya gösterir.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Veritabanı kısıtlarının adları → kullanıcıya gösterilecek mesaj.
     * Kod tarafında zaten kontrol ediyoruz; bu tablo "son savunma hattı"dır
     * (ör. iki istek aynı anda gelirse veritabanı ikincisini reddeder).
     */
    private static final Map<String, String> CONSTRAINT_MESSAGES = new LinkedHashMap<>();

    static {
        CONSTRAINT_MESSAGES.put("ux_meeting_slot_active", "Bu saatte onaylı başka bir görüşme var (BR-02).");
        CONSTRAINT_MESSAGES.put("uq_slot_advisor_start", "Bu başlangıç saatinde zaten bir müsaitlik kaydınız var.");
        CONSTRAINT_MESSAGES.put("en fazla 2", "Bir grupta en fazla 2 öğrenci olabilir.");
        CONSTRAINT_MESSAGES.put("group_members", "Bu öğrenci zaten bir grupta (BR-01).");
        CONSTRAINT_MESSAGES.put("uq_submission_version", "Aynı anda iki teslim yapıldı; lütfen tekrar deneyin.");
        CONSTRAINT_MESSAGES.put("ck_feedback_comment", "Düzeltme isteğinde açıklama zorunludur.");
        CONSTRAINT_MESSAGES.put("ck_file_size", "Dosya en fazla 20 MB olabilir.");
        CONSTRAINT_MESSAGES.put("UQ__users", "Bu e-posta adresi zaten kayıtlı.");
    }

    @ExceptionHandler(ApiException.class)
    public ProblemDetail handleApi(ApiException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getMessage());
        pd.setProperty("code", ex.getCode());
        return pd;
    }

    /** @Valid ile işaretlenmiş isteklerdeki alan hataları (ör. boş başlık). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(err -> fields.putIfAbsent(err.getField(), err.getDefaultMessage()));
        String first = fields.isEmpty() ? "Gönderilen bilgiler geçersiz." : fields.values().iterator().next();
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, first);
        pd.setProperty("code", "VALIDATION");
        pd.setProperty("fields", fields);
        return pd;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleIntegrity(DataIntegrityViolationException ex) {
        String raw = String.valueOf(ex.getMostSpecificCause().getMessage());
        String message = CONSTRAINT_MESSAGES.entrySet().stream()
                .filter(e -> raw.contains(e.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse("İşlem veritabanı kurallarına uymuyor.");
        log.warn("Veritabanı kısıtı ihlali: {}", raw);
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, message);
        pd.setProperty("code", "DB_CONSTRAINT");
        return pd;
    }

    /** @PreAuthorize ile korunan bir metoda yanlış rolle erişilirse. */
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Bu işlem için yetkiniz yok.");
        pd.setProperty("code", "FORBIDDEN");
        return pd;
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ProblemDetail handleUploadSize(MaxUploadSizeExceededException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.PAYLOAD_TOO_LARGE, "Dosya en fazla 20 MB olabilir.");
        pd.setProperty("code", "FILE_TOO_LARGE");
        return pd;
    }

    /** Beklenmeyen her şey: ayrıntıyı log'a yaz, kullanıcıya genel mesaj göster. */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleOther(Exception ex) throws Exception {
        // Spring'in kendi işlediği hataları (ör. 404 sayfa, 405 yöntem) olduğu gibi bırak.
        if (ex instanceof org.springframework.web.ErrorResponse) {
            throw ex;
        }
        log.error("Beklenmeyen hata", ex);
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "Beklenmeyen bir hata oluştu. Lütfen tekrar deneyin.");
        pd.setProperty("code", "INTERNAL");
        return pd;
    }
}
