package com.BlackDot.Finance.Tracker.Transactions;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;

final class TransactionSpecifications {

    private TransactionSpecifications() {}

    static Specification<Transaction> search(
            UUID userId, UUID id, TransactionType type, PaymentMode paymentMode,
            LocalDate from, LocalDate to, UUID categoryId, UUID subCategoryId) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("userId"), userId));
            if (id != null) {
                predicates.add(criteriaBuilder.equal(root.get("id"), id));
            }
            if (type != null) {
                predicates.add(criteriaBuilder.equal(root.get("type"), type));
            }
            if (paymentMode != null) {
                predicates.add(criteriaBuilder.equal(root.get("paymentMode"), paymentMode));
            }
            if (from != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("transactionDate"), from));
            }
            if (to != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        root.get("transactionDate"), to));
            }
            if (categoryId != null) {
                predicates.add(criteriaBuilder.equal(root.get("categoryId"), categoryId));
            }
            if (subCategoryId != null) {
                predicates.add(criteriaBuilder.equal(root.get("subCategoryId"), subCategoryId));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
