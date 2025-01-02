package com.example.demo.controller;

import com.example.demo.dto.UserExtendedDTO;
import com.example.demo.model.User;
import com.example.demo.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

import static com.example.demo.Utils.logError;
import static org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;

@Controller
@AllArgsConstructor
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private final UserService userService;
    private final AuthenticationManager authManager;

    private boolean login(HttpServletRequest req, String user, String pass) {
        UsernamePasswordAuthenticationToken authReq
                = new UsernamePasswordAuthenticationToken(user, pass);
        Authentication auth = authManager.authenticate(authReq);

        SecurityContext sc = SecurityContextHolder.getContext();
        sc.setAuthentication(auth);
        HttpSession session = req.getSession(true);
        session.setAttribute(SPRING_SECURITY_CONTEXT_KEY, sc);
        return auth.isAuthenticated();
    }

    @GetMapping("/")
    public String welcome() {
        return "welcome";
    }

    @GetMapping("/login")
    public String loginForm(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("noAuthBtn", true);
        return (userDetails == null) ? "login" : "redirect:/";
    }

    @PostMapping("/login")
    public String loginUser(
            @RequestParam("username") String username,
            @RequestParam("password") String password,
            HttpServletRequest request) {
        boolean success = login(request, username, password);
        return success ? "redirect:/" : "redirect:/login";
    }

    @GetMapping("/register")
    public String registerForm(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("noAuthBtn", true);
        return (userDetails == null) ? "register" : "redirect:/";
    }

    @PostMapping("/register")
    public String registerUser(
            @RequestParam("username") String username,
            @RequestParam("password") String password,
            @RequestParam("passwordConfirm") String passwordConfirm,
            @RequestParam("email") String email,
            @RequestParam(value = "phone", required = false) String phone,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) {
        try {
            if (!password.equals(passwordConfirm)) {
                redirectAttributes.addFlashAttribute("errorMessage", "Passwords do not match");
                return "redirect:/register";
            }

            User user = new User();
            user.setUsername(username);
            user.setPassword(password);
            user.setEmail(email);
            user.setVisibleName(username);
            user.setPhoneNumber(phone);
            userService.registerUser(user);
            boolean success = login(request, username, password);
            return success ? "redirect:/home" : "redirect:/register";
        } catch (ConstraintViolationException e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    ((ConstraintViolation<?>) e.getConstraintViolations().toArray()[0]).getMessage());
            logError(logger, e);
            return "redirect:/register";
        } catch (Exception e) {
            if (e.toString().equals("NullPointerException"))
                redirectAttributes.addFlashAttribute("errorMessage", "Something went wrong");
            else
                redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            logError(logger, e);
            return "redirect:/register";
        }
    }

    @ResponseBody
    @GetMapping("/api/session-status")
    public ResponseEntity<?> checkSessionStatus(HttpSession session) {
        if (session == null || session.getAttribute("userDetails") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok().build();
    }

    @ResponseBody
    @GetMapping("/api/public/current-user")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) return ResponseEntity.ok(Map.of("user", false));
        User user = userService.findByUsername(userDetails.getUsername());
        return ResponseEntity.ok(Map.of("user", new UserExtendedDTO(user)));
    }


}
