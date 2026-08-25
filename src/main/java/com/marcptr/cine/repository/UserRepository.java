package com.marcptr.cine.repository;

import com.marcptr.cine.model.User;
import com.marcptr.cine.model.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByRole(Role roleAdmin);
}
