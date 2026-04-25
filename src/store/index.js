import Vue from 'vue'
import Vuex from 'vuex'

Vue.use(Vuex)

export default new Vuex.Store({
  state: {
    detectionResult: null,
    isDetecting: false,
    imageUrl: null,
    detectionHistory: [],
    statistics: {
      total: 0,
      fresh: 0,
      rotten: 0
    },
    // 批量检测相关状态
    batchImages: [],
    batchResults: null,
    isBatchDetecting: false,
    // 摄像头相关状态
    cameraActive: false,
    cameraStream: null,
    detectionInterval: null,
    detectionBoxes: []
  },
  mutations: {
    setDetectionResult(state, result) {
      state.detectionResult = result
    },
    clearDetectionResult(state) {
      state.detectionResult = null
    },
    setIsDetecting(state, status) {
      state.isDetecting = status
    },
    setImageUrl(state, url) {
      state.imageUrl = url
    },
    // 批量检测 mutations
    setBatchImages(state, images) {
      state.batchImages = images
    },
    addBatchImage(state, image) {
      if (state.batchImages.length < 10) {
        state.batchImages.push(image)
      }
    },
    removeBatchImage(state, index) {
      state.batchImages.splice(index, 1)
    },
    clearBatchImages(state) {
      state.batchImages = []
      state.batchResults = null
    },
    setBatchResults(state, results) {
      state.batchResults = results
    },
    setIsBatchDetecting(state, status) {
      state.isBatchDetecting = status
    },
    appendToHistory(state, result) {
      const detections = result.detections || []
      if (detections.length === 0 || !result.fruitType) return

      const sourceImage = result.imageUrl || result.imageData || null
      const thumbnailUrl = result.annotatedImage || sourceImage

      const historyItem = {
        id: Date.now() + Math.random(),
        imageUrl: sourceImage,
        thumbnailUrl,
        fruitType: result.fruitType,
        freshness: result.freshness,
        confidence: result.confidence || 0,
        detections: detections,
        timestamp: new Date().toLocaleString()
      }
      state.detectionHistory.unshift(historyItem)

      if (state.detectionHistory.length > 10) {
        state.detectionHistory = state.detectionHistory.slice(0, 10)
      }

      const validDetections = detections.filter(d => d.fruitType)
      state.statistics.total += validDetections.length
      validDetections.forEach(box => {
        if (box.freshness === 'fresh') {
          state.statistics.fresh++
        } else {
          state.statistics.rotten++
        }
      })
    },
    // 摄像头相关 mutations
    setCameraActive(state, active) {
      state.cameraActive = active
    },
    setCameraStream(state, stream) {
      state.cameraStream = stream
    },
    setDetectionInterval(state, interval) {
      state.detectionInterval = interval
    },
    setDetectionBoxes(state, boxes) {
      state.detectionBoxes = boxes || []
    },
    clearCameraState(state) {
      if (state.detectionInterval) {
        clearInterval(state.detectionInterval)
      }
      if (state.cameraStream) {
        state.cameraStream.getTracks().forEach(track => track.stop())
      }
      state.cameraActive = false
      state.cameraStream = null
      state.detectionInterval = null
      state.detectionBoxes = []
    },
    addToHistory(state, result) {
      const detections = result.detections || []

      // 无检测结果时不添加历史记录
      if (detections.length === 0 || !result.fruitType) {
        return
      }

      const sourceImage = result.imageUrl || result.imageData || state.imageUrl || null
      const thumbnailUrl = result.annotatedImage || sourceImage

      const historyItem = {
        id: Date.now() + Math.random(),
        imageUrl: sourceImage,
        thumbnailUrl,
        fruitType: result.fruitType,
        freshness: result.freshness,
        confidence: result.confidence || 0,
        detections: detections,
        timestamp: new Date().toLocaleString()
      }
      state.detectionHistory.unshift(historyItem)

      if (state.detectionHistory.length > 10) {
        state.detectionHistory = state.detectionHistory.slice(0, 10)
      }

      // 只统计有效的检测结果
      const validDetections = detections.filter(d => d.fruitType)
      state.statistics.total += validDetections.length
      validDetections.forEach(box => {
        if (box.freshness === 'fresh') {
          state.statistics.fresh++
        } else {
          state.statistics.rotten++
        }
      })
    },
    clearHistory(state) {
      state.detectionHistory = []
      state.statistics = {
        total: 0,
        fresh: 0,
        rotten: 0
      }
    }
  },
  actions: {
    async detectFruit({ commit }, imageData) {
      commit('setIsDetecting', true)
      try {
        const response = await this._vm.$http.post('/detect', { imageUrl: imageData })
        const result = response.data || response
        commit('setDetectionResult', result)
        commit('addToHistory', result)
        return result
      } catch (error) {
        console.error('检测失败', error)
        commit('setDetectionResult', null)
        throw error
      } finally {
        commit('setIsDetecting', false)
      }
    },
    async batchDetect({ commit, state }) {
      commit('setIsBatchDetecting', true)
      try {
        const imageDataList = state.batchImages.map(img => img.data)
        const response = await this._vm.$http.post('/batch-detect', { imageDataList })
        const results = response.data || response
        commit('setBatchResults', results.results)
        // 将每张图的检测结果加入历史记录(append only not aggregate)
        results.results.forEach(result => {
          commit('appendToHistory', result)
        })
        return results
      } catch (error) {
        console.error('批量检测失败', error)
        commit('setBatchResults', null)
        throw error
      } finally {
        commit('setIsBatchDetecting', false)
      }
    }
  }
})