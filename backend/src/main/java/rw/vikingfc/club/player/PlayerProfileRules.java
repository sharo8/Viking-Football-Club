package rw.vikingfc.club.player;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PlayerProfileRules {

    private PlayerProfileRules() {
    }

    public static List<Map<String, Object>> issues(Map<String, Object> body, boolean creating, boolean executive, boolean hasPhoto, LocalDate today) {
        List<Map<String, Object>> issues = new ArrayList<>();
        require(issues, creating, "fullName", text(body.get("fullName")), 0);
        require(issues, creating, "dateOfBirth", text(body.get("dateOfBirth")), 0);
        require(issues, creating, "position", text(body.get("position")), 0);
        require(issues, creating, "jersey", text(body.get("jersey")), 0);
        require(issues, creating, "nationality", text(body.get("nationality")), 0);
        require(issues, creating, "foot", text(body.get("foot")), 0);
        require(issues, creating, "heightCm", text(body.get("heightCm")), 0);
        require(issues, creating, "weightKg", text(body.get("weightKg")), 0);
        require(issues, creating, "status", text(body.get("status")), 0);
        if (creating && !hasPhoto) {
            issues.add(issue("photo", "REQUIRED", "A profile photo is required", 0));
        }
        LocalDate dob = date(body.get("dateOfBirth"));
        if (dob != null) {
            int years = Period.between(dob, today).getYears();
            if (dob.isAfter(today) || years < 14 || years > 50) {
                issues.add(issue("dateOfBirth", "AGE", "Age must be between 14 and 50", 0));
            }
        }
        Integer height = integer(body.get("heightCm"));
        if (height != null && (height < 140 || height > 220)) {
            issues.add(issue("heightCm", "RANGE", "Height must be between 140 and 220 cm", 0));
        }
        Integer weight = integer(body.get("weightKg"));
        if (weight != null && (weight < 40 || weight > 130)) {
            issues.add(issue("weightKg", "RANGE", "Weight must be between 40 and 130 kg", 0));
        }
        Integer jersey = integer(body.get("jersey"));
        if (jersey != null && (jersey < 1 || jersey > 99)) {
            issues.add(issue("jersey", "RANGE", "Choose a jersey from 1 to 99", 0));
        }

        require(issues, creating, "phone", text(body.get("phone")), 1);
        require(issues, creating, "email", text(body.get("email")), 1);
        require(issues, creating, "preferredLanguage", text(body.get("preferredLanguage")), 1);
        require(issues, creating, "address", text(body.get("address")), 1);
        require(issues, creating, "emergencyName", text(body.get("emergencyName")), 1);
        require(issues, creating, "emergencyRelationship", text(body.get("emergencyRelationship")), 1);
        require(issues, creating, "emergencyPhone", text(body.get("emergencyPhone")), 1);
        String email = text(body.get("email"));
        if (!email.isBlank() && !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            issues.add(issue("email", "EMAIL", "Enter a valid email", 1));
        }
        String phone = text(body.get("phone"));
        if (!phone.isBlank() && !phone.matches("^\\+?[0-9][0-9\\s]{7,16}$")) {
            issues.add(issue("phone", "PHONE", "Use a valid phone number", 1));
        }

        if (creating && executive) {
            require(issues, true, "contractStart", text(body.get("contractStart")), 2);
            require(issues, true, "contractEnd", text(body.get("contractEnd")), 2);
            require(issues, true, "seasonalPay", text(body.get("seasonalPay")), 2);
            require(issues, true, "advance", text(body.get("advance")), 2);
            require(issues, true, "releaseClause", text(body.get("releaseClause")), 2);
            require(issues, true, "marketValue", text(body.get("marketValue")), 2);
            boolean exempt = Boolean.TRUE.equals(body.get("sfExempt"));
            if (!exempt) {
                require(issues, true, "socialFund", text(body.get("socialFund")), 2);
            }
        }
        LocalDate start = date(body.get("contractStart"));
        LocalDate end = date(body.get("contractEnd"));
        if (start != null && end != null && !end.isAfter(start)) {
            issues.add(issue("contractEnd", "DATE_ORDER", "The contract must end after it starts", 2));
        }
        Integer pay = integer(body.get("seasonalPay"));
        Integer advance = integer(body.get("advance"));
        if (pay != null && advance != null && advance > pay) {
            issues.add(issue("advance", "ADVANCE", "The advance cannot be higher than the seasonal pay", 2));
        }
        return issues;
    }

    public static List<Map<String, Object>> blocking(List<Map<String, Object>> issues, boolean creating) {
        if (creating) {
            return issues;
        }
        return issues.stream().filter(item -> !"REQUIRED".equals(item.get("code"))).toList();
    }

    private static void require(List<Map<String, Object>> issues, boolean creating, String field, String value, int step) {
        if (creating && value.isBlank()) {
            issues.add(issue(field, "REQUIRED", "This field is required", step));
        }
    }

    private static Map<String, Object> issue(String field, String code, String message, int step) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("field", field);
        row.put("code", code);
        row.put("message", message);
        row.put("step", step);
        return row;
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static Integer integer(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.valueOf(String.valueOf(value).replace(" ", "").replace(",", ""));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static LocalDate date(Object value) {
        String text = text(value);
        if (text.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(text.length() >= 10 ? text.substring(0, 10) : text);
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
