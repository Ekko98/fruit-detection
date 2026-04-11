package com.example.fruitdetection.controller;

import com.example.fruitdetection.dto.BaseResponse;
import com.example.fruitdetection.dto.FruitDetectionRequest;
import com.example.fruitdetection.dto.FruitDetectionResponse;
import com.example.fruitdetection.service.FruitDetectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class FruitDetectionController {

    @Autowired
    private FruitDetectionService fruitDetectionService;

    @PostMapping("/detect")
    public BaseResponse<FruitDetectionResponse> detectFruit(@RequestBody FruitDetectionRequest request) {
        FruitDetectionResponse response = fruitDetectionService.detectFruit(request);
        return new BaseResponse<>(response);
    }
}