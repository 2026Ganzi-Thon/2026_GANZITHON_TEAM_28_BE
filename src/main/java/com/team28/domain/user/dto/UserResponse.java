package com.team28.domain.user.dto;

import com.team28.domain.user.entity.User;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

public class UserResponse {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private UserResponse() {
    }

    /** userId 는 프론트가 브라우저에 저장하고 X-User-Id 헤더로 보낼 값 */
    public record Info(String userId, String nickname) {

        public static Info from(User user) {
            return new Info(user.getUuid(), user.getNickname());
        }
    }

    public record Notification(boolean enabled, String time, List<String> days) {

        public static Notification from(User user) {
            String time = user.getNotificationTime() == null
                    ? null
                    : user.getNotificationTime().format(TIME_FORMAT);
            List<String> days = (user.getNotificationDays() == null || user.getNotificationDays().isBlank())
                    ? List.of()
                    : Arrays.asList(user.getNotificationDays().split(","));
            return new Notification(user.isNotificationEnabled(), time, days);
        }
    }
}
