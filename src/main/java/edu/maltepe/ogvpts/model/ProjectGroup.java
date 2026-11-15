package edu.maltepe.ogvpts.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Tablo: project_groups — en fazla iki öğrenciden oluşan proje grubu (FR-12). */
@Entity
@Table(name = "project_groups")
@Getter
@Setter
@NoArgsConstructor
public class ProjectGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    /** Grubun sorumlu danışmanı; henüz atanmamışsa boş olabilir. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "advisor_id")
    private User advisor;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** BR-01 / FR-12: üyelik group_members tablosunda tutulur; en fazla 2 kayıt (veritabanı tetikleyicisi). */
    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GroupMember> members = new ArrayList<>();
}
