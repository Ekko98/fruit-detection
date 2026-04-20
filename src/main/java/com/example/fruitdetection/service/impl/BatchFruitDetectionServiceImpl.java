package com.example.fruitdetection.service.impl;

import com.example.fruitdetection.dto.BatchFruitDetectionRequest;
import com.example.fruitdetection.dto.BatchFruitDetectionResponse;
import com.example.fruitdetection.dto.FruitDetectionRequest;
import com.example.fruitdetection.dto.FruitDetectionResponse;
import com.example.fruitdetection.service.BatchFruitDetectionService;
import com.example.fruitdetection.service.FruitDetectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Service
public class BatchFruitDetectionServiceImpl implements BatchFruitDetectionService {

    private static final Logger logger = Logger.getLogger(BatchFruitDetectionServiceImpl.class.getName());

    @Autowired
    private FruitDetectionService fruitDetectionService;

    @Override
    public BatchFruitDetectionResponse detectBatch(BatchFruitDetectionRequest request) {
        // 参数验证
        if (request == null || request.getImageDataList() == null) {
            logger.warning("Batch detection request is null or imageDataList is null");
            return new BatchFruitDetectionResponse();
        }

        List<String> imageDataList = request.getImageDataList();

        // 检查图片数量限制
        if (imageDataList.isEmpty()) {
            logger.info("Empty image list provided for batch detection");
            return new BatchFruitDetectionResponse();
        }

        if (imageDataList.size() > 10) {
            logger.warning("Batch detection exceeded limit of 10 images: " + imageDataList.size());
            // 返回部分结果或错误？这里选择处理前10张图片
            imageDataList = imageDataList.subList(0, 10);
        }

        List<FruitDetectionResponse> results = new ArrayList<>();

        for (int i = 0; i < imageDataList.size(); i++) {
            String imageData = imageDataList.get(i);
            try {
                // 创建单张检测请求
                FruitDetectionRequest singleRequest = new FruitDetectionRequest();
                singleRequest.setImageData(imageData);

                // 如果指定了useService，则设置（这里简化处理，实际可能需要修改FruitDetectionRequest）
                // 为简化实现，我们使用现有服务的默认行为
                if (request.getUseService() != null) {
                    // 注意：这里我们无法直接修改FruitDetectionService的行为
                    // 实际实现可能需要在FruitDetectionService中添加useService参数
                    // 但根据计划，我们不能修改现有代码，所以这里忽略useService参数
                    // 或者我们可以创建一个临时的包装服务
                    logger.info("useService parameter provided but cannot be applied without modifying existing code");
                }

                // 调用现有的单张检测服务
                FruitDetectionResponse singleResponse = fruitDetectionService.detectFruit(singleRequest);
                results.add(singleResponse);

            } catch (Exception e) {
                final int imageIndex = i;
                logger.warning(() -> "Error processing image at index " + imageIndex + ": " + e.getMessage());
                // 为失败的图片创建空响应
                FruitDetectionResponse errorResponse = new FruitDetectionResponse();
                errorResponse.setImageUrl(imageData); // 保留原始数据用于调试
                results.add(errorResponse);
            }
        }

        BatchFruitDetectionResponse batchResponse = new BatchFruitDetectionResponse();
        batchResponse.setResults(results);
        return batchResponse;
    }
}