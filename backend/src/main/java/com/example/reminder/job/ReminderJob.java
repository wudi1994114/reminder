package com.example.reminder.job;

import com.example.reminder.model.AppUser;
import com.example.reminder.model.ReminderExecutionHistory;
import com.example.reminder.model.SimpleReminder;
import com.example.reminder.repository.AppUserRepository;
import com.example.reminder.repository.ReminderExecutionHistoryRepository;
import com.example.reminder.repository.SimpleReminderRepository;
import com.example.reminder.service.MailService;
import com.example.reminder.model.ReminderType;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Optional;

@Component
public class ReminderJob implements Job {

    private static final Logger log = LoggerFactory.getLogger(ReminderJob.class);

    @Autowired
    private MailService mailService;
    @Autowired
    private SimpleReminderRepository simpleReminderRepository;
    @Autowired
    private AppUserRepository appUserRepository;
    @Autowired
    private ReminderExecutionHistoryRepository historyRepository;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        Long simpleReminderId = context.getJobDetail().getJobDataMap().getLong("simpleReminderId");
        String eventTitle = context.getJobDetail().getJobDataMap().getString("eventTitle");

        log.info("Executing reminder job for SimpleReminder ID: {}, Title: {}", simpleReminderId, eventTitle);

        SimpleReminder reminder = null;
        AppUser toUser = null;
        String status = "FAILURE";
        String details = "";

        try {
            Optional<SimpleReminder> reminderOpt = simpleReminderRepository.findById(simpleReminderId);
            if (!reminderOpt.isPresent()) {
                details = "SimpleReminder with ID " + simpleReminderId + " not found.";
                log.warn(details);
                saveExecutionHistory(context, simpleReminderId, null, null, "SIMPLE", status, details, null, null);
                return;
            }
            reminder = reminderOpt.get();

            Optional<AppUser> userOpt = appUserRepository.findById(reminder.getToUserId());
            if (!userOpt.isPresent()) {
                details = "Recipient user with ID " + reminder.getToUserId() + " not found for reminder ID " + simpleReminderId;
                log.warn(details);
                saveExecutionHistory(context, simpleReminderId, reminder, null, "SIMPLE", status, details, reminder.getReminderType().name(), reminder.getEventTime());
                return;
            }
            toUser = userOpt.get();

            ReminderType reminderType = reminder.getReminderType();
            String actualMethod = reminderType.name();

            if (reminderType == ReminderType.EMAIL) {
                if (toUser.getEmail() == null || toUser.getEmail().trim().isEmpty()) {
                    details = "Recipient user ID " + toUser.getId() + " has no email address.";
                    log.error(details);
                } else {
                    String subject = "Reminder: " + reminder.getTitle();
                    String body = "This is a reminder for your event: \n" +
                                  "Title: " + reminder.getTitle() + "\n" +
                                  "Description: " + (reminder.getDescription() != null ? reminder.getDescription() : "N/A") + "\n" +
                                  "Time: " + reminder.getEventTime().toString();
                    try {
                        mailService.sendReminderEmail(toUser.getEmail(), subject, body);
                        status = "SUCCESS";
                        details = "Email sent successfully to " + toUser.getEmail();
                        log.info(details);
                    } catch (Exception e) {
                        details = "Failed to send email to " + toUser.getEmail() + ": " + e.getMessage();
                        log.error(details, e);
                    }
                }
            } else if (reminderType == ReminderType.SMS) {
                log.info("SMS reminder requested for event ID: {}. SMS sending not implemented yet.", simpleReminderId);
                details = "SMS sending not implemented yet for user ID " + toUser.getId();
            } else {
                details = "Unsupported reminder type: " + reminderType + " for reminder ID " + simpleReminderId;
                log.warn(details);
            }

            saveExecutionHistory(context, simpleReminderId, reminder, toUser, "SIMPLE", status, details, actualMethod, reminder.getEventTime());

        } catch (Exception e) {
            log.error("Unexpected error executing job for SimpleReminder ID: {}", simpleReminderId, e);
            details = "Unexpected error: " + e.getMessage();
            saveExecutionHistory(context, simpleReminderId, reminder, toUser, "SIMPLE", "FAILURE", details, (reminder != null ? reminder.getReminderType().name() : null), (reminder != null ? reminder.getEventTime() : null));
        }
    }

    private void saveExecutionHistory(JobExecutionContext context, Long reminderId, SimpleReminder reminder, AppUser toUser, String reminderType, String status, String details, String actualMethod, OffsetDateTime scheduledTime) {
        try {
            ReminderExecutionHistory history = new ReminderExecutionHistory();
            history.setTriggeringReminderType(reminderType);
            history.setTriggeringReminderId(reminderId);
            history.setStatus(status);
            history.setDetails(details);

            if (reminder != null) {
                history.setFromUserId(reminder.getFromUserId());
                history.setToUserId(reminder.getToUserId());
                history.setTitle(reminder.getTitle());
                history.setDescription(reminder.getDescription());
                history.setActualReminderMethod(actualMethod != null ? actualMethod : reminder.getReminderType().name());
                history.setScheduledEventTime(scheduledTime);
            }

            historyRepository.save(history);
            log.debug("Saved execution history for reminder ID: {} with status: {}", reminderId, status);

        } catch (Exception e) {
            log.error("Failed to save execution history for reminder ID: {}", reminderId, e);
        }
    }
} 