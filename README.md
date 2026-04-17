# 水果腐烂检测系统

基于 Spring Boot + Vue + YOLOv8 的水果新鲜度智能检测系统。

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Java 8 + Spring Boot 2.7.5 + Maven |
| 前端 | Vue 2.x + Vuex + Axios |
| 检测 | Python + YOLOv8 + Flask |

## 功能特性

- **图片检测**：上传图片检测水果新鲜度
- **实时检测**：开启摄像头实时检测
- **检测结果**：显示水果类型、新鲜度、置信度
- **检测框标注**：在图片上标注检测区域
- **历史记录**：保存最近10条检测记录

## 项目结构

```
fruit-detection/
├── start.bat                   # 一键启动脚本
├── pom.xml                     # Maven 配置
├── package.json                # Node.js 依赖
├── vue.config.js               # Vue 配置
├── src/                        # Vue 前端源码
│   ├── views/Home.vue          # 主页面
│   ├── store/index.js          # Vuex 状态管理
│   └── http/axios.js           # HTTP 请求配置
└── src/main/
    ├── java/                   # Java 后端源码
    │   └── com/example/fruitdetection/
    │       ├── controller/     # 控制器层
    │       ├── service/        # 服务层
    │       ├── dto/            # 数据传输对象
    │       └── exception/      # 异常处理
    └── resources/
        ├── python/             # Python 检测服务
        │   ├── fruit_detection_service.py  # Flask 常驻服务
        │   ├── yolov8_detector.py          # 进程调用脚本
        │   └── models/last.pt              # YOLOv8 模型
        └── opencv/             # OpenCV 库
```

## 快速启动

### 方式一：一键启动（推荐）

```bash
start.bat
```

等待约30秒，所有服务启动完成后访问 http://localhost:8081

### 方式二：分别启动

```bash
# 1. 启动 Python 检测服务（预加载模型，提高检测速度）
cd src/main/resources/python
py fruit_detection_service.py

# 2. 启动 Java 后端（等待Python服务启动后）
mvn spring-boot:run

# 3. 启动 Vue 前端
npm run serve
```

### 服务端口

| 服务 | 端口 | 说明 |
|------|------|------|
| Python 检测服务 | 5005 | YOLOv8 模型推理 |
| Java 后端 | 8080 | REST API |
| Vue 前端 | 8081 | Web 界面 |

## API 接口

### 健康检查
```
GET /api/health
响应: { "status": "ok" }
```

### 水果检测
```
POST /api/detect
请求体: { "imageUrl": "base64图片数据" }

响应:
{
  "code": 200,
  "message": "success",
  "data": {
    "fruitType": "apple",      // 水果类型
    "freshness": "fresh",      // fresh 或 rotten
    "confidence": 0.85,        // 置信度
    "detections": [...]        // 检测框列表
  }
}
```

### 无水果检测结果
```
响应:
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

## 检测性能

| 模式 | 单次检测耗时 | 说明 |
|------|-------------|------|
| Flask 服务模式 | 0.2-0.5秒 | 模型预加载，推荐 |
| 进程调用模式 | 2-3秒 | 每次启动新进程 |

## 注意事项

1. **启动顺序**：先启动 Python 服务（加载模型约10秒），再启动 Java 后端
2. **模型文件**：需要将训练好的 YOLOv8 模型放在 `src/main/resources/python/models/last.pt`
3. **图片限制**：上传图片最大 10MB
4. **CORS**：已配置允许跨域请求

## 配置说明

### application.properties
```properties
# 使用 Python Flask 服务模式（推荐）
fruit.detection.use-service=true

# 改为 false 使用进程调用模式（较慢）
```

## 开发环境

- JDK 8+
- Node.js 16+
- Python 3.8+ (需安装: flask, ultralytics, opencv-python, pillow, numpy)