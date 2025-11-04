package com.Cricket.Match.repository;

import com.Cricket.Match.model.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {

    List<Match> findByStatus(Match.MatchStatus status);

    List<Match> findByTeam1IdOrTeam2Id(Long teamId, Long teamId2);

    List<Match> findByOrderByCreatedAtDesc();
}