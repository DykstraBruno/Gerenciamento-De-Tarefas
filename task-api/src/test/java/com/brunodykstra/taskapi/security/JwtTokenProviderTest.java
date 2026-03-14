package com.brunodykstra.taskapi.security;

import org.junit.jupiter.api.*;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.assertj.core.api.Assertions.*;

@DisplayName("JwtTokenProvider — unit tests")
class JwtTokenProviderTest {

    private JwtTokenProvider jwt;
    private static final String SECRET = "testSecretKeyMustBeAtLeast256BitsLongForHmacSha256Algorithm!!";
    private static final long EXP      = 3600000L;
    private static final long EXPIRED  = -1000L;

    @BeforeEach
    void setUp() {
        jwt = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwt, "jwtSecret", SECRET);
        ReflectionTestUtils.setField(jwt, "jwtExpirationMs", EXP);
    }

    private UserDetails ud(String username) {
        return User.builder().username(username).password("pw").authorities(Collections.emptyList()).build();
    }

    @Nested @DisplayName("generateToken()")
    class Generate {
        @Test @DisplayName("returns non-blank token")
        void nonBlank() { assertThat(jwt.generateToken(ud("bruno"))).isNotBlank(); }

        @Test @DisplayName("token has 3 JWT parts")
        void threeparts() { assertThat(jwt.generateToken(ud("bruno")).split("\\.")).hasSize(3); }

        @Test @DisplayName("different users produce different tokens")
        void differentUsers() {
            assertThat(jwt.generateToken(ud("alice"))).isNotEqualTo(jwt.generateToken(ud("bob")));
        }
    }

    @Nested @DisplayName("getUsernameFromToken()")
    class GetUsername {
        @Test @DisplayName("extracts correct username")
        void extractsUsername() {
            assertThat(jwt.getUsernameFromToken(jwt.generateToken(ud("bruno")))).isEqualTo("bruno");
        }

        @Test @DisplayName("preserves special characters in username")
        void specialChars() {
            String u = "user.name+test@email.com";
            assertThat(jwt.getUsernameFromToken(jwt.generateToken(ud(u)))).isEqualTo(u);
        }
    }

    @Nested @DisplayName("validateToken()")
    class Validate {
        @Test @DisplayName("returns true for valid token and matching user")
        void valid() {
            UserDetails u = ud("bruno");
            assertThat(jwt.validateToken(jwt.generateToken(u), u)).isTrue();
        }

        @Test @DisplayName("returns false when username does not match")
        void wrongUser() {
            assertThat(jwt.validateToken(jwt.generateToken(ud("alice")), ud("bob"))).isFalse();
        }

        @Test @DisplayName("returns false for expired token")
        void expired() {
            ReflectionTestUtils.setField(jwt, "jwtExpirationMs", EXPIRED);
            assertThat(jwt.validateToken(jwt.generateToken(ud("bruno")), ud("bruno"))).isFalse();
        }
    }

    @Nested @DisplayName("isValidToken()")
    class IsValid {
        @Test @DisplayName("returns true for fresh token")
        void fresh() { assertThat(jwt.isValidToken(jwt.generateToken(ud("bruno")))).isTrue(); }

        @Test @DisplayName("returns false for malformed token")
        void malformed() { assertThat(jwt.isValidToken("not.a.jwt")).isFalse(); }

        @Test @DisplayName("returns false for empty string")
        void empty() { assertThat(jwt.isValidToken("")).isFalse(); }

        @Test @DisplayName("returns false for token signed with different secret")
        void wrongSecret() {
            JwtTokenProvider other = new JwtTokenProvider();
            ReflectionTestUtils.setField(other, "jwtSecret",
                "anotherSecretKeyThatIsAlso256BitsLongForHmacTests!!");
            ReflectionTestUtils.setField(other, "jwtExpirationMs", EXP);
            assertThat(jwt.isValidToken(other.generateToken(ud("bruno")))).isFalse();
        }

        @Test @DisplayName("returns false for expired token")
        void expired() {
            ReflectionTestUtils.setField(jwt, "jwtExpirationMs", EXPIRED);
            assertThat(jwt.isValidToken(jwt.generateToken(ud("bruno")))).isFalse();
        }
    }
}
