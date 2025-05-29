package com.task.reminder.sender;

import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.UserCredentials;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;
import java.util.Base64;

@Slf4j
@Service
public class GmailSender implements EmailSender {

    private static final String APPLICATION_NAME = "Reminder Application";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String USER_ID = "me"; // 代表已认证用户

    @Value("${gmail.client.id}")
    private String clientId;

    @Value("${gmail.client.secret}")
    private String clientSecret;

    @Value("${gmail.refresh.token}")
    private String refreshToken;

    @Value("${gmail.sender.email.address}")
    private String senderEmailAddress;

    // Gmail API 的 Scope
    private static final java.util.List<String> SCOPES =
            Collections.unmodifiableList(Arrays.asList("https://www.googleapis.com/auth/gmail.send"));

    private Gmail service = null;

    @PostConstruct
    public void initService() throws GeneralSecurityException, IOException {
        try {


            final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();

            // 使用注入的凭据
            GoogleCredentials credentials = UserCredentials.newBuilder()
                    .setClientId(clientId)
                    .setClientSecret(clientSecret)
                    .setRefreshToken(refreshToken)
                    .build();

            try {
                credentials.refreshAccessToken();
                System.out.println("Access token refreshed successfully using GoogleCredentials.");
            } catch (IOException e) {
                System.err.println("Failed to refresh access token using GoogleCredentials. Check your gmail.client.id, gmail.client.secret, and gmail.refresh.token in properties.");
                System.err.println("Ensure the refresh token was obtained with the correct scopes: " + SCOPES.toString());
                throw e;
            }

            HttpCredentialsAdapter adaptedCredentials = new HttpCredentialsAdapter(credentials);

            this.service = new Gmail.Builder(httpTransport, JSON_FACTORY, adaptedCredentials)
                    .setApplicationName(APPLICATION_NAME)
                    .build();
            System.out.println("Gmail service initialized successfully.");
        } catch (Exception e) {
            log.error("初始化Gmail服务时出错: ", e);
        }
    }

    /**
     * 创建一个MIME邮件。
     *
     * @param to       收件人邮箱地址。
     * @param subject  邮件主题。
     * @param bodyText 邮件正文。
     * @return MimeMessage对象。
     * @throws MessagingException 如果创建邮件失败。
     */
    private MimeMessage createEmail(String to, String subject, String bodyText) throws MessagingException {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);

        MimeMessage email = new MimeMessage(session);
        email.setFrom(new InternetAddress(senderEmailAddress));
        email.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(to));
        email.setSubject(subject);
        email.setContent(bodyText, "text/html; charset=utf-8");
        return email;
    }

    /**
     * 将MimeMessage转换为Gmail API所需的Message对象。
     *
     * @param emailContent MimeMessage对象。
     * @return Gmail API的Message对象。
     * @throws MessagingException 如果转换失败。
     * @throws IOException        如果转换失败。
     */
    private Message createMessageWithEmail(MimeMessage emailContent) throws MessagingException, IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        emailContent.writeTo(buffer);
        byte[] bytes = buffer.toByteArray();
        String encodedEmail = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Message message = new Message();
        message.setRaw(encodedEmail);
        return message;
    }

    /**
     * 发送邮件。
     *
     * @param to       收件人邮箱地址。
     * @param subject  邮件主题。
     * @param bodyText 邮件正文。
     * @return 发送结果，成功返回true，失败返回false。
     */
    @Override
    public boolean sendEmail(String to, String subject, String bodyText) {
        // 构造HTML格式的邮件正文，将subject加粗并放在第一行
        String htmlBody = "<b>" + subject + "</b><br/>" + bodyText;
        return sendHtmlEmail(to, subject, htmlBody);
    }

    /**
     * 发送HTML格式邮件
     *
     * @param to       收件人邮箱地址
     * @param subject  邮件主题
     * @param htmlBody HTML格式的邮件正文
     * @return 发送结果，成功返回true，失败返回false
     */
    @Override
    public boolean sendHtmlEmail(String to, String subject, String htmlBody) {
        if (this.service == null) {
            System.err.println("Gmail service not initialized. Cannot send email.");
            return false;
        }
        try {
            MimeMessage mimeMessage = createEmail(to, subject, htmlBody);
            Message gmailMessage = createMessageWithEmail(mimeMessage);

            // 使用Gmail服务发送消息
            // USER_ID 为 "me" 指示使用已验证用户的身份发送邮件
            Message sentMessage = service.users().messages().send(USER_ID, gmailMessage).execute();
            System.out.println("邮件已发送。 Message ID: " + sentMessage.getId());
            return true;
        } catch (MessagingException | IOException e) {
            System.err.println("发送邮件时出错: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public String getSenderType() {
        return "Gmail";
    }

    public static void main(String[] args) {
        // 这是一个简单的测试，请确保替换上面的凭据和SENDER_EMAIL_ADDRESS
        // 以及下面的收件人地址
        // 注意：作为Spring Bean后，main方法通常用于本地测试，而不是应用的主要入口点。
        // 要测试这个Bean，通常会通过Spring上下文加载它。
        // 如果要直接运行此main方法进行独立测试，你需要手动调用initService()或者模拟Spring环境，并设置相应的属性值。
        System.out.println("警告: main方法直接运行GmailSender可能无法获取通过@Value注入的配置属性。");
        System.out.println("请通过Spring Boot应用上下文运行以进行完整测试。");
        // 示例:
        // try {
        //     GmailSender sender = new GmailSender();
        //     // 手动设置属性 (仅用于脱离Spring的测试)
        //     sender.clientId = "YOUR_CLIENT_ID_FOR_TESTING";
        //     sender.clientSecret = "YOUR_CLIENT_SECRET_FOR_TESTING";
        //     sender.refreshToken = "YOUR_REFRESH_TOKEN_FOR_TESTING";
        //     sender.senderEmailAddress = "YOUR_SENDER_EMAIL_FOR_TESTING";
        //     sender.initService();
        //     sender.sendEmail("recipient@example.com", "Test Subject from main", "Test body from main");
        // } catch (GeneralSecurityException | IOException e) {
        //     System.err.println("初始化GmailSender失败 (main): " + e.getMessage());
        //     e.printStackTrace();
        // }
    }
} 