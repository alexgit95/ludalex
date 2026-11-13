package com.alexgit95.ludalex.reglages;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReglageRepository extends JpaRepository<Reglage, Long> {

    Optional<Reglage> findByCle(String cle);

    List<Reglage> findAllByOrderByIdAsc();
}
