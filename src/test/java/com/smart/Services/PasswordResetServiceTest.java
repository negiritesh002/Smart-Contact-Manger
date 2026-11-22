package com.smart.Services;

import com.smart.dao.UserRepository;
import com.smart.entities.User;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.time.*;
import java.util.regex.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PasswordResetServiceTest {
    UserRepository users = mock(UserRepository.class);
    EmailService emails = mock(EmailService.class);
    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    Clock clock = mock(Clock.class);
    Instant now = Instant.parse("2026-01-01T00:00:00Z");
    MockHttpSession session = new MockHttpSession();
    PasswordResetService service;
    User user;

    @BeforeEach void setUp() {
        when(clock.instant()).thenReturn(now);
        user = new User(); user.setEmail("test@example.com"); user.setEnabled(true);
        user.setPassword(encoder.encode("OldPassword1"));
        when(users.getUserByUserName(user.getEmail())).thenReturn(user);
        when(emails.sendEmail(anyString(), anyString(), anyString())).thenReturn(true);
        service = new PasswordResetService(users, emails, encoder, clock);
    }

    String requestToken() {
        service.request(user.getEmail(), session);
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(emails, atLeastOnce()).sendEmail(anyString(), body.capture(), eq(user.getEmail()));
        Matcher matcher = Pattern.compile("<b>([A-Za-z0-9_-]{43})</b>").matcher(body.getValue());
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    @Test void verifiedTokenCanResetOnlyOnce() {
        String token = requestToken();
        assertThat(service.reset("NewPassword2", session)).isFalse();
        assertThat(service.verify(token, session)).isTrue();
        assertThat(service.verify(token, session)).isFalse();
        assertThat(service.reset("NewPassword2", session)).isTrue();
        assertThat(encoder.matches("NewPassword2", user.getPassword())).isTrue();
        assertThat(service.reset("AnotherPassword3", session)).isFalse();
        verify(users, times(1)).saveAndFlush(user);
    }

    @Test void tokenAndVerifiedPermissionExpireAfterTenMinutes() {
        String token = requestToken();
        assertThat(service.verify(token, session)).isTrue();
        when(clock.instant()).thenReturn(now.plusSeconds(600));
        assertThat(service.reset("NewPassword2", session)).isFalse();
        assertThat(service.verify(token, session)).isFalse();
        verify(users, never()).saveAndFlush(any());
    }

    @Test void fiveWrongAttemptsBlockTheToken() {
        String token = requestToken();
        for (int i = 0; i < 5; i++) assertThat(service.verify("wrong", session)).isFalse();
        assertThat(service.verify(token, session)).isFalse();
    }

    @Test void tokensAreBoundToTheRequestingSession() {
        String token = requestToken();
        assertThat(service.verify(token, new MockHttpSession())).isFalse();
        assertThat(service.verify(token, session)).isTrue();
    }

    @Test void resendIsLimitedAndReplacesTheOldToken() {
        String old = requestToken();
        service.request(user.getEmail(), session);
        verify(emails, times(1)).sendEmail(anyString(), anyString(), anyString());
        when(clock.instant()).thenReturn(now.plusSeconds(61));
        String replacement = requestToken();
        assertThat(replacement).isNotEqualTo(old);
        assertThat(service.verify(old, session)).isFalse();
        assertThat(service.verify(replacement, session)).isTrue();
    }

    @Test void missingOrUnregisteredAccountCannotReset() {
        assertThat(service.reset("NewPassword2", session)).isFalse();
        service.request("missing@example.com", session);
        verify(emails, never()).sendEmail(anyString(), anyString(), anyString());
        assertThat(service.verify("a".repeat(43), session)).isFalse();
    }

    @Test void invalidPasswordDoesNotConsumeValidPermission() {
        String token = requestToken();
        service.verify(token, session);
        assertThat(service.reset("short", session)).isFalse();
        assertThat(service.reset("NewPassword2", session)).isTrue();
    }
}
