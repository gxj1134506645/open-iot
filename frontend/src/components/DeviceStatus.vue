<template>
  <div class="device-status">
    <div class="status-header">
      <h3>设备状态</h3>
      <el-button
        size="small"
        @click="refreshStatus"
        :loading="loading"
      >
        刷新
      </el-button>
    </div>

    <div class="status-content">
      <!-- 设备基本信息 -->
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="设备ID">
          {{ device?.id }}
        </el-descriptions-item>
        <el-descriptions-item label="设备编码">
          {{ device?.deviceCode }}
        </el-descriptions-item>
        <el-descriptions-item label="设备名称">
          {{ device?.deviceName }}
        </el-descriptions-item>
        <el-descriptions-item label="协议类型">
          <el-tag size="small">{{ device?.protocolType }}</el-tag>
        </el-descriptions-item>
      </el-descriptions>

      <!-- 在线状态 -->
      <div class="status-indicator">
        <div class="indicator-label">在线状态</div>
        <div class="indicator-value">
          <el-tag
            :type="status?.online ? 'success' : 'info'"
            size="large"
            effect="dark"
          >
            <el-icon v-if="status?.online">
              <CircleCheck />
            </el-icon>
            <el-icon v-else>
              <CircleClose />
            </el-icon>
            {{ status?.online ? '在线' : '离线' }}
          </el-tag>
        </div>
      </div>

      <!-- 最后在线时间 -->
      <div class="status-indicator">
        <div class="indicator-label">最后在线</div>
        <div class="indicator-value">
          {{ lastSeenTime }}
        </div>
      </div>

      <!-- 设备状态（启用/禁用） -->
      <div class="status-indicator">
        <div class="indicator-label">设备状态</div>
        <div class="indicator-value">
          <el-tag
            :type="device?.status === '1' ? 'success' : 'danger'"
            size="small"
          >
            {{ device?.status === '1' ? '启用' : '禁用' }}
          </el-tag>
        </div>
      </div>

      <!-- 操作按钮 -->
      <div class="status-actions">
        <el-button
          v-if="device?.status === '1'"
          type="danger"
          size="small"
          @click="handleDisableDevice"
        >
          禁用设备
        </el-button>
        <el-button
          v-else
          type="success"
          size="small"
          @click="handleEnableDevice"
        >
          启用设备
        </el-button>
        <el-button
          type="warning"
          size="small"
          @click="handleRefreshToken"
        >
          刷新 Token
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { CircleCheck, CircleClose } from '@element-plus/icons-vue';
import type { Device, DeviceStatus } from '@/stores/device';
import { formatTimestamp } from '@/utils/format';

/**
 * 组件属性
 */
interface Props {
  device?: Device;
  deviceId?: number;
}

const props = defineProps<Props>();
const emit = defineEmits<{
  (e: 'refresh'): void;
  (e: 'update:device', device: Device): void;
}>();

// ==================== 状态 ====================

const loading = ref(false);
const status = ref<DeviceStatus | null>(null);

// ==================== 计算属性 ====================

/**
 * 最后在线时间
 */
const lastSeenTime = computed(() => {
  if (!status.value?.lastSeen) {
    return '暂无数据';
  }
  return formatTimestamp(status.value.lastSeen);
});

// ==================== 方法 ====================

/**
 * 刷新设备状态
 */
async function refreshStatus() {
  if (!props.deviceId && !props.device?.id) {
    ElMessage.warning('设备ID不存在');
    return;
  }

  loading.value = true;

  try {
    // TODO: 调用 API 获取设备状态
    // const response = await request.get(`/api/device/status/${deviceId}`);
    // status.value = response.data;

    // 模拟数据
    status.value = {
      deviceId: props.device?.id || props.deviceId!,
      deviceCode: props.device?.deviceCode || '',
      online: Math.random() > 0.5,
      lastSeen: Date.now(),
    };

    ElMessage.success('刷新成功');
    emit('refresh');
  } catch (error) {
    console.error('[DeviceStatus] 刷新失败:', error);
    ElMessage.error('刷新失败');
  } finally {
    loading.value = false;
  }
}

/**
 * 禁用设备
 */
async function handleDisableDevice() {
  try {
    await ElMessageBox.confirm('确定要禁用该设备吗？禁用后设备将无法连接平台。', '确认禁用', {
      type: 'warning',
    });

    // TODO: 调用 API 禁用设备
    ElMessage.success('设备已禁用');
    emit('refresh');
  } catch (error) {
    if (error !== 'cancel') {
      console.error('[DeviceStatus] 禁用失败:', error);
      ElMessage.error('操作失败');
    }
  }
}

/**
 * 启用设备
 */
async function handleEnableDevice() {
  try {
    await ElMessageBox.confirm('确定要启用该设备吗？', '确认启用', {
      type: 'info',
    });

    // TODO: 调用 API 启用设备
    ElMessage.success('设备已启用');
    emit('refresh');
  } catch (error) {
    if (error !== 'cancel') {
      console.error('[DeviceStatus] 启用失败:', error);
      ElMessage.error('操作失败');
    }
  }
}

/**
 * 刷新设备 Token
 */
async function handleRefreshToken() {
  try {
    const { value } = await ElMessageBox.prompt('确定要刷新设备 Token 吗？刷新后旧 Token 将立即失效。', '刷新 Token', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputPattern: /confirm/,
      inputErrorMessage: '请输入 "confirm" 确认操作',
    });

    if (value === 'confirm') {
      // TODO: 调用 API 刷新 Token
      ElMessage.success('Token 已刷新');
      emit('refresh');
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('[DeviceStatus] 刷新 Token 失败:', error);
      ElMessage.error('操作失败');
    }
  }
}

// ==================== 生命周期 ====================

onMounted(() => {
  refreshStatus();
});

// 监听设备ID变化
watch(
  () => props.deviceId || props.device?.id,
  () => {
    refreshStatus();
  }
);
</script>

<style scoped lang="scss">
.device-status {
  background: #fff;
  border-radius: 4px;
  padding: 16px;

  .status-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;

    h3 {
      margin: 0;
      font-size: 16px;
      font-weight: 600;
    }
  }

  .status-content {
    .status-indicator {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 12px 0;
      border-bottom: 1px solid #e4e7ed;

      &:last-of-type {
        border-bottom: none;
      }

      .indicator-label {
        font-size: 14px;
        color: #606266;
      }

      .indicator-value {
        display: flex;
        align-items: center;
        gap: 8px;
        font-size: 14px;
        color: #303133;
      }
    }

    .status-actions {
      margin-top: 20px;
      display: flex;
      gap: 8px;
    }
  }
}
</style>
