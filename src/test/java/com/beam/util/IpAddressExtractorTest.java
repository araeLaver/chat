package com.beam.util;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("IpAddressExtractor Tests")
class IpAddressExtractorTest {

    private IpAddressExtractor ipAddressExtractor;

    @Mock
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        ipAddressExtractor = new IpAddressExtractor();
    }

    @Nested
    @DisplayName("extractClientIp Tests")
    class ExtractClientIpTests {

        @Test
        @DisplayName("Should return unknown for null request")
        void shouldReturnUnknownForNullRequest() {
            String ip = ipAddressExtractor.extractClientIp(null);
            assertThat(ip).isEqualTo("unknown");
        }

        @Test
        @DisplayName("Should extract IP from X-Forwarded-For header")
        void shouldExtractIpFromXForwardedForHeader() {
            when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.100");

            String ip = ipAddressExtractor.extractClientIp(request);
            assertThat(ip).isEqualTo("192.168.1.100");
        }

        @Test
        @DisplayName("Should extract first IP from X-Forwarded-For with multiple IPs")
        void shouldExtractFirstIpFromXForwardedForWithMultipleIps() {
            when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.100, 10.0.0.1, 172.16.0.1");

            String ip = ipAddressExtractor.extractClientIp(request);
            assertThat(ip).isEqualTo("192.168.1.100");
        }

        @Test
        @DisplayName("Should extract IP from X-Real-IP header")
        void shouldExtractIpFromXRealIpHeader() {
            when(request.getHeader("X-Real-IP")).thenReturn("192.168.1.200");

            String ip = ipAddressExtractor.extractClientIp(request);
            assertThat(ip).isEqualTo("192.168.1.200");
        }

        @Test
        @DisplayName("Should use remote addr when no headers present")
        void shouldUseRemoteAddrWhenNoHeadersPresent() {
            when(request.getRemoteAddr()).thenReturn("127.0.0.1");

            String ip = ipAddressExtractor.extractClientIp(request);
            assertThat(ip).isEqualTo("127.0.0.1");
        }

        @Test
        @DisplayName("Should skip unknown header value")
        void shouldSkipUnknownHeaderValue() {
            when(request.getHeader("X-Forwarded-For")).thenReturn("unknown");
            when(request.getRemoteAddr()).thenReturn("192.168.1.1");

            String ip = ipAddressExtractor.extractClientIp(request);
            assertThat(ip).isEqualTo("192.168.1.1");
        }

        @Test
        @DisplayName("Should skip empty header value")
        void shouldSkipEmptyHeaderValue() {
            when(request.getHeader("X-Forwarded-For")).thenReturn("");
            when(request.getRemoteAddr()).thenReturn("192.168.1.2");

            String ip = ipAddressExtractor.extractClientIp(request);
            assertThat(ip).isEqualTo("192.168.1.2");
        }

        @Test
        @DisplayName("Should return unknown when remote addr is null")
        void shouldReturnUnknownWhenRemoteAddrIsNull() {
            String ip = ipAddressExtractor.extractClientIp(request);
            assertThat(ip).isEqualTo("unknown");
        }
    }

    @Nested
    @DisplayName("isLocalhost Tests")
    class IsLocalhostTests {

        @Test
        @DisplayName("Should return true for 127.0.0.1")
        void shouldReturnTrueFor127001() {
            assertThat(ipAddressExtractor.isLocalhost("127.0.0.1")).isTrue();
        }

        @Test
        @DisplayName("Should return true for IPv6 localhost ::1")
        void shouldReturnTrueForIpv6Localhost() {
            assertThat(ipAddressExtractor.isLocalhost("::1")).isTrue();
        }

        @Test
        @DisplayName("Should return true for full IPv6 localhost")
        void shouldReturnTrueForFullIpv6Localhost() {
            assertThat(ipAddressExtractor.isLocalhost("0:0:0:0:0:0:0:1")).isTrue();
        }

        @Test
        @DisplayName("Should return true for localhost string")
        void shouldReturnTrueForLocalhostString() {
            assertThat(ipAddressExtractor.isLocalhost("localhost")).isTrue();
        }

        @Test
        @DisplayName("Should return true for LOCALHOST uppercase")
        void shouldReturnTrueForLocalhostUppercase() {
            assertThat(ipAddressExtractor.isLocalhost("LOCALHOST")).isTrue();
        }

        @Test
        @DisplayName("Should return false for external IP")
        void shouldReturnFalseForExternalIp() {
            assertThat(ipAddressExtractor.isLocalhost("192.168.1.1")).isFalse();
        }
    }

    @Nested
    @DisplayName("isInternalNetwork Tests")
    class IsInternalNetworkTests {

        @Test
        @DisplayName("Should return false for null IP")
        void shouldReturnFalseForNullIp() {
            assertThat(ipAddressExtractor.isInternalNetwork(null)).isFalse();
        }

        @Test
        @DisplayName("Should return true for localhost")
        void shouldReturnTrueForLocalhost() {
            assertThat(ipAddressExtractor.isInternalNetwork("127.0.0.1")).isTrue();
        }

        @Test
        @DisplayName("Should return true for 10.x.x.x network")
        void shouldReturnTrueFor10Network() {
            assertThat(ipAddressExtractor.isInternalNetwork("10.0.0.1")).isTrue();
            assertThat(ipAddressExtractor.isInternalNetwork("10.255.255.255")).isTrue();
        }

        @Test
        @DisplayName("Should return true for 172.16-31.x.x network")
        void shouldReturnTrueFor172Network() {
            assertThat(ipAddressExtractor.isInternalNetwork("172.16.0.1")).isTrue();
            assertThat(ipAddressExtractor.isInternalNetwork("172.31.255.255")).isTrue();
            assertThat(ipAddressExtractor.isInternalNetwork("172.20.10.5")).isTrue();
        }

        @Test
        @DisplayName("Should return true for 192.168.x.x network")
        void shouldReturnTrueFor192Network() {
            assertThat(ipAddressExtractor.isInternalNetwork("192.168.0.1")).isTrue();
            assertThat(ipAddressExtractor.isInternalNetwork("192.168.255.255")).isTrue();
        }

        @Test
        @DisplayName("Should return false for public IP")
        void shouldReturnFalseForPublicIp() {
            assertThat(ipAddressExtractor.isInternalNetwork("8.8.8.8")).isFalse();
            assertThat(ipAddressExtractor.isInternalNetwork("1.1.1.1")).isFalse();
        }
    }

    @Nested
    @DisplayName("isIPv4 Tests")
    class IsIpv4Tests {

        @Test
        @DisplayName("Should return false for null")
        void shouldReturnFalseForNull() {
            assertThat(ipAddressExtractor.isIPv4(null)).isFalse();
        }

        @Test
        @DisplayName("Should return false for empty string")
        void shouldReturnFalseForEmptyString() {
            assertThat(ipAddressExtractor.isIPv4("")).isFalse();
        }

        @Test
        @DisplayName("Should return true for valid IPv4")
        void shouldReturnTrueForValidIpv4() {
            assertThat(ipAddressExtractor.isIPv4("192.168.1.1")).isTrue();
            assertThat(ipAddressExtractor.isIPv4("0.0.0.0")).isTrue();
            assertThat(ipAddressExtractor.isIPv4("255.255.255.255")).isTrue();
        }

        @Test
        @DisplayName("Should return false for invalid format")
        void shouldReturnFalseForInvalidFormat() {
            assertThat(ipAddressExtractor.isIPv4("192.168.1")).isFalse();
            assertThat(ipAddressExtractor.isIPv4("192.168.1.1.1")).isFalse();
            assertThat(ipAddressExtractor.isIPv4("192.168.1.abc")).isFalse();
        }

        @Test
        @DisplayName("Should return false for out of range values")
        void shouldReturnFalseForOutOfRangeValues() {
            assertThat(ipAddressExtractor.isIPv4("256.1.1.1")).isFalse();
            assertThat(ipAddressExtractor.isIPv4("192.168.1.-1")).isFalse();
        }

        @Test
        @DisplayName("Should return false for IPv6 address")
        void shouldReturnFalseForIpv6() {
            assertThat(ipAddressExtractor.isIPv4("::1")).isFalse();
            assertThat(ipAddressExtractor.isIPv4("2001:db8::1")).isFalse();
        }
    }

    @Nested
    @DisplayName("maskIp Tests")
    class MaskIpTests {

        @Test
        @DisplayName("Should return null for null input")
        void shouldReturnNullForNullInput() {
            assertThat(ipAddressExtractor.maskIp(null)).isNull();
        }

        @Test
        @DisplayName("Should return empty for empty input")
        void shouldReturnEmptyForEmptyInput() {
            assertThat(ipAddressExtractor.maskIp("")).isEmpty();
        }

        @Test
        @DisplayName("Should mask last octet for IPv4")
        void shouldMaskLastOctetForIpv4() {
            assertThat(ipAddressExtractor.maskIp("192.168.1.100")).isEqualTo("192.168.1.***");
            assertThat(ipAddressExtractor.maskIp("10.0.0.1")).isEqualTo("10.0.0.***");
        }

        @Test
        @DisplayName("Should mask end for long non-IPv4 addresses")
        void shouldMaskEndForLongNonIpv4() {
            assertThat(ipAddressExtractor.maskIp("2001:db8::1")).isEqualTo("2001:db****");
        }

        @Test
        @DisplayName("Should return short IP unchanged")
        void shouldReturnShortIpUnchanged() {
            assertThat(ipAddressExtractor.maskIp("1.2.3.4")).isEqualTo("1.2.3.***");
        }
    }
}
