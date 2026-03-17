import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import { resolve } from 'path'
import autoprefixer from 'autoprefixer'

export default defineConfig({
  plugins: [
    vue(),
    AutoImport({
      resolvers: [ElementPlusResolver()],
    }),
    Components({
      resolvers: [ElementPlusResolver()],
    }),
  ],
  css: {
    // 内联 PostCSS 配置，避免外部 postcss.config.js ESM 加载失败
    postcss: {
      plugins: [autoprefixer()],
    },
  },
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src'),
    },
  },
  build: {
    // 代码分割优化：将大型第三方库拆分为独立 chunk，避免单个文件过大
    rollupOptions: {
      output: {
        manualChunks: {
          // Vue 核心生态（vue + vue-router + pinia）
          'vue-vendor': ['vue', 'vue-router', 'pinia'],
          // Element Plus UI 框架
          'element-plus': ['element-plus', '@element-plus/icons-vue'],
          // ECharts 图表库（按需导入，仅含 line + pie）
          'echarts': ['echarts/core', 'echarts/charts', 'echarts/components', 'echarts/renderers'],
          // 工具库
          'utils': ['axios', 'dayjs', '@vueuse/core'],
        },
      },
    },
    // Element Plus 独立 chunk 约 900KB 属正常范围，不再警告
    chunkSizeWarningLimit: 1000,
  },
  server: {
    port: 5173,
    hmr: {
      overlay: false, // 关闭 CSS 错误遮罩，避免 PostCSS 遗留错误阻塞页面
    },
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
