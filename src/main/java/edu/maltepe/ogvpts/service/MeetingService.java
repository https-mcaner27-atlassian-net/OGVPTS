package edu.maltepe.ogvpts.service;

import edu.maltepe.ogvpts.common.ApiException;
import edu.maltepe.ogvpts.dto.MeetingDtos.MeetingRequest;
import edu.maltepe.ogvpts.dto.MeetingDtos.MeetingResponse;
import edu.maltepe.ogvpts.dto.MeetingDtos.NoteRequest;
import edu.maltepe.ogvpts.dto.MeetingDtos.NoteResponse;
import edu.maltepe.ogvpts.model.AvailabilitySlot;
import edu.maltepe.ogvpts.model.Meeting;
import edu.maltepe.ogvpts.model.MeetingNote;
import edu.maltepe.ogvpts.model.ProjectGroup;
import edu.maltepe.ogvpts.model.User;
import edu.maltepe.ogvpts.model.enums.MeetingStatus;
import edu.maltepe.ogvpts.repository.AvailabilitySlotRepository;
import edu.maltepe.ogvpts.repository.MeetingNoteRepository;
import edu.maltepe.ogvpts.repository.MeetingRepository;
import edu.maltepe.ogvpts.repository.UserRepository;
import edu.maltepe.ogvpts.security.CurrentUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Randevu (görüşme) akışı — Sprint 2'deki "Randevu State Diagram"a göre:
 *
 * <pre>
 *   [öğrenci talep eder] → BEKLEMEDE
 *   BEKLEMEDE          → ONAYLANDI          (danışman onaylar; çakışma yoksa — BR-02)
 *   BEKLEMEDE          → REDDEDILDI         (danışman reddeder)
 *   BEKLEMEDE          → YENI_SAAT_ONERILDI (danışman başka bir saat önerir)
 *   YENI_SAAT_ONERILDI → ONAYLANDI          (öğrenci kabul eder)
 *   YENI_SAAT_ONERILDI → REDDEDILDI         (öğrenci reddeder)
 *   ONAYLANDI          → TAMAMLANDI         (görüşme yapılır, not girilir — FR-04)
 *   BEKLEMEDE / YENI_SAAT_ONERILDI / ONAYLANDI → IPTAL  (onaylıysa en az 24 saat önce — BR-03)
 * </pre>
 */
