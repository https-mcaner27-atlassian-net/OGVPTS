package edu.maltepe.ogvpts.service;

import edu.maltepe.ogvpts.common.ApiException;
import edu.maltepe.ogvpts.dto.ProjectDtos.CommentRequest;
import edu.maltepe.ogvpts.dto.ProjectDtos.CommentResponse;
import edu.maltepe.ogvpts.dto.ProjectDtos.CreateProjectRequest;
import edu.maltepe.ogvpts.dto.ProjectDtos.MilestoneRequest;
import edu.maltepe.ogvpts.dto.ProjectDtos.MilestoneResponse;
import edu.maltepe.ogvpts.dto.ProjectDtos.ProjectResponse;
import edu.maltepe.ogvpts.dto.ProjectDtos.ProjectSummary;
import edu.maltepe.ogvpts.dto.ProjectDtos.UpdateProjectRequest;
import edu.maltepe.ogvpts.model.Project;
import edu.maltepe.ogvpts.model.ProjectComment;
import edu.maltepe.ogvpts.model.ProjectGroup;
import edu.maltepe.ogvpts.model.ProjectMilestone;
import edu.maltepe.ogvpts.model.enums.ProjectStatus;
import edu.maltepe.ogvpts.repository.ProjectCommentRepository;
import edu.maltepe.ogvpts.repository.ProjectMilestoneRepository;
import edu.maltepe.ogvpts.repository.ProjectRepository;
import edu.maltepe.ogvpts.repository.UserRepository;
import edu.maltepe.ogvpts.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** FR-05 (proje + aşamalar), FR-06 (ilerleme güncelleme), FR-07 (danışman yorumu). */
@Service
public class ProjectService {

    private final ProjectRepository projects;
    private final ProjectMilestoneRepository milestones;
    private final ProjectCommentRepository comments;
    private final UserRepository users;
    private final AccessGuard guard;
    private final NotificationService notifications;

    public ProjectService(ProjectRepository projects, ProjectMilestoneRepository milestones,
                          ProjectCommentRepository comments, UserRepository users,
                          AccessGuard guard, NotificationService notifications) {
        this.projects = projects;
        this.milestones = milestones;
        this.comments = comments;
        this.users = users;
        this.guard = guard;
        this.notifications = notifications;
    }

    @Transactional
    public ProjectResponse create(CurrentUser cu, CreateProjectRequest req) {
        ProjectGroup group = guard.requireOwnGroup(cu);
        if (projects.findByGroup(group.getId()).isPresent()) {
            throw ApiException.conflict("PROJECT_EXISTS", "Grubunuzun zaten bir projesi var.");
        }
        Project p = new Project();
        p.setGroup(group);
        p.setTitle(req.title().trim());
        p.setDescription(req.description());
        p.setStatus(ProjectStatus.BASLAMADI);
        p = projects.saveAndFlush(p);
        for (MilestoneRequest m : req.milestones()) {
            addMilestoneInternal(p, m);
        }
        if (group.getAdvisor() != null) {
            notifications.notify(group.getAdvisor(), "PROJE",
                    "\"" + group.getName() + "\" grubu yeni bir proje oluşturdu: " + p.getTitle());
        }
        return toResponse(p);
    }

    /** Öğrencinin grubunun projesi (yoksa boş). */
    @Transactional(readOnly = true)
    public Optional<ProjectResponse> mine(CurrentUser cu) {
        ProjectGroup group = guard.requireOwnGroup(cu);
        return projects.findByGroup(group.getId()).map(this::toResponse);
    }

