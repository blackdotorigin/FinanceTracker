package com.BlackDot.Finance.Tracker.Dues;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.EntityGraph;

public interface DueRepository extends JpaRepository<Due, UUID>, JpaSpecificationExecutor<Due> {
    @Override
    @EntityGraph(attributePaths = {"category", "subCategory"})
    Page<Due> findAll(Specification<Due> specification, Pageable pageable);

    Optional<Due> findByIdAndUserId(UUID id, UUID userId);
}
