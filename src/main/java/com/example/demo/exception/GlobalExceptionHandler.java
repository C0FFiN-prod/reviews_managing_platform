package com.example.demo.exception;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(LoginAlreadyExistsException.class)
    public String handleLoginAlreadyExistsException(LoginAlreadyExistsException ex, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        return "redirect:/register"; // Redirect to the registration page
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public String handleEmailAlreadyExistsException(EmailAlreadyExistsException ex, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        return "redirect:/register"; // Redirect to the registration page
    }
}

