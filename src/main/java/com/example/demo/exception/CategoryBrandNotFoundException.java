package com.example.demo.exception;

public class CategoryBrandNotFoundException extends RuntimeException {
    public CategoryBrandNotFoundException(String message) {
        super(message);
    }
}
