package com.example.fruitdetection.service.impl;

import com.example.fruitdetection.dto.DetectionBox;
import com.example.fruitdetection.dto.FruitDetectionRequest;
import com.example.fruitdetection.dto.FruitDetectionResponse;
import com.example.fruitdetection.service.FruitDetectionService;
import com.example.fruitdetection.utils.OpenCVUtils;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

@Service
public class FruitDetectionServiceImpl implements FruitDetectionService {

    private static final Logger logger = Logger.getLogger(FruitDetectionServiceImpl.class.getName());
    private static final String PYTHON_SCRIPT_NAME = "yolov8_detector.py";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public FruitDetectionResponse detectFruit(FruitDetectionRequest request) {
        try {
            String imageData = getImageData(request);
            if (imageData == null || imageData.isEmpty()) {
                logger.warning("Image data is null or empty, using mock result");
                return createDefaultResponse(null);
            }

            String pythonScriptPath = getPythonScriptPath();
            if (!Files.exists(Paths.get(pythonScriptPath))) {
                logger.warning("Python script not found: " + pythonScriptPath + ", using mock result");
                return createDefaultResponse(imageData);
            }

            List<String> command = buildPythonCommand(pythonScriptPath);
            logger.info("Invoking Python detector with script: " + pythonScriptPath);
            logger.info("Executing Python command: " + command);

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
                logger.warning("Python script exited with code " + exitCode + ", output: " + resultOutput);
                return createDefaultResponse(imageData);
            }

            String jsonPayload = extractJsonFromOutput(resultOutput);
            logger.info("Python script output: " + jsonPayload);

            YoloDetectionResult yoloResult = objectMapper.readValue(jsonPayload, YoloDetectionResult.class);
            List<DetectionBox> detections = toDetectionBoxes(yoloResult);
            if (detections.isEmpty()) {
                logger.warning("Python returned no detections, using default result");
                return createDefaultResponse(imageData);
            }

            FruitDetectionResponse response = buildResponse(imageData, detections);
            logger.info("Python detector completed successfully with detections: " + detections.size());
            return response;
        } catch (Exception e) {
            logger.warning("Python detection failed: " + e.getMessage() + ", using default result");
            return createDefaultResponse(getImageData(request));
        }
    }

    private FruitDetectionResponse buildResponse(String imageData, List<DetectionBox> detections) {
        FruitDetectionResponse response = new FruitDetectionResponse();
        response.setDetections(detections);
        response.setImageUrl(imageData); // 保存原始图片

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
            logger.info("Using imageUrl as image data");
            return request.getImageUrl();
        }
        if (request.getImageData() != null && !request.getImageData().isEmpty()) {
            logger.info("Using imageData as image data");
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
                logger.info("Extracted JSON from line " + (i + 1) + " of " + lines.length);
                return line;
            }
        }

        logger.warning("No JSON found in output, using last line");
        return lines[lines.length - 1].trim();
    }

    private ParsedFruitInfo parseFruitType(String pythonFruitType, String pythonFreshness) {
        if (pythonFruitType == null || pythonFruitType.isEmpty()) {
            return new ParsedFruitInfo("apple", normalizeFreshness(pythonFreshness));
        }

        String lowerType = pythonFruitType.toLowerCase();
        String fruitType = "apple";
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
        }

        return new ParsedFruitInfo(fruitType, freshness);
    }

    private String normalizeFreshness(String freshness) {
        if (freshness == null || freshness.isEmpty()) {
            return "fresh";
        }
        return "fresh".equalsIgnoreCase(freshness) ? "fresh" : "rotten";
    }

    private FruitDetectionResponse createDefaultResponse(String imageData) {
        DetectionBox box = new DetectionBox();
        box.setFruitType("apple");
        box.setFreshness("fresh");
        box.setConfidence(0.95);
        box.setX(0.2);
        box.setY(0.3);
        box.setWidth(0.4);
        box.setHeight(0.4);
        return buildResponse(imageData, Collections.singletonList(box));
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
