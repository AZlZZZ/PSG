package com.psg.repository;

import com.psg.model.Evaluation;
import com.psg.model.EvaluationStatus;
import com.psg.model.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {
    List<Evaluation> findByOrganization(Organization organization);
    List<Evaluation> findByStatus(EvaluationStatus status);
}
