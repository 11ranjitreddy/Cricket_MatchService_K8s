package com.Cricket.Match.service;

import com.Cricket.Match.model.Team;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;

@Service
public class TeamServiceClient {

    @Value("${team.service.url}")
    private String teamServiceUrl;

    private final RestTemplate restTemplate;

    public TeamServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Team getTeamById(Long teamId) {
        try {
            String url = teamServiceUrl + "/teams/" + teamId;
            ResponseEntity<Team> response = restTemplate.getForEntity(url, Team.class);
            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch team with id: " + teamId + ". Error: " + e.getMessage());
        }
    }

    public Team[] getAllTeams() {
        try {
            String url = teamServiceUrl + "/teams";
            ResponseEntity<Team[]> response = restTemplate.getForEntity(url, Team[].class);
            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch teams. Error: " + e.getMessage());
        }
    }

    public boolean validateTeamExists(Long teamId) {
        try {
            getTeamById(teamId);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}