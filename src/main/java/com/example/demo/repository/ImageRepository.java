package com.example.demo.repository;

import com.example.demo.model.Review;
import com.example.demo.model.ReviewImage;
import lombok.NonNull;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ImageRepository extends JpaRepository<ReviewImage, Long> {
    @NonNull
    Optional<ReviewImage> findById(@NonNull Long id);

    List<ReviewImage> findAllByReviewId(Long id);

    void deleteById(@NonNull Long id);

    void deleteAllByReviewId(Long id);

    @Query("SELECT COUNT(DISTINCT ri.imagePath) FROM ReviewImage ri WHERE ri.review = :review")
    Long countDistinctByReview(@Param("review") Review review);

    @Query("SELECT DISTINCT ri.imagePath FROM ReviewImage ri WHERE ri.review = :review")
    List<String> findUniqueByReview(@Param("review") Review review, Pageable pageable);

    void deleteAllByImagePath(String imagePath);

    void deleteAllByImagePathIn(Collection<String> imagePath);

    List<ReviewImage> findAllByReview(Review review);
}
