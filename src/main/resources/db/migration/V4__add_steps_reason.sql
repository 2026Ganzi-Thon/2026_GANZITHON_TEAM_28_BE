-- V4: AI 가 쓴 이유 문구 (S-04 행동 밑 안내: "…에 맞춰 가장 작은 걸음을 골랐어요")
-- 화면을 다시 열어도 같은 문구가 보이도록 저장한다. 기존 행동은 NULL (문구 없음).

ALTER TABLE steps
    ADD COLUMN reason VARCHAR(100) NULL
        COMMENT 'AI 가 쓴 이유 문구 (없으면 NULL)'
        AFTER content;
