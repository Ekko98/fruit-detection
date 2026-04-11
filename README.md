# 水果腐烂检测系统

## 项目介绍
这是一个基于 Spring Boot 的水果腐烂检测系统，使用 YOLOv8 模型进行水果新鲜度检测。

## 技术栈
- Java 8
- Spring Boot 2.7.5
- Maven
- YOLOv8 (预留接口，目前为 mock 实现)

## 功能特性：

### 后端特性：
1. **健康检查接口** - `/api/health` 返回 `{ "status": "ok" }`
2. **全局异常处理** - 统一处理各种异常
3. **标准分层结构** - Controller/Service/DTO 分离
4. **统一响应格式** - 所有接口返回标准化 JSON
5. **水果检测接口** - `/api/detect` (目前为 mock 实现)
6. **Maven 配置** - 完整的 pom.xml
7. **应用配置** - application.properties

### 前端特性：
1. **水果风格界面** - 简洁美观的 UI 设计
2. **图片上传功能** - 支持本地图片上传
3. **实时检测** - 点击按钮立即获取检测结果
4. **响应式设计** - 适配不同屏幕尺寸
5. **Vue 2.x** - 使用 Vue 框架开发
6. **Vuex 状态管理** - 管理应用状态
7. **axios 请求** - 与后端 API 通信

## 项目结构
```
fruit-detection/
├── pom.xml                    # Maven 配置
├── package.json               # Node.js 依赖配置
├── vue.config.js              # Vue 配置
├── README.md                  # 项目说明
├── src/                       # Vue 源码
│   ├── main.js                # Vue 入口文件
│   ├── App.vue                # 主应用组件
│   ├── router/                # 路由配置
│   │   └── index.js
│   ├── store/                 # Vuex 状态管理
│   │   └── index.js
│   ├── views/                 # 页面组件
│   │   └── Home.vue           # 主页面
│   ├── http/                  # HTTP 请求
│   │   └── axios.js
│   └── public/                # 静态资源
│       └── index.html
└── src/main/java/com/example/fruitdetection/
    ├── controller/            # 控制器层
    │   ├── FruitDetectionController.java    # 水果检测接口
    │   └── HealthController.java           # 健康检查接口
    ├── service/               # 服务层
    │   ├── FruitDetectionService.java      # 水果检测服务接口
    │   └── impl/FruitDetectionServiceImpl.java  # 水果检测服务实现
    ├── dto/                   # 数据传输对象
    │   ├── BaseResponse.java              # 统一响应格式
    │   ├── FruitDetectionRequest.java     # 检测请求DTO
    │   └── FruitDetectionResponse.java    # 检测响应DTO
    ├── exception/             # 异常处理
    │   └── GlobalExceptionHandler.java    # 全局异常处理器
    └── FruitDetectionApplication.java     # 应用启动类
```

## 快速开始

### 1. 后端启动
```bash
# 构建项目
mvn clean install

# 运行后端
mvn spring-boot:run
```

### 2. 前端启动
```bash
# 安装依赖
npm install

# 启动开发服务器
npm run serve
```

### 3. 访问应用
- 前端: http://localhost:8081
- 后端API: http://localhost:8080

### 4. API 接口

#### 健康检查
```
GET http://localhost:8080/api/health
响应: { "status": "ok" }
```

#### 水果检测 (POST)
```
POST http://localhost:8080/api/detect
请求体:
{
  "imageUrl": "图片URL或base64数据"
}

响应:
{
  "code": 200,
  "message": "success",
  "data": {
    "fruitType": "apple",
    "freshness": "fresh",
    "confidence": 0.95
  }
}
```

## API 文档
- `/api/health` - 健康检查接口
- `/api/detect` - 水果检测接口（POST）

## 注意事项
1. 当前检测功能为 mock 实现，实际项目中需要集成 YOLOv8 模型
2. 图片上传大小限制为 10MB
3. 支持 CORS 跨域请求

## 开发计划
- [ ] 集成 YOLOv8 模型
- [ ] 实现图片上传功能
- [ ] 实现实时摄像头检测
- [ ] 前端 Vue 界面开发
- [ ] 添加用户认证和权限管理
- [ ] 优化性能和错误处理