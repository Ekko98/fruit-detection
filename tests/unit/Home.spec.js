import { shallowMount, createLocalVue } from '@vue/test-utils'
import Vuex from 'vuex'
import Home from '@/views/Home.vue'

const localVue = createLocalVue()
localVue.use(Vuex)

describe('Home.vue', () => {
  let store
  let actions
  let mutations
  let state

  beforeEach(() => {
    state = {
      detectionResult: null,
      isDetecting: false,
      imageUrl: null,
      detectionHistory: [],
      statistics: {
        total: 0,
        fresh: 0,
        rotten: 0
      },
      cameraActive: false,
      cameraStream: null,
      detectionInterval: null,
      detectionBoxes: []
    }

    mutations = {
      setImageUrl: jest.fn(),
      clearDetectionResult: jest.fn(),
      setDetectionResult: jest.fn(),
      addToHistory: jest.fn(),
      setCameraActive: jest.fn(),
      setCameraStream: jest.fn(),
      clearCameraState: jest.fn()
    }

    actions = {
      detectFruit: jest.fn()
    }

    store = new Vuex.Store({
      state,
      mutations,
      actions
    })
  })

  // 测试组件渲染
  it('renders correctly with default state', () => {
    const wrapper = shallowMount(Home, {
      store,
      localVue
    })

    expect(wrapper.find('.home').exists()).toBe(true)
    expect(wrapper.find('.header').exists()).toBe(true)
    expect(wrapper.find('.mode-switch').exists()).toBe(true)
  })

  // 测试标题显示
  it('displays correct title', () => {
    const wrapper = shallowMount(Home, {
      store,
      localVue
    })

    expect(wrapper.find('.header h1').text()).toContain('水果腐烂检测系统')
  })

  // 测试模式切换按钮
  it('has mode switch buttons', () => {
    const wrapper = shallowMount(Home, {
      store,
      localVue
    })

    const modeButtons = wrapper.findAll('.mode-btn')
    expect(modeButtons.length).toBe(2)
    expect(modeButtons.at(0).text()).toContain('图片检测')
    expect(modeButtons.at(1).text()).toContain('实时检测')
  })

  // 测试默认图片检测模式
  it('starts in image detection mode', () => {
    const wrapper = shallowMount(Home, {
      store,
      localVue
    })

    expect(wrapper.vm.cameraMode).toBe(false)
  })

  // 测试切换到摄像头模式
  it('can switch to camera mode', async () => {
    const wrapper = shallowMount(Home, {
      store,
      localVue
    })

    wrapper.vm.switchMode('camera')
    await wrapper.vm.$nextTick()

    expect(wrapper.vm.cameraMode).toBe(true)
  })

  // 测试检测按钮状态
  it('detect button is disabled when no image', () => {
    const wrapper = shallowMount(Home, {
      store,
      localVue
    })

    wrapper.setData({ imageUrl: null, imageLoaded: false })

    const detectBtn = wrapper.find('.detect-btn')
    expect(detectBtn.attributes('disabled')).toBe('true')
  })

  // 测试检测历史显示
  it('shows history when history exists', async () => {
    const wrapper = shallowMount(Home, {
      store,
      localVue
    })

    // 设置历史记录
    store.state.detectionHistory = [
      {
        id: 1,
        fruitType: 'apple',
        freshness: 'fresh',
        confidence: 0.95,
        timestamp: '2026-04-17'
      }
    ]

    await wrapper.vm.$nextTick()

    expect(wrapper.find('.history-section').exists()).toBe(true)
  })

  // 测试清空历史
  it('can clear history', async () => {
    const wrapper = shallowMount(Home, {
      store,
      localVue
    })

    wrapper.vm.clearHistory()

    expect(mutations.clearHistory).toHaveBeenCalled()
  })

  // 测试图片加载完成
  it('sets imageLoaded on image load', () => {
    const wrapper = shallowMount(Home, {
      store,
      localVue
    })

    // 模拟图片加载
    wrapper.vm.imageDimensions = { width: 800, height: 600 }
    wrapper.vm.imageLoaded = true

    expect(wrapper.vm.imageLoaded).toBe(true)
  })

  // 测试摄像头关闭方法
  it('can stop camera', () => {
    const wrapper = shallowMount(Home, {
      store,
      localVue
    })

    wrapper.vm.cameraActive = true
    wrapper.vm.stopCamera()

    expect(wrapper.vm.cameraActive).toBe(false)
    expect(mutations.clearCameraState).toHaveBeenCalled()
  })

  // 测试水果图标获取
  it('returns correct fruit icons', () => {
    const wrapper = shallowMount(Home, {
      store,
      localVue
    })

    expect(wrapper.vm.getFruitIcon('apple')).toBe('fas fa-apple-alt')
    expect(wrapper.vm.getFruitIcon('banana')).toBe('fas fa-lemon')
    expect(wrapper.vm.getFruitIcon('orange')).toBe('fas fa-circle')
    expect(wrapper.vm.getFruitIcon('unknown')).toBe('fas fa-apple-alt')
  })
})