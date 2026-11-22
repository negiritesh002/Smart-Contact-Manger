package com.smart.Services;

import com.smart.dao.UserRepository;
import com.smart.entities.User;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;

@Service
public class PasswordResetService {
    private static final String STATE = "passwordReset";
    private final UserRepository users;
    private final EmailService emails;
    private final BCryptPasswordEncoder encoder;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    @org.springframework.beans.factory.annotation.Autowired
    public PasswordResetService(UserRepository users, EmailService emails, BCryptPasswordEncoder encoder) {
        this(users, emails, encoder, Clock.systemUTC());
    }

    PasswordResetService(UserRepository users, EmailService emails, BCryptPasswordEncoder encoder, Clock clock) {
        this.users = users; this.emails = emails; this.encoder = encoder; this.clock = clock;
    }

    public void request(String email, HttpSession session) {
        synchronized (session) {
            Instant now = clock.instant();
            State previous = (State) session.getAttribute(STATE);
            if (previous != null && now.isBefore(previous.sentAt.plusSeconds(60))) return;
            byte[] bytes = new byte[32];
            random.nextBytes(bytes);
            String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            State state = new State(email, digest(token), now);
            session.setAttribute(STATE, state);
            User user = users.getUserByUserName(email);
            if (user != null && user.isEnabled()) {
                boolean sent = emails.sendEmail("Password reset - Smart Contact Manager",
                        "<p>Paste this reset code in the same browser where you requested it:</p><p><b>"
                                + token + "</b></p><p>It expires in 10 minutes and can be used once.</p>", email);
                if (!sent) state.attempts = 5;
            }
        }
    }

    public boolean verify(String token, HttpSession session) {
        synchronized (session) {
            State state = active(session);
            if (state == null || state.verified || token == null || token.length() != 43) {
                if (state != null && !state.verified) state.attempts++;
                return false;
            }
            state.attempts++;
            if (!MessageDigest.isEqual(state.hash, digest(token))) return false;
            state.verified = true;
            return true;
        }
    }

    public boolean reset(String password, HttpSession session) {
        synchronized (session) {
            State state = active(session);
            if (state == null || !state.verified || !PasswordRules.isValid(password)) return false;
            // Consume before saving: concurrent requests cannot reuse this permission.
            session.removeAttribute(STATE);
            User user = users.getUserByUserName(state.email);
            if (user == null || !user.isEnabled()) return false;
            user.setPassword(encoder.encode(password));
            users.saveAndFlush(user);
            return true;
        }
    }

    private State active(HttpSession session) {
        State state = (State) session.getAttribute(STATE);
        if (state == null || !clock.instant().isBefore(state.sentAt.plus(Duration.ofMinutes(10)))
                || (!state.verified && state.attempts >= 5)) return null;
        return state;
    }

    private byte[] digest(String value) {
        try { return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)); }
        catch (NoSuchAlgorithmException ex) { throw new IllegalStateException("SHA-256 is unavailable"); }
    }

    private static final class State implements java.io.Serializable {
        private final String email;
        private final byte[] hash;
        private final Instant sentAt;
        private int attempts;
        private boolean verified;
        private State(String email, byte[] hash, Instant sentAt) {
            this.email = email; this.hash = hash; this.sentAt = sentAt;
        }
    }
}
