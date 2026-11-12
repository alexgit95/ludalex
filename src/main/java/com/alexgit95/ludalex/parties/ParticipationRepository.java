package com.alexgit95.ludalex.parties;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ParticipationRepository extends JpaRepository<Participation, Long> {

    boolean existsByJoueurId(Long joueurId);

    List<Participation> findByPartieId(Long partieId);
}
