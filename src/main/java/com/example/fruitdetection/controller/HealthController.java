package com.example.fruitdetection.controller;

import com.example.fruitdetection.dto.BaseResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public BaseResponse<String> healthCheck() {
        return new BaseResponse<>("ok");
    }
}