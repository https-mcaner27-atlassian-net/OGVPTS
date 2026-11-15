package edu.maltepe.ogvpts.config;

import edu.maltepe.ogvpts.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Veritabanı bağlantısını ve JPA modellerinin tablolarla eşleştiğini kontrol eder.
 * Her tablodan bir kayıt sorgulanır; sütun adı ya da tipi uyuşmazsa hata burada görünür.
 */
@Service
public class DatabaseHealthService {

    private final DataSource dataSource;
    private final Map<String, JpaRepository<?, ?>> repositories = new LinkedHashMap<>();

    public DatabaseHealthService(DataSource dataSource,
                                 UserRepository users,
                                 ProjectGroupRepository projectGroups,
                                 GroupMemberRepository groupMembers,
                                 ProjectRepository projects,
                                 ProjectMilestoneRepository projectMilestones,
                                 ProjectCommentRepository projectComments,
                                 AvailabilitySlotRepository availabilitySlots,
                                 MeetingRepository meetings,
                                 MeetingNoteRepository meetingNotes,
                                 TaskRepository tasks,
                                 SubmissionRepository submissions,
                                 FeedbackRepository feedback,
                                 NotificationRepository notifications) {
        this.dataSource = dataSource;
        repositories.put("users", users);
        repositories.put("project_groups", projectGroups);
        repositories.put("group_members", groupMembers);
        repositories.put("projects", projects);
        repositories.put("project_milestones", projectMilestones);
        repositories.put("project_comments", projectComments);
        repositories.put("availability_slots", availabilitySlots);
        repositories.put("meetings", meetings);
        repositories.put("meeting_notes", meetingNotes);
        repositories.put("tasks", tasks);
        repositories.put("submissions", submissions);
        repositories.put("feedback", feedback);
        repositories.put("notifications", notifications);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> check() throws SQLException {
        Map<String, Object> result = new LinkedHashMap<>();
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData meta = connection.getMetaData();
            result.put("database", connection.getCatalog());
            result.put("server", meta.getDatabaseProductName() + " " + meta.getDatabaseProductVersion());
        }

        Map<String, Long> tables = new LinkedHashMap<>();
        repositories.forEach((table, repository) -> {
            repository.findAll(PageRequest.of(0, 1));   // modeldeki tüm sütunları seçer
            tables.put(table, repository.count());
        });
        result.put("tables", tables);
        result.put("status", "OK");
        return result;
    }
}
