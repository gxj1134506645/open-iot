/**
 * 导出 TabItem 接口定义
 * 用于在 TabsView 组件中传递给父组件
 * 方便类型检查和当前标签页是否激活
 */
export interface TabItem {
  path: string           // 路由路径
  title: string          // 标签页标题
  name: string           // 苯由名称(用于 keep-alive 缓存)
  query?: object         // 查询参数
  closable: boolean      // 是否可关闭
  affix?: boolean        // 是否固定(不可关闭)
  needRefresh?: number   // 刷新时间戳(内部使用)
}
