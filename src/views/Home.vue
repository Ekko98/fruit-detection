<template>
  <div class="home">
    <header class="header">
      <h1><i class="fas fa-apple-alt"></i> 水果腐烂检测系统</h1>
    </header>

    <main class="main-content">
      <div class="upload-section">
        <div class="upload-area" @click="selectImage">
          <input type="file" ref="fileInput" @change="handleFileSelect" accept="image/*" style="display: none;">
          <div v-if="!imageUrl" class="upload-placeholder">
            <i class="fas fa-cloud-upload-alt"></i>
            <p>点击上传图片或拖拽图片到此处</p>
          </div>
          <div v-else class="image-preview">
            <div class="detection-image-container">
              <img :src="imageUrl" alt="上传的图片" ref="detectionImage" @load="onImageLoad">
              <div
                v-for="(box, index) in detectionBoxes"
                :key="index"
                class="detection-box"
                :style="getBoxStyle(box)">
                <div class="detection-label">
                  {{ box.fruitType }} - {{ box.freshness }}
                </div>
              </div>
            </div>
            <button @click="clearImage" class="clear-btn">
              <i class="fas fa-times"></i>
            </button>
          </div>
        </div>

        <div class="action-buttons">
          <button @click="detectFruit" :disabled="!imageUrl || isDetecting" class="detect-btn">
            <i class="fas fa-search"></i>
            {{ isDetecting ? '检测中...' : '开始检测' }}
          </button>
        </div>
      </div>

      <div class="statistics-section" v-if="currentStatistics.total > 0">
        <div class="stats-card">
          <h3>检测统计 ({{ currentStatistics.total }} 个水果)</h3>
          <div class="stats-content">
            <div class="stat-item">
              <span>总水果数:</span>
              <span>{{ currentStatistics.total }}</span>
            </div>
            <div class="stat-item">
              <span>新鲜:</span>
              <span class="fresh">{{ currentStatistics.fresh }}</span>
            </div>
            <div class="stat-item">
              <span>腐烂:</span>
              <span class="rotten">{{ currentStatistics.rotten }}</span>
            </div>
            <div class="stat-item">
              <span>新鲜率:</span>
              <span>{{ currentStatistics.total > 0 ? ((currentStatistics.fresh / currentStatistics.total) * 100).toFixed(1) : 0 }}%</span>
            </div>
          </div>
        </div>
      </div>

      <div class="result-section" v-if="detectionResult">
        <div class="result-card">
          <h3>检测结果 (共 {{ detectionResult.detections.length }} 个)</h3>

          <!-- 主要结果（第一个） -->
          <div class="result-item primary">
            <span>主要检测:</span>
            <div class="result-details">
              <span class="fruit-type">{{ detectionResult.fruitType }}</span>
              <span :class="detectionResult.freshness === 'fresh' ? 'fresh' : 'rotten'">
                {{ detectionResult.freshness === 'fresh' ? '新鲜' : '腐烂' }}
              </span>
              <span class="confidence">{{ (detectionResult.confidence * 100).toFixed(1) }}%</span>
            </div>
          </div>

          <!-- 所有检测结果列表 -->
          <div class="all-detections">
            <h4>详细检测结果：</h4>
            <div class="detection-list" v-for="(box, index) in detectionResult.detections" :key="index">
              <div class="detection-item">
                <span class="detection-index">{{ index + 1 }}.</span>
                <div class="detection-info">
                  <span class="fruit-type">{{ box.fruitType }}</span>
                  <span :class="box.freshness === 'fresh' ? 'fresh' : 'rotten'">
                    {{ box.freshness === 'fresh' ? '新鲜' : '腐烂' }}
                  </span>
                  <span class="confidence">{{ (box.confidence * 100).toFixed(1) }}%</span>
                </div>
                <div class="detection-coords">
                  X: {{ (box.x * 100).toFixed(1) }}%, Y: {{ (box.y * 100).toFixed(1) }}%, W: {{ (box.width * 100).toFixed(1) }}%, H: {{ (box.height * 100).toFixed(1) }}%
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="history-section" v-if="detectionHistory.length > 0">
        <div class="history-card">
          <div class="history-header">
            <h3>检测历史</h3>
            <button @click="clearHistory" class="clear-history-btn">
              <i class="fas fa-trash"></i> 清空历史
            </button>
          </div>
          <div class="history-content">
            <div class="history-item" v-for="item in detectionHistory" :key="item.id">
              <div class="history-image">
                <img :src="item.imageUrl" alt="检测图片" v-if="item.imageUrl">
                <div class="placeholder" v-else>
                  <i class="fas fa-image"></i>
                </div>
              </div>
              <div class="history-details">
                <div class="history-item-header">
                  <span class="fruit-type">{{ item.fruitType }}</span>
                  <span :class="item.freshness === 'fresh' ? 'fresh' : 'rotten'">
                    {{ item.freshness === 'fresh' ? '新鲜' : '腐烂' }}
                  </span>
                  <span class="confidence">{{ (item.confidence * 100).toFixed(1) }}%</span>
                </div>
                <div class="history-item-time">{{ item.timestamp }}</div>
              </div>
            </div>
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
      isDetecting: false,
      imageLoaded: false,
      imageDimensions: { width: 0, height: 0 }
    }
  },
  computed: {
    detectionResult() {
      return this.$store.state.detectionResult
    },
    detectionHistory() {
      return this.$store.state.detectionHistory
    },
    statistics() {
      return this.$store.state.statistics
    },
    detectionBoxes() {
      if (!this.detectionResult) return []
      return this.detectionResult.detections || []
    },
    currentStatistics() {
      if (!this.detectionResult) return { total: 0, fresh: 0, rotten: 0 }
      const detections = this.detectionResult.detections || []
      return {
        total: detections.length,
        fresh: detections.filter(d => d.freshness === 'fresh').length,
        rotten: detections.filter(d => d.freshness === 'rotten').length
      }
    }
  },
  methods: {
    selectImage() {
      this.$refs.fileInput.click()
    },
    handleFileSelect(event) {
      const file = event.target.files[0]
      if (file) {
        const reader = new FileReader()
        reader.onload = (e) => {
          this.$store.commit('setImageUrl', e.target.result)
          this.$store.commit('clearDetectionResult') // 清除旧的检测结果
          this.imageUrl = e.target.result
          this.imageLoaded = false
        }
        reader.readAsDataURL(file)
      }
    },
    clearImage() {
      this.imageUrl = null
      this.$store.commit('setImageUrl', null)
      this.$store.commit('clearDetectionResult') // 清除检测结果
      this.$refs.fileInput.value = ''
      this.imageLoaded = false
    },
    async detectFruit() {
      if (this.imageUrl && this.imageLoaded) {
        await this.$store.dispatch('detectFruit', this.imageUrl)
      }
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
    getBoxStyle(box) {
      if (!box || !this.imageDimensions.width || !this.imageDimensions.height) return {}

      // YOLO坐标格式处理：相对坐标 (0-1范围)
      const relX = box.x
      const relY = box.y
      const relWidth = box.width
      const relHeight = box.height

      return {
        position: 'absolute',
        left: `${relX * 100}%`,
        top: `${relY * 100}%`,
        width: `${relWidth * 100}%`,
        height: `${relHeight * 100}%`,
        border: '2px solid #ff6b6b',
        backgroundColor: 'rgba(255, 107, 107, 0.1)',
        borderRadius: '4px'
      }
    },
    clearHistory() {
      this.$store.commit('clearHistory')
    }
  }
}
</script>

