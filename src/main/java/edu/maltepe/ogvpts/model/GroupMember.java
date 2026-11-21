package edu.maltepe.ogvpts.model;

import jakarta.persistence.*;

/**
 * Tablo: group_members — öğrenci ile grup arasındaki üyelik.
 * BR-01: user_id UNIQUE olduğu için bir öğrenci yalnızca tek grupta yer alabilir.
 */
@Entity
@Table(name = "group_members")
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

    public GroupMember() {
    }

    public GroupMemberId getId() {
        return id;
    }

    public void setId(GroupMemberId id) {
        this.id = id;
    }

    public ProjectGroup getGroup() {
        return group;
    }

    public void setGroup(ProjectGroup group) {
        this.group = group;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
