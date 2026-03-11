# Tab页签中文标题实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 为前端tab页签添加中文标题，提升用户体验

**Architecture:** 在路由配置中添加 `meta.title` 属性，tab组件自动从路由元数据读取并显示中文标题

**Tech Stack:** Vue 3 + Vue Router + TypeScript

---

## Task 1: 修改路由配置添加中文标题

**Files:**
- Modify: `frontend/src/router/index.ts`

**Step 1: 打开路由配置文件**

文件路径：`frontend/src/router/index.ts`

**Step 2: 为登录路由添加标题**

找到登录路由配置（第 6-9 行），修改为：

```typescript
{
  path: '/login',
  name: 'Login',
  component: () => import('@/views/auth/Login.vue'),
  meta: { requiresAuth: false, title: '登录' }
}
```

**Step 3: 为设备监控路由添加标题**

找到设备监控路由配置（第 21-24 行），修改为：

```typescript
{
  path: 'monitor',
  name: 'Monitor',
  component: () => import('@/views/monitor/DeviceMonitor.vue'),
  meta: { title: '设备监控' }
}
```

**Step 4: 为产品列表路由添加标题**

找到产品列表路由配置（第 37-40 行），修改为：

```typescript
{
  path: 'product',
  name: 'ProductList',
  component: () => import('@/views/product/ProductList.vue'),
  meta: { title: '产品管理' }
}
```

**Step 5: 为产品详情路由添加标题**

找到产品详情路由配置（第 42-45 行），修改为：

```typescript
{
  path: 'product/:id',
  name: 'ProductDetail',
  component: () => import('@/views/product/ProductDetail.vue'),
  meta: { title: '产品详情' }
}
```

**Step 6: 为设备列表路由添加标题**

找到设备列表路由配置（第 27-29 行和第 47-50 行，有两个重复配置），修改第一个：

```typescript
{
  path: 'devices',
  name: 'Devices',
  component: () => import('@/views/device/DeviceList.vue'),
  meta: { title: '设备管理' }
}
```

**注意：** 第 47-50 行有重复的 devices 路由配置，需要删除重复项。

**Step 7: 为设备详情路由添加标题**

找到设备详情路由配置（第 52-55 行），修改为：

```typescript
{
  path: 'devices/:id',
  name: 'DeviceDetail',
  component: () => import('@/views/device/DeviceDetail.vue'),
  meta: { title: '设备详情' }
}
```

**Step 8: 为告警管理路由添加标题**

找到告警管理路由配置（第 57-60 行），修改为：

```typescript
{
  path: 'alerts',
  name: 'Alerts',
  component: () => import('@/views/alert/AlertList.vue'),
  meta: { title: '告警管理' }
}
```

**Step 9: 为规则引擎路由添加标题**

找到规则引擎路由配置（第 62-65 行），修改为：

```typescript
{
  path: 'rules',
  name: 'Rules',
  component: () => import('@/views/rule/RuleList.vue'),
  meta: { title: '规则引擎' }
}
```

**Step 10: 为租户管理路由添加标题**

找到租户管理路由配置（第 31-35 行），修改为：

```typescript
{
  path: 'tenants',
  name: 'Tenants',
  component: () => import('@/views/tenant/TenantList.vue'),
  meta: { requiresAdmin: true, title: '租户管理' }
}
```

**Step 11: 删除重复的设备列表路由配置**

删除第 47-50 行的重复配置：

```typescript
// 删除这段重复代码
{
  path: 'devices',
  name: 'Devices',
  component: () => import('@/views/device/DeviceList.vue')
}
```

**Step 12: 验证修改**

检查文件，确保：
- 所有路由都有 `meta.title` 属性
- 没有重复的路由配置
- 代码格式正确

---

## Task 2: 测试验证

**Step 1: 启动前端开发服务器**

```bash
cd frontend
npm run dev
```

Expected: 前端服务启动成功，访问 http://localhost:5173

**Step 2: 测试基础页面tab标题**

1. 登录系统
2. 访问设备监控页面，检查tab是否显示"设备监控"
3. 访问产品管理页面，检查tab是否显示"产品管理"
4. 访问设备管理页面，检查tab是否显示"设备管理"
5. 访问告警管理页面，检查tab是否显示"告警管理"
6. 访问规则引擎页面，检查tab是否显示"规则引擎"

Expected: 所有tab都显示中文标题

**Step 3: 测试详情页面tab标题**

1. 点击某个产品，进入产品详情页
2. 检查tab是否显示"产品详情"
3. 点击某个设备，进入设备详情页
4. 检查tab是否显示"设备详情"

Expected: 详情页tab显示正确的中文标题

**Step 4: 测试tab持久化**

1. 打开多个页面（3-5个tab）
2. 刷新浏览器页面（F5）
3. 检查tab标题是否保持中文

Expected: 刷新后tab标题保持中文

**Step 5: 测试tab切换**

1. 打开多个页面
2. 在不同tab之间切换
3. 检查每个tab的标题是否正确
4. 检查当前激活的tab是否高亮显示

Expected: tab切换正常，标题显示正确

---

## Task 3: 提交代码

**Step 1: 查看修改**

```bash
git status
```

Expected: 显示 `frontend/src/router/index.ts` 被修改

**Step 2: 添加文件到暂存区**

```bash
git add frontend/src/router/index.ts
```

**Step 3: 提交修改**

```bash
git commit -m "feat(router): 为路由添加中文标题

- 为所有路由添加 meta.title 属性
- tab页签现在显示中文标题
- 删除重复的设备列表路由配置"
```

**Step 4: 推送到远程仓库**

```bash
git push
```

Expected: 代码成功推送到远程仓库

---

## 完整的修改后代码

```typescript
import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

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

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()

  // token 存在但 userInfo 缺失时（如页面刷新且旧 session 未缓存 userInfo），
  // 先从服务器拉取用户信息，保证 isAdmin 等计算属性正确
  if (userStore.token && !userStore.userInfo) {
    try {
      await userStore.fetchUserInfo()
    } catch {
      // token 无效或过期，清除后跳转登录
      localStorage.removeItem('token')
      next('/login')
      return
    }
  }

  // ===== 装饰:路由恢复逻辑 =====
  // 登录成功后,检查是否有需要恢复的路由
  if (to.path === '/login' && userStore.isLoggedIn) {
    const routeToRestore = userStore.getRouteToRestore()
    if (routeToRestore) {
      next(routeToRestore)
      return
    }
    next('/monitor')
    return
  }

  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    next('/login')
  } else if (to.meta.requiresAdmin && !userStore.isAdmin) {
    next('/monitor')
  } else {
    next()
  }
})

export default router
```

---

## 注意事项

1. **重复路由问题**：原文件中第 47-50 行有重复的 `devices` 路由配置，必须删除
2. **meta 合并**：对于已有 meta 的路由（如 `/login` 和 `/tenants`），需要将 title 添加到现有 meta 对象中
3. **编码格式**：确保文件使用 UTF-8 编码保存
4. **测试覆盖**：虽然不写单元测试，但必须手动测试所有页面的tab标题显示
