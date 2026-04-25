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
        <button :class="['mode-btn', { active: mode === 'image' }]" @click="mode = 'image'">
          <i class="fas fa-image"></i>
          图片检测
        </button>
        <button :class="['mode-btn', { active: mode === 'batch' }]" @click="mode = 'batch'">
          <i class="fas fa-images"></i>
          批量检测
        </button>
        <button :class="['mode-btn', { active: mode === 'camera' }]" @click="mode = 'camera'">
          <i class="fas fa-video"></i>
          实时检测
        </button>
      </div>

      <!-- 图片检测模式 -->
      <div v-if="mode === 'image'" class="detection-panel">
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

      <!-- 批量检测模式 -->
      <div v-if="mode === 'batch'" class="batch-panel">
        <div class="batch-section glass-card">
          <div class="batch-header">
            <h3>上传图片</h3>
            <span class="image-count">已选 {{ batchImages.length }}/10 张</span>
          </div>

          <div class="batch-upload-area" @click="selectBatchImages" @dragover.prevent @drop.prevent="batchHandleDrop">
            <input type="file" ref="batchFileInput" @change="batchHandleFileSelect" accept="image/*" multiple style="display: none;">
            <div v-if="batchImages.length === 0" class="upload-placeholder">
              <i class="fas fa-plus-circle"></i>
              <p>点击选择多张图片或拖拽到此处</p>
              <span class="upload-tip">最多支持 10 张，支持 JPG、PNG</span>
            </div>
          </div>

          <!-- 已选图片列表 -->
          <div v-if="batchImages.length > 0" class="batch-image-list">
            <div class="batch-thumbs">
              <div v-for="(img, idx) in batchImages" :key="idx" class="batch-thumb">
                <img :src="img.url" alt="图片">
                <button @click="removeBatchImage(idx)" class="remove-thumb">
                  <i class="fas fa-times"></i>
                </button>
              </div>
            </div>

            <div class="batch-actions">
              <button @click="selectMoreImages" class="add-more-btn">
                <i class="fas fa-plus"></i>
                继续添加
              </button>
              <button @click="startBatchDetect" :disabled="batchImages.length === 0" class="batch-detect-btn neon-btn">
                <i class="fas fa-search"></i>
                <span>{{ isBatchDetecting ? '检测中...' : '开始批量检测' }}</span>
              </button>
              <button @click="clearBatch" class="clear-all-btn">
                <i class="fas fa-trash-alt"></i>
                清空
              </button>
            </div>
          </div>
        </div>

        <!-- 批量检测结果 -->
        <div v-if="batchResults && batchResults.length > 0" class="batch-results-section glass-card">
          <h3>批量检测结果</h3>
          <div class="batch-results-list">
            <div v-for="(result, idx) in batchResults" :key="idx" class="batch-result-item">
              <div class="result-image detection-image-container">
                <img :src="result.imageUrl" alt="结果图片" v-if="result.imageUrl">
                <div v-else class="result-placeholder"><i class="fas fa-image"></i></div>
                <div
                  v-for="(box, bIdx) in result.detections"
                  :key="bIdx"
                  class="detection-box"
                  :class="box.freshness"
                  :style="getBoxStyle(box)">
                  <div class="detection-label" :class="box.freshness">
                    {{ box.fruitType }} · {{ box.freshness === 'fresh' ? '新鲜' : '腐烂' }}
                  </div>
                </div>
              </div>
              <div class="result-info-card">
                <div v-if="result.fruitType" class="result-tags">
                  <span class="tag fruit-name">{{ result.fruitType }}</span>
                  <span :class="['tag', 'tag-freshness', result.freshness]">
                    {{ result.freshness === 'fresh' ? '新鲜' : '腐烂' }}
                  </span>
                  <span class="tag tag-confidence">{{ (result.confidence * 100).toFixed(1) }}%</span>
                </div>
                <div v-else class="result-empty-tag">
                  <i class="fas fa-search"></i>
                  <span>未检测到水果</span>
                </div>
                <div v-if="result.detections && result.detections.length > 1" class="result-count">
                  共 {{ result.detections.length }} 个水果
                </div>
              </div>
            </div>
          </div>

          <!-- 批量统计 -->
          <div class="batch-stats">
            <div class="batch-stat-item">
              <span class="stat-value">{{ batchTotalStats.total }}</span>
              <span class="stat-label">总图片</span>
            </div>
            <div class="batch-stat-item">
              <span class="stat-value">{{ batchTotalStats.fruits }}</span>
              <span class="stat-label">水果数</span>
            </div>
            <div class="batch-stat-item">
              <span class="stat-value">{{ batchTotalStats.freshCount }}</span>
              <span class="stat-label">新鲜</span>
            </div>
            <div class="batch-stat-item">
              <span class="stat-value">{{ batchTotalStats.rottenCount }}</span>
              <span class="stat-label">腐烂</span>
            </div>
            <div class="batch-stat-item">
              <span class="stat-value">{{ batchFreshRate }}%</span>
              <span class="stat-label">新鲜率</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 摄像头检测模式 -->
      <div v-if="mode === 'camera'" class="camera-panel">
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
                :class="box.freshness"
                :style="getBoxStyle(box)">
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
                  <li>配置完成后访问: <code>{{ accessURL }}</code></li>
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
            <div class="history-image detection-image-container">
              <img :src="item.imageUrl" alt="检测图片" v-if="item.imageUrl">
              <div v-else class="placeholder"><i class="fas fa-image"></i></div>
              <!-- 检测框 -->
              <div
                v-for="(box, index) in item.detections"
                :key="index"
                class="detection-box"
                :class="box.freshness"
                :style="getBoxStyle(box)">
                <div class="detection-label" :class="box.freshness">
                  {{ box.fruitType }} · {{ box.freshness === 'fresh' ? '新鲜' : '腐烂' }}
                </div>
              </div>
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
      mode: 'image', // 'image' | 'batch' | 'camera'
      cameraActive: false,
      detectionFrameInterval: null,
      liveDetectionBoxes: [],
      accessURL: window.location.protocol + '//' + window.location.hostname + ':3000'
    }
  },
  computed: {
    detectionResult() {
      return this.$store.state.detectionResult
    },
    detectionHistory() {
      return this.$store.state.detectionHistory
    },
    // 过滤有效的历史记录（有水果类型且有检测框的）
    validHistoryItems() {
      return this.detectionHistory.filter(item => item.fruitType && item.confidence > 0 && item.detections && item.detections.length > 0)
    },
    batchImages() {
      return this.$store.state.batchImages
    },
    batchResults() {
      return this.$store.state.batchResults
    },
    isBatchDetecting() {
      return this.$store.state.isBatchDetecting
    },
    batchTotalStats() {
      if (!this.batchResults) return { total: 0, fruits: 0, freshCount: 0, rottenCount: 0 }
      let fruits = 0, fresh = 0, rotten = 0
      this.batchResults.forEach(r => {
        const detections = (r.detections || []).filter(d => d.fruitType)
        fruits += detections.length
        detections.forEach(d => {
          if (d.freshness === 'fresh') fresh++
          else rotten++
        })
      })
      return { total: this.batchResults.length, fruits, freshCount: fresh, rottenCount: rotten }
    },
    batchFreshRate() {
      if (this.batchTotalStats.fruits === 0) return 0
      return ((this.batchTotalStats.freshCount / this.batchTotalStats.fruits) * 100).toFixed(1)
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
    switchMode(newMode) {
      this.mode = newMode
      if (newMode !== 'camera') {
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

    // 统一的检测框样式（三种模式共用）
    getBoxStyle(box) {
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
    },

    // 批量检测相关方法
    batchHandleDrop(e) {
      const files = Array.from(e.dataTransfer.files).filter(f => f.type.startsWith('image/'))
      this.loadBatchFiles(files)
    },
    batchHandleFileSelect(event) {
      const files = Array.from(event.target.files)
      this.loadBatchFiles(files)
      this.clearBatchInput()
    },
    selectBatchImages() {
      if (this.$refs.batchFileInput) {
        this.$refs.batchFileInput.value = ''
        this.$refs.batchFileInput.click()
      }
    },
    selectMoreImages() {
      this.selectBatchImages()
    },
    loadBatchFiles(files) {
      const remaining = 10 - this.batchImages.length
      const toLoad = files.slice(0, remaining)
      toLoad.forEach(file => {
        const reader = new FileReader()
        reader.onload = (e) => {
          this.$store.commit('addBatchImage', {
            url: e.target.result,
            data: e.target.result,
            fileName: file.name
          })
        }
        reader.readAsDataURL(file)
      })
    },
    clearBatchInput() {
      if (this.$refs.batchFileInput) {
        this.$refs.batchFileInput.value = ''
      }
    },
    removeBatchImage(index) {
      this.$store.commit('removeBatchImage', index)
      this.$store.commit('setBatchResults', null)
    },
    clearBatch() {
      this.$store.commit('clearBatchImages')
    },
    async startBatchDetect() {
      if (this.batchImages.length === 0) return
      await this.$store.dispatch('batchDetect')
    }
  },
  beforeDestroy() {
    this.stopCamera()
  }
}
</script>

<style scoped>
/* 多巴胺风格 - 活泼明亮的设计 */
.home {
  min-height: 100vh;
  padding: 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 25%, #f093fb 50%, #f5576c 75%, #ffecd2 100%);
  background-size: 400% 400%;
  animation: gradientBG 15s ease infinite;
  color: #1a1a2e;
  font-family: 'Segoe UI', 'Microsoft YaHei', sans-serif;
  position: relative;
  overflow-x: hidden;
}

/* 背景动画 */
.home::before {
  content: '';
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background:
    radial-gradient(circle at 20% 80%, rgba(255, 107, 237, 0.3) 0%, transparent 50%),
    radial-gradient(circle at 80% 20%, rgba(72, 219, 251, 0.3) 0%, transparent 50%),
    radial-gradient(circle at 40% 40%, rgba(254, 202, 87, 0.2) 0%, transparent 40%);
  pointer-events: none;
  z-index: 0;
}

@keyframes gradientBG {
  0% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
  100% { background-position: 0% 50%; }
}

/* Header - 多巴胺风格 */
.header {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.95) 0%, rgba(255, 255, 255, 0.85) 100%);
  border: 3px solid transparent;
  border-image: linear-gradient(135deg, #ff6b6b, #feca57, #48dbfb, #ff9ff3) 1;
  border-radius: 24px;
  padding: 35px;
  margin-bottom: 30px;
  text-align: center;
  backdrop-filter: blur(20px);
  box-shadow: 0 10px 40px rgba(102, 126, 234, 0.3), 0 0 0 1px rgba(255, 255, 255, 0.5);
  position: relative;
  overflow: hidden;
}

.header::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: linear-gradient(45deg, transparent 30%, rgba(255, 255, 255, 0.3) 50%, transparent 70%);
  animation: shine 3s infinite;
}

