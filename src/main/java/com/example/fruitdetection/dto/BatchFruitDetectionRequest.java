package com.example.fruitdetection.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@Data
public class BatchFruitDetectionRequest {
    @NotNull(message = "imageDataList cannot be null")
    @Size(max = 10, message = "imageDataList cannot exceed 10 images")
    private List<String> imageDataList; // base64编码的图片数据列表
    private Boolean useService; // 可选：是否使用Python服务，默认使用配置值
}