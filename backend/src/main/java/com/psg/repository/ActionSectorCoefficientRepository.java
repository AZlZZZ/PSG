package com.psg.repository;

import com.psg.model.ActionSectorCoefficient;
import com.psg.model.Sector;
import com.psg.model.SecurityAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ActionSectorCoefficientRepository extends JpaRepository<ActionSectorCoefficient, Long> {
    Optional<ActionSectorCoefficient> findByActionAndSector(SecurityAction action, Sector sector);
    List<ActionSectorCoefficient> findBySector(Sector sector);
    List<ActionSectorCoefficient> findByAction(SecurityAction action);
    boolean existsByActionAndSector(SecurityAction action, Sector sector);
}
