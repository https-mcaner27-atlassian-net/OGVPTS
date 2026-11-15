package edu.maltepe.ogvpts.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Tablo: group_members — öğrenci ile grup arasındaki üyelik.
 * BR-01: user_id UNIQUE olduğu için bir öğrenci yalnızca tek grupta yer alabilir.
 */
@Entity
@Table(name = "group_members")
@Getter
@Setter
@NoArgsConstructor
public class GroupMember {

    @EmbeddedId
    private GroupMemberId id = new GroupMemberId();

    @MapsId("groupId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id")
    private ProjectGroup group;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    public GroupMember(ProjectGroup group, User user) {
        this.group = group;
        this.user = user;
        this.id = new GroupMemberId(group.getId(), user.getId());
    }
}
