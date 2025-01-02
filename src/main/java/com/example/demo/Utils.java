package com.example.demo;

import com.example.demo.dto.ReviewDTO;
import com.example.demo.enums.Role;
import com.example.demo.model.Review;
import com.example.demo.model.ReviewImage;
import com.example.demo.model.User;
import com.example.demo.service.ImageService;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Utils {

    private static final Logger logger = LoggerFactory.getLogger(Utils.class);

    public static String formatLikes(Long likes) {
        if (likes < 10000) {
            return Long.toString(likes);
        } else if (likes < 1000000) {
            return (likes / 1000) + "K";
        } else if (likes < 1000000000) {
            return (likes / 1000000) + "M";
        } else {
            return (likes / 1000000000) + "B";
        }
    }

    public static ResponseEntity<?> makeResponseString(String text, HttpStatus status) {
        return makeResponse(new JSONObject(Map.of("response", text)).toString(), status);
    }

    public static boolean userDetailsHasRole(UserDetails userDetails, Role role) {
        return userDetails.getAuthorities().stream().anyMatch(
                a -> a.getAuthority().replace("ROLE_", "").equals(role.toString()));
    }

    public static Role userDetailsGetRole(UserDetails userDetails) {
        return Role.valueOf(userDetails.getAuthorities().stream().findFirst()
                .orElseThrow().getAuthority().replace("ROLE_", ""));
    }

    public static void logError(Logger logger, Exception e) {
        logger.error("Error [{}]: {}", e, e.getMessage());
    }

    public static ResponseEntity<?> makeResponse(Object body, HttpStatus status) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<>(body, headers, status);
    }

    public static String listToString(List<String> items) {
        return items.stream()
                .map(s -> s.replace("\u0001", "\u0002")) // escape any delimiter chars in content
                .collect(Collectors.joining("\u0001"));
    }

    // Converting String back to List<String>
    public static List<String> stringToList(String concatenated) {
        if (concatenated == null || concatenated.isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.stream(concatenated.split("\u0001"))
                .map(s -> s.replace("\u0002", "\u0001")) // restore original chars
                .collect(Collectors.toList());
    }

    public static String cleanReviewContent(String content) {
        String cleanText = content.replaceAll("</div>", "\n")
                .replaceAll("(</?(img|br|div|b|i|font|p|a|span)[^>]*>)|(\\[\\[IMAGE:\\d+]])", "");
        return cleanText.trim();
    }

    public static String shortenReview(String reviewText, int maxLength) {
        if (reviewText == null || reviewText.isEmpty()) {
            return "";
        }
        String regex = "(^|(?<=\\s))(\\p{L}{1,3})\\s+";

        logger.info(cleanReviewContent(reviewText));
        logger.info(Arrays.toString(cleanReviewContent(reviewText)
                .replaceAll(regex, "$2&nbsp;").split("(?<=[.!?])\\s+")));
        String[] sentences = cleanReviewContent(reviewText)
                .replaceAll(regex, "$2&nbsp;")
                .split("(?<=[.!?])\\s+");
        if (sentences.length == 0) {
            return reviewText.length() <= maxLength ? reviewText : reviewText.substring(0, maxLength) + "...";
        }

        String firstSentence = sentences[0];
        String secondSentence = sentences.length > 1 ? sentences[1] : "";

        // Если первое предложение слишком длинное
        if (firstSentence.length() >= maxLength) {
            return truncateByWords(firstSentence, maxLength);
        }

        // Проверяем, помещаются ли оба предложения
        String combined = firstSentence + " " + secondSentence;
        if (combined.length() <= maxLength) {
            return combined;
        }

        // Если второе не влезает, пробуем разделить по запятой
        int commaIndex = secondSentence.indexOf(',');
        if (commaIndex != -1) {
            String[] parts = secondSentence.split(",");
            StringBuilder secondPart = new StringBuilder();

            long length = firstSentence.replace("&nbsp;", " ").length();
            int cnt = 0;
            for (String part : parts) {
                long shortPartLength = part.replace("&nbsp;", " ").trim().length();
                if (length + shortPartLength > maxLength) {
                    secondPart.setLength(secondPart.length() - 2);
                    break;
                }
                length += shortPartLength + 1;
                secondPart.append(part.trim()).append(", ");
                cnt++;
            }
            long lastWordsLength = 0;
            if (cnt < parts.length) {
                String[] lastBlockWords = parts[cnt].trim().split("\\s+");
                StringBuilder lastWords = new StringBuilder();
                long wordLength;
                cnt = 0;
                while ((cnt < lastBlockWords.length) &&
                        (lastWordsLength + length +
                                (wordLength = lastBlockWords[cnt].replace("&nbsp;", " ").length()) < maxLength)) {
                    lastWords.append(lastBlockWords[cnt]);
                    lastWordsLength += wordLength;
                    cnt++;
                }
                if ((lastBlockWords.length < 3 || cnt >= 3) && (length + lastWordsLength <= maxLength)) {
                    secondPart.append(lastWords);
                }
            }
            String withComma = firstSentence + " " + secondPart.toString().trim();
            if (length + lastWordsLength <= maxLength * 1.1) {
                return withComma + "...";
            }

            return truncateByWords(withComma, maxLength);
        }

        String truncated = firstSentence + " " + truncateByWords(secondSentence, maxLength - firstSentence.length() - 1);
        return truncated.trim();
    }

    private static String truncateByWords(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }

        String[] words = text.split("\\s+");
        StringBuilder truncated = new StringBuilder();
        long length = 0;
        long wordLength;
        for (String word : words) {
            if (length + (wordLength = word.replace("&nbsp;", "").length()) > maxLength) {
                break;
            }
            if (!truncated.isEmpty()) {
                truncated.append(" ");
            }
            truncated.append(word);
            length += wordLength;
        }
        return truncated.toString().trim() + "...";
    }

    public static ReviewDTO mapToDTO(Review review, ImageService imageService) {
        List<String> imagePaths = imageService.findUniqueByReview(
                review, PageRequest.of(0, 3)
        );
        Long totalImages = imageService.countImagesByReview(review);
        User user = review.getUser();
        boolean isUserNull = user == null;
        return new ReviewDTO(
                review.getId(),
                isUserNull ? -1 : user.getId(),
                isUserNull ? "Deleted User" : user.getVisibleName(),
                isUserNull ? null : user.getAvatarPath(),
                review.getRating(),
                formatLikes(review.getLikes()),
                review.getTitle(),
                review.getCreatedAt().toString(),
                review.getShortReview(),
                shortenReview(review.getContent(), 150),
                totalImages,
                imagePaths,
                review.getStatus()
        );
    }

    public static String replaceImageTags(String reviewContent, List<ReviewImage> images) {
        for (ReviewImage image : images) {
            String imageTag = String.format(
                    "<img src=\"/images/%s\" alt=\"Review Image\" data-img-key=\"%s\"" +
                            "data-img-index=\"%s\" class=\"review-img\"/>",
                    image.getImagePath(), image.getId(), image.getPosition());
            reviewContent = reviewContent.replaceAll("\\[\\[IMAGE:" + image.getPosition() + "]]", imageTag);
        }
        return reviewContent;
    }
}
