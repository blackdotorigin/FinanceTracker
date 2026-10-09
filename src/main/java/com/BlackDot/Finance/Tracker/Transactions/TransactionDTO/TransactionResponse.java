package com.BlackDot.Finance.Tracker.Transactions.TransactionDTO;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.BlackDot.Finance.Tracker.Categories.Category;
import com.BlackDot.Finance.Tracker.Categories.CategoryService;
import com.BlackDot.Finance.Tracker.SubCategories.SubCategory;
import com.BlackDot.Finance.Tracker.Transactions.Transaction;
import com.BlackDot.Finance.Tracker.Transactions.TransactionType;

public record TransactionResponse(
    UUID id, BigDecimal amount, String currency, TransactionType type,
    LocalDate transactionDate, String description,
    Ref category, Ref subCategory, Instant createdAt) {

    public record Ref(UUID id, String name) {}

    public static TransactionResponse from(Transaction t, CategoryService.RefData ref) {
        Category c = ref.categories().get(t.getCategoryId());
        SubCategory s = t.getSubCategoryId() == null ? null : ref.subCategories().get(t.getSubCategoryId());
        return new TransactionResponse(t.getId(), t.getAmount(), t.getCurrency(), t.getType(),
                t.getTransactionDate(), t.getDescription(),
                c == null ? null : new Ref(c.getId(), c.getName()),
                s == null ? null : new Ref(s.getId(), s.getName()),
                t.getCreatedAt());
    }
}
