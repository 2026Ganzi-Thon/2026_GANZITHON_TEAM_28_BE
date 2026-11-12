package com.team28.domain.activity.repository;

import com.team28.domain.activity.entity.GoalActivity;
import com.team28.domain.user.entity.User;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GoalActivityRepository extends JpaRepository<GoalActivity, Long> {

    /** 행동 기록 화면(월 단위): 사용자의 기간 내 기록을 목표·행동과 함께 조회 */
    @Query("""
            select a from GoalActivity a
            join fetch a.goal g
            left join fetch a.step s
            where g.user = :user
              and a.createdAt >= :from and a.createdAt < :to
            order by a.createdAt asc
            """)
    List<GoalActivity> findAllByUserAndPeriod(@Param("user") User user,
                                              @Param("from") LocalDateTime from,
                                              @Param("to") LocalDateTime to);
}
