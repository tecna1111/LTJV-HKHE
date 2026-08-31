package com.cosre.cosre_backend.common.util;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Utility dùng chung để đọc dữ liệu dạng bảng từ file CSV hoặc Excel (.xlsx) được người dùng tải lên.
 * Hỗ trợ các chức năng import (tài khoản, lớp học, danh sách thành viên lớp, ...) bằng Apache POI.
 */
public final class SpreadsheetReader {

    private static final long DEFAULT_MAX_FILE_SIZE = 5L * 1024 * 1024;

    private SpreadsheetReader() {
    }

    /** Kiểm tra file hợp lệ (không rỗng, đúng định dạng, không vượt quá kích thước cho phép). */
    public static void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn tệp CSV hoặc XLSX");
        }
        if (file.getSize() > DEFAULT_MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Kích thước tệp không được vượt quá 5 MB");
        }
        String name = safeName(file);
        if (!name.endsWith(".csv") && !name.endsWith(".xlsx")) {
            throw new IllegalArgumentException("Chỉ hỗ trợ tệp có định dạng .csv hoặc .xlsx");
        }
    }

    /**
     * Đọc toàn bộ các dòng dữ liệu (bỏ qua dòng tiêu đề và các dòng trống) từ file CSV/XLSX.
     * Ném {@link IllegalArgumentException} nếu thiếu cột bắt buộc hoặc không đọc được file.
     */
    public static List<SpreadsheetRow> readRows(MultipartFile file, List<String> requiredHeaders) {
        validate(file);
        try {
            return safeName(file).endsWith(".csv") ? readCsv(file, requiredHeaders) : readXlsx(file, requiredHeaders);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Không thể đọc tệp import");
        }
    }

    private static List<SpreadsheetRow> readCsv(MultipartFile file, List<String> requiredHeaders) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) throw new IllegalArgumentException("Tệp import không có dữ liệu");
            int headerRowNumber = 1;
            if (stripBom(headerLine).trim().equalsIgnoreCase("sep=,")) {
                headerLine = reader.readLine();
                headerRowNumber = 2;
                if (headerLine == null) throw new IllegalArgumentException("Tệp import không có dòng tiêu đề");
            }
            List<String> headerValues = parseCsvLine(stripBom(headerLine));
            Map<String, Integer> headers = mapHeaders(headerValues, requiredHeaders);
            List<SpreadsheetRow> rows = new ArrayList<>();
            String line;
            int rowNumber = headerRowNumber;
            while ((line = reader.readLine()) != null) {
                rowNumber++;
                List<String> cellValues = parseCsvLine(line);
                if (!isBlankRow(cellValues)) rows.add(toRow(rowNumber, cellValues, headers));
            }
            return rows;
        }
    }

    private static List<SpreadsheetRow> readXlsx(MultipartFile file, List<String> requiredHeaders) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            if (workbook.getNumberOfSheets() == 0) throw new IllegalArgumentException("Tệp Excel không có sheet dữ liệu");
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) throw new IllegalArgumentException("Tệp import không có dòng tiêu đề");
            DataFormatter formatter = new DataFormatter();
            List<String> headerValues = new ArrayList<>();
            for (int index = 0; index < headerRow.getLastCellNum(); index++) {
                headerValues.add(formatter.formatCellValue(headerRow.getCell(index)));
            }
            Map<String, Integer> headers = mapHeaders(headerValues, requiredHeaders);
            List<SpreadsheetRow> rows = new ArrayList<>();
            for (int index = headerRow.getRowNum() + 1; index <= sheet.getLastRowNum(); index++) {
                Row excelRow = sheet.getRow(index);
                if (excelRow == null) continue;
                List<String> cellValues = new ArrayList<>();
                for (int column = 0; column < headerValues.size(); column++) {
                    cellValues.add(formatter.formatCellValue(excelRow.getCell(column)));
                }
                if (!isBlankRow(cellValues)) rows.add(toRow(index + 1, cellValues, headers));
            }
            return rows;
        }
    }

    private static Map<String, Integer> mapHeaders(List<String> values, List<String> requiredHeaders) {
        Map<String, Integer> headers = new LinkedHashMap<>();
        for (int index = 0; index < values.size(); index++) {
            headers.put(values.get(index).trim().toLowerCase(Locale.ROOT), index);
        }
        List<String> missing = requiredHeaders.stream().filter(header -> !headers.containsKey(header)).toList();
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("Thiếu cột bắt buộc: " + String.join(", ", missing));
        }
        return headers;
    }

    private static SpreadsheetRow toRow(int rowNumber, List<String> cellValues, Map<String, Integer> headers) {
        Map<String, String> values = new LinkedHashMap<>();
        headers.forEach((header, index) -> values.put(header, index < cellValues.size() ? cellValues.get(index).trim() : ""));
        return new SpreadsheetRow(rowNumber, values);
    }

    private static List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder value = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < line.length(); index++) {
            char current = line.charAt(index);
            if (current == '"') {
                if (quoted && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    value.append('"');
                    index++;
                } else quoted = !quoted;
            } else if (current == ',' && !quoted) {
                values.add(value.toString().trim());
                value.setLength(0);
            } else value.append(current);
        }
        if (quoted) throw new IllegalArgumentException("Tệp CSV có dấu ngoặc kép không hợp lệ");
        values.add(value.toString().trim());
        return values;
    }

    private static boolean isBlankRow(List<String> values) {
        return values.stream().allMatch(String::isBlank);
    }

    private static String stripBom(String value) {
        return value.startsWith("\uFEFF") ? value.substring(1) : value;
    }

    private static String safeName(MultipartFile file) {
        return file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
    }
}
