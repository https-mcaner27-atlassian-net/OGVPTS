package edu.maltepe.ogvpts.model;

import edu.maltepe.ogvpts.model.enums.FeedbackDecision;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Tablo: feedback — danışmanın teslime verdiği karar (FR-15).
 * "duzeltme" kararında yorum zorunludur (ck_feedback_comment).
 */
@Entity
@Table(name = "feedback")
@Getter
@Setter
@NoArgsConstructor
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submission_id", nullable = false)
    private Submission submission;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "advisor_id", nullable = false)
    private User advisor;

    @Column(name = "decision", nullable = false, length = 20)
    private FeedbackDecision decision;

    @Column(name = "comment", columnDefinition = "NVARCHAR(MAX)")
    private String comment;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
