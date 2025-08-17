package com.pat.crewhive.repository;

import com.pat.crewhive.model.user.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(attributePaths = {"role", "role.role"})
    Optional<User> findById(Long id);

    @EntityGraph(attributePaths = {"role", "role.role"})
    Optional<User> findByEmail(String email);

    @EntityGraph(attributePaths = {"role", "role.role"})
    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

}
