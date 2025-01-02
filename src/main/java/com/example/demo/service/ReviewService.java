package com.example.demo.service;

import com.example.demo.enums.Status;
import com.example.demo.exception.ReviewNotFoundException;
import com.example.demo.model.Brand;
import com.example.demo.model.Category;
import com.example.demo.model.Review;
import com.example.demo.model.User;
import com.example.demo.repository.ReviewRepository;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Collection;
import java.util.List;

@Service
@AllArgsConstructor
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final ImageService imageService;
    private final TransactionTemplate transactionTemplate;
    private EntityManager entityManager;

    public List<Review> findAll() {
        return reviewRepository.findAll();
    }

    public Page<Review> findAll(Pageable pageable) {
        return reviewRepository.findAll(pageable);
    }

    public Review findById(Long id) {
        return reviewRepository.findById(id)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found with id: " + id));
    }

    public List<Review> findAllByUser(User user) {
        return reviewRepository.findAllByUserId(user.getId());
    }

    public List<Review> findAllByUserId(Long userId) {
        return reviewRepository.findAllByUserId(userId);
    }

    public List<Review> findAllByCategoryId(Long categoryId) {
        return reviewRepository.findAllByCategoryId(categoryId);
    }

    public Page<Review> findAllByStatus(Status status, Pageable pageable) {
        return reviewRepository.findAllByStatus(status, pageable);
    }

    public Page<Review> findAllByUserAndStatus(User user, Status status, Pageable pageable) {
        return reviewRepository.findAllByUserAndStatus(user, status, pageable);
    }

    public Page<Review> findAllByBrandAndStatus(Brand brand, Status status, Pageable pageable) {
        return reviewRepository.findAllByBrandAndStatus(brand, status, pageable);
    }


    public Review save(Review review) {
        return reviewRepository.save(review);
    }

    public void deleteById(Long id) {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                try {
                    Review managedReview = reviewRepository.findById(id)
                            .orElseThrow(() -> new ReviewNotFoundException("Review not found with id: " + id));
                    // First get images while the review is still in database
                    var images = imageService.findAllByReview(managedReview);
                    // Then delete the review
                    reviewRepository.deleteById(id);
                    // Finally delete the image files
                    imageService.deleteAllFiles(images);
                } catch (Exception e) {
                    status.setRollbackOnly(); // Rollback transaction in case of an error
                    throw e;
                }
            }
        });
    }


    public Page<Review> findByStatusInAndTitleContaining(Collection<Status> statuses, String title, Pageable pageable) {
        return reviewRepository.findPageByStatusInAndTitleContainingIgnoreCase(statuses, title, pageable);
    }

    public Page<Review> findPageByUser(User user, Pageable pageable) {
        return reviewRepository.findPageByUser(user, pageable);
    }

    public Page<Review> findPagePublicByUser(User user, Pageable pageable) {
        return reviewRepository.findPagePublicByUser(user, pageable);
    }

    public Review updateStatus(Review review, Status status) {
        review.setStatus(status);
        return reviewRepository.save(review);
    }

    public boolean existsById(Long id) {
        return reviewRepository.existsById(id);
    }

    @Transactional
    public Review mergeOrSave(Review review) {
        if (review.getId() != null) {
            return entityManager.merge(review);
        } else {
            entityManager.persist(review);
            return review;
        }
    }

    public Page<Review> findAllByStatusAndCategoryIn(Status status, Collection<Category> categories, Pageable pageable) {
        return reviewRepository.findAllByStatusAndCategoryIn(status, categories, pageable);
    }

    public Page<Review> findAllByStatusAndCategoryIdIn(Status status, Collection<Long> categoryIds, Pageable pageable) {
        return reviewRepository.findAllByStatusAndCategoryIdIn(status, categoryIds, pageable);
    }
}
