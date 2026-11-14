package edu.maltepe.ogvpts.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/** group_members tablosunun bileşik birincil anahtarı (group_id, user_id). */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class GroupMemberId implements Serializable {

    @Column(name = "group_id")
    private Integer groupId;

    @Column(name = "user_id")
    private Integer userId;
}
