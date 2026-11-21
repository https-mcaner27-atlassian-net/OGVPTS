package edu.maltepe.ogvpts.service;

import edu.maltepe.ogvpts.model.Meeting;
import edu.maltepe.ogvpts.model.ProjectMilestone;
import edu.maltepe.ogvpts.model.Task;
import edu.maltepe.ogvpts.model.enums.MeetingStatus;
import edu.maltepe.ogvpts.model.enums.ProjectStatus;
import edu.maltepe.ogvpts.model.enums.TaskStatus;
import edu.maltepe.ogvpts.repository.GroupMemberRepository;
import edu.maltepe.ogvpts.repository.MeetingRepository;
import edu.maltepe.ogvpts.repository.ProjectMilestoneRepository;
import edu.maltepe.ogvpts.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * FR-08 hatırlatmaları (zamanlanmış görevler).
 * - Onaylı randevudan 24 saat önce: öğrencilere ve danışmana.
 * - Proje aşaması ve görev son tarihinden 2 gün önce: öğrencilere.
 * Aynı hatırlatma iki kez gönderilmez (NotificationService.notifyOnce).
 *
 * Zamanlayıcı OgvptsApplication'daki @EnableScheduling ile açılır.
 */
@Component
public class ReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReminderScheduler.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final MeetingRepository meetings;
    private final ProjectMilestoneRepository milestones;
    private final TaskRepository tasks;
    private final GroupMemberRepository groupMembers;
    private final NotificationService notifications;

    public ReminderScheduler(MeetingRepository meetings, ProjectMilestoneRepository milestones, TaskRepository tasks,
                             GroupMemberRepository groupMembers, NotificationService notifications) {
        this.meetings = meetings;
        this.milestones = milestones;
        this.tasks = tasks;
        this.groupMembers = groupMembers;
        this.notifications = notifications;
    }

    /** Her 15 dakikada bir: önümüzdeki 24 saat içinde başlayacak onaylı görüşmeler. */
    @Scheduled(cron = "0 */15 * * * *", zone = "Europe/Istanbul")
    @Transactional
    public void meetingReminders() {
        LocalDateTime now = LocalDateTime.now();
        for (Meeting m : meetings.findStartingBetween(MeetingStatus.ONAYLANDI, now, now.plusHours(24))) {
            String msg = "Hatırlatma: \"" + m.getTopic() + "\" görüşmeniz " + m.getSlot().getStartTime().format(FMT) + " tarihinde.";
            notifications.notifyOnce(m.getSlot().getAdvisor(), "RANDEVU_HATIRLATMA", msg);
            groupMembers.findByGroup(m.getGroup().getId())
                    .forEach(gm -> notifications.notifyOnce(gm.getUser(), "RANDEVU_HATIRLATMA", msg));
        }
    }

    /** Her gün 08:00: 2 gün sonra teslim tarihi olan aşamalar ve görevler. */
    @Scheduled(cron = "0 0 8 * * *", zone = "Europe/Istanbul")
    @Transactional
    public void deadlineReminders() {
        LocalDate target = LocalDate.now().plusDays(2);
        for (ProjectMilestone m : milestones.findDueOn(target, ProjectStatus.TAMAMLANDI)) {
            String msg = "Hatırlatma: \"" + m.getTitle() + "\" aşamasının teslim tarihi " + target.format(DATE) + ".";
            groupMembers.findByGroup(m.getProject().getGroup().getId())
                    .forEach(gm -> notifications.notifyOnce(gm.getUser(), "TESLIM_HATIRLATMA", msg));
        }
        for (Task t : tasks.findDueOn(target, TaskStatus.ONAYLANDI)) {
            String msg = "Hatırlatma: \"" + t.getTitle() + "\" görevinin son teslim tarihi " + target.format(DATE) + ".";
            groupMembers.findByGroup(t.getGroup().getId())
                    .forEach(gm -> notifications.notifyOnce(gm.getUser(), "TESLIM_HATIRLATMA", msg));
        }
        log.info("Teslim hatırlatmaları kontrol edildi ({}).", target);
    }
}
