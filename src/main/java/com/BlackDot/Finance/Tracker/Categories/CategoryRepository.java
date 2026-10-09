package com.BlackDot.Finance.Tracker.Categories;

import java.util.List;
import java.util.UUID;
import com.BlackDot.Finance.Tracker.Transactions.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    @Query("""
           select c from Category c left join fetch c.subCategories
           where c.active = true and (:type is null or c.type = :type)
           """)
    List<Category> findAllActiveWithSubCategories(@Param("type") TransactionType type);

    boolean existsByIdAndActiveTrueAndType(UUID id, TransactionType type);

    @Query("""
           select count(c) > 0 from Category c join c.subCategories s
           where c.id = :categoryId and s.id = :subCategoryId
             and c.active = true and s.active = true and c.type = :type
           """)
    boolean existsActivePair(UUID categoryId, UUID subCategoryId, TransactionType type);
}
