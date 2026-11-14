package edu.maltepe.ogvpts.repository;

import edu.maltepe.ogvpts.model.GroupMember;
import edu.maltepe.ogvpts.model.GroupMemberId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupMemberRepository extends JpaRepository<GroupMember, GroupMemberId> {
}
