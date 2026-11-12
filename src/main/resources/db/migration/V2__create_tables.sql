-- V2: ERD v2 테이블 생성 (users, goals, steps, goal_activities)
-- 이미 머지된 마이그레이션 파일은 수정하지 말고, 변경이 필요하면 V3__ 파일을 새로 만드세요.

CREATE TABLE users (
    id                    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '내부용 PK',
    uuid                  CHAR(36)     NOT NULL COMMENT '프론트에 주는 사용자 ID (X-User-Id)',
    nickname              VARCHAR(12)  NOT NULL COMMENT '닉네임 1~12자',
    notification_enabled  BIT(1)       NOT NULL DEFAULT b'0' COMMENT '리마인드 알림 켜짐 여부',
    notification_time     TIME         NULL COMMENT '알림 시간',
    notification_days     VARCHAR(30)  NULL COMMENT '알림 요일 (예: MON,WED,FRI)',
    created_at            DATETIME(6)  NOT NULL,
    updated_at            DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_uuid (uuid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사용자';

CREATE TABLE goals (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    user_id       BIGINT        NOT NULL,
    content       VARCHAR(200)  NOT NULL COMMENT '목표 1~200자',
    energy_level  TINYINT       NOT NULL COMMENT '시작할 때 에너지 1~4',
    burden_level  TINYINT       NOT NULL COMMENT '심리적 부담 1~4',
    status        VARCHAR(20)   NOT NULL COMMENT 'IN_PROGRESS / RESTING / COMPLETED / STOPPED',
    rested_at     DATETIME(6)   NULL COMMENT '마지막으로 쉬어간 시각',
    ended_at      DATETIME(6)   NULL COMMENT '완료·중단 시각',
    created_at    DATETIME(6)   NOT NULL,
    updated_at    DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    KEY idx_goals_user_status (user_id, status),
    CONSTRAINT fk_goals_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='목표';

CREATE TABLE steps (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    goal_id         BIGINT        NOT NULL,
    parent_step_id  BIGINT        NULL COMMENT '더 작게로 만들어진 경우 원래 행동',
    sequence        INT           NOT NULL COMMENT '몇 걸음째인지 (더 작게는 같은 번호 유지)',
    shrink_level    TINYINT       NOT NULL DEFAULT 0 COMMENT '같은 걸음 안에서 줄인 횟수 0~5',
    energy_level    TINYINT       NOT NULL COMMENT '이 행동을 받을 때 고른 에너지 1~4',
    content         VARCHAR(200)  NOT NULL COMMENT 'AI가 제안한 행동',
    is_last_step    BIT(1)        NOT NULL DEFAULT b'0' COMMENT 'AI가 마지막 걸음으로 판단했는지 (해봤어요 후 마무리 제안)',
    status          VARCHAR(20)   NOT NULL COMMENT 'SUGGESTED / SHRUNK / TRIED',
    tried_at        DATETIME(6)   NULL COMMENT '해봤어요 누른 시각',
    created_at      DATETIME(6)   NOT NULL,
    updated_at      DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    KEY idx_steps_goal_status (goal_id, status),
    CONSTRAINT fk_steps_goal   FOREIGN KEY (goal_id)        REFERENCES goals (id),
    CONSTRAINT fk_steps_parent FOREIGN KEY (parent_step_id) REFERENCES steps (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 제안 행동';

CREATE TABLE goal_activities (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    goal_id     BIGINT       NOT NULL,
    step_id     BIGINT       NULL COMMENT '관련 행동 (쉬어가기·이어하기·종료는 NULL 가능)',
    type        VARCHAR(20)  NOT NULL COMMENT 'SHRINK / TRY / REST / RESUME / COMPLETE / STOP',
    created_at  DATETIME(6)  NOT NULL COMMENT '선택한 시각 (기록 화면 날짜 기준)',
    PRIMARY KEY (id),
    KEY idx_activities_goal_created (goal_id, created_at),
    CONSTRAINT fk_activities_goal FOREIGN KEY (goal_id) REFERENCES goals (id),
    CONSTRAINT fk_activities_step FOREIGN KEY (step_id) REFERENCES steps (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사용자 선택 기록';
