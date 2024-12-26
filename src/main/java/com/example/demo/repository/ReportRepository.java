package com.example.demo.repository;

import com.example.demo.model.Report;
import com.example.demo.model.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    Page<Report> findAllByReview(Review review, Pageable pageable);

    Page<Report> findByReview(Review review, Pageable pageable);
}