@keyframes shine {
  0% { transform: translateX(-100%) rotate(45deg); }
  100% { transform: translateX(100%) rotate(45deg); }
}

.header h1 {
  font-size: 2.8rem;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 50%, #f093fb 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin: 0;
  position: relative;
  z-index: 1;
  font-weight: 800;
  letter-spacing: 2px;
}

.header p {
  color: #6c5ce7;
  margin-top: 12px;
  font-size: 1.1rem;
  font-weight: 500;
  position: relative;
  z-index: 1;
}

/* 模式切换 - 多巴胺按钮 */
.mode-switch {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-bottom: 30px;
  position: relative;
  z-index: 1;
}

.mode-btn {
  background: rgba(255, 255, 255, 0.9);
  border: 3px solid transparent;
  background-clip: padding-box;
  color: #6c5ce7;
  padding: 16px 45px;
  border-radius: 50px;
  font-size: 1.1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  display: flex;
  align-items: center;
  gap: 10px;
  box-shadow: 0 8px 25px rgba(102, 126, 234, 0.25);
  position: relative;
}

.mode-btn::before {
  content: '';
  position: absolute;
  top: -3px;
  left: -3px;
  right: -3px;
  bottom: -3px;
  background: linear-gradient(135deg, #667eea, #764ba2, #f093fb, #feca57);
  border-radius: 53px;
  z-index: -1;
  opacity: 0;
  transition: opacity 0.3s;
}

.mode-btn:hover {
  transform: translateY(-5px) scale(1.02);
  box-shadow: 0 15px 35px rgba(102, 126, 234, 0.4);
}

.mode-btn:hover::before {
  opacity: 1;
}

.mode-btn.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  transform: scale(1.05);
  box-shadow: 0 15px 40px rgba(102, 126, 234, 0.5);
}

