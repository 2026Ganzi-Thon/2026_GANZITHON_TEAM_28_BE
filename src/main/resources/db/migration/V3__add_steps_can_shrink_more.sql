-- V3: AI 가 더 작게 만들 수 없다고 판단한 행동 표시 (S-04-2 '가장 작은 걸음 도달')
-- 기존 행동은 줄일 수 있는 것으로 둠. 줄인 횟수 5회 제한은 shrink_level 로 따로 확인.

ALTER TABLE steps
    ADD COLUMN can_shrink_more BIT(1) NOT NULL DEFAULT b'1'
        COMMENT 'AI 판단으로 더 작게 만들 수 있는지'
        AFTER shrink_level;
