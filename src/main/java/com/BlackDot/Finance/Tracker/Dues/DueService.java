package com.BlackDot.Finance.Tracker.Dues;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.BlackDot.Finance.Tracker.CustomException.BadRequestException;
import com.BlackDot.Finance.Tracker.CustomException.ResourceNotFoundException;
import com.BlackDot.Finance.Tracker.Dues.DTO.DueRequest;
import com.BlackDot.Finance.Tracker.Dues.DTO.DueResponse;
import com.BlackDot.Finance.Tracker.Dues.DTO.DueTemplateResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DueService {

    private final DueRepository repository;
    private final DueCategoryRepository categoryRepository;
    private final DueSubcategoryRepository subcategoryRepository;

    @Transactional(readOnly = true)
    @PreAuthorize("@auth.canAccess(#id)")
    public Page<DueResponse> list(
            UUID id, UUID categoryId, UUID subCategoryId, DueStatus status,
            LocalDate from, LocalDate to,
            Pageable pageable) {
        validateDateRange(from, to);
        Specification<Due> specification = (root, query, builder) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("userId"), id));
            if (categoryId != null) {
                predicates.add(builder.equal(root.get("category").get("id"), categoryId));
            }
            if (subCategoryId != null) {
                predicates.add(builder.equal(root.get("subCategory").get("id"), subCategoryId));
            }
            if (status != null) {
                predicates.add(builder.equal(root.get("status"), status));
            }
            if (from != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("dueDate"), from));
            }
            if (to != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("dueDate"), to));
            }
            return builder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        return repository.findAll(specification, pageable).map(DueResponse::from);
    }

    @Transactional(readOnly = true)
    public DueTemplateResponse template() {
        List<DueTemplateResponse.CategoryOption> categories = categoryRepository
                .findAllByActiveTrueOrderBySortOrderAscNameAsc()
                .stream()
                .map(category -> new DueTemplateResponse.CategoryOption(
                        category.getId(),
                        category.getCode(),
                        category.getName(),
                        subcategoryRepository.findActiveByCategoryId(category.getId())
                                .stream()
                                .map(subcategory -> new DueTemplateResponse.SubcategoryOption(
                                        subcategory.getId(), subcategory.getCode(),
                                        subcategory.getName()))
                                .toList()))
                .toList();
        return DueTemplateResponse.of(categories);
    }

    @Transactional
    @PreAuthorize("@auth.canAccess(#id)")
    public DueResponse create(UUID id, DueRequest request) {
        validateFrequency(request.frequency(), request.frequencyInterval());
        validateReminders(request.reminderDaysBefore());
        Due due = new Due();
        due.setUserId(id);
        apply(due, request);
        return DueResponse.from(repository.save(due));
    }

    @Transactional
    @PreAuthorize("@auth.canAccess(#id)")
    public DueResponse update(UUID id, UUID dueId, DueRequest request) {
        validateFrequency(request.frequency(), request.frequencyInterval());
        validateReminders(request.reminderDaysBefore());
        Due due = repository.findByIdAndUserId(dueId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Due not found"));
        apply(due, request);
        return DueResponse.from(repository.save(due));
    }

    @Transactional
    @PreAuthorize("@auth.canAccess(#id)")
    public void delete(UUID id, UUID dueId) {
        Due due = repository.findByIdAndUserId(dueId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Due not found"));
        repository.delete(due);
    }

    private void apply(Due due, DueRequest request) {
        DueCategory category = categoryRepository.findByIdAndActiveTrue(request.categoryId())
                .orElseThrow(() -> new BadRequestException("Invalid due category"));
        DueSubcategory subcategory = request.subCategoryId() == null ? null
                : subcategoryRepository.findByIdAndCategory_IdAndActiveTrue(
                        request.subCategoryId(), request.categoryId())
                        .orElseThrow(() ->
                                new BadRequestException("Subcategory does not belong to category"));
        due.setTitle(request.title().trim());
        due.setDescription(trimToNull(request.description()));
        due.setCategory(category);
        due.setSubCategory(subcategory);
        due.setAmount(request.amount());
        due.setCurrency(request.currency());
        due.setDueDate(request.dueDate());
        due.setFrequency(request.frequency());
        due.setFrequencyInterval(request.frequencyInterval());
        due.setReminderDaysBefore(request.reminderDaysBefore().stream().distinct().sorted()
                .mapToInt(Integer::intValue).toArray());
        due.setPayeeName(trimToNull(request.payeeName()));
        due.setReference(trimToNull(request.reference()));
        due.setAutoPay(request.autoPay());
        due.setStatus(request.status());
    }

    private void validateFrequency(DueFrequency frequency, int interval) {
        if (frequency == DueFrequency.ONCE && interval != 1) {
            throw new BadRequestException("frequencyInterval must be 1 when frequency is ONCE");
        }
    }

    private void validateReminders(List<Integer> reminderDaysBefore) {
        if (reminderDaysBefore.stream().distinct().count() != reminderDaysBefore.size()) {
            throw new BadRequestException("reminderDaysBefore values must be unique");
        }
    }

    private void validateDateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException("'from' must be on or before 'to'");
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