.mode-btn.active::before {
  opacity: 1;
  background: linear-gradient(135deg, #feca57, #ff6b6b, #48dbfb, #ff9ff3);
  animation: borderRotate 3s linear infinite;
}

@keyframes borderRotate {
  0% { filter: hue-rotate(0deg); }
  100% { filter: hue-rotate(360deg); }
}

/* 玻璃卡片 - 多巴胺风格 */
.glass-card {
  background: rgba(255, 255, 255, 0.85);
  border-radius: 24px;
  padding: 28px;
  backdrop-filter: blur(20px);
  margin-bottom: 25px;
  box-shadow: 0 10px 40px rgba(102, 126, 234, 0.2), 0 0 0 1px rgba(255, 255, 255, 0.5);
  position: relative;
  z-index: 1;
  overflow: hidden;
}

.glass-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #667eea, #764ba2, #f093fb, #feca57, #48dbfb, #ff6b6b);
  background-size: 200% 100%;
  animation: gradientMove 3s linear infinite;
}

@keyframes gradientMove {
  0% { background-position: 0% 0%; }
  100% { background-position: 200% 0%; }
}

.glass-card h3 {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin: 0 0 20px 0;
  font-size: 1.4rem;
  font-weight: 700;
  padding-bottom: 15px;
  border-bottom: 2px solid rgba(102, 126, 234, 0.2);
}

/* 上传区域 - 多巴胺风格 */
.upload-area {
  border: 3px dashed #c4b5fd;
  border-radius: 20px;
  padding: 45px;
  text-align: center;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  position: relative;
  min-height: 220px;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.05) 0%, rgba(240, 147, 251, 0.05) 100%);
}

.upload-area:hover {
  border-color: #764ba2;
  border-style: solid;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.1) 0%, rgba(240, 147, 251, 0.1) 100%);
  transform: scale(1.02);
  box-shadow: 0 15px 40px rgba(102, 126, 234, 0.2);
}

.upload-placeholder {
  color: #6c5ce7;
}

.upload-placeholder i {
  font-size: 4rem;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 50%, #f093fb 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: 18px;
  animation: bounce 2s ease-in-out infinite;
}

@keyframes bounce {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-15px); }
}

.upload-placeholder p {
  margin: 12px 0;
  font-weight: 500;
  font-size: 1.1rem;
}

.upload-tip {
  color: #a29bfe;
  font-size: 0.95rem;
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

/* 检测框 - 多巴胺风格 */
.detection-box {
  position: absolute;
  border: 3px solid #764ba2;
  border-radius: 8px;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  animation: popIn 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  box-shadow: 0 0 20px rgba(118, 75, 162, 0.4);
}

@keyframes popIn {
  0% { transform: scale(0); opacity: 0; }
  100% { transform: scale(1); opacity: 1; }
}

.detection-box.fresh {
  border-color: #00d2d3;
  box-shadow: 0 0 25px rgba(0, 210, 211, 0.5);
}

.detection-box.rotten {
  border-color: #ff6b6b;
  box-shadow: 0 0 25px rgba(255, 107, 107, 0.5);
}

.detection-label {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 13px;
  color: white;
  white-space: nowrap;
  font-weight: 600;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
}

.detection-label.fresh {
  background: linear-gradient(135deg, #00d2d3 0%, #10ac84 100%);
  box-shadow: 0 4px 15px rgba(0, 210, 211, 0.4);
}

.detection-label.rotten {
  background: linear-gradient(135deg, #ff6b6b 0%, #ee5a24 100%);
  box-shadow: 0 4px 15px rgba(255, 107, 107, 0.4);
}

/* 霓虹按钮 - 多巴胺风格 */
.neon-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  color: white;
  padding: 18px 45px;
  border-radius: 50px;
  font-size: 1.15rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  display: flex;
  align-items: center;
  gap: 12px;
  box-shadow: 0 10px 30px rgba(102, 126, 234, 0.4);
  position: relative;
  overflow: hidden;
}

.neon-btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.3), transparent);
  transition: left 0.5s;
}

