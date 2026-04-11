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
    }
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
    addToHistory(state, result) {
      const detections = result.detections || []
      const sourceImage = result.imageUrl || result.imageData || state.imageUrl || null
      const thumbnailUrl = result.annotatedImage || sourceImage

      if (detections.length === 0) {
        const historyItem = {
          id: Date.now(),
          imageUrl: sourceImage,
          thumbnailUrl,
          fruitType: result.fruitType || 'unknown',
          freshness: result.freshness || 'fresh',
          confidence: result.confidence || 0,
          timestamp: new Date().toLocaleString()
        }
        state.detectionHistory.unshift(historyItem)
        if (state.detectionHistory.length > 10) {
          state.detectionHistory = state.detectionHistory.slice(0, 10)
        }
        state.statistics.total++
        if (result.freshness === 'fresh') {
          state.statistics.fresh++
        } else {
          state.statistics.rotten++
        }
        return
      }

      detections.forEach(box => {
        const historyItem = {
          id: Date.now() + Math.random(),
          imageUrl: sourceImage,
          thumbnailUrl,
          fruitType: box.fruitType || 'unknown',
          freshness: box.freshness || 'fresh',
          confidence: box.confidence || 0,
          timestamp: new Date().toLocaleString()
        }
        state.detectionHistory.unshift(historyItem)
      })

      if (state.detectionHistory.length > 10) {
        state.detectionHistory = state.detectionHistory.slice(0, 10)
      }

      state.statistics.total += detections.length
      detections.forEach(box => {
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
    async detectFruit({ commit }, imageUrl) {
      commit('setIsDetecting', true)
      try {
        const response = await this._vm.$http.post('/api/detect', { imageUrl })
        const result = response.data
        commit('setDetectionResult', result)
        commit('addToHistory', result)
      } catch (error) {
        console.error('检测失败', error)
        commit('setDetectionResult', null)
      } finally {
        commit('setIsDetecting', false)
      }
    }
  }
})