<style scoped>
.home {
  min-height: 100vh;
  padding: 20px;
  background: linear-gradient(135deg, #ffecd2 0%, #fcb69f 100%);
}

.header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 20px;
  border-radius: 15px;
  margin-bottom: 30px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  animation: fadeIn 0.5s ease-in;
}

.header h1 {
  margin: 0;
  font-size: 2rem;
}

.main-content {
  max-width: 1200px;
  margin: 0 auto;
}

.upload-section {
  background: white;
  border-radius: 15px;
  padding: 30px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  margin-bottom: 30px;
  animation: slideIn 0.5s ease-out;
}

.upload-area {
  border: 2px dashed #ddd;
  border-radius: 10px;
  padding: 40px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
  position: relative;
  margin-bottom: 20px;
}

.upload-area:hover {
  border-color: #667eea;
  background-color: #f8f9fa;
  transform: scale(1.02);
}

.upload-placeholder {
  color: #999;
}

.upload-placeholder i {
  font-size: 3rem;
  margin-bottom: 15px;
  animation: bounce 2s infinite;
}

.detection-image-container {
  position: relative;
  display: inline-block;
  max-width: 100%;
}

.image-preview img {
  max-width: 100%;
  max-height: 400px;
  border-radius: 10px;
  display: block;
  transition: transform 0.3s ease;
}

.image-preview img:hover {
  transform: scale(1.05);
}

.detection-box {
  position: absolute;
  border: 2px solid #ff6b6b;
  background-color: rgba(255, 107, 107, 0.1);
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: bold;
  font-size: 14px;
  text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.5);
  animation: fadeIn 0.3s ease-in;
}

.detection-label {
  padding: 4px 8px;
  background-color: rgba(255, 107, 107, 0.8);
  border-radius: 3px;
}

.clear-btn {
  position: absolute;
  top: 10px;
  right: 10px;
  background: rgba(255, 255, 255, 0.8);
  border: none;
  border-radius: 50%;
  width: 30px;
  height: 30px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s;
}

.clear-btn:hover {
  background: rgba(255, 255, 255, 1);
  transform: rotate(90deg);
}

.action-buttons {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.detect-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  padding: 12px 30px;
  border-radius: 25px;
  font-size: 1.1rem;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  gap: 10px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
}

.detect-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 12px rgba(0, 0, 0, 0.2);
}

