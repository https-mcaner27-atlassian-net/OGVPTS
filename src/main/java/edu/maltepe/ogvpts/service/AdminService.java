package edu.maltepe.ogvpts.service;

import edu.maltepe.ogvpts.common.ApiException;
import edu.maltepe.ogvpts.dto.GroupDtos.AdvisorLoad;
import edu.maltepe.ogvpts.dto.GroupDtos.AssignAdvisorRequest;
import edu.maltepe.ogvpts.dto.GroupDtos.GroupResponse;
import edu.maltepe.ogvpts.dto.UserDtos.CreateUserRequest;
import edu.maltepe.ogvpts.dto.UserDtos.UserAdminView;
import edu.maltepe.ogvpts.dto.UserDtos.UserSummary;
import edu.maltepe.ogvpts.model.ProjectGroup;
import edu.maltepe.ogvpts.model.User;
import edu.maltepe.ogvpts.model.enums.UserRole;
import edu.maltepe.ogvpts.repository.GroupMemberRepository;
import edu.maltepe.ogvpts.repository.ProjectGroupRepository;
import edu.maltepe.ogvpts.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Yönetici işlemleri (ROL-03): kullanıcılar ve FR-10 öğrenci grubu – danışman eşleştirmesi.
 * Tüm /api/admin/** adresleri SecurityConfig'te yalnızca YONETICI rolüne açıktır.
 */
@Service
public class AdminService {

    private final UserRepository users;
    private final ProjectGroupRepository groups;
    private final GroupMemberRepository groupMembers;
    private final GroupService groupService;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notifications;
    private final int maxGroups;

    public AdminService(UserRepository users, ProjectGroupRepository groups, GroupMemberRepository groupMembers,
                        GroupService groupService, PasswordEncoder passwordEncoder,
                        NotificationService notifications,
                        @Value("${ogvpts.advisor.max-groups}") int maxGroups) {
        this.users = users;
        this.groups = groups;
        this.groupMembers = groupMembers;
        this.groupService = groupService;
        this.passwordEncoder = passwordEncoder;
        this.notifications = notifications;
        this.maxGroups = maxGroups;
    }

    // ------------------------------------------------------------------ kullanıcılar

    @Transactional(readOnly = true)
    public List<UserAdminView> listUsers(UserRole role) {
        List<User> list = role == null
                ? users.findAll().stream().sorted((a, b) -> a.getFullName().compareToIgnoreCase(b.getFullName())).toList()
                : users.findByRole(role);
        return list.stream().map(u -> new UserAdminView(u.getId(), u.getFullName(), u.getEmail(), u.getRole(),
                u.getCreatedAt(),
                groupMembers.findByUser(u.getId()).map(gm -> gm.getGroup().getName()).orElse(null))).toList();
    }

    @Transactional
    public UserSummary createUser(CreateUserRequest req) {
        String email = req.email().trim().toLowerCase();
        if (users.findByEmail(email).isPresent()) {
            throw ApiException.conflict("EMAIL_EXISTS", "Bu e-posta adresi zaten kayıtlı.");
        }
        User u = new User();
        u.setFullName(req.fullName().trim());
        u.setEmail(email);
        u.setRole(req.role());
        u.setPasswordHash(passwordEncoder.encode(req.password()));
        return UserSummary.of(users.saveAndFlush(u));
    }

    @Transactional
    public void resetPassword(Integer userId, String newPassword) {
        User u = users.findById(userId).orElseThrow(() -> ApiException.notFound("Kullanıcı"));
        u.setPasswordHash(passwordEncoder.encode(newPassword));
        notifications.notify(u, "HESAP", "Şifreniz yönetici tarafından sıfırlandı.");
    }

    // ------------------------------------------------------------------ gruplar ve eşleştirme (FR-10)

    @Transactional(readOnly = true)
    public List<GroupResponse> listGroups() {
        return groups.findAllOrdered().stream().map(groupService::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<AdvisorLoad> advisorLoads() {
        return users.findByRole(UserRole.DANISMAN).stream()
                .map(a -> new AdvisorLoad(UserSummary.of(a), groups.countByAdvisor(a.getId()), maxGroups))
                .toList();
    }

    /**
     * FR-10 + BR-01: grubun (dolayısıyla öğrencilerin) tek sorumlu danışmanı olur.
     * Atama değiştirilebilir. Kontenjan doluysa uyarı verilir; force=true ile yine de atanır.
     */
    @Transactional
    public GroupResponse assignAdvisor(Integer groupId, AssignAdvisorRequest req) {
        ProjectGroup group = groups.findById(groupId).orElseThrow(() -> ApiException.notFound("Grup"));
        User advisor = users.findById(req.advisorId()).orElseThrow(() -> ApiException.notFound("Danışman"));
        if (advisor.getRole() != UserRole.DANISMAN) {
            throw ApiException.badRequest("Seçilen kullanıcı danışman değil.");
        }
        User previous = group.getAdvisor();
        if (previous != null && previous.getId().equals(advisor.getId())) {
            return groupService.toResponse(group);
        }
        long current = groups.countByAdvisor(advisor.getId());
        if (current >= maxGroups && !req.force()) {
            throw ApiException.conflict("ADVISOR_QUOTA_FULL",
                    advisor.getFullName() + " danışmanının kontenjanı dolu (" + current + "/" + maxGroups
                            + "). Yine de atamak için onaylayın.");
        }
        group.setAdvisor(advisor);
        groups.saveAndFlush(group);
        notifications.notify(advisor, "GRUP", "\"" + group.getName() + "\" grubunun danışmanı olarak atandınız.");
        notifications.notifyGroup(group, "GRUP", "Grubunuza danışman atandı: " + advisor.getFullName());
        if (previous != null) {
            notifications.notify(previous, "GRUP", "\"" + group.getName() + "\" grubunun danışmanlığı " + advisor.getFullName() + " olarak değiştirildi.");
        }
        return groupService.toResponse(group);
    }
}
