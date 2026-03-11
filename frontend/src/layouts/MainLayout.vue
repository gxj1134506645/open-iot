<template>
  <div class="admin-layout">
    <!-- 左侧边栏 -->
    <aside class="sidebar glass-sidebar" :class="{ collapsed: isCollapsed }">
      <!-- Logo 区域 -->
      <div class="sidebar-logo">
        <div class="logo-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="3" y="3" width="7" height="7" rx="1" />
            <rect x="14" y="3" width="7" height="7" rx="1" />
            <rect x="3" y="14" width="7" height="7" rx="1" />
            <rect x="14" y="14" width="7" height="7" rx="1" />
          </svg>
        </div>
        <span v-show="!isCollapsed" class="logo-text">open-iot</span>
      </div>

      <!-- 导航菜单 -->
      <nav class="sidebar-nav">
        <router-link
          v-for="item in visibleMenuItems"
          :key="item.path"
          :to="item.path"
          class="nav-item"
          :class="{ active: isActive(item.path) }"
        >
          <span class="nav-icon" v-html="item.iconSvg"></span>
          <span v-show="!isCollapsed" class="nav-text">{{ item.title }}</span>
          <span v-if="isActive(item.path)" class="active-bar"></span>
        </router-link>
      </nav>

      <!-- 底部系统状态 -->
      <div class="sidebar-footer" v-show="!isCollapsed">
        <div class="system-status">
          <span class="status-dot"></span>
          <span v-show="!isCollapsed">系统运行正常</span>
        </div>
      </div>
    </aside>

    <!-- 右侧主区域 -->
    <div class="main-wrapper">
      <!-- 顶部 Header -->
      <header class="header glass-header">
        <div class="header-left">
          <!-- 折叠/展开按钮 -->
          <button class="collapse-btn" @click="toggleCollapse">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="3" y="3" width="18" height="18" rx="2" />
              <line x="3" y="12" x2="21" y="12" />
            </svg>
          </button>
          <!-- 鷻加 TabsView 组件 -->
          <TabsView style="margin-left: 16px;" />
        </div>

        <!-- 右侧用户信息 -->
        <div class="header-right">
          <el-dropdown trigger="click" @command="handleUserCommand">
            <div class="user-trigger">
              <div class="user-avatar">{{ avatarLetter }}</div>
              <div class="user-info">
                <span class="user-name">{{ username }}</span>
                <span class="user-role-badge">{{ userRoleText }}</span>
              </div>
              <el-icon class="dropdown-arrow"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <div class="dropdown-user-detail">
                <div class="dropdown-avatar">{{ avatarLetter }}</div>
                <div>
                  <div class="dropdown-username">{{ username }}</div>
                  <div class="dropdown-role">{{ userRoleText }}</div>
                </div>
              </div>
              <el-dropdown-menu class="user-dropdown-menu">
                <el-dropdown-item command="logout" class="logout-item">
                  <el-icon><SwitchButton /></el-icon>
                  <span>退出登录</span>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <!-- 主内容区:添加 keep-alive -->
      <div class="main-content">
        <router-view v-slot="{ Component, route }">
          <keep-alive :include="tabsStore.cachedViews">
            <component :is="Component" :key="route.path" />
          </keep-alive>
        </router-view>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown, SwitchButton } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useTabsStore } from '@/stores/tabs'
import TabsView from '@/components/TabsView.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const tabsStore = useTabsStore()

// 侧边栏折叠状态
const isCollapsed = ref(false)

// 装饰:根据 localStorage 恢复折叠状态
onMounted(() => {
  const savedCollapsed = localStorage.getItem('sidebar-collapsed')
  if (savedCollapsed !== null) {
    isCollapsed.value = savedCollapsed === 'true'
  }
  // 初始化标签页
  tabsStore.initTabs()
})

// ===== 迎面屑 =====
const pathTitleMap: Record<string, string> = {
  '/monitor': '设备监控',
  '/product': '产品管理',
  '/devices': '设备管理',
  '/alerts': '告警管理',
  '/rules': '规则引擎',
  '/tenants': '租户管理',
}


</script>
</script>

