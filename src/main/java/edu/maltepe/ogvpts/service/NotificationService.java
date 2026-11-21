package edu.maltepe.ogvpts.service;

import edu.maltepe.ogvpts.common.ApiException;
import edu.maltepe.ogvpts.dto.NotificationDtos.NotificationResponse;
import edu.maltepe.ogvpts.model.GroupMember;
import edu.maltepe.ogvpts.model.Notification;
import edu.maltepe.ogvpts.model.ProjectGroup;
import edu.maltepe.ogvpts.model.User;
import edu.maltepe.ogvpts.repository.GroupMemberRepository;
import edu.maltepe.ogvpts.repository.NotificationRepository;
import edu.maltepe.ogvpts.security.CurrentUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * FR-08: sistem içi bildirim (her zaman) + e-posta (ayarlanmışsa).
 *
 * Bildirim türleri (type sütunu): RANDEVU, RANDEVU_HATIRLATMA, PROJE, GOREV, TESLIM, GERI_BILDIRIM, GRUP, KVKK
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notifications;
    private final GroupMemberRepository groupMembers;
    private final ObjectProvider<JavaMailSender> mailSender;
    private final boolean mailEnabled;
    private final String mailFrom;

    public NotificationService(NotificationRepository notifications,
                               GroupMemberRepository groupMembers,
                               ObjectProvider<JavaMailSender> mailSender,
                               @Value("${ogvpts.mail.enabled}") boolean mailEnabled,
                               @Value("${ogvpts.mail.from}") String mailFrom) {
        this.notifications = notifications;
        this.groupMembers = groupMembers;
        this.mailSender = mailSender;
        this.mailEnabled = mailEnabled;
        this.mailFrom = mailFrom;
    }

    /** Tek bir kullanıcıya bildirim. */
    @Transactional
    public void notify(User user, String type, String message) {
        if (user == null) {
            return;
        }
        Notification n = new Notification();
        n.setUser(user);
        n.setType(type);
        n.setMessage(message.length() > 500 ? message.substring(0, 497) + "..." : message);
        notifications.save(n);
        sendMail(user, message);
    }

    /** Grubun tüm öğrencilerine bildirim. */
    @Transactional
    public void notifyGroup(ProjectGroup group, String type, String message) {
        for (GroupMember gm : groupMembers.findByGroup(group.getId())) {
            notify(gm.getUser(), type, message);
        }
    }

    /** Aynı mesaj daha önce gönderilmediyse gönder (zamanlanmış hatırlatmalar için). */
    @Transactional
    public void notifyOnce(User user, String type, String message) {
        if (!notifications.exists(user.getId(), type, message)) {
            notify(user, type, message);
        }
    }

    private void sendMail(User user, String message) {
        if (!mailEnabled) {
            return;
        }
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            return; // spring.mail.host tanımlı değil
        }
        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(mailFrom);
            mail.setTo(user.getEmail());
            mail.setSubject("OGVPTS bildirimi");
            mail.setText(message);
            sender.send(mail);
        } catch (Exception e) {
            // E-posta gönderilemese bile sistem içi bildirim kaydedildi; işlemi bozmayalım.
            log.warn("E-posta gönderilemedi ({}): {}", user.getEmail(), e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> list(CurrentUser cu) {
        return notifications.findLatest(cu.id(), PageRequest.of(0, 100)).stream()
                .map(n -> new NotificationResponse(n.getId(), n.getType(), n.getMessage(), n.isRead(), n.getCreatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount(CurrentUser cu) {
        return notifications.countUnread(cu.id());
    }

    @Transactional
    public void markRead(CurrentUser cu, Integer id) {
        Notification n = notifications.findById(id).orElseThrow(() -> ApiException.notFound("Bildirim"));
        if (!n.getUser().getId().equals(cu.id())) {
            throw ApiException.forbidden("Bu bildirim size ait değil.");
        }
        n.setRead(true);
    }

    @Transactional
    public void markAllRead(CurrentUser cu) {
        notifications.markAllRead(cu.id());
    }
}
