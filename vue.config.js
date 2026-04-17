const { defineConfig } = require('@vue/cli-service')
const fs = require('fs')
const path = require('path')

// 证书文件在项目根目录
const certPath = path.join(__dirname, 'server.crt')
const keyPath = path.join(__dirname, 'server.key')
const hasCerts = fs.existsSync(certPath) && fs.existsSync(keyPath)

module.exports = defineConfig({
  transpileDependencies: true,
  devServer: {
    port: 3000,
    host: '0.0.0.0',
    // 如果证书存在则启用HTTPS
    https: hasCerts ? {
      key: fs.readFileSync(keyPath),
      cert: fs.readFileSync(certPath)
    } : false,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})