.neon-btn:hover:not(:disabled)::before {
  left: 100%;
}

.neon-btn:hover:not(:disabled) {
  transform: translateY(-5px) scale(1.05);
  box-shadow: 0 20px 50px rgba(102, 126, 234, 0.5);
}

.neon-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

.detect-btn {
  justify-content: center;
}

.action-buttons {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

/* 结果展示 - 多巴胺风格 */
.result-primary {
  display: flex;
  align-items: center;
  gap: 25px;
  padding: 25px;
  border-radius: 20px;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.15) 0%, rgba(118, 75, 162, 0.15) 100%);
  border: 2px solid rgba(102, 126, 234, 0.3);
  transition: all 0.3s;
}

.result-primary:hover {
  transform: scale(1.02);
  box-shadow: 0 10px 30px rgba(102, 126, 234, 0.2);
}

/* 无水果检测结果样式 */
.result-empty {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 35px;
  border-radius: 20px;
  background: linear-gradient(135deg, rgba(162, 155, 254, 0.1) 0%, rgba(108, 92, 231, 0.1) 100%);
  border: 2px dashed #a29bfe;
}

.empty-icon {
  width: 70px;
  height: 70px;
  border-radius: 50%;
  background: linear-gradient(135deg, #a29bfe 0%, #6c5ce7 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.empty-icon i {
  font-size: 2rem;
  color: white;
}

.empty-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.empty-title {
  font-size: 1.6rem;
  font-weight: 700;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.empty-tip {
  color: #6c5ce7;
  font-size: 0.95rem;
}

.result-primary.fresh {
  background: linear-gradient(135deg, rgba(0, 210, 211, 0.15) 0%, rgba(16, 172, 132, 0.15) 100%);
  border-color: rgba(0, 210, 211, 0.4);
}

.result-primary.rotten {
  background: linear-gradient(135deg, rgba(255, 107, 107, 0.15) 0%, rgba(238, 90, 36, 0.15) 100%);
  border-color: rgba(255, 107, 107, 0.4);
}

.result-icon {
  width: 70px;
  height: 70px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 25px rgba(102, 126, 234, 0.4);
}

.result-icon i {
  font-size: 2.2rem;
  color: white;
}

.result-info {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.fruit-name {
  font-size: 1.7rem;
  font-weight: 700;
  background: linear-gradient(135deg, #1a1a2e 0%, #4a4a6a 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.freshness-tag {
  padding: 6px 18px;
  border-radius: 20px;
  font-size: 0.95rem;
  font-weight: 600;
}

.freshness-tag, .status.fresh {
  background: linear-gradient(135deg, #00d2d3 0%, #10ac84 100%);
  color: white;
}

.freshness-tag, .status.rotten {
  background: linear-gradient(135deg, #ff6b6b 0%, #ee5a24 100%);
  color: white;
}

.confidence-value {
  color: #6c5ce7;
  font-weight: 600;
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

/* 统计卡片 - 多巴胺风格 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 18px;
}

.stat-item {
  text-align: center;
  padding: 22px 15px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.9) 0%, rgba(255, 255, 255, 0.7) 100%);
  border-radius: 18px;
  border: 2px solid transparent;
  transition: all 0.3s;
  position: relative;
  overflow: hidden;
}

.stat-item::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #667eea, #764ba2);
}

.stat-item:hover {
  transform: translateY(-5px);
  box-shadow: 0 15px 35px rgba(102, 126, 234, 0.2);
}

.stat-item.fresh {
  background: linear-gradient(135deg, rgba(0, 210, 211, 0.15) 0%, rgba(16, 172, 132, 0.15) 100%);
  border-color: rgba(0, 210, 211, 0.3);
}

.stat-item.fresh::before {
  background: linear-gradient(90deg, #00d2d3, #10ac84);
}

.stat-item.rotten {
  background: linear-gradient(135deg, rgba(255, 107, 107, 0.15) 0%, rgba(238, 90, 36, 0.15) 100%);
  border-color: rgba(255, 107, 107, 0.3);
}

.stat-item.rotten::before {
  background: linear-gradient(90deg, #ff6b6b, #ee5a24);
}

.stat-value {
  font-size: 2.4rem;
  font-weight: 800;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  display: block;
}

.stat-item.fresh .stat-value {
  background: linear-gradient(135deg, #00d2d3 0%, #10ac84 100%);
  -webkit-background-clip: text;
  background-clip: text;
}

.stat-item.rotten .stat-value {
  background: linear-gradient(135deg, #ff6b6b 0%, #ee5a24 100%);
  -webkit-background-clip: text;
  background-clip: text;
}

.stat-label {
  color: #6c5ce7;
  font-size: 0.95rem;
  font-weight: 500;
  margin-top: 5px;
}

/* 摄像头区域 - 多巴胺风格 */
.camera-container {
  position: relative;
  width: 100%;
  max-width: 640px;
  margin: 0 auto;
  border-radius: 20px;
  overflow: hidden;
  min-height: 500px;
  background: linear-gradient(135deg, #1a1a2e 0%, #2d2d44 100%);
  box-shadow: 0 15px 40px rgba(102, 126, 234, 0.3);
}

.camera-video {
  width: 100%;
  display: block;
  border-radius: 20px;
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
  border: 3px solid #764ba2;
  border-radius: 8px;
  animation: popIn 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  box-shadow: 0 0 20px rgba(118, 75, 162, 0.5);
}

.live-detection-box.fresh {
  border-color: #00d2d3;
  box-shadow: 0 0 25px rgba(0, 210, 211, 0.5);
}

.live-detection-box.rotten {
  border-color: #ff6b6b;
  box-shadow: 0 0 25px rgba(255, 107, 107, 0.5);
}

.live-detection-label {
  position: absolute;
  top: -28px;
  left: 0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 5px 14px;
  border-radius: 15px;
  font-size: 13px;
  color: white;
  white-space: nowrap;
  font-weight: 600;
}

.live-detection-label.fresh {
  background: linear-gradient(135deg, #00d2d3 0%, #10ac84 100%);
}

.live-detection-label.rotten {
  background: linear-gradient(135deg, #ff6b6b 0%, #ee5a24 100%);
}

/* 扫描线动画 - 多巴胺风格 */
.scan-line {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 4px;
  background: linear-gradient(90deg, transparent, #667eea, #f093fb, #feca57, transparent);
  animation: scan 2s linear infinite;
  box-shadow: 0 0 20px rgba(102, 126, 234, 0.8);
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
  color: white;
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.95) 0%, rgba(45, 45, 68, 0.95) 100%);
  padding: 25px;
}

.camera-placeholder i:first-child {
  font-size: 5rem;
  margin-bottom: 18px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 50%, #f093fb 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.1); opacity: 0.8; }
}

.placeholder-title {
  font-size: 1.3rem;
  margin-bottom: 12px;
  font-weight: 600;
}

.placeholder-tip {
  font-size: 0.95rem;
  color: rgba(255, 255, 255, 0.7);
  margin-bottom: 25px;
}

.placeholder-tip i {
  margin-right: 8px;
}

.permission-tips {
  text-align: left;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.2) 0%, rgba(118, 75, 162, 0.2) 100%);
  border: 2px solid rgba(102, 126, 234, 0.4);
  border-radius: 15px;
  padding: 18px 22px;
  max-width: 420px;
  width: 90%;
}

.permission-tips p {
  color: #f093fb;
  margin: 0 0 12px 0;
  font-weight: 700;
  font-size: 1rem;
}

.permission-tips ul {
  margin: 0;
  padding-left: 22px;
}

.permission-tips li {
  color: rgba(255, 255, 255, 0.85);
  font-size: 0.9rem;
  line-height: 1.9;
}

.camera-controls {
  display: flex;
  justify-content: center;
  margin-top: 25px;
}

.camera-btn {
  justify-content: center;
}

.stop-btn {
  background: linear-gradient(135deg, #ff6b6b 0%, #ee5a24 100%);
  box-shadow: 0 10px 30px rgba(255, 107, 107, 0.4);
}

.stop-btn:hover {
  box-shadow: 0 20px 50px rgba(255, 107, 107, 0.5);
}

.live-stats {
  display: flex;
  justify-content: center;
  gap: 25px;
  margin-top: 18px;
  padding: 14px 20px;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.15) 0%, rgba(118, 75, 162, 0.15) 100%);
  border-radius: 15px;
  border: 2px solid rgba(102, 126, 234, 0.3);
}

.live-stats.empty {
  background: rgba(162, 155, 254, 0.1);
  border-color: rgba(162, 155, 254, 0.3);
}

.no-fruit {
  color: #6c5ce7;
  font-size: 0.95rem;
  font-weight: 500;
}

.live-count {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  font-weight: 700;
}

.live-fresh {
  background: linear-gradient(135deg, #00d2d3 0%, #10ac84 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  font-weight: 600;
}

.live-rotten {
  background: linear-gradient(135deg, #ff6b6b 0%, #ee5a24 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  font-weight: 600;
}

/* 历史记录 - 多巴胺风格 */
.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 18px;
  margin-bottom: 22px;
}

.history-header h3 {
  border-bottom: none;
  margin-bottom: 0;
  padding-bottom: 0;
}

.clear-history-btn {
  background: linear-gradient(135deg, #ff6b6b 0%, #ee5a24 100%);
  border: none;
  color: white;
  padding: 10px 18px;
  border-radius: 25px;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  box-shadow: 0 5px 15px rgba(255, 107, 107, 0.3);
}

.clear-history-btn:hover {
  transform: translateY(-3px);
  box-shadow: 0 10px 25px rgba(255, 107, 107, 0.4);
}

.history-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 18px;
}

.history-item {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.9) 0%, rgba(255, 255, 255, 0.7) 100%);
  border-radius: 16px;
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  border: 2px solid transparent;
  transition: all 0.3s;
  position: relative;
  overflow: hidden;
}

.history-item::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, #667eea, #764ba2, #f093fb);
}

.history-item:hover {
  transform: translateY(-5px);
  box-shadow: 0 15px 35px rgba(102, 126, 234, 0.25);
}

.history-image {
  width: 100%;
  min-height: 110px;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.1) 0%, rgba(118, 75, 162, 0.1) 100%);
  position: relative;
}

.history-image img {
  width: 100%;
  height: auto;
  max-height: 150px;
  object-fit: contain;
  border-radius: 12px;
  display: block;
}

.history-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.history-time {
  color: #a29bfe;
  font-size: 0.85rem;
}

/* 检测列表 - 多巴胺风格 */
.all-detections h4 {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin: 22px 0 18px 0;
  font-weight: 600;
}

.detection-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.detection-item {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 15px 18px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.9) 0%, rgba(255, 255, 255, 0.7) 100%);
  border-radius: 14px;
  border-left: 4px solid #764ba2;
  transition: all 0.3s;
}

