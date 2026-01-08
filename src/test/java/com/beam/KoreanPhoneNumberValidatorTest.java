package com.beam;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("KoreanPhoneNumberValidator Tests")
class KoreanPhoneNumberValidatorTest {

    private KoreanPhoneNumberValidator validator;

    @BeforeEach
    void setUp() {
        validator = new KoreanPhoneNumberValidator();
    }

    @Nested
    @DisplayName("isValidKoreanPhoneNumber Tests")
    class IsValidKoreanPhoneNumberTests {

        @Test
        @DisplayName("Should return true for valid mobile number")
        void shouldReturnTrueForValidMobileNumber() {
            assertTrue(validator.isValidKoreanPhoneNumber("010-1234-5678"));
            assertTrue(validator.isValidKoreanPhoneNumber("01012345678"));
            assertTrue(validator.isValidKoreanPhoneNumber("010-123-4567"));
        }

        @Test
        @DisplayName("Should return true for valid landline number")
        void shouldReturnTrueForValidLandlineNumber() {
            assertTrue(validator.isValidKoreanPhoneNumber("02-1234-5678"));
            assertTrue(validator.isValidKoreanPhoneNumber("031-123-4567"));
            assertTrue(validator.isValidKoreanPhoneNumber("0511234567"));
        }

        @Test
        @DisplayName("Should return false for null")
        void shouldReturnFalseForNull() {
            assertFalse(validator.isValidKoreanPhoneNumber(null));
        }

        @Test
        @DisplayName("Should return false for empty string")
        void shouldReturnFalseForEmptyString() {
            assertFalse(validator.isValidKoreanPhoneNumber(""));
            assertFalse(validator.isValidKoreanPhoneNumber("   "));
        }

        @Test
        @DisplayName("Should return false for invalid format")
        void shouldReturnFalseForInvalidFormat() {
            assertFalse(validator.isValidKoreanPhoneNumber("12345"));
            assertFalse(validator.isValidKoreanPhoneNumber("abc-def-ghij"));
        }
    }

    @Nested
    @DisplayName("normalizePhoneNumber Tests")
    class NormalizePhoneNumberTests {

        @Test
        @DisplayName("Should return null for null input")
        void shouldReturnNullForNullInput() {
            assertNull(validator.normalizePhoneNumber(null));
        }

        @Test
        @DisplayName("Should normalize 010 mobile number")
        void shouldNormalize010MobileNumber() {
            assertEquals("010-1234-5678", validator.normalizePhoneNumber("01012345678"));
        }

        @Test
        @DisplayName("Should normalize 02 Seoul number")
        void shouldNormalize02SeoulNumber() {
            assertEquals("02-1234-5678", validator.normalizePhoneNumber("0212345678"));
        }

        @Test
        @DisplayName("Should normalize 10 digit number")
        void shouldNormalize10DigitNumber() {
            assertEquals("031-123-4567", validator.normalizePhoneNumber("0311234567"));
        }

        @Test
        @DisplayName("Should return as-is for other formats")
        void shouldReturnAsIsForOtherFormats() {
            assertEquals("12345", validator.normalizePhoneNumber("12345"));
        }
    }

    @Nested
    @DisplayName("maskPhoneNumber Tests")
    class MaskPhoneNumberTests {

        @Test
        @DisplayName("Should return as-is for null")
        void shouldReturnAsIsForNull() {
            assertNull(validator.maskPhoneNumber(null));
        }

        @Test
        @DisplayName("Should return as-is for short number")
        void shouldReturnAsIsForShortNumber() {
            assertEquals("1234567", validator.maskPhoneNumber("1234567"));
        }

        @Test
        @DisplayName("Should mask 11 digit mobile number")
        void shouldMask11DigitMobileNumber() {
            assertEquals("010-****-5678", validator.maskPhoneNumber("01012345678"));
        }

        @Test
        @DisplayName("Should mask 10 digit number")
        void shouldMask10DigitNumber() {
            assertEquals("031***-4567", validator.maskPhoneNumber("0311234567"));
        }

        @Test
        @DisplayName("Should return as-is for other lengths")
        void shouldReturnAsIsForOtherLengths() {
            assertEquals("12345678901234", validator.maskPhoneNumber("12345678901234"));
        }
    }

    @Nested
    @DisplayName("getCarrierFromPhoneNumber Tests")
    class GetCarrierFromPhoneNumberTests {

        @Test
        @DisplayName("Should return UNKNOWN for null")
        void shouldReturnUnknownForNull() {
            assertEquals("UNKNOWN", validator.getCarrierFromPhoneNumber(null));
        }

        @Test
        @DisplayName("Should return MOBILE for 010")
        void shouldReturnMobileFor010() {
            assertEquals("MOBILE", validator.getCarrierFromPhoneNumber("010-1234-5678"));
        }

        @Test
        @DisplayName("Should return SEOUL for 02")
        void shouldReturnSeoulFor02() {
            assertEquals("SEOUL", validator.getCarrierFromPhoneNumber("02-1234-5678"));
        }

        @Test
        @DisplayName("Should return GYEONGGI for 031")
        void shouldReturnGyeonggiFor031() {
            assertEquals("GYEONGGI", validator.getCarrierFromPhoneNumber("031-123-4567"));
        }

        @Test
        @DisplayName("Should return INCHEON for 032")
        void shouldReturnIncheonFor032() {
            assertEquals("INCHEON", validator.getCarrierFromPhoneNumber("032-123-4567"));
        }

        @Test
        @DisplayName("Should return BUSAN for 051")
        void shouldReturnBusanFor051() {
            assertEquals("BUSAN", validator.getCarrierFromPhoneNumber("051-123-4567"));
        }

        @Test
        @DisplayName("Should return DAEGU for 053")
        void shouldReturnDaeguFor053() {
            assertEquals("DAEGU", validator.getCarrierFromPhoneNumber("053-123-4567"));
        }

        @Test
        @DisplayName("Should return GWANGJU for 062")
        void shouldReturnGwangjuFor062() {
            assertEquals("GWANGJU", validator.getCarrierFromPhoneNumber("062-123-4567"));
        }

        @Test
        @DisplayName("Should return DAEJEON for 042")
        void shouldReturnDaejeonFor042() {
            assertEquals("DAEJEON", validator.getCarrierFromPhoneNumber("042-123-4567"));
        }

        @Test
        @DisplayName("Should return SEJONG for 044")
        void shouldReturnSejongFor044() {
            assertEquals("SEJONG", validator.getCarrierFromPhoneNumber("044-123-4567"));
        }

        @Test
        @DisplayName("Should return ULSAN for 052")
        void shouldReturnUlsanFor052() {
            assertEquals("ULSAN", validator.getCarrierFromPhoneNumber("052-123-4567"));
        }

        @Test
        @DisplayName("Should return OTHER for other area codes")
        void shouldReturnOtherForOtherAreaCodes() {
            assertEquals("OTHER", validator.getCarrierFromPhoneNumber("055-123-4567"));
        }
    }
}
