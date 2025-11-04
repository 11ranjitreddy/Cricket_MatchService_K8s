package com.Cricket.Match.model;

public class MatchCreationEmail {
    private String team1Name;
    private String team2Name;
    private String venue;
    private int overs;
    private String tossWinner;
    private String tossDecision;
    private String creationDate;

    public MatchCreationEmail() {}

    public MatchCreationEmail(String team1Name, String team2Name, String venue, int overs,
                              String tossWinner, String tossDecision, String creationDate) {
        this.team1Name = team1Name;
        this.team2Name = team2Name;
        this.venue = venue;
        this.overs = overs;
        this.tossWinner = tossWinner;
        this.tossDecision = tossDecision;
        this.creationDate = creationDate;
    }

    // Getters and Setters
    public String getTeam1Name() { return team1Name; }
    public void setTeam1Name(String team1Name) { this.team1Name = team1Name; }

    public String getTeam2Name() { return team2Name; }
    public void setTeam2Name(String team2Name) { this.team2Name = team2Name; }

    public String getVenue() { return venue; }
    public void setVenue(String venue) { this.venue = venue; }

    public int getOvers() { return overs; }
    public void setOvers(int overs) { this.overs = overs; }

    public String getTossWinner() { return tossWinner; }
    public void setTossWinner(String tossWinner) { this.tossWinner = tossWinner; }

    public String getTossDecision() { return tossDecision; }
    public void setTossDecision(String tossDecision) { this.tossDecision = tossDecision; }

    public String getCreationDate() { return creationDate; }
    public void setCreationDate(String creationDate) { this.creationDate = creationDate; }

    @Override
    public String toString() {
        return "MatchCreationEmail{" +
                "team1Name='" + team1Name + '\'' +
                ", team2Name='" + team2Name + '\'' +
                ", venue='" + venue + '\'' +
                ", overs=" + overs +
                ", tossWinner='" + tossWinner + '\'' +
                ", tossDecision='" + tossDecision + '\'' +
                ", creationDate='" + creationDate + '\'' +
                '}';
    }
}