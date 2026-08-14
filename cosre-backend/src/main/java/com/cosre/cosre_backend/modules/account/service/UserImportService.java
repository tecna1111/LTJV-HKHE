package com.cosre.cosre_backend.modules.account.service;

import com.cosre.cosre_backend.common.constants.RoleEnum;
import com.cosre.cosre_backend.modules.account.dto.ImportUserError;
import com.cosre.cosre_backend.modules.account.dto.ImportUsersResult;
import com.cosre.cosre_backend.modules.account.entity.User;
import com.cosre.cosre_backend.modules.account.repository.UserRepository;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class UserImportService {

    private static final List<String> REQUIRED_HEADERS =
            List.of("username", "email", "fullname", "password", "role");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserImportService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public ImportUsersResult importUsers(MultipartFile file) {
        validateFile(file);
        List<ImportRow> rows = readRows(file);
        List<ImportUserError> errors = new ArrayList<>();
        Set<String> usernames = new HashSet<>();
        Set<String> emails = new HashSet<>();
        int imported = 0;

        for (ImportRow row : rows) {
            String error = validateRow(row, usernames, emails);
            if (error != null) {
                errors.add(new ImportUserError(row.number(), row.username(), error));
                continue;
            }

            User user = new User();
            user.setUsername(row.username());
            user.setEmail(row.email());
            user.setFullName(row.fullName());
            user.setPassword(passwordEncoder.encode(row.password()));
            user.setRole(RoleEnum.valueOf(row.role().toUpperCase(Locale.ROOT)));
            user.setActive(true);
            userRepository.save(user);
            usernames.add(row.username().toLowerCase(Locale.ROOT));
            emails.add(row.email().toLowerCase(Locale.ROOT));
            imported++;
        }

        return new ImportUsersResult(rows.size(), imported, errors.size(), List.copyOf(errors));
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn tệp CSV hoặc XLSX");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Kích thước tệp không được vượt quá 5 MB");
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (!name.endsWith(".csv") && !name.endsWith(".xlsx")) {
            throw new IllegalArgumentException("Chỉ hỗ trợ tệp có định dạng .csv hoặc .xlsx");
        }
    }

    private List<ImportRow> readRows(MultipartFile file) {
        String name = file.getOriginalFilename().toLowerCase(Locale.ROOT);
        try {
            return name.endsWith(".csv") ? readCsv(file) : readXlsx(file);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Không thể đọc tệp import");
        }
    }

    private List<ImportRow> readCsv(MultipartFile file) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) throw new IllegalArgumentException("Tệp import không có dữ liệu");
            int headerRowNumber = 1;
            if (stripBom(headerLine).trim().equalsIgnoreCase("sep=,")) {
                headerLine = reader.readLine();
                headerRowNumber = 2;
                if (headerLine == null) throw new IllegalArgumentException("Tệp import không có dòng tiêu đề");
            }
            Map<String, Integer> headers = mapHeaders(parseCsvLine(stripBom(headerLine)));
            List<ImportRow> rows = new ArrayList<>();
            String line;
            int rowNumber = headerRowNumber;
            while ((line = reader.readLine()) != null) {
                rowNumber++;
                List<String> values = parseCsvLine(line);
                if (!isBlankRow(values)) rows.add(toRow(rowNumber, values, headers));
            }
            return rows;
        }
    }

    private List<ImportRow> readXlsx(MultipartFile file) throws IOException {
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
            Map<String, Integer> headers = mapHeaders(headerValues);
            List<ImportRow> rows = new ArrayList<>();
            for (int index = headerRow.getRowNum() + 1; index <= sheet.getLastRowNum(); index++) {
                Row excelRow = sheet.getRow(index);
                if (excelRow == null) continue;
                List<String> values = new ArrayList<>();
                for (int column = 0; column < headerValues.size(); column++) {
                    values.add(formatter.formatCellValue(excelRow.getCell(column)));
                }
                if (!isBlankRow(values)) rows.add(toRow(index + 1, values, headers));
            }
            return rows;
        }
    }

    private Map<String, Integer> mapHeaders(List<String> values) {
        Map<String, Integer> headers = new HashMap<>();
        for (int index = 0; index < values.size(); index++) {
            headers.put(values.get(index).trim().toLowerCase(Locale.ROOT), index);
        }
        List<String> missing = REQUIRED_HEADERS.stream().filter(header -> !headers.containsKey(header)).toList();
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("Thiếu cột bắt buộc: " + String.join(", ", missing));
        }
        return headers;
    }

    private ImportRow toRow(int number, List<String> values, Map<String, Integer> headers) {
        return new ImportRow(number, value(values, headers.get("username")), value(values, headers.get("email")),
                value(values, headers.get("fullname")), value(values, headers.get("password")),
                value(values, headers.get("role")));
    }

    private String validateRow(ImportRow row, Set<String> usernames, Set<String> emails) {
        if (row.username().length() < 3 || row.username().length() > 50) return "Username phải có từ 3 đến 50 ký tự";
        if (!EMAIL_PATTERN.matcher(row.email()).matches() || row.email().length() > 100) return "Email không hợp lệ";
        if (row.fullName().isBlank() || row.fullName().length() > 100) return "Họ tên không hợp lệ";
        if (row.password().length() < 8 || row.password().length() > 100) return "Mật khẩu phải có từ 8 đến 100 ký tự";
        String role = row.role().toUpperCase(Locale.ROOT);
        if (!role.equals(RoleEnum.STUDENT.name()) && !role.equals(RoleEnum.LECTURER.name())) {
            return "Vai trò chỉ được là STUDENT hoặc LECTURER";
        }
        String usernameKey = row.username().toLowerCase(Locale.ROOT);
        String emailKey = row.email().toLowerCase(Locale.ROOT);
        if (usernames.contains(usernameKey)) return "Username bị trùng trong tệp";
        if (emails.contains(emailKey)) return "Email bị trùng trong tệp";
        if (userRepository.findByUsername(row.username()).isPresent()) return "Username đã tồn tại";
        if (userRepository.findByEmail(row.email()).isPresent()) return "Email đã tồn tại";
        return null;
    }

    private List<String> parseCsvLine(String line) {
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

    private String value(List<String> values, int index) {
        return index < values.size() ? values.get(index).trim() : "";
    }

    private boolean isBlankRow(List<String> values) {
        return values.stream().allMatch(String::isBlank);
    }

    private String stripBom(String value) {
        return value.startsWith("\uFEFF") ? value.substring(1) : value;
    }

    private record ImportRow(int number, String username, String email, String fullName, String password, String role) {
    }
}
