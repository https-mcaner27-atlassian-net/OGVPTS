package edu.maltepe.ogvpts.dto;

import edu.maltepe.ogvpts.model.enums.ProjectStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class ProjectDtos {

    private ProjectDtos() {
    }

    public record MilestoneRequest(
            @NotBlank(message = "Aşama başlığı zorunludur.") @Size(max = 200) String title,
            @NotNull(message = "Aşama teslim tarihi zorunludur.") LocalDate dueDate) {
    }

    /** FR-05: proje adı, açıklama ve en az bir aşama zorunlu. */
    public record CreateProjectRequest(
            @NotBlank(message = "Proje adı zorunludur.") @Size(max = 200) String title,
            String description,
            @NotEmpty(message = "En az bir aşama eklenmelidir.") List<@Valid MilestoneRequest> milestones) {
    }

    public record UpdateProjectRequest(
            @NotBlank(message = "Proje adı zorunludur.") @Size(max = 200) String title,
            String description) {
    }

    public record UpdateMilestoneStatusRequest(@NotNull(message = "Durum seçilmelidir.") ProjectStatus status) {
    }

    public record CommentRequest(@NotBlank(message = "Yorum boş olamaz.") String body) {
    }

    public record MilestoneResponse(Integer id, String title, LocalDate dueDate, ProjectStatus status) {
    }

    public record CommentResponse(Integer id, String authorName, String body, LocalDateTime createdAt) {
    }

    public record ProjectResponse(Integer id, Integer groupId, String groupName, String advisorName,
                                  String title, String description, ProjectStatus status, LocalDateTime createdAt,
                                  int progressPercent, List<MilestoneResponse> milestones,
                                  List<CommentResponse> comments) {
    }

    /** Danışman listesindeki satır (FR-07). */
    public record ProjectSummary(Integer id, Integer groupId, String groupName, String title, ProjectStatus status,
                                 int progressPercent, LocalDate nextDueDate) {
    }
}
