package com.psg.repository;

import com.psg.model.Evaluation;
import com.psg.model.EvaluationItem;
import com.psg.model.ImplementationStatus;
import com.psg.model.SecurityAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EvaluationItemRepository extends JpaRepository<EvaluationItem, Long> {
    List<EvaluationItem> findByEvaluation(Evaluation evaluation);
    Optional<EvaluationItem> findByEvaluationAndSecurityAction(Evaluation evaluation, SecurityAction securityAction);
    List<EvaluationItem> findByEvaluationAndImplementationStatus(Evaluation evaluation, ImplementationStatus implementationStatus);
    boolean existsByEvaluationAndSecurityAction(Evaluation evaluation, SecurityAction securityAction);
}
