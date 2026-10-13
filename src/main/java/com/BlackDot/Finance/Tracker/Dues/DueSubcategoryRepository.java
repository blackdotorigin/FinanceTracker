package com.BlackDot.Finance.Tracker.Dues;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DueSubcategoryRepository extends JpaRepository<DueSubcategory, UUID> {
    @Query("""
            select subcategory from DueSubcategory subcategory
            where subcategory.category.id = :categoryId and subcategory.active = true
            order by subcategory.sortOrder, subcategory.name
            """)
    List<DueSubcategory> findActiveByCategoryId(@Param("categoryId") UUID categoryId);

    List<DueSubcategory> findAllByActiveTrueOrderBySortOrderAscNameAsc();

    Optional<DueSubcategory> findByIdAndCategory_IdAndActiveTrue(UUID id, UUID categoryId);
}
