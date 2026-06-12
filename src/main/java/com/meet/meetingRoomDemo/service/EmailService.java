package com.meet.meetingRoomDemo.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String from;

    @Value("${app.server-url:http://localhost:8080}")
    private String serverUrl;

    // Set app.email.mock=false to send real emails via SMTP
    @Value("${app.email.mock:true}")
    private boolean mockEnabled;

    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter TIME_FMT     = DateTimeFormatter.ofPattern("HH:mm");

    // ─── 驗證信 ───────────────────────────────────────────────────────────────

    public void sendVerificationEmail(String to, String token) {
        String verifyLink = serverUrl + "/auth/verify-email?token=" + token;
        if (mockEnabled) {
            log.info("[MOCK EMAIL] Verification link for {} → {}", to, verifyLink);
            return;
        }
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject("請驗證您的電子郵件");
            helper.setText(buildVerificationBody(verifyLink), true);
            mailSender.send(msg);
            log.info("Verification email sent to {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send verification email to {}: {}", to, e.getMessage());
            // 不中斷流程，用戶已建立，可日後重發
        }
    }

    // ─── 預約提醒信 ───────────────────────────────────────────────────────────

    /**
     * 發送預約提醒。
     * 若 SMTP 失敗會拋出 RuntimeException，讓 NotificationScheduler 保留 is_notified=0 供下次重試。
     */
    public void sendBookingReminderEmail(String to, String userName, String roomName,
                                         String title, OffsetDateTime startedTime, OffsetDateTime endedTime) {
        String timeRange = startedTime.format(DATE_TIME_FMT) + " ~ " + endedTime.format(TIME_FMT);

        if (mockEnabled) {
            log.info("[MOCK EMAIL] Reminder to {} — [{}] {} in {} at {}",
                to, title, userName, roomName, timeRange);
            return;
        }
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject("【預約提醒】" + title);
            helper.setText(buildReminderBody(userName, roomName, title, timeRange), true);
            mailSender.send(msg);
            log.info("Reminder sent to {} — {} at {}", to, title, timeRange);
        } catch (MessagingException e) {
            log.error("Failed to send reminder to {}: {}", to, e.getMessage());
            throw new RuntimeException("Reminder email failed", e);
        }
    }

    // ─── Email 內容 ───────────────────────────────────────────────────────────

    private String buildVerificationBody(String verifyLink) {
        return "<h2>歡迎加入會議室預約系統</h2>" +
               "<p>請點擊以下連結驗證您的電子郵件：</p>" +
               "<p><a href=\"" + verifyLink + "\">驗證電子郵件</a></p>" +
               "<p>此連結將在 24 小時後失效。</p>";
    }

    private String buildReminderBody(String userName, String roomName, String title, String timeRange) {
        return "<h2>📅 預約提醒</h2>" +
               "<p>您好 <strong>" + userName + "</strong>，</p>" +
               "<p>提醒您以下預約即將開始：</p>" +
               "<table style='border-collapse:collapse;margin:12px 0'>" +
               "<tr><td style='padding:4px 12px;font-weight:bold'>主題</td><td>" + title + "</td></tr>" +
               "<tr><td style='padding:4px 12px;font-weight:bold'>會議室</td><td>" + roomName + "</td></tr>" +
               "<tr><td style='padding:4px 12px;font-weight:bold'>時間</td><td>" + timeRange + "</td></tr>" +
               "</table>" +
               "<p>請準時出席，謝謝！</p>";
    }
}
