package com.beam;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class KoreanPhoneNumberValidator {

    private static final Pattern KOREAN_MOBILE_PATTERN = Pattern.compile("^01[0-9]-?[0-9]{3,4}-?[0-9]{4}$");
    private static final Pattern KOREAN_LANDLINE_PATTERN = Pattern.compile("^0[2-6][0-9]-?[0-9]{3,4}-?[0-9]{4}$");

    public boolean isValidKoreanPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }

        String normalized = phoneNumber.replaceAll("[\\s-]", "");

        return KOREAN_MOBILE_PATTERN.matcher(normalized).matches() ||
               KOREAN_LANDLINE_PATTERN.matcher(normalized).matches();
    }

    public String normalizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null) {
            return null;
        }

        String normalized = phoneNumber.replaceAll("[\\s-]", "");

        if (normalized.length() == 11 && normalized.startsWith("010")) {
            return normalized.substring(0, 3) + "-" + normalized.substring(3, 7) + "-" + normalized.substring(7);
        } else if (normalized.length() == 10 && normalized.startsWith("02")) {
            return normalized.substring(0, 2) + "-" + normalized.substring(2, 6) + "-" + normalized.substring(6);
        } else if (normalized.length() == 10) {
            return normalized.substring(0, 3) + "-" + normalized.substring(3, 6) + "-" + normalized.substring(6);
        }

        return normalized;
    }

    public String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 8) {
            return phoneNumber;
        }

        String normalized = phoneNumber.replaceAll("[\\s-]", "");

        if (normalized.length() == 11) {
            return normalized.substring(0, 3) + "-****-" + normalized.substring(7);
        } else if (normalized.length() == 10) {
            return normalized.substring(0, 3) + "***-" + normalized.substring(6);
        }

        return phoneNumber;
    }

    public String getCarrierFromPhoneNumber(String phoneNumber) {
        if (phoneNumber == null) {
            return "UNKNOWN";
        }

        String normalized = phoneNumber.replaceAll("[\\s-]", "");

        if (normalized.startsWith("010")) {
            return "MOBILE";
        } else if (normalized.startsWith("02")) {
            return "SEOUL";
        } else if (normalized.startsWith("031")) {
            return "GYEONGGI";
        } else if (normalized.startsWith("032")) {
            return "INCHEON";
        } else if (normalized.startsWith("051")) {
            return "BUSAN";
        } else if (normalized.startsWith("053")) {
            return "DAEGU";
        } else if (normalized.startsWith("062")) {
            return "GWANGJU";
        } else if (normalized.startsWith("042")) {
            return "DAEJEON";
        } else if (normalized.startsWith("044")) {
            return "SEJONG";
        } else if (normalized.startsWith("052")) {
            return "ULSAN";
        }

        return "OTHER";
    }
}