package com.core.reminder.config;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.common.reminder.dto.UserProfileDto;
import com.core.reminder.controller.DailyRecordController;
import com.core.reminder.dto.DailyRecordDayResponse;
import com.core.reminder.dto.DailyRecordResponse;
import com.core.reminder.service.DailyRecordService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DailyRecordRequestLoggingFilterTest {
    private final Logger logger = (Logger) LoggerFactory.getLogger(RequestLoggingFilter.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    @BeforeEach
    void captureFilterLogs() {
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void stopCapture() {
        logger.detachAppender(appender);
        appender.stop();
    }

    @Test
    void dailyCollectionAndSubpathsLogMetadataWithoutPrivatePayloadsOrQueries() throws Exception {
        for (String path : new String[]{"/api/daily-records", "/api/daily-records/2026-10-27", "/api/daily-records/trend"}) {
            MockHttpServletRequest request = new MockHttpServletRequest("PUT", path);
            request.setContent("{\"diary\":\"request-private-sentinel\",\"weightKg\":60.1}".getBytes(StandardCharsets.UTF_8));
            request.setQueryString("month=2026-10&diary=query-private-sentinel");
            MockHttpServletResponse response = new MockHttpServletResponse();

            new RequestLoggingFilter().doFilter(request, response, (req, res) -> {
                req.getInputStream().readAllBytes();
                res.getWriter().write("{\"diary\":\"response-private-sentinel\"}");
            });

            String logs = capturedLogs();
            assertThat(logs).contains("PUT", path).doesNotContain("private-sentinel", "weightKg", "month=");
            assertThat(response.getContentAsString()).contains("response-private-sentinel");
        }
    }

    @Test
    void dailyGetUnderServletContextAlsoKeepsReturnedDiaryOutOfLogs() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/reminder/api/daily-records");
        request.setContextPath("/reminder");
        request.setQueryString("month=2026-10");
        MockHttpServletResponse response = new MockHttpServletResponse();
        new RequestLoggingFilter().doFilter(request, response, (req, res) -> {
            res.getWriter().write("[{\"diary\":\"historical-private-sentinel\"}]");
        });

        assertThat(capturedLogs()).contains("GET", "/reminder/api/daily-records").doesNotContain("private-sentinel");
        assertThat(response.getContentAsString()).contains("historical-private-sentinel");
    }

    @Test
    void nonDailyEndpointsRetainExistingPayloadLogging() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/holidays");
        MockHttpServletResponse response = new MockHttpServletResponse();
        new RequestLoggingFilter().doFilter(request, response, (req, res) -> res.getWriter().write("public-test-response"));

        assertThat(capturedLogs()).contains("GET", "/api/holidays", "public-test-response");
        assertThat(response.getContentAsString()).isEqualTo("public-test-response");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/%64aily-records?month=2026-10",
            "/api/daily-records;foo=bar?month=2026-10",
            "/api/%64aily-records/2026-10-27",
            "/api/daily-records;foo=bar/2026-10-27"
    })
    void encodedAndMatrixAliasesMapToDailyControllerWithoutLoggingReturnedDiary(String uri) throws Exception {
        DailyRecordService service = mock(DailyRecordService.class);
        LocalDate day = LocalDate.of(2026, 10, 27);
        OffsetDateTime now = OffsetDateTime.parse("2026-10-27T10:00:00+08:00");
        DailyRecordResponse record = new DailyRecordResponse(7L, day, null, null,
                "alias-private-sentinel", 0L, now, now);
        when(service.list(11L, YearMonth.of(2026, 10))).thenReturn(List.of(record));
        when(service.get(11L, day)).thenReturn(new DailyRecordDayResponse(record, null));
        UserProfileDto user = new UserProfileDto();
        user.setId(11L);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new DailyRecordController(service))
                .addFilter(new RequestLoggingFilter()).build();

        mvc.perform(get(URI.create(uri)).requestAttr("currentUser", user))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("alias-private-sentinel")));

        assertThat(capturedLogs()).contains("GET", URI.create(uri).getRawPath()).doesNotContain("private-sentinel", "month=");
    }

    private String capturedLogs() {
        return appender.list.stream().map(ILoggingEvent::getFormattedMessage).collect(Collectors.joining("\n"));
    }
}
