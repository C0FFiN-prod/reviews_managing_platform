package com.example.demo.service;

import com.example.demo.model.ReviewComment;
import com.example.demo.repository.ReviewCommentRepository; // Предполагается, что у вас есть репозиторий для комментариев
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewCommentService {
    private final ReviewCommentRepository reviewCommentRepository;

    public List<ReviewComment> findByReviewId(Long reviewId) {
        return reviewCommentRepository.findByReviewId(reviewId);
    }

    public ReviewComment save(ReviewComment comment) {
        return reviewCommentRepository.save(comment);
    }
}
