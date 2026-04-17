<template>
  <div class="home">
    <header class="header">
      <div class="header-content">
        <h1>水果腐烂检测系统</h1>
        <p>AI智能识别 · 实时检测</p>
      </div>
    </header>

    <main class="main-content">
      <!-- 模式切换 -->
      <div class="mode-switch">
        <button :class="['mode-btn', { active: !cameraMode }]" @click="switchMode('image')">
          <i class="fas fa-image"></i>
          图片检测
        </button>
        <button :class="['mode-btn', { active: cameraMode }]" @click="switchMode('camera')">
          <i class="fas fa-video"></i>
          实时检测
        </button>
      </div>

      <!-- 图片检测模式 -->
      <div v-if="!cameraMode" class="detection-panel">
        <div class="upload-section glass-card">
          <div class="upload-area" @click="selectImage" @dragover.prevent @drop.prevent="handleDrop">
            <input type="file" ref="fileInput" @change="handleFileSelect" accept="image/*" style="display: none;">
            <div v-if="!imageUrl" class="upload-placeholder">
              <i class="fas fa-cloud-upload-alt"></i>
              <p>点击或拖拽图片到此处</p>
              <span class="upload-tip">支持 JPG、PNG 格式</span>
            </div>
            <div v-else class="image-preview">
              <div class="detection-image-container">
                <img :src="imageUrl" alt="上传的图片" ref="detectionImage" @load="onImageLoad">
                <div
                  v-for="(box, index) in detectionBoxes"
                  :key="index"
                  class="detection-box"
                  :style="getBoxStyle(box)">
                  <div class="detection-label" :class="box.freshness">
                    {{ box.fruitType }} · {{ box.freshness === 'fresh' ? '新鲜' : '腐烂' }}
                  </div>
                </div>
              </div>
              <button @click.stop="clearImage" class="clear-btn">
                <i class="fas fa-times"></i>
              </button>
            </div>
          </div>

          <div class="action-buttons">
            <button @click="detectFruit" :disabled="!imageUrl || isDetecting" class="detect-btn neon-btn">
              <i class="fas fa-search"></i>
              <span>{{ isDetecting ? '检测中...' : '开始检测' }}</span>
            </button>
          </div>
        </div>

        <!-- 检测结果 -->
        <div v-if="detectionResult" class="result-section glass-card">
          <h3>检测结果</h3>

          <!-- 无水果检测结果 -->
          <div v-if="!detectionResult.fruitType || detectionResult.detections.length === 0" class="result-empty">
            <div class="empty-icon">
              <i class="fas fa-search"></i>
            </div>
            <div class="empty-info">
              <span class="empty-title">未检测到水果</span>
              <span class="empty-tip">请确保图片中有清晰可见的水果</span>
            </div>
          </div>

          <!-- 有水果检测结果 -->
          <div v-else class="result-primary" :class="detectionResult.freshness">
            <div class="result-icon">
              <i :class="getFruitIcon(detectionResult.fruitType)"></i>
            </div>
            <div class="result-info">
              <span class="fruit-name">{{ detectionResult.fruitType }}</span>
              <span class="freshness-tag">{{ detectionResult.freshness === 'fresh' ? '新鲜' : '腐烂' }}</span>
              <span class="confidence-value">{{ (detectionResult.confidence * 100).toFixed(1) }}%</span>
            </div>
          </div>

          <div v-if="detectionResult.detections && detectionResult.detections.length > 1" class="all-detections">
            <h4>共检测到 {{ detectionResult.detections.length }} 个水果</h4>
            <div class="detection-list">
              <div v-for="(box, index) in detectionResult.detections" :key="index" class="detection-item">
                <span class="index">{{ index + 1 }}</span>
                <span class="fruit-type">{{ box.fruitType }}</span>
                <span :class="['status', box.freshness]">{{ box.freshness === 'fresh' ? '新鲜' : '腐烂' }}</span>
                <span class="confidence">{{ (box.confidence * 100).toFixed(1) }}%</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 统计信息 -->
        <div v-if="currentStatistics.total > 0" class="stats-section glass-card">
          <h3>本次统计</h3>
          <div class="stats-grid">
            <div class="stat-item">
              <span class="stat-value">{{ currentStatistics.total }}</span>
              <span class="stat-label">总数</span>
            </div>
            <div class="stat-item fresh">
              <span class="stat-value">{{ currentStatistics.fresh }}</span>
              <span class="stat-label">新鲜</span>
            </div>
            <div class="stat-item rotten">
              <span class="stat-value">{{ currentStatistics.rotten }}</span>
              <span class="stat-label">腐烂</span>
            </div>
            <div class="stat-item">
              <span class="stat-value">{{ freshRate }}%</span>
              <span class="stat-label">新鲜率</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 摄像头检测模式 -->
      <div v-if="cameraMode" class="camera-panel">
        <div class="camera-section glass-card">
          <div class="camera-container">
            <video ref="cameraVideo" autoplay playsinline muted class="camera-video"></video>
            <canvas ref="cameraCanvas" style="display: none;"></canvas>

            <!-- 检测框叠加层 -->
            <div class="detection-overlay">
              <div
                v-for="(box, index) in liveDetectionBoxes"
                :key="index"
                class="live-detection-box"
                :style="getLiveBoxStyle(box)">
                <div class="live-detection-label" :class="box.freshness">
                  {{ box.fruitType }} · {{ box.freshness === 'fresh' ? '新鲜' : '腐烂' }}
                </div>
              </div>
            </div>

            <!-- 扫描线动画 -->
            <div v-if="cameraActive && isDetecting" class="scan-line"></div>

            <!-- 摄像头关闭时的提示 -->
            <div v-if="!cameraActive" class="camera-placeholder">
              <i class="fas fa-video"></i>
              <p class="placeholder-title">点击下方按钮开启摄像头</p>
              <p class="placeholder-tip">
                <i class="fas fa-info-circle"></i>
                首次使用需要授权摄像头权限
              </p>
              <div class="permission-tips">
                <p>💡 使用提示：</p>
                <ul>
                  <li>首次使用请运行 <code>setup-https.bat</code>（管理员权限）</li>
                  <li>配置完成后访问: <code>https://192.168.1.124:3000</code></li>
                  <li>浏览器提示不安全时点击"高级"→"继续访问"</li>
                  <li>然后点击"开启摄像头"即可使用</li>
                </ul>
              </div>
            </div>
          </div>

          <div class="camera-controls">
            <button v-if="!cameraActive" @click="startCamera" class="camera-btn neon-btn">
              <i class="fas fa-video"></i>
              <span>开启摄像头</span>
            </button>
            <button v-else @click="stopCamera" class="camera-btn stop-btn">
              <i class="fas fa-stop"></i>
              <span>停止检测</span>
            </button>
          </div>

          <!-- 实时检测结果 -->
          <div v-if="liveDetectionBoxes.length > 0" class="live-stats">
            <span class="live-count">检测到 {{ liveDetectionBoxes.length }} 个水果</span>
            <span class="live-fresh">{{ liveDetectionBoxes.filter(b => b.freshness === 'fresh').length }} 新鲜</span>
            <span class="live-rotten">{{ liveDetectionBoxes.filter(b => b.freshness === 'rotten').length }} 腐烂</span>
          </div>
          <div v-else-if="cameraActive && !isDetecting" class="live-stats empty">
            <span class="no-fruit">未检测到水果</span>
          </div>
        </div>
      </div>

      <!-- 检测历史 -->
      <div v-if="validHistoryItems.length > 0" class="history-section glass-card">
        <div class="history-header">
          <h3>检测历史</h3>
          <button @click="clearHistory" class="clear-history-btn">
            <i class="fas fa-trash"></i>
            清空
          </button>
        </div>
        <div class="history-grid">
          <div v-for="item in validHistoryItems" :key="item.id" class="history-item">
            <div class="history-image">
              <img :src="item.imageUrl" alt="检测图片" v-if="item.imageUrl">
              <div v-else class="placeholder"><i class="fas fa-image"></i></div>
            </div>
            <div class="history-info">
              <span class="fruit-type">{{ item.fruitType }}</span>
              <span :class="['status', item.freshness]">{{ item.freshness === 'fresh' ? '新鲜' : '腐烂' }}</span>
              <span class="confidence">{{ (item.confidence * 100).toFixed(1) }}%</span>
            </div>
            <span class="history-time">{{ item.timestamp }}</span>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script>
