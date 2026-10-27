package com.core.reminder.service;

import com.core.reminder.repository.DailyRecordRepository;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.validation.Validator;
import java.time.LocalDate;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class DailyRecordTrendServiceTest {
    @Mock private DailyRecordRepository repository;
    @Mock private Validator validator;
    @InjectMocks private DailyRecordService service;

    @ParameterizedTest
    @ValueSource(ints = {30, 90, 365})
    void oldestDatesClampTheQueryBoundaryWithoutRelyingOnLegacyJdbcCalendarConversion(int days) {
        LocalDate oldest = LocalDate.of(1, 1, 1);
        LocalDate end = oldest.plusDays(4);

        service.trend(11L, "heightCm", end, days);
        service.trend(11L, "weightKg", oldest, days);

        verify(repository).findHeightTrend(11L, oldest, end);
        verify(repository).findWeightTrend(11L, oldest, oldest);
        verifyNoMoreInteractions(repository);
    }
}
