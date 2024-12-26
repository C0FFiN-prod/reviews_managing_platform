package com.example.demo.service;

import com.example.demo.dto.CategoryBrandDTO;
import com.example.demo.exception.BrandNotFoundException;
import com.example.demo.model.Brand;
import com.example.demo.model.CategoryBrand;
import com.example.demo.repository.BrandRepository;
import com.example.demo.repository.CategoryBrandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BrandService {
    private final BrandRepository brandRepository;

    public List<Brand> findAll() {
        return brandRepository.findAll();
    }

    public Brand findById(Long id) {
        return brandRepository.findById(id)
                .orElseThrow(() -> new BrandNotFoundException("Brand not found with id: " + id));
    }

    public Brand save(Brand brand) {
        return brandRepository.save(brand);
    }

    public void delete(Brand brand) {
        brandRepository.delete(brand);
    }

    public void deleteById(Long id) {
        brandRepository.deleteById(id);
    }

    public void deleteByName(String name) {
        brandRepository.deleteByName(name);
    }

    public List<Brand> findAllByCategoryId(Long categoryId) {
        return brandRepository.findBrandsByCategoryId(categoryId);
    }

    public List<CategoryBrandDTO> findBrandsByCategoryIds(List<Long> categoryIds) {
        return brandRepository.findCategoryBrandPairsByCategoryIds(categoryIds);
    }

    public Page<Brand> findByNameContaining(String name, Pageable pageable) {
        return brandRepository.findPageByNameContainingIgnoreCase(name, pageable);
    }
}
