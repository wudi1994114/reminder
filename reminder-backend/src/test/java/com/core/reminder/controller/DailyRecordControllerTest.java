package com.core.reminder.controller;

import com.common.reminder.dto.UserProfileDto;
import com.core.reminder.dto.DailyRecordDayResponse;
import com.core.reminder.dto.DailyRecordRequest;
import com.core.reminder.dto.DailyRecordResponse;
import com.core.reminder.dto.MeasurementTrendPoint;
import com.core.reminder.exception.GlobalExceptionHandler;
import com.core.reminder.service.DailyRecordService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class DailyRecordControllerTest {
    @Mock private DailyRecordService service;
    private MockMvc mvc;
    private final LocalDate day = LocalDate.of(2026, 10, 27);
    private UserProfileDto user;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new DailyRecordController(service))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(Jackson2ObjectMapperBuilder.json().build()))
                .setControllerAdvice(new GlobalExceptionHandler()).setValidator(validator).build();
        user = new UserProfileDto();
        user.setId(11L);
    }

    @Test
    void requiresAuthenticatedRequestUserForEveryEndpoint() throws Exception {
        mvc.perform(get("/api/daily-records").param("month", "2026-10")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/daily-records/2026-10-27")).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/daily-records/2026-10-27").contentType(MediaType.APPLICATION_JSON).content("{\"diary\":\"日记\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/daily-records/2026-10-27").param("version", "0").param("recordId", "7")).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test
    void monthlyQueryUsesOnlyContextOwnerAndReturnsArray() throws Exception {
        when(service.list(11L, YearMonth.of(2026, 10))).thenReturn(List.of(record()));
        mvc.perform(get("/api/daily-records").requestAttr("currentUser", user)
                        .param("month", "2026-10").param("ownerUserId", "22"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].recordDate").value("2026-10-27"))
                .andExpect(jsonPath("$[0].ownerUserId").doesNotExist());
        verify(service).list(11L, YearMonth.of(2026, 10));
    }

    @Test
    void dayResponseContainsExplicitNullRecordAndHistoricalHeight() throws Exception {
        when(service.get(11L, day)).thenReturn(new DailyRecordDayResponse(null,
                new DailyRecordDayResponse.LatestHeight(day.minusDays(1), new BigDecimal("170.1"))));
        mvc.perform(get("/api/daily-records/2026-10-27").requestAttr("currentUser", user))
                .andExpect(status().isOk()).andExpect(content().string(containsString("\"record\":null")))
                .andExpect(jsonPath("$.latestHeight.recordDate").value("2026-10-26"))
                .andExpect(jsonPath("$.latestHeight.heightCm").value(170.1));
    }

    @Test
    void putIgnoresSpoofedOwnerAndReturnsTheSavedRecord() throws Exception {
        when(service.save(eq(11L), eq(day), any())).thenReturn(record());
        mvc.perform(put("/api/daily-records/2026-10-27").requestAttr("currentUser", user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ownerUserId\":22,\"weightKg\":60.1,\"diary\":\"日记\",\"version\":null}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.weightKg").value(60.1)).andExpect(jsonPath("$.version").value(0))
                .andExpect(jsonPath("$.createdAt").exists()).andExpect(jsonPath("$.ownerUserId").doesNotExist());
        verify(service).save(eq(11L), eq(day), any(DailyRecordRequest.class));
    }

    @Test
    void malformedCalendarValuesReturnChineseBadRequestMessage() throws Exception {
        for (String month : new String[]{"2026-2", "2026-13", "0000-01", "bad"}) {
            mvc.perform(get("/api/daily-records").requestAttr("currentUser", user).param("month", month))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(containsString("月份")));
        }
        for (String date : new String[]{"2026-02-30", "2026-2-02", "0000-01-01", "bad"}) {
            mvc.perform(get("/api/daily-records/" + date).requestAttr("currentUser", user))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(containsString("日期")));
        }
        verifyNoInteractions(service);
    }

    @Test
    void invalidNumericDiaryAndVersionPayloadsAreRejectedBeforeService() throws Exception {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        for (String body : new String[]{"{\"weightKg\":0}", "{\"weightKg\":1000}", "{\"weightKg\":60.11}",
                "{\"heightCm\":0}", "{\"heightCm\":300.1}", "{\"heightCm\":170.11}",
                "{\"diary\":\"文字\",\"version\":-1}", "{\"diary\":\"文字\",\"version\":0.5}", "{\"diary\":\"文字\",\"recordId\":0}",
                "{\"diary\":\"文字\",\"recordId\":1.5}", "{\"weightKg\":\"abc\"}",
                mapper.writeValueAsString(java.util.Map.of("diary", "字".repeat(10001)))}) {
            mvc.perform(put("/api/daily-records/2026-10-27").requestAttr("currentUser", user)
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").isNotEmpty());
        }
        verifyNoInteractions(service);
    }

    @Test
    void conflictStatusAndChineseMessageSurviveGlobalExceptionAdvice() throws Exception {
        when(service.save(eq(11L), eq(day), any())).thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "记录已更新，请刷新后重试"));
        mvc.perform(put("/api/daily-records/2026-10-27").requestAttr("currentUser", user)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"diary\":\"日记\",\"recordId\":7,\"version\":0}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("记录已更新，请刷新后重试"));
    }

    @Test
    void deletionUsesContextOwnerAndRequiresValidVersion() throws Exception {
        mvc.perform(delete("/api/daily-records/2026-10-27").requestAttr("currentUser", user).param("recordId", "7").param("version", "0"))
                .andExpect(status().isNoContent());
        verify(service).delete(11L, day, 7L, 0L);
        for (String version : new String[]{"-1", "abc"}) {
            mvc.perform(delete("/api/daily-records/2026-10-27").requestAttr("currentUser", user).param("recordId", "7").param("version", version))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(delete("/api/daily-records/2026-10-27").requestAttr("currentUser", user))
                .andExpect(status().isBadRequest());
        for (String recordId : new String[]{"0", "-1", "abc", "7.5"}) {
            mvc.perform(delete("/api/daily-records/2026-10-27").requestAttr("currentUser", user).param("recordId", recordId).param("version", "0"))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(delete("/api/daily-records/2026-10-27").requestAttr("currentUser", user).param("version", "0"))
                .andExpect(status().isBadRequest());
        verifyNoMoreInteractions(service);
    }

    @Test
    void trendStaticRouteUsesContextOwnerAndReturnsOnlyDateAndValue() throws Exception {
        when(service.trend(11L, "weightKg", day, 30)).thenReturn(
                List.of(new MeasurementTrendPoint(day, new BigDecimal("62.5"))));
        mvc.perform(get("/api/daily-records/trend").requestAttr("currentUser", user)
                        .param("metric", "weightKg").param("end", "2026-10-27").param("days", "30")
                        .param("ownerUserId", "22"))
                .andExpect(status().isOk())
                .andExpect(content().json("[{\"recordDate\":\"2026-10-27\",\"value\":62.5}]", true));
        verify(service).trend(11L, "weightKg", day, 30);
        verifyNoMoreInteractions(service);
    }

    @Test
    void trendRequiresAuthenticatedContext() throws Exception {
        mvc.perform(get("/api/daily-records/trend").param("metric", "weightKg")
                        .param("end", "2026-10-27").param("days", "30"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.message").value(containsString("登录")));
        verifyNoInteractions(service);
    }

    @Test
    void trendSupportsBothMetricsAndAllWindowsIncludingEmptyResults() throws Exception {
        when(service.trend(eq(11L), anyString(), eq(day), anyInt())).thenReturn(List.of());
        for (String metric : new String[]{"weightKg", "heightCm"}) {
            for (int days : new int[]{30, 90, 365}) {
                mvc.perform(get("/api/daily-records/trend").requestAttr("currentUser", user)
                                .param("metric", metric).param("end", "2026-10-27").param("days", String.valueOf(days)))
                        .andExpect(status().isOk()).andExpect(content().json("[]", true));
                verify(service).trend(11L, metric, day, days);
            }
        }
        verifyNoMoreInteractions(service);
    }

    @Test
    void trendRejectsMalformedDatesWindowsAndMissingParameters() throws Exception {
        for (String date : new String[]{"2026-02-30", "2026-1-01", "0000-01-01", "+2026-10-27", "2026-10-27T00:00"}) {
            mvc.perform(get("/api/daily-records/trend").requestAttr("currentUser", user)
                            .param("metric", "weightKg").param("end", date).param("days", "30"))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(containsString("日期")));
        }
        for (String days : new String[]{"0", "-1", "31", "30.0", "030", "+30", "bad", "2147483648"}) {
            mvc.perform(get("/api/daily-records/trend").requestAttr("currentUser", user)
                            .param("metric", "weightKg").param("end", "2026-10-27").param("days", days))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(containsString("天数")));
        }
        for (String missing : new String[]{"metric", "end", "days"}) {
            var request = get("/api/daily-records/trend").requestAttr("currentUser", user);
            if (!missing.equals("metric")) request.param("metric", "weightKg");
            if (!missing.equals("end")) request.param("end", "2026-10-27");
            if (!missing.equals("days")) request.param("days", "30");
            mvc.perform(request).andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").isNotEmpty());
        }
        verifyNoInteractions(service);
    }

    private DailyRecordResponse record() {
        OffsetDateTime now = OffsetDateTime.parse("2026-10-27T10:00:00+08:00");
        return new DailyRecordResponse(7L, day, new BigDecimal("60.1"), null, "日记", 0L, now, now);
    }
}
