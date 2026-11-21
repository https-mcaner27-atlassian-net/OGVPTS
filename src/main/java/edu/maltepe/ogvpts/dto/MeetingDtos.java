package edu.maltepe.ogvpts.dto;

import edu.maltepe.ogvpts.model.enums.MeetingStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public final class MeetingDtos {

    private MeetingDtos() {
    }

    /** Danışmanın müsait saat eklemesi (FR-02'nin ön koşulu). */
    public record SlotRequest(
            @NotNull(message = "Başlangıç zamanı zorunludur.") LocalDateTime start,
            @NotNull(message = "Bitiş zamanı zorunludur.") LocalDateTime end) {
    }

    /**
     * Müsaitlik satırı. meetingStatus doluysa bu saate bağlı açık bir görüşme var demektir.
     */
    public record SlotResponse(Integer id, LocalDateTime start, LocalDateTime end,
                               MeetingStatus meetingStatus, String groupName) {
    }

    /** FR-02: öğrencinin randevu talebi. */
    public record MeetingRequest(
            @NotNull(message = "Bir saat seçilmelidir.") Integer slotId,
            @NotBlank(message = "Görüşme konusu zorunludur.") @Size(max = 300) String topic) {
    }

    /** FR-03: danışmanın yeni saat önerisi. */
    public record ProposeRequest(@NotNull(message = "Önerilecek saat seçilmelidir.") Integer slotId) {
    }

    /** FR-04: görüşme notu. */
    public record NoteRequest(
            @NotBlank(message = "Not boş olamaz.") String note,
            String decisions) {
    }

    public record NoteResponse(String note, String decisions, String advisorName, LocalDateTime updatedAt) {
    }

    public record MeetingResponse(Integer id, String topic, MeetingStatus status,
                                  LocalDateTime start, LocalDateTime end,
                                  Integer groupId, String groupName,
                                  Integer advisorId, String advisorName,
                                  String requestedByName, LocalDateTime createdAt,
                                  boolean canCancel, NoteResponse note) {
    }
}
