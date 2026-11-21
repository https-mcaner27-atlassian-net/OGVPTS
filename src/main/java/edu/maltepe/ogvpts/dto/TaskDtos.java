package edu.maltepe.ogvpts.dto;

import edu.maltepe.ogvpts.model.enums.FeedbackDecision;
import edu.maltepe.ogvpts.model.enums.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class TaskDtos {

    private TaskDtos() {
    }

    /** FR-13: danışmanın gruba görev ataması. */
    public record CreateTaskRequest(
            @NotNull(message = "Grup seçilmelidir.") Integer groupId,
            @NotBlank(message = "Görev başlığı zorunludur.") @Size(max = 200) String title,
            String description,
            @NotNull(message = "Son teslim tarihi zorunludur.") LocalDate dueDate) {
    }

    public record TaskResponse(Integer id, Integer groupId, String groupName, String title, String description,
                               LocalDate dueDate, TaskStatus status, LocalDateTime createdAt,
                               int submissionCount) {
    }

    public record FeedbackResponse(Integer id, FeedbackDecision decision, String comment,
                                   String advisorName, LocalDateTime createdAt) {
    }

    public record SubmissionResponse(Integer id, int versionNo, String fileName, int fileSizeKb, boolean late,
                                     LocalDateTime submittedAt, String submittedByName,
                                     FeedbackResponse feedback) {
    }

    /** Görev ayrıntısı: görev + tüm teslimler (en yenisi üstte) + her teslimin geri bildirimi. */
    public record TaskDetail(TaskResponse task, List<SubmissionResponse> submissions) {
    }

    /** FR-15: "duzeltme" kararında açıklama zorunlu (serviste kontrol edilir). */
    public record FeedbackRequest(
            @NotNull(message = "Karar seçilmelidir.") FeedbackDecision decision,
            String comment) {
    }
}
