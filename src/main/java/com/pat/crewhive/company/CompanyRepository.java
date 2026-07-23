package com.pat.crewhive.company;

import com.pat.crewhive.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface CompanyRepository extends JpaRepository<Company, UUID> {

    Optional<Company> findByName(String name);

    Optional<Company> findByUsers(Set<User> users);

    boolean existsByName(String name);

}
