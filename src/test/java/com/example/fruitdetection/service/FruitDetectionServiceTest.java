package com.example.fruitdetection.service;

import com.example.fruitdetection.dto.DetectionBox;
import com.example.fruitdetection.dto.FruitDetectionRequest;
import com.example.fruitdetection.dto.FruitDetectionResponse;
import com.example.fruitdetection.service.impl.FruitDetectionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FruitDetectionServiceTest {

    private FruitDetectionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FruitDetectionServiceImpl();
    }

    @Test
    void testDetectWithNullRequest() {
        FruitDetectionRequest request = new FruitDetectionRequest();
        request.setImageUrl(null);
        request.setImageData(null);

        FruitDetectionResponse response = service.detectFruit(request);

        assertNotNull(response);
        assertEquals("apple", response.getFruitType());
        assertEquals("fresh", response.getFreshness());
        assertTrue(response.getConfidence() > 0);
    }

    @Test
    void testDetectWithEmptyImageUrl() {
        FruitDetectionRequest request = new FruitDetectionRequest();
        request.setImageUrl("");

        FruitDetectionResponse response = service.detectFruit(request);

        assertNotNull(response);
        // 应该返回 mock 默认结果
        assertNotNull(response.getFruitType());
    }

    @Test
    void testDetectWithValidImageData() {
        FruitDetectionRequest request = new FruitDetectionRequest();
        // 使用简单的 base64 测试数据
        request.setImageData("data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAQABAAD/2wBDAAgGBgcGBQgHBwcJCQgKDBQNDAsLDBkSEw8UHRofHh0aHBwgJC4nICIsIxwcKCPbGRDZhQBQamRkYFBSUJCQgJ/wAARCABgAGADASIAAhEBAxEB/8QAFQABAQAAAAAAAAAAAAAAAAAAAAb/xAAUEAEAAAAAAAAAAAAAAAAAAAAA/8QAFQEBAQAAAAAAAAAAAAAAAAAAAAX/xAAUEQEAAAAAAAAAAAAAAAAAAAAA/9oADAMBQIRAxEAPwA/ABgA//9k=");

        FruitDetectionResponse response = service.detectFruit(request);

        assertNotNull(response);
        assertNotNull(response.getFruitType());
        assertNotNull(response.getFreshness());
        assertTrue(response.getConfidence() >= 0 && response.getConfidence() <= 1);
    }

    @Test
    void testDetectReturnsDetectionsList() {
        FruitDetectionRequest request = new FruitDetectionRequest();
        request.setImageUrl("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==");

        FruitDetectionResponse response = service.detectFruit(request);

        assertNotNull(response);
        List<DetectionBox> detections = response.getDetections();
        assertNotNull(detections);
        assertTrue(detections.size() > 0);

        DetectionBox firstBox = detections.get(0);
        assertNotNull(firstBox.getFruitType());
        assertNotNull(firstBox.getFreshness());
        assertTrue(firstBox.getX() >= 0 && firstBox.getX() <= 1);
        assertTrue(firstBox.getY() >= 0 && firstBox.getY() <= 1);
        assertTrue(firstBox.getWidth() >= 0 && firstBox.getWidth() <= 1);
        assertTrue(firstBox.getHeight() >= 0 && firstBox.getHeight() <= 1);
    }

    @Test
    void testDetectResponseStructure() {
        FruitDetectionRequest request = new FruitDetectionRequest();
        request.setImageUrl("test-image-data");

        FruitDetectionResponse response = service.detectFruit(request);

        // 验证响应结构完整性
        assertNotNull(response.getFruitType());
        assertNotNull(response.getFreshness());
        assertNotNull(response.getConfidence());
        assertNotNull(response.getDetections());

        // 验证置信度范围
        assertTrue(response.getConfidence() >= 0.0 && response.getConfidence() <= 1.0);
    }
}