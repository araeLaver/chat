package com.beam;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AuthResponse Tests")
class AuthResponseTest {

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create with default constructor")
        void shouldCreateWithDefaultConstructor() {
            AuthResponse response = new AuthResponse();
            assertNotNull(response);
        }

        @Test
        @DisplayName("Should create with all args constructor")
        void shouldCreateWithAllArgsConstructor() {
            AuthResponse response = new AuthResponse("token123", "user", 1L, "User Name", "010-1234-5678", "Success");

            assertEquals("token123", response.getToken());
            assertEquals("user", response.getUsername());
            assertEquals(1L, response.getUserId());
            assertEquals("User Name", response.getDisplayName());
            assertEquals("010-1234-5678", response.getPhoneNumber());
            assertEquals("Success", response.getMessage());
        }
    }

    @Nested
    @DisplayName("Getter/Setter Tests")
    class GetterSetterTests {

        @Test
        @DisplayName("Should set and get all fields")
        void shouldSetAndGetAllFields() {
            AuthResponse response = new AuthResponse();

            response.setToken("token");
            response.setUsername("username");
            response.setUserId(1L);
            response.setDisplayName("displayName");
            response.setPhoneNumber("010-1234-5678");
            response.setMessage("message");
            response.setEmail("test@example.com");
            response.setEmailVerified(true);

            assertEquals("token", response.getToken());
            assertEquals("username", response.getUsername());
            assertEquals(1L, response.getUserId());
            assertEquals("displayName", response.getDisplayName());
            assertEquals("010-1234-5678", response.getPhoneNumber());
            assertEquals("message", response.getMessage());
            assertEquals("test@example.com", response.getEmail());
            assertTrue(response.getEmailVerified());
        }
    }

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("Should build with all fields")
        void shouldBuildWithAllFields() {
            AuthResponse response = AuthResponse.builder()
                    .token("token123")
                    .username("user")
                    .userId(1L)
                    .displayName("User Name")
                    .phoneNumber("010-1234-5678")
                    .message("Success")
                    .email("test@example.com")
                    .emailVerified(true)
                    .build();

            assertEquals("token123", response.getToken());
            assertEquals("user", response.getUsername());
            assertEquals(1L, response.getUserId());
            assertEquals("User Name", response.getDisplayName());
            assertEquals("010-1234-5678", response.getPhoneNumber());
            assertEquals("Success", response.getMessage());
            assertEquals("test@example.com", response.getEmail());
            assertTrue(response.getEmailVerified());
        }

        @Test
        @DisplayName("Should build with partial fields")
        void shouldBuildWithPartialFields() {
            AuthResponse response = AuthResponse.builder()
                    .token("token123")
                    .username("user")
                    .build();

            assertEquals("token123", response.getToken());
            assertEquals("user", response.getUsername());
            assertNull(response.getUserId());
        }
    }

    @Nested
    @DisplayName("Equals and HashCode Tests")
    class EqualsHashCodeTests {

        @Test
        @DisplayName("Should be equal for same values")
        void shouldBeEqualForSameValues() {
            AuthResponse response1 = new AuthResponse("token", "user", 1L, "name", "phone", "msg");
            AuthResponse response2 = new AuthResponse("token", "user", 1L, "name", "phone", "msg");

            assertEquals(response1, response2);
            assertEquals(response1.hashCode(), response2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal for different values")
        void shouldNotBeEqualForDifferentValues() {
            AuthResponse response1 = new AuthResponse("token1", "user", 1L, "name", "phone", "msg");
            AuthResponse response2 = new AuthResponse("token2", "user", 1L, "name", "phone", "msg");

            assertNotEquals(response1, response2);
        }

        @Test
        @DisplayName("Should be equal to itself")
        void shouldBeEqualToItself() {
            AuthResponse response = new AuthResponse("token", "user", 1L, "name", "phone", "msg");
            assertEquals(response, response);
        }

        @Test
        @DisplayName("Should not be equal to null")
        void shouldNotBeEqualToNull() {
            AuthResponse response = new AuthResponse("token", "user", 1L, "name", "phone", "msg");
            assertNotEquals(response, null);
        }

        @Test
        @DisplayName("Should not be equal to different class")
        void shouldNotBeEqualToDifferentClass() {
            AuthResponse response = new AuthResponse("token", "user", 1L, "name", "phone", "msg");
            assertNotEquals(response, "string");
        }
    }

    @Nested
    @DisplayName("ToString Tests")
    class ToStringTests {

        @Test
        @DisplayName("Should contain all fields in toString")
        void shouldContainAllFieldsInToString() {
            AuthResponse response = new AuthResponse("token123", "user", 1L, "User Name", "010-1234-5678", "Success");

            String str = response.toString();

            assertTrue(str.contains("token123"));
            assertTrue(str.contains("user"));
            assertTrue(str.contains("User Name"));
            assertTrue(str.contains("010-1234-5678"));
            assertTrue(str.contains("Success"));
        }
    }
}
