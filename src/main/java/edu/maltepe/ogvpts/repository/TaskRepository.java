package edu.maltepe.ogvpts.repository;

import edu.maltepe.ogvpts.model.Task;
import edu.maltepe.ogvpts.model.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Integer> {

    @Query("select t from Task t join fetch t.group where t.group.id = :groupId order by t.dueDate, t.id")
    List<Task> findByGroup(@Param("groupId") Integer groupId);

    @Query("select t from Task t join fetch t.group where t.advisor.id = :advisorId order by t.dueDate, t.id")
    List<Task> findByAdvisor(@Param("advisorId") Integer advisorId);

    /** FR-08: son teslim tarihi verilen gün olan, henüz onaylanmamış görevler. */
    @Query("select t from Task t join fetch t.group where t.dueDate = :date and t.status <> :approved")
    List<Task> findDueOn(@Param("date") LocalDate date, @Param("approved") TaskStatus approved);
}
