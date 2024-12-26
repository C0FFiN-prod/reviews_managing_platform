package com.example.demo.repository;

import com.example.demo.model.ReviewComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewCommentRepository extends JpaRepository<ReviewComment, Long> {
    List<ReviewComment> findByReviewId(Long reviewId);
}
