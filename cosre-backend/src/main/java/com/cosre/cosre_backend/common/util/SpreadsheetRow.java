package com.cosre.cosre_backend.common.util;

import java.util.Map;

/**
 * Đại diện cho một dòng dữ liệu đã được đọc từ file CSV/XLSX.
 * {@code values} ánh xạ tên cột (đã chuẩn hóa, chữ thường) sang giá trị ô tương ứng.
 */
public record SpreadsheetRow(int rowNumber, Map<String, String> values) {

    /** Trả về giá trị đã trim của cột {@code header}, hoặc chuỗi rỗng nếu không có. */
    public String get(String header) {
        String value = values.get(header);
        return value == null ? "" : value.trim();
    }
}
