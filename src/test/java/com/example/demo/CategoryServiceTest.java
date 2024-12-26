package com.example.demo;


import com.example.demo.model.Category;
import com.example.demo.repository.CategoryRepository;
import com.example.demo.service.CategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class CategoryServiceTest {

    @InjectMocks
    private CategoryService categoryService;

    @Mock
    private CategoryRepository categoryRepository;

    private Category category;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        category = new Category();
        category.setId(1L);
        category.setName("Test Category");
        category.setParentId(0L);
    }

    @Test
    public void testFindById_Success() {
        // Arrange
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        // Act
        Category foundCategory = categoryService.findById(1L);

        // Assert
        assertNotNull(foundCategory);
        assertEquals("Test Category", foundCategory.getName());
    }


    @Test
    public void testSaveCategory() {
        // Arrange
        when(categoryRepository.save(category)).thenReturn(category);

        // Act
        Category savedCategory = categoryService.save(category);

        // Assert
        assertNotNull(savedCategory);
        assertEquals("Test Category", savedCategory.getName());
        verify(categoryRepository, times(1)).save(category);
    }

    @Test
    public void testDeleteCategory() {
        // Arrange
        doNothing().when(categoryRepository).delete(category);

        // Act
        categoryService.delete(category);

        // Assert
        verify(categoryRepository, times(1)).delete(category);
    }

    @Test
    public void testDeleteById() {
        // Arrange
        doNothing().when(categoryRepository).deleteById(1L);

        // Act
        categoryService.deleteById(1L);

        // Assert
        verify(categoryRepository, times(1)).deleteById(1L);
    }
}
