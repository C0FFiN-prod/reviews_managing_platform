package com.example.demo.controller;

import com.example.demo.component.AppConfig;
import com.example.demo.service.BrandService;
import com.example.demo.service.CategoryService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

import static com.example.demo.Utils.logError;
import static com.example.demo.Utils.makeResponseString;

@Controller
@AllArgsConstructor
public class CategoryBrandController {
    private final Logger logger = LoggerFactory.getLogger(CategoryBrandController.class);
    private final CategoryService categoryService;
    private final BrandService brandService;
    private final AppConfig appConfig;

    @GetMapping("/api/public/get-update-timestamps")
    public ResponseEntity<?> getUpdateTimestamps() {
        return ResponseEntity.ok(Map.of("updates", Map.of(
                "categories", appConfig.getLastUpdate().getCategories(),
                "brands", appConfig.getLastUpdate().getBrands()
        )));
    }

    @PostMapping("/api/public/get-categories")
    public ResponseEntity<?> getCategories(@RequestBody Map<String, Object> request) {
        try {
            List<Long> categoryIds = new ObjectMapper().readValue(
                    request.get("categoryIds").toString(), new TypeReference<>() {
                    });

            return ResponseEntity.ok(Map.of("categories", categoryService.findAllExcept(categoryIds)));
        } catch (Exception e) {
            logError(logger, e);
            return makeResponseString(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/api/public/get-brands")
    public ResponseEntity<?> getBrandsByCategories(@RequestBody Map<String, Object> request) {
        try {
            List<Long> categoryIds = new ObjectMapper().readValue(
                    request.get("categoryIds").toString(), new TypeReference<>() {
                    });

            return ResponseEntity.ok(
                    Map.of("brands", brandService.findBrandsByCategoryIds(categoryIds)));
        } catch (Exception e) {
            logError(logger, e);
            return makeResponseString(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

}