export default {
  name: 'HomePage',
  data() {
    return {
      imageUrl: null,
      imageLoaded: false,
      imageDimensions: { width: 0, height: 0 },
      cameraMode: false,
      cameraActive: false,
      detectionFrameInterval: null,
      liveDetectionBoxes: []
    }
  },
  computed: {
    detectionResult() {
      return this.$store.state.detectionResult
    },
    detectionHistory() {
      return this.$store.state.detectionHistory
    },
    // 过滤有效的历史记录（有水果类型的）
    validHistoryItems() {
      return this.detectionHistory.filter(item => item.fruitType && item.confidence > 0)
    },
    isDetecting() {
      return this.$store.state.isDetecting
    },
    detectionBoxes() {
      if (!this.detectionResult) return []
      return this.detectionResult.detections || []
    },
    currentStatistics() {
      if (!this.detectionResult) return { total: 0, fresh: 0, rotten: 0 }
      const detections = this.detectionResult.detections || []
      // 只统计有效检测
      const validDetections = detections.filter(d => d.fruitType)
      return {
        total: validDetections.length,
        fresh: validDetections.filter(d => d.freshness === 'fresh').length,
        rotten: validDetections.filter(d => d.freshness === 'rotten').length
      }
    },
    freshRate() {
      if (!this.detectionResult || this.currentStatistics.total === 0) return 0
      return ((this.currentStatistics.fresh / this.currentStatistics.total) * 100).toFixed(1)
    }
  },
  methods: {
    // 模式切换
    switchMode(mode) {
      this.cameraMode = mode === 'camera'
      if (mode === 'image') {
        this.stopCamera()
      }
    },

    // 图片相关
    selectImage() {
      this.$refs.fileInput.click()
    },
    handleDrop(e) {
      const file = e.dataTransfer.files[0]
      if (file && file.type.startsWith('image/')) {
        this.loadImageFile(file)
      }
    },
    handleFileSelect(event) {
      const file = event.target.files[0]
      if (file) {
        this.loadImageFile(file)
      }
    },
    loadImageFile(file) {
      const reader = new FileReader()
      reader.onload = (e) => {
        this.$store.commit('setImageUrl', e.target.result)
        this.$store.commit('clearDetectionResult')
        this.imageUrl = e.target.result
        this.imageLoaded = false
      }
      reader.readAsDataURL(file)
    },
    clearImage() {
      this.imageUrl = null
      this.$store.commit('setImageUrl', null)
      this.$store.commit('clearDetectionResult')
      this.$refs.fileInput.value = ''
      this.imageLoaded = false
    },
    onImageLoad() {
      const img = this.$refs.detectionImage
      if (img) {
        this.imageDimensions = {
          width: img.naturalWidth || img.width,
          height: img.naturalHeight || img.height
        }
        this.imageLoaded = true
      }
    },
    async detectFruit() {
      if (this.imageUrl && this.imageLoaded) {
        await this.$store.dispatch('detectFruit', this.imageUrl)
      }
    },

    // 检测框样式
    getBoxStyle(box) {
      if (!box || !this.imageDimensions.width) return {}
      return {
        left: `${box.x * 100}%`,
        top: `${box.y * 100}%`,
        width: `${box.width * 100}%`,
        height: `${box.height * 100}%`
      }
    },
    getLiveBoxStyle(box) {
      if (!box) return {}
      return {
        left: `${box.x * 100}%`,
        top: `${box.y * 100}%`,
        width: `${box.width * 100}%`,
        height: `${box.height * 100}%`
      }
    },

    // 摄像头相关
    async startCamera() {
      try {
        // 先检查浏览器是否支持
        if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
          alert('您的浏览器不支持摄像头功能，请使用Chrome、Safari或Firefox浏览器')
          return
        }

        console.log('当前访问地址:', location.href)
        console.log('协议:', location.protocol)
        console.log('主机名:', location.hostname)

        // 检查权限状态（如果浏览器支持）
        if (navigator.permissions && navigator.permissions.query) {
          try {
            const permissionStatus = await navigator.permissions.query({ name: 'camera' })
            console.log('摄像头权限状态:', permissionStatus.state)

            if (permissionStatus.state === 'denied') {
              alert('摄像头权限已被拒绝，请在浏览器设置中允许摄像头访问，然后刷新页面重试')
              return
            }

            if (permissionStatus.state === 'prompt') {
              // 权限未确定，会弹出提示
              console.log('即将请求摄像头权限...')
            }
          } catch (permError) {
            console.log('权限查询不支持:', permError)
          }
        }

        // 请求摄像头权限
        console.log('正在请求摄像头权限...')
        const stream = await navigator.mediaDevices.getUserMedia({
          video: {
            facingMode: { ideal: 'environment' }, // 优先后置摄像头
            width: { ideal: 1280 },
            height: { ideal: 720 }
          },
          audio: false
        })

        console.log('摄像头权限获取成功')

        // 设置视频流
        this.$refs.cameraVideo.srcObject = stream
        this.cameraActive = true
        this.$store.commit('setCameraActive', true)
        this.$store.commit('setCameraStream', stream)

        // 等待视频加载
        await new Promise(resolve => {
          this.$refs.cameraVideo.onloadedmetadata = resolve
        })

        // 开始实时检测
        this.startRealtimeDetection()

      } catch (error) {
        console.error('摄像头启动失败:', error)
        this.handleCameraError(error)
      }
    },

    // 处理摄像头错误
    handleCameraError(error) {
      console.error('摄像头错误详情:', error)
      let message = '无法访问摄像头\n\n'

      switch (error.name) {
        case 'NotAllowedError':
        case 'PermissionDeniedError':
          message += '原因：摄像头权限被拒绝\n\n'
          message += '解决方法：\n'
          message += '• 点击地址栏左侧的图标，允许摄像头权限\n'
          message += '• Android：设置 → 应用 → Chrome → 权限 → 相机 → 允许\n'
          message += '• 刷新页面后重试'
          break
        case 'NotFoundError':
        case 'DevicesNotFoundError':
          message += '原因：未找到摄像头设备\n\n'
          message += '请确认您的设备有可用的摄像头'
          break
        case 'NotReadableError':
        case 'TrackStartError':
          message += '原因：摄像头被其他应用占用\n\n'
          message += '请关闭其他正在使用摄像头的应用后重试'
          break
        case 'OverconstrainedError':
        case 'ConstraintNotSatisfiedError':
          message += '原因：摄像头不支持请求的分辨率\n\n'
          message += '正在尝试使用默认设置...'
          // 可以尝试用更简单的配置重试
          this.retryWithBasicConfig()
          return
        case 'NotSupportedError':
          message += '原因：浏览器不支持或页面不安全\n\n'
          message += '解决方案：\n'
          message += '• 使用 Chrome 60+ 或 Safari 11+ 浏览器\n'
          message += '• Android Chrome 允许在局域网IP访问摄像头'
          break
        case 'TypeError':
          message += '原因：无法访问媒体设备\n\n'
          message += '当前地址：' + location.href + '\n\n'
          message += '提示：Chrome浏览器支持局域网IP访问摄像头'
          break
        default:
          message += `错误类型：${error.name}\n`
          message += `错误信息：${error.message}`
      }

      alert(message)
    },

    // 使用基本配置重试
    async retryWithBasicConfig() {
      try {
        const stream = await navigator.mediaDevices.getUserMedia({
          video: true // 使用最基本配置
        })

        this.$refs.cameraVideo.srcObject = stream
        this.cameraActive = true
        this.$store.commit('setCameraActive', true)
        this.$store.commit('setCameraStream', stream)

        await new Promise(resolve => {
          this.$refs.cameraVideo.onloadedmetadata = resolve
        })

        this.startRealtimeDetection()
      } catch (retryError) {
        console.error('重试失败:', retryError)
        alert('摄像头启动失败，请检查设备设置')
      }
    },
    stopCamera() {
      // 停止检测
      if (this.detectionFrameInterval) {
        clearInterval(this.detectionFrameInterval)
        this.detectionFrameInterval = null
      }

      // 停止所有视频轨道
      if (this.$refs.cameraVideo && this.$refs.cameraVideo.srcObject) {
        const tracks = this.$refs.cameraVideo.srcObject.getTracks()
        tracks.forEach(track => {
          track.stop()
          console.log('已停止视频轨道:', track.label)
        })
        this.$refs.cameraVideo.srcObject = null
      }

      // 清除store中的状态
      this.cameraActive = false
      this.liveDetectionBoxes = []
      this.$store.commit('clearCameraState')

      console.log('摄像头已关闭')
    },
    startRealtimeDetection() {
      // 配置参数
      const DETECTION_INTERVAL = 1000 // 检测间隔：1秒（等待上次请求完成）
      const IMAGE_QUALITY = 0.6 // 图片质量：降低到60%以加快传输
      const IMAGE_SCALE = 0.5 // 图片缩放：缩小到50%以加快处理

      let isProcessing = false // 防止请求堆积的锁

      this.detectionFrameInterval = setInterval(async () => {
        if (!this.cameraActive || !this.$refs.cameraVideo) return

        // 如果正在处理，跳过本次
        if (isProcessing) return

        isProcessing = true

        try {
          const canvas = this.$refs.cameraCanvas
          const video = this.$refs.cameraVideo

          // 降低分辨率加快处理
          const targetWidth = Math.floor((video.videoWidth || 640) * IMAGE_SCALE)
          const targetHeight = Math.floor((video.videoHeight || 480) * IMAGE_SCALE)

          canvas.width = targetWidth
          canvas.height = targetHeight

          const ctx = canvas.getContext('2d')
          ctx.drawImage(video, 0, 0, targetWidth, targetHeight)

          // 使用较低的图片质量
          const imageData = canvas.toDataURL('image/jpeg', IMAGE_QUALITY)

          this.$store.commit('setIsDetecting', true)
          const response = await this.$http.post('/detect', { imageUrl: imageData })

          if (response.data && response.data.detections) {
            this.liveDetectionBoxes = response.data.detections
          }
        } catch (error) {
          console.error('实时检测失败:', error)
          // 不显示错误，继续下一帧检测
        } finally {
          this.$store.commit('setIsDetecting', false)
          isProcessing = false
        }

      }, DETECTION_INTERVAL)
    },

    // 水果图标
    getFruitIcon(fruitType) {
      const icons = {
        apple: 'fas fa-apple-alt',
        banana: 'fas fa-lemon',
        orange: 'fas fa-circle'
      }
      return icons[fruitType] || 'fas fa-apple-alt'
    },

    // 清空历史
    clearHistory() {
      this.$store.commit('clearHistory')
    }
  },
  beforeDestroy() {
    this.stopCamera()
  }
}
</script>

