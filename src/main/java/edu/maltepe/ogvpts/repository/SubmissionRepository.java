package edu.maltepe.ogvpts.repository;

import edu.maltepe.ogvpts.model.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SubmissionRepository extends JpaRepository<Submission, Integer> {

    /** Göreve ait tüm teslimler, en yeni sürüm en üstte (önceki teslimler korunur). */
    @Query("select s from Submission s join fetch s.submittedBy where s.task.id = :taskId order by s.versionNo desc")
    List<Submission> findByTask(@Param("taskId") Integer taskId);

    @Query("select coalesce(max(s.versionNo), 0) from Submission s where s.task.id = :taskId")
    int maxVersion(@Param("taskId") Integer taskId);
}
