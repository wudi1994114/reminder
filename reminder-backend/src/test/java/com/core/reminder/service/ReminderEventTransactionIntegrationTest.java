package com.core.reminder.service;

import com.common.reminder.model.ComplexReminder;
import com.common.reminder.model.ReminderType;
import com.common.reminder.model.SimpleReminder;
import com.core.reminder.ReminderApplication;
import com.core.reminder.config.ApplicationInitializer;
import com.core.reminder.repository.ComplexReminderRepository;
import com.core.reminder.repository.SimpleReminderRepository;
import com.core.reminder.utils.CacheUtils;
import com.task.reminder.config.QuartzInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(
        classes = ReminderApplication.class,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:reminder-rollback;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
                "spring.jpa.hibernate.ddl-auto=none",
                "spring.sql.init.mode=never",
                "spring.quartz.job-store-type=memory",
                "spring.quartz.auto-startup=false",
                "nacos.config.enabled=false",
                "reminder.holiday.cache.enabled=false",
                "email.provider=none",
                "app.jwtSecret=test-only",
                "wechat.miniprogram.appid=test-appid",
                "wechat.miniprogram.secret=test-secret"
        })
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ReminderEventTransactionIntegrationTest {

    @Autowired
    private ReminderEventServiceImpl reminderEventService;

    @Autowired
    private ComplexReminderRepository complexReminderRepository;

    @Autowired
    private SimpleReminderRepository simpleReminderRepository;

    @MockBean
    private RedisTemplate<String, Object> redisTemplate;

    @MockBean
    private CacheUtils cacheUtils;

    @MockBean
    private ApplicationInitializer applicationInitializer;

    @MockBean
    private QuartzInitializer quartzInitializer;

    @MockBean
    private UserActivityLogService userActivityLogService;

    @Test
    @Sql(scripts = "classpath:reminder-rollback-schema.sql")
    void updateRollsBackTemplateAndExecutionsWhenGenerationFails() {
        ComplexReminder originalTemplate = new ComplexReminder();
        originalTemplate.setFromUserId(101L);
        originalTemplate.setToUserId(101L);
        originalTemplate.setTitle("original-title");
        originalTemplate.setDescription("original-description");
        originalTemplate.setCronExpression("0 0 9 * * ?");
        originalTemplate.setReminderType(ReminderType.EMAIL);
        originalTemplate = complexReminderRepository.saveAndFlush(originalTemplate);

        SimpleReminder originalExecution = new SimpleReminder();
        originalExecution.setFromUserId(101L);
        originalExecution.setToUserId(101L);
        originalExecution.setTitle("original-execution");
        originalExecution.setDescription("original-description");
        originalExecution.setEventTime(OffsetDateTime.now().plusDays(1));
        originalExecution.setReminderType(ReminderType.EMAIL);
        originalExecution.setOriginatingComplexReminderId(originalTemplate.getId());
        simpleReminderRepository.saveAndFlush(originalExecution);

        ComplexReminder invalidUpdate = complexReminderRepository.findById(originalTemplate.getId()).orElseThrow();
        invalidUpdate.setTitle("should-not-commit");
        invalidUpdate.setCronExpression("invalid cron expression");

        assertThrows(IllegalStateException.class,
                () -> reminderEventService.updateComplexReminderWithSimpleReminders(invalidUpdate, 1));

        ComplexReminder reloadedTemplate = complexReminderRepository.findById(originalTemplate.getId()).orElseThrow();
        assertEquals("original-title", reloadedTemplate.getTitle());
        assertEquals("0 0 9 * * ?", reloadedTemplate.getCronExpression());

        List<SimpleReminder> executions =
                simpleReminderRepository.findByOriginatingComplexReminderId(originalTemplate.getId());
        assertEquals(1, executions.size());
        assertEquals("original-execution", executions.get(0).getTitle());
    }
}