<style scoped>
/* 基础样式 */
.home {
  min-height: 100vh;
  padding: 20px;
  background: linear-gradient(135deg, #0a0a1a 0%, #1a1a3a 50%, #0d1b2a 100%);
  color: #e0e0e0;
  font-family: 'Segoe UI', 'Microsoft YaHei', sans-serif;
}

/* Header */
.header {
  background: linear-gradient(135deg, rgba(0, 245, 255, 0.1) 0%, rgba(0, 128, 255, 0.1) 100%);
  border: 1px solid rgba(0, 245, 255, 0.3);
  border-radius: 20px;
  padding: 30px;
  margin-bottom: 30px;
  text-align: center;
  backdrop-filter: blur(10px);
}

.header h1 {
  font-size: 2.5rem;
  color: #00f5ff;
  margin: 0;
  text-shadow: 0 0 20px rgba(0, 245, 255, 0.5);
}

.header p {
  color: rgba(255, 255, 255, 0.6);
  margin-top: 10px;
  font-size: 1rem;
}

/* 模式切换 */
.mode-switch {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-bottom: 30px;
}

.mode-btn {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(0, 245, 255, 0.3);
  color: rgba(255, 255, 255, 0.7);
  padding: 15px 40px;
  border-radius: 15px;
  font-size: 1.1rem;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  gap: 10px;
}

.mode-btn:hover {
  background: rgba(0, 245, 255, 0.1);
  border-color: rgba(0, 245, 255, 0.5);
}

.mode-btn.active {
  background: linear-gradient(135deg, rgba(0, 245, 255, 0.2) 0%, rgba(0, 128, 255, 0.2) 100%);
  border-color: #00f5ff;
  color: #00f5ff;
  box-shadow: 0 0 30px rgba(0, 245, 255, 0.3);
}

/* 玻璃卡片 */
.glass-card {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(0, 245, 255, 0.2);
  border-radius: 20px;
  padding: 25px;
  backdrop-filter: blur(10px);
  margin-bottom: 25px;
}

.glass-card h3 {
  color: #00f5ff;
  margin: 0 0 20px 0;
  font-size: 1.3rem;
  border-bottom: 1px solid rgba(0, 245, 255, 0.2);
  padding-bottom: 15px;
}

/* 上传区域 */
.upload-area {
  border: 2px dashed rgba(0, 245, 255, 0.3);
  border-radius: 15px;
  padding: 40px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
  min-height: 200px;
}

.upload-area:hover {
  border-color: #00f5ff;
  background: rgba(0, 245, 255, 0.05);
  box-shadow: inset 0 0 30px rgba(0, 245, 255, 0.1);
}

.upload-placeholder {
  color: rgba(255, 255, 255, 0.6);
}

.upload-placeholder i {
  font-size: 3rem;
  color: #00f5ff;
  margin-bottom: 15px;
  animation: float 3s ease-in-out infinite;
}

.upload-placeholder p {
  margin: 10px 0;
}

.upload-tip {
  color: rgba(255, 255, 255, 0.4);
  font-size: 0.9rem;
}

/* 图片预览 */
.image-preview {
  position: relative;
}

.detection-image-container {
  position: relative;
  display: inline-block;
}

.detection-image-container img {
  max-width: 100%;
  max-height: 400px;
  border-radius: 10px;
}

.clear-btn {
  position: absolute;
  top: 10px;
  right: 10px;
  background: rgba(255, 100, 100, 0.8);
  border: none;
  border-radius: 50%;
  width: 30px;
  height: 30px;
  color: white;
  cursor: pointer;
  transition: all 0.3s;
}

.clear-btn:hover {
  background: #ff6b6b;
  transform: rotate(90deg);
}

/* 检测框 */
.detection-box {
  position: absolute;
  border: 2px solid #00f5ff;
  border-radius: 4px;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  animation: fadeIn 0.3s ease;
}

.detection-box.fresh {
  border-color: #00ff88;
}

.detection-box.rotten {
  border-color: #ff6b6b;
}

.detection-label {
  background: rgba(0, 0, 0, 0.7);
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 12px;
  color: white;
  white-space: nowrap;
}

.detection-label.fresh {
  background: rgba(0, 255, 136, 0.8);
}

.detection-label.rotten {
  background: rgba(255, 107, 107, 0.8);
}

/* 霓虹按钮 */
.neon-btn {
  background: linear-gradient(135deg, rgba(0, 245, 255, 0.3) 0%, rgba(0, 128, 255, 0.3) 100%);
  border: 1px solid #00f5ff;
  color: #00f5ff;
  padding: 15px 35px;
  border-radius: 15px;
  font-size: 1.1rem;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  gap: 10px;
  box-shadow: 0 0 20px rgba(0, 245, 255, 0.3);
}

.neon-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, rgba(0, 245, 255, 0.5) 0%, rgba(0, 128, 255, 0.5) 100%);
  box-shadow: 0 0 40px rgba(0, 245, 255, 0.5);
  transform: translateY(-2px);
}