.detection-item:hover {
  transform: translateX(5px);
  box-shadow: 0 5px 20px rgba(102, 126, 234, 0.15);
}

.detection-item .index {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  font-weight: 800;
  font-size: 1.2rem;
  min-width: 25px;
}

.detection-item .fruit-type {
  background: linear-gradient(135deg, #1a1a2e 0%, #4a4a6a 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  font-weight: 600;
  font-size: 1.05rem;
}

.detection-item .status {
  padding: 5px 14px;
  border-radius: 15px;
  font-size: 0.9rem;
  font-weight: 600;
}

.detection-item .confidence {
  color: #6c5ce7;
  font-size: 0.9rem;
  font-weight: 600;
}

/* 图片预览 - 多巴胺风格 */
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
  border-radius: 16px;
  box-shadow: 0 10px 30px rgba(102, 126, 234, 0.3);
}

.clear-btn {
  position: absolute;
  top: 12px;
  right: 12px;
  background: linear-gradient(135deg, #ff6b6b 0%, #ee5a24 100%);
  border: none;
  border-radius: 50%;
  width: 35px;
  height: 35px;
  color: white;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 5px 15px rgba(255, 107, 107, 0.4);
}

.clear-btn:hover {
  transform: rotate(90deg) scale(1.1);
  box-shadow: 0 8px 20px rgba(255, 107, 107, 0.5);
}

/* 批量检测样式 - 多巴胺风格 */
.batch-panel {
  position: relative;
  z-index: 1;
}

.batch-section {
  margin-bottom: 25px;
}

.batch-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.batch-header h3 {
  margin-bottom: 0;
  padding-bottom: 0;
  border-bottom: none;
}

.image-count {
  color: #6c5ce7;
  font-weight: 600;
  font-size: 0.95rem;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.15), rgba(118, 75, 162, 0.15));
  padding: 6px 16px;
  border-radius: 20px;
}

