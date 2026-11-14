package edu.maltepe.ogvpts.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Tablo: submissions — öğrencinin görev teslimi (FR-14).
 * Her teslim ayrı kayıttır (task_id, version_no UNIQUE); önceki teslimler silinmez.
 */
@Entity
@Table(name = "submissions",
        uniqueConstraints = @UniqueConstraint(name = "uq_submission_version", columnNames = {"task_id", "version_no"}))
@Getter
@Setter
@NoArgsConstructor
public class Submission {

    /** FR-06: yüklenen dosya en fazla 20 MB (20480 KB). */
    public static final int MAX_FILE_SIZE_KB = 20480;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submitted_by", nullable = false)
    private User submittedBy;

    @Column(name = "version_no", nullable = false)
    private Integer versionNo = 1;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Max(MAX_FILE_SIZE_KB)
    @Column(name = "file_size_kb", nullable = false)
    private Integer fileSizeKb;

    @Column(name = "is_late", nullable = false)
    private boolean late;

    @CreationTimestamp
    @Column(name = "submitted_at", nullable = false, updatable = false)
    private LocalDateTime submittedAt;
}
