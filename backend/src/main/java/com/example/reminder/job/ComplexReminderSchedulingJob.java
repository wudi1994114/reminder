package com.example.reminder.job;

import com.example.reminder.model.ComplexReminder;
import com.example.reminder.model.SimpleReminder;
import com.example.reminder.repository.ComplexReminderRepository;
import com.example.reminder.repository.SimpleReminderRepository;
import com.example.reminder.service.ReminderEventServiceImpl; // Use Service Interface later

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.support.CronSequenceGenerator; // For CRON parsing
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

/**
 * Quartz Job responsible for checking ComplexReminder templates
 * and generating SimpleReminder instances when they are due based on their CRON expression.
 */
@Component
@DisallowConcurrentExecution // Prevent multiple instances running simultaneously
public class ComplexReminderSchedulingJob implements Job {

    private static final Logger log = LoggerFactory.getLogger(ComplexReminderSchedulingJob.class);

    @Autowired
    private ComplexReminderRepository complexReminderRepository;

    @Autowired
    private SimpleReminderRepository simpleReminderRepository; // Needed for duplicate check

    @Autowired
    private ReminderEventServiceImpl reminderService; // Inject Service to create SimpleReminders

    @Override
    @Transactional // Wrap in transaction to ensure atomicity
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("Running ComplexReminderSchedulingJob...");

        List<ComplexReminder> templates = complexReminderRepository.findAll();
        if (templates.isEmpty()) {
            log.info("No complex reminder templates found.");
            return;
        }

        // Get the current time and the time for the next minute start (our check window)
        // Using Instant for time comparisons
        Instant now = Instant.now();
        Instant nextMinuteStart = now.plusSeconds(60).truncatedTo(java.time.temporal.ChronoUnit.MINUTES);


        for (ComplexReminder template : templates) {
            try {
                if (!CronSequenceGenerator.isValidExpression(template.getCronExpression())) {
                    log.warn("Invalid CRON expression '{}' for ComplexReminder ID: {}. Skipping.",
                             template.getCronExpression(), template.getId());
                    continue;
                }

                CronSequenceGenerator generator = new CronSequenceGenerator(template.getCronExpression());
                // Calculate the next execution time AFTER the current moment
                Date nextExecutionDate = generator.next(Date.from(now));
                Instant nextExecutionInstant = nextExecutionDate.toInstant();

                // Check if the calculated next execution time falls within the next minute
                if (!nextExecutionInstant.isBefore(nextMinuteStart)) {
                   // log.debug("Next execution for complex reminder {} ({}) is at {}, which is not within the next minute. Skipping.", template.getId(), template.getTitle(), nextExecutionInstant);
                    continue; // Not due in the next minute window
                }

                log.info("Complex reminder {} ('{}') is due at {}. Checking for duplicates and generating SimpleReminder.",
                         template.getId(), template.getTitle(), nextExecutionInstant);

                // Check if a SimpleReminder already exists for this template and this exact time
                // This prevents duplicates if the job runs slightly faster/slower than exactly 1 min
                OffsetDateTime nextExecutionOffsetTime = OffsetDateTime.ofInstant(nextExecutionInstant, ZoneId.systemDefault());

                // ** Refined Duplicate Check **
                // Check if a simple reminder exists originating from this complex one AND scheduled exactly at the calculated next execution time.
                boolean exists = simpleReminderRepository.existsByOriginatingComplexReminderIdAndEventTime(template.getId(), nextExecutionOffsetTime);

                if (exists) {
                    log.warn("SimpleReminder already exists for ComplexReminder ID {} at {}. Skipping generation.", template.getId(), nextExecutionOffsetTime);
                    continue;
                }

                // Create and save the SimpleReminder instance
                SimpleReminder instance = createSimpleReminderFromTemplate(template, nextExecutionOffsetTime);
                reminderService.createSimpleReminder(instance);
                log.info("Successfully generated and scheduled SimpleReminder (ID: {}) from ComplexReminder ID: {}", instance.getId(), template.getId());

            } catch (Exception e) {
                log.error("Error processing ComplexReminder ID: {}. Cron: '{}'",
                          template.getId(), template.getCronExpression(), e);
                // Continue processing other templates
            }
        }
        log.info("ComplexReminderSchedulingJob finished.");
    }

    private SimpleReminder createSimpleReminderFromTemplate(ComplexReminder template, OffsetDateTime eventTime) {
        SimpleReminder instance = new SimpleReminder();
        instance.setFromUserId(template.getFromUserId());
        instance.setToUserId(template.getToUserId());
        instance.setTitle(template.getTitle()); // Use template title directly
        instance.setDescription(template.getDescription()); // Use template description
        instance.setEventTime(eventTime); // The calculated next execution time
        instance.setReminderType(template.getReminderType());
        instance.setOriginatingComplexReminderId(template.getId()); // Link back to the template
        // createdAt and updatedAt will be set automatically by Hibernate
        return instance;
    }
} 