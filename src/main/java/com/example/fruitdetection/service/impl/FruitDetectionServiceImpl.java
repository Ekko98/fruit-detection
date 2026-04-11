package com.example.fruitdetection.service.impl;

import com.example.fruitdetection.dto.FruitDetectionRequest;
import com.example.fruitdetection.dto.FruitDetectionResponse;
import com.example.fruitdetection.service.FruitDetectionService;
import com.example.fruitdetection.utils.OpenCVUtils;
import org.springframework.stereotype.Service;

@Service
public class FruitDetectionServiceImpl implements FruitDetectionService {

    @Override
    public FruitDetectionResponse detectFruit(FruitDetectionRequest request) {
        // Mock 实现，实际项目中会调用 YOLOv8 模型
        // YOLO 标准格式：相对坐标 (0-1范围)
        FruitDetectionResponse response = new FruitDetectionResponse();
        response.setFruitType("apple");
        response.setFreshness("fresh");
        response.setConfidence(0.95);
        response.setX(0.2);  // 相对x坐标 (20%)
        response.setY(0.3);  // 相对y坐标 (30%)
        response.setWidth(0.4);  // 相对宽度 (40%)
        response.setHeight(0.4);  // 相对高度 (40%)

        // 使用OpenCV处理图片并添加检测框
        String annotatedImage = OpenCVUtils.addDetectionBoxToImage(
            request.getImageData(),
            response.getFruitType(),
            response.getFreshness(),
            response.getConfidence(),
            response.getX(),
            response.getY(),
            response.getWidth(),
            response.getHeight()
        );

        response.setAnnotatedImage(annotatedImage);
        return response;
    }
}