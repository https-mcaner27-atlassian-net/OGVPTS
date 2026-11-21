package edu.maltepe.ogvpts.repository;

import edu.maltepe.ogvpts.model.ProjectMilestone;
import edu.maltepe.ogvpts.model.enums.ProjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ProjectMilestoneRepository extends JpaRepository<ProjectMilestone, Integer> {

    @Query("select m from ProjectMilestone m where m.project.id = :projectId order by m.dueDate, m.id")
    List<ProjectMilestone> findByProject(@Param("projectId") Integer projectId);

    /** FR-08: teslim tarihi verilen gün olan ve tamamlanmamış aşamalar (hatırlatma için). */
    @Query("select m from ProjectMilestone m join fetch m.project p join fetch p.group "
            + "where m.dueDate = :date and m.status <> :done")
    List<ProjectMilestone> findDueOn(@Param("date") LocalDate date, @Param("done") ProjectStatus done);
}
