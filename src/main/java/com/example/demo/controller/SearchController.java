package com.example.demo.controller;

import com.example.demo.dto.ReviewDTO;
import com.example.demo.dto.UserDTO;
import com.example.demo.enums.Status;
import com.example.demo.exception.UserNotFoundException;
import com.example.demo.model.Brand;
import com.example.demo.model.Category;
import com.example.demo.model.Review;
import com.example.demo.model.User;
import com.example.demo.service.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static com.example.demo.Utils.*;

@Controller
@AllArgsConstructor
public class SearchController {
    private static final Logger logger = LoggerFactory.getLogger(SearchController.class);
    private final ReviewService reviewService;
    private final ImageService imageService;
    private final BrandService brandService;
    private final CategoryService categoryService;
    private final UserService userService;

    @ResponseBody
    @GetMapping("/api/public/search")
    public ResponseEntity<?> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        var result = searchWithName(query, page, size);
        return ResponseEntity.ok(Map.of(
                "reviews", result.get("reviews").getContent(),
                "brands", result.get("brands").getContent(),
                "categories", result.get("categories").getContent(),
                "totalPages", Math.max(result.get("reviews").getTotalPages(), Math.max(
                        result.get("brands").getTotalPages(),
                        result.get("categories").getTotalPages())),
                "currentPage", page
        ));
    }

    @GetMapping("/search")
    public String searchPage(
            @RequestParam String query,
            Model model) {
        int page = 0;
        int size = 10;
        var result = searchWithName(query, page, size);
        model.addAllAttributes(Map.of(
                "query", query,
                "reviews", result.get("reviews").getContent(),
                "hasReviews", result.get("reviews").hasContent(),
                "brands", result.get("brands").getContent(),
                "hasBrands", result.get("brands").hasContent(),
                "categories", result.get("categories").getContent(),
                "hasCategories", result.get("categories").hasContent()
        ));

        return "search-page";
    }

    private Map<String, Page<?>> searchWithName(String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        List<Status> statuses = List.of(Status.PUBLISHED);
        Page<ReviewDTO> reviews = reviewService.findByStatusInAndTitleContaining(statuses, query, pageable)
                .map((review) -> mapToDTO(review, imageService));
        Page<Brand> brands = brandService.findByNameContaining(query, pageable)
                .map(brand -> (Brand) Hibernate.unproxy(brand));
        Page<Category> categories = categoryService.findByNameContaining(query, pageable)
                .map(category -> (Category) Hibernate.unproxy(category));
        return Map.of("reviews", reviews, "brands", brands, "categories", categories);
    }

    @GetMapping("/user/{userIdStr}")
    public String getUserProfile(
            @PathVariable String userIdStr,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        try {
            if (userIdStr == null || userIdStr.isEmpty()) return "redirect:/";
            long userId = Long.parseLong(userIdStr);
            if (userId < 1) return "error-404";
            User user = userService.findById(userId);
            Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
            Page<Review> publicReviews = reviewService.findAllByUserAndStatus(user, Status.PUBLISHED, pageable);

            long totalLikes = publicReviews.stream().mapToLong(Review::getLikes).sum();
            long reviewCount = publicReviews.getTotalElements();

            Map<String, Object> profileData = Map.of(
                    "user", new UserDTO(
                            user.getId(),
                            user.getVisibleName(),
                            user.getAvatarPath()),
                    "reviews", publicReviews.getContent().stream().map(i -> mapToDTO(i, imageService)).toList(),
                    "totalLikes", formatLikes(totalLikes),
                    "reviewCount", formatLikes(reviewCount),
                    "registrationDate", user.getRegistrationDate(),
                    "page", page,
                    "totalPages", publicReviews.getTotalPages(),
                    "hasNext", page + 1 < publicReviews.getTotalPages()
            );
            model.addAttribute("type", "user");
            model.addAllAttributes(profileData);

            return "search";
        } catch (NumberFormatException | UserNotFoundException e) {
            logError(logger, e);
            return "error-404";
        } catch (Exception e) {
            logError(logger, e);
            model.addAllAttributes(Map.of(
                    "status", HttpStatus.INTERNAL_SERVER_ERROR,
                    "error", e.getClass().toString(),
                    "message", e.getMessage()));
            return "error";
        }
    }

    @GetMapping("/brand/{brandIdStr}")
    public String getBrandReviews(
            @PathVariable String brandIdStr,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        try {
            if (brandIdStr == null || brandIdStr.isEmpty()) return "redirect:/";
            long brandId = Long.parseLong(brandIdStr);
            if (brandId < 1) return "error-404";
            Brand brand = brandService.findById(brandId);
            Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
            Page<Review> publicReviews = reviewService.findAllByBrandAndStatus(brand, Status.PUBLISHED, pageable);

            model.addAttribute("type", "brand");
            model.addAllAttributes(Map.of(
                    "brand", brand,
                    "reviews", publicReviews.getContent().stream().map(i -> mapToDTO(i, imageService)).toList(),
                    "page", page,
                    "totalPages", publicReviews.getTotalPages(),
                    "hasNext", page + 1 < publicReviews.getTotalPages()
            ));
            return "search";
        } catch (NumberFormatException | UserNotFoundException e) {
            logError(logger, e);
            return "error-404";
        } catch (Exception e) {
            logError(logger, e);
            model.addAllAttributes(Map.of(
                    "status", HttpStatus.INTERNAL_SERVER_ERROR,
                    "error", e.getClass().toString(),
                    "message", e.getMessage()));
            return "error";
        }
    }

    @GetMapping("/category/{categoryIdStr}")
    public String getCategoryReviews(
            @PathVariable String categoryIdStr,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        try {
            if (categoryIdStr == null || categoryIdStr.isEmpty()) return "redirect:/";
            long categoryId = Long.parseLong(categoryIdStr);
            if (categoryId == 0) return "redirect:/";
            if (categoryId < 1) return "error-404";

            Category category = categoryService.findById(categoryId);
            Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
            Page<Review> publicReviews = reviewService.findAllByStatusAndCategoryIn(Status.PUBLISHED, List.of(category), pageable);

            model.addAttribute("type", "category");
            model.addAllAttributes(Map.of(
                    "category", category,
                    "reviews", List.of(),
                    "page", page,
                    "totalPages", publicReviews.getTotalPages(),
                    "hasNext", page + 1 < publicReviews.getTotalPages()
            ));
            return "search";
        } catch (NumberFormatException | UserNotFoundException e) {
            logError(logger, e);
            return "error-404";
        } catch (Exception e) {
            logError(logger, e);
            model.addAllAttributes(Map.of(
                    "status", HttpStatus.INTERNAL_SERVER_ERROR,
                    "error", e.getClass().toString(),
                    "message", e.getMessage()));
            return "error";
        }
    }

    @ResponseBody
    @PostMapping("/api/public/get-reviews-by-categories/")
    public ResponseEntity<?> getCategoryReviews(@RequestBody Map<String, Object> params) {
        try {
            List<Long> categoryIds = new ObjectMapper().readValue(params.get("categoryIds").toString(), new TypeReference<>() {
            });
            if (categoryIds.isEmpty())
                return ResponseEntity.ok(Map.of(
                        "reviews", List.of(),
                        "page", 0,
                        "totalPages", 0,
                        "hasNext", false
                ));
            int page = Integer.parseInt(params.get("page").toString());
            int size = Integer.parseInt(params.get("size").toString());
            Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
            Page<Review> publicReviews = reviewService.findAllByStatusAndCategoryIdIn(Status.PUBLISHED, categoryIds, pageable);
            return ResponseEntity.ok(Map.of(
                    "reviews", publicReviews.getContent().stream().map(i -> mapToDTO(i, imageService)).toList(),
                    "page", page,
                    "totalPages", publicReviews.getTotalPages(),
                    "hasNext", publicReviews.hasNext()
            ));
        } catch (Exception e) {
            logError(logger, e);
            return makeResponseString(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
