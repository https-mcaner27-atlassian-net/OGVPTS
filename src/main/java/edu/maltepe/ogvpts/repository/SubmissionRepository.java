package edu.maltepe.ogvpts.repository;

import edu.maltepe.ogvpts.model.Submission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubmissionRepository extends JpaRepository<Submission, Integer> {
}
