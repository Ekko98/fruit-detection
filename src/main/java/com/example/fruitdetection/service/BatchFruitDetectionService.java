package com.example.fruitdetection.service;

import com.example.fruitdetection.dto.BatchFruitDetectionRequest;
import com.example.fruitdetection.dto.BatchFruitDetectionResponse;

/**
 * 批量水果检测服务接口
 */
public interface BatchFruitDetectionService {
    /**
     * 批量检测水果
     * @param request 批量检测请求
     * @return 批量检测响应
     */
    BatchFruitDetectionResponse detectBatch(BatchFruitDetectionRequest request);
}