.batch-upload-area {
  border: 3px dashed #c4b5fd;
  border-radius: 20px;
  padding: 45px;
  text-align: center;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  min-height: 180px;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.05) 0%, rgba(240, 147, 251, 0.05) 100%);
}

.batch-upload-area:hover {
  border-color: #764ba2;
  border-style: solid;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.1) 0%, rgba(240, 147, 251, 0.1) 100%);
  transform: scale(1.01);
  box-shadow: 0 15px 40px rgba(102, 126, 234, 0.2);
}

.batch-image-list {
  margin-top: 20px;
}

.batch-thumbs {
  display: flex;
  flex-wrap: wrap;
  gap: 15px;
  padding: 15px 0;
}

.batch-thumb {
  position: relative;
  width: 120px;
  height: 120px;
  border-radius: 16px;
  overflow: hidden;
  border: 3px solid transparent;
  background: linear-gradient(white, white) padding-box,
              linear-gradient(135deg, #667eea, #764ba2) border-box;
  transition: all 0.3s;
  box-shadow: 0 8px 20px rgba(102, 126, 234, 0.2);
}

.batch-thumb:hover {
  transform: scale(1.05) rotate(2deg);
  box-shadow: 0 15px 35px rgba(102, 126, 234, 0.35);
}

.batch-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.remove-thumb {
  position: absolute;
  top: 8px;
  right: 8px;
  background: linear-gradient(135deg, #ff6b6b, #ee5a24);
  border: none;
  border-radius: 50%;
  width: 28px;
  height: 28px;
  color: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.75rem;
  box-shadow: 0 4px 12px rgba(255, 107, 107, 0.5);
  transition: transform 0.2s;
}

.remove-thumb:hover {
  transform: scale(1.2) rotate(90deg);
}

.batch-actions {
  display: flex;
  gap: 15px;
  justify-content: center;
  padding: 20px 0 10px;
}

.add-more-btn {
  background: linear-gradient(135deg, #48dbfb, #0abde3);
  border: none;
  color: white;
  padding: 14px 30px;
  border-radius: 50px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 10px;
  box-shadow: 0 8px 25px rgba(72, 219, 251, 0.4);
  transition: all 0.3s;
}

.add-more-btn:hover {
  transform: translateY(-4px);
  box-shadow: 0 15px 35px rgba(72, 219, 251, 0.5);
}

.batch-detect-btn {
  min-width: 220px;
  justify-content: center;
}

.clear-all-btn {
  background: linear-gradient(135deg, #ff6b6b, #ee5a24);
  border: none;
  color: white;
  padding: 14px 30px;
  border-radius: 50px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 10px;
  box-shadow: 0 8px 25px rgba(255, 107, 107, 0.4);
  transition: all 0.3s;
}

.clear-all-btn:hover {
  transform: translateY(-4px);
  box-shadow: 0 15px 35px rgba(255, 107, 107, 0.5);
}

/* 批量检测结果 */
.batch-results-section {
  margin-top: 25px;
}

.batch-results-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.batch-result-item {
  display: flex;
  gap: 25px;
  padding: 20px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.9), rgba(255, 255, 255, 0.7));
  border-radius: 20px;
  border: 2px solid rgba(102, 126, 234, 0.3);
  transition: all 0.3s;
  align-items: center;
}

.batch-result-item:hover {
  transform: translateX(8px);
  box-shadow: 0 12px 30px rgba(102, 126, 234, 0.2);
}

.result-image {
  flex-shrink: 0;
  width: auto;
  height: 120px;
  border-radius: 16px;
  overflow: hidden;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.1), rgba(118, 75, 162, 0.1));
  box-shadow: 0 8px 20px rgba(102, 126, 234, 0.2);
}

