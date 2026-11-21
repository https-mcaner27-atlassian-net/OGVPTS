package edu.maltepe.ogvpts.repository;

import edu.maltepe.ogvpts.model.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Integer> {

    @Query("select f from Feedback f join fetch f.advisor where f.submission.task.id = :taskId order by f.createdAt")
    List<Feedback> findByTask(@Param("taskId") Integer taskId);

    @Query("select case when count(f) > 0 then true else false end from Feedback f where f.submission.id = :submissionId")
    boolean existsForSubmission(@Param("submissionId") Integer submissionId);
}
