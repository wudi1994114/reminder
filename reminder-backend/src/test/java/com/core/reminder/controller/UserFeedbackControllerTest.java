package com.core.reminder.controller;

import com.common.reminder.model.UserFeedback;
import com.core.reminder.dto.UserFeedbackRequest;
import com.core.reminder.service.UserFeedbackService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserFeedbackControllerTest {

    @Mock
    private UserFeedbackService userFeedbackService;

    @InjectMocks
    private UserFeedbackController userFeedbackController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(userFeedbackController)
                .setValidator(validator)
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void submitsValidAnonymousFeedback() throws Exception {
        UserFeedbackRequest request = new UserFeedbackRequest();
        request.setMessage("这是一个测试反馈");

        UserFeedback saved = new UserFeedback();
        saved.setId(12L);
        when(userFeedbackService.submitFeedback(any(UserFeedbackRequest.class), isNull())).thenReturn(saved);

        mockMvc.perform(post("/api/feedback/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.feedbackId").value(12))
                .andExpect(jsonPath("$.message").value("反馈提交成功，感谢您的宝贵意见！"));
    }

    @Test
    void rejectsEmptyMessage() throws Exception {
        UserFeedbackRequest request = new UserFeedbackRequest();
        request.setMessage("");

        mockMvc.perform(post("/api/feedback/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsMessageLongerThanLimit() throws Exception {
        UserFeedbackRequest request = new UserFeedbackRequest();
        request.setMessage("a".repeat(2001));

        mockMvc.perform(post("/api/feedback/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
