package edu.maltepe.ogvpts.repository;

import edu.maltepe.ogvpts.model.ProjectComment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectCommentRepository extends JpaRepository<ProjectComment, Integer> {
}
