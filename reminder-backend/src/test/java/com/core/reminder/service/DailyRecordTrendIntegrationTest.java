package com.core.reminder.service;

import com.common.reminder.dto.UserProfileDto;
import com.core.reminder.controller.DailyRecordController;
import com.core.reminder.dto.DailyRecordRequest;
import com.core.reminder.dto.MeasurementTrendPoint;
import com.core.reminder.repository.DailyRecordRepository;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import javax.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DataJpaTest(showSql = false)
@ActiveProfiles("test")
@ContextConfiguration(classes = DailyRecordTrendIntegrationTest.JpaConfig.class)
@Import({DailyRecordService.class, ValidationAutoConfiguration.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class DailyRecordTrendIntegrationTest {
    @EntityScan(basePackages = {"com.common.reminder.model", "com.core.reminder.model"})
    @EnableJpaRepositories(basePackageClasses = DailyRecordRepository.class)
    static class JpaConfig { }

    @Autowired private DailyRecordService service;
    @Autowired private DailyRecordRepository repository;
    @Autowired private EntityManagerFactory entityManagerFactory;

    private final LocalDate end = LocalDate.of(2026, 10, 27);

    @BeforeEach
    void clearRecords() {
        repository.deleteAll();
    }

    @Test
    void weightTrendUsesOnlyCurrentOwnersMeasuredValuesWithinTheWindowInDateOrder() {
        LocalDate start = end.minusDays(29);
        save(11L, end, "62.5", null, "当日备注");
        save(11L, start, "60.1", null, "起始备注");
        save(11L, end.minusDays(10), "61.2", "170.2", null);
        save(11L, start.minusDays(1), "59.0", null, null);
        save(11L, end.plusDays(1), "63.0", null, null);
        save(22L, end.minusDays(1), "80.0", null, null);
        save(11L, end.minusDays(2), null, "170.0", null);
        save(11L, end.minusDays(3), null, null, "只有备注");

        assertThat(service.trend(11L, "weightKg", end, 30)).containsExactly(
                point(start, "60.1"), point(end.minusDays(10), "61.2"), point(end, "62.5"));
    }

    @Test
    void heightTrendDoesNotTurnInheritedDisplayValuesIntoMeasurements() {
        save(11L, end.minusDays(40), null, "169.0", null);
        LocalDate measured = end.minusDays(8);
        save(11L, measured, null, "170.2", null);
        save(11L, end.minusDays(1), null, null, "只有备注");
        save(11L, end, "62.5", null, null);
        save(11L, end.plusDays(1), null, "180.0", null);
        save(22L, end.minusDays(1), null, "190.0", null);
        assertThat(service.get(11L, end).getLatestHeight().getRecordDate()).isEqualTo(measured);

        assertThat(service.trend(11L, "heightCm", end, 30)).containsExactly(point(measured, "170.2"));
        assertThat(service.get(11L, end).getRecord().getHeightCm()).isNull();
        assertThat(repository.count()).isEqualTo(6);
    }

    @ParameterizedTest
    @ValueSource(ints = {30, 90, 365})
    void everySupportedWindowIncludesBothBoundariesAndExcludesAdjacentDays(int days) {
        LocalDate start = end.minusDays(days - 1L);
        save(11L, start.minusDays(1), "50.0", null, null);
        save(11L, start, "51.0", null, null);
        save(11L, end, "52.0", null, null);
        save(11L, end.plusDays(1), "53.0", null, null);

        assertThat(service.trend(11L, "weightKg", end, days)).containsExactly(
                point(start, "51.0"), point(end, "52.0"));
    }

    @Test
    void emptyTrendRemainsEmptyForUnmeasuredFieldsAndOtherOwners() {
        save(11L, end, null, null, "只有备注");
        save(22L, end, "60.0", "170.0", null);

        assertThat(service.trend(11L, "weightKg", end, 30)).isEmpty();
        assertThat(service.trend(11L, "heightCm", end, 365)).isEmpty();
    }

    @Test
    void projectionQueriesDoNotHydrateRecordsOrNotes() {
        save(11L, end, "62.5", "170.2", "测试备注".repeat(2000));
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();
        try {
            assertThat(service.trend(11L, "weightKg", end, 30)).containsExactly(point(end, "62.5"));
            assertThat(service.trend(11L, "heightCm", end, 90)).containsExactly(point(end, "170.2"));
            assertThat(statistics.getQueryExecutionCount()).isEqualTo(2);
            assertThat(statistics.getEntityLoadCount()).isZero();
        } finally {
            statistics.setStatisticsEnabled(false);
        }
    }

    @Test
    void trendServiceRejectsUnsupportedParametersAndMissingIdentity() {
        for (String metric : new String[]{null, "", "weight", "diary", "WeightKg", "heightCm "}) {
            assertStatus(HttpStatus.BAD_REQUEST, () -> service.trend(11L, metric, end, 30));
        }
        for (int days : new int[]{0, -1, 1, 29, 31, 89, 91, 364, 366, Integer.MAX_VALUE}) {
            assertStatus(HttpStatus.BAD_REQUEST, () -> service.trend(11L, "weightKg", end, days));
        }
        for (LocalDate date : new LocalDate[]{null, LocalDate.of(0, 1, 1), LocalDate.of(10000, 1, 1)}) {
            assertStatus(HttpStatus.BAD_REQUEST, () -> service.trend(11L, "weightKg", date, 30));
        }
        assertStatus(HttpStatus.UNAUTHORIZED, () -> service.trend(null, "weightKg", end, 30));
        assertThat(repository.count()).isZero();
    }

    @Test
    void trendControllerReturnsChineseBadRequestForInvalidMetrics() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new DailyRecordController(service)).build();
        UserProfileDto user = new UserProfileDto();
        user.setId(11L);
        for (String metric : new String[]{"", "weight", "diary", "WeightKg", "heightCm "}) {
            mvc.perform(get("/api/daily-records/trend").requestAttr("currentUser", user)
                            .param("metric", metric).param("end", "2026-10-27").param("days", "30"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsString("指标")));
        }
    }

    private void save(Long owner, LocalDate day, String weight, String height, String diary) {
        DailyRecordRequest request = new DailyRecordRequest();
        request.setWeightKg(weight == null ? null : new BigDecimal(weight));
        request.setHeightCm(height == null ? null : new BigDecimal(height));
        request.setDiary(diary);
        service.save(owner, day, request);
    }

    private static MeasurementTrendPoint point(LocalDate day, String value) {
        return new MeasurementTrendPoint(day, new BigDecimal(value));
    }

    private static void assertStatus(HttpStatus status, Runnable action) {
        assertThat(assertThrows(ResponseStatusException.class, action::run).getStatus()).isEqualTo(status);
    }
}
