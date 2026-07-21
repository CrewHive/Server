package com.pat.crewhive.shiftprogrammed;


import com.pat.crewhive.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ShiftUserRepository extends JpaRepository<ShiftUser, ShiftUserId> {

    @Query("""
        select distinct u
        from ShiftUser su
        join su.user u
        where su.shift.shiftProgrammedId = :shiftId
        order by u.username asc
        """)
    List<User> findUsersByShiftId(@Param("shiftId") Long shiftId);


    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from ShiftUser su where su.shift.shiftProgrammedId = :shiftId")
    int deleteByShiftId(@Param("shiftId") Long shiftId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from ShiftUser su where su.user.userId = :userId")
    int deleteByUserId(@Param("userId") Long userId);
}
