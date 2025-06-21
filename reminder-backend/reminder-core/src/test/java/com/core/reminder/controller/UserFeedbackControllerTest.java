package com.core.reminder.controller;

import com.core.reminder.dto.UserFeedbackRequest;
import com.core.reminder.service.UserFeedbackService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * UserFeedbackController 测试类
 */
@WebMvcTest(UserFeedbackController.class)
public class UserFeedbackControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserFeedbackService userFeedbackService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testSubmitFeedback_Success() throws Exception {
        // 准备测试数据
        UserFeedbackRequest request = new UserFeedbackRequest();
        request.setMessage("这是一个测试反馈");

        // 执行测试
        mockMvc.perform(post("/api/feedback/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("反馈提交成功，感谢您的宝贵意见！"));
    }

    @Test
    public void testSubmitFeedback_EmptyMessage() throws Exception {
        // 准备测试数据 - 空消息
        UserFeedbackRequest request = new UserFeedbackRequest();
        request.setMessage("");

        // 执行测试
        mockMvc.perform(post("/api/feedback/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testSubmitFeedback_TooLongMessage() throws Exception {
        // 准备测试数据 - 超长消息
        UserFeedbackRequest request = new UserFeedbackRequest();
        StringBuilder longMessage = new StringBuilder();
        for (int i = 0; i < 2001; i++) {
            longMessage.append("a");
        }
        request.setMessage(longMessage.toString());

        // 执行测试
        mockMvc.perform(post("/api/feedback/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
