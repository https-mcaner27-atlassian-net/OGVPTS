package edu.maltepe.ogvpts.repository;

import edu.maltepe.ogvpts.model.GroupMember;
import edu.maltepe.ogvpts.model.GroupMemberId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GroupMemberRepository extends JpaRepository<GroupMember, GroupMemberId> {

    /** Öğrencinin üyeliği (BR-01 gereği en fazla bir tane olur). */
    @Query("select gm from GroupMember gm where gm.user.id = :userId")
    Optional<GroupMember> findByUser(@Param("userId") Integer userId);

    @Query("select gm from GroupMember gm join fetch gm.user where gm.group.id = :groupId order by gm.user.fullName")
    List<GroupMember> findByGroup(@Param("groupId") Integer groupId);

    @Query("select count(gm) from GroupMember gm where gm.group.id = :groupId")
    long countByGroup(@Param("groupId") Integer groupId);
}