.result-image img {
  height: 100%;
  width: auto;
  display: block;
  border-radius: 0;
  box-shadow: none;
}

.result-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #a29bfe;
  font-size: 2rem;
}

.result-info-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.result-tags {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  align-items: center;
}

.tag {
  padding: 8px 18px;
  border-radius: 25px;
  font-weight: 600;
  font-size: 0.95rem;
}

.tag.fruit-name {
  background: linear-gradient(135deg, #1a1a2e, #4a4a6a);
  color: white;
  -webkit-text-fill-color: white;
  -webkit-background-clip: border-box;
  background-clip: border-box;
}

.tag.tag-freshness {
  color: white;
}

.tag.tag-freshness.fresh {
  background: linear-gradient(135deg, #00d2d3, #10ac84);
}

.tag.tag-freshness.rotten {
  background: linear-gradient(135deg, #ff6b6b, #ee5a24);
}

.tag.tag-confidence {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
}

.result-empty-tag {
  color: #a29bfe;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 1rem;
}

.result-count {
  color: #6c5ce7;
  font-size: 0.9rem;
  background: rgba(108, 92, 231, 0.1);
  padding: 6px 14px;
  border-radius: 15px;
  display: inline-block;
  align-self: flex-start;
}

/* 批量统计 */
.batch-stats {
  display: flex;
  gap: 18px;
  justify-content: center;
  flex-wrap: wrap;
  margin-top: 28px;
  padding-top: 22px;
  border-top: 2px solid rgba(102, 126, 234, 0.15);
}

.batch-stat-item {
  text-align: center;
  padding: 18px 24px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.95), rgba(255, 255, 255, 0.8));
  border-radius: 18px;
  border: 2px solid transparent;
  min-width: 100px;
  transition: all 0.3s;
  position: relative;
  overflow: hidden;
}

.batch-stat-item::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #667eea, #764ba2);
}

.batch-stat-item:hover {
  transform: translateY(-5px);
  box-shadow: 0 12px 30px rgba(102, 126, 234, 0.2);
}

