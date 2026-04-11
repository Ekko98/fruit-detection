package com.example.fruitdetection.dto;

import lombok.Data;

@Data
public class FruitDetectionResponse {
    private String fruitType; // 水果类型
    private String freshness; // 新鲜度（fresh/rotten）
    private double confidence; // 置信度
    private String annotatedImage; // 带标注的图片（base64）
}