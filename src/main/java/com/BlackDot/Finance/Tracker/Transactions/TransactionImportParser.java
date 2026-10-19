package com.BlackDot.Finance.Tracker.Transactions;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.NotOfficeXmlFileException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.BlackDot.Finance.Tracker.CustomException.BadRequestException;

@Component
public class TransactionImportParser {

    private static final long MAX_FILE_BYTES = 5 * 1024 * 1024;
    private static final int MAX_ROWS = 1_000;
    private static final Map<String, List<String>> HEADER_ALIASES = Map.of(
            "date", List.of("date", "transactiondate", "valuedate", "postingdate"),
            "description", List.of("description", "narration", "particulars", "details",
                    "transactiondetails", "merchant"),
            "amount", List.of("amount", "transactionamount"),
            "debit", List.of("debit", "withdrawal", "moneyout"),
            "credit", List.of("credit", "deposit", "moneyin"),
            "currency", List.of("currency", "currencycode"),
            "type", List.of("type", "transactiontype"),
            "paymentmode", List.of("paymentmode", "paymentmethod", "mode"),
            "category", List.of("category", "categoryname", "categoryid"),
            "subcategory", List.of("subcategory", "subcategoryname", "subcategoryid"));

    public record ParsedRow(int rowNumber, Map<String, String> values) {}

    public List<ParsedRow> parse(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Upload a non-empty .csv or .xlsx file");
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new BadRequestException("Import file cannot exceed 5 MB");
        }
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new BadRequestException("The uploaded file must have a .csv or .xlsx filename");
        }
        String extension = filename.substring(filename.lastIndexOf('.') + 1)
                .toLowerCase(Locale.ROOT);
        try (InputStream input = new BufferedInputStream(file.getInputStream())) {
            return switch (extension) {
                case "csv" -> parseCsv(input);
                case "xlsx" -> parseXlsx(input);
                default -> throw new BadRequestException("Only .csv and .xlsx files are supported");
            };
        } catch (BadRequestException exception) {
            throw exception;
        } catch (IOException | EncryptedDocumentException | NotOfficeXmlFileException exception) {
            throw new BadRequestException(
                    "Could not read the uploaded transaction file; check that it is a valid .csv or .xlsx file");
        }
    }

    private List<ParsedRow> parseCsv(InputStream input) throws IOException {
        try (CSVParser parser = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .build()
                .parse(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            List<String> headers = parser.getHeaderNames();
            Map<String, Integer> columns = findColumns(headers);
            List<ParsedRow> rows = new ArrayList<>();
            parser.forEach(record -> {
                Map<String, String> values = new LinkedHashMap<>();
                columns.forEach((name, index) -> values.put(name,
                        index < record.size() ? record.get(index).trim() : ""));
                if (values.values().stream().anyMatch(value -> !value.isEmpty())) {
                    if (rows.size() >= MAX_ROWS) {
                        throw new BadRequestException("Import cannot contain more than 1,000 rows");
                    }
                    rows.add(new ParsedRow(Math.toIntExact(record.getRecordNumber() + 1), values));
                }
            });
            return requireRows(rows);
        }
    }

    private List<ParsedRow> parseXlsx(InputStream input) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(input)) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new BadRequestException("The workbook does not contain a worksheet");
            }
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) {
                throw new BadRequestException("The first worksheet is empty");
            }
            DataFormatter formatter = new DataFormatter(Locale.ROOT);
            var evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            List<String> headers = new ArrayList<>();
            for (int index = 0; index < headerRow.getLastCellNum(); index++) {
                headers.add(cellText(headerRow.getCell(index), formatter, evaluator));
            }
            Map<String, Integer> columns = findColumns(headers);
            List<ParsedRow> rows = new ArrayList<>();
            for (int index = headerRow.getRowNum() + 1; index <= sheet.getLastRowNum(); index++) {
                Row row = sheet.getRow(index);
                if (row == null) {
                    continue;
                }
                Map<String, String> values = new LinkedHashMap<>();
                columns.forEach((name, column) -> values.put(name,
                        cellText(row.getCell(column), formatter, evaluator).trim()));
                if (values.values().stream().anyMatch(value -> !value.isEmpty())) {
                    if (rows.size() >= MAX_ROWS) {
                        throw new BadRequestException("Import cannot contain more than 1,000 rows");
                    }
                    rows.add(new ParsedRow(index + 1, values));
                }
            }
            return requireRows(rows);
        }
    }

    private String cellText(Cell cell, DataFormatter formatter,
                            org.apache.poi.ss.usermodel.FormulaEvaluator evaluator) {
        return cell == null ? "" : formatter.formatCellValue(cell, evaluator);
    }

    private Map<String, Integer> findColumns(List<String> headers) {
        Map<String, Integer> columns = new LinkedHashMap<>();
        for (int index = 0; index < headers.size(); index++) {
            String normalized = normalizeHeader(headers.get(index));
            for (Map.Entry<String, List<String>> entry : HEADER_ALIASES.entrySet()) {
                if (entry.getValue().contains(normalized)) {
                    if (columns.putIfAbsent(entry.getKey(), index) != null) {
                        throw new BadRequestException(
                                "The file has more than one column for '" + entry.getKey() + "'");
                    }
                    break;
                }
            }
        }
        if (!columns.containsKey("date") || !columns.containsKey("description")
                || !columns.containsKey("category")) {
            throw new BadRequestException(
                    "File must include date, description, and category columns");
        }
        if (!columns.containsKey("amount")
                && !(columns.containsKey("debit") && columns.containsKey("credit"))) {
            throw new BadRequestException(
                    "File must include an amount column or both debit and credit columns");
        }
        return columns;
    }

    private String normalizeHeader(String header) {
        String noBom = header.replace("\uFEFF", "");
        return Normalizer.normalize(noBom, Normalizer.Form.NFKD)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");
    }

    private List<ParsedRow> requireRows(List<ParsedRow> rows) {
        if (rows.isEmpty()) {
            throw new BadRequestException("The file contains no transaction rows");
        }
        return List.copyOf(rows);
    }
}
