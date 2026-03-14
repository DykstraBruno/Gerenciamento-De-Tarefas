package com.brunodykstra.taskapi.service;

import com.brunodykstra.taskapi.dto.AuthDTO;
import com.brunodykstra.taskapi.exception.BusinessException;
import com.brunodykstra.taskapi.model.User;
import com.brunodykstra.taskapi.repository.UserRepository;
import com.brunodykstra.taskapi.security.JwtTokenProvider;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService — unit tests")
class AuthServiceTest {

    @Mock private UserRepository       userRepository;
    @Mock private PasswordEncoder      passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtTokenProvider     jwtTokenProvider;
    @InjectMocks private AuthService authService;

    private static final String USERNAME = "brunodykstra";
    private static final String EMAIL    = "brunodykstra@gmail.com";
    private static final String PASSWORD = "senha123";
    private static final String TOKEN    = "mocked.jwt.token";

    private User savedUser;
    private Authentication mockAuth;
    private UserDetails mockUserDetails;

    @BeforeEach
    void setUp() {
        savedUser = User.builder().id(1L).username(USERNAME).email(EMAIL).password("encoded").build();

        mockUserDetails = org.springframework.security.core.userdetails.User.builder()
                .username(USERNAME).password("encoded").authorities(Collections.emptyList()).build();

        mockAuth = new UsernamePasswordAuthenticationToken(mockUserDetails, null, Collections.emptyList());
    }

    // ── register ───────────────────────────────────────────────────────────

    @Nested @DisplayName("register()")
    class Register {

        @Test @DisplayName("returns TokenResponse when registration succeeds")
        void success() {
            when(userRepository.existsByUsername(USERNAME)).thenReturn(false);
            when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(passwordEncoder.encode(PASSWORD)).thenReturn("encoded");
            when(userRepository.save(any())).thenReturn(savedUser);
            when(authenticationManager.authenticate(any())).thenReturn(mockAuth);
            when(jwtTokenProvider.generateToken(any())).thenReturn(TOKEN);

            AuthDTO.RegisterRequest req = new AuthDTO.RegisterRequest(USERNAME, EMAIL, PASSWORD);
            AuthDTO.TokenResponse res = authService.register(req);

            assertThat(res.getToken()).isEqualTo(TOKEN);
            assertThat(res.getUsername()).isEqualTo(USERNAME);
            assertThat(res.getEmail()).isEqualTo(EMAIL);
            assertThat(res.getType()).isEqualTo("Bearer");
        }

        @Test @DisplayName("throws BusinessException when username is taken")
        void duplicateUsername() {
            when(userRepository.existsByUsername(USERNAME)).thenReturn(true);
            assertThatThrownBy(() -> authService.register(new AuthDTO.RegisterRequest(USERNAME, EMAIL, PASSWORD)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(USERNAME);
            verify(userRepository, never()).save(any());
        }

        @Test @DisplayName("throws BusinessException when email is already in use")
        void duplicateEmail() {
            when(userRepository.existsByUsername(USERNAME)).thenReturn(false);
            when(userRepository.existsByEmail(EMAIL)).thenReturn(true);
            assertThatThrownBy(() -> authService.register(new AuthDTO.RegisterRequest(USERNAME, EMAIL, PASSWORD)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(EMAIL);
            verify(userRepository, never()).save(any());
        }

        @Test @DisplayName("encodes password before saving")
        void encodesPassword() {
            when(userRepository.existsByUsername(any())).thenReturn(false);
            when(userRepository.existsByEmail(any())).thenReturn(false);
            when(passwordEncoder.encode(PASSWORD)).thenReturn("bcrypt_encoded");
            when(userRepository.save(any())).thenReturn(savedUser);
            when(authenticationManager.authenticate(any())).thenReturn(mockAuth);
            when(jwtTokenProvider.generateToken(any())).thenReturn(TOKEN);

            authService.register(new AuthDTO.RegisterRequest(USERNAME, EMAIL, PASSWORD));

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getPassword()).isEqualTo("bcrypt_encoded");
        }
    }

    // ── login ──────────────────────────────────────────────────────────────

    @Nested @DisplayName("login()")
    class Login {

        @Test @DisplayName("returns TokenResponse on successful login")
        void success() {
            when(authenticationManager.authenticate(any())).thenReturn(mockAuth);
            when(jwtTokenProvider.generateToken(any())).thenReturn(TOKEN);
            when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(savedUser));

            AuthDTO.TokenResponse res = authService.login(new AuthDTO.LoginRequest(USERNAME, PASSWORD));

            assertThat(res.getToken()).isEqualTo(TOKEN);
            assertThat(res.getUsername()).isEqualTo(USERNAME);
            assertThat(res.getType()).isEqualTo("Bearer");
        }

        @Test @DisplayName("throws BadCredentialsException on wrong password")
        void wrongPassword() {
            when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

            assertThatThrownBy(() -> authService.login(new AuthDTO.LoginRequest(USERNAME, "wrong")))
                .isInstanceOf(BadCredentialsException.class);
        }
    }
}
