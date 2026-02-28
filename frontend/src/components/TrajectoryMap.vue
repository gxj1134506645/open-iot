<template>
  <div class="trajectory-map">
    <div class="map-header">
      <h3>设备轨迹</h3>
      <div class="map-controls">
        <el-button-group>
          <el-button
            :type="autoRefresh ? 'primary' : 'default'"
            size="small"
            @click="toggleAutoRefresh"
          >
            {{ autoRefresh ? '停止刷新' : '自动刷新' }}
          </el-button>
          <el-button
            size="small"
            @click="clearTrajectory"
          >
            清空轨迹
          </el-button>
        </el-button-group>
      </div>
    </div>

    <div class="map-container" ref="mapContainer">
      <!-- TODO: 集成地图组件（如高德地图、百度地图） -->
      <div class="map-placeholder">
        <el-empty description="地图组件待集成">
          <el-button type="primary" @click="showTrajectoryData">
            查看轨迹数据
          </el-button>
        </el-empty>
      </div>
    </div>

    <div class="trajectory-info">
      <el-descriptions :column="3" border size="small">
        <el-descriptions-item label="设备ID">
          {{ deviceId }}
        </el-descriptions-item>
        <el-descriptions-item label="轨迹点数">
          {{ trajectoryPoints.length }}
        </el-descriptions-item>
        <el-descriptions-item label="最后更新">
          {{ lastUpdateTime }}
        </el-descriptions-item>
      </el-descriptions>
    </div>

    <!-- 轨迹数据对话框 -->
    <el-dialog
      v-model="showDataDialog"
      title="轨迹数据"
      width="80%"
      :close-on-click-modal="false"
    >
      <el-table :data="trajectoryPoints" border max-height="400">
        <el-table-column prop="timestamp" label="时间" width="180">
          <template #default="{ row }">
            {{ formatTimestamp(row.timestamp) }}
          </template>
        </el-table-column>
        <el-table-column prop="latitude" label="纬度" width="120" />
        <el-table-column prop="longitude" label="经度" width="120" />
        <el-table-column prop="altitude" label="海拔(m)" width="100" />
        <el-table-column prop="speed" label="速度(km/h)" width="120" />
        <el-table-column prop="direction" label="方向(°)" width="100" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue';
import { useDeviceStore, type TrajectoryPoint } from '@/stores/device';
import { formatTimestamp } from '@/utils/format';

/**
 * 组件属性
 */
interface Props {
  deviceId: number;
  autoRefresh?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  autoRefresh: false,
});

const emit = defineEmits<{
  (e: 'point-click', point: TrajectoryPoint): void;
}>();

// ==================== 状态 ====================

const deviceStore = useDeviceStore();
const mapContainer = ref<HTMLElement>();
const autoRefresh = ref(props.autoRefresh);
const showDataDialog = ref(false);
let refreshTimer: number | null = null;

// ==================== 计算属性 ====================

/**
 * 轨迹点列表
 */
const trajectoryPoints = computed<TrajectoryPoint[]>(() => {
  return deviceStore.getDeviceTrajectory(props.deviceId);
});

/**
 * 最后更新时间
 */
const lastUpdateTime = computed(() => {
  if (trajectoryPoints.value.length === 0) {
    return '暂无数据';
  }
  const lastPoint = trajectoryPoints.value[trajectoryPoints.value.length - 1];
  return formatTimestamp(lastPoint.timestamp);
});

// ==================== 方法 ====================

/**
 * 切换自动刷新
 */
function toggleAutoRefresh() {
  autoRefresh.value = !autoRefresh.value;

  if (autoRefresh.value) {
    startAutoRefresh();
  } else {
    stopAutoRefresh();
  }
}

/**
 * 开始自动刷新
 */
function startAutoRefresh() {
  stopAutoRefresh();
  refreshTimer = window.setInterval(() => {
    // 触发轨迹刷新
    console.log('[TrajectoryMap] 自动刷新轨迹');
  }, 3000);
}

/**
 * 停止自动刷新
 */
function stopAutoRefresh() {
  if (refreshTimer) {
    clearInterval(refreshTimer);
    refreshTimer = null;
  }
}

/**
 * 清空轨迹
 */
function clearTrajectory() {
  deviceStore.clearDeviceTrajectory(props.deviceId);
}

/**
 * 显示轨迹数据
 */
function showTrajectoryData() {
  showDataDialog.value = true;
}

/**
 * 处理轨迹点点击
 */
function handlePointClick(point: TrajectoryPoint) {
  emit('point-click', point);
}

// ==================== 生命周期 ====================

onMounted(() => {
  if (autoRefresh.value) {
    startAutoRefresh();
  }
});

onUnmounted(() => {
  stopAutoRefresh();
});

// 监听设备ID变化
watch(
  () => props.deviceId,
  (newDeviceId) => {
    console.log('[TrajectoryMap] 设备ID变化:', newDeviceId);
    clearTrajectory();
  }
);
</script>

<style scoped lang="scss">
.trajectory-map {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 4px;

  .map-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 16px;
    border-bottom: 1px solid #e4e7ed;

    h3 {
      margin: 0;
      font-size: 16px;
      font-weight: 600;
    }
  }

  .map-container {
    flex: 1;
    position: relative;
    min-height: 400px;

    .map-placeholder {
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      display: flex;
      align-items: center;
      justify-content: center;
      background: #f5f7fa;
    }
  }

  .trajectory-info {
    padding: 16px;
    border-top: 1px solid #e4e7ed;
  }
}
</style>
