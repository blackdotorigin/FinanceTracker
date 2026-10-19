package com.BlackDot.Finance.Tracker.Transactions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import com.BlackDot.Finance.Tracker.CustomException.BadRequestException;

class TransactionImportParserTest {

    private final TransactionImportParser parser = new TransactionImportParser();

    @Test
    void parsesCsvHeadersAndQuotedDescription() {
        String csv = "\uFEFFTransaction Date,Description,Amount,Category,Sub Category\n"
                + "2026-10-16,\"Coffee, bakery\",-125.50,Food,Dining\n";
        MockMultipartFile file = new MockMultipartFile(
                "file", "bank.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        var rows = parser.parse(file);

        assertEquals(1, rows.size());
        assertEquals(2, rows.getFirst().rowNumber());
        assertEquals("Coffee, bakery", rows.getFirst().values().get("description"));
        assertEquals("-125.50", rows.getFirst().values().get("amount"));
        assertEquals("Food", rows.getFirst().values().get("category"));
    }

    @Test
    void parsesXlsxCellsAndDayFirstBankHeaders() throws Exception {
        byte[] bytes;
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet();
            var header = sheet.createRow(0);
            header.createCell(0).setCellValue("Value Date");
            header.createCell(1).setCellValue("Particulars");
            header.createCell(2).setCellValue("Debit");
            header.createCell(3).setCellValue("Credit");
            header.createCell(4).setCellValue("Category");
            var data = sheet.createRow(1);
            data.createCell(0).setCellValue("17/10/2026");
            data.createCell(1).setCellValue("Monthly rent");
            data.createCell(2).setCellValue(18000);
            data.createCell(4).setCellValue("Housing");
            workbook.write(output);
            bytes = output.toByteArray();
        }
        MockMultipartFile file = new MockMultipartFile(
                "file", "statement.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);

        var rows = parser.parse(file);

        assertEquals(1, rows.size());
        assertEquals("18000", rows.getFirst().values().get("debit"));
        assertEquals("", rows.getFirst().values().get("credit"));
        assertEquals(2, rows.getFirst().rowNumber());
    }

    @Test
    void rejectsFilesWithoutCategoryColumn() {
        String csv = "date,description,amount\n2026-10-16,Groceries,24.00\n";
        MockMultipartFile file = new MockMultipartFile(
                "file", "bank.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        assertThrows(BadRequestException.class, () -> parser.parse(file));
    }

    @Test
    void rejectsMoreThanOneThousandTransactions() {
        StringBuilder csv = new StringBuilder("date,description,amount,category\n");
        for (int row = 0; row < 1_001; row++) {
            csv.append("2026-10-16,Coffee,2.50,Food\n");
        }
        MockMultipartFile file = new MockMultipartFile(
                "file", "bank.csv", "text/csv", csv.toString().getBytes(StandardCharsets.UTF_8));

        assertThrows(BadRequestException.class, () -> parser.parse(file));
    }
}
