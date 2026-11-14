package edu.maltepe.ogvpts.repository;

import edu.maltepe.ogvpts.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Integer> {
}
