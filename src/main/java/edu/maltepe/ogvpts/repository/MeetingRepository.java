package edu.maltepe.ogvpts.repository;

import edu.maltepe.ogvpts.model.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingRepository extends JpaRepository<Meeting, Integer> {
}
