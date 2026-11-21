package edu.maltepe.ogvpts.service;

import edu.maltepe.ogvpts.common.ApiException;
import edu.maltepe.ogvpts.model.GroupMember;
import edu.maltepe.ogvpts.model.ProjectGroup;
import edu.maltepe.ogvpts.repository.GroupMemberRepository;
import edu.maltepe.ogvpts.security.CurrentUser;
import org.springframework.stereotype.Component;

/**
 * FR-16 "Kullanıcı yalnızca yetkili olduğu kayıtlara erişir" kuralının tek merkezi.
 *
 * URL düzeyindeki rol kontrolü (SecurityConfig) "öğrenci yönetici ekranına giremez" der;
 * bu sınıf ise kayıt düzeyinde kontrol yapar: "öğrenci yalnızca KENDİ grubunun verisini görür",
 * "danışman yalnızca KENDİ sorumluluğundaki grupları yönetir".
 */
@Component
public class AccessGuard {

    private final GroupMemberRepository groupMembers;

    public AccessGuard(GroupMemberRepository groupMembers) {
        this.groupMembers = groupMembers;
    }

    public void requireStudent(CurrentUser cu) {
        if (!cu.isStudent()) {
            throw ApiException.forbidden("Bu işlem yalnızca öğrenciler içindir.");
        }
    }

    public void requireAdvisor(CurrentUser cu) {
        if (!cu.isAdvisor()) {
            throw ApiException.forbidden("Bu işlem yalnızca danışmanlar içindir.");
        }
    }

    /** Öğrencinin grubu; grubu yoksa anlaşılır bir hata. */
    public ProjectGroup requireOwnGroup(CurrentUser cu) {
        requireStudent(cu);
        return groupMembers.findByUser(cu.id())
                .map(GroupMember::getGroup)
                .orElseThrow(() -> ApiException.conflict("NO_GROUP", "Önce bir proje grubu oluşturmalı veya bir gruba katılmalısınız."));
    }

    public boolean isMember(ProjectGroup group, CurrentUser cu) {
        return cu.isStudent() && groupMembers.findByUser(cu.id())
                .map(gm -> gm.getGroup().getId().equals(group.getId()))
                .orElse(false);
    }

    public boolean isGroupAdvisor(ProjectGroup group, CurrentUser cu) {
        return cu.isAdvisor() && group.getAdvisor() != null && group.getAdvisor().getId().equals(cu.id());
    }

    /** Görüntüleme yetkisi: grubun üyesi, grubun danışmanı veya yönetici. */
    public void requireCanView(ProjectGroup group, CurrentUser cu) {
        if (!(isMember(group, cu) || isGroupAdvisor(group, cu) || cu.isAdmin())) {
            throw ApiException.forbidden("Bu gruba ait kayıtlara erişim yetkiniz yok.");
        }
    }

    public void requireMember(ProjectGroup group, CurrentUser cu) {
        if (!isMember(group, cu)) {
            throw ApiException.forbidden("Yalnızca grubun üyeleri bu işlemi yapabilir.");
        }
    }

    public void requireGroupAdvisor(ProjectGroup group, CurrentUser cu) {
        if (!isGroupAdvisor(group, cu)) {
            throw ApiException.forbidden("Yalnızca grubun sorumlu danışmanı bu işlemi yapabilir.");
        }
    }
}
