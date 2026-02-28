import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { subscribeDeviceRealtimeData, DeviceRealtimeData } from '@/utils/sse';
import type { SSEClient } from '@/utils/sse';

/**
 * 设备信息接口
 */
export interface Device {
  id: number;
  tenantId: number;
  deviceCode: string;
  deviceName: string;
  protocolType: string;
  status: string;
  createTime: string;
  updateTime: string;
}

/**
 * 设备状态接口
 */
export interface DeviceStatus {
  deviceId: number;
  deviceCode: string;
  online: boolean;
  lastSeen: number | null;
}

/**
 * 轨迹点接口
 */
export interface TrajectoryPoint {
  deviceId: number;
  latitude: number;
  longitude: number;
  altitude?: number;
  speed?: number;
  direction?: number;
  timestamp: number;
  extra?: string;
}

/**
 * 设备 Store
 * 管理设备列表、设备状态、实时轨迹等
 */
export const useDeviceStore = defineStore('device', () => {
  // ==================== 状态 ====================

  /**
   * 设备列表
   */
  const devices = ref<Device[]>([]);

  /**
   * 设备状态映射（deviceId -> DeviceStatus）
   */
  const deviceStatusMap = ref<Map<number, DeviceStatus>>(new Map());

  /**
   * 设备轨迹映射（deviceId -> TrajectoryPoint[]）
   */
  const deviceTrajectoryMap = ref<Map<number, TrajectoryPoint[]>>(new Map());

  /**
   * SSE 客户端实例
   */
  const sseClient = ref<SSEClient | null>(null);

  /**
   * 当前租户 ID
   */
  const currentTenantId = ref<string>('');

  // ==================== 计算属性 ====================

  /**
   * 在线设备列表
   */
  const onlineDevices = computed(() => {
    return devices.value.filter((device) => {
      const status = deviceStatusMap.value.get(device.id);
      return status?.online;
    });
  });

  /**
   * 在线设备数量
   */
  const onlineDeviceCount = computed(() => onlineDevices.value.length);

  /**
   * 设备总数
   */
  const totalDeviceCount = computed(() => devices.value.length);

  // ==================== 方法 ====================

  /**
   * 设置设备列表
   */
  function setDevices(deviceList: Device[]) {
    devices.value = deviceList;
  }

  /**
   * 更新设备状态
   */
  function updateDeviceStatus(status: DeviceStatus) {
    deviceStatusMap.value.set(status.deviceId, status);
  }

  /**
   * 批量更新设备状态
   */
  function batchUpdateDeviceStatus(statusList: DeviceStatus[]) {
    statusList.forEach((status) => {
      deviceStatusMap.value.set(status.deviceId, status);
    });
  }

  /**
   * 添加轨迹点
   */
  function addTrajectoryPoint(deviceId: number, point: TrajectoryPoint) {
    const trajectory = deviceTrajectoryMap.value.get(deviceId) || [];
    trajectory.push(point);

    // 限制轨迹点数量（保留最近 100 个点）
    if (trajectory.length > 100) {
      trajectory.shift();
    }

    deviceTrajectoryMap.value.set(deviceId, [...trajectory]);
  }

  /**
   * 设置设备轨迹
   */
  function setDeviceTrajectory(deviceId: number, trajectory: TrajectoryPoint[]) {
    deviceTrajectoryMap.value.set(deviceId, trajectory);
  }

  /**
   * 获取设备轨迹
   */
  function getDeviceTrajectory(deviceId: number): TrajectoryPoint[] {
    return deviceTrajectoryMap.value.get(deviceId) || [];
  }

  /**
   * 清空设备轨迹
   */
  function clearDeviceTrajectory(deviceId: number) {
    deviceTrajectoryMap.value.delete(deviceId);
  }

  /**
   * 连接 SSE 实时数据流
   */
  function connectRealtimeData(tenantId: string) {
    // 断开旧连接
    disconnectRealtimeData();

    currentTenantId.value = tenantId;

    // 创建 SSE 连接
    sseClient.value = subscribeDeviceRealtimeData(
      tenantId,
      handleRealtimeData,
      {
        onOpen: () => {
          console.log('[DeviceStore] SSE 连接成功');
        },
        onError: (error) => {
          console.error('[DeviceStore] SSE 连接错误:', error);
        },
        onReconnect: (attempt) => {
          console.log(`[DeviceStore] SSE 重连中 (${attempt}/5)`);
        },
      }
    );
  }

  /**
   * 断开 SSE 连接
   */
  function disconnectRealtimeData() {
    if (sseClient.value) {
      sseClient.value.disconnect();
      sseClient.value = null;
    }
  }

  /**
   * 处理实时数据
   */
  function handleRealtimeData(data: DeviceRealtimeData) {
    console.log('[DeviceStore] 接收实时数据:', data);

    switch (data.eventType) {
      case 'status':
        // 更新设备状态
        updateDeviceStatus({
          deviceId: data.deviceId,
          deviceCode: data.deviceCode,
          online: data.payload.online,
          lastSeen: data.timestamp,
        });
        break;

      case 'trajectory':
        // 添加轨迹点
        addTrajectoryPoint(data.deviceId, {
          deviceId: data.deviceId,
          latitude: data.payload.latitude,
          longitude: data.payload.longitude,
          altitude: data.payload.altitude,
          speed: data.payload.speed,
          direction: data.payload.direction,
          timestamp: data.timestamp,
        });
        break;

      case 'alarm':
        // TODO: 处理告警
        console.warn('[DeviceStore] 设备告警:', data);
        break;

      default:
        console.warn('[DeviceStore] 未知事件类型:', data.eventType);
    }
  }

  /**
   * 重置 Store
   */
  function reset() {
    devices.value = [];
    deviceStatusMap.value.clear();
    deviceTrajectoryMap.value.clear();
    disconnectRealtimeData();
    currentTenantId.value = '';
  }

  return {
    // 状态
    devices,
    deviceStatusMap,
    deviceTrajectoryMap,
    sseClient,
    currentTenantId,

    // 计算属性
    onlineDevices,
    onlineDeviceCount,
    totalDeviceCount,

    // 方法
    setDevices,
    updateDeviceStatus,
    batchUpdateDeviceStatus,
    addTrajectoryPoint,
    setDeviceTrajectory,
    getDeviceTrajectory,
    clearDeviceTrajectory,
    connectRealtimeData,
    disconnectRealtimeData,
    handleRealtimeData,
    reset,
  };
});