@Service
public class MeetingService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final Set<MeetingStatus> ACTIVE = EnumSet.of(MeetingStatus.ONAYLANDI, MeetingStatus.TAMAMLANDI);
    private static final Set<MeetingStatus> CANCELLABLE = EnumSet.of(
            MeetingStatus.BEKLEMEDE, MeetingStatus.YENI_SAAT_ONERILDI, MeetingStatus.ONAYLANDI);

    private final MeetingRepository meetings;
    private final AvailabilitySlotRepository slots;
    private final MeetingNoteRepository notes;
    private final UserRepository users;
    private final AccessGuard guard;
    private final NotificationService notifications;
    private final long cancelMinHours;

    public MeetingService(MeetingRepository meetings, AvailabilitySlotRepository slots,
                          MeetingNoteRepository notes, UserRepository users,
                          AccessGuard guard, NotificationService notifications,
                          @Value("${ogvpts.meeting.cancel-min-hours}") long cancelMinHours) {
        this.meetings = meetings;
        this.slots = slots;
        this.notes = notes;
        this.users = users;
        this.guard = guard;
        this.notifications = notifications;
        this.cancelMinHours = cancelMinHours;
    }

    // ------------------------------------------------------------------ öğrenci

    /** FR-02: öğrenci, grubunun danışmanının boş bir saatine talep gönderir. */
    @Transactional
    public MeetingResponse request(CurrentUser cu, MeetingRequest req) {
        ProjectGroup group = guard.requireOwnGroup(cu);
        if (group.getAdvisor() == null) {
            throw ApiException.conflict("NO_ADVISOR", "Grubunuza henüz danışman atanmadı.");
        }
        AvailabilitySlot slot = slots.findById(req.slotId()).orElseThrow(() -> ApiException.notFound("Saat"));
        // FR-10: öğrenci yalnızca atanmış danışmanından randevu isteyebilir.
        if (!slot.getAdvisor().getId().equals(group.getAdvisor().getId())) {
            throw ApiException.forbidden("Yalnızca kendi danışmanınızdan randevu isteyebilirsiniz.");
        }
        requireFuture(slot);
        if (meetings.existsOnSlot(slot.getId(), -1, AvailabilityService.OPEN_STATUSES)) {
            throw ApiException.conflict("SLOT_TAKEN", "Bu saat artık müsait değil; lütfen başka bir saat seçin.");
        }
        Meeting m = new Meeting();
        m.setSlot(slot);
        m.setGroup(group);
        m.setRequestedBy(users.getReferenceById(cu.id()));
        m.setTopic(req.topic().trim());
        m.setStatus(MeetingStatus.BEKLEMEDE);
        m = meetings.saveAndFlush(m);
        notifications.notify(slot.getAdvisor(), "RANDEVU",
                "\"" + group.getName() + "\" grubu " + when(slot) + " için randevu talep etti: " + m.getTopic());
        return toResponse(m, cu, null);
    }

    /** Öğrenci danışmanın önerdiği yeni saati kabul eder. */
    @Transactional
    public MeetingResponse acceptProposal(CurrentUser cu, Integer id) {
        Meeting m = find(id);
        guard.requireMember(m.getGroup(), cu);
        requireStatus(m, MeetingStatus.YENI_SAAT_ONERILDI);
        requireFuture(m.getSlot());
        requireNoActiveConflict(m);
        m.setStatus(MeetingStatus.ONAYLANDI);
        meetings.saveAndFlush(m);
        notifications.notify(advisorOf(m), "RANDEVU",
                "\"" + m.getGroup().getName() + "\" grubu önerdiğiniz " + when(m.getSlot()) + " saatini kabul etti.");
        return toResponse(m, cu, null);
    }

    /** Öğrenci önerilen saati reddeder. */
    @Transactional
    public MeetingResponse declineProposal(CurrentUser cu, Integer id) {
        Meeting m = find(id);
        guard.requireMember(m.getGroup(), cu);
        requireStatus(m, MeetingStatus.YENI_SAAT_ONERILDI);
        m.setStatus(MeetingStatus.REDDEDILDI);
        notifications.notify(advisorOf(m), "RANDEVU",
                "\"" + m.getGroup().getName() + "\" grubu önerdiğiniz " + when(m.getSlot()) + " saatini reddetti.");
        return toResponse(m, cu, null);
    }

    // ------------------------------------------------------------------ danışman (FR-03)

    @Transactional
    public MeetingResponse approve(CurrentUser cu, Integer id) {
        Meeting m = find(id);
        requireMeetingAdvisor(m, cu);
        requireStatus(m, MeetingStatus.BEKLEMEDE);
        requireFuture(m.getSlot());
        requireNoActiveConflict(m);
        m.setStatus(MeetingStatus.ONAYLANDI);
        // saveAndFlush: aynı anda gelen iki onayı veritabanındaki benzersiz indeks yakalar.
        meetings.saveAndFlush(m);
        notifications.notifyGroup(m.getGroup(), "RANDEVU",
                "Randevu talebiniz onaylandı: " + when(m.getSlot()) + " – " + m.getTopic());
        return toResponse(m, cu, null);
    }

    @Transactional
    public MeetingResponse reject(CurrentUser cu, Integer id) {
        Meeting m = find(id);
        requireMeetingAdvisor(m, cu);
        requireStatus(m, MeetingStatus.BEKLEMEDE);
        m.setStatus(MeetingStatus.REDDEDILDI);
        notifications.notifyGroup(m.getGroup(), "RANDEVU",
                "Randevu talebiniz reddedildi: " + when(m.getSlot()) + " – " + m.getTopic());
        return toResponse(m, cu, null);
    }

    /** Danışman, kendi boş saatlerinden birini önerir. */
    @Transactional
    public MeetingResponse proposeNewTime(CurrentUser cu, Integer id, Integer newSlotId) {
        Meeting m = find(id);
        requireMeetingAdvisor(m, cu);
        requireStatus(m, MeetingStatus.BEKLEMEDE);
        AvailabilitySlot slot = slots.findById(newSlotId).orElseThrow(() -> ApiException.notFound("Saat"));
        if (!slot.getAdvisor().getId().equals(cu.id())) {
            throw ApiException.forbidden("Yalnızca kendi müsaitlik saatlerinizden birini önerebilirsiniz.");
        }
        requireFuture(slot);
        if (meetings.existsOnSlot(slot.getId(), m.getId(), AvailabilityService.OPEN_STATUSES)) {
            throw ApiException.conflict("SLOT_TAKEN", "Önerdiğiniz saat dolu.");
        }
        m.setSlot(slot);
        m.setStatus(MeetingStatus.YENI_SAAT_ONERILDI);
        meetings.saveAndFlush(m);
        notifications.notifyGroup(m.getGroup(), "RANDEVU",
                "Danışmanınız \"" + m.getTopic() + "\" görüşmesi için yeni saat önerdi: " + when(slot)
                        + ". Lütfen kabul edin veya reddedin.");
        return toResponse(m, cu, null);
    }

    /** Görüşme yapıldı: ONAYLANDI → TAMAMLANDI (başlangıç saati geçmiş olmalı). */
    @Transactional
    public MeetingResponse complete(CurrentUser cu, Integer id) {
        Meeting m = find(id);
        requireMeetingAdvisor(m, cu);
        requireStatus(m, MeetingStatus.ONAYLANDI);
        if (m.getSlot().getStartTime().isAfter(LocalDateTime.now())) {
            throw ApiException.conflict("NOT_STARTED", "Henüz başlamamış bir görüşme tamamlandı olarak işaretlenemez.");
        }
        m.setStatus(MeetingStatus.TAMAMLANDI);
        meetings.saveAndFlush(m);
        return toResponse(m, cu, null);
    }

    /** FR-04 + BR-04: notu yalnızca görüşmeyi yapan danışman girer/düzenler. */
    @Transactional
    public MeetingResponse saveNote(CurrentUser cu, Integer id, NoteRequest req) {
        Meeting m = find(id);
        requireMeetingAdvisor(m, cu);
        requireStatus(m, MeetingStatus.TAMAMLANDI);
        MeetingNote note = notes.findByMeeting(m.getId()).orElseGet(MeetingNote::new);
        boolean isNew = note.getId() == null;
        note.setMeeting(m);
        note.setAdvisor(users.getReferenceById(cu.id()));
        note.setNote(req.note().trim());
        note.setDecisions(req.decisions() == null || req.decisions().isBlank() ? null : req.decisions().trim());
        note = notes.saveAndFlush(note);
        notifications.notifyGroup(m.getGroup(), "RANDEVU",
                (isNew ? "Görüşme notu eklendi: " : "Görüşme notu güncellendi: ") + m.getTopic());
        return toResponse(m, cu, note);
    }

    // ------------------------------------------------------------------ ortak

    /** BR-03: onaylı görüşme en az 24 saat önceden iptal edilebilir. */
    @Transactional
    public MeetingResponse cancel(CurrentUser cu, Integer id) {
        Meeting m = find(id);
        boolean student = guard.isMember(m.getGroup(), cu);
        boolean advisor = isMeetingAdvisor(m, cu);
        if (!student && !advisor) {
            throw ApiException.forbidden("Bu görüşmeyi iptal etme yetkiniz yok.");
        }
        if (!CANCELLABLE.contains(m.getStatus())) {
            throw ApiException.conflict("INVALID_STATUS", "Bu görüşme artık iptal edilemez.");
        }
        if (m.getStatus() == MeetingStatus.ONAYLANDI && !canCancelNow(m)) {
            throw ApiException.conflict("CANCEL_TOO_LATE",
                    "Onaylı görüşme en geç " + cancelMinHours + " saat öncesine kadar iptal edilebilir (BR-03).");
        }
        m.setStatus(MeetingStatus.IPTAL);
        meetings.saveAndFlush(m);
        String msg = "\"" + m.getTopic() + "\" görüşmesi (" + when(m.getSlot()) + ") " + cu.name() + " tarafından iptal edildi.";
        if (student) {
            notifications.notify(advisorOf(m), "RANDEVU", msg);
        } else {
            notifications.notifyGroup(m.getGroup(), "RANDEVU", msg);
        }
        return toResponse(m, cu, null);
    }

    /**
     * FR-09: kullanıcının görüşmeleri (öğrenci → grubunun, danışman → kendisinin).
     * İsteğe bağlı filtreler: durum ve tarih aralığı.
     */
    @Transactional(readOnly = true)
    public List<MeetingResponse> list(CurrentUser cu, MeetingStatus status, LocalDate from, LocalDate to) {
        List<Meeting> list;
        if (cu.isStudent()) {
            list = meetings.findByGroup(guard.requireOwnGroup(cu).getId());
        } else if (cu.isAdvisor()) {
            list = meetings.findByAdvisor(cu.id());
        } else {
            throw ApiException.forbidden("Görüşme listesi öğrenci ve danışmanlar içindir.");
        }
        List<Meeting> filtered = list.stream()
                .filter(m -> status == null || m.getStatus() == status)
                .filter(m -> from == null || !m.getSlot().getStartTime().toLocalDate().isBefore(from))
                .filter(m -> to == null || !m.getSlot().getStartTime().toLocalDate().isAfter(to))
                .toList();
        Map<Integer, MeetingNote> noteMap = filtered.isEmpty() ? Map.of()
                : notes.findByMeetings(filtered.stream().map(Meeting::getId).toList()).stream()
                .collect(Collectors.toMap(n -> n.getMeeting().getId(), Function.identity()));
        return filtered.stream().map(m -> toResponse(m, cu, noteMap.get(m.getId()))).toList();
    }

    @Transactional(readOnly = true)
    public MeetingResponse get(CurrentUser cu, Integer id) {
        Meeting m = find(id);
        if (!(guard.isMember(m.getGroup(), cu) || isMeetingAdvisor(m, cu) || cu.isAdmin())) {
            throw ApiException.forbidden("Bu görüşmeye erişim yetkiniz yok.");
        }
        return toResponse(m, cu, notes.findByMeeting(id).orElse(null));
    }

    // ------------------------------------------------------------------ yardımcılar

    private Meeting find(Integer id) {
        return meetings.findById(id).orElseThrow(() -> ApiException.notFound("Görüşme"));
    }

    /** Görüşmenin danışmanı = saatin sahibi (grubun danışmanı sonradan değişse bile). */
    private User advisorOf(Meeting m) {
        return m.getSlot().getAdvisor();
    }

    private boolean isMeetingAdvisor(Meeting m, CurrentUser cu) {
        return cu.isAdvisor() && advisorOf(m).getId().equals(cu.id());
    }

    private void requireMeetingAdvisor(Meeting m, CurrentUser cu) {
        if (!isMeetingAdvisor(m, cu)) {
            throw ApiException.forbidden("Bu işlemi yalnızca görüşmenin danışmanı yapabilir.");
        }
    }

    private static void requireStatus(Meeting m, MeetingStatus expected) {
        if (m.getStatus() != expected) {
            throw ApiException.conflict("INVALID_STATUS", "Bu işlem görüşmenin mevcut durumunda yapılamaz.");
        }
    }

    private static void requireFuture(AvailabilitySlot slot) {
        if (!slot.getStartTime().isAfter(LocalDateTime.now())) {
            throw ApiException.conflict("SLOT_PAST", "Geçmiş bir saat için işlem yapılamaz.");
        }
    }

    /** BR-02: aynı saatte onaylı başka görüşme olamaz. */
    private void requireNoActiveConflict(Meeting m) {
        if (meetings.existsOnSlot(m.getSlot().getId(), m.getId(), ACTIVE)) {
            throw ApiException.conflict("SLOT_CONFLICT", "Bu saatte onaylı başka bir görüşme var (BR-02).");
        }
    }

    private boolean canCancelNow(Meeting m) {
        return LocalDateTime.now().plusHours(cancelMinHours).isBefore(m.getSlot().getStartTime());
    }

    private static String when(AvailabilitySlot s) {
        return s.getStartTime().format(FMT);
    }

    private MeetingResponse toResponse(Meeting m, CurrentUser cu, MeetingNote note) {
        AvailabilitySlot s = m.getSlot();
        User advisor = s.getAdvisor();
        boolean canCancel = CANCELLABLE.contains(m.getStatus())
                && (m.getStatus() != MeetingStatus.ONAYLANDI || canCancelNow(m));
        NoteResponse nr = note == null ? null
                : new NoteResponse(note.getNote(), note.getDecisions(), note.getAdvisor().getFullName(), note.getUpdatedAt());
        return new MeetingResponse(m.getId(), m.getTopic(), m.getStatus(), s.getStartTime(), s.getEndTime(),
                m.getGroup().getId(), m.getGroup().getName(), advisor.getId(), advisor.getFullName(),
                m.getRequestedBy().getFullName(), m.getCreatedAt(), canCancel, nr);
    }
}
