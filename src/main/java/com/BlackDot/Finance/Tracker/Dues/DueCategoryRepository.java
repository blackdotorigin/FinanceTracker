package com.BlackDot.Finance.Tracker.Dues;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DueCategoryRepository extends JpaRepository<DueCategory, UUID> {
    List<DueCategory> findAllByActiveTrueOrderBySortOrderAscNameAsc();
    Optional<DueCategory> findByIdAndActiveTrue(UUID id);
}
