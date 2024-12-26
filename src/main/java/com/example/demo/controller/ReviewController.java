package com.example.demo.controller;

import com.example.demo.Pair;
import com.example.demo.dto.ReviewCommentDTO;
import com.example.demo.dto.ReviewDTO;
import com.example.demo.dto.UserDTO;
import com.example.demo.enums.Role;
import com.example.demo.enums.Status;
import com.example.demo.exception.ReviewNotFoundException;
import com.example.demo.model.*;
import com.example.demo.service.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

import static com.example.demo.Utils.*;
import static com.example.demo.Utils.makeResponseString;

@Controller
@AllArgsConstructor
public class ReviewController {
    private static final Logger logger = LoggerFactory.getLogger(ReviewController.class);

    private final ImageService imageService;
    private final ReviewService reviewService;
    private final CategoryService categoryService;
    private final BrandService brandService;
    private final UserService userService;

    private final ReviewCommentService reviewCommentService;
    private LikeService likeService;
    private BookmarkService bookmarkService;
    private ReportService reportService;
    private EntityManager entityManager;

    private static ReviewCommentDTO getReviewCommentDTO(User user, ReviewComment comment) {
        UserDTO userDTO;
        if (user != null && user.getId() != -1) {
            userDTO = new UserDTO(user.getId(),
                    user.getVisibleName(),
                    user.getAvatarPath());
        } else {
            userDTO = new UserDTO(-1L, "Deleted User", "");
        }
        return new ReviewCommentDTO(
                comment.getId(),
                comment.getCommentText(),
                comment.getCreatedAt(),
                comment.getIsAnonymous(),
                userDTO);
    }

