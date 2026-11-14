package edu.maltepe.ogvpts.repository;

import edu.maltepe.ogvpts.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Integer> {
}
