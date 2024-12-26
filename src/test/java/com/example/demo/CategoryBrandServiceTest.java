package com.example.demo;

import com.example.demo.exception.CategoryBrandNotFoundException;
import com.example.demo.model.CategoryBrand;
import com.example.demo.repository.CategoryBrandRepository;
import com.example.demo.service.CategoryBrandService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class CategoryBrandServiceTest {

    @InjectMocks
    private CategoryBrandService categoryBrandService;

    @Mock
    private CategoryBrandRepository categoryBrandRepository;

    private CategoryBrand categoryBrand;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        categoryBrand = new CategoryBrand();
        categoryBrand.setId(1L);
        // Установите другие необходимые поля для categoryBrand
    }

    @Test
    public void testFindAll() {
        // Arrange
        when(categoryBrandRepository.findAll()).thenReturn(List.of(categoryBrand));

        // Act
        List<CategoryBrand> result = categoryBrandService.findAll();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(categoryBrand, result.get(0));
    }

    @Test
    public void testFindById_Success() {
        // Arrange
        when(categoryBrandRepository.findById(1L)).thenReturn(Optional.of(categoryBrand));

        // Act
        CategoryBrand foundCategoryBrand = categoryBrandService.findById(1L);

        // Assert
        assertNotNull(foundCategoryBrand);
        assertEquals(categoryBrand.getId(), foundCategoryBrand.getId());
    }

    @Test
    public void testFindById_NotFound() {
        // Arrange
        when(categoryBrandRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(CategoryBrandNotFoundException.class, () -> categoryBrandService.findById(1L));
    }

}


