import axios from 'axios'

const request = axios.create({
  baseURL: '/api/v1',
  timeout: 10000
})

// 请求拦截器
request.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = token
    }
    return config
  },
  error => Promise.reject(error)
)

// 响应拦截器
request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 200) {
      return Promise.reject(new Error(res.msg || '请求失败'))
    }
    return res.data
  },
  error => {
    if (error.response?.status === 401) {
    // 导入 Element Plus Message 和 user store
    import('element-plus').then(({ ElMessage }) => {
      const { useUserStore } = require('@/stores/user')
      const { useTabsStore } = require('@/stores/tabs')

      const userStore = useUserStore()
      const tabsStore = useTabsStore()

      // 记住当前路由(用于登录后恢复)
      const currentPath = window.location.pathname
      userStore.rememberExpiredRoute(currentPath)

      // 显示轻量提示,3秒后自动关闭
      ElMessage.warning({
        message: '登录已过期,请重新登录',
        duration: 3000,
        showClose: true
      })

      // 清空用户会话和标签页
      userStore.logout()
      tabsStore.clearTabs()

      // 3秒后跳转登录页
      setTimeout(() => {
        window.location.href = '/login'
      }, 3000)
    })
  }
  return Promise.reject(error)
  }
)

export default request
