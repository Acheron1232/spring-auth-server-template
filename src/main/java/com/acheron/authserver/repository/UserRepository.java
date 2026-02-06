package com.acheron.authserver.repository;

import com.acheron.authserver.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    boolean existsUserByEmail(String email);

    Optional<User> findUserByUsername(String username);

    Optional<User> findUserByEmail(String email);
}