    @GetMapping("/review/{id}")
    public String viewReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id, Model model) {
        try {
            Review review = reviewService.findById(id);
            if (!review.getStatus().equals(Status.PUBLISHED) &&
                    !review.getUser().getUsername().equals(userDetails.getUsername())) {
                return "error-404";
            }
            List<ReviewImage> images = imageService.findAllByReview(review);
            String contentWithImages = replaceImageTags(review.getContent(), images);
            review.setContent(contentWithImages);

            model.addAttribute("review", review);
            model.addAttribute("likes", formatLikes(review.getLikes()));
            model.addAttribute("images", images);
            return "review";
        } catch (ReviewNotFoundException e) {
            logError(logger, e);
            return "error-404";
        }
    }

    @ResponseBody
    @GetMapping("/api/public/get-reviews/{id}")
    public ResponseEntity<?> getReviewsById(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestParam(defaultValue = "true") Boolean published,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        User user;
        Page<Review> reviews;
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        if (published) {
            user = userService.findById(id);
            reviews = reviewService.findPagePublicByUser(user, pageable);
        } else if (userDetails != null) {
            user = userService.findByUsername(userDetails.getUsername());
            if (!user.getId().equals(id)) {
                return makeResponseString("Not allowed to get private reviews", HttpStatus.FORBIDDEN);
            }
            reviews = reviewService.findPageByUser(user, pageable);
        } else {
            return makeResponseString("Not allowed to get private reviews", HttpStatus.FORBIDDEN);
        }

        return ResponseEntity.ok(Map.of(
                "reviews", reviews.getContent().stream()
                        .map(review -> Map.of(
                                "id", review.getId(),
                                "rating", review.getRating(),
                                "likes", review.getLikes(),
                                "model", review.getModel(),
                                "title", review.getTitle(),
                                "createdAt", review.getCreatedAt(),
                                "status", review.getStatus().toString()
                        )).toList(),
                "totalPages", reviews.getTotalPages(),
                "totalElements", reviews.getTotalElements(),
                "currentPage", page
        ));
    }

    @ResponseBody
    @GetMapping("/api/review-status/{reviewId}")
    public ResponseEntity<?> getReviewStatus(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long reviewId) {
        try {
            User user = userService.findByUsername(userDetails.getUsername());
            Review review = reviewService.findById(reviewId);
            boolean isLiked = likeService.isLiked(user, review);
            boolean isBookmarked = bookmarkService.isBookmarked(user, review); // Предполагается, что у вас есть такой метод
            return ResponseEntity.ok(Map.of("isLiked", isLiked, "isBookmarked", isBookmarked));
        } catch (Exception e) {
            logError(logger, e);
            return makeResponseString(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @ResponseBody
    @PostMapping("/api/save-comment")
    public ResponseEntity<?> saveComment(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, Object> requestData
    ) {
        try {
            Long reviewId = Long.valueOf(requestData.get("reviewId").toString());
            Boolean isAnonymous = (Boolean) requestData.get("isAnonymous");
            String commentText = (String) requestData.get("commentText");
            User user = userService.findByUsername(userDetails.getUsername());
            ReviewComment comment = new ReviewComment();
            comment.setReview(reviewService.findById(reviewId));
            comment.setUser(user);
            comment.setCommentText(commentText);
            comment.setIsAnonymous(isAnonymous);
            ReviewComment savedComment = reviewCommentService.save(comment);
            logger.info("Comment saved: {}", savedComment);
            ReviewCommentDTO commentDTO = getReviewCommentDTO(user, comment);

            return ResponseEntity.ok(commentDTO);
        } catch (ReviewNotFoundException e) {
            return makeResponseString("Review not found", HttpStatus.NOT_FOUND);
        } catch (UsernameNotFoundException e) {
            return makeResponseString("User not found", HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            logError(logger, e);
            return makeResponseString(!e.toString().equals("NullPointerException") ? e.getMessage() : "Null Pointer Exception", HttpStatus.BAD_REQUEST);
        }
    }

    @ResponseBody
    @PostMapping("/api/toggle-like/{reviewId}")
    public ResponseEntity<?> toggleLike(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long reviewId) {
        try {
            User user = userService.findByUsername(userDetails.getUsername());
            Review review = reviewService.findById(reviewId);
            boolean isLiked = likeService.toggleLike(user, review);
            long likeCount = likeService.countLikes(review);
            review.setLikes(likeCount);
            reviewService.save(review);
            return ResponseEntity.ok(Map.of(
                    "isLiked", isLiked,
                    "likeCount", likeCount));
        } catch (Exception e) {
            logError(logger, e);
            return makeResponseString(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @ResponseBody
    @PostMapping("/api/toggle-bookmark/{reviewId}")
    public ResponseEntity<?> toggleBookmark(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long reviewId) {
        try {
            User user = userService.findByUsername(userDetails.getUsername());
            Review review = reviewService.findById(reviewId);
            boolean isBookmarked = bookmarkService.toggleBookmark(user, review);
            return ResponseEntity.ok(Map.of("isBookmarked", isBookmarked));
        } catch (Exception e) {
            logError(logger, e);
            return makeResponseString(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @ResponseBody
    @GetMapping("/api/public/get-comments/{id}")
    public ResponseEntity<?> getCommentsByReviewId(@PathVariable Long id) {
        List<ReviewComment> comments = reviewCommentService.findByReviewId(id);
        List<ReviewCommentDTO> commentDTOS = comments.stream().map(
                reviewComment -> getReviewCommentDTO(reviewComment.getUser(), reviewComment)).toList();
        return ResponseEntity.ok(commentDTOS);
    }

    @ResponseBody
    @GetMapping("/api/public/get-reviews")
    public ResponseEntity<?> getReviewsPublic(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            logger.info("Fetching reviews: page={}, size={}", page, size);
            return getResponseWithReviewDTOs(page, size, Sort.by("createdAt").descending(), Status.PUBLISHED);
        } catch (Exception e) {
            logError(logger, e);
            return makeResponseString(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @ResponseBody
    @GetMapping("/api/get-reviews/{statusString}")
    public ResponseEntity<?> getReviews(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @PathVariable String statusString
    ) {
        try {
            logger.info("Fetching reviews: page={}, size={}, status={}", page, size, statusString);
            Status status = Status.valueOf(statusString);
            if (status == Status.HIDDEN) {
                return makeResponseString("Hidden reviews are private", HttpStatus.FORBIDDEN);
            } else if (status != Status.PUBLISHED && userDetailsHasRole(userDetails, Role.USER)) {
                return makeResponseString("Users can see only published reviews", HttpStatus.FORBIDDEN);
            }
            return getResponseWithReviewDTOs(page, size, Sort.by("id").descending(), status);
        } catch (Exception e) {
            logError(logger, e);
            return makeResponseString("Error: " + e + '\n' + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private ResponseEntity<?> getResponseWithReviewDTOs(int page, int size, Sort sort, Status status) {
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Review> reviews = reviewService.findAllByStatus(status, pageable);
        if (page >= reviews.getTotalPages()) {
            logger.info("No pages left");
            return makeResponse(null, HttpStatus.NO_CONTENT);
        }
        Page<ReviewDTO> reviewDTOs = reviews.map((review) -> mapToDTO(review, imageService));
        System.out.println(reviewDTOs);

        Map<String, Object> response = new HashMap<>();
        response.put("reviews", reviewDTOs.getContent());
        response.put("currentPage", reviewDTOs.getNumber());
        response.put("totalItems", reviewDTOs.getTotalElements());
        response.put("totalPages", reviewDTOs.getTotalPages());

        return ResponseEntity.ok(response);
    }

    @Transactional
    @ResponseBody
    @PostMapping(value = "/api/save-review", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> saveReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(value = "reviewId", required = false) String reviewIdStr,
            @RequestParam("rating") String rating,
            @RequestParam("reviewBody") String reviewBody,
            @RequestParam("categoryId") String categoryId,
            @RequestParam("brandId") String brandId,
            @RequestParam("modelName") String modelName,
            @RequestParam("shortReview") String shortSummary,
            @RequestParam("action") String action,
            @RequestPart("pros") List<String> pros,
            @RequestPart("cons") List<String> cons,
            @RequestParam("positions") String positionsJson,
            @RequestPart(value = "images", required = false) List<MultipartFile> images // Handle files
    ) {
        List<Pair<Integer, String>> savedImages = new ArrayList<>();
        List<Pair<Integer, String>> updatedImages = new ArrayList<>();
        Set<String> unusedImages = null;
        boolean isNewReview = true;
        User user = userService.findByUsername(userDetails.getUsername());
        try {
            Review review;
            if (reviewIdStr != null) {
                Long reviewId = Long.parseLong(reviewIdStr);
                review = reviewService.findById(reviewId);
                if (!review.getUser().getId().equals(user.getId())) {
                    throw new BadRequestException("You cannot edit someone else's review.");
                }
                unusedImages = imageService.findAllByReview(review).stream()
                        .map(ReviewImage::getImagePath).collect(Collectors.toSet());
                isNewReview = false;
            } else {
                review = new Review();
                review.setUser(user);
            }
            review.setRating(Integer.parseInt(rating));
            review.setContent(reviewBody);
            Category category = categoryService.findById(Long.valueOf(categoryId));
            Brand brand = brandService.findById(Long.valueOf(brandId));
            review.setBrand(brand);
            review.setCategory(category);
            review.setPros(listToString(pros));
            review.setCons(listToString(cons));
            switch (action.toUpperCase()) {
                case "DRAFT":
                    review.setStatus(Status.DRAFT);
                    break;
                case "PUBLISH":
                    review.setStatus(Status.MODERATION);
                    break;
                default:
                    throw new BadRequestException("Invalid action");
            }

            review.setModel(modelName);
            review.setTitle(category.getName() + ' ' + brand.getName() + ' ' + modelName);
            review.setShortReview(shortSummary);
            Map<Integer, List<Integer>> positions = new ObjectMapper().readValue(positionsJson, new TypeReference<>() {
            });
            logger.info("Positions: {}", positions);
            if (images != null)
                for (MultipartFile image : images) {
                    if (image.getOriginalFilename() == null) {
                        throw new BadRequestException("Invalid image filename");
                    }
                    logger.info(image.getOriginalFilename());
                    Integer index = Integer.valueOf(image.getOriginalFilename().split("_")[0]);
                    if (Objects.equals(image.getContentType(), "text/plain")) {
                        String fileName = image.getOriginalFilename().split("_", 2)[1];
                        updatedImages.add(new Pair<>(index, fileName));
                        if (unusedImages != null) unusedImages.remove(fileName);
                        logger.info("Updated file: {}", fileName);
                    } else {
                        String fileName = imageService.storeImage(image);
                        savedImages.add(new Pair<>(index, fileName));
                        logger.info("Saved file: {}", fileName);
                    }
                }
            if (review.getId() != null && reviewService.existsById(review.getId()))
                imageService.deleteAllByReviewId(review.getId());
            review = reviewService.mergeOrSave(review);
            saveReviewImages(savedImages, review, positions);
            if (unusedImages != null && !unusedImages.isEmpty())
                imageService.deleteAllFilesByNames(unusedImages);

            saveReviewImages(updatedImages, review, positions);
            if (isNewReview)
                user.setReviewCount(user.getReviewCount() + 1);
            return makeResponseString("Review saved successfully", HttpStatus.OK);
        } catch (Exception e) {
            logError(logger, e);
            for (var image : savedImages) {
                imageService.deleteImage(image.second);
            }
            return makeResponseString(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }


    @Transactional
    protected void saveReviewImages(
            List<Pair<Integer, String>> updatedImages,
            Review review, Map<Integer,
            List<Integer>> positions) {
        for (Pair<Integer, String> pair : updatedImages) {
            for (var position : positions.get(pair.first)) {
                ReviewImage image = new ReviewImage();
                image.setReview(review);
                image.setImagePath(pair.second);
                image.setPosition(position);
                imageService.save(image);
            }
        }
    }

    @ResponseBody
    @PostMapping("/api/report-review")
    public ResponseEntity<?> reportReview(
            @RequestBody Map<String, Object> requestData) {
        try {
            Long reviewId = Long.valueOf(requestData.get("reviewId").toString());
            String description = requestData.get("description").toString();
            Review review = reviewService.findById(reviewId);

            Report report = new Report();
            report.setReview(review);
            report.setDescription(description);
            reportService.save(report);

            return makeResponseString("Report submitted successfully", HttpStatus.OK);
        } catch (Exception e) {
            logError(logger, e);
            return makeResponseString("Error: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Transactional
    @ResponseBody
    @PostMapping("/api/delete-review")
    public ResponseEntity<?> deleteReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, Object> requestData) {
        try {
            Long reviewId = Long.valueOf(requestData.get("reviewId").toString());
            Review review = reviewService.findById(reviewId);
            User user = review.getUser();
            if (!user.getUsername().equals(userDetails.getUsername())) {
                return makeResponseString("Only author can delete theirs review", HttpStatus.FORBIDDEN);
            }
            reviewService.deleteById(review.getId());
            user.setReviewCount(user.getReviewCount() - 1);
            return makeResponseString("Review deleted successfully", HttpStatus.OK);
        } catch (Exception e) {
            logError(logger, e);
            return makeResponseString("Error: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}
