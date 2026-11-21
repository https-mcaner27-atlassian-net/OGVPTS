package edu.maltepe.ogvpts.config;

import edu.maltepe.ogvpts.model.AvailabilitySlot;
import edu.maltepe.ogvpts.model.GroupMember;
import edu.maltepe.ogvpts.model.Meeting;
import edu.maltepe.ogvpts.model.Project;
import edu.maltepe.ogvpts.model.ProjectGroup;
import edu.maltepe.ogvpts.model.ProjectMilestone;
import edu.maltepe.ogvpts.model.Task;
import edu.maltepe.ogvpts.model.User;
import edu.maltepe.ogvpts.model.enums.MeetingStatus;
import edu.maltepe.ogvpts.model.enums.ProjectStatus;
import edu.maltepe.ogvpts.model.enums.TaskStatus;
import edu.maltepe.ogvpts.model.enums.UserRole;
import edu.maltepe.ogvpts.repository.AvailabilitySlotRepository;
import edu.maltepe.ogvpts.repository.GroupMemberRepository;
import edu.maltepe.ogvpts.repository.MeetingRepository;
import edu.maltepe.ogvpts.repository.ProjectGroupRepository;
import edu.maltepe.ogvpts.repository.ProjectMilestoneRepository;
import edu.maltepe.ogvpts.repository.ProjectRepository;
import edu.maltepe.ogvpts.repository.TaskRepository;
import edu.maltepe.ogvpts.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Demo verisi — yalnızca "demo" profiliyle çalışır ve yalnızca kullanıcı tablosu boşsa veri ekler.
 *
 * Çalıştırma: IntelliJ → Run Configuration → Active profiles: demo
 *             ya da: mvn spring-boot:run -Dspring-boot.run.profiles=demo
 *
 * Tüm demo hesaplarının şifresi: Demo1234!  (yalnızca yerel deneme içindir)
 */
