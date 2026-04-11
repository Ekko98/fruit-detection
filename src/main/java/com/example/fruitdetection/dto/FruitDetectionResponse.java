package com.example.fruitdetection.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class FruitDetectionResponse {
    private String fruitType;
    private String freshness;
    private double confidence;
    private double x;
    private double y;
    private double width;
    private double height;
    private String annotatedImage;
    private String imageUrl; // 原始图片（用于历史记录）
    private List<DetectionBox> detections = new ArrayList<>();
}
