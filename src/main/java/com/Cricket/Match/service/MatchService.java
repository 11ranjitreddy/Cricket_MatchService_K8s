package com.Cricket.Match.service;

import com.Cricket.Match.model.Match;
import java.util.List;

public interface MatchService {

    Match createMatch(Match match);

    Match getMatchById(Long id);

    List<Match> getAllMatches();

    List<Match> getMatchesByStatus(Match.MatchStatus status);

    List<Match> getMatchesByTeamId(Long teamId);

    Match updateMatchStatus(Long matchId, Match.MatchStatus status);

    Match updateMatch(Long id, Match match);

    void deleteMatch(Long id);
}