package com.core.reminder;

import com.core.reminder.config.ApplicationInitializer;
import com.core.reminder.controller.ReminderEventController;
import com.task.reminder.config.QuartzInitializer;
import com.task.reminder.job.MonthlyComplexReminderJob;
import com.task.reminder.job.PrepareReminderJob;
import com.task.reminder.job.SendReminderJob;
import com.task.reminder.sender.NotificationSenderFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(
        classes = ReminderApplication.class,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:reminder-context;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.sql.init.mode=never",
                "spring.quartz.job-store-type=memory",
                "spring.quartz.auto-startup=false",
                "nacos.config.enabled=false",
                "reminder.holiday.cache.enabled=false",
                "email.provider=none",
                "app.jwtSecret=test-only",
                "wechat.miniprogram.appid=test-appid",
                "wechat.miniprogram.secret=test-secret",
                "wechat.notification.template-id=test-template"
        })
@ActiveProfiles("test")
class ReminderApplicationContextTest {

    @MockBean
    private ApplicationInitializer applicationInitializer;

    @MockBean
    private QuartzInitializer quartzInitializer;

    @Autowired
    private ReminderEventController reminderEventController;

    @Autowired
    private PrepareReminderJob prepareReminderJob;

    @Autowired
    private SendReminderJob sendReminderJob;

    @Autowired
    private MonthlyComplexReminderJob monthlyComplexReminderJob;

    @Autowired
    private NotificationSenderFactory notificationSenderFactory;

    @Test
    void loadsHttpQuartzAndNotificationBeansInOneApplication() {
        assertNotNull(reminderEventController);
        assertNotNull(prepareReminderJob);
        assertNotNull(sendReminderJob);
        assertNotNull(monthlyComplexReminderJob);
        assertNotNull(notificationSenderFactory);
    }
}
