package com.core.reminder.service;

import com.common.reminder.model.UserFeedback;
import com.core.reminder.dto.UserFeedbackRequest;
import com.core.reminder.repository.UserFeedbackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户反馈服务类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserFeedbackService {

    private final UserFeedbackRepository userFeedbackRepository;

    /**
     * 提交用户反馈
     *
     * @param request 反馈请求
     * @param userId 用户ID，可为空
     * @return 保存的反馈实体
     */
    @Transactional
    public UserFeedback submitFeedback(UserFeedbackRequest request, Long userId) {
        log.info("用户提交反馈，用户ID: {}, 邮箱: {}, 内容长度: {}",
                userId, request.getEmail(), request.getMessage().length());

        UserFeedback feedback = new UserFeedback();
        feedback.setUserId(userId);
        feedback.setEmail(request.getEmail());
        feedback.setMessage(request.getMessage());

        UserFeedback savedFeedback = userFeedbackRepository.save(feedback);
        log.info("反馈保存成功，ID: {}", savedFeedback.getId());

        return savedFeedback;
    }
}
