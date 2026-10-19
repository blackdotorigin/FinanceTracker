package com.BlackDot.Finance.Tracker.Dues;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import com.BlackDot.Finance.Tracker.Auth.OwnedEntity;
import com.BlackDot.Finance.Tracker.SuperClasses.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "dues")
@Getter
@Setter
public class Due extends BaseEntity implements OwnedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(length = 1000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private DueCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_category_id")
    private DueSubcategory subCategory;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private DueFrequency frequency;

    @Column(name = "frequency_interval", nullable = false)
    private int frequencyInterval;

    @Column(name = "reminder_days_before", nullable = false)
    private int[] reminderDaysBefore;

    @Column(name = "payee_name", length = 120)
    private String payeeName;

    @Column(name = "payee_upi_id", length = 100)
    private String payeeUpiId;

    @Column(length = 100)
    private String reference;

    @Column(name = "auto_pay", nullable = false)
    private boolean autoPay;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private DueStatus status;

    public Due() {
        super();
    }

    @Override
    public UUID getOwnerId() {
        return userId;
    }
}
