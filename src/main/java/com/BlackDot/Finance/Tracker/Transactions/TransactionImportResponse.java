package com.BlackDot.Finance.Tracker.Transactions;

import java.util.List;

public record TransactionImportResponse(int importedCount, List<RowError> errors) {

    public record RowError(int rowNumber, List<String> messages) {}
}
