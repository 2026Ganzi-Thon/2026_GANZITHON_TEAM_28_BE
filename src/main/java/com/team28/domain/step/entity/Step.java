package com.team28.domain.step.entity;

import com.team28.domain.goal.entity.Goal;
import com.team28.domain.step.enums.StepStatus;
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
 * AI가 제안한 행동(걸음).
 * 목표의 현재 행동 = status 가 SUGGESTED 인 행동 1개. 없으면 프론트는 S-04-3(다음 걸음 받기)로 보낸다.
 */
@Getter
@Entity
@Table(name = "steps")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Step extends BaseEntity {

    public static final int MAX_SHRINK_LEVEL = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "goal_id", nullable = false)
    private Goal goal;

    /** '더 작게'로 만들어진 경우 원래 행동 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_step_id")
    private Step parentStep;

    /** 몇 걸음째인지 (더 작게는 같은 번호 유지) */
    @Column(nullable = false)
    private Integer sequence;

    /** 같은 걸음 안에서 줄인 횟수 0~5 */
    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "shrink_level", nullable = false)
    private Integer shrinkLevel;

    /** AI 판단으로 더 작게 만들 수 있는지 (V3). 5회 제한은 shrinkLevel 로 따로 확인 */
    @Column(name = "can_shrink_more", nullable = false)
    private boolean shrinkable;

    /** 이 행동을 받을 때 고른 에너지 1~4 */
    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "energy_level", nullable = false)
    private Integer energyLevel;

    @Column(nullable = false, length = 200)
    private String content;

    /** AI가 마지막 걸음으로 판단했는지. true 면 '해봤어요' 후 S-07(목표 종료)로 안내 */
    @Column(name = "is_last_step", nullable = false)
    private boolean lastStep;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private StepStatus status;

    @Column(name = "tried_at")
    private LocalDateTime triedAt;

    private Step(Goal goal, Step parentStep, int sequence, int shrinkLevel, int energyLevel,
                 String content, boolean shrinkable, boolean lastStep) {
        this.goal = goal;
        this.parentStep = parentStep;
        this.sequence = sequence;
        this.shrinkLevel = shrinkLevel;
        this.energyLevel = energyLevel;
        this.content = truncate(content);
        this.shrinkable = shrinkable;
        this.lastStep = lastStep;
        this.status = StepStatus.SUGGESTED;
    }

    /** 새 걸음 (목표 생성 시 첫 걸음, 다음 걸음 받기) */
    public static Step next(Goal goal, int sequence, int energyLevel, String content,
                            boolean shrinkable, boolean lastStep) {
        return new Step(goal, null, sequence, 0, energyLevel, content, shrinkable, lastStep);
    }

    /** '더 작게'로 만든 행동. 같은 걸음 번호와 에너지를 유지하고 shrinkLevel 만 1 올린다. */
    public static Step shrunkFrom(Step origin, String content, boolean shrinkable, boolean lastStep) {
        return new Step(origin.goal, origin, origin.sequence, origin.shrinkLevel + 1,
                origin.energyLevel, content, shrinkable, lastStep);
    }

    public boolean isSuggested() {
        return this.status == StepStatus.SUGGESTED;
    }

    /** AI 가 더 줄일 수 있다고 했고, 5회 제한에도 안 걸렸을 때만 true */
    public boolean canShrinkMore() {
        return this.shrinkable && this.shrinkLevel < MAX_SHRINK_LEVEL;
    }

    public void markShrunk() {
        this.status = StepStatus.SHRUNK;
    }

    public void markTried() {
        this.status = StepStatus.TRIED;
        this.triedAt = LocalDateTime.now();
    }

    private static String truncate(String content) {
        if (content == null) {
            return "";
        }
        String trimmed = content.strip();
        return trimmed.length() > 200 ? trimmed.substring(0, 200) : trimmed;
    }
}
