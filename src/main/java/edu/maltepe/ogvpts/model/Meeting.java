package edu.maltepe.ogvpts.model;

import edu.maltepe.ogvpts.model.enums.MeetingStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Tablo: meetings — grubun danışmandan talep ettiği görüşme (FR-03, FR-04).
 * BR-02: aynı slot için yalnızca bir aktif (onaylı/tamamlanmış) görüşme olabilir;
 * veritabanındaki is_active sütunu durumdan otomatik hesaplanır.
 */
@Entity
@Table(name = "meetings")
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
    @Column(name = "is_active", nullable = false)
    private boolean active;

    @PrePersist
    @PreUpdate
    void syncActiveFlag() {
        this.active = status == MeetingStatus.ONAYLANDI || status == MeetingStatus.TAMAMLANDI;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public AvailabilitySlot getSlot() {
        return slot;
    }

    public void setSlot(AvailabilitySlot slot) {
        this.slot = slot;
    }

    public ProjectGroup getGroup() {
        return group;
    }

    public void setGroup(ProjectGroup group) {
        this.group = group;
    }

    public User getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(User requestedBy) {
        this.requestedBy = requestedBy;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public MeetingStatus getStatus() {
        return status;
    }

    public void setStatus(MeetingStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isActive() {
        return active;
    }
}