.detect-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.result-section {
  background: white;
  border-radius: 15px;
  padding: 30px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  margin-bottom: 30px;
  animation: slideIn 0.5s ease-out;
}

.result-card {
  text-align: left;
}

.result-card h3 {
  margin-top: 0;
  color: #333;
  border-bottom: 2px solid #f0f0f0;
  padding-bottom: 10px;
}

.result-content {
  margin-top: 20px;
}

.result-item {
  display: flex;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px solid #f0f0f0;
}

.result-item span:first-child {
  font-weight: bold;
  color: #666;
}

.result-item.primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 15px 20px;
  border-radius: 10px;
  color: white;
  margin-bottom: 20px;
}

.result-item.primary span:first-child {
  color: white;
  font-size: 1.1rem;
}

.primary .result-details {
  display: flex;
  gap: 15px;
  align-items: center;
}

.primary .fruit-type {
  font-size: 1.3rem;
  font-weight: bold;
}

.primary .fresh {
  font-size: 1.1rem;
}

.primary .confidence {
  font-size: 1rem;
  background: rgba(255, 255, 255, 0.2);
  padding: 5px 12px;
  border-radius: 15px;
}

.all-detections h4 {
  margin: 20px 0 10px 0;
  color: #333;
  font-size: 1rem;
}

.detection-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 10px;
}

.detection-item {
  background: #f8f9fa;
  padding: 12px 15px;
  border-radius: 8px;
  border-left: 4px solid #667eea;
  transition: all 0.3s;
}

.detection-item:hover {
  background: #e9ecef;
  transform: translateX(5px);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.detection-index {
  font-weight: bold;
  color: #667eea;
  min-width: 25px;
}

.detection-info {
  display: flex;
  gap: 12px;
  align-items: center;
  flex: 1;
}

.detection-info .fruit-type {
  font-weight: bold;
  color: #333;
}

.detection-info .fresh {
  color: #28a745;
}

.detection-info .rotten {
  color: #dc3545;
}

.detection-info .confidence {
  color: #6c757d;
  font-size: 0.85rem;
}

.detection-coords {
  color: #6c757d;
  font-size: 0.85rem;
  font-family: monospace;
}

.fresh {
  color: #28a745;
}

.rotten {
  color: #dc3545;
}

.statistics-section {
  background: white;
  border-radius: 15px;
  padding: 30px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  margin-bottom: 30px;
  animation: slideIn 0.5s ease-out;
}

.stats-card {
  text-align: center;
}

.stats-card h3 {
  margin-top: 0;
  color: #333;
  border-bottom: 2px solid #f0f0f0;
  padding-bottom: 10px;
}

.stats-content {
  display: flex;
  justify-content: space-around;
  margin-top: 20px;
}

.stat-item {
  padding: 10px;
  border-radius: 8px;
  background: #f8f9fa;
  transition: all 0.3s;
}

.stat-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.stat-item span:first-child {
  font-weight: bold;
  color: #666;
  display: block;
  margin-bottom: 5px;
}

.history-section {
  background: white;
  border-radius: 15px;
  padding: 30px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  margin-bottom: 30px;
  animation: slideIn 0.5s ease-out;
}

.history-card {
  text-align: left;
}

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  border-bottom: 2px solid #f0f0f0;
  padding-bottom: 10px;
}

.history-header h3 {
  margin: 0;
  color: #333;
}

.clear-history-btn {
  background: #dc3545;
  color: white;
  border: none;
  padding: 8px 15px;
  border-radius: 20px;
  cursor: pointer;
  font-size: 0.9rem;
  transition: all 0.3s;
}

.clear-history-btn:hover {
  background: #c82333;
  transform: translateY(-1px);
}

.history-content {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 15px;
}

.history-item {
  display: flex;
  background: white;
  border: 1px solid #e9ecef;
  border-radius: 8px;
  padding: 10px;
  transition: all 0.3s;
}

.history-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.15);
  border-color: #667eea;
}

.history-image {
  width: 80px;
  height: 80px;
  border-radius: 8px;
  overflow: hidden;
  margin-right: 12px;
  flex-shrink: 0;
  border: 2px solid #e9ecef;
  background: white;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.history-image img {
  width: 100%;
  height: 100%;
  object-fit: contain; /* 使用 contain 确保完整显示图片 */
  background: white;
}

.placeholder {
  width: 100%;
  height: 100%;
  background: #e9ecef;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #6c757d;
}

.history-details {
  flex: 1;
}

.history-item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 5px;
}

.history-item-header .fruit-type {
  font-weight: bold;
  color: #333;
  font-size: 1rem;
}

.history-item-header .fresh {
  color: #28a745;
  font-weight: bold;
}

.history-item-header .rotten {
  color: #dc3545;
  font-weight: bold;
}

.history-item-header .confidence {
  color: #6c757d;
  font-size: 0.85rem;
  font-weight: 500;
}

.history-item-time {
  font-size: 0.75rem;
  color: #adb5bd;
  margin-top: 3px;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes bounce {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}
</style>