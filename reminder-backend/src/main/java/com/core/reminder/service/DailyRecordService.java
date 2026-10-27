package com.core.reminder.service;

import com.common.reminder.model.DailyRecord;
import com.core.reminder.dto.DailyRecordDayResponse;
import com.core.reminder.dto.DailyRecordRequest;
import com.core.reminder.dto.DailyRecordResponse;
import com.core.reminder.dto.MeasurementTrendPoint;
import com.core.reminder.repository.DailyRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.Validator;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DailyRecordService {
    private final DailyRecordRepository repository;
    private final Validator validator;

    public List<DailyRecordResponse> list(Long ownerUserId, YearMonth month) {
        requireOwner(ownerUserId);
        if (month == null || month.getYear() < 1 || month.getYear() > 9999) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "月份格式应为 YYYY-MM");
        }
        return repository.findByOwnerUserIdAndRecordDateGreaterThanEqualAndRecordDateLessThanOrderByRecordDateAsc(
                        ownerUserId, month.atDay(1), month.plusMonths(1).atDay(1)).stream()
                .map(DailyRecordResponse::from).collect(Collectors.toList());
    }

    public DailyRecordDayResponse get(Long ownerUserId, LocalDate date) {
        requireOwner(ownerUserId);
        requireDate(date);
        DailyRecordResponse record = repository.findByOwnerUserIdAndRecordDate(ownerUserId, date)
                .map(DailyRecordResponse::from).orElse(null);
        DailyRecordDayResponse.LatestHeight height = repository
                .findFirstByOwnerUserIdAndRecordDateLessThanEqualAndHeightCmIsNotNullOrderByRecordDateDesc(ownerUserId, date)
                .map(measured -> new DailyRecordDayResponse.LatestHeight(measured.getRecordDate(), measured.getHeightCm()))
                .orElse(null);
        return new DailyRecordDayResponse(record, height);
    }

    public List<MeasurementTrendPoint> trend(Long ownerUserId, String metric, LocalDate end, int days) {
        requireOwner(ownerUserId);
        requireDate(end);
        if (!"weightKg".equals(metric) && !"heightCm".equals(metric)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "指标只支持 weightKg 或 heightCm");
        }
        if (days != 30 && days != 90 && days != 365) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "天数只支持 30、90 或 365");
        }
        LocalDate start = end.minusDays(days - 1L);
        LocalDate oldest = LocalDate.of(1, 1, 1);
        if (start.isBefore(oldest)) start = oldest;
        return "weightKg".equals(metric) ? repository.findWeightTrend(ownerUserId, start, end)
                : repository.findHeightTrend(ownerUserId, start, end);
    }

    @Transactional
    public DailyRecordResponse save(Long ownerUserId, LocalDate date, DailyRecordRequest request) {
        requireOwner(ownerUserId);
        requireDate(date);
        if (request == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请填写每日记录");
        validator.validate(request).stream().findFirst().ifPresent(violation -> {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, violation.getMessage());
        });

        DailyRecord record = repository.findByOwnerUserIdAndRecordDate(ownerUserId, date).orElse(null);
        if (record == null) {
            if (request.getVersion() != null || request.getRecordId() != null) throw conflict();
            record = new DailyRecord();
            record.setOwnerUserId(ownerUserId);
            record.setRecordDate(date);
        } else if (!Objects.equals(record.getId(), request.getRecordId())
                || !Objects.equals(record.getVersion(), request.getVersion())) {
            throw conflict();
        }
        record.setWeightKg(request.getWeightKg());
        record.setHeightCm(request.getHeightCm());
        record.setDiary(request.normalizedDiary());
        try {
            return DailyRecordResponse.from(repository.saveAndFlush(record));
        } catch (DataIntegrityViolationException | OptimisticLockingFailureException ex) {
            // The unique owner/date key also protects two concurrent first saves.
            throw conflict();
        }
    }

    @Transactional
    public void delete(Long ownerUserId, LocalDate date, Long recordId, Long version) {
        requireOwner(ownerUserId);
        requireDate(date);
        if (version == null || version < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请提供有效的记录版本号");
        }
        if (recordId == null || recordId < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请提供有效的记录 ID");
        }
        DailyRecord record = repository.findByOwnerUserIdAndRecordDate(ownerUserId, date)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "当天记录不存在或已删除"));
        if (!Objects.equals(record.getId(), recordId) || !Objects.equals(record.getVersion(), version)) throw conflict();
        try {
            repository.delete(record);
            repository.flush();
        } catch (OptimisticLockingFailureException ex) {
            throw conflict();
        }
    }

    private static ResponseStatusException conflict() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "记录已更新或删除，请刷新后重试；当前输入不会自动覆盖新记录");
    }

    private static void requireOwner(Long ownerUserId) {
        if (ownerUserId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录后再使用每日记录");
    }

    private static void requireDate(LocalDate date) {
        if (date == null || date.getYear() < 1 || date.getYear() > 9999) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "日期格式应为 YYYY-MM-DD");
        }
    }
}
