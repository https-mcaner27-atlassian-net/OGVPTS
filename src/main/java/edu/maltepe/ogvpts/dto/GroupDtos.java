package edu.maltepe.ogvpts.dto;

import edu.maltepe.ogvpts.dto.UserDtos.UserSummary;
import edu.maltepe.ogvpts.model.enums.ProjectStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class GroupDtos {

    private GroupDtos() {
    }

    public record GroupResponse(Integer id, String name, UserSummary advisor, List<UserSummary> members,
                                Integer projectId, String projectTitle, ProjectStatus projectStatus) {
    }

    public record CreateGroupRequest(
            @NotBlank(message = "Grup adı zorunludur.") @Size(max = 150) String name) {
    }

    /** FR-12: gruba ikinci öğrenciyi e-postasıyla ekleme. */
    public record AddMemberRequest(
            @NotBlank(message = "E-posta zorunludur.") @Email(message = "Geçerli bir e-posta girin.") String email) {
    }

    /**
     * FR-10: gruba danışman atama.
     * force = true → kontenjan dolu uyarısına rağmen atamayı yap.
     */
    public record AssignAdvisorRequest(@NotNull(message = "Danışman seçilmelidir.") Integer advisorId, boolean force) {
    }

    /** Danışmanın mevcut yükü (kontenjan ekranı için). */
    public record AdvisorLoad(UserSummary advisor, long groupCount, int maxGroups) {
    }
}
