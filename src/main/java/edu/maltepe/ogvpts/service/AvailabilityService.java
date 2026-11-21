package edu.maltepe.ogvpts.service;

import edu.maltepe.ogvpts.common.ApiException;
import edu.maltepe.ogvpts.dto.MeetingDtos.SlotRequest;
import edu.maltepe.ogvpts.dto.MeetingDtos.SlotResponse;
import edu.maltepe.ogvpts.model.AvailabilitySlot;
import edu.maltepe.ogvpts.model.Meeting;
import edu.maltepe.ogvpts.model.ProjectGroup;
import edu.maltepe.ogvpts.model.enums.MeetingStatus;
import edu.maltepe.ogvpts.repository.AvailabilitySlotRepository;
import edu.maltepe.ogvpts.repository.MeetingRepository;
import edu.maltepe.ogvpts.repository.UserRepository;
import edu.maltepe.ogvpts.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Danışmanın müsait saatleri.
 * Öğrenci randevu talebini bu saatlerden birini seçerek yapar (FR-02).
 */
@Service
public class AvailabilityService {

    /** Bir saat üzerinde bu durumlardan birinde görüşme varsa saat "dolu" sayılır. */
    public static final Set<MeetingStatus> OPEN_STATUSES = EnumSet.of(
            MeetingStatus.BEKLEMEDE, MeetingStatus.YENI_SAAT_ONERILDI,
            MeetingStatus.ONAYLANDI, MeetingStatus.TAMAMLANDI);

    private static final Duration MIN_LENGTH = Duration.ofMinutes(10);
    private static final Duration MAX_LENGTH = Duration.ofHours(4);

    private final AvailabilitySlotRepository slots;
    private final MeetingRepository meetings;
    private final UserRepository users;
    private final AccessGuard guard;

    public AvailabilityService(AvailabilitySlotRepository slots, MeetingRepository meetings,
                               UserRepository users, AccessGuard guard) {
        this.slots = slots;
        this.meetings = meetings;
        this.users = users;
        this.guard = guard;
    }

    /** Danışmanın bugünden itibaren saatleri ve her saatin doluluk durumu. */
    @Transactional(readOnly = true)
    public List<SlotResponse> mySlots(CurrentUser cu) {
        guard.requireAdvisor(cu);
        LocalDateTime from = LocalDateTime.now().toLocalDate().atStartOfDay();
        Map<Integer, Meeting> openBySlot = meetings.findByAdvisor(cu.id()).stream()
                .filter(m -> OPEN_STATUSES.contains(m.getStatus()))
                .collect(Collectors.toMap(m -> m.getSlot().getId(), Function.identity(), (a, b) -> a));
        return slots.findByAdvisorFrom(cu.id(), from).stream()
                .map(s -> {
                    Meeting m = openBySlot.get(s.getId());
                    return new SlotResponse(s.getId(), s.getStartTime(), s.getEndTime(),
                            m == null ? null : m.getStatus(), m == null ? null : m.getGroup().getName());
                })
                .toList();
    }

    @Transactional
    public SlotResponse create(CurrentUser cu, SlotRequest req) {
        guard.requireAdvisor(cu);
        if (!req.end().isAfter(req.start())) {
            throw ApiException.badRequest("Bitiş zamanı başlangıçtan sonra olmalıdır.");
        }
        if (!req.start().isAfter(LocalDateTime.now())) {
            throw ApiException.badRequest("Geçmiş bir zamana müsaitlik eklenemez.");
        }
        Duration length = Duration.between(req.start(), req.end());
        if (length.compareTo(MIN_LENGTH) < 0 || length.compareTo(MAX_LENGTH) > 0) {
            throw ApiException.badRequest("Görüşme süresi 10 dakika ile 4 saat arasında olmalıdır.");
        }
        // BR-02: aynı danışmanın çakışan iki saati olamaz.
        if (slots.existsOverlapping(cu.id(), req.start(), req.end())) {
            throw ApiException.conflict("SLOT_OVERLAP", "Bu aralık mevcut bir müsaitlik saatinizle çakışıyor.");
        }
        AvailabilitySlot s = new AvailabilitySlot();
        s.setAdvisor(users.getReferenceById(cu.id()));
        s.setStartTime(req.start());
        s.setEndTime(req.end());
        s = slots.saveAndFlush(s);
        return new SlotResponse(s.getId(), s.getStartTime(), s.getEndTime(), null, null);
    }

    @Transactional
    public void delete(CurrentUser cu, Integer id) {
        AvailabilitySlot s = slots.findById(id).orElseThrow(() -> ApiException.notFound("Müsaitlik"));
        if (!s.getAdvisor().getId().equals(cu.id())) {
            throw ApiException.forbidden("Bu müsaitlik size ait değil.");
        }
        // Görüşme kayıtları geçmiş olarak saklandığı için, bağlı görüşmesi olan saat silinemez.
        if (meetings.existsAnyOnSlot(id)) {
            throw ApiException.conflict("SLOT_IN_USE", "Bu saate bağlı bir görüşme kaydı olduğu için silinemez.");
        }
        slots.delete(s);
    }

    /**
     * FR-02 + FR-10: öğrenci yalnızca KENDİ grubunun danışmanının boş saatlerini görür.
     */
    @Transactional(readOnly = true)
    public List<SlotResponse> availableForStudent(CurrentUser cu) {
        ProjectGroup group = guard.requireOwnGroup(cu);
        if (group.getAdvisor() == null) {
            throw ApiException.conflict("NO_ADVISOR", "Grubunuza henüz danışman atanmadı. Yöneticinizle iletişime geçin.");
        }
        return free(group.getAdvisor().getId());
    }

    /** Danışmanın kendi boş saatleri (yeni saat önerirken kullanılır). */
    @Transactional(readOnly = true)
    public List<SlotResponse> myFreeSlots(CurrentUser cu) {
        guard.requireAdvisor(cu);
        return free(cu.id());
    }

    private List<SlotResponse> free(Integer advisorId) {
        return slots.findFree(advisorId, LocalDateTime.now(), OPEN_STATUSES).stream()
                .sorted(Comparator.comparing(AvailabilitySlot::getStartTime))
                .map(s -> new SlotResponse(s.getId(), s.getStartTime(), s.getEndTime(), null, null))
                .toList();
    }
}
