package edu.maltepe.ogvpts.repository;

import edu.maltepe.ogvpts.model.User;
import edu.maltepe.ogvpts.model.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {

    @Query("select u from User u where lower(u.email) = lower(:email)")
    Optional<User> findByEmail(@Param("email") String email);

    @Query("select u from User u where u.role = :role order by u.fullName")
    List<User> findByRole(@Param("role") UserRole role);

    @Query("select case when count(u) > 0 then true else false end from User u where u.role = :role")
    boolean existsByRole(@Param("role") UserRole role);
}
