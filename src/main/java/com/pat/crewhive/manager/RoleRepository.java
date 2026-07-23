package com.pat.crewhive.manager;

import com.pat.crewhive.company.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, Long> {

    boolean existsByRoleNameIgnoreCaseAndCompany(String roleName, Company company);

    Optional<Role> findByRoleNameIgnoreCaseAndCompany(String roleName, Company company);

    Optional<Role> findByRoleNameIgnoreCaseAndCompanyIsNull(String roleName);

    boolean existsByRoleNameIgnoreCaseAndCompanyIsNull(String roleName);

    List<Role> findAllByCompany_CompanyId(UUID companyId);

}
