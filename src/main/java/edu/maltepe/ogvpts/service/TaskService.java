package edu.maltepe.ogvpts.service;

import edu.maltepe.ogvpts.common.ApiException;
import edu.maltepe.ogvpts.dto.TaskDtos.CreateTaskRequest;
import edu.maltepe.ogvpts.dto.TaskDtos.FeedbackRequest;
import edu.maltepe.ogvpts.dto.TaskDtos.FeedbackResponse;
import edu.maltepe.ogvpts.dto.TaskDtos.SubmissionResponse;
import edu.maltepe.ogvpts.dto.TaskDtos.TaskDetail;
import edu.maltepe.ogvpts.dto.TaskDtos.TaskResponse;
import edu.maltepe.ogvpts.model.Feedback;
import edu.maltepe.ogvpts.model.ProjectGroup;
import edu.maltepe.ogvpts.model.Submission;
import edu.maltepe.ogvpts.model.Task;
import edu.maltepe.ogvpts.model.enums.FeedbackDecision;
import edu.maltepe.ogvpts.model.enums.TaskStatus;
import edu.maltepe.ogvpts.repository.FeedbackRepository;
import edu.maltepe.ogvpts.repository.ProjectGroupRepository;
import edu.maltepe.ogvpts.repository.SubmissionRepository;
import edu.maltepe.ogvpts.repository.TaskRepository;
import edu.maltepe.ogvpts.repository.UserRepository;
import edu.maltepe.ogvpts.security.CurrentUser;
import edu.maltepe.ogvpts.storage.FileStorageService;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * FR-13 görev atama, FR-14 teslim, FR-15 onay / düzeltme isteği.
 *
 * Görev durumu akışı:
 *   ATANDI → (öğrenci teslim eder) → TESLIM_EDILDI → (danışman) → ONAYLANDI
 *                                                              ↘ DUZELTME_ISTENDI → (yeni teslim) → TESLIM_EDILDI ...
 */
@Service
public class TaskService {

    private final TaskRepository tasks;
    private final SubmissionRepository submissions;
    private final FeedbackRepository feedbacks;
    private final ProjectGroupRepository groups;
    private final UserRepository users;
    private final FileStorageService storage;
    private final AccessGuard guard;
    private final NotificationService notifications;

    public TaskService(TaskRepository tasks, SubmissionRepository submissions, FeedbackRepository feedbacks,
                       ProjectGroupRepository groups, UserRepository users, FileStorageService storage,
                       AccessGuard guard, NotificationService notifications) {
        this.tasks = tasks;
        this.submissions = submissions;
        this.feedbacks = feedbacks;
        this.groups = groups;
        this.users = users;
        this.storage = storage;
        this.guard = guard;
        this.notifications = notifications;
    }

