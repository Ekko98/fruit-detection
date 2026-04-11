package com.example.fruitdetection.utils;

import com.example.fruitdetection.dto.DetectionBox;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

public class OpenCVUtils {
    private static final Logger logger = Logger.getLogger(OpenCVUtils.class.getName());

    static {
        try {
            System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
            logger.info("OpenCV native library loaded successfully: " + Core.NATIVE_LIBRARY_NAME);
        } catch (UnsatisfiedLinkError e) {
            logger.warning("Failed to load standard OpenCV library, trying direct file load...");
            try {
                String libraryPath = getOpenCVDllPath();
                logger.info("Loading OpenCV from: " + libraryPath);
                System.load(libraryPath);
                logger.info("OpenCV native library loaded successfully from file");
            } catch (Exception e2) {
                logger.severe("Failed to load OpenCV native library: " + e2.getMessage());
                logger.severe("Library path: " + System.getProperty("java.library.path"));
                throw new RuntimeException("Failed to load OpenCV native library", e2);
            }
        } catch (Exception e) {
            logger.severe("Error loading OpenCV library: " + e.getMessage());
            throw new RuntimeException("Error loading OpenCV library", e);
        }
    }

    private static String getOpenCVDllPath() {
        String[] possiblePaths = {
            "src/main/resources/opencv/opencv_java4120.dll",
            "target/classes/opencv/opencv_java4120.dll",
            "./opencv/opencv_java4120.dll"
        };

        for (String path : possiblePaths) {
            File file = new File(path);
            if (file.exists()) {
                return file.getAbsolutePath();
            }
        }

        String projectDir = System.getProperty("user.dir");
        return projectDir + "/target/classes/opencv/opencv_java4120.dll";
    }

    public static String addDetectionBoxToImage(String base64Image, String fruitType, String freshness, double confidence, double x, double y, double width, double height) {
        DetectionBox box = new DetectionBox();
        box.setFruitType(fruitType);
        box.setFreshness(freshness);
        box.setConfidence(confidence);
        box.setX(x);
        box.setY(y);
        box.setWidth(width);
        box.setHeight(height);
        return addDetectionBoxesToImage(base64Image, Collections.singletonList(box));
    }

    public static String addDetectionBoxesToImage(String base64Image, List<DetectionBox> detections) {
        if (base64Image == null || base64Image.isEmpty()) {
            logger.warning("Base64 image data is null or empty, returning empty string");
            return "";
        }

        if (detections == null || detections.isEmpty()) {
            logger.warning("Detection list is empty, returning original image");
            return base64Image;
        }

        try {
            String[] parts = base64Image.split(",", 2);
            if (parts.length < 2) {
                logger.warning("Invalid base64 image format, returning original");
                return base64Image;
            }

            byte[] imageBytes = Base64.getDecoder().decode(parts[1]);
            Mat image = Imgcodecs.imdecode(new MatOfByte(imageBytes), Imgcodecs.IMREAD_UNCHANGED);

            if (image.empty()) {
                logger.warning("Failed to decode image, returning original");
                return base64Image;
            }

            int channels = image.channels();
            logger.info("Image loaded - channels: " + channels + ", size: " + image.cols() + "x" + image.rows());

            if (channels < 1 || channels > 4) {
                logger.warning("Unsupported number of channels: " + channels + ", returning original image");
                return base64Image;
            }

            Mat annotatedImage = image.clone();
            int imgWidth = annotatedImage.cols();
            int imgHeight = annotatedImage.rows();
            Scalar boxColor = createBoxColor(channels);
            Scalar textColor = createTextColor(channels);

            for (DetectionBox detection : detections) {
                if (detection == null) {
                    continue;
                }

                int boxX = clamp((int) (detection.getX() * imgWidth), 0, Math.max(0, imgWidth - 1));
                int boxY = clamp((int) (detection.getY() * imgHeight), 0, Math.max(0, imgHeight - 1));
                int boxWidth = clamp((int) (detection.getWidth() * imgWidth), 1, Math.max(1, imgWidth - boxX));
                int boxHeight = clamp((int) (detection.getHeight() * imgHeight), 1, Math.max(1, imgHeight - boxY));

                Imgproc.rectangle(
                    annotatedImage,
                    new Point(boxX, boxY),
                    new Point(boxX + boxWidth, boxY + boxHeight),
                    boxColor,
                    2,
                    Imgproc.LINE_8,
                    0
                );

                String safeFruitType = detection.getFruitType() == null || detection.getFruitType().isBlank()
                    ? "unknown"
                    : detection.getFruitType();
                String safeFreshness = "fresh".equalsIgnoreCase(detection.getFreshness()) ? "fresh" : "rotten";
                String label = safeFruitType + " " + safeFreshness + String.format(" %.2f", detection.getConfidence());

                int labelHeight = 25;
                int labelWidth = Math.min(Math.max(140, label.length() * 10), Math.max(1, imgWidth - boxX));
                int labelTop = Math.max(0, boxY - labelHeight);

                Imgproc.rectangle(
                    annotatedImage,
                    new Point(boxX, labelTop),
                    new Point(boxX + labelWidth, boxY),
                    boxColor,
                    -1,
                    Imgproc.LINE_8,
                    0
                );

                Imgproc.putText(
                    annotatedImage,
                    label,
                    new Point(boxX + 5, Math.max(15, boxY - 8)),
                    Imgproc.FONT_HERSHEY_SIMPLEX,
                    0.55,
                    textColor,
                    1,
                    Imgproc.LINE_8,
                    false
                );
            }

            MatOfByte matOfByte = new MatOfByte();
            String extension = channels == 4 ? ".png" : ".jpg";
            String mimeType = channels == 4 ? "image/png" : "image/jpeg";
            boolean encoded = Imgcodecs.imencode(extension, annotatedImage, matOfByte);

            if (!encoded) {
                logger.warning("Failed to encode annotated image, returning original");
                return base64Image;
            }

            return "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(matOfByte.toArray());
        } catch (Exception e) {
            logger.warning("Failed to annotate image: " + e.getMessage());
            return base64Image;
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    private static Scalar createBoxColor(int channels) {
        if (channels == 1) {
            return new Scalar(220);
        }
        if (channels == 4) {
            return new Scalar(107, 107, 255, 255);
        }
        return new Scalar(107, 107, 255);
    }

    private static Scalar createTextColor(int channels) {
        if (channels == 1) {
            return new Scalar(255);
        }
        if (channels == 4) {
            return new Scalar(255, 255, 255, 255);
        }
        return new Scalar(255, 255, 255);
    }
}
