package com.BlackDot.Finance.Tracker.Roles;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.web.PagedModel;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.BlackDot.Finance.Tracker.Categories.CategoryService;
import com.BlackDot.Finance.Tracker.Transactions.TransactionController;
import com.BlackDot.Finance.Tracker.Transactions.TransactionRepository;
import com.BlackDot.Finance.Tracker.Transactions.Transaction;
import com.BlackDot.Finance.Tracker.Transactions.TransactionDTO.TransactionResponse;
import com.BlackDot.Finance.Tracker.Users.UserService;
import com.BlackDot.Finance.Tracker.Users.DTO.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final TransactionRepository tnxrepository;
    private final CategoryService categoryService;

    @GetMapping("/users/{userId}/transactions")
    public PagedModel<TransactionResponse> userTransactions(
            @PathVariable UUID userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "DATE") String sortBy,
            @RequestParam(defaultValue = "ASC") String direction) {
        var transactions = tnxrepository.findByUserIdAndTransactionDateBetween(
                userId, from, to, TransactionController.buildPageable(page, size, sortBy, direction));
        Set<UUID> categoryIds = new HashSet<>();
        Set<UUID> subCategoryIds = new HashSet<>();
        transactions.forEach(transaction -> {
            categoryIds.add(transaction.getCategoryId());
            if (transaction.getSubCategoryId() != null) {
                subCategoryIds.add(transaction.getSubCategoryId());
            }
        });
        CategoryService.RefData refData = categoryService.lookup(categoryIds, subCategoryIds);
        return new PagedModel<>(transactions.map(transaction -> TransactionResponse.from(transaction, refData)));
    }
    @PatchMapping("/users/{userId}/role")
    public UserResponse changeRole(@PathVariable UUID userId, @Valid Roles role) {
        return userService.changeRole(userId, role);
    }
}
