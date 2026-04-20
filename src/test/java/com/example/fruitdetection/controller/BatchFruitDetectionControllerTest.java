package com.example.fruitdetection.controller;

import com.example.fruitdetection.dto.BatchFruitDetectionRequest;
import com.example.fruitdetection.dto.BatchFruitDetectionResponse;
import com.example.fruitdetection.dto.FruitDetectionResponse;
import com.example.fruitdetection.service.BatchFruitDetectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(BatchFruitDetectionController.class)
class BatchFruitDetectionControllerTest {

    private MockMvc mockMvc;

    @Mock
    private BatchFruitDetectionService batchFruitDetectionService;

    @InjectMocks
    private BatchFruitDetectionController batchFruitDetectionController;

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

        mockMvc = MockMvcBuilders.standaloneSetup(batchFruitDetectionController).build();
    }

    @Test
    void testBatchDetect_Success() throws Exception {
        // Arrange
        BatchFruitDetectionRequest request = new BatchFruitDetectionRequest();
        request.setImageDataList(testImageDataList);

        BatchFruitDetectionResponse expectedResponse = new BatchFruitDetectionResponse();
        expectedResponse.setResults(List.of(mockSingleResponse, mockSingleResponse));

        when(batchFruitDetectionService.detectBatch(any(BatchFruitDetectionRequest.class)))
                .thenReturn(expectedResponse);

        // Act & Assert
        mockMvc.perform(post("/api/batch-detect")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"imageDataList\":[\"testBase64Image1\",\"testBase64Image2\"]}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results", hasSize(2)))
                .andExpect(jsonPath("$.results[0].fruitType", is("apple")))
                .andExpect(jsonPath("$.results[0].freshness", is("fresh")))
                .andExpect(jsonPath("$.results[0].confidence", is(0.95)))
                .andExpect(jsonPath("$.results[1].fruitType", is("apple")))
                .andExpect(jsonPath("$.results[1].freshness", is("fresh")))
                .andExpect(jsonPath("$.results[1].confidence", is(0.95)));

        // Verify
        verify(batchFruitDetectionService, times(1)).detectBatch(any(BatchFruitDetectionRequest.class));
    }

    @Test
    void testBatchDetect_EmptyList() throws Exception {
        // @Size has no min=1, so empty list passes validation and reaches service
        BatchFruitDetectionResponse emptyResponse = new BatchFruitDetectionResponse();
        when(batchFruitDetectionService.detectBatch(any(BatchFruitDetectionRequest.class)))
                .thenReturn(emptyResponse);

        mockMvc.perform(post("/api/batch-detect")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"imageDataList\":[]}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results", hasSize(0)));
    }

    @Test
    void testBatchDetect_NullRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/batch-detect")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest()); // Should fail validation for null imageDataList
    }

    @Test
    void testBatchDetect_ExceedsLimit() throws Exception {
        // DTO @Size(max=10) blocks requests with >10 images at validation level → returns 400
        mockMvc.perform(post("/api/batch-detect")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"imageDataList\":[\"img0\",\"img1\",\"img2\",\"img3\",\"img4\",\"img5\",\"img6\",\"img7\",\"img8\",\"img9\",\"img10\",\"img11\",\"img12\",\"img13\",\"img14\"]}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}