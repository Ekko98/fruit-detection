package com.example.fruitdetection.service.impl;

import com.example.fruitdetection.dto.DetectionBox;
import com.example.fruitdetection.dto.FruitDetectionRequest;
import com.example.fruitdetection.dto.FruitDetectionResponse;
import com.example.fruitdetection.service.FruitDetectionService;
import com.example.fruitdetection.utils.OpenCVUtils;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;

@Service
public class FruitDetectionServiceImpl implements FruitDetectionService {

    private static final Logger logger = Logger.getLogger(FruitDetectionServiceImpl.class.getName());
    private static final String PYTHON_SCRIPT_NAME = "yolov8_detector.py";
    private static final String PYTHON_SERVICE_URL = "http://127.0.0.1:5005/detect";
    private static final String PYTHON_HEALTH_URL = "http://127.0.0.1:5005/health";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    // 是否使用Python常驻服务模式（更高效）
    @Value("${fruit.detection.use-service:true}")
    private boolean usePythonService;

    // Python服务是否可用（缓存状态）
    private volatile Boolean pythonServiceAvailable = null;
    private volatile long lastHealthCheckTime = 0;
    private static final long HEALTH_CHECK_INTERVAL = 30000; // 30秒检查一次

    @Override
    public FruitDetectionResponse detectFruit(FruitDetectionRequest request) {
        try {
            String imageData = getImageData(request);
            if (imageData == null || imageData.isEmpty()) {
                logger.warning("Image data is null or empty");
                return createEmptyResponse(null);
            }

            // 优先使用Python常驻服务（更快）
            if (usePythonService && checkPythonServiceAvailable()) {
                return detectViaService(imageData);
            }

            // 降级：使用传统进程调用方式
            logger.info("Using fallback process-based detection");
            return detectViaProcess(imageData);

        } catch (Exception e) {
            logger.warning("Detection failed: " + e.getMessage());
            return createEmptyResponse(getImageData(request));
        }
    }

