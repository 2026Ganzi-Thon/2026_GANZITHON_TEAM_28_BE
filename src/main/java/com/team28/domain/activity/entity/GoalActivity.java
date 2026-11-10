package com.team28.domain.activity.entity;

import com.team28.domain.activity.enums.ActivityType;
import com.team28.domain.goal.entity.Goal;
import com.team28.domain.step.entity.Step;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 사용자 선택 기록. 수정하지 않는 기록이라 updated_at 이 없어서 BaseEntity 를 상속하지 않는다.
 */
@Getter
@Entity
@Table(name = "goal_activities")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GoalActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "goal_id", nullable = false)
    private Goal goal;

    /** 관련 행동 (쉬어가기·이어하기·종료는 null 가능) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "step_id")
    private Step step;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private ActivityType type;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private GoalActivity(Goal goal, Step step, ActivityType type) {
        this.goal = goal;
        this.step = step;
        this.type = type;
    }

    public static GoalActivity of(Goal goal, Step step, ActivityType type) {
        return new GoalActivity(goal, step, type);
    }

    public static GoalActivity of(Goal goal, ActivityType type) {
        return new GoalActivity(goal, null, type);
    }
}
