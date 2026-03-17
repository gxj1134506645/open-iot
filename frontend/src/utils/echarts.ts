/**
 * ECharts 按需导入配置
 * 只引入项目实际用到的组件，减少打包体积（从 ~1100KB 降至 ~300KB）
 */
import * as echarts from 'echarts/core'

// 图表类型：折线图、饼图
import { LineChart, PieChart } from 'echarts/charts'

// 组件：标题、提示框、图例、坐标轴、数据集、数据缩放
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
  DatasetComponent,
  DataZoomComponent,
} from 'echarts/components'

// 渲染器：Canvas
import { CanvasRenderer } from 'echarts/renderers'

// 注册所需组件
echarts.use([
  LineChart,
  PieChart,
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
  DatasetComponent,
  DataZoomComponent,
  CanvasRenderer,
])

export default echarts
