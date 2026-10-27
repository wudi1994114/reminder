package com.core.reminder.service;

import com.common.reminder.model.DailyRecord;
import com.core.reminder.dto.DailyRecordDayResponse;
import com.core.reminder.dto.DailyRecordRequest;
import com.core.reminder.dto.DailyRecordResponse;
import com.core.reminder.repository.DailyRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest(showSql = false)
@ActiveProfiles("test")
@ContextConfiguration(classes = DailyRecordServiceIntegrationTest.JpaConfig.class)
@Import({DailyRecordService.class, ValidationAutoConfiguration.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class DailyRecordServiceIntegrationTest {

    // Import-only configuration: the application's explicit component scan must not discover it.
    @EntityScan(basePackages = {"com.common.reminder.model", "com.core.reminder.model"})
    @EnableJpaRepositories(basePackageClasses = DailyRecordRepository.class)
    static class JpaConfig { }

    @Autowired private DailyRecordService service;
    @Autowired private DailyRecordRepository repository;

    private final LocalDate day = LocalDate.of(2026, 10, 27);

    @BeforeEach
    void clearRecords() {
        repository.deleteAll();
    }

    @Test
    void monthQueryIncludesBothBoundariesAndOnlyCurrentOwner() {
        service.save(11L, day.withDayOfMonth(1), request("60.1", null, null, null));
        service.save(11L, day.withDayOfMonth(31), request(null, null, "月底", null));
        service.save(11L, day.withDayOfMonth(1).minusDays(1), request(null, null, "上月", null));
        service.save(11L, day.withDayOfMonth(31).plusDays(1), request(null, null, "下月", null));
        service.save(22L, day, request(null, null, "他人", null));

        assertThat(service.list(11L, YearMonth.of(2026, 10)))
                .extracting(DailyRecordResponse::getRecordDate)
                .containsExactly(day.withDayOfMonth(1), day.withDayOfMonth(31));
        assertThat(service.get(11L, day).getRecord()).isNull();
        assertThat(service.get(22L, day).getRecord().getDiary()).isEqualTo("他人");
    }

    @Test
    void historicalHeightNeverReadsAnotherOwnerOrAFutureMeasurement() {
        LocalDate measured = day.minusDays(8);
        service.save(11L, measured, request(null, "170.2", null, null));
        service.save(11L, day.plusDays(1), request(null, "180.0", null, null));
        service.save(22L, day, request(null, "190.0", null, null));

        DailyRecordDayResponse result = service.get(11L, day);
        assertThat(result.getRecord()).isNull();
        assertThat(result.getLatestHeight().getRecordDate()).isEqualTo(measured);
        assertThat(result.getLatestHeight().getHeightCm()).isEqualByComparingTo("170.2");
        assertThat(service.get(11L, measured.minusDays(1)).getLatestHeight()).isNull();
    }

    @Test
    void clearingTodaysHeightFallsBackWithoutCopyingHistoryIntoTheRecord() {
        service.save(11L, day.minusDays(1), request(null, "169.0", null, null));
        DailyRecordResponse original = service.save(11L, day, request("60.0", "170.0", null, null));
        assertThat(service.get(11L, day).getLatestHeight().getRecordDate()).isEqualTo(day);

        service.save(11L, day, request("60.0", null, null, original.getVersion(), original.getId()));

        DailyRecordDayResponse result = service.get(11L, day);
        assertThat(result.getRecord().getHeightCm()).isNull();
        assertThat(result.getLatestHeight().getRecordDate()).isEqualTo(day.minusDays(1));
    }

    @Test
    void upsertKeepsIdentityAndCreationTimeAndAdvancesVersion() {
        DailyRecordResponse original = service.save(11L, day, request("60.1", null, "首记", null));
        DailyRecordResponse saved = service.save(11L, day, request(null, "170.1", "改记", original.getVersion(), original.getId()));

        assertThat(original.getVersion()).isZero();
        assertThat(saved.getId()).isEqualTo(original.getId());
        assertThat(saved.getCreatedAt()).isEqualTo(original.getCreatedAt());
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getVersion()).isEqualTo(original.getVersion() + 1);
        assertThat(saved.getWeightKg()).isNull();
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void staleUpdatesAndMissingVersionsCannotOverwriteExistingContent() {
        DailyRecordResponse first = service.save(11L, day, request(null, null, "初始", null));
        assertStatus(HttpStatus.CONFLICT, () -> service.save(11L, day, request(null, null, "缺少记录 ID", first.getVersion())));
        assertStatus(HttpStatus.CONFLICT, () -> service.save(11L, day, request(null, null, "错误记录 ID", first.getVersion(), first.getId() + 1)));
        service.save(11L, day, request(null, null, "较新", first.getVersion(), first.getId()));

        assertStatus(HttpStatus.CONFLICT, () -> service.save(11L, day, request(null, null, "过期", first.getVersion(), first.getId())));
        assertStatus(HttpStatus.CONFLICT, () -> service.save(11L, day, request(null, null, "重复创建", null)));
        assertThat(service.get(11L, day).getRecord().getDiary()).isEqualTo("较新");
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void staleSaveAfterDeletionDoesNotResurrectRecord() {
        DailyRecordResponse first = service.save(11L, day, request(null, null, "初始", null));
        service.delete(11L, day, first.getId(), first.getVersion());

        assertStatus(HttpStatus.CONFLICT, () -> service.save(11L, day, request(null, null, "过期", first.getVersion(), first.getId())));
        assertThat(repository.count()).isZero();
    }

    @Test
    void deletionChecksVersionAndOwner() {
        DailyRecordResponse first = service.save(11L, day, request(null, null, "自己的", null));
        service.save(22L, day, request(null, null, "他人的", null));
        DailyRecordResponse latest = service.save(11L, day, request(null, null, "更新", first.getVersion(), first.getId()));

        assertStatus(HttpStatus.CONFLICT, () -> service.delete(11L, day, first.getId(), first.getVersion()));
        assertThat(service.get(11L, day).getRecord()).isNotNull();
        service.delete(11L, day, latest.getId(), latest.getVersion());

        assertThat(service.get(11L, day).getRecord()).isNull();
        assertThat(service.get(22L, day).getRecord().getDiary()).isEqualTo("他人的");
        assertStatus(HttpStatus.NOT_FOUND, () -> service.delete(11L, day, latest.getId(), latest.getVersion()));
    }

    @Test
    void blankDiaryIsNormalizedButNonblankTextIsPreserved() {
        DailyRecordResponse numeric = service.save(11L, day, request("0.1", null, " \n\t　", null));
        assertThat(numeric.getDiary()).isNull();
        DailyRecordResponse text = service.save(11L, day, request(null, null, "  日记\n正文  ", numeric.getVersion(), numeric.getId()));
        assertThat(text.getDiary()).isEqualTo("  日记\n正文  ");
        assertStatus(HttpStatus.BAD_REQUEST, () -> service.save(11L, day, request(null, null, " \n　", text.getVersion(), text.getId())));
        assertThat(service.get(11L, day).getRecord().getDiary()).isEqualTo(text.getDiary());
    }

    @Test
    void acceptsInclusiveNumericAndDiaryLimits() {
        DailyRecordResponse lower = service.save(11L, day, request("0.1", "0.1", null, null));
        DailyRecordResponse upper = service.save(11L, day, request("999.9", "300.0", "字".repeat(10000), lower.getVersion(), lower.getId()));
        assertThat(upper.getWeightKg()).isEqualByComparingTo("999.9");
        assertThat(upper.getHeightCm()).isEqualByComparingTo("300.0");
        assertThat(upper.getDiary()).hasSize(10000);
    }

    @Test
    void rejectsInvalidValuesBeforePersistence() {
        for (String weight : new String[]{"0", "-0.1", "1000", "1.01"}) {
            assertStatus(HttpStatus.BAD_REQUEST, () -> service.save(11L, day, request(weight, null, null, null)));
        }
        for (String height : new String[]{"0", "-0.1", "300.1", "1.01"}) {
            assertStatus(HttpStatus.BAD_REQUEST, () -> service.save(11L, day, request(null, height, null, null)));
        }
        assertStatus(HttpStatus.BAD_REQUEST, () -> service.save(11L, day, request(null, null, "字".repeat(10001), null)));
        assertStatus(HttpStatus.BAD_REQUEST, () -> service.save(11L, day, request(null, null, null, null)));
        assertStatus(HttpStatus.BAD_REQUEST, () -> service.save(11L, day, request(null, null, "日记", -1L)));
        assertThat(repository.count()).isZero();
    }

    @Test
    void databaseRejectsDuplicateOwnerAndDate() {
        service.save(11L, day, request("60.0", null, null, null));
        DailyRecord duplicate = new DailyRecord();
        duplicate.setOwnerUserId(11L);
        duplicate.setRecordDate(day);
        duplicate.setDiary("重复");
        assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(duplicate));
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void databaseVersionRejectsDetachedStaleUpdates() {
        DailyRecordResponse original = service.save(11L, day, request(null, null, "初始", null));
        DailyRecord detached = repository.findById(original.getId()).orElseThrow();
        service.save(11L, day, request(null, null, "较新", original.getVersion(), original.getId()));
        detached.setDiary("过期覆盖");

        assertThrows(OptimisticLockingFailureException.class, () -> repository.saveAndFlush(detached));
        assertThat(service.get(11L, day).getRecord().getDiary()).isEqualTo("较新");
    }

    @Test
    void staleUpdateAfterDeleteAndRecreationPreservesReplacement() {
        DailyRecordResponse original = service.save(11L, day, request(null, null, "原记录", null));
        service.delete(11L, day, original.getId(), original.getVersion());
        DailyRecordResponse replacement = service.save(11L, day, request(null, null, "重新创建", null));
        assertThat(replacement.getId()).isNotEqualTo(original.getId());
        assertThat(replacement.getVersion()).isEqualTo(original.getVersion());

        assertStatus(HttpStatus.CONFLICT, () -> service.save(11L, day,
                request(null, null, "过期覆盖", original.getVersion(), original.getId())));
        assertThat(service.get(11L, day).getRecord().getDiary()).isEqualTo("重新创建");
    }

    @Test
    void staleDeletionAfterDeleteAndRecreationPreservesReplacement() {
        DailyRecordResponse original = service.save(11L, day, request(null, null, "原记录", null));
        service.delete(11L, day, original.getId(), original.getVersion());
        DailyRecordResponse replacement = service.save(11L, day, request(null, null, "重新创建", null));
        assertThat(replacement.getVersion()).isEqualTo(original.getVersion());

        assertStatus(HttpStatus.CONFLICT, () -> service.delete(11L, day, original.getId(), original.getVersion()));
        assertThat(service.get(11L, day).getRecord().getId()).isEqualTo(replacement.getId());
    }

    private static void assertStatus(HttpStatus expected, Runnable action) {
        assertThat(assertThrows(ResponseStatusException.class, action::run).getStatus()).isEqualTo(expected);
    }

    private static DailyRecordRequest request(String weight, String height, String diary, Long version) {
        return request(weight, height, diary, version, null);
    }

    private static DailyRecordRequest request(String weight, String height, String diary, Long version, Long recordId) {
        DailyRecordRequest request = new DailyRecordRequest();
        request.setWeightKg(weight == null ? null : new BigDecimal(weight));
        request.setHeightCm(height == null ? null : new BigDecimal(height));
        request.setDiary(diary);
        request.setVersion(version);
        request.setRecordId(recordId);
        return request;
    }
}
