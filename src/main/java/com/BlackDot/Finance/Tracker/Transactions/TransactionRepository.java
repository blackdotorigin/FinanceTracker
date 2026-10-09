package com.BlackDot.Finance.Tracker.Transactions;

import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Page<Transaction> findByUserIdAndTransactionDateBetween(
            UUID userId, LocalDate from, LocalDate to, Pageable pageable);
}