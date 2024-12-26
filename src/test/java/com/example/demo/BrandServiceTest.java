package com.example.demo;

import com.example.demo.exception.BrandNotFoundException;
import com.example.demo.model.Brand;
import com.example.demo.repository.BrandRepository;
import com.example.demo.service.BrandService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class BrandServiceTest {

    @InjectMocks
    private BrandService brandService;

    @Mock
    private BrandRepository brandRepository;

    private Brand brand;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        brand = new Brand();
        brand.setId(1L);
        brand.setName("Test Brand");
    }

    @Test
    public void testFindById_Success() {
        // Arrange
        when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));

        // Act
        Brand foundBrand = brandService.findById(1L);

        // Assert
        assertNotNull(foundBrand);
        assertEquals("Test Brand", foundBrand.getName());
    }

    @Test
    public void testFindById_NotFound() {
        // Arrange
        when(brandRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(BrandNotFoundException.class, () -> brandService.findById(1L));
    }

    @Test
    public void testSaveBrand() {
        // Arrange
        when(brandRepository.save(brand)).thenReturn(brand);

        // Act
        Brand savedBrand = brandService.save(brand);

        // Assert
        assertNotNull(savedBrand);
        assertEquals("Test Brand", savedBrand.getName());
        verify(brandRepository, times(1)).save(brand);
    }

    @Test
    public void testDeleteBrand() {
        // Arrange
        doNothing().when(brandRepository).deleteById(1L);

        // Act
        brandService.deleteById(1L);

        // Assert
        verify(brandRepository, times(1)).deleteById(1L);
    }
}