.batch-stat-item .stat-value {
  font-size: 2rem;
  font-weight: 800;
  display: block;
  background: linear-gradient(135deg, #667eea, #764ba2);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.batch-stat-item:nth-child(2) .stat-value {
  background: linear-gradient(135deg, #48dbfb, #0abde3);
  -webkit-background-clip: text;
}

.batch-stat-item:nth-child(3) .stat-value {
  background: linear-gradient(135deg, #00d2d3, #10ac84);
  -webkit-background-clip: text;
}

.batch-stat-item:nth-child(4) .stat-value {
  background: linear-gradient(135deg, #ff6b6b, #ee5a24);
  -webkit-background-clip: text;
}

.batch-stat-item:nth-child(5) .stat-value {
  background: linear-gradient(135deg, #feca57, #ff9ff3);
  -webkit-background-clip: text;
}

.batch-stat-item .stat-label {
  color: #6c5ce7;
  font-size: 0.85rem;
  font-weight: 500;
  margin-top: 5px;
}

/* 动画 */
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes scan {
  0% { top: 0; }
  100% { top: 100%; }
}

/* 响应式 */
@media (max-width: 768px) {
  .header h1 {
    font-size: 2rem;
  }

  .mode-switch {
    flex-direction: column;
    gap: 12px;
  }

  .mode-btn {
    width: 100%;
    justify-content: center;
  }

  .batch-result-item {
    flex-direction: column;
    text-align: center;
  }

  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .result-primary {
    flex-direction: column;
    text-align: center;
  }

  .batch-upload-area {
    min-height: 180px;
  }

  .batch-thumbs {
    display: flex;
    flex-wrap: wrap;
    gap: 15px;
    padding: 15px 0;
  }

  .batch-thumb {
    position: relative;
    width: 110px;
    height: 110px;
    border-radius: 14px;
    overflow: hidden;
    border: 2px solid rgba(102, 126, 234, 0.3);
    transition: all 0.3s;
    box-shadow: 0 5px 15px rgba(102, 126, 234, 0.15);
  }

  .batch-thumb:hover {
    transform: scale(1.05);
    box-shadow: 0 10px 25px rgba(102, 126, 234, 0.3);
  }

  .batch-thumb img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  .remove-thumb {
    position: absolute;
    top: 5px;
    right: 5px;
    background: linear-gradient(135deg, #ff6b6b, #ee5a24);
    border: none;
    border-radius: 50%;
    width: 25px;
    height: 25px;
    color: white;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 0.7rem;
    box-shadow: 0 3px 10px rgba(255, 107, 107, 0.5);
    transition: transform 0.2s;
  }

  .remove-thumb:hover {
    transform: scale(1.2);
  }

  .batch-actions {
    display: flex;
    gap: 15px;
    justify-content: center;
    flex-wrap: wrap;
    padding: 15px 0 5px;
  }

  .add-more-btn {
    background: linear-gradient(135deg, #48dbfb, #0abde3);
    border: none;
    color: white;
    padding: 14px 28px;
    border-radius: 50px;
    font-size: 0.95rem;
    font-weight: 600;
    cursor: pointer;
    display: flex;
    align-items: center;
    gap: 8px;
    box-shadow: 0 8px 20px rgba(72, 219, 251, 0.35);
    transition: all 0.3s;
  }

  .add-more-btn:hover {
    transform: translateY(-3px);
    box-shadow: 0 12px 30px rgba(72, 219, 251, 0.45);
  }

  .batch-detect-btn {
    min-width: 200px;
  }

  .clear-all-btn {
    background: linear-gradient(135deg, #ff6b6b, #ee5a24);
    border: none;
    color: white;
    padding: 14px 28px;
    border-radius: 50px;
    font-size: 0.95rem;
    font-weight: 600;
    cursor: pointer;
    display: flex;
    align-items: center;
    gap: 8px;
    box-shadow: 0 8px 20px rgba(255, 107, 107, 0.35);
    transition: all 0.3s;
  }

  .clear-all-btn:hover {
    transform: translateY(-3px);
    box-shadow: 0 12px 30px rgba(255, 107, 107, 0.45);
  }

  /* Batch results */
  .batch-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 15px;
  }

  .batch-header h3 {
    margin-bottom: 0;
    padding-bottom: 0;
    border-bottom: none;
  }

  .image-count {
    color: #6c5ce7;
    font-weight: 600;
    font-size: 0.95rem;
    background: linear-gradient(135deg, rgba(102, 126, 234, 0.15), rgba(118, 75, 162, 0.15));
    padding: 6px 16px;
    border-radius: 20px;
  }

  .batch-results-list {
    display: flex;
    flex-direction: column;
    gap: 18px;
  }

  .batch-result-item {
    display: flex;
    gap: 20px;
    padding: 15px;
    background: linear-gradient(135deg, rgba(255, 255, 255, 0.85), rgba(255, 255, 255, 0.6));
    border-radius: 16px;
    border: 2px solid rgba(102, 126, 234, 0.15);
    transition: all 0.3s;
    align-items: center;
  }

  .batch-result-item:hover {
    transform: scale(1.01);
    box-shadow: 0 8px 25px rgba(102, 126, 234, 0.2);
  }

  .result-image {
    flex-shrink: 0;
    width: auto;
    height: 100px;
    border-radius: 14px;
    overflow: hidden;
    background: linear-gradient(135deg, rgba(102, 126, 234, 0.1), rgba(118, 75, 162, 0.1));
    box-shadow: 0 5px 15px rgba(102, 126, 234, 0.2);
  }

  .result-image img {
    height: 100%;
    width: auto;
    display: block;
    border-radius: 0;
    box-shadow: none;
  }

  .result-placeholder {
    display: flex;
    align-items: center;
    justify-content: center;
    height: 100%;
    color: #a29bfe;
  }

  .result-info-card {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  .result-tags {
    display: flex;
    gap: 10px;
    flex-wrap: wrap;
    align-items: center;
  }

  .tag {
    padding: 6px 16px;
    border-radius: 20px;
    font-weight: 600;
    font-size: 0.9rem;
  }

  .tag.fruit-name {
    background: linear-gradient(135deg, #1a1a2e, #4a4a6a);
    color: white;
    -webkit-text-fill-color: white;
    -webkit-background-clip: border-box;
    background-clip: border-box;
  }

  .tag.tag-freshness {
    color: white;
  }
  .tag.tag-freshness.fresh {
    background: linear-gradient(135deg, #00d2d3, #10ac84);
  }
  .tag.tag-freshness.rotten {
    background: linear-gradient(135deg, #ff6b6b, #ee5a24);
  }

  .tag.tag-confidence {
    background: linear-gradient(135deg, #667eea, #764ba2);
    color: white;
  }

  .result-empty-tag {
    color: #a29bfe;
    font-weight: 500;
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .result-count {
    color: #6c5ce7;
    font-size: 0.85rem;
    background: rgba(108, 92, 231, 0.1);
    padding: 4px 12px;
    border-radius: 12px;
    display: inline-block;
    align-self: flex-start;
  }

  /* Batch stats */
  .batch-stats {
    display: flex;
    gap: 15px;
    justify-content: center;
    flex-wrap: wrap;
    margin-top: 25px;
    padding-top: 20px;
    border-top: 2px solid rgba(102, 126, 234, 0.15);
  }

  .batch-stat-item {
    text-align: center;
    padding: 15px 20px;
    background: linear-gradient(135deg, rgba(255, 255, 255, 0.9), rgba(255, 255, 255, 0.7));
    border-radius: 15px;
    border: 2px solid transparent;
    min-width: 90px;
    transition: all 0.3s;
  }

  .batch-stat-item:hover {
    transform: translateY(-3px);
  }

  .batch-stat-item .stat-value {
    font-size: 1.8rem;
    font-weight: 800;
    display: block;
    background: linear-gradient(135deg, #667eea, #764ba2);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
  }

  .batch-stat-item:nth-child(2) .stat-value {
    background: linear-gradient(135deg, #48dbfb, #0abde3);
    -webkit-background-clip: text;
  }

  .batch-stat-item:nth-child(3) .stat-value {
    background: linear-gradient(135deg, #00d2d3, #10ac84);
    -webkit-background-clip: text;
  }

  .batch-stat-item:nth-child(4) .stat-value {
    background: linear-gradient(135deg, #ff6b6b, #ee5a24);
    -webkit-background-clip: text;
  }

  .batch-stat-item:nth-child(5) .stat-value {
    background: linear-gradient(135deg, #feca57, #ff9ff3);
    -webkit-background-clip: text;
  }

  .batch-stat-item .stat-label {
    color: #6c5ce7;
    font-size: 0.8rem;
    font-weight: 500;
    margin-top: 5px;
  }
}
</style>