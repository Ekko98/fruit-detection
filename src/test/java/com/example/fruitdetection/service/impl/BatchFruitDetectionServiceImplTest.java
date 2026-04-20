package com.example.fruitdetection.service.impl;

import com.example.fruitdetection.dto.BatchFruitDetectionRequest;
import com.example.fruitdetection.dto.BatchFruitDetectionResponse;
import com.example.fruitdetection.dto.FruitDetectionResponse;
import com.example.fruitdetection.service.BatchFruitDetectionService;
import com.example.fruitdetection.service.FruitDetectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BatchFruitDetectionServiceImplTest {

    @Mock
    private FruitDetectionService fruitDetectionService;

    @InjectMocks
    private BatchFruitDetectionServiceImpl batchFruitDetectionService;

    private List<String> testImageDataList;
    private FruitDetectionResponse mockSingleResponse;

    @BeforeEach
    void setUp() {
        testImageDataList = new ArrayList<>();
        testImageDataList.add("testBase64Image1");
        testImageDataList.add("testBase64Image2");

        mockSingleResponse = new FruitDetectionResponse();
        mockSingleResponse.setFruitType("apple");
        mockSingleResponse.setFreshness("fresh");
        mockSingleResponse.setConfidence(0.95);
    }

    @Test
    void testDetectBatch_NormalCase() {
        // Arrange
        when(fruitDetectionService.detectFruit(any())).thenReturn(mockSingleResponse);

        BatchFruitDetectionRequest request = new BatchFruitDetectionRequest();
        request.setImageDataList(testImageDataList);

        // Act
        BatchFruitDetectionResponse response = batchFruitDetectionService.detectBatch(request);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getResults());
        assertEquals(2, response.getResults().size());
        assertEquals("apple", response.getResults().get(0).getFruitType());
        assertEquals("fresh", response.getResults().get(0).getFreshness());
        assertEquals(0.95, response.getResults().get(0).getConfidence());
        assertEquals("apple", response.getResults().get(1).getFruitType());
        assertEquals("fresh", response.getResults().get(1).getFreshness());
        assertEquals(0.95, response.getResults().get(1).getConfidence());

        // Verify that the service was called twice
        verify(fruitDetectionService, times(2)).detectFruit(any());
    }

    @Test
    void testDetectBatch_EmptyList() {
        // Arrange
        BatchFruitDetectionRequest request = new BatchFruitDetectionRequest();
        request.setImageDataList(new ArrayList<>());

        // Act
        BatchFruitDetectionResponse response = batchFruitDetectionService.detectBatch(request);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getResults());
        assertTrue(response.getResults().isEmpty());

        // Verify that the service was not called
        verify(fruitDetectionService, never()).detectFruit(any());
    }

    @Test
    void testDetectBatch_NullRequest() {
        // Act
        BatchFruitDetectionResponse response = batchFruitDetectionService.detectBatch(null);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getResults());
        assertTrue(response.getResults().isEmpty());

        // Verify that the service was not called
        verify(fruitDetectionService, never()).detectFruit(any());
    }

    @Test
    void testDetectBatch_NullImageDataList() {
        // Arrange
        BatchFruitDetectionRequest request = new BatchFruitDetectionRequest();
        request.setImageDataList(null);

        // Act
        BatchFruitDetectionResponse response = batchFruitDetectionService.detectBatch(request);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getResults());
        assertTrue(response.getResults().isEmpty());

        // Verify that the service was not called
        verify(fruitDetectionService, never()).detectFruit(any());
    }

    @Test
    void testDetectBatch_ExceedsLimit() {
        // Arrange
        List<String> manyImages = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            manyImages.add("testBase64Image" + i);
        }

        BatchFruitDetectionRequest request = new BatchFruitDetectionRequest();
        request.setImageDataList(manyImages);

        when(fruitDetectionService.detectFruit(any())).thenReturn(mockSingleResponse);

        // Act
        BatchFruitDetectionResponse response = batchFruitDetectionService.detectBatch(request);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getResults());
        // Should only process first 10 images due to limit
        assertEquals(10, response.getResults().size());

        // Verify that the service was called 10 times
        verify(fruitDetectionService, times(10)).detectFruit(any());
    }

    @Test
    void testDetectBatch_SingleImage() {
        // Arrange
        List<String> singleImageList = new ArrayList<>();
        singleImageList.add("testBase64Image");

        BatchFruitDetectionRequest request = new BatchFruitDetectionRequest();
        request.setImageDataList(singleImageList);

        when(fruitDetectionService.detectFruit(any())).thenReturn(mockSingleResponse);

        // Act
        BatchFruitDetectionResponse response = batchFruitDetectionService.detectBatch(request);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getResults());
        assertEquals(1, response.getResults().size());
        assertEquals("apple", response.getResults().get(0).getFruitType());
        assertEquals("fresh", response.getResults().get(0).getFreshness());
        assertEquals(0.95, response.getResults().get(0).getConfidence());

        // Verify that the service was called once
        verify(fruitDetectionService, times(1)).detectFruit(any());
    }

    @Test
    void testDetectBatch_WithExceptions() {
        // Arrange
        List<String> imageDataList = new ArrayList<>();
        imageDataList.add("goodImage");
        imageDataList.add("badImage");
    
        BatchFruitDetectionRequest request = new BatchFruitDetectionRequest();
        request.setImageDataList(imageDataList);

        // First call succeeds, second throws exception
        when(fruitDetectionService.detectFruit(any()))
                .thenReturn(mockSingleResponse)
                .thenThrow(new RuntimeException("Processing failed"));

        // Act
        BatchFruitDetectionResponse response = batchFruitDetectionService.detectBatch(request);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getResults());
        assertEquals(2, response.getResults().size());

        // First image should have successful result
        assertEquals("apple", response.getResults().get(0).getFruitType());
        assertEquals("fresh", response.getResults().get(0).getFreshness());
        assertEquals(0.95, response.getResults().get(0).getConfidence());

        // Second image should have error response (empty response)
        FruitDetectionResponse errorResponse = response.getResults().get(1);
        assertNull(errorResponse.getFruitType());
        assertNull(errorResponse.getFreshness());
        assertEquals(0.0, errorResponse.getConfidence());
        assertEquals("badImage", errorResponse.getImageUrl()); // Should preserve the original data

        // Verify that the service was called twice
        verify(fruitDetectionService, times(2)).detectFruit(any());
    }
}