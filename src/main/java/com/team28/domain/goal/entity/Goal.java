package com.team28.domain.goal.entity;

import com.team28.domain.goal.enums.GoalStatus;
import com.team28.domain.user.entity.User;
import com.team28.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/**
 * 목표. 상태 변경 규칙(진행 중 1개, 이어하기 시 기존 목표 자동 쉬어가기)은 Service 에서 처리한다.
 */
@Getter
@Entity
@Table(name = "goals")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Goal extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 200)
    private String content;

    /** 시작할 때 에너지 1~4 */
    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "energy_level", nullable = false)
    private Integer energyLevel;

    /** 심리적 부담 1~4 */
    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "burden_level", nullable = false)
    private Integer burdenLevel;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private GoalStatus status;

    @Column(name = "rested_at")
    private LocalDateTime restedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    private Goal(User user, String content, int energyLevel, int burdenLevel) {
        this.user = user;
        this.content = content;
        this.energyLevel = energyLevel;
        this.burdenLevel = burdenLevel;
        this.status = GoalStatus.IN_PROGRESS;
    }

    public static Goal create(User user, String content, int energyLevel, int burdenLevel) {
        return new Goal(user, content, energyLevel, burdenLevel);
    }

    public boolean isOwnedBy(User user) {
        return this.user.getId().equals(user.getId());
    }

    public void rest() {
        this.status = GoalStatus.RESTING;
        this.restedAt = LocalDateTime.now();
    }

    public void resume() {
        this.status = GoalStatus.IN_PROGRESS;
    }

    public void complete() {
        this.status = GoalStatus.COMPLETED;
        this.endedAt = LocalDateTime.now();
    }

    public void stop() {
        this.status = GoalStatus.STOPPED;
        this.endedAt = LocalDateTime.now();
    }
}
