package com.cosre.cosre_backend.modules.classroom.service;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Tạo các tệp Excel (.xlsx) mẫu bằng Apache POI để Staff tải về, điền dữ liệu đúng cột
 * rồi import ngược lại qua {@link ClassroomImportService}.
 */
@Service
public class ClassroomTemplateService {

    private static final String[] CLASSROOM_HEADERS = {"code", "name", "subjectCode", "semester", "academicYear"};
    private static final String[][] CLASSROOM_SAMPLES = {
            {"SE1801", "Software Engineering 01", "SE101", "Fall", "2025-2026"},
            {"SE1802", "Software Engineering 02", "SE101", "Fall", "2025-2026"},
    };

    private static final String[] MEMBER_HEADERS = {"username"};
    private static final String[][] MEMBER_SAMPLES = {{"sv001"}, {"sv002"}};

    /** Sinh file mẫu để import danh sách lớp học ("Lớp"). */
    public byte[] buildClassroomTemplate() {
        return buildWorkbook("Danh sách lớp", CLASSROOM_HEADERS, CLASSROOM_SAMPLES);
    }

    /** Sinh file mẫu để import danh sách thành viên (SV/GV) vào một lớp học. */
    public byte[] buildMemberTemplate() {
        return buildWorkbook("Danh sách thành viên", MEMBER_HEADERS, MEMBER_SAMPLES);
    }

    private byte[] buildWorkbook(String sheetName, String[] headers, String[][] samples) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(sheetName);
            CellStyle headerStyle = headerStyle(workbook);

            Row headerRow = sheet.createRow(0);
            for (int column = 0; column < headers.length; column++) {
                Cell cell = headerRow.createCell(column);
                cell.setCellValue(headers[column]);
                cell.setCellStyle(headerStyle);
            }

            for (int rowIndex = 0; rowIndex < samples.length; rowIndex++) {
                Row row = sheet.createRow(rowIndex + 1);
                String[] sample = samples[rowIndex];
                for (int column = 0; column < sample.length; column++) {
                    row.createCell(column).setCellValue(sample[column]);
                }
            }

            for (int column = 0; column < headers.length; column++) {
                sheet.autoSizeColumn(column);
            }

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể tạo tệp Excel mẫu", exception);
        }
    }

    private CellStyle headerStyle(XSSFWorkbook workbook) {
        Font boldFont = workbook.createFont();
        boldFont.setBold(true);
        CellStyle style = workbook.createCellStyle();
        style.setFont(boldFont);
        return style;
    }
}
