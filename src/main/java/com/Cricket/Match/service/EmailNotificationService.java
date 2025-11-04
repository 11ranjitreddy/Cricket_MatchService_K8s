package com.Cricket.Match.service;

import com.Cricket.Match.model.MatchCreationEmail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(EmailNotificationService.class);

    private final JavaMailSender mailSender;

    @Value("${team.notification.email:ranjithredd8479@gmail.com}")
    private String fixedEmailAddress;

    public EmailNotificationService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @RabbitListener(queues = "match_creation_queue")
    public void sendMatchCreationEmail(MatchCreationEmail emailRequest) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(fixedEmailAddress);
            message.setSubject("Match Scheduled: " + emailRequest.getTeam1Name() + " vs " + emailRequest.getTeam2Name());
            message.setText(createMatchEmailContent(emailRequest));

            mailSender.send(message);
            logger.info("Match creation email sent successfully to: {}", fixedEmailAddress);
        } catch (Exception e) {
            logger.error("Failed to send match creation email to: {}", fixedEmailAddress, e);
        }
    }

    private String createMatchEmailContent(MatchCreationEmail emailRequest) {
        return String.format(
                "NEW MATCH SCHEDULED NOTIFICATION\n\n" +
                        "A new cricket match has been scheduled:\n\n" +
                        "Match Details:\n" +
                        "Teams: %s vs %s\n" +
                        "Venue: %s\n" +
                        "Overs: %d\n" +
                        "Toss Winner: %s\n" +
                        "Toss Decision: %s\n" +
                        "Scheduled On: %s\n\n" +
                        "This is an automated notification from Cricket Team Management System",
                emailRequest.getTeam1Name() != null ? emailRequest.getTeam1Name() : "N/A",
                emailRequest.getTeam2Name() != null ? emailRequest.getTeam2Name() : "N/A",
                emailRequest.getVenue() != null ? emailRequest.getVenue() : "N/A",
                emailRequest.getOvers(),
                emailRequest.getTossWinner() != null ? emailRequest.getTossWinner() : "N/A",
                emailRequest.getTossDecision() != null ? emailRequest.getTossDecision() : "N/A",
                emailRequest.getCreationDate() != null ? emailRequest.getCreationDate() : "N/A"
        );
    }
}