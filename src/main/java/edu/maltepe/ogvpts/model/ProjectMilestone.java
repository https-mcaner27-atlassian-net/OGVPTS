package edu.maltepe.ogvpts.model;

import edu.maltepe.ogvpts.model.enums.ProjectStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/** Tablo: project_milestones — proje aşamaları ve teslim tarihleri (FR-05, FR-06). */
@Entity
@Table(name = "project_milestones")
@Getter
@Setter
@NoArgsConstructor
public class ProjectMilestone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "status", nullable = false, length = 20)
    private ProjectStatus status = ProjectStatus.BASLAMADI;
}
