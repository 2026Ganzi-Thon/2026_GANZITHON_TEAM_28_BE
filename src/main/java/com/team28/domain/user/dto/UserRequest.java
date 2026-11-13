package com.team28.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public class UserRequest {

    private UserRequest() {
    }

    /** S-01 시작 화면, S-09 닉네임 수정 공통 */
    public record Nickname(
            @NotBlank(message = "닉네임을 입력해 주세요.")
            @Size(max = 12, message = "닉네임은 12자 이하로 입력해 주세요.")
            String nickname
    ) {
    }

    /** S-09-1 알림 설정. enabled=true 면 time 과 days 필수 */
    public record Notification(
            @NotNull(message = "알림 켜짐 여부를 보내주세요.")
            Boolean enabled,

            @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "시간은 HH:mm 형식으로 보내주세요. (예: 21:00)")
            String time,

            List<@Pattern(regexp = "^(MON|TUE|WED|THU|FRI|SAT|SUN)$",
                    message = "요일은 MON~SUN 으로 보내주세요.") String> days
    ) {
    }
}
