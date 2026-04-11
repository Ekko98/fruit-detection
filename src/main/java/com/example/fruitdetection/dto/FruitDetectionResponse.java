package com.example.fruitdetection.dto;

import lombok.Data;

@Data
public class FruitDetectionResponse {
    private String fruitType; // 水果类型
    private String freshness; // 新鲜度（fresh/rotten）
    private double confidence; // 置信度
    private double x; // 检测框x坐标（相对，0-1）
    private double y; // 检测框y坐标（相对，0-1）
    private double width; // 检测框宽度（相对，0-1）
    private double height; // 检测框高度（相对，0-1）
}