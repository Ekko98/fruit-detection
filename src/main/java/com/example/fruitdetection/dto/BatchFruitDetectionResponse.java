package com.example.fruitdetection.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class BatchFruitDetectionResponse {
    private List<FruitDetectionResponse> results = new ArrayList<>();
}