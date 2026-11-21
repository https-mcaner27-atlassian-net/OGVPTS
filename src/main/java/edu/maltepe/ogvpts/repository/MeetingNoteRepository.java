package edu.maltepe.ogvpts.repository;

import edu.maltepe.ogvpts.model.MeetingNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MeetingNoteRepository extends JpaRepository<MeetingNote, Integer> {

    @Query("select n from MeetingNote n where n.meeting.id = :meetingId")
    Optional<MeetingNote> findByMeeting(@Param("meetingId") Integer meetingId);

    @Query("select n from MeetingNote n where n.meeting.id in :meetingIds")
    List<MeetingNote> findByMeetings(@Param("meetingIds") Collection<Integer> meetingIds);
}
