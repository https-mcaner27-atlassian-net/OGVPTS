package edu.maltepe.ogvpts.repository;

import edu.maltepe.ogvpts.model.Project;
import edu.maltepe.ogvpts.model.enums.ProjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Integer> {

    @Query("select p from Project p where p.group.id = :groupId")
    Optional<Project> findByGroup(@Param("groupId") Integer groupId);

    /** Danışmanın gruplarına ait projeler (FR-07). */
    @Query("select p from Project p join fetch p.group g where g.advisor.id = :advisorId order by p.title")
    List<Project> findByAdvisor(@Param("advisorId") Integer advisorId);

    @Query("select p.status, count(p) from Project p group by p.status")
    List<Object[]> countByStatus();

    @Query("select count(p) from Project p where p.status = :status")
    long countWithStatus(@Param("status") ProjectStatus status);
}
