package com.example.demo.controller;

import com.example.demo.model.Review;
import com.example.demo.model.ReviewImage;
import com.example.demo.model.Role;
import com.example.demo.model.User;
import com.example.demo.service.ImageService;
import com.example.demo.service.ReviewService;
import com.example.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static com.example.demo.Utils.*;

@Controller
@RequiredArgsConstructor
public class HomeController {
    private static final Logger logger = LoggerFactory.getLogger(HomeController.class);
    private final UserService userService;
    private final ReviewService reviewService;
    private final ImageService imageService;

    @GetMapping("/home")
    public String home(
            @AuthenticationPrincipal UserDetails userDetails,
            Model model
    ) {
        try {
            if (userDetails == null) {
                System.out.println("Still null");
                return "redirect:/login";
            }
            Role role = userDetailsGetRole(userDetails);
            System.out.println("Retrieved role: " + role);
            model.addAttribute("role", role.toString());
            switch (role) {
                case Role.ADMIN:
                    model.addAttribute("users", userService.getAllUsers());
                    break;
                case Role.MANAGER:
                case Role.USER:
                    break;
            }
            return "home";
        } catch (Exception e) {
            logError(logger, e);
            model.addAllAttributes(Map.of(
                    "status", HttpStatus.INTERNAL_SERVER_ERROR,
                    "error", e.getClass().toString(),
                    "message", e.getMessage()));
            return "redirect:/error";
        }
    }

    @GetMapping("/new-review")
    public String newReview() {
        return "edit-review";
    }

    @GetMapping("/edit-review/{id}")
    public String editReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            Model model) {
        try {
            User user = userService.findByUsername(userDetails.getUsername());
            Review review = reviewService.findById(id);
            if (!review.getUser().getId().equals(user.getId())) {
                throw new Exception("Unable to edit someone else's review");
            }
            model.addAttribute("review", review);
            List<ReviewImage> images = imageService.findAllByReview(review);
            model.addAttribute("reviewContent", replaceImageTags(review.getContent(), images));
            var a = images.stream().map(ReviewImage::getImagePath).distinct().toList();
            model.addAttribute("gallery", a);
            logger.info("{}", a);
            return "edit-review";
        } catch (Exception e) {
            logError(logger, e);
            model.addAttribute("errorMessage",
                    e.toString().equals("NullPointerException") ? e.toString() : e.getMessage());
            return "redirect:/error";
        }
    }
}

