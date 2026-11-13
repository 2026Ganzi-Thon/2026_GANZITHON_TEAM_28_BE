package com.team28.domain.goal.dto;

import com.team28.domain.goal.enums.GoalEndType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class GoalRequest {

    private GoalRequest() {
    }

    /** S-03 목표 입력 */
    public record Create(
            @NotBlank(message = "목표를 입력해 주세요.")
            @Size(max = 200, message = "목표는 200자 이하로 입력해 주세요.")
            String content,

            @NotNull(message = "에너지를 골라주세요.")
            @Min(value = 1, message = "에너지는 1~4 중에서 골라주세요.")
            @Max(value = 4, message = "에너지는 1~4 중에서 골라주세요.")
            Integer energyLevel,

            @NotNull(message = "부담 정도를 골라주세요.")
            @Min(value = 1, message = "부담 정도는 1~4 중에서 골라주세요.")
            @Max(value = 4, message = "부담 정도는 1~4 중에서 골라주세요.")
            Integer burdenLevel
    ) {
    }

    /** S-04-3 다음 걸음 받기. 처음 고른 에너지 ±1 */
    public record NextStep(
            @NotNull(message = "에너지를 골라주세요.")
            @Min(value = 1, message = "에너지는 1~4 중에서 골라주세요.")
            @Max(value = 4, message = "에너지는 1~4 중에서 골라주세요.")
            Integer energy
    ) {
    }

    /** S-07 목표 종료 */
    public record End(
            @NotNull(message = "COMPLETED 또는 STOPPED 를 보내주세요.")
            GoalEndType type
    ) {
    }
}
