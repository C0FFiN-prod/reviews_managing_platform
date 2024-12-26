package com.example.demo.service;

import com.example.demo.exception.CategoryBrandNotFoundException;
import com.example.demo.model.CategoryBrand;
import com.example.demo.repository.CategoryBrandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryBrandService {
    private final CategoryBrandRepository categoryBrandRepository;

    public List<CategoryBrand> findAll() {
        return categoryBrandRepository.findAll();
    }

    public CategoryBrand findById(Long id) {
        return categoryBrandRepository.findById(id)
                .orElseThrow(() -> new CategoryBrandNotFoundException("CategoryBrand not found with id: " + id));
    }

    public List<CategoryBrand> findAllByCategoryId(Long categoryId) {
        return categoryBrandRepository.findAllByCategoryId(categoryId);
    }

    public List<CategoryBrand> findAllByBrandId(Long categoryId) {
        return categoryBrandRepository.findAllByCategoryId(categoryId);
    }
}
