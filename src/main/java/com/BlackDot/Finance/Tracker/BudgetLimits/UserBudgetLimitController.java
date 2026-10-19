package com.BlackDot.Finance.Tracker.BudgetLimits;

import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.BlackDot.Finance.Tracker.BudgetLimits.DTO.BudgetLimitResponse;
import com.BlackDot.Finance.Tracker.BudgetLimits.DTO.SetBudgetLimitRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/budget-limits")
public class UserBudgetLimitController {

    private final UserBudgetLimitService service;

    public UserBudgetLimitController(UserBudgetLimitService service) {
        this.service = service;
    }

    @GetMapping
    public List<BudgetLimitResponse> list() {
        return service.list();
    }

    @PostMapping
    public BudgetLimitResponse createOrSet(@Valid @RequestBody SetBudgetLimitRequest request) {
        return service.set(request);
    }

    @PutMapping
    public BudgetLimitResponse set(@Valid @RequestBody SetBudgetLimitRequest request) {
        return service.set(request);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @RequestParam UUID categoryId,
            @RequestParam(required = false) UUID subCategoryId) {
        service.delete(categoryId, subCategoryId);
    }
}
