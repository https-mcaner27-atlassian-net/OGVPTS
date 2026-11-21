package edu.maltepe.ogvpts.config;

import edu.maltepe.ogvpts.model.User;
import edu.maltepe.ogvpts.model.enums.UserRole;
import edu.maltepe.ogvpts.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * İlk kurulum: veritabanında hiç yönetici yoksa ve ADMIN_EMAIL / ADMIN_PASSWORD
 * ayarlanmışsa bir yönetici hesabı oluşturur. Diğer kullanıcıları bu yönetici ekler.
 */
@Component
@Order(1)
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;
    private final String fullName;

    public AdminBootstrap(UserRepository users, PasswordEncoder passwordEncoder,
                          @Value("${ogvpts.bootstrap-admin.email}") String email,
                          @Value("${ogvpts.bootstrap-admin.password}") String password,
                          @Value("${ogvpts.bootstrap-admin.full-name}") String fullName) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
        this.fullName = fullName;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (users.existsByRole(UserRole.YONETICI)) {
            return;
        }
        if (email == null || email.isBlank() || password == null || password.length() < 8) {
            log.warn("Sistemde yönetici yok. ADMIN_EMAIL ve ADMIN_PASSWORD (en az 8 karakter) ayarlayın "
                    + "ya da demo verisi için 'demo' profiliyle başlatın.");
            return;
        }
        User admin = new User();
        admin.setFullName(fullName);
        admin.setEmail(email.trim().toLowerCase());
        admin.setRole(UserRole.YONETICI);
        admin.setPasswordHash(passwordEncoder.encode(password));
        users.save(admin);
        log.info("İlk yönetici hesabı oluşturuldu: {}", admin.getEmail());
    }
}
