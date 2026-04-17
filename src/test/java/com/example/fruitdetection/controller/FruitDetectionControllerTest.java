package com.example.fruitdetection.controller;

import com.example.fruitdetection.dto.BaseResponse;
import com.example.fruitdetection.dto.DetectionBox;
import com.example.fruitdetection.dto.FruitDetectionRequest;
import com.example.fruitdetection.dto.FruitDetectionResponse;
import com.example.fruitdetection.service.FruitDetectionService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FruitDetectionController.class)
class FruitDetectionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FruitDetectionService fruitDetectionService;

    @Test
    void testDetectEndpointWithValidRequest() throws Exception {
        // 准备 mock 响应
        FruitDetectionResponse mockResponse = new FruitDetectionResponse();
        mockResponse.setFruitType("apple");
        mockResponse.setFreshness("fresh");
        mockResponse.setConfidence(0.95);

        DetectionBox box = new DetectionBox();
        box.setFruitType("apple");
        box.setFreshness("fresh");
        box.setConfidence(0.95);
        box.setX(0.2);
        box.setY(0.3);
        box.setWidth(0.4);
        box.setHeight(0.4);
        mockResponse.setDetections(Collections.singletonList(box));

        when(fruitDetectionService.detectFruit(any(FruitDetectionRequest.class)))
                .thenReturn(mockResponse);

        // 发送请求并验证响应
        mockMvc.perform(post("/api/detect")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"imageUrl\":\"data:image/jpeg;base64,test\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fruitType").value("apple"))
                .andExpect(jsonPath("$.data.freshness").value("fresh"))
                .andExpect(jsonPath("$.data.confidence").value(0.95));
    }

    @Test
    void testDetectEndpointWithEmptyRequest() throws Exception {
        // 发送空请求
        mockMvc.perform(post("/api/detect")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void testDetectEndpointWithImageData() throws Exception {
        // 准备 mock 响应
        FruitDetectionResponse mockResponse = new FruitDetectionResponse();
        mockResponse.setFruitType("banana");
        mockResponse.setFreshness("rotten");
        mockResponse.setConfidence(0.88);

        when(fruitDetectionService.detectFruit(any(FruitDetectionRequest.class)))
                .thenReturn(mockResponse);

        // 发送 imageData 请求
        mockMvc.perform(post("/api/detect")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"imageData\":\"data:image/png;base64,testdata\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fruitType").value("banana"))
                .andExpect(jsonPath("$.data.freshness").value("rotten"));
    }
}