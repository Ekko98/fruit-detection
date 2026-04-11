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
    setIsDetecting(state, status) {
      state.isDetecting = status
    },
    setImageUrl(state, url) {
      state.imageUrl = url
    },
    addToHistory(state, result) {
      const historyItem = {
        id: Date.now(),
        ...result,
        timestamp: new Date().toLocaleString()
      }
      state.detectionHistory.unshift(historyItem)
      // 限制历史记录数量
      if (state.detectionHistory.length > 10) {
        state.detectionHistory = state.detectionHistory.slice(0, 10)
      }
      // 更新统计
      state.statistics.total++
      if (result.freshness === 'fresh') {
        state.statistics.fresh++
      } else {
        state.statistics.rotten++
      }
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
        console.error('检测失败:', error)
        commit('setDetectionResult', null)
      } finally {
        commit('setIsDetecting', false)
      }
    }
  }
})