    /** FR-07: danışman yalnızca kendi gruplarının projelerini listeler. */
    @Transactional(readOnly = true)
    public List<ProjectSummary> listForAdvisor(CurrentUser cu) {
        guard.requireAdvisor(cu);
        return projects.findByAdvisor(cu.id()).stream().map(this::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public List<ProjectSummary> listAll() {
        return projects.findAll().stream().map(this::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse get(CurrentUser cu, Integer id) {
        Project p = find(id);
        guard.requireCanView(p.getGroup(), cu);
        return toResponse(p);
    }

    @Transactional
    public ProjectResponse update(CurrentUser cu, Integer id, UpdateProjectRequest req) {
        Project p = find(id);
        guard.requireMember(p.getGroup(), cu);
        p.setTitle(req.title().trim());
        p.setDescription(req.description());
        return toResponse(p);
    }

    @Transactional
    public ProjectResponse addMilestone(CurrentUser cu, Integer projectId, MilestoneRequest req) {
        Project p = find(projectId);
        guard.requireMember(p.getGroup(), cu);
        addMilestoneInternal(p, req);
        recomputeStatus(p);
        return toResponse(p);
    }

    /** FR-06: aşama durumu (Başlamadı / Devam ediyor / Tamamlandı). Proje durumu otomatik güncellenir. */
    @Transactional
    public ProjectResponse updateMilestoneStatus(CurrentUser cu, Integer milestoneId, ProjectStatus status) {
        ProjectMilestone m = milestones.findById(milestoneId).orElseThrow(() -> ApiException.notFound("Aşama"));
        Project p = m.getProject();
        guard.requireMember(p.getGroup(), cu);
        m.setStatus(status);
        milestones.flush();
        ProjectStatus before = p.getStatus();
        recomputeStatus(p);
        ProjectGroup group = p.getGroup();
        if (status == ProjectStatus.TAMAMLANDI && group.getAdvisor() != null) {
            notifications.notify(group.getAdvisor(), "PROJE",
                    "\"" + group.getName() + "\" grubu \"" + m.getTitle() + "\" aşamasını tamamladı.");
        }
        if (before != ProjectStatus.TAMAMLANDI && p.getStatus() == ProjectStatus.TAMAMLANDI && group.getAdvisor() != null) {
            notifications.notify(group.getAdvisor(), "PROJE", "\"" + p.getTitle() + "\" projesinin tüm aşamaları tamamlandı.");
        }
        return toResponse(p);
    }

    @Transactional
    public ProjectResponse deleteMilestone(CurrentUser cu, Integer milestoneId) {
        ProjectMilestone m = milestones.findById(milestoneId).orElseThrow(() -> ApiException.notFound("Aşama"));
        Project p = m.getProject();
        guard.requireMember(p.getGroup(), cu);
        if (milestones.findByProject(p.getId()).size() <= 1) {
            throw ApiException.conflict("LAST_MILESTONE", "Projede en az bir aşama kalmalıdır.");
        }
        milestones.delete(m);
        milestones.flush();
        recomputeStatus(p);
        return toResponse(p);
    }

    /** FR-07: danışman yorumu; öğrencilere bildirim gider. */
    @Transactional
    public ProjectResponse addComment(CurrentUser cu, Integer projectId, CommentRequest req) {
        Project p = find(projectId);
        guard.requireGroupAdvisor(p.getGroup(), cu);
        ProjectComment c = new ProjectComment();
        c.setProject(p);
        c.setAuthor(users.getReferenceById(cu.id()));
        c.setBody(req.body().trim());
        comments.saveAndFlush(c);
        notifications.notifyGroup(p.getGroup(), "PROJE",
                "Danışmanınız \"" + p.getTitle() + "\" projesine yorum yazdı.");
        return toResponse(p);
    }

    // ------------------------------------------------------------------ yardımcılar

    private Project find(Integer id) {
        return projects.findById(id).orElseThrow(() -> ApiException.notFound("Proje"));
    }

    private void addMilestoneInternal(Project p, MilestoneRequest req) {
        ProjectMilestone m = new ProjectMilestone();
        m.setProject(p);
        m.setTitle(req.title().trim());
        m.setDueDate(req.dueDate());
        m.setStatus(ProjectStatus.BASLAMADI);
        milestones.saveAndFlush(m);
    }

    /**
     * Proje durumu aşamalardan türetilir:
     * hepsi tamamlandıysa TAMAMLANDI, hiçbiri başlamadıysa BASLAMADI, aksi halde DEVAM_EDIYOR.
     */
    private void recomputeStatus(Project p) {
        List<ProjectMilestone> list = milestones.findByProject(p.getId());
        boolean allDone = !list.isEmpty() && list.stream().allMatch(m -> m.getStatus() == ProjectStatus.TAMAMLANDI);
        boolean noneStarted = list.stream().allMatch(m -> m.getStatus() == ProjectStatus.BASLAMADI);
        p.setStatus(allDone ? ProjectStatus.TAMAMLANDI : noneStarted ? ProjectStatus.BASLAMADI : ProjectStatus.DEVAM_EDIYOR);
    }

    private static int progress(List<ProjectMilestone> list) {
        if (list.isEmpty()) {
            return 0;
        }
        long done = list.stream().filter(m -> m.getStatus() == ProjectStatus.TAMAMLANDI).count();
        return (int) Math.round(done * 100.0 / list.size());
    }

    private ProjectResponse toResponse(Project p) {
        List<ProjectMilestone> ms = milestones.findByProject(p.getId());
        ProjectGroup g = p.getGroup();
        return new ProjectResponse(p.getId(), g.getId(), g.getName(),
                g.getAdvisor() == null ? null : g.getAdvisor().getFullName(),
                p.getTitle(), p.getDescription(), p.getStatus(), p.getCreatedAt(), progress(ms),
                ms.stream().map(m -> new MilestoneResponse(m.getId(), m.getTitle(), m.getDueDate(), m.getStatus())).toList(),
                comments.findByProject(p.getId()).stream()
                        .map(c -> new CommentResponse(c.getId(), c.getAuthor().getFullName(), c.getBody(), c.getCreatedAt()))
                        .toList());
    }

    private ProjectSummary toSummary(Project p) {
        List<ProjectMilestone> ms = milestones.findByProject(p.getId());
        LocalDate next = ms.stream()
                .filter(m -> m.getStatus() != ProjectStatus.TAMAMLANDI)
                .map(ProjectMilestone::getDueDate)
                .min(LocalDate::compareTo)
                .orElse(null);
        return new ProjectSummary(p.getId(), p.getGroup().getId(), p.getGroup().getName(), p.getTitle(),
                p.getStatus(), progress(ms), next);
    }
}
