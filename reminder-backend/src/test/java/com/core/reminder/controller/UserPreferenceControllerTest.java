package com.core.reminder.controller;

import com.common.reminder.dto.UserProfileDto;
import com.core.reminder.dto.UserPreferenceDto;
import com.core.reminder.enums.UserPreferenceKey;
import com.core.reminder.service.UserPreferenceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserPreferenceControllerTest {

    private static final UserProfileDto CURRENT_USER =
            new UserProfileDto(7L, "user", null, null, null, null);

    @Mock
    private UserPreferenceService userPreferenceService;

    @InjectMocks
    private UserPreferenceController controller;

    @Test
    void returnsEnumDefaultWhenKnownPreferenceHasNotBeenPersisted() {
        String key = UserPreferenceKey.USER_TAG_MANAGEMENT_ENABLED.getKey();
        when(userPreferenceService.getUserPreference(CURRENT_USER.getId(), key))
                .thenReturn(Optional.empty());

        ResponseEntity<UserPreferenceDto> response = controller.getUserPreference(CURRENT_USER, key);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(key, response.getBody().getKey());
        assertEquals(UserPreferenceKey.USER_TAG_MANAGEMENT_ENABLED.getDefaultValue(), response.getBody().getValue());
    }

    @Test
    void keepsNotFoundForUnknownPreference() {
        String key = "unknownPreference";
        when(userPreferenceService.getUserPreference(CURRENT_USER.getId(), key))
                .thenReturn(Optional.empty());

        ResponseEntity<UserPreferenceDto> response = controller.getUserPreference(CURRENT_USER, key);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
