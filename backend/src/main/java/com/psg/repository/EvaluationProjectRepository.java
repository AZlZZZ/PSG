package com.psg.repository;

import com.psg.model.EvaluationProject;
import com.psg.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EvaluationProjectRepository extends JpaRepository<EvaluationProject, Long> {
    List<EvaluationProject> findByAssignedUser(User user);
}
