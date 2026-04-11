package com.example.fruitdetection.dto;

import lombok.Data;

@Data
public class FruitDetectionRequest {
    private String imageUrl; // 图片URL或base64编码
    private String imageData; // 图片数据（base64）
}