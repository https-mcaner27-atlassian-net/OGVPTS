package edu.maltepe.ogvpts.repository;

import edu.maltepe.ogvpts.model.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    @Query("select n from Notification n where n.user.id = :userId order by n.createdAt desc, n.id desc")
    List<Notification> findLatest(@Param("userId") Integer userId, Pageable pageable);

    @Query("select count(n) from Notification n where n.user.id = :userId and n.read = false")
    long countUnread(@Param("userId") Integer userId);

    @Modifying
    @Query("update Notification n set n.read = true where n.user.id = :userId and n.read = false")
    int markAllRead(@Param("userId") Integer userId);

    /** Aynı hatırlatmanın iki kez gönderilmesini önlemek için. */
    @Query("select case when count(n) > 0 then true else false end from Notification n where n.user.id = :userId and n.type = :type and n.message = :message")
    boolean exists(@Param("userId") Integer userId, @Param("type") String type, @Param("message") String message);
}
