# 水果腐烂检测系统

基于 Spring Boot + Vue + YOLOv8 的水果新鲜度智能检测系统。支持单张图片检测、批量检测和摄像头实时检测三种模式。

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Java 8 + Spring Boot 2.7.5 + Maven + OpenCV 4.5.1 |
| 前端 | Vue 2.6 + Vuex 3.6 + Vue Router 3.5 + Axios |
| 检测引擎 | Python 3 + YOLOv8 (Ultralytics) + Flask + OpenCV-Python |

## 项目结构

```
fruit-detection/
├── pom.xml                              # Maven 配置
├── package.json                         # Node.js 依赖
├── vue.config.js                        # Vue 开发服务器配置（HTTPS + 代理）
├── start.bat                            # Windows 一键启动脚本
├── server.crt / server.key              # 自签名 HTTPS 证书（用于摄像头权限）
│
├── src/                                 # Vue 前端源码
│   ├── main.js                          # 入口
│   ├── App.vue                          # 根组件
│   ├── router/index.js                  # 路由（仅首页）
│   ├── store/index.js                   # Vuex 状态管理
│   ├── http/axios.js                    # Axios 实例（baseURL=/api, timeout=60s）
│   ├── views/Home.vue                   # 主页面（图片/批量/实时三模式）
│   └── test.html                        # 调试页面
│
├── src/main/java/com/example/fruitdetection/
│   ├── FruitDetectionApplication.java   # 启动类
│   ├── config/CorsConfig.java           # 全局 CORS 配置
│   ├── controller/
│   │   ├── HealthController.java        # GET /api/health
│   │   ├── TestController.java          # GET /api/test
│   │   ├── FruitDetectionController.java    # POST /api/detect
│   │   └── BatchFruitDetectionController.java # POST /api/batch-detect
│   ├── service/
│   │   ├── FruitDetectionService.java           # 单张检测接口
│   │   ├── BatchFruitDetectionService.java      # 批量检测接口
│   │   └── impl/
│   │       ├── FruitDetectionServiceImpl.java   # 核心检测逻辑
│   │       └── BatchFruitDetectionServiceImpl.java # 批量编排（上限10张）
│   ├── dto/
│   │   ├── BaseResponse.java                    # 统一响应包装 {code,message,data}
│   │   ├── FruitDetectionRequest.java           # imageUrl / imageData
│   │   ├── FruitDetectionResponse.java          # 水果类型+新鲜度+置信度+检测框列表
│   │   ├── DetectionBox.java                    # 单框坐标与属性
│   │   ├── BatchFruitDetectionRequest.java      # imageDataList (max 10)
│   │   └── BatchFruitDetectionResponse.java     # results 列表
│   ├── exception/GlobalExceptionHandler.java    # 全局异常捕获
│   └── utils/OpenCVUtils.java                   # base64 解码 + 绘制检测框
│
├── src/main/resources/
│   ├── application.properties           # 端口8080、上传限制10MB、检测模式开关
│   ├── opencv/opencv_java4120.dll     # Windows OpenCV native 库
│   └── python/                          # Python 检测服务
│       ├── fruit_detection_service.py   # Flask 常驻服务（端口5005，模型预加载）
│       ├── yolov8_detector.py           # 命令行调用脚本（进程模式）
│       ├── test_detector.py             # 环境自检脚本
│       ├── download_model.py            # 下载 yolov8n.pt
│       ├── requirements.txt             # Python 依赖
│       └── models/
│           ├── last.pt                  # 训练好的水果检测模型（YOLOv8）
│           └── yolov8n.pt               # YOLOv8n 预训练权重
│
├── src/test/java/                       # Java 单元测试（JUnit 5 + Mockito）
│   └── com/example/fruitdetection/
│       ├── controller/
│       │   ├── FruitDetectionControllerTest.java
│       │   └── BatchFruitDetectionControllerTest.java
│       ├── service/
│       │   └── FruitDetectionServiceTest.java
│       └── service/impl/
│           └── BatchFruitDetectionServiceImplTest.java
│
├── tests/unit/Home.spec.js              # 前端单元测试（Jest + Vue Test Utils）
└── dist/                                # 前端构建产物
```

## 核心功能

- **单张图片检测**：上传 JPG/PNG，返回水果类型（apple/banana/orange）、新鲜度（fresh/rotten）、置信度、检测框坐标；图片上叠加 OpenCV 绘制的高亮框
- **批量检测**：一次上传最多 10 张图片，逐张调用检测服务，返回结果列表与汇总统计
- **实时检测**：调用摄像头（需 HTTPS），每秒截取一帧发送至后端检测，叠加动态检测框与扫描线动画
- **历史记录**：前端本地保存最近 10 条有效检测结果，含缩略图、时间戳、统计

