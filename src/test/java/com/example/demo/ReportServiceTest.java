package com.example.demo;

import com.example.demo.model.Report;
import com.example.demo.model.Review;
import com.example.demo.repository.ReportRepository;
import com.example.demo.repository.ReviewRepository;
import com.example.demo.service.ImageService;
import com.example.demo.service.ReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ReportServiceTest {

    @InjectMocks
    private ReportService reportService;

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ImageService imageService;

    private Review review;
    private Report report;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        review = new Review();
        review.setId(1L);
        report = new Report();
        report.setId(1L);
        report.setReview(review);
        report.setDescription("Test report");
    }

    @Test
    public void testSaveReport() {
        // Arrange
        when(reportRepository.save(report)).thenReturn(report);

        // Act
        Report savedReport = reportService.save(report);

        // Assert
        assertNotNull(savedReport);
        assertEquals("Test report", savedReport.getDescription());
        verify(reportRepository, times(1)).save(report);
    }

    @Test
    public void testFindAllByReview() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        when(reportRepository.findAllByReview(review, pageable)).thenReturn(Page.empty());

        // Act
        Page<Report> reports = reportService.findAllByReview(review, pageable);

        // Assert
        assertNotNull(reports);
        assertTrue(reports.isEmpty());
        verify(reportRepository, times(1)).findAllByReview(review, pageable);
    }

    @Test
    public void testResolveReport() {
        // Arrange
        doNothing().when(reportRepository).deleteById(1L);

        // Act
        reportService.resolveReport(1L);

        // Assert
        verify(reportRepository, times(1)).deleteById(1L);
    }

}
