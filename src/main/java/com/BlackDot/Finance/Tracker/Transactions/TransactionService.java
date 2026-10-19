package com.BlackDot.Finance.Tracker.Transactions;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
import com.BlackDot.Finance.Tracker.UserActivity.UserActivityService;

@Service
public class TransactionService {

    private final TransactionRepository repo;
    private final AuthorizationService auth;
    private final CategoryService categoryService;
    private final UserActivityService userActivityService;
    private static final long MAX_RANGE_DAYS = 366;
    private static final int GRAPH_LIMIT = 10;

    public TransactionService(
            TransactionRepository repo,
            AuthorizationService auth,
            CategoryService categoryService,
            UserActivityService userActivityService) {
        this.repo = repo;
        this.auth = auth;
        this.categoryService = categoryService;
        this.userActivityService = userActivityService;
    }

    @Transactional
    public TransactionResponse create(CreateTransactionRequest r) {
        categoryService.validateSelection(r.type(), r.categoryId(), r.subCategoryId());
        Transaction t = new Transaction();
        t.setUserId(CurrentUser.id());
        apply(t, r.amount(), r.currency(), r.type(), r.paymentMode(), r.transactionDate(),
            r.description(), r.categoryId(), r.subCategoryId());
        Transaction saved = repo.save(t);
        userActivityService.recordTransaction(t.getUserId(), t.getTransactionDate());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public TransactionResponse get(UUID id) {
        return toResponse(findOwned(id));
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> list(LocalDate from, LocalDate to, Pageable pageable) {
        LocalDate start = from != null ? from : YearMonth.now().atDay(1);
        LocalDate end   = to   != null ? to   : YearMonth.now().atEndOfMonth();
        validateDateRange(start, end);

        Page<Transaction> transactions =
                repo.findByUserIdAndTransactionDateBetween(CurrentUser.id(), start, end, pageable);
        return toResponses(transactions);
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> search(
            UUID id, TransactionType type, PaymentMode paymentMode,
            LocalDate from, LocalDate to, UUID categoryId, UUID subCategoryId,
            Pageable pageable) {
        validateDateRange(from, to);
        Page<Transaction> transactions = repo.findAll(
                TransactionSpecifications.search(CurrentUser.id(), id, type, paymentMode,
                        from, to, categoryId, subCategoryId),
                pageable);
        return toResponses(transactions);
    }

    private Page<TransactionResponse> toResponses(Page<Transaction> transactions) {
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

    private void validateDateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null) {
            if (from.isAfter(to))
                throw new BadRequestException("'from' must be on or before 'to'");
            if (ChronoUnit.DAYS.between(from, to) > MAX_RANGE_DAYS)
                throw new BadRequestException("Date range cannot exceed 366 days");
        }
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> graph(String period) {
        LocalDate today = LocalDate.now();
        LocalDate start;
        LocalDate end;

        switch (period == null ? "" : period.trim().toLowerCase(Locale.ROOT)) {
            case "week" -> {
                start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                end = start.plusDays(6);
            }
            case "month" -> {
                YearMonth month = YearMonth.from(today);
                start = month.atDay(1);
                end = month.atEndOfMonth();
            }
            case "year" -> {
                start = today.with(TemporalAdjusters.firstDayOfYear());
                end = today.with(TemporalAdjusters.lastDayOfYear());
            }
            default -> throw new BadRequestException("period must be week, month, or year");
        }

        Page<Transaction> transactions = repo.findByUserIdAndTransactionDateBetween(
                CurrentUser.id(), start, end,
                PageRequest.of(0, GRAPH_LIMIT, Sort.by(
                        Sort.Order.desc("transactionDate"), Sort.Order.desc("id"))));
        Set<UUID> categoryIds = new HashSet<>();
        Set<UUID> subCategoryIds = new HashSet<>();
        transactions.forEach(transaction -> {
            categoryIds.add(transaction.getCategoryId());
            if (transaction.getSubCategoryId() != null) {
                subCategoryIds.add(transaction.getSubCategoryId());
            }
        });
        CategoryService.RefData refData = categoryService.lookup(categoryIds, subCategoryIds);
        return transactions.map(transaction -> TransactionResponse.from(transaction, refData))
                .getContent();
    }

    @Transactional
    public TransactionResponse update(UUID id, UpdateTransactionRequest updateTransactionRequest) {
        Transaction transaction = findOwned(id);
        LocalDate previousDate = transaction.getTransactionDate();
        categoryService.validateSelection(
                updateTransactionRequest.type(), updateTransactionRequest.categoryId(),
                updateTransactionRequest.subCategoryId());
        apply(transaction, updateTransactionRequest.amount(), updateTransactionRequest.currency(),
                updateTransactionRequest.type(), updateTransactionRequest.paymentMode(),
                updateTransactionRequest.transactionDate(),
                updateTransactionRequest.description(), updateTransactionRequest.categoryId(),
                updateTransactionRequest.subCategoryId());
        if (!previousDate.equals(transaction.getTransactionDate())) {
            userActivityService.removeTransaction(transaction.getUserId(), previousDate);
            userActivityService.recordTransaction(
                    transaction.getUserId(), transaction.getTransactionDate());
        }
        return toResponse(transaction);
    }

    @Transactional
    public void delete(UUID id) {
        Transaction transaction = findOwned(id);
        repo.delete(transaction);
        userActivityService.removeTransaction(
                transaction.getUserId(), transaction.getTransactionDate());
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
                       TransactionType type, PaymentMode paymentMode, LocalDate date, String description,
                       UUID categoryId, UUID subCategoryId) {
        transaction.setAmount(amount);
        transaction.setCurrency(currency);
        transaction.setType(type);
        transaction.setPaymentMode(paymentMode);
        transaction.setTransactionDate(date);
        transaction.setDescription(description);
        transaction.setCategoryId(categoryId);
        transaction.setSubCategoryId(subCategoryId);
    }
}
