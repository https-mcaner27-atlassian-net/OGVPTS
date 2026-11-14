package edu.maltepe.ogvpts.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Tablo: availability_slots — danışmanın müsait saatleri (FR-02).
 * BR-02: (advisor_id, start_time) UNIQUE; end_time > start_time (CHECK).
 */
@Entity
@Table(name = "availability_slots",
        uniqueConstraints = @UniqueConstraint(name = "uq_slot_advisor_start", columnNames = {"advisor_id", "start_time"}))
@Getter
@Setter
@NoArgsConstructor
public class AvailabilitySlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "advisor_id", nullable = false)
    private User advisor;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;
}
