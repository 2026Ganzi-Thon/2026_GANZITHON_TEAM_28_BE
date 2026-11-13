package com.team28.domain.activity.dto;

import com.team28.domain.activity.entity.GoalActivity;
import com.team28.domain.activity.enums.ActivityType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class RecordResponse {

    private RecordResponse() {
    }

    /** 행동 기록 화면 (S-06). 기록이 있는 날만 days 에 들어감 → 달력 점 표시, 날짜 누르면 스크롤 이동 */
    public record Month(String month, List<Day> days) {
    }

    public record Day(LocalDate date, List<Item> records) {
    }

    /** stepId, stepContent 는 쉬어가기·이어하기·종료일 때 null */
    public record Item(
            Long activityId,
            ActivityType type,
            Long goalId,
            String goalContent,
            Long stepId,
            String stepContent,
            LocalDateTime createdAt
    ) {
        public static Item from(GoalActivity activity) {
            return new Item(
                    activity.getId(),
                    activity.getType(),
                    activity.getGoal().getId(),
                    activity.getGoal().getContent(),
                    activity.getStep() == null ? null : activity.getStep().getId(),
                    activity.getStep() == null ? null : activity.getStep().getContent(),
                    activity.getCreatedAt());
        }
    }
}
