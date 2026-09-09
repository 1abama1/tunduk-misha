package org.misha.authservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.misha.authservice.dto.UserRegistrationDTO;
import org.misha.authservice.entity.RefreshToken;
import org.misha.authservice.entity.Role;
import org.misha.authservice.entity.User;
import org.misha.authservice.exception.AppException;
import org.misha.authservice.repository.RefreshTokenRepository;
import org.misha.authservice.repository.UserRepository;
import org.misha.authservice.security.JwtUtil;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Изолированные модульные тесты для сервиса авторизации и регистрации AuthService.
 * Архитектура: JUnit 5, Mockito, шаблон AAA (Arrange-Act-Assert).
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        // Установка значения @Value("${jwt.refresh.expiration}")
        ReflectionTestUtils.setField(authService, "refreshExpirationMs", 604_800_000L);
    }

    @Nested
    @DisplayName("Тестирование регистрации пользователя (register)")
    class RegisterTests {

        @Test
        @DisplayName("Успешная регистрация по Email и валидному паролю")
        void register_WhenValidEmailAndPassword_ShouldEncodePasswordAndSaveUser() {
            // [Arrange] Подготовка входных данных и поведения зависимостей
            UserRegistrationDTO dto = new UserRegistrationDTO();
            dto.setFullName("Иван Иванов");
            dto.setEmail("ivan@example.com");
            dto.setPassword("Secret123!");

            when(passwordEncoder.encode("Secret123!")).thenReturn("hashedPasswordXYZ");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User userToSave = invocation.getArgument(0);
                userToSave.setId(10L);
                return userToSave;
            });

            // [Act] Выполнение тестируемого действия
            User createdUser = authService.register(dto);

            // [Assert] Проверка состояния и взаимодействия
            assertThat(createdUser).isNotNull();
            assertThat(createdUser.getId()).isEqualTo(10L);
            assertThat(createdUser.getFullName()).isEqualTo("Иван Иванов");
            assertThat(createdUser.getEmail()).isEqualTo("ivan@example.com");
            assertThat(createdUser.getPasswordHash()).isEqualTo("hashedPasswordXYZ");
            assertThat(createdUser.getRole()).isEqualTo(Role.TRADER);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository, times(1)).save(captor.capture());
            assertThat(captor.getValue().getPasswordHash()).isEqualTo("hashedPasswordXYZ");
        }

        @Test
        @DisplayName("Выброс AppException (400), если не указаны ни email, ни телефон")
        void register_WhenNoEmailAndNoPhone_ShouldThrowBadRequestException() {
            // [Arrange]
            UserRegistrationDTO dto = new UserRegistrationDTO();
            dto.setPassword("Password123!");

            // [Act & Assert]
            AppException ex = assertThrows(AppException.class, () -> authService.register(dto));

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(ex.getCode()).isEqualTo("LOGIN_IDENTIFIER_REQUIRED");
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Выброс AppException (400), если пароль пустой")
        void register_WhenPasswordEmpty_ShouldThrowBadRequestException() {
            // [Arrange]
            UserRegistrationDTO dto = new UserRegistrationDTO();
            dto.setEmail("test@test.com");
            dto.setPassword("");

            // [Act & Assert]
            AppException ex = assertThrows(AppException.class, () -> authService.register(dto));

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(ex.getCode()).isEqualTo("PASSWORD_REQUIRED");
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Выброс AppException (409 CONFLICT) при нарушении unique-констрейнта в БД (email занят)")
        void register_WhenEmailAlreadyExists_ShouldThrowConflictException() {
            // [Arrange] Дубликат теперь ловится через unique-констрейнт (race-safe),
            // а не через existsByEmail pre-check
            UserRegistrationDTO dto = new UserRegistrationDTO();
            dto.setEmail("existing@example.com");
            dto.setPassword("Secret123!");

            when(userRepository.save(any(User.class)))
                    .thenThrow(new DataIntegrityViolationException("unique constraint"));

            // [Act & Assert]
            AppException ex = assertThrows(AppException.class, () -> authService.register(dto));

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(ex.getCode()).isEqualTo("CONFLICT");
        }

        @Test
        @DisplayName("Выброс AppException (409 CONFLICT) при нарушении unique-констрейнта в БД (телефон занят)")
        void register_WhenPhoneAlreadyExists_ShouldThrowConflictException() {
            // [Arrange]
            UserRegistrationDTO dto = new UserRegistrationDTO();
            dto.setPhone("+996555999888");
            dto.setPassword("Secret123!");

            when(userRepository.save(any(User.class)))
                    .thenThrow(new DataIntegrityViolationException("unique constraint"));

            // [Act & Assert]
            AppException ex = assertThrows(AppException.class, () -> authService.register(dto));

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(ex.getCode()).isEqualTo("CONFLICT");
        }
    }

    @Nested
    @DisplayName("Тестирование токенов (JWT и RefreshToken)")
    class TokenTests {

        @Test
        @DisplayName("issueTokenForUser: Генерация Access токена через JwtUtil")
        void issueTokenForUser_ShouldDelegateToJwtUtil() {
            // [Arrange]
            User user = User.builder()
                    .id(42L)
                    .role(Role.ADMIN)
                    .build();
            when(jwtUtil.generateAccessToken("42", "ADMIN")).thenReturn("mocked.jwt.token");

            // [Act]
            String token = authService.issueTokenForUser(user);

            // [Assert]
            assertThat(token).isEqualTo("mocked.jwt.token");
            verify(jwtUtil, times(1)).generateAccessToken("42", "ADMIN");
        }

        @Test
        @DisplayName("createRefreshForUser: Создание и сохранение Refresh токена в БД")
        void createRefreshForUser_ShouldSaveRefreshTokenWithJti() {
            // [Arrange]
            User user = User.builder().id(1L).build();
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // [Act]
            RefreshToken refreshToken = authService.createRefreshForUser(user);

            // [Assert]
            assertThat(refreshToken).isNotNull();
            assertThat(refreshToken.getUser()).isEqualTo(user);
            assertThat(refreshToken.getJti()).isNotBlank();
            assertThat(refreshToken.isRevoked()).isFalse();
            assertThat(refreshToken.getExpiresAt()).isAfter(java.time.OffsetDateTime.now());
            verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
        }

        @Test
        @DisplayName("revokeRefresh: Отзыв токена с установкой флага revoked = true")
        void revokeRefresh_ShouldMarkTokenRevokedAndSave() {
            // [Arrange]
            RefreshToken token = RefreshToken.builder()
                    .jti("uuid-123")
                    .revoked(false)
                    .build();

            // [Act]
            authService.revokeRefresh(token);

            // [Assert]
            assertThat(token.isRevoked()).isTrue();
            verify(refreshTokenRepository, times(1)).save(token);
        }
    }
}
