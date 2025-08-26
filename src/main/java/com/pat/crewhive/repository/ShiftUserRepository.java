package com.pat.crewhive.repository;


import com.pat.crewhive.model.shift.shiftprogrammed.ShiftUser;
import com.pat.crewhive.model.shift.shiftprogrammed.ShiftUsersId;
import com.pat.crewhive.model.shift.shiftprogrammed.entity.ShiftProgrammed;
import com.pat.crewhive.model.user.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ShiftUserRepository extends JpaRepository<ShiftUser, ShiftUsersId> {

    @EntityGraph(attributePaths = {"shift", "shift.users", "shift.users.user"})
    @Query("""
        select s
        from ShiftUser su
        join su.shift s
        where su.user.userId = :userId
          and s.date between :from and :to
        order by s.start asc
        """)
    List<ShiftProgrammed> findShiftsByUserIdAndDateBetween(
            @Param("userId") Long userId,
            @Param("from") LocalDate from,
            @Param("to")     LocalDate to);


@EntityGraph(attributePaths = {"shift", "shift.users", "shift.users.user"})
    @Query("""
        select distinct sp
        from ShiftUser su
        join su.shift sp
        where su.user.company.companyId = :companyId
          and sp.date between :from and :to
        order by sp.start asc
        """)
    List<ShiftProgrammed> findShiftsByCompanyIdAndDateBetween(
            @Param("companyId") Long companyId,
            @Param("from") LocalDate from,
            @Param("to")     LocalDate to);


    @Query("""
        select distinct u
        from ShiftUser su
        join su.user u
        where su.shift.shiftProgrammedId = :shiftId
        order by u.username asc
        """)
    List<User> findUsersByShiftId(@Param("shiftId") Long shiftId);


    @Modifying
    @Query("delete from ShiftUser su where su.shift.shiftProgrammedId = :shiftId")
    int deleteByShiftId(@Param("shiftId") Long shiftId);
}
