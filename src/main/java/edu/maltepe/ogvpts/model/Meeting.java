package edu.maltepe.ogvpts.model;

import edu.maltepe.ogvpts.model.enums.MeetingStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Tablo: meetings — grubun danışmandan talep ettiği görüşme (FR-03, FR-04).
 * BR-02: aynı slot için yalnızca bir aktif (onaylı/tamamlanmış) görüşme olabilir;
 * veritabanındaki is_active sütunu durumdan otomatik hesaplanır.
 */
@Entity
@Table(name = "meetings")
@Getter
@Setter
@NoArgsConstructor
public class Meeting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "slot_id", nullable = false)
    private AvailabilitySlot slot;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private ProjectGroup group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by", nullable = false)
    private User requestedBy;

    @Column(name = "topic", nullable = false, length = 300)
    private String topic;

    @Column(name = "status", nullable = false, length = 30)
    private MeetingStatus status = MeetingStatus.BEKLEMEDE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** ck_meetings_active kısıtıyla uyumlu kalması için elle değiştirilmez. */
    @Setter(AccessLevel.NONE)
    @Column(name = "is_active", nullable = false)
    private boolean active;

    @PrePersist
    @PreUpdate
    void syncActiveFlag() {
        this.active = status == MeetingStatus.ONAYLANDI || status == MeetingStatus.TAMAMLANDI;
    }
}
