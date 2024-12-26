package com.example.demo.repository;

import com.example.demo.model.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.lang.NonNull;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    @NonNull
    Optional<Review> findById(@NonNull Long id);

    List<Review> findAllByCategoryId(@NonNull Long categoryId);

    List<Review> findAllByBrandId(@NonNull Long brandId);

    List<Review> findAllByUserId(@NonNull Long userId);

    List<Review> findAllByRating(@NonNull int rating);

    List<Review> findAllByModelContainingIgnoreCase(@NonNull String model);

    List<Review> findAllByTitleContainingIgnoreCase(@NonNull String substr);

    boolean existsById(@NonNull Long id);

    @Modifying
    @Query("DELETE FROM Review r WHERE r.id = :id")
    void deleteById(@Param("id") Long id);

    @NonNull
    Page<Review> findAll(@NonNull Pageable pageable);

    Page<Review> findAllByStatus(Status status, Pageable pageable);

    Page<Review> findPageByTitleContainingIgnoreCase(String title, Pageable pageable);

    Page<Review> findPageByUser(User user, Pageable pageable);

    @Query("""
                SELECT r FROM Review r
                WHERE r.user = :user AND r.status = com.example.demo.model.Status.PUBLISHED
            """)
    Page<Review> findPagePublicByUser(@Param("user") User user, Pageable pageable);

    Page<Review> findAllByStatusIn(Collection<Status> status, Pageable pageable);

    Page<Review> findAllByUserAndStatus(User user, Status status, Pageable pageable);

    Page<Review> findAllByBrandAndStatus(Brand brand, Status status, Pageable pageable);

    Page<Review> findAllByStatusAndCategoryIn(Status status, Collection<Category> categories, Pageable pageable);

    Page<Review> findAllByStatusAndCategoryIdIn(Status status, Collection<Long> categoryIds, Pageable pageable);
}
