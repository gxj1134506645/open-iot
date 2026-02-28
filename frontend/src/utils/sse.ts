/**
 * SSE（Server-Sent Events）工具类
 * 用于实时接收服务器推送的设备数据
 */

export interface SSEOptions {
  /**
   * 连接超时时间（毫秒）
   */
  timeout?: number;

  /**
   * 自动重连
   */
  autoReconnect?: boolean;

  /**
   * 重连延迟（毫秒）
   */
  reconnectDelay?: number;

  /**
   * 最大重连次数
   */
  maxReconnectAttempts?: number;

  /**
   * 连接成功回调
   */
  onOpen?: () => void;

  /**
   * 消息接收回调
   */
  onMessage?: (event: MessageEvent) => void;

  /**
   * 错误回调
   */
  onError?: (error: Event) => void;

  /**
   * 重连回调
   */
  onReconnect?: (attempt: number) => void;
}

/**
 * SSE 客户端
 */
export class SSEClient {
  private eventSource: EventSource | null = null;
  private url: string;
  private options: SSEOptions;
  private reconnectAttempts = 0;
  private isConnecting = false;

  constructor(url: string, options: SSEOptions = {}) {
    this.url = url;
    this.options = {
      timeout: 30000,
      autoReconnect: true,
      reconnectDelay: 3000,
      maxReconnectAttempts: 5,
      ...options,
    };
  }

  /**
   * 建立 SSE 连接
   */
  connect(): void {
    if (this.eventSource || this.isConnecting) {
      console.warn('[SSE] 连接已存在或正在连接中');
      return;
    }

    this.isConnecting = true;

    try {
      // 创建 EventSource 实例
      this.eventSource = new EventSource(this.url);

      // 连接成功
      this.eventSource.onopen = () => {
        console.log('[SSE] 连接成功');
        this.isConnecting = false;
        this.reconnectAttempts = 0;
        this.options.onOpen?.();
      };

      // 接收消息
      this.eventSource.onmessage = (event: MessageEvent) => {
        console.debug('[SSE] 接收消息:', event.data);
        this.options.onMessage?.(event);
      };

      // 连接错误
      this.eventSource.onerror = (error: Event) => {
        console.error('[SSE] 连接错误:', error);
        this.isConnecting = false;
        this.options.onError?.(error);

        // 自动重连
        if (this.options.autoReconnect) {
          this.reconnect();
        } else {
          this.disconnect();
        }
      };
    } catch (error) {
      console.error('[SSE] 创建连接失败:', error);
      this.isConnecting = false;
    }
  }

  /**
   * 重新连接
   */
  private reconnect(): void {
    if (this.reconnectAttempts >= (this.options.maxReconnectAttempts || 5)) {
      console.error('[SSE] 达到最大重连次数，停止重连');
      this.disconnect();
      return;
    }

    this.reconnectAttempts++;
    console.log(`[SSE] 尝试重连 (${this.reconnectAttempts}/${this.options.maxReconnectAttempts})`);
    this.options.onReconnect?.(this.reconnectAttempts);

    // 先断开旧连接
    this.disconnect();

    // 延迟重连
    setTimeout(() => {
      this.connect();
    }, this.options.reconnectDelay);
  }

  /**
   * 断开连接
   */
  disconnect(): void {
    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
      console.log('[SSE] 连接已断开');
    }
  }

  /**
   * 检查连接状态
   */
  isConnected(): boolean {
    return this.eventSource?.readyState === EventSource.OPEN;
  }

  /**
   * 获取当前重连次数
   */
  getReconnectAttempts(): number {
    return this.reconnectAttempts;
  }
}

/**
 * 创建 SSE 客户端实例
 */
export function createSSEClient(url: string, options?: SSEOptions): SSEClient {
  return new SSEClient(url, options);
}

/**
 * 设备实时数据 SSE 订阅
 */
export interface DeviceRealtimeData {
  /**
   * 设备 ID
   */
  deviceId: number;

  /**
   * 设备编码
   */
  deviceCode: string;

  /**
   * 事件类型
   */
  eventType: 'status' | 'trajectory' | 'alarm';

  /**
   * 数据载荷
   */
  payload: any;

  /**
   * 时间戳
   */
  timestamp: number;
}

/**
 * 订阅设备实时数据
 */
export function subscribeDeviceRealtimeData(
  tenantId: string,
  onMessage: (data: DeviceRealtimeData) => void,
  options?: SSEOptions
): SSEClient {
  const url = `/api/data/sse/realtime?tenantId=${tenantId}`;

  const client = new SSEClient(url, {
    ...options,
    onMessage: (event) => {
      try {
        const data = JSON.parse(event.data) as DeviceRealtimeData;
        onMessage(data);
      } catch (error) {
        console.error('[SSE] 解析消息失败:', error);
      }
    },
  });

  client.connect();
  return client;
}

/**
 * 订阅设备轨迹更新
 */
export function subscribeTrajectoryUpdate(
  deviceId: string,
  onMessage: (data: any) => void,
  options?: SSEOptions
): SSEClient {
  const url = `/api/data/sse/trajectory?deviceId=${deviceId}`;

  const client = new SSEClient(url, {
    ...options,
    onMessage: (event) => {
      try {
        const data = JSON.parse(event.data);
        onMessage(data);
      } catch (error) {
        console.error('[SSE] 解析轨迹数据失败:', error);
      }
    },
  });

  client.connect();
  return client;
}
