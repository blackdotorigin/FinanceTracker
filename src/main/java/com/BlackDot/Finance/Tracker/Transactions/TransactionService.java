package com.BlackDot.Finance.Tracker.Transactions;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.BlackDot.Finance.Tracker.Auth.AuthorizationService;
import com.BlackDot.Finance.Tracker.Auth.CurrentUser;
import com.BlackDot.Finance.Tracker.Categories.CategoryService;
import com.BlackDot.Finance.Tracker.CustomException.BadRequestException;
import com.BlackDot.Finance.Tracker.CustomException.ResourceNotFoundException;
import com.BlackDot.Finance.Tracker.Transactions.TransactionDTO.CreateTransactionRequest;
import com.BlackDot.Finance.Tracker.Transactions.TransactionDTO.TransactionResponse;
import com.BlackDot.Finance.Tracker.Transactions.TransactionDTO.UpdateTransactionRequest;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository repo;
    private final AuthorizationService auth;
    private final CategoryService categoryService;
    private static final long MAX_RANGE_DAYS = 366;

    @Transactional
    public TransactionResponse create(CreateTransactionRequest r) {
        categoryService.validateSelection(r.categoryId(), r.subCategoryId());
        Transaction t = new Transaction();
        t.setUserId(CurrentUser.id());
        apply(t, r.amount(), r.currency(), r.type(), r.transactionDate(),
            r.description(), r.categoryId(), r.subCategoryId());
        return toResponse(repo.save(t));
    }

    @Transactional(readOnly = true)
    public TransactionResponse get(UUID id) {
        return toResponse(findOwned(id));
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> list(LocalDate from, LocalDate to, Pageable pageable) {
        LocalDate start = from != null ? from : YearMonth.now().atDay(1);
        LocalDate end   = to   != null ? to   : YearMonth.now().atEndOfMonth();

        if (start.isAfter(end))
            throw new BadRequestException("'from' must be on or before 'to'");
        if (ChronoUnit.DAYS.between(start, end) > MAX_RANGE_DAYS)
            throw new BadRequestException("Date range cannot exceed 366 days");

        Page<Transaction> transactions =
                repo.findByUserIdAndTransactionDateBetween(CurrentUser.id(), start, end, pageable);
        Set<UUID> categoryIds = new HashSet<>();
        Set<UUID> subCategoryIds = new HashSet<>();
        transactions.forEach(transaction -> {
            categoryIds.add(transaction.getCategoryId());
            if (transaction.getSubCategoryId() != null) {
                subCategoryIds.add(transaction.getSubCategoryId());
            }
        });
        CategoryService.RefData refData = categoryService.lookup(categoryIds, subCategoryIds);
        return transactions.map(transaction -> TransactionResponse.from(transaction, refData));
    }

    @Transactional
    public TransactionResponse update(UUID id, UpdateTransactionRequest updateTransactionRequest) {
        Transaction transaction = findOwned(id);
        categoryService.validateSelection(
                updateTransactionRequest.categoryId(), updateTransactionRequest.subCategoryId());
        apply(transaction, updateTransactionRequest.amount(), updateTransactionRequest.currency(),
                updateTransactionRequest.type(), updateTransactionRequest.transactionDate(),
                updateTransactionRequest.description(), updateTransactionRequest.categoryId(),
                updateTransactionRequest.subCategoryId());
        return toResponse(transaction);
    }

    @Transactional
    public void delete(UUID id) {
        repo.delete(findOwned(id));
    }

    private Transaction findOwned(UUID id) {
        Transaction transaction = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
        auth.assertOwner(transaction);                   // 403 if it belongs to someone else
        return transaction;
    }

    private TransactionResponse toResponse(Transaction transaction) {
        Set<UUID> categoryIds = Set.of(transaction.getCategoryId());
        Set<UUID> subCategoryIds = transaction.getSubCategoryId() == null
                ? Set.of()
                : Set.of(transaction.getSubCategoryId());
        return TransactionResponse.from(
                transaction, categoryService.lookup(categoryIds, subCategoryIds));
    }

    private void apply(Transaction transaction, BigDecimal amount, String currency,
                       TransactionType type, LocalDate date, String description,
                       UUID categoryId, UUID subCategoryId) {
        transaction.setAmount(amount);
        transaction.setCurrency(currency);
        transaction.setType(type);
        transaction.setTransactionDate(date);
        transaction.setDescription(description);
        transaction.setCategoryId(categoryId);
        transaction.setSubCategoryId(subCategoryId);
    }
}
