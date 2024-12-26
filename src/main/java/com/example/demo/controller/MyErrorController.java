package com.example.demo.controller;

import com.example.demo.service.UserService;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

@Controller
@RequiredArgsConstructor
public class MyErrorController implements ErrorController {
    private final UserService userService;

    @RequestMapping("/error")
    public String handleError(
            @AuthenticationPrincipal UserDetails userDetails,
            Model model,
            HttpServletRequest request
    ) {
        if (userDetails != null) {
            model.addAttribute("user",
                    userService.findByUsername(userDetails.getUsername()));
        }
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int statusCode = 200;
        if (status != null) {
            statusCode = Integer.parseInt(status.toString());
        }
        if (statusCode == HttpStatus.NOT_FOUND.value()) {
            return "error-404";
        } else if (statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
            return "error-500";
        }

        Throwable throwable = (Throwable) request.getAttribute("javax.servlet.error.exception");
        String errorMessage = (String) request.getAttribute("javax.servlet.error.message");

        model.addAllAttributes(Map.of(
                "status", statusCode,
                "error", status != null ? HttpStatus.valueOf(statusCode).getReasonPhrase() : "",
                "message", errorMessage != null ? errorMessage : "",
                "exception", throwable != null ? throwable.getMessage() : ""
        ));
        return "error";
    }
}
