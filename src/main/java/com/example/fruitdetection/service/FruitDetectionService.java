package com.example.fruitdetection.service;

import com.example.fruitdetection.dto.FruitDetectionRequest;
import com.example.fruitdetection.dto.FruitDetectionResponse;

public interface FruitDetectionService {
    FruitDetectionResponse detectFruit(FruitDetectionRequest request);
}