    /**
     * 检查Python服务是否可用
     */
    private boolean checkPythonServiceAvailable() {
        long now = System.currentTimeMillis();

        // 缓存有效期内直接返回
        if (pythonServiceAvailable != null && (now - lastHealthCheckTime) < HEALTH_CHECK_INTERVAL) {
            return pythonServiceAvailable;
        }

        // 执行健康检查
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>("{}", headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                PYTHON_HEALTH_URL,
                HttpMethod.GET,
                entity,
                Map.class
            );

            boolean available = response.getStatusCode() == HttpStatus.OK &&
                response.getBody() != null &&
                "ok".equals(response.getBody().get("status"));

            pythonServiceAvailable = available;
            lastHealthCheckTime = now;

            if (available) {
                logger.info("Python service is available");
            } else {
                logger.warning("Python service health check returned non-ok status");
            }

            return available;

        } catch (Exception e) {
            logger.warning("Python service not available: " + e.getMessage());
            pythonServiceAvailable = false;
            lastHealthCheckTime = now;
            return false;
        }
    }

    /**
     * 通过HTTP服务调用Python检测（快速方式）
     */
    private FruitDetectionResponse detectViaService(String imageData) {
        try {
            logger.info("Calling Python detection service...");

            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("imageUrl", imageData);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                PYTHON_SERVICE_URL,
                HttpMethod.POST,
                entity,
                Map.class
            );

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                logger.warning("Python service returned non-OK status: " + response.getStatusCode());
                return createEmptyResponse(imageData);
            }

            YoloDetectionResult yoloResult = objectMapper.convertValue(response.getBody(), YoloDetectionResult.class);
            List<DetectionBox> detections = toDetectionBoxes(yoloResult);

            // 无检测结果时返回空响应
            if (detections.isEmpty()) {
                logger.info("No fruit detected in image");
                return createEmptyResponse(imageData);
            }

            FruitDetectionResponse fruitResponse = buildResponse(imageData, detections);
            logger.info("Python service detection completed with " + detections.size() + " detections");
            return fruitResponse;

        } catch (Exception e) {
            logger.warning("Python service call failed: " + e.getMessage());
            // 服务调用失败，标记为不可用
            pythonServiceAvailable = false;
            // 降级到进程方式
            return detectViaProcess(imageData);
        }
    }

    /**
     * 通过启动Python进程调用检测（传统方式，较慢）
     */
    private FruitDetectionResponse detectViaProcess(String imageData) {
        try {
            String pythonScriptPath = getPythonScriptPath();
            if (!Files.exists(Paths.get(pythonScriptPath))) {
                logger.warning("Python script not found: " + pythonScriptPath);
                return createEmptyResponse(imageData);
            }

            List<String> command = buildPythonCommand(pythonScriptPath);
            logger.info("Invoking Python detector (process mode) with script: " + pythonScriptPath);

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            try (OutputStream outputStream = process.getOutputStream()) {
                outputStream.write(imageData.getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
            }

            String resultOutput;
            try (InputStream inputStream = process.getInputStream()) {
                resultOutput = StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                logger.warning("Python script exited with code " + exitCode);
                return createEmptyResponse(imageData);
            }

            String jsonPayload = extractJsonFromOutput(resultOutput);
            YoloDetectionResult yoloResult = objectMapper.readValue(jsonPayload, YoloDetectionResult.class);
            List<DetectionBox> detections = toDetectionBoxes(yoloResult);

            // 无检测结果时返回空响应
            if (detections.isEmpty()) {
                logger.info("No fruit detected in image (process mode)");
                return createEmptyResponse(imageData);
            }

            return buildResponse(imageData, detections);

        } catch (Exception e) {
            logger.warning("Python process detection failed: " + e.getMessage());
            return createEmptyResponse(imageData);
        }
    }

    private FruitDetectionResponse buildResponse(String imageData, List<DetectionBox> detections) {
        FruitDetectionResponse response = new FruitDetectionResponse();
        response.setDetections(detections);
        response.setImageUrl(imageData);

        if (detections.isEmpty()) {
            return createEmptyResponse(imageData);
        }

        DetectionBox primary = detections.get(0);
        response.setFruitType(primary.getFruitType());
        response.setFreshness(primary.getFreshness());
        response.setConfidence(primary.getConfidence());
        response.setX(primary.getX());
        response.setY(primary.getY());
        response.setWidth(primary.getWidth());
        response.setHeight(primary.getHeight());
        response.setAnnotatedImage(OpenCVUtils.addDetectionBoxesToImage(imageData, detections));
        return response;
    }

    private List<DetectionBox> toDetectionBoxes(YoloDetectionResult result) {
        if (result == null) {
            return Collections.emptyList();
        }

        List<DetectionBox> boxes = new ArrayList<>();
        if (result.getDetections() != null && !result.getDetections().isEmpty()) {
            for (YoloDetectionItem item : result.getDetections()) {
                boxes.add(toDetectionBox(item));
            }
            return boxes;
        }

        if (result.getFruitType() == null || result.getFruitType().isEmpty()) {
            return boxes;
        }

        YoloDetectionItem single = new YoloDetectionItem();
        single.setFruitType(result.getFruitType());
        single.setFreshness(result.getFreshness());
        single.setConfidence(result.getConfidence());
        single.setX(result.getX());
        single.setY(result.getY());
        single.setWidth(result.getWidth());
        single.setHeight(result.getHeight());
        boxes.add(toDetectionBox(single));
        return boxes;
    }

    private DetectionBox toDetectionBox(YoloDetectionItem item) {
        ParsedFruitInfo parsedInfo = parseFruitType(item.getFruitType(), item.getFreshness());
        DetectionBox box = new DetectionBox();
        box.setFruitType(parsedInfo.fruitType);
        box.setFreshness(parsedInfo.freshness);
        box.setConfidence(item.getConfidence());
        box.setX(item.getX());
        box.setY(item.getY());
        box.setWidth(item.getWidth());
        box.setHeight(item.getHeight());
        return box;
    }

    private String getImageData(FruitDetectionRequest request) {
        if (request.getImageUrl() != null && !request.getImageUrl().isEmpty()) {
            return request.getImageUrl();
        }
        if (request.getImageData() != null && !request.getImageData().isEmpty()) {
            return request.getImageData();
        }
        return null;
    }

    private String getPythonScriptPath() {
        String[] possiblePaths = {
            "src/main/resources/python/" + PYTHON_SCRIPT_NAME,
            "target/classes/python/" + PYTHON_SCRIPT_NAME
        };

        String projectDir = System.getProperty("user.dir");
        for (String path : possiblePaths) {
            String fullPath = Paths.get(projectDir, path).toAbsolutePath().toString();
            if (Files.exists(Paths.get(fullPath))) {
                return fullPath;
            }
        }
        return Paths.get(projectDir, possiblePaths[0]).toAbsolutePath().toString();
    }

    private List<String> buildPythonCommand(String scriptPath) {
        List<String> command = new ArrayList<>();
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            if (isCommandAvailable("py")) {
                command.add("py");
            } else if (isCommandAvailable("python")) {
                command.add("python");
            } else {
                command.add("python3");
            }
        } else {
            if (isCommandAvailable("python3")) {
                command.add("python3");
            } else {
                command.add("python");
            }
        }

        command.add(scriptPath);
        return command;
    }

    private boolean isCommandAvailable(String command) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command, "--version");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private String extractJsonFromOutput(String output) {
        if (output == null || output.isEmpty()) {
            return "{}";
        }

        String[] lines = output.trim().split("\\r?\\n");
        for (int i = lines.length - 1; i >= 0; i--) {
            String line = lines[i].trim();
            if (line.startsWith("{") && line.endsWith("}")) {
                return line;
            }
        }
        return lines[lines.length - 1].trim();
    }

    private ParsedFruitInfo parseFruitType(String pythonFruitType, String pythonFreshness) {
        // 无水果类型时返回空信息
        if (pythonFruitType == null || pythonFruitType.isEmpty()) {
            return new ParsedFruitInfo(null, null);
        }

        String lowerType = pythonFruitType.toLowerCase();
        String fruitType = null;
        String freshness = normalizeFreshness(pythonFreshness);

        if (lowerType.startsWith("bad")) {
            freshness = "rotten";
        } else if (lowerType.startsWith("good")) {
            freshness = "fresh";
        }

        if (lowerType.contains("banana")) {
            fruitType = "banana";
        } else if (lowerType.contains("orange")) {
            fruitType = "orange";
        } else if (lowerType.contains("apple")) {
            fruitType = "apple";
        } else {
            // 其他类型，保留原始类型名称
            fruitType = pythonFruitType;
        }

        return new ParsedFruitInfo(fruitType, freshness);
    }

    private String normalizeFreshness(String freshness) {
        if (freshness == null || freshness.isEmpty()) {
            return null;
        }
        return "fresh".equalsIgnoreCase(freshness) ? "fresh" : "rotten";
    }

    /**
     * 创建无检测结果（不返回mock数据）
     */
    private FruitDetectionResponse createEmptyResponse(String imageData) {
        FruitDetectionResponse response = new FruitDetectionResponse();
        response.setDetections(Collections.emptyList());
        response.setImageUrl(imageData);
        response.setFruitType(null);
        response.setFreshness(null);
        response.setConfidence(0);
        response.setX(0);
        response.setY(0);
        response.setWidth(0);
        response.setHeight(0);
        response.setAnnotatedImage(imageData); // 返回原图
        logger.info("No fruit detected, returning empty response");
        return response;
    }

    private static class ParsedFruitInfo {
        private final String fruitType;
        private final String freshness;

        private ParsedFruitInfo(String fruitType, String freshness) {
            this.fruitType = fruitType;
            this.freshness = freshness;
        }
    }

    private static class YoloDetectionResult {
        @JsonProperty("fruit_type")
        private String fruitType;
        @JsonProperty("freshness")
        private String freshness;
        @JsonProperty("confidence")
        private double confidence;
        @JsonProperty("x")
        private double x;
        @JsonProperty("y")
        private double y;
        @JsonProperty("width")
        private double width;
        @JsonProperty("height")
        private double height;
        @JsonProperty("detections")
        private List<YoloDetectionItem> detections;

        public String getFruitType() { return fruitType; }
        public void setFruitType(String fruitType) { this.fruitType = fruitType; }
        public String getFreshness() { return freshness; }
        public void setFreshness(String freshness) { this.freshness = freshness; }
        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
        public double getX() { return x; }
        public void setX(double x) { this.x = x; }
        public double getY() { return y; }
        public void setY(double y) { this.y = y; }
        public double getWidth() { return width; }
        public void setWidth(double width) { this.width = width; }
        public double getHeight() { return height; }
        public void setHeight(double height) { this.height = height; }
        public List<YoloDetectionItem> getDetections() { return detections; }
        public void setDetections(List<YoloDetectionItem> detections) { this.detections = detections; }
    }

    private static class YoloDetectionItem {
        @JsonProperty("fruit_type")
        private String fruitType;
        @JsonProperty("freshness")
        private String freshness;
        @JsonProperty("confidence")
        private double confidence;
        @JsonProperty("x")
        private double x;
        @JsonProperty("y")
        private double y;
        @JsonProperty("width")
        private double width;
        @JsonProperty("height")
        private double height;

        public String getFruitType() { return fruitType; }
        public void setFruitType(String fruitType) { this.fruitType = fruitType; }
        public String getFreshness() { return freshness; }
        public void setFreshness(String freshness) { this.freshness = freshness; }
        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
        public double getX() { return x; }
        public void setX(double x) { this.x = x; }
        public double getY() { return y; }
        public void setY(double y) { this.y = y; }
        public double getWidth() { return width; }
        public void setWidth(double width) { this.width = width; }
        public double getHeight() { return height; }
        public void setHeight(double height) { this.height = height; }
    }
}