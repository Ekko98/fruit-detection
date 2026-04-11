package com.example.fruitdetection.dto;

import lombok.Data;

@Data
public class DetectionBox {
    private String fruitType;
    private String freshness;
    private double confidence;
    private double x;
    private double y;
    private double width;
    private double height;
}
