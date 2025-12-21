package com.pat.crewhive.repository;

import com.pat.crewhive.model.company.entity.Company;
import com.pat.crewhive.model.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.Set;

public interface CompanyRepository extends JpaRepository<Company, Long> {

    Optional<Company> findByName(String name);

    Optional<Company> findByUsers(Set<User> users);

    boolean existsByName(String name);

}
