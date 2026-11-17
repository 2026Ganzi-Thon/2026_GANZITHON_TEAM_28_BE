package com.team28.domain.goal.dto;

import com.team28.domain.goal.enums.GoalEndType;
import io.swagger.v3.oas.annotations.media.Schema;
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

            @Schema(description = "현재 에너지 (화면 선택지 순서). 1=완전히 방전됨, 2=조금 남아 있음, 3=보통이에요, 4=꽤 괜찮아요",
                    example = "2")
            @NotNull(message = "에너지를 골라주세요.")
            @Min(value = 1, message = "에너지는 1~4 중에서 골라주세요.")
            @Max(value = 4, message = "에너지는 1~4 중에서 골라주세요.")
            Integer energyLevel,

            @Schema(description = "심리적 부담 (화면 선택지 순서, 에너지와 방향이 반대). "
                    + "1=너무 막막해요, 2=꽤 부담돼요, 3=약간 부담돼요, 4=해볼 만해요",
                    example = "1")
            @NotNull(message = "부담 정도를 골라주세요.")
            @Min(value = 1, message = "부담 정도는 1~4 중에서 골라주세요.")
            @Max(value = 4, message = "부담 정도는 1~4 중에서 골라주세요.")
            Integer burdenLevel
    ) {
    }

    /** S-04-3 다음 걸음 받기. 처음 고른 에너지 ±1 */
    public record NextStep(
            @Schema(description = "다시 고른 에너지. 목표 생성 때와 같은 기준(1=완전히 방전됨 ~ 4=꽤 괜찮아요). "
                    + "화면은 그대로예요/조금 생겼어요/더 지쳤어요 3택이라 프론트가 처음 값 기준 ±1로 바꿔 보낸다",
                    example = "3")
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
