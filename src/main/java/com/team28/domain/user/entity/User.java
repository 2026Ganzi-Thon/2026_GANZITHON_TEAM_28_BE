package com.team28.domain.user.entity;

import com.team28.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자. 로그인 없이 닉네임으로 시작하고, 서버가 발급한 uuid 를 프론트가 X-User-Id 헤더로 보낸다.
 */
@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 36, columnDefinition = "CHAR(36)")
    private String uuid;

    @Column(nullable = false, length = 12)
    private String nickname;

    @Column(name = "notification_enabled", nullable = false)
    private boolean notificationEnabled;

    @Column(name = "notification_time")
    private LocalTime notificationTime;

    /** 예: "MON,WED,FRI" */
    @Column(name = "notification_days", length = 30)
    private String notificationDays;

    private User(String nickname) {
        this.uuid = UUID.randomUUID().toString();
        this.nickname = nickname;
        this.notificationEnabled = false;
    }

    public static User create(String nickname) {
        return new User(nickname);
    }

    public void changeNickname(String nickname) {
        this.nickname = nickname;
    }

    public void updateNotification(boolean enabled, LocalTime time, String days) {
        this.notificationEnabled = enabled;
        this.notificationTime = time;
        this.notificationDays = days;
    }
}
