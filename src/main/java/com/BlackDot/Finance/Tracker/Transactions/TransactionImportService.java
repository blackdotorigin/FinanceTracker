package com.BlackDot.Finance.Tracker.Transactions;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.BlackDot.Finance.Tracker.Auth.CurrentUser;
import com.BlackDot.Finance.Tracker.Categories.Category;
import com.BlackDot.Finance.Tracker.Categories.CategoryRepository;
import com.BlackDot.Finance.Tracker.CustomException.BadRequestException;
import com.BlackDot.Finance.Tracker.CustomException.ResourceNotFoundException;
import com.BlackDot.Finance.Tracker.SubCategories.SubCategory;
import com.BlackDot.Finance.Tracker.Transactions.TransactionDTO.CreateTransactionRequest;
import com.BlackDot.Finance.Tracker.Transactions.TransactionImportParser.ParsedRow;
import com.BlackDot.Finance.Tracker.Users.UserRepository;
import com.BlackDot.Finance.Tracker.UserActivity.UserActivityService;

@Service
public class TransactionImportService {

    private static final Pattern MONEY_PATTERN =
            Pattern.compile("[+-]?(?:\\d{1,3}(?:,\\d{3})+|\\d+)(?:\\.\\d+)?");
    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            strictDateFormat("d/M/uuuu"),
            strictDateFormat("d-M-uuuu"),
            strictDateFormat("d MMM uuuu"),
            strictDateFormat("d MMMM uuuu"));

    private final TransactionImportParser parser;
    private final TransactionRepository transactions;
    private final CategoryRepository categories;
    private final UserRepository users;
    private final UserActivityService userActivityService;
    private final Validator validator;

    public TransactionImportService(
            TransactionImportParser parser,
            TransactionRepository transactions,
            CategoryRepository categories,
            UserRepository users,
            UserActivityService userActivityService,
            Validator validator) {
        this.parser = parser;
        this.transactions = transactions;
        this.categories = categories;
        this.users = users;
        this.userActivityService = userActivityService;
        this.validator = validator;
    }

    private record AmountValue(BigDecimal amount, TransactionType direction) {}

    @Transactional
    public TransactionImportResponse importFile(MultipartFile file) {
        List<ParsedRow> rows = parser.parse(file);
        UUID userId = CurrentUser.id();
        String defaultCurrency = users.findByIdAndActiveTrue(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"))
                .getDefaultCurrency();
        List<Category> activeCategories = categories.findAllActiveWithSubCategories(null);
        List<Transaction> validTransactions = new ArrayList<>();
        List<TransactionImportResponse.RowError> rowErrors = new ArrayList<>();
        Map<LocalDate, Integer> countsByDate = new HashMap<>();

        for (ParsedRow row : rows) {
            List<String> errors = new ArrayList<>();
            Transaction transaction = mapRow(row, activeCategories, defaultCurrency, userId, errors);
            if (!errors.isEmpty()) {
                rowErrors.add(new TransactionImportResponse.RowError(row.rowNumber(), errors));
            } else {
                validTransactions.add(transaction);
                countsByDate.merge(transaction.getTransactionDate(), 1, Integer::sum);
            }
        }

        if (!rowErrors.isEmpty()) {
            return new TransactionImportResponse(0, List.copyOf(rowErrors));
        }

        transactions.saveAll(validTransactions);
        userActivityService.recordTransactions(userId, countsByDate);
        return new TransactionImportResponse(validTransactions.size(), List.of());
    }

    private Transaction mapRow(ParsedRow row, List<Category> activeCategories,
                               String defaultCurrency, UUID userId, List<String> errors) {
        Map<String, String> values = row.values();
        LocalDate date = parseDate(values.get("date"), errors);
        String description = values.get("description");
        if (description == null || description.isBlank()) {
            errors.add("description is required");
        } else if (description.length() > 255) {
            errors.add("description cannot exceed 255 characters");
        }

        Category category = resolveCategory(values.get("category"), activeCategories, errors);
        SubCategory subCategory = resolveSubCategory(
                values.get("subcategory"), category, errors);
        AmountValue parsedAmount = parseAmount(values, errors);
        BigDecimal amount = parsedAmount == null ? null : parsedAmount.amount();
        TransactionType statedType = parseType(values.get("type"), errors);
        TransactionType type = category == null ? statedType : category.getType();
        if (type != null && statedType != null && type != statedType) {
            errors.add("type does not match the selected category");
        }
        if (type != null && parsedAmount != null && parsedAmount.direction() != null
                && type != parsedAmount.direction()) {
            errors.add("amount direction does not match the selected category");
        }

        String currency = parseCurrency(values.get("currency"), defaultCurrency, errors);
        PaymentMode paymentMode = parsePaymentMode(values.get("paymentmode"), errors);
        if (date != null && amount != null && category != null && type != null
                && paymentMode != null && currency != null) {
            Set<ConstraintViolation<CreateTransactionRequest>> violations = validator.validate(
                    new CreateTransactionRequest(
                            amount.abs(), currency, type, paymentMode, date, category.getId(),
                            subCategory == null ? null : subCategory.getId(), description));
            violations.forEach(violation -> errors.add(
                    violation.getPropertyPath() + ": " + violation.getMessage()));
        }

        if (!errors.isEmpty() || date == null || amount == null || category == null || type == null) {
            return null;
        }

        Transaction transaction = new Transaction();
        transaction.setUserId(userId);
        transaction.setAmount(amount.abs());
        transaction.setCurrency(currency);
        transaction.setType(type);
        transaction.setPaymentMode(paymentMode);
        transaction.setCategoryId(category.getId());
        transaction.setSubCategoryId(subCategory == null ? null : subCategory.getId());
        transaction.setTransactionDate(date);
        transaction.setDescription(description);
        return transaction;
    }

    private LocalDate parseDate(String value, List<String> errors) {
        if (value == null || value.isBlank()) {
            errors.add("date is required");
            return null;
        }
        for (DateTimeFormatter formatter : DATE_FORMATS) {
            try {
                return LocalDate.parse(value.trim(), formatter);
            } catch (DateTimeParseException ignored) {
                // Try the next documented date format.
            }
        }
        errors.add("date must be ISO (yyyy-MM-dd) or day-first (d/M/yyyy)");
        return null;
    }

    private Category resolveCategory(String value, List<Category> activeCategories,
                                     List<String> errors) {
        if (value == null || value.isBlank()) {
            errors.add("category is required");
            return null;
        }
        String candidate = value.trim();
        List<Category> matches;
        try {
            UUID id = UUID.fromString(candidate);
            matches = activeCategories.stream().filter(category -> category.getId().equals(id))
                    .toList();
        } catch (IllegalArgumentException ignored) {
            String normalized = normalizeName(candidate);
            matches = activeCategories.stream()
                    .filter(category -> normalizeName(category.getName()).equals(normalized))
                    .toList();
        }
        if (matches.isEmpty()) {
            errors.add("category does not match an active category name or ID");
            return null;
        }
        if (matches.size() > 1) {
            errors.add("category name is ambiguous; use its category ID");
            return null;
        }
        return matches.getFirst();
    }

    private SubCategory resolveSubCategory(String value, Category category, List<String> errors) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if (category == null) {
            errors.add("subcategory cannot be checked until category is valid");
            return null;
        }
        String candidate = value.trim();
        List<SubCategory> matches;
        try {
            UUID id = UUID.fromString(candidate);
            matches = category.getSubCategories().stream()
                    .filter(SubCategory::isActive)
                    .filter(subCategory -> subCategory.getId().equals(id))
                    .toList();
        } catch (IllegalArgumentException ignored) {
            String normalized = normalizeName(candidate);
            matches = category.getSubCategories().stream()
                    .filter(SubCategory::isActive)
                    .filter(subCategory -> normalizeName(subCategory.getName()).equals(normalized))
                    .toList();
        }
        if (matches.isEmpty()) {
            errors.add("subcategory is not active under the selected category");
            return null;
        }
        if (matches.size() > 1) {
            errors.add("subcategory name is ambiguous; use its subcategory ID");
            return null;
        }
        return matches.getFirst();
    }

    private AmountValue parseAmount(Map<String, String> values, List<String> errors) {
        String amountValue = values.get("amount");
        if (amountValue != null && !amountValue.isBlank()) {
            if (hasDebitCredit(values)
                    && (!blank(values.get("debit")) || !blank(values.get("credit")))) {
                errors.add("use either amount or debit/credit values, not both");
                return null;
            }
            BigDecimal amount = parseOptionalAmount(amountValue, "amount", errors);
            if (amount == null) {
                return null;
            }
            if (amount.signum() == 0) {
                errors.add("amount must be greater than zero");
                return null;
            }
            return new AmountValue(amount,
                    amount.signum() < 0 ? TransactionType.EXPENSE : null);
        }
        if (!hasDebitCredit(values)) {
            errors.add("amount is required");
            return null;
        }
        BigDecimal debit = parseOptionalAmount(values.get("debit"), "debit", errors);
        BigDecimal credit = parseOptionalAmount(values.get("credit"), "credit", errors);
        if (debit != null && debit.signum() < 0) {
            errors.add("debit cannot be negative");
        }
        if (credit != null && credit.signum() < 0) {
            errors.add("credit cannot be negative");
        }
        if (debit == null || credit == null) {
            return null;
        }
        boolean hasDebit = debit != null && debit.signum() > 0;
        boolean hasCredit = credit != null && credit.signum() > 0;
        if (hasDebit == hasCredit) {
            errors.add("exactly one of debit or credit must be greater than zero");
            return null;
        }
        return new AmountValue(hasDebit ? debit : credit,
                hasDebit ? TransactionType.EXPENSE : TransactionType.INCOME);
    }

    private BigDecimal parseOptionalAmount(String value, String field, List<String> errors) {
        if (blank(value)) {
            return BigDecimal.ZERO;
        }
        String normalized = value.trim().replaceAll("[₹$€£\\s]", "");
        boolean parenthesized = normalized.startsWith("(") && normalized.endsWith(")");
        if (parenthesized) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        if (!MONEY_PATTERN.matcher(normalized).matches()) {
            errors.add(field + " must be a valid decimal amount");
            return null;
        }
        try {
            BigDecimal amount = new BigDecimal(normalized.replace(",", ""));
            if (parenthesized) {
                amount = amount.negate();
            }
            BigDecimal scaled = amount.abs().setScale(4, RoundingMode.UNNECESSARY);
            if (scaled.precision() > 19 || scaled.precision() - scaled.scale() > 15) {
                errors.add(field + " exceeds the supported precision");
                return null;
            }
            return amount;
        } catch (ArithmeticException exception) {
            errors.add(field + " supports at most four decimal places");
            return null;
        }
    }

    private TransactionType parseType(String value, List<String> errors) {
        if (blank(value)) {
            return null;
        }
        return switch (normalizeName(value)) {
            case "income", "credit", "deposit", "inflow" -> TransactionType.INCOME;
            case "expense", "debit", "withdrawal", "outflow" -> TransactionType.EXPENSE;
            default -> {
                errors.add("type must be INCOME or EXPENSE");
                yield null;
            }
        };
    }

    private String parseCurrency(String value, String defaultCurrency, List<String> errors) {
        String currency = blank(value) ? defaultCurrency : value.trim().toUpperCase(Locale.ROOT);
        if (!currency.matches("[A-Z]{3}")) {
            errors.add("currency must be a three-letter ISO code");
            return null;
        }
        return currency;
    }

    private PaymentMode parsePaymentMode(String value, List<String> errors) {
        if (blank(value)) {
            return PaymentMode.BANK_ACCOUNT;
        }
        return switch (normalizeName(value)) {
            case "cash" -> PaymentMode.CASH;
            case "credit", "creditcard" -> PaymentMode.CREDIT;
            case "bankaccount", "bank", "banktransfer", "transfer", "upi", "online",
                    "debitcard" -> PaymentMode.BANK_ACCOUNT;
            default -> {
                errors.add("paymentMode must be CASH, CREDIT, or BANK_ACCOUNT");
                yield null;
            }
        };
    }

    private boolean hasDebitCredit(Map<String, String> values) {
        return values.containsKey("debit") && values.containsKey("credit");
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private String normalizeName(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}", "")
                .trim()
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
    }

    private static DateTimeFormatter strictDateFormat(String pattern) {
        return new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendPattern(pattern)
                .toFormatter(Locale.ENGLISH)
                .withResolverStyle(ResolverStyle.STRICT);
    }
}
