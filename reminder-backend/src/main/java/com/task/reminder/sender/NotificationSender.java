package com.task.reminder.sender;

/**
 * 通知发送器接口
 * 定义统一的通知发送方法，支持多种通知渠道（邮件、微信等）
 */
public interface NotificationSender {
    
    /**
     * 发送通知
     *
     * @param recipient 接收者标识（邮箱地址、微信openid等）
     * @param title 通知标题
     * @param content 通知内容
     * @param extraData 额外数据（如微信模板消息的data字段）
     * @return 发送结果，成功返回true，失败返回false
     */
    boolean sendNotification(String recipient, String title, String content, Object extraData);
    
    /**
     * 获取发送器类型名称
     *
     * @return 发送器类型名称（如：EMAIL, WECHAT）
     */
    String getSenderType();
    
    /**
     * 检查接收者标识是否有效
     *
     * @param recipient 接收者标识
     * @return 是否有效
     */
    boolean isValidRecipient(String recipient);
}