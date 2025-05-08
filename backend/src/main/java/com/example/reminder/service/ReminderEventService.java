package com.example.reminder.service;

import com.example.reminder.model.ReminderEvent;
import java.util.List;

public interface ReminderEventService {

    ReminderEvent createEvent(ReminderEvent event);

    List<ReminderEvent> getAllEvents();

    // 后续可以添加更新、删除事件及对应的任务调度逻辑
    // ReminderEvent updateEvent(Long id, ReminderEvent eventDetails);
    // void deleteEvent(Long id);
} 