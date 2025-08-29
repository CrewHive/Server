package com.pat.crewhive.repository;

import com.pat.crewhive.model.user.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(attributePaths = {"role", "role.role"})
    Optional<User> findById(Long id);

    List<User> findAllByCompany_CompanyId(Long companyId);

    @EntityGraph(attributePaths = {"role", "role.role"})
    Optional<User> findByEmail(String email);

    @EntityGraph(attributePaths = {"role", "role.role"})
    Optional<User> findByUsername(String username);

    @EntityGraph(attributePaths = {"role", "role.role"})
    @Query("select u from User u where u.userId in :ids")
    List<User> findAllByIds(@Param("ids") Set<Long> ids);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);


    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        UPDATE users
           SET vacation_days_accumulated = vacation_days_accumulated +
               CASE
                 WHEN contract_type IN ('FULL_TIME','PART_TIME_HORIZONTAL') THEN 2.17
                 WHEN contract_type = 'PART_TIME_VERTICAL' THEN 1.3
                 ELSE 0
               END
         WHERE contract_type IS NOT NULL
        """,
            nativeQuery = true)
    int accrueMonthlyVacationDays();


    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        UPDATE users
           SET leave_days_accumulated = leave_days_accumulated +
               CASE
                 WHEN contract_type IN ('FULL_TIME','PART_TIME_HORIZONTAL') THEN 1.5
                 WHEN contract_type = 'PART_TIME_VERTICAL' THEN 0.9
                 ELSE 0
               END
         WHERE contract_type IS NOT NULL
        """,
            nativeQuery = true)
    int accrueMonthlyLeaveDays();

}