@Component
@Profile("demo")
@Order(1) // AdminBootstrap'tan önce: bkz. AdminBootstrap açıklaması
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
    public static final String DEMO_PASSWORD = "Demo1234!";

    private final UserRepository users;
    private final ProjectGroupRepository groups;
    private final GroupMemberRepository groupMembers;
    private final ProjectRepository projects;
    private final ProjectMilestoneRepository milestones;
    private final AvailabilitySlotRepository slots;
    private final MeetingRepository meetings;
    private final TaskRepository tasks;
    private final PasswordEncoder passwordEncoder;

    public DemoDataSeeder(UserRepository users, ProjectGroupRepository groups, GroupMemberRepository groupMembers,
                          ProjectRepository projects, ProjectMilestoneRepository milestones,
                          AvailabilitySlotRepository slots, MeetingRepository meetings, TaskRepository tasks,
                          PasswordEncoder passwordEncoder) {
        this.users = users;
        this.groups = groups;
        this.groupMembers = groupMembers;
        this.projects = projects;
        this.milestones = milestones;
        this.slots = slots;
        this.meetings = meetings;
        this.tasks = tasks;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.count() > 0) {
            log.info("Demo verisi atlandı: kullanıcı tablosu boş değil.");
            return;
        }
        String hash = passwordEncoder.encode(DEMO_PASSWORD);

        user("Bölüm Sekreterliği", "yonetici@ogvpts.test", UserRole.YONETICI, hash);
        User ayse = user("Dr. Ayşe Yıldız", "ayse.yildiz@ogvpts.test", UserRole.DANISMAN, hash);
        User emre = user("Dr. Emre Kaya", "emre.kaya@ogvpts.test", UserRole.DANISMAN, hash);
        User ali = user("Ali Demir", "ali.demir@ogvpts.test", UserRole.OGRENCI, hash);
        User zeynep = user("Zeynep Çelik", "zeynep.celik@ogvpts.test", UserRole.OGRENCI, hash);
        User can = user("Can Şahin", "can.sahin@ogvpts.test", UserRole.OGRENCI, hash);
        user("Elif Aydın", "elif.aydin@ogvpts.test", UserRole.OGRENCI, hash); // grubu yok: grup oluşturmayı denemek için

        // Grup 1: Ali + Zeynep, danışman Ayşe Yıldız, projesi ve görevi var
        ProjectGroup g1 = group("Akıllı Kampüs Ekibi", ayse);
        member(g1, ali);
        member(g1, zeynep);
        Project p1 = new Project();
        p1.setGroup(g1);
        p1.setTitle("Akıllı Kampüs Otopark Takip Sistemi");
        p1.setDescription("Kampüs otoparklarındaki boş yerleri sensör verisiyle gösteren web uygulaması.");
        p1.setStatus(ProjectStatus.DEVAM_EDIYOR);
        p1 = projects.saveAndFlush(p1);
        LocalDate today = LocalDate.now();
        milestone(p1, "Gereksinim analizi", today.minusDays(10), ProjectStatus.TAMAMLANDI);
        milestone(p1, "Veritabanı tasarımı", today.plusDays(2), ProjectStatus.DEVAM_EDIYOR);
        milestone(p1, "Prototip", today.plusDays(21), ProjectStatus.BASLAMADI);

        Task t1 = new Task();
        t1.setGroup(g1);
        t1.setAdvisor(ayse);
        t1.setTitle("ER diyagramını teslim edin");
        t1.setDescription("Veritabanı tasarımınızı PDF olarak yükleyin.");
        t1.setDueDate(today.plusDays(5));
        t1.setStatus(TaskStatus.ATANDI);
        tasks.save(t1);

        // Grup 2: yalnızca Can, danışman Emre Kaya, henüz projesi yok
        ProjectGroup g2 = group("Veri Madencileri", emre);
        member(g2, can);

        // Danışman müsaitlikleri (önümüzdeki günler) ve bir bekleyen talep
        AvailabilitySlot s1 = slot(ayse, today.plusDays(3), 10, 0);
        slot(ayse, today.plusDays(3), 11, 0);
        slot(ayse, today.plusDays(4), 14, 0);
        slot(emre, today.plusDays(2), 13, 30);
        slot(emre, today.plusDays(5), 9, 0);

        Meeting m = new Meeting();
        m.setSlot(s1);
        m.setGroup(g1);
        m.setRequestedBy(ali);
        m.setTopic("Veritabanı tasarımı geri bildirimi");
        m.setStatus(MeetingStatus.BEKLEMEDE);
        meetings.save(m);

        log.info("Demo verisi oluşturuldu. Giriş: yonetici@ogvpts.test / ayse.yildiz@ogvpts.test / ali.demir@ogvpts.test — şifre {}", DEMO_PASSWORD);
    }

    private User user(String name, String email, UserRole role, String hash) {
        User u = new User();
        u.setFullName(name);
        u.setEmail(email);
        u.setRole(role);
        u.setPasswordHash(hash);
        return users.saveAndFlush(u);
    }

    private ProjectGroup group(String name, User advisor) {
        ProjectGroup g = new ProjectGroup();
        g.setName(name);
        g.setAdvisor(advisor);
        return groups.saveAndFlush(g);
    }

    private void member(ProjectGroup g, User u) {
        groupMembers.saveAndFlush(new GroupMember(g, u));
    }

    private void milestone(Project p, String title, LocalDate due, ProjectStatus status) {
        ProjectMilestone m = new ProjectMilestone();
        m.setProject(p);
        m.setTitle(title);
        m.setDueDate(due);
        m.setStatus(status);
        milestones.save(m);
    }

    private AvailabilitySlot slot(User advisor, LocalDate day, int hour, int minute) {
        AvailabilitySlot s = new AvailabilitySlot();
        s.setAdvisor(advisor);
        LocalDateTime start = LocalDateTime.of(day, LocalTime.of(hour, minute));
        s.setStartTime(start);
        s.setEndTime(start.plusMinutes(30));
        return slots.saveAndFlush(s);
    }
}
