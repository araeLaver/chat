package com.beam.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 * 클라이언트 IP 주소 추출 유틸리티
 * 프록시, 로드밸런서 등을 고려하여 실제 클라이언트 IP를 추출합니다.
 */
@Component
public class IpAddressExtractor {

    private static final String[] IP_HEADERS = {
        "X-Forwarded-For",
        "Proxy-Client-IP",
        "WL-Proxy-Client-IP",
        "HTTP_X_FORWARDED_FOR",
        "HTTP_X_FORWARDED",
        "HTTP_X_CLUSTER_CLIENT_IP",
        "HTTP_CLIENT_IP",
        "HTTP_FORWARDED_FOR",
        "HTTP_FORWARDED",
        "X-Real-IP"
    };

    private static final String UNKNOWN = "unknown";

    /**
     * HTTP 요청에서 클라이언트 IP 주소를 추출합니다.
     * 프록시 헤더를 순차적으로 확인하여 실제 클라이언트 IP를 반환합니다.
     *
     * @param request HTTP 요청
     * @return 클라이언트 IP 주소
     */
    public String extractClientIp(HttpServletRequest request) {
        if (request == null) {
            return UNKNOWN;
        }

        String ip = null;

        // 프록시 헤더 순차 확인
        for (String header : IP_HEADERS) {
            ip = request.getHeader(header);
            if (isValidIp(ip)) {
                break;
            }
        }

        // 헤더에서 찾지 못한 경우 직접 연결 IP 사용
        if (!isValidIp(ip)) {
            ip = request.getRemoteAddr();
        }

        // 여러 IP가 있는 경우 (X-Forwarded-For) 첫 번째 IP 사용
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip != null ? ip : UNKNOWN;
    }

    /**
     * IP 주소가 유효한지 확인합니다.
     *
     * @param ip IP 주소 문자열
     * @return 유효하면 true
     */
    private boolean isValidIp(String ip) {
        return ip != null && !ip.isEmpty() && !UNKNOWN.equalsIgnoreCase(ip);
    }

    /**
     * IP 주소가 로컬호스트인지 확인합니다.
     *
     * @param ip IP 주소
     * @return 로컬호스트이면 true
     */
    public boolean isLocalhost(String ip) {
        return "127.0.0.1".equals(ip) ||
               "0:0:0:0:0:0:0:1".equals(ip) ||
               "::1".equals(ip) ||
               "localhost".equalsIgnoreCase(ip);
    }

    /**
     * IP 주소가 내부 네트워크(사설 IP)인지 확인합니다.
     *
     * @param ip IP 주소
     * @return 내부 네트워크이면 true
     */
    public boolean isInternalNetwork(String ip) {
        if (ip == null) {
            return false;
        }

        // 로컬호스트
        if (isLocalhost(ip)) {
            return true;
        }

        // 사설 IP 대역
        return ip.startsWith("10.") ||
               ip.startsWith("172.16.") || ip.startsWith("172.17.") ||
               ip.startsWith("172.18.") || ip.startsWith("172.19.") ||
               ip.startsWith("172.20.") || ip.startsWith("172.21.") ||
               ip.startsWith("172.22.") || ip.startsWith("172.23.") ||
               ip.startsWith("172.24.") || ip.startsWith("172.25.") ||
               ip.startsWith("172.26.") || ip.startsWith("172.27.") ||
               ip.startsWith("172.28.") || ip.startsWith("172.29.") ||
               ip.startsWith("172.30.") || ip.startsWith("172.31.") ||
               ip.startsWith("192.168.");
    }

    /**
     * IPv4 주소 형식인지 확인합니다.
     *
     * @param ip IP 주소
     * @return IPv4 형식이면 true
     */
    public boolean isIPv4(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        String[] parts = ip.split("\\.");
        if (parts.length != 4) {
            return false;
        }
        for (String part : parts) {
            try {
                int num = Integer.parseInt(part);
                if (num < 0 || num > 255) {
                    return false;
                }
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return true;
    }

    /**
     * IP 주소를 마스킹합니다 (개인정보 보호용).
     * 예: 192.168.1.100 -> 192.168.1.***
     *
     * @param ip IP 주소
     * @return 마스킹된 IP 주소
     */
    public String maskIp(String ip) {
        if (ip == null || ip.isEmpty()) {
            return ip;
        }

        if (isIPv4(ip)) {
            int lastDot = ip.lastIndexOf('.');
            if (lastDot > 0) {
                return ip.substring(0, lastDot) + ".***";
            }
        }

        // IPv6 또는 다른 형식의 경우 뒷부분 마스킹
        if (ip.length() > 8) {
            return ip.substring(0, ip.length() - 4) + "****";
        }

        return ip;
    }
}
