package com.example.demo;

import com.example.demo.service.TextGenerationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class TextGenerationServiceTest {

    @InjectMocks
    private TextGenerationService textGenerationService;

    @Mock
    private RestTemplate restTemplate;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testAskQuestion_Success() {
        // Arrange
        String modelName = "qwen2.5:14b";
        String question = "What is the capital of France?";
        String expectedResponse = "Paris";

        // Mocking the response from the RestTemplate
        when(restTemplate.postForObject(any(String.class), any(HttpEntity.class), eq(String.class)))
                .thenReturn("{\"response\": \"" + expectedResponse + "\"}");

        // Act
        String actualResponse = textGenerationService.askQuestion(question);

        // Assert
        assertTrue(actualResponse.contains(expectedResponse));
    }

    @Test
    public void testAskQuestion_Error() {
        // Arrange
        String modelName = "wrongModelName";
        String question = "What is the capital of France?";

        // Mocking an exception during the request
        when(restTemplate.postForObject(any(String.class), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RuntimeException("Error during request"));

        // Act
        String actualResponse = textGenerationService.askQuestion(question);

        // Assert
        assertTrue(actualResponse.contains("model 'wrongModelName' not found"), actualResponse);
    }
}
