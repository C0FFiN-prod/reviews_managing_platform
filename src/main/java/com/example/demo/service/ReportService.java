package com.example.demo.service;

import com.example.demo.dto.ReportDTO;
import com.example.demo.model.Report;
import com.example.demo.model.Review;
import com.example.demo.enums.Status;
import com.example.demo.repository.ReportRepository;
import com.example.demo.repository.ReviewRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.example.demo.Utils.mapToDTO;

@Service
@AllArgsConstructor
public class ReportService {
    private final ReportRepository reportRepository;
    private final ReviewRepository reviewRepository;
    private final ImageService imageService;

    public Report save(Report report) {
        return reportRepository.save(report);
    }

    public void resolveReport(Long reportId) {
        reportRepository.deleteById(reportId); // Удалить жалобу из базы данных
    }

    public Page<Report> findAllByReview(Review review, Pageable pageable) {
        return reportRepository.findAllByReview(review, pageable);
    }

    public void resolveReports(List<Long> reportIds) {
        reportRepository.deleteAllByIdInBatch(reportIds);
    }

    @Transactional
    public Map<String, ?> findReviewsWithReports(Pageable pageable) {
        Page<Review> reviewsPage = reviewRepository.findAllByStatusIn(
                List.of(Status.PUBLISHED, Status.MODERATION),
                pageable);
        //Map<String,Object> {reviews:{DTOi->{reports:[], page:0, totalPages:N}},hasNext:bool,totalPages:N,page:M}
        List<Object> reviewReportsMap = new ArrayList<>();

        for (Review review : reviewsPage.getContent()) {
            Page<Report> reports = reportRepository.findByReview(review, PageRequest.of(0, 10));
            if (reports.getNumberOfElements() == 0 && review.getStatus() != Status.MODERATION) continue;
            reviewReportsMap.add(Map.of(
                    "review", mapToDTO(review, imageService),
                    "totalPages", reports.getTotalPages(),
                    "totalItems", reports.getTotalElements(),
                    "page", 0,
                    "reports", reports.getContent().stream()
                            .map(report -> new ReportDTO(
                                    report.getId(),
                                    review.getId(),
                                    report.getDescription(),
                                    report.getCreatedAt()))
            ));
        }

        return Map.of(
                "reviews", reviewReportsMap,
                "totalPages", reviewsPage.getTotalPages(),
                "page", pageable.getPageNumber(),
                "hasNext", pageable.getPageNumber() + 1 < reviewsPage.getTotalPages());
    }

}
