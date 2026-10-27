package com.core.reminder.controller;

import com.common.reminder.dto.UserProfileDto;
import com.core.reminder.dto.DailyRecordDayResponse;
import com.core.reminder.dto.DailyRecordRequest;
import com.core.reminder.dto.DailyRecordResponse;
import com.core.reminder.dto.MeasurementTrendPoint;
import com.core.reminder.service.DailyRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.Valid;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/daily-records")
@RequiredArgsConstructor
public class DailyRecordController {
    private final DailyRecordService service;

    @GetMapping
    public List<DailyRecordResponse> list(
            @RequestAttribute(value = "currentUser", required = false) UserProfileDto user,
            @RequestParam String month) {
        return service.list(owner(user), parseMonth(month));
    }

    @GetMapping("/trend")
    public List<MeasurementTrendPoint> trend(
            @RequestAttribute(value = "currentUser", required = false) UserProfileDto user,
            @RequestParam String metric, @RequestParam String end, @RequestParam String days) {
        return service.trend(owner(user), metric, parseDate(end), parseTrendDays(days));
    }

    @GetMapping("/{date}")
    public DailyRecordDayResponse get(
            @RequestAttribute(value = "currentUser", required = false) UserProfileDto user,
            @PathVariable String date) {
        return service.get(owner(user), parseDate(date));
    }

    @PutMapping("/{date}")
    public DailyRecordResponse save(
            @RequestAttribute(value = "currentUser", required = false) UserProfileDto user,
            @PathVariable String date, @Valid @RequestBody DailyRecordRequest request) {
        return service.save(owner(user), parseDate(date), request);
    }

    @DeleteMapping("/{date}")
    public ResponseEntity<Void> delete(
            @RequestAttribute(value = "currentUser", required = false) UserProfileDto user,
            @PathVariable String date, @RequestParam Long recordId, @RequestParam Long version) {
        Long ownerUserId = owner(user);
        if (version < 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "版本号必须为非负整数");
        if (recordId < 1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "记录 ID 必须为正整数");
        service.delete(ownerUserId, parseDate(date), recordId, version);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleStatus(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatus()).body(Map.of("message", ex.getReason() == null ? "请求失败，请重试" : ex.getReason()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getAllErrors().stream()
                .map(error -> error.getDefaultMessage() == null ? "请检查每日记录内容" : error.getDefaultMessage())
                .findFirst().orElse("请检查每日记录内容");
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Map<String, String>> handleInvalidRequest(Exception ex) {
        return ResponseEntity.badRequest().body(Map.of("message", "请求参数无效，请检查月份、日期、指标、天数、数值和版本号"));
    }

    private static Long owner(UserProfileDto user) {
        if (user == null || user.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录后再使用每日记录");
        }
        return user.getId();
    }

    private static YearMonth parseMonth(String month) {
        try {
            if (!month.matches("[0-9]{4}-[0-9]{2}")) throw new DateTimeParseException("format", month, 0);
            YearMonth parsed = YearMonth.parse(month);
            if (parsed.getYear() < 1) throw new DateTimeParseException("year", month, 0);
            return parsed;
        } catch (DateTimeParseException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "月份格式应为 YYYY-MM，且为有效月份");
        }
    }

    private static int parseTrendDays(String days) {
        switch (days) {
            case "30": return 30;
            case "90": return 90;
            case "365": return 365;
            default: throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "天数只支持 30、90 或 365");
        }
    }

    private static LocalDate parseDate(String date) {
        try {
            if (!date.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) throw new DateTimeParseException("format", date, 0);
            LocalDate parsed = LocalDate.parse(date);
            if (parsed.getYear() < 1) throw new DateTimeParseException("year", date, 0);
            return parsed;
        } catch (DateTimeParseException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "日期格式应为 YYYY-MM-DD，且为有效日期");
        }
    }
}
