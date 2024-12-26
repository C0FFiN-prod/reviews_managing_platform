package com.example.demo.controller;

import com.example.demo.dto.ReportDTO;
import com.example.demo.model.Report;
import com.example.demo.model.Review;
import com.example.demo.model.Role;
import com.example.demo.model.Status;
import com.example.demo.service.ReportService;
import com.example.demo.service.ReviewService;
import com.example.demo.service.TextGenerationService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.example.demo.Utils.*;
import static com.example.demo.Utils.makeResponseString;

@RestController
@RequiredArgsConstructor
public class ManagerController {
    private final ReportService reportService;
    private final ReviewService reviewService;
    private final Logger logger = LoggerFactory.getLogger(ManagerController.class);
    private final TextGenerationService textGenerationService;

    @GetMapping("/api/get-reports/{reviewId}")
    public ResponseEntity<?> getReportsByReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long reviewId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        Review review = reviewService.findById(reviewId);
        if (review.getStatus() == Status.DRAFT && !review.getUser().getUsername().equals(userDetails.getUsername())) {
            return makeResponseString("Not allowed to get reports of draft", HttpStatus.FORBIDDEN);
        }
        Pageable pageable = PageRequest.of(page, size);
        Page<Report> reports = reportService.findAllByReview(review, pageable);
        return ResponseEntity.ok(Map.of(
                "reports", reports.getContent().stream().map(report -> new ReportDTO(
                        report.getId(),
                        report.getReview().getId(),
                        report.getDescription(),
                        report.getCreatedAt())),
                "currentPage", reports.getNumber(),
                "totalItems", reports.getTotalElements(),
                "totalPages", reports.getTotalPages(),
                "hasNext", page + 1 < reports.getTotalPages()));
    }

    @GetMapping("/api/get-reports")
    public ResponseEntity<?> getReports(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        logger.info("{}", userDetails);
        logger.info("{} {}", userDetailsHasRole(userDetails, Role.MANAGER), userDetailsHasRole(userDetails, Role.USER));
        if (!userDetailsHasRole(userDetails, Role.MANAGER)) {
            return makeResponseString("Only managers can get all reports", HttpStatus.FORBIDDEN);
        }
        Pageable pageable = PageRequest.of(page, size);
        Map<String, ?> reports = reportService.findReviewsWithReports(pageable);
        return ResponseEntity.ok(Map.of("result", reports));
    }

    @PostMapping("/api/resolve-reports")
    public ResponseEntity<?> resolveReports(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, Object> requestData) {
        try {
            if (!userDetailsHasRole(userDetails, Role.MANAGER))
                return makeResponseString("Only managers can resolve reports", HttpStatus.FORBIDDEN);
            List<Long> reportIds = new ObjectMapper().readValue(requestData.get("reportIds").toString(), new TypeReference<>() {
            });
            reportService.resolveReports(reportIds);
            return ResponseEntity.ok(Map.of("reportIds", reportIds));
        } catch (Exception e) {
            logError(logger, e);
            return makeResponseString("Error: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/api/analyse-review")
    public ResponseEntity<?> analyseReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, Object> requestData) {
        try {
            if (!userDetailsHasRole(userDetails, Role.MANAGER))
                return makeResponseString("Only managers can use AI review analyser", HttpStatus.FORBIDDEN);
            Long reviewId = Long.valueOf(requestData.get("reviewId").toString());
            Review review = reviewService.findById(reviewId);
            if (review.getStatus() != Status.MODERATION && review.getStatus() != Status.PUBLISHED)
                return makeResponseString("Can only analyse published and moderated reviews", HttpStatus.FORBIDDEN);
            String text = "Review text:\n" + cleanReviewContent(review.getContent());
            Pageable pageable = PageRequest.of(0, 20);
            Page<Report> reports = reportService.findAllByReview(review, pageable);
            StringBuilder reportsText = new StringBuilder("\nReports:\n");
            for (Report report : reports.getContent()) {
                reportsText.append("[").append(report.getDescription()).append("]\n");
            }
            List<String> answer = List.of(textGenerationService.askQuestion(text + reportsText).split("\n"));
            StringBuilder result = new StringBuilder();

            for (String line : answer) {
                if (line.trim().isEmpty()) continue;
                List<String> parts = new ArrayList<>(Arrays.stream(line.split(":", 2)).toList());
                if (parts.size() < 2) parts.addFirst("INFO");
                logger.info(parts.toString());
                parts.addAll(Arrays.stream(parts.removeLast().split("#", 2)).toList());
                if (parts.size() < 3) parts.add("");
                logger.info(parts.toString());
                String type = switch (parts.get(0)) {
                    case "SEVERE" -> "danger";
                    case "WARN" -> "warning";
                    default -> "info";
                };

                result.append(parts.get(1)).append("<i class='text-").append(type).append("'>")
                        .append(parts.get(2)).append("</i><br>");
            }
            return makeResponseString(result.toString(), HttpStatus.OK);
        } catch (Exception e) {
            logError(logger, e);
            return makeResponseString("Error: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/api/update-review-status")
    public ResponseEntity<?> updateReviewStatus(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, Object> requestData) {
        if (!userDetailsHasRole(userDetails, Role.MANAGER))
            return makeResponseString("Only managers can update status of review", HttpStatus.FORBIDDEN);

        Long reviewId = Long.valueOf(requestData.get("reviewId").toString());
        Review review = reviewService.findById(reviewId);
        if (review.getStatus() == Status.DRAFT)
            return makeResponseString("Not allowed to change status of draft", HttpStatus.FORBIDDEN);

        Status status = Status.valueOf(requestData.get("status").toString());
        if (!userDetailsHasRole(userDetails, Role.MANAGER))
            return makeResponseString("Not allowed to change status to draft", HttpStatus.FORBIDDEN);

        reviewService.updateStatus(review, status); // Обновить статус обзора
        return ResponseEntity.ok(Map.of(
                "reviewStatus", status,
                "reviewId", reviewId));
    }
}
