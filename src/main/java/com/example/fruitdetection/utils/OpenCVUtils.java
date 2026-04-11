package com.example.fruitdetection.utils;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.util.Base64;

public class OpenCVUtils {

    static {
        // 加载OpenCV native库
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    public static String addDetectionBoxToImage(String base64Image, String fruitType, String freshness, double confidence, double x, double y, double width, double height) {
        try {
            // 解码base64图片
            byte[] imageBytes = Base64.getDecoder().decode(base64Image.split(",")[1]);
            Mat image = Imgcodecs.imdecode(new MatOfByte(imageBytes), Imgcodecs.IMREAD_COLOR);

            if (image.empty()) {
                return base64Image; // 如果解码失败，返回原始图片
            }

            // 转换为RGB格式
            Mat rgbImage = new Mat();
            Imgproc.cvtColor(image, rgbImage, Imgproc.COLOR_BGR2RGB);

            // 计算实际像素坐标
            int imgWidth = rgbImage.width();
            int imgHeight = rgbImage.height();

            int boxX = (int)(x * imgWidth);
            int boxY = (int)(y * imgHeight);
            int boxWidth = (int)(width * imgWidth);
            int boxHeight = (int)(height * imgHeight);

            // 绘制检测框
            Scalar boxColor = new Scalar(255, 107, 107); // 红色
            int lineWidth = 2;

            // 绘制矩形框
            Imgproc.rectangle(rgbImage,
                new Point(boxX, boxY),
                new Point(boxX + boxWidth, boxY + boxHeight),
                boxColor,
                lineWidth);

            // 添加标签背景
            int labelHeight = 25;
            int labelWidth = 120;
            Imgproc.rectangle(rgbImage,
                new Point(boxX, boxY - labelHeight),
                new Point(boxX + labelWidth, boxY),
                boxColor,
                -1); // 填充

            // 添加文字
            String label = fruitType + " - " + (freshness.equals("fresh") ? "新鲜" : "腐烂");
            Scalar textColor = new Scalar(255, 255, 255); // 白色
            int fontSize = 0.6;
            int thickness = 1;

            Imgproc.putText(rgbImage,
                label,
                new Point(boxX + 5, boxY - 8),
                Imgproc.FONT_HERSHEY_SIMPLEX,
                fontSize,
                textColor,
                thickness);

            // 转换回BGR格式
            Imgproc.cvtColor(rgbImage, image, Imgproc.COLOR_RGB2BGR);

            // 编码为base64
            MatOfByte matOfByte = new MatOfByte();
            Imgcodecs.imencode(".jpg", image, matOfByte);
            byte[] byteArray = matOfByte.toArray();
            String resultBase64 = "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(byteArray);

            return resultBase64;

        } catch (Exception e) {
            e.printStackTrace();
            return base64Image; // 如果处理失败，返回原始图片
        }
    }
}