package com.psg.repository;

import com.psg.model.SecurityAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SecurityActionRepository extends JpaRepository<SecurityAction, Long> {
    Optional<SecurityAction> findByCode(String code);
    List<SecurityAction> findByTheme(String theme);
    List<SecurityAction> findByProjectSubdomain(String projectSubdomain);
    boolean existsByCode(String code);
}
