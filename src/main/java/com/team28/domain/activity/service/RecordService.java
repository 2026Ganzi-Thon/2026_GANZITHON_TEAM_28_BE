package com.team28.domain.activity.service;

import com.team28.domain.activity.dto.RecordResponse;
import com.team28.domain.activity.entity.GoalActivity;
import com.team28.domain.activity.repository.GoalActivityRepository;
import com.team28.domain.user.entity.User;
import com.team28.global.apiPayload.code.ErrorStatus;
import com.team28.global.apiPayload.exception.GeneralException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecordService {

    private final GoalActivityRepository activityRepository;

    /** 한 달치 기록을 날짜별로 묶어서 (오래된 날짜 → 최근 날짜, 같은 날은 시간순) */
    public RecordResponse.Month getMonth(User user, String month) {
        YearMonth yearMonth = parse(month);
        LocalDateTime from = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime to = yearMonth.plusMonths(1).atDay(1).atStartOfDay();

        List<GoalActivity> activities = activityRepository.findAllByUserAndPeriod(user, from, to);

        Map<LocalDate, List<RecordResponse.Item>> byDate = activities.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getCreatedAt().toLocalDate(),
                        LinkedHashMap::new,
                        Collectors.mapping(RecordResponse.Item::from, Collectors.toList())));

        List<RecordResponse.Day> days = byDate.entrySet().stream()
                .map(e -> new RecordResponse.Day(e.getKey(), e.getValue()))
                .toList();

        return new RecordResponse.Month(yearMonth.toString(), days);
    }

    private YearMonth parse(String month) {
        try {
            return YearMonth.parse(month);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new GeneralException(ErrorStatus.BAD_REQUEST);
        }
    }
}
