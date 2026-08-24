package com.pat.crewhive.user;

import org.jspecify.annotations.NullMarked;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    @NullMarked
    @EntityGraph(attributePaths = {"roles", "roles.role", "shiftUsers", "shiftUsers.shift"})
    Optional<User> findById(UUID id);

    List<User> findAllByCompany_CompanyId(UUID companyId);

    @EntityGraph(attributePaths = {"roles", "roles.role"})
    Optional<User> findByEmail(String email);

    @EntityGraph(attributePaths = {"roles", "roles.role"})
    @Query("select u from User u where u.userId in :ids")
    List<User> findAllByIds(@Param("ids") Set<UUID> ids);

    boolean existsByEmail(String email);

    /**
     * Verifica se esiste un utente disattivato con questa email, bypassando il
     * filtro {@code active = true} di {@code @SQLRestriction}. Usato dal login per
     * distinguere "account disattivato" da "email mai registrata".
     */
    @Query(value = "SELECT EXISTS(SELECT 1 FROM users WHERE email = :email AND active = false)", nativeQuery = true)
    boolean existsInactiveByEmail(@Param("email") String email);

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
