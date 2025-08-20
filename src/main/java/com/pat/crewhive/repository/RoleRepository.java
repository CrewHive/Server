package com.pat.crewhive.repository;

import com.pat.crewhive.model.company.entity.Company;
import com.pat.crewhive.model.role.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    boolean existsByRoleNameIgnoreCaseAndCompany(String roleName, Company company);

    Optional<Role> findByRoleNameIgnoreCaseAndCompany(String roleName, Company company);

    Optional<Role> findByRoleNameIgnoreCaseAndCompanyIsNull(String roleName);

    boolean existsByRoleNameIgnoreCaseAndCompanyIsNull(String roleName);

    List<Role> findAllByCompany_CompanyId(Long companyId);

}
