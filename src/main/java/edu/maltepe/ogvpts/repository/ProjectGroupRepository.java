package edu.maltepe.ogvpts.repository;

import edu.maltepe.ogvpts.model.ProjectGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProjectGroupRepository extends JpaRepository<ProjectGroup, Integer> {

    /** Danışmanın sorumlu olduğu gruplar (FR-07, FR-13, FR-16). */
    @Query("select g from ProjectGroup g where g.advisor.id = :advisorId order by g.name")
    List<ProjectGroup> findByAdvisor(@Param("advisorId") Integer advisorId);

    @Query("select count(g) from ProjectGroup g where g.advisor.id = :advisorId")
    long countByAdvisor(@Param("advisorId") Integer advisorId);

    @Query("select g from ProjectGroup g order by g.name")
    List<ProjectGroup> findAllOrdered();
}
