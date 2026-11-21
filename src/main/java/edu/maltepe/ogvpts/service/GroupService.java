package edu.maltepe.ogvpts.service;

import edu.maltepe.ogvpts.common.ApiException;
import edu.maltepe.ogvpts.dto.GroupDtos.AddMemberRequest;
import edu.maltepe.ogvpts.dto.GroupDtos.CreateGroupRequest;
import edu.maltepe.ogvpts.dto.GroupDtos.GroupResponse;
import edu.maltepe.ogvpts.dto.UserDtos.UserSummary;
import edu.maltepe.ogvpts.model.GroupMember;
import edu.maltepe.ogvpts.model.Project;
import edu.maltepe.ogvpts.model.ProjectGroup;
import edu.maltepe.ogvpts.model.User;
import edu.maltepe.ogvpts.model.enums.UserRole;
import edu.maltepe.ogvpts.repository.GroupMemberRepository;
import edu.maltepe.ogvpts.repository.ProjectGroupRepository;
import edu.maltepe.ogvpts.repository.ProjectRepository;
import edu.maltepe.ogvpts.repository.UserRepository;
import edu.maltepe.ogvpts.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * FR-12: proje grupları.
 * Kurallar: grupta en fazla 2 öğrenci; bir öğrenci yalnızca tek grupta (BR-01).
 * Bu kurallar veritabanında da (tetikleyici + UNIQUE) korunur; burada kullanıcıya
 * anlaşılır mesaj verebilmek için önceden kontrol ediyoruz.
 */
@Service
public class GroupService {

    public static final int MAX_MEMBERS = 2;

    private final ProjectGroupRepository groups;
    private final GroupMemberRepository groupMembers;
    private final ProjectRepository projects;
    private final UserRepository users;
    private final AccessGuard guard;
    private final NotificationService notifications;

    public GroupService(ProjectGroupRepository groups, GroupMemberRepository groupMembers,
                        ProjectRepository projects, UserRepository users,
                        AccessGuard guard, NotificationService notifications) {
        this.groups = groups;
        this.groupMembers = groupMembers;
        this.projects = projects;
        this.users = users;
        this.guard = guard;
        this.notifications = notifications;
    }

    /** Öğrencinin kendi grubu; grubu yoksa boş döner. */
    @Transactional(readOnly = true)
    public Optional<GroupResponse> myGroup(CurrentUser cu) {
        guard.requireStudent(cu);
        return groupMembers.findByUser(cu.id()).map(gm -> toResponse(gm.getGroup()));
    }

    @Transactional
    public GroupResponse create(CurrentUser cu, CreateGroupRequest req) {
        guard.requireStudent(cu);
        if (groupMembers.findByUser(cu.id()).isPresent()) {
            throw ApiException.conflict("ALREADY_IN_GROUP", "Zaten bir grubunuz var; bir öğrenci yalnızca tek grupta yer alabilir.");
        }
        ProjectGroup group = new ProjectGroup();
        group.setName(req.name().trim());
        group = groups.saveAndFlush(group);
        User me = users.getReferenceById(cu.id());
        groupMembers.save(new GroupMember(group, me));
        return toResponse(group);
    }

    @Transactional
    public GroupResponse addMember(CurrentUser cu, Integer groupId, AddMemberRequest req) {
        ProjectGroup group = groups.findById(groupId).orElseThrow(() -> ApiException.notFound("Grup"));
        guard.requireMember(group, cu);
        if (groupMembers.countByGroup(groupId) >= MAX_MEMBERS) {
            throw ApiException.conflict("GROUP_FULL", "Bir grupta en fazla " + MAX_MEMBERS + " öğrenci olabilir.");
        }
        User student = users.findByEmail(req.email().trim())
                .orElseThrow(() -> ApiException.badRequest("Bu e-posta ile kayıtlı bir kullanıcı bulunamadı."));
        if (student.getRole() != UserRole.OGRENCI) {
            throw ApiException.badRequest("Gruba yalnızca öğrenci eklenebilir.");
        }
        if (groupMembers.findByUser(student.getId()).isPresent()) {
            throw ApiException.conflict("ALREADY_IN_GROUP", student.getFullName() + " zaten başka bir grupta.");
        }
        groupMembers.save(new GroupMember(group, student));
        notifications.notify(student, "GRUP", cu.name() + " sizi \"" + group.getName() + "\" grubuna ekledi.");
        return toResponse(group);
    }

    /** Danışmanın sorumlu olduğu gruplar. */
    @Transactional(readOnly = true)
    public List<GroupResponse> advisorGroups(CurrentUser cu) {
        guard.requireAdvisor(cu);
        return groups.findByAdvisor(cu.id()).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public GroupResponse get(CurrentUser cu, Integer id) {
        ProjectGroup group = groups.findById(id).orElseThrow(() -> ApiException.notFound("Grup"));
        guard.requireCanView(group, cu);
        return toResponse(group);
    }

    /** Entity → DTO. Üyeler ve proje ayrı sorgularla okunur. */
    @Transactional(readOnly = true)
    public GroupResponse toResponse(ProjectGroup g) {
        List<UserSummary> members = groupMembers.findByGroup(g.getId()).stream()
                .map(gm -> UserSummary.of(gm.getUser()))
                .toList();
        Project p = projects.findByGroup(g.getId()).orElse(null);
        return new GroupResponse(g.getId(), g.getName(), UserSummary.of(g.getAdvisor()), members,
                p == null ? null : p.getId(),
                p == null ? null : p.getTitle(),
                p == null ? null : p.getStatus());
    }
}
