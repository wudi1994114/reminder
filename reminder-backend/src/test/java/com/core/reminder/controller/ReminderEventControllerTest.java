package com.core.reminder.controller;

import com.common.reminder.dto.UserProfileDto;
import com.common.reminder.model.ComplexReminder;
import com.core.reminder.dto.ComplexReminderDTO;
import com.core.reminder.service.ReminderEventServiceImpl;
import com.core.reminder.utils.ReminderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReminderEventControllerTest {

    @Mock
    private ReminderEventServiceImpl reminderService;

    @Mock
    private ReminderMapper reminderMapper;

    private ReminderEventController controller;
    private UserProfileDto currentUser;

    @BeforeEach
    void setUp() {
        controller = new ReminderEventController(reminderService, reminderMapper);
        currentUser = new UserProfileDto(7L, "user", null, null, null, null);
    }

    @Test
    void createsComplexReminderAndItsExecutionsInTheSameServiceCall() {
        ComplexReminderDTO request = ComplexReminderDTO.builder().title("喝水").build();
        ComplexReminder entity = new ComplexReminder();
        ComplexReminder saved = new ComplexReminder();
        saved.setId(11L);
        ComplexReminderDTO responseBody = ComplexReminderDTO.builder().id(11L).build();

        when(reminderMapper.toEntity(request)).thenReturn(entity);
        when(reminderService.createComplexReminderWithSimpleReminders(entity, 3)).thenReturn(saved);
        when(reminderMapper.toDTO(saved)).thenReturn(responseBody);

        ResponseEntity<ComplexReminderDTO> response =
                controller.createComplexReminder(request, currentUser, null);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(responseBody, response.getBody());
        verify(reminderService).createComplexReminderWithSimpleReminders(entity, 3);
    }

    @Test
    void updatesComplexReminderAndRegeneratesItsExecutionsInTheSameServiceCall() {
        ComplexReminderDTO request = ComplexReminderDTO.builder().title("运动").build();
        ComplexReminder existing = new ComplexReminder();
        existing.setId(12L);
        existing.setFromUserId(7L);
        existing.setToUserId(7L);
        ComplexReminder updated = new ComplexReminder();
        updated.setId(12L);
        ComplexReminderDTO responseBody = ComplexReminderDTO.builder().id(12L).build();

        when(reminderService.getComplexReminderById(12L)).thenReturn(Optional.of(existing));
        when(reminderService.updateComplexReminderWithSimpleReminders(existing, 3)).thenReturn(updated);
        when(reminderMapper.toDTO(updated)).thenReturn(responseBody);

        ResponseEntity<ComplexReminderDTO> response =
                controller.updateComplexReminder(12L, request, currentUser);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(responseBody, response.getBody());
        verify(reminderService).updateComplexReminderWithSimpleReminders(existing, 3);
    }
}
