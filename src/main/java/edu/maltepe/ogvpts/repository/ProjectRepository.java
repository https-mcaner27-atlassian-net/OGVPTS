package edu.maltepe.ogvpts.repository;

import edu.maltepe.ogvpts.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Integer> {
}
