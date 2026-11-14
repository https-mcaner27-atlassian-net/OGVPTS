package edu.maltepe.ogvpts.repository;

import edu.maltepe.ogvpts.model.MeetingNote;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingNoteRepository extends JpaRepository<MeetingNote, Integer> {
}
