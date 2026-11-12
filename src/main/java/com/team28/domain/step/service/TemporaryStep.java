package com.team28.domain.step.service;

/**
 * AI 연결 전까지 쓰는 임시 행동 문장.
 * AI 담당자가 GoalService / StepService 의 TODO(AI 연결) 자리를 AI 호출로 바꾸면 이 클래스는 지워도 됩니다.
 */
public final class TemporaryStep {

    private TemporaryStep() {
    }

    /** 목표 생성 시 첫 걸음 */
    public static String first(String goal) {
        return "[임시] '" + goal + "'을 위해 필요한 물건 하나 꺼내두기";
    }

    /** 더 작게 */
    public static String shrink(String currentStep) {
        return "[임시] '" + currentStep + "'의 첫 동작만 해보기";
    }

    /** 다음 걸음 받기 */
    public static String next(String goal, int sequence) {
        return "[임시] '" + goal + "' " + sequence + "걸음째 행동";
    }
}
