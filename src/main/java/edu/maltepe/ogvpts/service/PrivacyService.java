package edu.maltepe.ogvpts.service;

import edu.maltepe.ogvpts.model.User;
import edu.maltepe.ogvpts.model.enums.UserRole;
import edu.maltepe.ogvpts.repository.UserRepository;
import edu.maltepe.ogvpts.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * NFR-03 KVKK: kullanıcı kişisel verilerinin silinmesini talep edebilir.
 * Talep tüm yöneticilere bildirim olarak iletilir; yönetici talebi değerlendirir.
 */
@Service
public class PrivacyService {

    private final UserRepository users;
    private final NotificationService notifications;

    public PrivacyService(UserRepository users, NotificationService notifications) {
        this.users = users;
        this.notifications = notifications;
    }

    @Transactional
    public void requestDataDeletion(CurrentUser cu) {
        User me = users.findById(cu.id()).orElseThrow();
        String msg = "KVKK veri silme talebi: " + me.getFullName() + " (" + me.getEmail() + ")";
        for (User admin : users.findByRole(UserRole.YONETICI)) {
            notifications.notify(admin, "KVKK", msg);
        }
        notifications.notify(me, "KVKK", "Veri silme talebiniz yöneticiye iletildi.");
    }
}
