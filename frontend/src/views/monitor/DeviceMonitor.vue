<template>
  <div class="monitor-page">
    <el-row :gutter="20">
      <el-col :span="16">
        <el-card>
          <template #header>
            <span>实时轨迹</span>
          </template>
          <div class="map-container" ref="mapContainer">
            <p>地图区域（待集成地图组件）</p>
            <div class="trajectory-info">
              <p>当前坐标: {{ currentPosition }}</p>
              <p>最后更新: {{ lastUpdate }}</p>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <template #header>
            <span>设备状态</span>
          </template>
          <el-table :data="devices" style="width: 100%">
            <el-table-column prop="deviceCode" label="设备编码" />
            <el-table-column prop="status" label="状态">
              <template #default="{ row }">
                <el-tag :type="row.online ? 'success' : 'info'">
                  {{ row.online ? '在线' : '离线' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import request from '@/utils/request'

const mapContainer = ref()
const currentPosition = ref('--')
const lastUpdate = ref('--')
const devices = ref<any[]>([])

let eventSource: EventSource | null = null

async function loadDevices() {
  try {
    const data = await request.get('/devices', { params: { page: 1, size: 10 } })
    devices.value = data.list || []
  } catch (error) {
    console.error('加载设备列表失败', error)
  }
}

function startSSE() {
  // SSE 连接（示例）
  // eventSource = new EventSource('/api/v1/sse/devices/1/trajectory/stream')
  // eventSource.onmessage = (event) => {
  //   const data = JSON.parse(event.data)
  //   currentPosition.value = `${data.lat}, ${data.lng}`
  //   lastUpdate.value = new Date().toLocaleString()
  // }
}

onMounted(() => {
  loadDevices()
  startSSE()
})

onUnmounted(() => {
  if (eventSource) {
    eventSource.close()
  }
})
</script>

<style scoped>
.monitor-page {
  padding: 20px;
}
.map-container {
  height: 400px;
  background: #f5f5f5;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
}
.trajectory-info {
  margin-top: 20px;
  text-align: center;
}
</style>
