package com.example.fruitdetection.controller;

import com.example.fruitdetection.dto.BatchFruitDetectionRequest;
import com.example.fruitdetection.dto.BatchFruitDetectionResponse;
import com.example.fruitdetection.service.BatchFruitDetectionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@RestController
@RequestMapping("/api")
@Slf4j
@Validated
public class BatchFruitDetectionController {

    @Autowired
    private BatchFruitDetectionService batchFruitDetectionService;

    /**
     * 批量水果检测端点
     * @param request 批量检测请求，包含base64编码的图片数据列表
     * @return 批量检测响应
     */
    @PostMapping("/batch-detect")
    public ResponseEntity<BatchFruitDetectionResponse> batchDetect(
            @Valid @RequestBody BatchFruitDetectionRequest request) {
        log.info("Received batch detection request with {} images",
                request.getImageDataList() != null ? request.getImageDataList().size() : 0);

        BatchFruitDetectionResponse response = batchFruitDetectionService.detectBatch(request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}