package edu.maltepe.ogvpts.repository;

import edu.maltepe.ogvpts.model.Meeting;
import edu.maltepe.ogvpts.model.enums.MeetingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface MeetingRepository extends JpaRepository<Meeting, Integer> {

    /** Grubun görüşmeleri, en yeni saat en üstte (FR-09). */
    @Query("select m from Meeting m join fetch m.slot s join fetch m.group join fetch m.requestedBy "
            + "where m.group.id = :groupId order by s.startTime desc")
    List<Meeting> findByGroup(@Param("groupId") Integer groupId);

    /** Danışmanın görüşmeleri (slot sahibi danışman). */
    @Query("select m from Meeting m join fetch m.slot s join fetch m.group join fetch m.requestedBy "
            + "where s.advisor.id = :advisorId order by s.startTime desc")
    List<Meeting> findByAdvisor(@Param("advisorId") Integer advisorId);

    /** Slot üzerinde, verilen görüşme dışında, belirtilen durumlarda görüşme var mı? */
    @Query("select case when count(m) > 0 then true else false end from Meeting m where m.slot.id = :slotId and m.id <> :excludeId and m.status in :statuses")
    boolean existsOnSlot(@Param("slotId") Integer slotId,
                         @Param("excludeId") Integer excludeId,
                         @Param("statuses") Collection<MeetingStatus> statuses);

    @Query("select case when count(m) > 0 then true else false end from Meeting m where m.slot.id = :slotId")
    boolean existsAnyOnSlot(@Param("slotId") Integer slotId);

    /** FR-08: verilen aralıkta başlayacak onaylı görüşmeler (24 saat önce hatırlatma). */
    @Query("select m from Meeting m join fetch m.slot s join fetch s.advisor join fetch m.group "
            + "where m.status = :status and s.startTime >= :from and s.startTime < :to")
    List<Meeting> findStartingBetween(@Param("status") MeetingStatus status,
                                      @Param("from") LocalDateTime from,
                                      @Param("to") LocalDateTime to);

    /** FR-11: dönem içindeki görüşmelerin danışman + durum bazında sayısı. */
    @Query("select s.advisor.id, s.advisor.fullName, m.status, count(m) from Meeting m join m.slot s "
            + "where s.startTime >= :from and s.startTime < :to "
            + "group by s.advisor.id, s.advisor.fullName, m.status")
    List<Object[]> countByAdvisorAndStatus(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
