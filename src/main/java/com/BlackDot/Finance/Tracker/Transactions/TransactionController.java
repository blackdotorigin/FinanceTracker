package com.BlackDot.Finance.Tracker.Transactions;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
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
import com.BlackDot.Finance.Tracker.Transactions.TransactionDTO.CreateTransactionRequest;
import com.BlackDot.Finance.Tracker.Transactions.TransactionDTO.TransactionResponse;
import com.BlackDot.Finance.Tracker.Transactions.TransactionDTO.UpdateTransactionRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService service;

    @PostMapping
    public ResponseEntity<TransactionResponse> create(@Valid @RequestBody CreateTransactionRequest r) {
        TransactionResponse created = service.create(r);
        return ResponseEntity.created(URI.create("/api/v1/transactions/" + created.id())).body(created);
    }

    @GetMapping("/{id}")
    public TransactionResponse get(@PathVariable UUID id) { return service.get(id); }

    @GetMapping
    public PagedModel<TransactionResponse> list(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "DATE") String sortBy,
        @RequestParam(defaultValue = "ASC") String direction) {

        return new PagedModel<>(service.list(from, to, buildPageable(page, size, sortBy, direction)));
    }

    @GetMapping("/graph")
    public List<TransactionResponse> graph(@RequestParam String period) {
        return service.graph(period);
    }

    
    @PutMapping("/{id}")
    public TransactionResponse update(@PathVariable UUID id,
        @Valid @RequestBody UpdateTransactionRequest r) {
            return service.update(id, r);
        }
        
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) { 
        service.delete(id); 
    }

    
    public static Pageable buildPageable(int page, int size, String sortBy, String direction) {
        if (page < 0)                 throw new BadRequestException("page must be >= 0");
        if (size < 1 || size > 100)   throw new BadRequestException("size must be between 1 and 100");
        
        Sort.Direction dir = Sort.Direction.fromOptionalString(direction)
        .orElseThrow(() -> new BadRequestException("direction must be ASC or DESC"));
        
        String property = TransactionSortField.fromParam(sortBy).property();
        
        // "id" tie-breaker keeps pagination stable when many rows share the same date
        return PageRequest.of(page, size, Sort.by(new Sort.Order(dir, property), Sort.Order.asc("id")));
    }
}
    