package com.Cricket.Match.service;

import com.Cricket.Match.model.Match;
import com.Cricket.Match.model.Team;
import com.Cricket.Match.model.MatchCreationEmail;
import com.Cricket.Match.repository.MatchRepository;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MatchServiceImpl implements MatchService {

    private static final Logger logger = LoggerFactory.getLogger(MatchServiceImpl.class);

    private final MatchRepository matchRepository;
    private final TeamServiceClient teamServiceClient;
    private final RabbitTemplate rabbitTemplate;
    private final JavaMailSender mailSender;

    @Value("${team.notification.email:admin@cricketmanager.com}")
    private String fixedEmailAddress;

    public MatchServiceImpl(MatchRepository matchRepository, TeamServiceClient teamServiceClient,
                            RabbitTemplate rabbitTemplate, JavaMailSender mailSender) {
        this.matchRepository = matchRepository;
        this.teamServiceClient = teamServiceClient;
        this.rabbitTemplate = rabbitTemplate;
        this.mailSender = mailSender;
    }

    @Override
    public Match createMatch(Match match) {
        // Validate teams exist in Team Service
        if (!teamServiceClient.validateTeamExists(match.getTeam1Id())) {
            throw new RuntimeException("Team 1 not found in team service");
        }
        if (!teamServiceClient.validateTeamExists(match.getTeam2Id())) {
            throw new RuntimeException("Team 2 not found in team service");
        }

        // Validate teams are different
        if (match.getTeam1Id().equals(match.getTeam2Id())) {
            throw new RuntimeException("Cannot create match with the same team");
        }

        Match savedMatch = matchRepository.save(match);

        // Enrich with team data for response
        savedMatch.setTeam1(teamServiceClient.getTeamById(savedMatch.getTeam1Id()));
        savedMatch.setTeam2(teamServiceClient.getTeamById(savedMatch.getTeam2Id()));

        // Send email notification via RabbitMQ
        sendMatchCreationNotification(savedMatch);

        return savedMatch;
    }

    @Override
    public Match getMatchById(Long id) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Match not found with id: " + id));

        // Enrich with team data
        match.setTeam1(teamServiceClient.getTeamById(match.getTeam1Id()));
        match.setTeam2(teamServiceClient.getTeamById(match.getTeam2Id()));

        return match;
    }

    @Override
    public List<Match> getAllMatches() {
        List<Match> matches = matchRepository.findByOrderByCreatedAtDesc();

        // Enrich all matches with team data
        return matches.stream().map(match -> {
            match.setTeam1(teamServiceClient.getTeamById(match.getTeam1Id()));
            match.setTeam2(teamServiceClient.getTeamById(match.getTeam2Id()));
            return match;
        }).collect(Collectors.toList());
    }

    @Override
    public List<Match> getMatchesByStatus(Match.MatchStatus status) {
        List<Match> matches = matchRepository.findByStatus(status);

        // Enrich matches with team data
        return matches.stream().map(match -> {
            match.setTeam1(teamServiceClient.getTeamById(match.getTeam1Id()));
            match.setTeam2(teamServiceClient.getTeamById(match.getTeam2Id()));
            return match;
        }).collect(Collectors.toList());
    }

    @Override
    public List<Match> getMatchesByTeamId(Long teamId) {
        List<Match> matches = matchRepository.findByTeam1IdOrTeam2Id(teamId, teamId);

        // Enrich matches with team data
        return matches.stream().map(match -> {
            match.setTeam1(teamServiceClient.getTeamById(match.getTeam1Id()));
            match.setTeam2(teamServiceClient.getTeamById(match.getTeam2Id()));
            return match;
        }).collect(Collectors.toList());
    }

    @Override
    public Match updateMatchStatus(Long matchId, Match.MatchStatus status) {
        Match match = getMatchById(matchId);
        match.setStatus(status);
        Match updatedMatch = matchRepository.save(match);

        // Enrich with team data for response
        updatedMatch.setTeam1(teamServiceClient.getTeamById(updatedMatch.getTeam1Id()));
        updatedMatch.setTeam2(teamServiceClient.getTeamById(updatedMatch.getTeam2Id()));

        return updatedMatch;
    }

    @Override
    public Match updateMatch(Long id, Match updatedMatch) {
        Match existingMatch = getMatchById(id);

        // Update basic fields
        existingMatch.setOvers(updatedMatch.getOvers());
        existingMatch.setTossWinner(updatedMatch.getTossWinner());
        existingMatch.setTossDecision(updatedMatch.getTossDecision());
        existingMatch.setStatus(updatedMatch.getStatus());
        existingMatch.setVenue(updatedMatch.getVenue());

        // Update teams if provided
        if (updatedMatch.getTeam1Id() != null) {
            if (!teamServiceClient.validateTeamExists(updatedMatch.getTeam1Id())) {
                throw new RuntimeException("Team 1 not found in team service");
            }
            existingMatch.setTeam1Id(updatedMatch.getTeam1Id());
        }

        if (updatedMatch.getTeam2Id() != null) {
            if (!teamServiceClient.validateTeamExists(updatedMatch.getTeam2Id())) {
                throw new RuntimeException("Team 2 not found in team service");
            }
            existingMatch.setTeam2Id(updatedMatch.getTeam2Id());
        }

        Match savedMatch = matchRepository.save(existingMatch);

        // Enrich with team data for response
        savedMatch.setTeam1(teamServiceClient.getTeamById(savedMatch.getTeam1Id()));
        savedMatch.setTeam2(teamServiceClient.getTeamById(savedMatch.getTeam2Id()));

        return savedMatch;
    }

    @Override
    public void deleteMatch(Long id) {
        Match match = getMatchById(id);
        matchRepository.delete(match);
    }

    private void sendMatchCreationNotification(Match match) {
        try {
            String creationDate = match.getCreatedAt() != null
                    ? match.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    : java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            MatchCreationEmail emailRequest = new MatchCreationEmail(
                    match.getTeam1().getName(),
                    match.getTeam2().getName(),
                    match.getVenue(),
                    match.getOvers(),
                    match.getTossWinner(),
                    match.getTossDecision().toString(),
                    creationDate
            );

            rabbitTemplate.convertAndSend("match_creation_queue", emailRequest);
            logger.info("Match creation notification sent via RabbitMQ");

        } catch (AmqpException e) {
            logger.warn("RabbitMQ not available, sending email directly: {}", e.getMessage());
            sendMatchEmailDirectly(match);
        } catch (Exception e) {
            logger.error("Error in match creation notification: {}", e.getMessage());
        }
    }

    private void sendMatchEmailDirectly(Match match) {
        try {
            String creationDate = match.getCreatedAt() != null
                    ? match.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    : java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(fixedEmailAddress);
            message.setSubject("Match Scheduled: " + match.getTeam1().getName() + " vs " + match.getTeam2().getName());
            message.setText(createMatchEmailContent(match, creationDate));

            mailSender.send(message);
            logger.info("Match creation email sent directly to: {}", fixedEmailAddress);
        } catch (Exception e) {
            logger.error("Failed to send match creation email: {}", e.getMessage());
        }
    }

    private String createMatchEmailContent(Match match, String creationDate) {
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
                match.getTeam1().getName(),
                match.getTeam2().getName(),
                match.getVenue(),
                match.getOvers(),
                match.getTossWinner(),
                match.getTossDecision().toString(),
                creationDate
        );
    }
}