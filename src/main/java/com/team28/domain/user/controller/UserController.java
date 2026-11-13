package com.team28.domain.user.controller;

import com.team28.domain.user.dto.UserRequest;
import com.team28.domain.user.dto.UserResponse;
import com.team28.domain.user.entity.User;
import com.team28.domain.user.service.UserService;
import com.team28.global.apiPayload.ApiResponse;
import com.team28.global.apiPayload.code.SuccessStatus;
import com.team28.global.auth.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User", description = "닉네임 시작, 내 정보, 설정")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "시작하기 (S-01)",
            description = "닉네임으로 사용자를 만들고 userId 를 돌려줍니다. 프론트는 userId 를 저장하고 이후 요청마다 X-User-Id 헤더로 보내세요.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserResponse.Info> create(@Valid @RequestBody UserRequest.Nickname request) {
        return ApiResponse.of(SuccessStatus.CREATED, userService.create(request));
    }

    @Operation(summary = "내 정보 확인", description = "재방문 시 저장된 userId 가 유효한지 확인. 401 이면 시작 화면으로.")
    @GetMapping("/me")
    public ApiResponse<UserResponse.Info> me(@CurrentUser User user) {
        return ApiResponse.onSuccess(userService.getInfo(user));
    }

    @Operation(summary = "닉네임 수정 (S-09)")
    @PatchMapping("/me")
    public ApiResponse<UserResponse.Info> updateNickname(@CurrentUser User user,
                                                         @Valid @RequestBody UserRequest.Nickname request) {
        return ApiResponse.onSuccess(userService.updateNickname(user, request));
    }

    @Operation(summary = "알림 설정 조회 (S-09-1)")
    @GetMapping("/me/notification")
    public ApiResponse<UserResponse.Notification> getNotification(@CurrentUser User user) {
        return ApiResponse.onSuccess(userService.getNotification(user));
    }

    @Operation(summary = "알림 설정 수정 (S-09-1)",
            description = "켜짐 여부·시간(HH:mm)·요일(MON~SUN) 저장. 켤 때는 시간과 요일 1개 이상 필수. 실제 발송은 MVP 이후.")
    @PutMapping("/me/notification")
    public ApiResponse<UserResponse.Notification> updateNotification(@CurrentUser User user,
                                                                     @Valid @RequestBody UserRequest.Notification request) {
        return ApiResponse.onSuccess(userService.updateNotification(user, request));
    }
}
