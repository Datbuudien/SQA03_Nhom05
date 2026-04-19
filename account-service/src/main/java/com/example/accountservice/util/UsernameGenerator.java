package com.example.accountservice.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.accountservice.repository.AccountRepository;
import com.example.accountservice.service.AccountService;

import java.text.Normalizer;
import java.util.regex.Pattern;

@Component
public class UsernameGenerator {

    @Autowired
    private AccountRepository accountRepository;

    private static final Pattern DIACRITICS_PATTERN = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    /**
     * Tạo username từ họ tên
     * Ví dụ: "Nguyễn Hoàng Hiệp" -> "hiepnh"
     */
    public String generateUsername(String firstName, String lastName) {
        if (firstName == null || lastName == null || firstName.trim().isEmpty() || lastName.trim().isEmpty()) {
            throw new IllegalArgumentException("First name and last name are required");
        }

        // Loại bỏ dấu và chuyển thành chữ thường
        String cleanFirstName = removeAccents(firstName.trim().toLowerCase());
        String cleanLastName = removeAccents(lastName.trim().toLowerCase());

        // Tách các từ trong họ và đệm
        String[] lastNameParts = cleanLastName.split("\\s+");

        StringBuilder usernameBuilder = new StringBuilder();

        // Thêm tên (từ cuối cùng)
        usernameBuilder.append(cleanFirstName.replaceAll("\\s+", ""));

        // Thêm chữ cái đầu của các từ trong họ và đệm
        for (String part : lastNameParts) {
            if (!part.isEmpty()) {
                usernameBuilder.append(part.charAt(0));
            }
        }

        String baseUsername = usernameBuilder.toString();

        // Đảm bảo username là duy nhất
        return ensureUniqueUsername(baseUsername);
    }

    /**
     * Loại bỏ dấu tiếng Việt
     */
    private String removeAccents(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        String withoutDiacritics = DIACRITICS_PATTERN.matcher(normalized).replaceAll("");

        // Xử lý các ký tự đặc biệt của tiếng Việt
        return withoutDiacritics
                .replace("đ", "d")
                .replace("Đ", "D")
                .replaceAll("[^a-zA-Z0-9\\s]", "");
    }

    /**
     * Đảm bảo username là duy nhất bằng cách thêm số nếu cần
     */
    private String ensureUniqueUsername(String baseUsername) {
        String username = baseUsername;
        int counter = 1;

        while (accountRepository.existsByUsernameAndVisible(username, 1)) {
            username = baseUsername + counter;
            counter++;
        }

        return username;
    }

    /**
     * Tạo password mặc định
     */
    public String getDefaultPassword() {
        return "123456Aa@";
    }
}