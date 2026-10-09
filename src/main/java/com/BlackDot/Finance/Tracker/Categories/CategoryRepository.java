package com.BlackDot.Finance.Tracker.Categories;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    // one query for the whole tree (Hibernate 6 de-duplicates the root entities itself)
    @Query("select c from Category c left join fetch c.subCategories where c.active = true")
    List<Category> findAllActiveWithSubCategories();

    boolean existsByIdAndActiveTrue(UUID id);

    @Query("""
           select count(c) > 0 from Category c join c.subCategories s
           where c.id = :categoryId and s.id = :subCategoryId
             and c.active = true and s.active = true
           """)
    boolean existsActivePair(UUID categoryId, UUID subCategoryId);
}
