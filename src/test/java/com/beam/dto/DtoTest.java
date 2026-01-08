package com.beam.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DTO Unit Tests")
class DtoTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Nested
    @DisplayName("CreateRoomRequest Tests")
    class CreateRoomRequestTests {

        @Test
        @DisplayName("Should create valid request")
        void shouldCreateValidRequest() {
            CreateRoomRequest request = new CreateRoomRequest();
            request.setRoomName("Test Room");
            request.setDescription("Test Description");
            request.setRoomType("PUBLIC");
            request.setMaxMembers(50);

            Set<ConstraintViolation<CreateRoomRequest>> violations = validator.validate(request);
            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("Should have default values")
        void shouldHaveDefaultValues() {
            CreateRoomRequest request = new CreateRoomRequest();
            assertEquals("PUBLIC", request.getRoomType());
            assertEquals(100, request.getMaxMembers());
        }

        @Test
        @DisplayName("Should fail validation when room name is blank")
        void shouldFailValidationWhenRoomNameIsBlank() {
            CreateRoomRequest request = new CreateRoomRequest();
            request.setRoomName("");

            Set<ConstraintViolation<CreateRoomRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
        }

        @Test
        @DisplayName("Should fail validation when room name is too short")
        void shouldFailValidationWhenRoomNameIsTooShort() {
            CreateRoomRequest request = new CreateRoomRequest();
            request.setRoomName("A");

            Set<ConstraintViolation<CreateRoomRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
        }

        @Test
        @DisplayName("Should implement equals correctly")
        void shouldImplementEqualsCorrectly() {
            CreateRoomRequest request1 = new CreateRoomRequest();
            request1.setRoomName("Test");
            request1.setDescription("Desc");

            CreateRoomRequest request2 = new CreateRoomRequest();
            request2.setRoomName("Test");
            request2.setDescription("Desc");

            assertEquals(request1, request2);
            assertEquals(request1.hashCode(), request2.hashCode());
        }

        @Test
        @DisplayName("Should not equal different object")
        void shouldNotEqualDifferentObject() {
            CreateRoomRequest request1 = new CreateRoomRequest();
            request1.setRoomName("Test1");

            CreateRoomRequest request2 = new CreateRoomRequest();
            request2.setRoomName("Test2");

            assertNotEquals(request1, request2);
        }

        @Test
        @DisplayName("Should not equal null")
        void shouldNotEqualNull() {
            CreateRoomRequest request = new CreateRoomRequest();
            assertNotEquals(null, request);
        }

        @Test
        @DisplayName("Should equal itself")
        void shouldEqualItself() {
            CreateRoomRequest request = new CreateRoomRequest();
            assertEquals(request, request);
        }

        @Test
        @DisplayName("Should not equal different class")
        void shouldNotEqualDifferentClass() {
            CreateRoomRequest request = new CreateRoomRequest();
            assertNotEquals("string", request);
        }

        @Test
        @DisplayName("Should have proper toString")
        void shouldHaveProperToString() {
            CreateRoomRequest request = new CreateRoomRequest();
            request.setRoomName("Test");
            String str = request.toString();
            assertTrue(str.contains("Test"));
            assertTrue(str.contains("CreateRoomRequest"));
        }
    }

    @Nested
    @DisplayName("SendMessageRequest Tests")
    class SendMessageRequestTests {

        @Test
        @DisplayName("Should create valid request")
        void shouldCreateValidRequest() {
            SendMessageRequest request = new SendMessageRequest();
            request.setContent("Hello World");
            request.setMessageType("TEXT");

            Set<ConstraintViolation<SendMessageRequest>> violations = validator.validate(request);
            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("Should have default message type")
        void shouldHaveDefaultMessageType() {
            SendMessageRequest request = new SendMessageRequest();
            assertEquals("TEXT", request.getMessageType());
        }

        @Test
        @DisplayName("Should fail validation when content is blank")
        void shouldFailValidationWhenContentIsBlank() {
            SendMessageRequest request = new SendMessageRequest();
            request.setContent("");

            Set<ConstraintViolation<SendMessageRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
        }

        @Test
        @DisplayName("Should implement equals correctly")
        void shouldImplementEqualsCorrectly() {
            SendMessageRequest request1 = new SendMessageRequest();
            request1.setContent("Hello");

            SendMessageRequest request2 = new SendMessageRequest();
            request2.setContent("Hello");

            assertEquals(request1, request2);
            assertEquals(request1.hashCode(), request2.hashCode());
        }

        @Test
        @DisplayName("Should not equal different object")
        void shouldNotEqualDifferentObject() {
            SendMessageRequest request1 = new SendMessageRequest();
            request1.setContent("Hello");

            SendMessageRequest request2 = new SendMessageRequest();
            request2.setContent("World");

            assertNotEquals(request1, request2);
        }

        @Test
        @DisplayName("Should not equal null")
        void shouldNotEqualNull() {
            SendMessageRequest request = new SendMessageRequest();
            assertNotEquals(null, request);
        }

        @Test
        @DisplayName("Should equal itself")
        void shouldEqualItself() {
            SendMessageRequest request = new SendMessageRequest();
            assertEquals(request, request);
        }

        @Test
        @DisplayName("Should have proper toString")
        void shouldHaveProperToString() {
            SendMessageRequest request = new SendMessageRequest();
            request.setContent("Test message");
            String str = request.toString();
            assertTrue(str.contains("Test message"));
        }
    }

    @Nested
    @DisplayName("UpdateRoomRequest Tests")
    class UpdateRoomRequestTests {

        @Test
        @DisplayName("Should create valid request")
        void shouldCreateValidRequest() {
            UpdateRoomRequest request = new UpdateRoomRequest();
            request.setRoomName("Updated Room");
            request.setDescription("Updated Description");
            request.setMaxMembers(200);

            Set<ConstraintViolation<UpdateRoomRequest>> violations = validator.validate(request);
            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("Should allow null fields for partial update")
        void shouldAllowNullFieldsForPartialUpdate() {
            UpdateRoomRequest request = new UpdateRoomRequest();
            // All fields are null - should be valid for partial update

            Set<ConstraintViolation<UpdateRoomRequest>> violations = validator.validate(request);
            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("Should implement equals correctly")
        void shouldImplementEqualsCorrectly() {
            UpdateRoomRequest request1 = new UpdateRoomRequest();
            request1.setRoomName("Test");
            request1.setMaxMembers(50);

            UpdateRoomRequest request2 = new UpdateRoomRequest();
            request2.setRoomName("Test");
            request2.setMaxMembers(50);

            assertEquals(request1, request2);
            assertEquals(request1.hashCode(), request2.hashCode());
        }

        @Test
        @DisplayName("Should not equal null")
        void shouldNotEqualNull() {
            UpdateRoomRequest request = new UpdateRoomRequest();
            assertNotEquals(null, request);
        }

        @Test
        @DisplayName("Should equal itself")
        void shouldEqualItself() {
            UpdateRoomRequest request = new UpdateRoomRequest();
            assertEquals(request, request);
        }

        @Test
        @DisplayName("Should have proper toString")
        void shouldHaveProperToString() {
            UpdateRoomRequest request = new UpdateRoomRequest();
            request.setRoomName("TestRoom");
            String str = request.toString();
            assertTrue(str.contains("TestRoom"));
        }
    }

    @Nested
    @DisplayName("AddMemberRequest Tests")
    class AddMemberRequestTests {

        @Test
        @DisplayName("Should create valid request")
        void shouldCreateValidRequest() {
            AddMemberRequest request = new AddMemberRequest();
            request.setUserId(1L);

            Set<ConstraintViolation<AddMemberRequest>> violations = validator.validate(request);
            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("Should fail validation when userId is null")
        void shouldFailValidationWhenUserIdIsNull() {
            AddMemberRequest request = new AddMemberRequest();
            request.setUserId(null);

            Set<ConstraintViolation<AddMemberRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
        }

        @Test
        @DisplayName("Should fail validation when userId is not positive")
        void shouldFailValidationWhenUserIdIsNotPositive() {
            AddMemberRequest request = new AddMemberRequest();
            request.setUserId(-1L);

            Set<ConstraintViolation<AddMemberRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
        }

        @Test
        @DisplayName("Should implement equals correctly")
        void shouldImplementEqualsCorrectly() {
            AddMemberRequest request1 = new AddMemberRequest();
            request1.setUserId(1L);

            AddMemberRequest request2 = new AddMemberRequest();
            request2.setUserId(1L);

            assertEquals(request1, request2);
            assertEquals(request1.hashCode(), request2.hashCode());
        }

        @Test
        @DisplayName("Should not equal null")
        void shouldNotEqualNull() {
            AddMemberRequest request = new AddMemberRequest();
            assertNotEquals(null, request);
        }

        @Test
        @DisplayName("Should equal itself")
        void shouldEqualItself() {
            AddMemberRequest request = new AddMemberRequest();
            assertEquals(request, request);
        }

        @Test
        @DisplayName("Should have proper toString")
        void shouldHaveProperToString() {
            AddMemberRequest request = new AddMemberRequest();
            request.setUserId(123L);
            String str = request.toString();
            assertTrue(str.contains("123"));
        }
    }

    @Nested
    @DisplayName("FriendRequestDto Tests")
    class FriendRequestDtoTests {

        @Test
        @DisplayName("Should create valid request")
        void shouldCreateValidRequest() {
            FriendRequestDto request = new FriendRequestDto();
            request.setFriendId(1L);

            Set<ConstraintViolation<FriendRequestDto>> violations = validator.validate(request);
            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("Should fail validation when friendId is null")
        void shouldFailValidationWhenFriendIdIsNull() {
            FriendRequestDto request = new FriendRequestDto();
            request.setFriendId(null);

            Set<ConstraintViolation<FriendRequestDto>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
        }

        @Test
        @DisplayName("Should implement equals correctly")
        void shouldImplementEqualsCorrectly() {
            FriendRequestDto request1 = new FriendRequestDto();
            request1.setFriendId(1L);

            FriendRequestDto request2 = new FriendRequestDto();
            request2.setFriendId(1L);

            assertEquals(request1, request2);
            assertEquals(request1.hashCode(), request2.hashCode());
        }

        @Test
        @DisplayName("Should not equal null")
        void shouldNotEqualNull() {
            FriendRequestDto request = new FriendRequestDto();
            assertNotEquals(null, request);
        }

        @Test
        @DisplayName("Should equal itself")
        void shouldEqualItself() {
            FriendRequestDto request = new FriendRequestDto();
            assertEquals(request, request);
        }

        @Test
        @DisplayName("Should have proper toString")
        void shouldHaveProperToString() {
            FriendRequestDto request = new FriendRequestDto();
            request.setFriendId(456L);
            String str = request.toString();
            assertTrue(str.contains("456"));
        }
    }

    @Nested
    @DisplayName("AcceptFriendRequestDto Tests")
    class AcceptFriendRequestDtoTests {

        @Test
        @DisplayName("Should create valid request")
        void shouldCreateValidRequest() {
            AcceptFriendRequestDto request = new AcceptFriendRequestDto();
            request.setRequesterId(1L);

            Set<ConstraintViolation<AcceptFriendRequestDto>> violations = validator.validate(request);
            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("Should fail validation when requesterId is null")
        void shouldFailValidationWhenRequesterIdIsNull() {
            AcceptFriendRequestDto request = new AcceptFriendRequestDto();

            Set<ConstraintViolation<AcceptFriendRequestDto>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
        }

        @Test
        @DisplayName("Should implement equals correctly")
        void shouldImplementEqualsCorrectly() {
            AcceptFriendRequestDto request1 = new AcceptFriendRequestDto();
            request1.setRequesterId(1L);

            AcceptFriendRequestDto request2 = new AcceptFriendRequestDto();
            request2.setRequesterId(1L);

            assertEquals(request1, request2);
            assertEquals(request1.hashCode(), request2.hashCode());
        }

        @Test
        @DisplayName("Should not equal null")
        void shouldNotEqualNull() {
            AcceptFriendRequestDto request = new AcceptFriendRequestDto();
            assertNotEquals(null, request);
        }

        @Test
        @DisplayName("Should equal itself")
        void shouldEqualItself() {
            AcceptFriendRequestDto request = new AcceptFriendRequestDto();
            assertEquals(request, request);
        }

        @Test
        @DisplayName("Should have proper toString")
        void shouldHaveProperToString() {
            AcceptFriendRequestDto request = new AcceptFriendRequestDto();
            request.setRequesterId(789L);
            String str = request.toString();
            assertTrue(str.contains("789"));
        }
    }

    @Nested
    @DisplayName("BlockUserRequestDto Tests")
    class BlockUserRequestDtoTests {

        @Test
        @DisplayName("Should create valid request")
        void shouldCreateValidRequest() {
            BlockUserRequestDto request = new BlockUserRequestDto();
            request.setUserId(1L);

            Set<ConstraintViolation<BlockUserRequestDto>> violations = validator.validate(request);
            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("Should fail validation when userId is null")
        void shouldFailValidationWhenUserIdIsNull() {
            BlockUserRequestDto request = new BlockUserRequestDto();

            Set<ConstraintViolation<BlockUserRequestDto>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
        }

        @Test
        @DisplayName("Should implement equals correctly")
        void shouldImplementEqualsCorrectly() {
            BlockUserRequestDto request1 = new BlockUserRequestDto();
            request1.setUserId(1L);

            BlockUserRequestDto request2 = new BlockUserRequestDto();
            request2.setUserId(1L);

            assertEquals(request1, request2);
            assertEquals(request1.hashCode(), request2.hashCode());
        }

        @Test
        @DisplayName("Should not equal null")
        void shouldNotEqualNull() {
            BlockUserRequestDto request = new BlockUserRequestDto();
            assertNotEquals(null, request);
        }

        @Test
        @DisplayName("Should equal itself")
        void shouldEqualItself() {
            BlockUserRequestDto request = new BlockUserRequestDto();
            assertEquals(request, request);
        }

        @Test
        @DisplayName("Should have proper toString")
        void shouldHaveProperToString() {
            BlockUserRequestDto request = new BlockUserRequestDto();
            request.setUserId(999L);
            String str = request.toString();
            assertTrue(str.contains("999"));
        }
    }

    @Nested
    @DisplayName("EmailSendCodeRequest Tests")
    class EmailSendCodeRequestTests {

        @Test
        @DisplayName("Should create valid request")
        void shouldCreateValidRequest() {
            EmailSendCodeRequest request = new EmailSendCodeRequest();
            request.setEmail("test@example.com");

            Set<ConstraintViolation<EmailSendCodeRequest>> violations = validator.validate(request);
            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("Should create with constructor")
        void shouldCreateWithConstructor() {
            EmailSendCodeRequest request = new EmailSendCodeRequest("test@example.com");
            assertEquals("test@example.com", request.getEmail());
        }

        @Test
        @DisplayName("Should fail validation when email is blank")
        void shouldFailValidationWhenEmailIsBlank() {
            EmailSendCodeRequest request = new EmailSendCodeRequest();
            request.setEmail("");

            Set<ConstraintViolation<EmailSendCodeRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
        }

        @Test
        @DisplayName("Should fail validation when email format is invalid")
        void shouldFailValidationWhenEmailFormatIsInvalid() {
            EmailSendCodeRequest request = new EmailSendCodeRequest();
            request.setEmail("invalid-email");

            Set<ConstraintViolation<EmailSendCodeRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
        }
    }

    @Nested
    @DisplayName("EmailVerifyRequest Tests")
    class EmailVerifyRequestTests {

        @Test
        @DisplayName("Should create valid request")
        void shouldCreateValidRequest() {
            EmailVerifyRequest request = new EmailVerifyRequest();
            request.setEmail("test@example.com");
            request.setVerificationCode("123456");

            Set<ConstraintViolation<EmailVerifyRequest>> violations = validator.validate(request);
            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("Should create with constructor")
        void shouldCreateWithConstructor() {
            EmailVerifyRequest request = new EmailVerifyRequest("test@example.com", "123456");
            assertEquals("test@example.com", request.getEmail());
            assertEquals("123456", request.getVerificationCode());
        }

        @Test
        @DisplayName("Should fail validation when verification code is wrong length")
        void shouldFailValidationWhenVerificationCodeIsWrongLength() {
            EmailVerifyRequest request = new EmailVerifyRequest();
            request.setEmail("test@example.com");
            request.setVerificationCode("12345");

            Set<ConstraintViolation<EmailVerifyRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
        }

        @Test
        @DisplayName("Should fail validation when verification code contains non-digits")
        void shouldFailValidationWhenVerificationCodeContainsNonDigits() {
            EmailVerifyRequest request = new EmailVerifyRequest();
            request.setEmail("test@example.com");
            request.setVerificationCode("12345a");

            Set<ConstraintViolation<EmailVerifyRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
        }
    }

    @Nested
    @DisplayName("EmailRegisterRequest Tests")
    class EmailRegisterRequestTests {

        @Test
        @DisplayName("Should create valid request")
        void shouldCreateValidRequest() {
            EmailRegisterRequest request = new EmailRegisterRequest();
            request.setEmail("test@example.com");
            request.setDisplayName("Test User");
            request.setUsername("testuser");

            Set<ConstraintViolation<EmailRegisterRequest>> violations = validator.validate(request);
            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("Should create with constructor")
        void shouldCreateWithConstructor() {
            EmailRegisterRequest request = new EmailRegisterRequest("test@example.com", "Test User", "testuser");
            assertEquals("test@example.com", request.getEmail());
            assertEquals("Test User", request.getDisplayName());
            assertEquals("testuser", request.getUsername());
        }

        @Test
        @DisplayName("Should fail validation when username has invalid characters")
        void shouldFailValidationWhenUsernameHasInvalidCharacters() {
            EmailRegisterRequest request = new EmailRegisterRequest();
            request.setEmail("test@example.com");
            request.setDisplayName("Test User");
            request.setUsername("test-user!");

            Set<ConstraintViolation<EmailRegisterRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
        }

        @Test
        @DisplayName("Should fail validation when displayName is too short")
        void shouldFailValidationWhenDisplayNameIsTooShort() {
            EmailRegisterRequest request = new EmailRegisterRequest();
            request.setEmail("test@example.com");
            request.setDisplayName("T");
            request.setUsername("testuser");

            Set<ConstraintViolation<EmailRegisterRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
        }
    }

    @Nested
    @DisplayName("PhoneSendCodeRequest Tests")
    class PhoneSendCodeRequestTests {

        @Test
        @DisplayName("Should create valid request")
        void shouldCreateValidRequest() {
            PhoneSendCodeRequest request = new PhoneSendCodeRequest();
            request.setPhoneNumber("01012345678");

            Set<ConstraintViolation<PhoneSendCodeRequest>> violations = validator.validate(request);
            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("Should create with constructor")
        void shouldCreateWithConstructor() {
            PhoneSendCodeRequest request = new PhoneSendCodeRequest("01012345678");
            assertEquals("01012345678", request.getPhoneNumber());
        }

        @Test
        @DisplayName("Should fail validation when phone number format is invalid")
        void shouldFailValidationWhenPhoneNumberFormatIsInvalid() {
            PhoneSendCodeRequest request = new PhoneSendCodeRequest();
            request.setPhoneNumber("1234567890");

            Set<ConstraintViolation<PhoneSendCodeRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
        }

        @Test
        @DisplayName("Should fail validation when phone number is blank")
        void shouldFailValidationWhenPhoneNumberIsBlank() {
            PhoneSendCodeRequest request = new PhoneSendCodeRequest();
            request.setPhoneNumber("");

            Set<ConstraintViolation<PhoneSendCodeRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
        }
    }
}
