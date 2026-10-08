package com.psg.repository;

import com.psg.model.Organization;
import com.psg.model.Sector;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    Optional<Organization> findByName(String name);
    List<Organization> findBySector(Sector sector);
    boolean existsByName(String name);
}
