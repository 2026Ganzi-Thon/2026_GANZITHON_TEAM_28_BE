package com.team28.domain.goal.repository;

import com.team28.domain.goal.entity.Goal;
import com.team28.domain.goal.enums.GoalStatus;
import com.team28.domain.user.entity.User;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoalRepository extends JpaRepository<Goal, Long> {

    /** 진행 중 목표 (사용자당 1개) */
    Optional<Goal> findByUserAndStatus(User user, GoalStatus status);

    /** 홈 화면: 쉬어가는 목표 목록 등 */
    List<Goal> findAllByUserAndStatusInOrderByUpdatedAtDesc(User user, Collection<GoalStatus> statuses);

    /** 목표 3개 제한 확인용 (IN_PROGRESS + RESTING) */
    long countByUserAndStatusIn(User user, Collection<GoalStatus> statuses);
}
