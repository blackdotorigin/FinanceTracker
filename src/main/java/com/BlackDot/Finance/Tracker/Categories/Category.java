package com.BlackDot.Finance.Tracker.Categories;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import com.BlackDot.Finance.Tracker.SubCategories.SubCategory;
import com.BlackDot.Finance.Tracker.SuperClasses.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "categories")
@Getter @Setter @NoArgsConstructor
public class Category extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(name = "system_key", nullable = false, updatable = false)
    private String systemKey;

    private String icon;
    private String color;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(nullable = false)
    private boolean active = true;

    @ManyToMany
    @JoinTable(name = "category_sub_categories",
            joinColumns = @JoinColumn(name = "category_id"),
            inverseJoinColumns = @JoinColumn(name = "sub_category_id"))
    private Set<SubCategory> subCategories = new HashSet<>();
}