## 检测引擎双模式

后端 `FruitDetectionServiceImpl` 支持两种调用 Python 的方式，通过 `fruit.detection.use-service` 切换：

| 模式 | 机制 | 速度 | 适用场景 |
|------|------|------|----------|
| **Flask 服务**（默认）| HTTP POST 到 `127.0.0.1:5005/detect`，模型启动时预加载 | ~0.2-0.5s | 生产推荐 |
| **进程调用** | 启动 `py yolov8_detector.py`，通过 stdin/stdout 传图收结果 | ~2-3s | 服务未启动时的降级 |

服务健康检查每 30 秒缓存一次；服务不可用时自动降级到进程模式。

## API 接口

### 健康检查
```
GET /api/health
响应: { "code": 200, "message": "success", "data": "ok" }
```

### 单张检测
```
POST /api/detect
请求体: { "imageUrl": "data:image/jpeg;base64,/9j/..." }

响应:
{
  "code": 200,
  "message": "success",
  "data": {
    "fruitType": "apple",
    "freshness": "fresh",
    "confidence": 0.95,
    "x": 0.2, "y": 0.3, "width": 0.4, "height": 0.4,
    "annotatedImage": "data:image/jpeg;base64,...",
    "detections": [
      { "fruitType": "apple", "freshness": "fresh", "confidence": 0.95, "x": 0.2, "y": 0.3, "width": 0.4, "height": 0.4 }
    ]
  }
}
```

### 批量检测
```
POST /api/batch-detect
请求体: { "imageDataList": ["base64...", "base64..."] }

响应:
{
  "results": [
    { "fruitType": "apple", "freshness": "fresh", ... },
    ...
  ]
}
```

### 无水果检测结果
```json
{
  "code": 200,
  "data": {
    "fruitType": null,
    "freshness": null,
    "confidence": 0,
    "detections": []
  }
}
```

## 快速启动

### 前置依赖
- JDK 8+
- Node.js 16+
- Python 3.8+（安装 `requirements.txt` 中的依赖）
- 已训练好的模型文件 `src/main/resources/python/models/last.pt`

### 方式一：一键启动（Windows）
```bat
start.bat
```
依次启动 Python 服务（8秒等待）→ Java 后端（15秒等待）→ Vue 前端。最终访问 https://localhost:3000

### 方式二：手动分别启动
```bash
# 1. 启动 Python Flask 检测服务（模型预加载约10秒）
cd src/main/resources/python
py fruit_detection_service.py

# 2. 启动 Java 后端
mvn spring-boot:run

# 3. 启动 Vue 前端开发服务器
npm run serve
```

### 服务端口

| 服务 | 端口 | 说明 |
|------|------|------|
| Python 检测服务 | 5005 | YOLOv8 模型推理 |
| Java 后端 | 8080 | REST API |
| Vue 前端 | 3000 (HTTPS) | Web 界面，代理 /api 到 8080 |

### 摄像头实时检测
浏览器要求页面在 HTTPS 或 localhost 下才能请求摄像头权限。项目已自带 `server.crt/server.key`，`vue.config.js` 会自动检测证书并启用 HTTPS。首次使用：
1. 访问 `https://<局域网IP>:3000`
2. 浏览器提示不安全时点击"高级"→"继续访问"
3. 授权摄像头权限即可使用实时检测

## 配置项

`src/main/resources/application.properties`：
```properties
# 检测模式：true=Flask HTTP服务（快），false=进程调用（慢）
fruit.detection.use-service=true

# 文件上传限制
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB
```

## 测试

```bash
# Java 单元测试
mvn test

# 前端单元测试
npm run test:unit

# Python 环境自检
py src/main/resources/python/test_detector.py
```

## 开发注意事项

1. **启动顺序**：必须先启动 Python Flask 服务（模型加载约 10 秒），再启动 Java 后端，否则 Java 会降级到进程模式
2. **模型类别命名**：YOLO 模型中的类别名应以 `good` 或 `bad` 开头（如 `good_apple`、`bad_banana`），后端据此解析新鲜度
3. **前端测试与实际代码差异**：`tests/unit/Home.spec.js` 中的部分断言与当前三模式实现不完全对齐，如需严格测试建议同步更新
4. **OpenCV native 库**：Windows 环境下 `OpenCVUtils` 会自动尝试加载 `src/main/resources/opencv/opencv_java4120.dll`

## 许可证

本项目使用 Ultralytics YOLOv8（AGPL-3.0 许可证），模型文件 `last.pt` 中的元数据包含许可证信息。