    /** FR-13: danışman yalnızca kendi sorumluluğundaki gruplara görev atar. */
    @Transactional
    public TaskResponse create(CurrentUser cu, CreateTaskRequest req) {
        guard.requireAdvisor(cu);
        ProjectGroup group = groups.findById(req.groupId()).orElseThrow(() -> ApiException.notFound("Grup"));
        guard.requireGroupAdvisor(group, cu);
        if (req.dueDate().isBefore(LocalDate.now())) {
            throw ApiException.badRequest("Son teslim tarihi geçmişte olamaz.");
        }
        Task t = new Task();
        t.setGroup(group);
        t.setAdvisor(users.getReferenceById(cu.id()));
        t.setTitle(req.title().trim());
        t.setDescription(req.description());
        t.setDueDate(req.dueDate());
        t.setStatus(TaskStatus.ATANDI);
        t = tasks.saveAndFlush(t);
        notifications.notifyGroup(group, "GOREV",
                "Yeni görev: \"" + t.getTitle() + "\" (son teslim " + t.getDueDate() + ")");
        return toResponse(t, 0);
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> list(CurrentUser cu) {
        List<Task> list;
        if (cu.isStudent()) {
            list = tasks.findByGroup(guard.requireOwnGroup(cu).getId());
        } else if (cu.isAdvisor()) {
            list = tasks.findByAdvisor(cu.id());
        } else {
            throw ApiException.forbidden("Görev listesi öğrenci ve danışmanlar içindir.");
        }
        return list.stream().map(t -> toResponse(t, submissions.findByTask(t.getId()).size())).toList();
    }

    @Transactional(readOnly = true)
    public TaskDetail detail(CurrentUser cu, Integer id) {
        Task t = find(id);
        requireCanView(t, cu);
        return toDetail(t);
    }

    /**
     * FR-14: teslim.
     * - Yalnızca kendi grubunun görevine teslim yapılabilir.
     * - Her teslim yeni kayıt (sürüm) olur; öncekiler silinmez.
     * - Son tarihten sonraki teslim "geç teslim" olarak işaretlenir.
     * - Onaylanmış göreve yeni teslim yapılamaz.
     */
    @Transactional
    public TaskDetail submit(CurrentUser cu, Integer taskId, MultipartFile file) {
        Task t = find(taskId);
        guard.requireMember(t.getGroup(), cu);
        if (t.getStatus() == TaskStatus.ONAYLANDI) {
            throw ApiException.conflict("TASK_APPROVED", "Onaylanmış bir göreve yeni teslim yapılamaz.");
        }
        String path = storage.storeTaskFile(t.getId(), file);
        Submission s = new Submission();
        s.setTask(t);
        s.setSubmittedBy(users.getReferenceById(cu.id()));
        s.setVersionNo(submissions.maxVersion(t.getId()) + 1);
        s.setFileName(FileStorageServiceNames.displayName(file.getOriginalFilename()));
        s.setFilePath(path);
        s.setFileSizeKb((int) Math.max(1, Math.ceil(file.getSize() / 1024.0)));
        s.setLate(LocalDate.now().isAfter(t.getDueDate()));
        submissions.saveAndFlush(s);
        t.setStatus(TaskStatus.TESLIM_EDILDI);
        notifications.notify(t.getAdvisor(), "TESLIM",
                "\"" + t.getGroup().getName() + "\" grubu \"" + t.getTitle() + "\" görevine teslim yaptı (sürüm "
                        + s.getVersionNo() + (s.isLate() ? ", geç teslim" : "") + ").");
        return toDetail(t);
    }

    /** Teslim dosyasını indirme (yalnızca grup üyeleri, görevin danışmanı ve yönetici). */
    @Transactional(readOnly = true)
    public DownloadedFile download(CurrentUser cu, Integer submissionId) {
        Submission s = submissions.findById(submissionId).orElseThrow(() -> ApiException.notFound("Teslim"));
        requireCanView(s.getTask(), cu);
        Resource resource = storage.load(s.getFilePath());
        return new DownloadedFile(resource, s.getFileName());
    }

    /** FR-15: danışman en son teslimi onaylar veya düzeltme ister. */
    @Transactional
    public TaskDetail giveFeedback(CurrentUser cu, Integer submissionId, FeedbackRequest req) {
        Submission s = submissions.findById(submissionId).orElseThrow(() -> ApiException.notFound("Teslim"));
        Task t = s.getTask();
        if (!(cu.isAdvisor() && t.getAdvisor().getId().equals(cu.id()))) {
            throw ApiException.forbidden("Yalnızca görevi atayan danışman değerlendirme yapabilir.");
        }
        if (s.getVersionNo() != submissions.maxVersion(t.getId())) {
            throw ApiException.conflict("NOT_LATEST", "Yalnızca en son teslim değerlendirilebilir.");
        }
        if (feedbacks.existsForSubmission(s.getId())) {
            throw ApiException.conflict("ALREADY_REVIEWED", "Bu teslim zaten değerlendirildi.");
        }
        String comment = req.comment() == null ? null : req.comment().trim();
        if (req.decision() == FeedbackDecision.DUZELTME && (comment == null || comment.isEmpty())) {
            throw ApiException.badRequest("Düzeltme isteğinde açıklama zorunludur.");
        }
        Feedback f = new Feedback();
        f.setSubmission(s);
        f.setAdvisor(users.getReferenceById(cu.id()));
        f.setDecision(req.decision());
        f.setComment(comment == null || comment.isEmpty() ? null : comment);
        feedbacks.saveAndFlush(f);
        boolean approved = req.decision() == FeedbackDecision.ONAY;
        t.setStatus(approved ? TaskStatus.ONAYLANDI : TaskStatus.DUZELTME_ISTENDI);
        notifications.notifyGroup(t.getGroup(), "GERI_BILDIRIM",
                "\"" + t.getTitle() + "\" teslimi " + (approved ? "onaylandı." : "için düzeltme istendi: " + comment));
        return toDetail(t);
    }

    // ------------------------------------------------------------------ yardımcılar

    public record DownloadedFile(Resource resource, String fileName) {
    }

    private Task find(Integer id) {
        return tasks.findById(id).orElseThrow(() -> ApiException.notFound("Görev"));
    }

    private void requireCanView(Task t, CurrentUser cu) {
        boolean taskAdvisor = cu.isAdvisor() && t.getAdvisor().getId().equals(cu.id());
        if (!(taskAdvisor || guard.isMember(t.getGroup(), cu) || guard.isGroupAdvisor(t.getGroup(), cu) || cu.isAdmin())) {
            throw ApiException.forbidden("Bu göreve erişim yetkiniz yok.");
        }
    }

    private TaskResponse toResponse(Task t, int submissionCount) {
        return new TaskResponse(t.getId(), t.getGroup().getId(), t.getGroup().getName(), t.getTitle(),
                t.getDescription(), t.getDueDate(), t.getStatus(), t.getCreatedAt(), submissionCount);
    }

    private TaskDetail toDetail(Task t) {
        List<Submission> subs = submissions.findByTask(t.getId());
        Map<Integer, Feedback> fbBySubmission = feedbacks.findByTask(t.getId()).stream()
                .collect(Collectors.toMap(f -> f.getSubmission().getId(), Function.identity(), (a, b) -> b));
        List<SubmissionResponse> subResponses = subs.stream().map(s -> {
            Feedback f = fbBySubmission.get(s.getId());
            FeedbackResponse fr = f == null ? null : new FeedbackResponse(f.getId(), f.getDecision(), f.getComment(),
                    f.getAdvisor().getFullName(), f.getCreatedAt());
            return new SubmissionResponse(s.getId(), s.getVersionNo(), s.getFileName(), s.getFileSizeKb(), s.isLate(),
                    s.getSubmittedAt(), s.getSubmittedBy().getFullName(), fr);
        }).toList();
        return new TaskDetail(toResponse(t, subs.size()), subResponses);
    }

    /** Görüntülenecek dosya adı (yol bilgisi atılır). */
    static final class FileStorageServiceNames {
        private FileStorageServiceNames() {
        }

        static String displayName(String original) {
            if (original == null || original.isBlank()) {
                return "dosya";
            }
            String n = original.replace('\\', '/');
            n = n.substring(n.lastIndexOf('/') + 1);
            return n.length() > 255 ? n.substring(n.length() - 255) : n;
        }
    }
}
