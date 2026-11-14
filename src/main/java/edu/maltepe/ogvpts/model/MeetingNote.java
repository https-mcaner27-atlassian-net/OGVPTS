package edu.maltepe.ogvpts.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/** Tablo: meeting_notes — görüşme notu ve alınan kararlar; yalnızca ilgili danışman yazar (FR-09, BR-04). */
@Entity
@Table(name = "meeting_notes")
@Getter
@Setter
@NoArgsConstructor
public class MeetingNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meeting_id", nullable = false, unique = true)
    private Meeting meeting;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "advisor_id", nullable = false)
    private User advisor;

    @Column(name = "note", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String note;

    @Column(name = "decisions", columnDefinition = "NVARCHAR(MAX)")
    private String decisions;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
