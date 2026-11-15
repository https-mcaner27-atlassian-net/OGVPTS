package edu.maltepe.ogvpts.repository;

import edu.maltepe.ogvpts.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Integer> {
}
