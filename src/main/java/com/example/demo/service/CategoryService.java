package com.example.demo.service;

import com.example.demo.model.Category;
import com.example.demo.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;

    public Category findById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("Category not found with id: " + id));
    }

    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    public List<Category> findAllExcept(List<Long> categoryIds) {
        return categoryRepository.findAllExcept(categoryIds);
    }

    public Category save(Category category) {
        return categoryRepository.save(category);
    }

    public void delete(Category category) {
        categoryRepository.delete(category);
    }

    public void deleteById(Long id) {
        categoryRepository.deleteById(id);
    }

    public void deleteByName(String name) {
        categoryRepository.deleteByName(name);
    }

    public List<Category> findAllSubcategoriesByParentId(Long parentId) {
        return categoryRepository.findAllSubcategoriesByParentId(parentId);
    }

    public List<Category> findAllParentCategories(Long categoryId) {
        return categoryRepository.findAllParentCategories(categoryId);
    }

    public Page<Category> findByNameContaining(String name, Pageable pageable) {
        return categoryRepository.findPageByNameContainingIgnoreCase(name, pageable);
    }
}