package edu.maltepe.ogvpts.repository;

import edu.maltepe.ogvpts.model.ProjectComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProjectCommentRepository extends JpaRepository<ProjectComment, Integer> {

    @Query("select c from ProjectComment c join fetch c.author where c.project.id = :projectId order by c.createdAt desc")
    List<ProjectComment> findByProject(@Param("projectId") Integer projectId);
}
