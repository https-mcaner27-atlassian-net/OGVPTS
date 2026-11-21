package edu.maltepe.ogvpts.repository;

import edu.maltepe.ogvpts.model.AvailabilitySlot;
import edu.maltepe.ogvpts.model.enums.MeetingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface AvailabilitySlotRepository extends JpaRepository<AvailabilitySlot, Integer> {

    @Query("select s from AvailabilitySlot s where s.advisor.id = :advisorId and s.startTime >= :from order by s.startTime")
    List<AvailabilitySlot> findByAdvisorFrom(@Param("advisorId") Integer advisorId, @Param("from") LocalDateTime from);

    /** Aynı danışmanın verilen aralıkla çakışan başka bir müsaitliği var mı? (BR-02) */
    @Query("select case when count(s) > 0 then true else false end from AvailabilitySlot s where s.advisor.id = :advisorId "
            + "and s.startTime < :end and s.endTime > :start")
    boolean existsOverlapping(@Param("advisorId") Integer advisorId,
                              @Param("start") LocalDateTime start,
                              @Param("end") LocalDateTime end);

    /**
     * FR-02: öğrencinin seçebileceği boş saatler —
     * gelecekte olan ve üzerinde "açık" (bekleyen / önerilen / onaylı / tamamlanmış) görüşme bulunmayan slotlar.
     */
    @Query("select s from AvailabilitySlot s where s.advisor.id = :advisorId and s.startTime > :now "
            + "and not exists (select m from Meeting m where m.slot = s and m.status in :openStatuses) "
            + "order by s.startTime")
    List<AvailabilitySlot> findFree(@Param("advisorId") Integer advisorId,
                                    @Param("now") LocalDateTime now,
                                    @Param("openStatuses") Collection<MeetingStatus> openStatuses);
}
