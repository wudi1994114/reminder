package com.example.reminder.service;

public interface MailService {
    void sendReminderEmail(String to, String subject, String body);
} 