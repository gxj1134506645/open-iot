# Tab页签中文标题设计方案

## 概述

为前端tab页签显示中文标题，提升用户体验。

## 当前问题

当前tab页签显示的是英文路由名称（如 'Monitor', 'Devices', 'Tenants'），不够直观。

**原因分析**：
- tab标题从 `route.meta.title` 获取
- 路由配置中缺少 `meta.title` 属性
- 降级使用路由的 `name`（英文）

## 解决方案

### 方案选择

对比了3个方案后，选择**方案1：直接在路由配置中添加 meta.title**

**理由**：
- 符合 KISS 原则（最简单）
- 符合 YAGNI 原则（不过度设计）
- 不需要额外依赖或配置文件
- 代码可读性强，易于维护

## 设计详情

### 路由配置修改

修改文件：`frontend/src/router/index.ts`

为所有路由添加 `meta.title` 属性：

```typescript
const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/Login.vue'),
    meta: { requiresAuth: false, title: '登录' }
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        redirect: '/monitor'
      },
      {
        path: 'monitor',
        name: 'Monitor',
        component: () => import('@/views/monitor/DeviceMonitor.vue'),
        meta: { title: '设备监控' }
      },
      {
        path: 'product',
        name: 'ProductList',
        component: () => import('@/views/product/ProductList.vue'),
        meta: { title: '产品管理' }
      },
      {
        path: 'product/:id',
        name: 'ProductDetail',
        component: () => import('@/views/product/ProductDetail.vue'),
        meta: { title: '产品详情' }
      },
      {
        path: 'devices',
        name: 'Devices',
        component: () => import('@/views/device/DeviceList.vue'),
        meta: { title: '设备管理' }
      },
      {
        path: 'devices/:id',
        name: 'DeviceDetail',
        component: () => import('@/views/device/DeviceDetail.vue'),
        meta: { title: '设备详情' }
      },
      {
        path: 'alerts',
        name: 'Alerts',
        component: () => import('@/views/alert/AlertList.vue'),
        meta: { title: '告警管理' }
      },
      {
        path: 'rules',
        name: 'Rules',
        component: () => import('@/views/rule/RuleList.vue'),
        meta: { title: '规则引擎' }
      },
      {
        path: 'tenants',
        name: 'Tenants',
        component: () => import('@/views/tenant/TenantList.vue'),
        meta: { requiresAdmin: true, title: '租户管理' }
      }
    ]
  }
]
```

### 标题映射表

| 路由路径 | 英文名称 | 中文标题 |
|---------|---------|---------|
| /login | Login | 登录 |
| /monitor | Monitor | 设备监控 |
| /product | ProductList | 产品管理 |
| /product/:id | ProductDetail | 产品详情 |
| /devices | Devices | 设备管理 |
| /devices/:id | DeviceDetail | 设备详情 |
| /alerts | Alerts | 告警管理 |
| /rules | Rules | 规则引擎 |
| /tenants | Tenants | 租户管理 |

## 实现细节

### 不需要修改的组件

**1. TabsView.vue**
- 已正确实现从 `route.meta.title` 读取标题
- 无需修改

**2. MainLayout.vue**
- 已通过 `watch` 监听路由变化并自动添加标签页
- 无需修改

**3. tabs.ts (Pinia Store)**
- 已实现标题读取逻辑：`title: (meta.title as string) || (name as string)`
- 无需修改

### 兼容性处理

如果某个路由忘记添加 `meta.title`：
- 自动降级使用路由的 `name`（英文）
- 不会导致程序报错
- 保证系统稳定性

## 实施步骤

1. 修改 `frontend/src/router/index.ts` 文件
2. 为所有路由添加 `meta.title` 属性
3. 测试验证：
   - 访问各个页面，检查tab标题是否显示为中文
   - 刷新页面，检查标题是否保持
   - 打开多个tab，检查标题是否正确

## 影响范围

- **修改文件**：仅 `frontend/src/router/index.ts`
- **不需要修改**：TabsView.vue、MainLayout.vue、tabs.ts
- **不需要安装**：额外依赖

## 测试计划

### 功能测试

1. **基础功能**
   - [ ] 访问设备监控页面，tab显示"设备监控"
   - [ ] 访问产品管理页面，tab显示"产品管理"
   - [ ] 访问设备管理页面，tab显示"设备管理"
   - [ ] 访问告警管理页面，tab显示"告警管理"
   - [ ] 访问规则引擎页面，tab显示"规则引擎"
   - [ ] 访问租户管理页面，tab显示"租户管理"

2. **详情页面**
   - [ ] 打开产品详情，tab显示"产品详情"
   - [ ] 打开设备详情，tab显示"设备详情"

3. **持久化测试**
   - [ ] 刷新页面，tab标题保持中文
   - [ ] 关闭浏览器重新打开，tab标题保持中文

4. **多tab测试**
   - [ ] 打开多个页面，每个tab标题正确显示
   - [ ] 切换tab，标题正确高亮

## 风险评估

- **风险等级**：低
- **影响范围**：仅前端路由配置
- **回滚方案**：删除 `meta.title` 属性即可回滚

## 后续优化（可选）

如果未来需要支持多语言，可以考虑：
1. 引入 vue-i18n
2. 将标题改为 i18n key
3. 添加语言切换功能

但当前项目不需要，遵循 YAGNI 原则。
