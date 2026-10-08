package com.psg.repository;

import com.psg.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    java.util.List<User> findByOrganization(com.psg.model.Organization organization);
    boolean existsByUsername(String username);
}
