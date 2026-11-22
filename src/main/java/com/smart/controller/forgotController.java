package com.smart.controller;

import com.smart.Services.PasswordResetService;
import com.smart.Services.PasswordRules;
import com.smart.helper.Message;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@Controller
public class forgotController {
    private final PasswordResetService resets;
    public forgotController(PasswordResetService resets) { this.resets = resets; }

    public static class EmailForm {
        @NotBlank @Email @Size(max = 255)
        private String email;
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    @GetMapping("/forgot")
    public String openEmailForm() { return "forgot_email_form"; }

    @PostMapping("/send-otp")
    public String sendOTP(@Valid @ModelAttribute EmailForm form, BindingResult errors, HttpSession session) {
        if (errors.hasErrors()) {
            session.setAttribute("message", new Message("Enter a valid email address.", "danger"));
            return "forgot_email_form";
        }
        resets.request(form.getEmail().trim().toLowerCase(java.util.Locale.ROOT), session);
        return "verify_otp";
    }

    @PostMapping("/verify-otp")
    public String verifyotp(@RequestParam("otp") String token, HttpSession session) {
        if (resets.verify(token, session)) return "password_change_form";
        session.setAttribute("message", new Message("The code is invalid or expired. Request a new code after one minute.", "danger"));
        return "verify_otp";
    }

    @PostMapping("/change-password")
    public String changepassword(@RequestParam String newPassword, HttpSession session) {
        if (!PasswordRules.isValid(newPassword)) {
            session.setAttribute("message", new Message(PasswordRules.MESSAGE, "danger"));
            return "password_change_form";
        }
        if (!resets.reset(newPassword, session)) {
            session.setAttribute("message", new Message("Request and verify a new reset code first.", "danger"));
            return "redirect:/forgot";
        }
        return "redirect:/signin?change=Password+changed+successfully";
    }
}
