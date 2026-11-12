package com.team28.domain.user.service;

import com.team28.domain.user.dto.UserRequest;
import com.team28.domain.user.dto.UserResponse;
import com.team28.domain.user.entity.User;
import com.team28.domain.user.exception.UserErrorStatus;
import com.team28.domain.user.repository.UserRepository;
import com.team28.global.apiPayload.exception.GeneralException;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    /** 요일을 항상 월~일 순서로 저장하기 위한 기준 */
    private static final List<String> DAY_ORDER = List.of("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN");

    private final UserRepository userRepository;

    @Transactional
    public UserResponse.Info create(UserRequest.Nickname request) {
        User user = userRepository.save(User.create(request.nickname().strip()));
        return UserResponse.Info.from(user);
    }

    public UserResponse.Info getInfo(User user) {
        return UserResponse.Info.from(user);
    }

    @Transactional
    public UserResponse.Info updateNickname(User currentUser, UserRequest.Nickname request) {
        User user = reload(currentUser);
        user.changeNickname(request.nickname().strip());
        return UserResponse.Info.from(user);
    }

    public UserResponse.Notification getNotification(User user) {
        return UserResponse.Notification.from(user);
    }

    /** 설정 저장만 한다. 실제 푸시 발송은 MVP 이후 (PM 확정 11/12) */
    @Transactional
    public UserResponse.Notification updateNotification(User currentUser, UserRequest.Notification request) {
        List<String> days = request.days() == null ? List.of() : request.days();
        boolean enabled = request.enabled();
        if (enabled && (request.time() == null || days.isEmpty())) {
            throw new GeneralException(UserErrorStatus.NOTIFICATION_INVALID);
        }

        LocalTime time = request.time() == null ? null : LocalTime.parse(request.time());
        String daysValue = days.isEmpty() ? null : String.join(",", sortDays(days));

        User user = reload(currentUser);
        user.updateNotification(enabled, time, daysValue);
        return UserResponse.Notification.from(user);
    }

    /** @CurrentUser 로 받은 User 는 트랜잭션 밖에서 조회된 것이라, 수정할 때는 다시 조회한다. */
    private User reload(User user) {
        return userRepository.findById(user.getId())
                .orElseThrow(() -> new GeneralException(UserErrorStatus.UNAUTHORIZED));
    }

    private List<String> sortDays(List<String> days) {
        LinkedHashSet<String> unique = new LinkedHashSet<>(days);
        return DAY_ORDER.stream().filter(unique::contains).toList();
    }
}
