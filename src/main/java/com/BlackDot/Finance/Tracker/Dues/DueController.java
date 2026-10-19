package com.BlackDot.Finance.Tracker.Dues;

import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.BlackDot.Finance.Tracker.CustomException.BadRequestException;
import com.BlackDot.Finance.Tracker.Dues.DTO.DueRequest;
import com.BlackDot.Finance.Tracker.Dues.DTO.DueResponse;
import com.BlackDot.Finance.Tracker.Dues.DTO.DueTemplateResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class DueController {

    private final DueService service;

    public DueController(DueService service) {
        this.service = service;
    }

    @GetMapping("/users/dues/template")
    public DueTemplateResponse template() {
        return service.template();
    }

    @GetMapping("/user/{userId}/dues")
    public PagedModel<DueResponse> list(
            @PathVariable UUID userId,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID subCategoryId,
            @RequestParam(required = false) DueStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 0) {
            throw new BadRequestException("page must be >= 0");
        }
        if (size < 1 || size > 100) {
            throw new BadRequestException("size must be between 1 and 100");
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by(
                Sort.Order.asc("dueDate"), Sort.Order.asc("id")));
        return new PagedModel<>(service.list(
                userId, categoryId, subCategoryId, status, from, to, pageable));
    }

    @PostMapping("/user/{userId}/dues")
    public ResponseEntity<DueResponse> create(
            @PathVariable UUID userId, @Valid @RequestBody DueRequest request) {
        DueResponse created = service.create(userId, request);
        return ResponseEntity.created(URI.create(
                "/api/v1/user/" + userId + "/dues/" + created.id())).body(created);
    }

    @PutMapping("/user/{userId}/dues/{dueId}")
    public DueResponse update(
            @PathVariable UUID userId, @PathVariable UUID dueId,
            @Valid @RequestBody DueRequest request) {
        return service.update(userId, dueId, request);
    }

    @DeleteMapping("/user/{userId}/dues/{dueId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID userId, @PathVariable UUID dueId) {
        service.delete(userId, dueId);
    }
}