.neon-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  box-shadow: none;
}

.detect-btn {
  justify-content: center;
}

.action-buttons {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

/* 结果展示 */
.result-primary {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 20px;
  border-radius: 15px;
  background: rgba(0, 245, 255, 0.1);
  border: 1px solid rgba(0, 245, 255, 0.3);
}

/* 无水果检测结果样式 */
.result-empty {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 30px;
  border-radius: 15px;
  background: rgba(150, 150, 150, 0.1);
  border: 1px solid rgba(150, 150, 150, 0.3);
}

.empty-icon {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background: rgba(150, 150, 150, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
}

.empty-icon i {
  font-size: 2rem;
  color: rgba(255, 255, 255, 0.5);
}

.empty-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.empty-title {
  font-size: 1.5rem;
  font-weight: bold;
  color: rgba(255, 255, 255, 0.7);
}

.empty-tip {
  color: rgba(255, 255, 255, 0.4);
  font-size: 0.9rem;
}

.result-primary.fresh {
  background: rgba(0, 255, 136, 0.1);
  border-color: rgba(0, 255, 136, 0.3);
}

.result-primary.rotten {
  background: rgba(255, 107, 107, 0.1);
  border-color: rgba(255, 107, 107, 0.3);
}

.result-icon {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background: rgba(0, 245, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
}

.result-icon i {
  font-size: 2rem;
  color: #00f5ff;
}

.result-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.fruit-name {
  font-size: 1.5rem;
  font-weight: bold;
  color: white;
}

.freshness-tag {
  padding: 5px 15px;
  border-radius: 10px;
  font-size: 0.9rem;
}

.freshness-tag, .status.fresh {
  background: rgba(0, 255, 136, 0.2);
  color: #00ff88;
}

.freshness-tag, .status.rotten {
  background: rgba(255, 107, 107, 0.2);
  color: #ff6b6b;
}

.confidence-value {
  color: rgba(255, 255, 255, 0.8);
}

.all-detections h4 {
  color: rgba(255, 255, 255, 0.6);
  margin: 20px 0 15px 0;
}

.detection-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.detection-item {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 12px 15px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 10px;
  border-left: 3px solid #00f5ff;
}

.detection-item .index {
  color: #00f5ff;
  font-weight: bold;
  min-width: 20px;
}

.detection-item .fruit-type {
  color: white;
  font-weight: 500;
}

.detection-item .status {
  padding: 3px 10px;
  border-radius: 5px;
  font-size: 0.85rem;
}

.detection-item .confidence {
  color: rgba(255, 255, 255, 0.5);
  font-size: 0.85rem;
}

/* 统计卡片 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 15px;
}

.stat-item {
  text-align: center;
  padding: 15px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 10px;
}

.stat-item.fresh {
  background: rgba(0, 255, 136, 0.1);
  border: 1px solid rgba(0, 255, 136, 0.2);
}

.stat-item.rotten {
  background: rgba(255, 107, 107, 0.1);
  border: 1px solid rgba(255, 107, 107, 0.2);
}

.stat-value {
  font-size: 2rem;
  font-weight: bold;
  color: #00f5ff;
  display: block;
}

.stat-item.fresh .stat-value {
  color: #00ff88;
}

.stat-item.rotten .stat-value {
  color: #ff6b6b;
}

.stat-label {
  color: rgba(255, 255, 255, 0.5);
  font-size: 0.9rem;
}

/* 摄像头区域 */
.camera-container {
  position: relative;
  width: 100%;
  max-width: 640px;
  margin: 0 auto;
  border-radius: 15px;
  overflow: hidden;
  background: rgba(0, 0, 0, 0.3);
}

.camera-video {
  width: 100%;
  display: block;
  border-radius: 15px;
}

.detection-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.live-detection-box {
  position: absolute;
  border: 2px solid #00f5ff;
  border-radius: 4px;
  animation: fadeIn 0.2s ease;
}

.live-detection-box.fresh {
  border-color: #00ff88;
}

.live-detection-box.rotten {
  border-color: #ff6b6b;
}

.live-detection-label {
  position: absolute;
  top: -25px;
  left: 0;
  background: rgba(0, 0, 0, 0.8);
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 12px;
  color: white;
  white-space: nowrap;
}

.live-detection-label.fresh {
  background: rgba(0, 255, 136, 0.9);
}

.live-detection-label.rotten {
  background: rgba(255, 107, 107, 0.9);
}

/* 扫描线动画 */
.scan-line {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 3px;
  background: linear-gradient(90deg, transparent, #00f5ff, transparent);
  animation: scan 2s linear infinite;
}

.camera-placeholder {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.6);
  background: rgba(0, 0, 0, 0.3);
  padding: 20px;
}

.camera-placeholder i:first-child {
  font-size: 4rem;
  margin-bottom: 15px;
  color: #00f5ff;
}

.placeholder-title {
  font-size: 1.2rem;
  margin-bottom: 10px;
}

.placeholder-tip {
  font-size: 0.9rem;
  color: rgba(255, 255, 255, 0.5);
  margin-bottom: 20px;
}

.placeholder-tip i {
  margin-right: 5px;
}

.permission-tips {
  text-align: left;
  background: rgba(0, 245, 255, 0.1);
  border: 1px solid rgba(0, 245, 255, 0.2);
  border-radius: 10px;
  padding: 15px 20px;
  max-width: 400px;
  width: 90%;
}

.permission-tips p {
  color: #00f5ff;
  margin: 0 0 10px 0;
  font-weight: bold;
}

.permission-tips ul {
  margin: 0;
  padding-left: 20px;
}

.permission-tips li {
  color: rgba(255, 255, 255, 0.7);
  font-size: 0.85rem;
  line-height: 1.8;
}

.camera-controls {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.camera-btn {
  justify-content: center;
}

.stop-btn {
  background: linear-gradient(135deg, rgba(255, 107, 107, 0.3) 0%, rgba(255, 50, 50, 0.3) 100%);
  border-color: #ff6b6b;
  color: #ff6b6b;
  box-shadow: 0 0 20px rgba(255, 107, 107, 0.3);
}

.stop-btn:hover {
  background: linear-gradient(135deg, rgba(255, 107, 107, 0.5) 0%, rgba(255, 50, 50, 0.5) 100%);
  box-shadow: 0 0 40px rgba(255, 107, 107, 0.5);
}

.live-stats {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-top: 15px;
  padding: 10px;
  background: rgba(0, 245, 255, 0.1);
  border-radius: 10px;
}

.live-stats.empty {
  background: rgba(150, 150, 150, 0.1);
}

.no-fruit {
  color: rgba(255, 255, 255, 0.6);
  font-size: 0.9rem;
}

.live-count {
  color: #00f5ff;
  font-weight: bold;
}

.live-fresh {
  color: #00ff88;
}

.live-rotten {
  color: #ff6b6b;
}

/* 历史记录 */
.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid rgba(0, 245, 255, 0.2);
  padding-bottom: 15px;
  margin-bottom: 20px;
}

.clear-history-btn {
  background: rgba(255, 107, 107, 0.2);
  border: 1px solid rgba(255, 107, 107, 0.3);
  color: #ff6b6b;
  padding: 8px 15px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  gap: 5px;
}

.clear-history-btn:hover {
  background: rgba(255, 107, 107, 0.3);
}

.history-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 15px;
}

.history-item {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 10px;
  padding: 15px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  border: 1px solid rgba(0, 245, 255, 0.1);
  transition: all 0.3s;
}

.history-item:hover {
  border-color: rgba(0, 245, 255, 0.3);
  transform: translateY(-3px);
}

.history-image {
  width: 100%;
  height: 100px;
  border-radius: 8px;
  overflow: hidden;
  background: rgba(0, 0, 0, 0.2);
}

.history-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.history-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.history-time {
  color: rgba(255, 255, 255, 0.3);
  font-size: 0.8rem;
}

/* 动画 */
@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}

@keyframes scan {
  0% { top: 0; }
  100% { top: 100%; }
}

/* 响应式 */
@media (max-width: 768px) {
  .header h1 {
    font-size: 1.8rem;
  }

  .mode-switch {
    flex-direction: column;
    gap: 10px;
  }

  .mode-btn {
    width: 100%;
    justify-content: center;
  }

  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .result-primary {
    flex-direction: column;
    text-align: center;
  }
}
</style>