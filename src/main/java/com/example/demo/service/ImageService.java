package com.example.demo.service;

import com.example.demo.model.Review;
import com.example.demo.model.ReviewImage;
import com.example.demo.repository.ImageRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.List;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class ImageService {
    private static final Logger logger = LoggerFactory.getLogger(ImageService.class);

    @Getter
    private static final String uploadDir = "uploads/images/";  // Base directory for images
    private final ImageRepository imageRepository;

    public Long countImagesByReview(Review review) {
        return imageRepository.countDistinctByReview(review);
    }

    public String storeImage(MultipartFile file) {
        try {
            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path filePath = Paths.get(uploadDir + fileName);

            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            return fileName; // Store this path in your DB
        } catch (IOException e) {
            logger.error("{} {}", e, e.getMessage());
        }
        return null;
    }

    public void deleteImage(String fileName) {
        try {
            Path filePath = Paths.get(uploadDir + fileName);
            if (Files.exists(filePath))
                Files.delete(filePath);
            else
                logger.info("File already deleted: {}", fileName);
        } catch (Exception e) {
            logger.error("{} {}", e, e.getMessage());
        }
    }

    public void deleteAllFilesByNames(Collection<String> fileNames) {
        for (String fileName : fileNames) {
            deleteImage(fileName);
        }
    }

    public void deleteAllFiles(Collection<ReviewImage> images) {
        for (ReviewImage image : images) {
            deleteImage(image.getImagePath());
        }
    }

    public List<ReviewImage> findAllByReview(Review review) {
        return imageRepository.findAllByReview(review);
    }

    public List<String> findUniqueByReview(Review review, PageRequest pageRequest) {
        return imageRepository.findUniqueByReview(review, pageRequest);
    }

    public void deleteByImagePath(String imagePath) {
        imageRepository.deleteAllByImagePath(imagePath);
    }

    public void deleteByImagePathIn(Collection<String> imagePaths) {
        imageRepository.deleteAllByImagePathIn(imagePaths);
    }

    public void deleteAllByReviewId(Long reviewId) {
        imageRepository.deleteAllByReviewId(reviewId);
    }

    public void save(ReviewImage image) {
        imageRepository.save(image);
    }
}
