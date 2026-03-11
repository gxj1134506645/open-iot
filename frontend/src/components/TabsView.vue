<template>
  <div class="tabs-view">
    <!-- 标签页滚动容器 -->
    <div class="tabs-scroll-container">
      <div class="tabs-wrapper">
        <div
          v-for="tab in tabsStore.tabList"
          :key="tab.path"
          class="tab-item"
          :class="{ active: tabsStore.activeTab === tab.path }"
          @click="handleTabClick(tab)"
          @contextmenu.prevent="handleContextMenu($event, tab)"
        >
          <!-- 标签页标题 -->
          <span class="tab-title">{{ tab.title }}</span>

          <!-- 关闭按钮(固定标签页不显示) -->
          <el-icon
            v-if="tab.closable"
            class="tab-close"
            @click.stop="handleTabClose(tab)"
          >
            <Close />
          </el-icon>
        </div>
      </div>
    </div>

    <!-- 右键菜单 -->
    <div
      v-show="contextMenuVisible"
      class="context-menu glass-card"
      :style="{ left: contextMenuX + 'px', top: contextMenuY + 'px' }"
    >
      <div class="menu-item" @click="handleRefresh">
        <el-icon><Refresh /></el-icon>
        <span>刷新当前页</span>
      </div>
      <div
        v-if="currentContextTab?.closable"
        class="menu-item"
        @click="handleCloseOther"
      >
        <el-icon><Close /></el-icon>
        <span>关闭其他</span>
      </div>
      <div class="menu-item" @click="handleCloseAll">
        <el-icon><CircleClose /></el-icon>
        <span>关闭所有</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Close, Refresh, CircleClose } from '@element-plus/icons-vue'
import { useTabsStore, type TabItem } from '@/stores/tabs'

const router = useRouter()
const tabsStore = useTabsStore()

// 右键菜单状态
const contextMenuVisible = ref(false)
const contextMenuX = ref(0)
const contextMenuY = ref(0)
const currentContextTab = ref<TabItem | null>(null)

/**
 * 标签页点击事件
 */
function handleTabClick(tab: TabItem) {
  tabsStore.setActiveTab(tab.path)
  router.push(tab.path)
}

/**
 * 标签页关闭事件
 */
function handleTabClose(tab: TabItem) {
  const nextPath = tabsStore.closeTab(tab.path)
  if (nextPath) {
    router.push(nextPath)
  } else if (tabsStore.tabList.length === 0) {
    // 没有其他标签页,跳转到首页
    router.push('/monitor')
  }
}

/**
 * 右键菜单事件
 */
function handleContextMenu(event: MouseEvent, tab: TabItem) {
  currentContextTab.value = tab
  contextMenuX.value = event.clientX
  contextMenuY.value = event.clientY
  contextMenuVisible.value = true
}

/**
 * 刷新当前标签页
 */
function handleRefresh() {
  if (currentContextTab.value) {
    tabsStore.refreshTab(currentContextTab.value.path)
    ElMessage.success('页面已刷新')
  }
  hideContextMenu()
}

/**
 * 关闭其他标签页
 */
function handleCloseOther() {
  if (currentContextTab.value) {
    tabsStore.closeOtherTabs(currentContextTab.value.path)
    router.push(currentContextTab.value.path)
  }
  hideContextMenu()
}

/**
 * 关闭所有标签页
 */
function handleCloseAll() {
  const nextPath = tabsStore.closeAllTabs()
  if (nextPath) {
    router.push(nextPath)
  } else {
    router.push('/monitor')
  }
  hideContextMenu()
}

/**
 * 隐藏右键菜单
 */
function hideContextMenu() {
  contextMenuVisible.value = false
  currentContextTab.value = null
}

/**
 * 点击页面其他位置时隐藏右键菜单
 */
function handleClickOutside(event: MouseEvent) {
  if (contextMenuVisible.value) {
    hideContextMenu()
  }
}

// 生命周期钩子
onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped>
.tabs-view {
  position: relative;
  background: rgba(15, 23, 42, 0.6);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  padding: 8px 16px;
  user-select: none;
}

.tabs-scroll-container {
  overflow-x: auto;
  overflow-y: hidden;
  scrollbar-width: thin;
  scrollbar-color: rgba(99, 102, 241, 0.3) transparent;
}

.tabs-scroll-container::-webkit-scrollbar {
  height: 4px;
}

.tabs-scroll-container::-webkit-scrollbar-thumb {
  background: rgba(99, 102, 241, 0.3);
  border-radius: 2px;
}

.tabs-scroll-container::-webkit-scrollbar-thumb:hover {
  background: rgba(99, 102, 241, 0.5);
}

.tabs-wrapper {
  display: flex;
  gap: 8px;
  white-space: nowrap;
}

.tab-item {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 8px;
  color: #94a3b8;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
  min-width: 100px;
  max-width: 200px;
}

.tab-item:hover {
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(99, 102, 241, 0.3);
  color: #e2e8f0;
}

.tab-item.active {
  background: rgba(99, 102, 241, 0.2);
  border-color: rgba(99, 102, 241, 0.5);
  color: #a5b4fc;
}

.tab-title {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

.tab-close {
  font-size: 12px;
  color: #64748b;
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.tab-close:hover {
  color: #f87171;
  transform: scale(1.2);
}

/* 右键菜单 */
.context-menu {
  position: fixed;
  z-index: 9999;
  min-width: 160px;
  padding: 8px 0;
  border-radius: 8px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.4);
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  color: #cbd5e1;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.menu-item:hover {
  background: rgba(99, 102, 241, 0.2);
  color: #a5b4fc;
}

.menu-item .el-icon {
  font-size: 16px;
}
</style>
