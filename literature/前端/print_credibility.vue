//time_credibility.vue
<template>
  <div class="time-credibility" v-loading="loading">
    <div style="margin-bottom: 24px; display: flex; gap: 12px; flex-wrap: wrap;">
      <el-button class="retro-btn" :class="{ primary: $route.path === '/platform/literature/credibility' }"
                 @click="$router.push({ path: '/platform/literature/credibility', query: { id: $route.query.id } })">综合可信度</el-button>
      <el-button class="retro-btn" :class="{ primary: $route.path === '/platform/literature/author_credibility' }"
                 @click="$router.push({ path: '/platform/literature/author_credibility', query: { id: $route.query.id } })">作者可信度</el-button>
      <el-button class="retro-btn" :class="{ primary: $route.path === '/platform/literature/time_credibility' }"
                 @click="$router.push({ path: '/platform/literature/time_credibility', query: { id: $route.query.id } })">时效可信度</el-button>
      <el-button class="retro-btn" :class="{ primary: $route.path === '/platform/literature/print_credibility' }"
                 @click="$router.push({ path: '/platform/literature/print_credibility', query: { id: $route.query.id } })">期刊可信度</el-button>
    </div>

    <el-collapse class="retro-collapse" style="margin-bottom: 20px;">
      <el-collapse-item>
        <template slot="title">
          <i class="el-icon-info" style="color: #3C848C; margin-right: 8px;"></i>
          <span class="collapse-title">📅 时效可信度计算方法</span>
        </template>
        <div class="collapse-content">
          <div class="inner-card">
            <p class="formula"><strong>C_time(p)</strong> = (y - Y_min) / (Y_max - Y_min)</p>
            <p class="rule-desc">• y：文献发表日期 • Y_min：数据集中最小日期 • Y_max：最大日期 • 结果归一化至 [0,1]</p>
          </div>
        </div>
      </el-collapse-item>
    </el-collapse>

    <el-row :gutter="20">
      <el-col :xs="24" :lg="14">
        <el-card class="retro-card" shadow="never" style="margin-bottom: 20px;">
          <div slot="header" class="retro-card-header">
            <span><i class="el-icon-data-line"></i> <span class="card-title">文献数据集日期范围</span></span>
          </div>
          <el-row :gutter="20" class="stat-cards">
            <el-col :span="8"><div class="stat-item"><div class="stat-label">📉 最小发表日期</div><div class="stat-number">{{ minDateStr }}</div></div></el-col>
            <el-col :span="8"><div class="stat-item"><div class="stat-label">📈 最大发表日期</div><div class="stat-number">{{ maxDateStr }}</div></div></el-col>
            <el-col :span="8"><div class="stat-item"><div class="stat-label">📊 文献总数</div><div class="stat-number">{{ totalCount }}</div></div></el-col>
          </el-row>
        </el-card>

        <el-card class="retro-card" shadow="never">
          <div slot="header" class="retro-card-header">
            <span><i class="el-icon-calculator"></i> <span class="card-title">本论文时效可信度结果</span></span>
          </div>
          <div class="inner-card accent-bg calculation-result">
            <p><span class="info-label">目标日期：</span><span class="info-value">{{ targetDateStr }}</span></p>
            <p><span class="info-label">时间范围：</span><span class="info-value">{{ minDateStr }} 至 {{ maxDateStr }}</span></p>
            <p class="result-number">C_time = {{ displayCtime.toFixed(4) }} <span class="result-badge">{{ (displayCtime * 100).toFixed(1) }}%</span></p>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="10">
        <el-card class="retro-card" shadow="never" style="margin-bottom: 20px;">
          <div slot="header" class="retro-card-header">
            <span><i class="el-icon-s-marketing"></i> <span class="card-title">文献发表日期分布</span></span>
            <el-tooltip content="每个点代表一篇文献" placement="top"><i class="el-icon-question" style="color: #909399; cursor: help;"></i></el-tooltip>
          </div>
          <div ref="timelineChart" style="width: 100%; height: 240px;"></div>
          <div class="chart-legend">
            <span><span class="legend-dot" style="background: #6baed6;"></span> 文献日期</span>
            <span><span class="legend-dot" style="background: #f4a2a2; border-radius: 50%;"></span> 最小日期</span>
            <span><span class="legend-dot" style="background: #9ac9a8; border-radius: 50%;"></span> 最大日期</span>
            <span><span class="legend-dot" style="background: #fdbe85; border-radius: 50%;"></span> 目标日期</span>
          </div>
        </el-card>

        <el-card class="retro-card" shadow="never">
          <div slot="header" class="retro-card-header"><span><i class="el-icon-document"></i> <span class="card-title">时效性解读</span></span></div>
          <div class="interpretation-list">
            <div><span class="info-label">本论文发表日期</span><span class="info-value">{{ targetDateStr }}</span></div>
            <div><span class="info-label">在数据集中的位置</span><span class="info-value">第 {{ rankInDataset }} / {{ totalCount }} 新</span></div>
            <div><span class="info-label">时效可信度得分</span><span class="info-value">{{ (displayCtime * 100).toFixed(1) }}%</span></div>
            <div><span class="info-label">时效性评级</span><el-tag :type="ratingType" size="medium" class="rating-tag">{{ ratingText }}</el-tag></div>
          </div>
          <div class="note">时效可信度越接近 100% 表示文献越新。</div>
        </el-card>
      </el-col>
    </el-row>

    <div class="action-footer">
      <el-button type="text" icon="el-icon-back" @click="$router.push('/platform/literature')" class="retro-btn">返回文献上传</el-button>
    </div>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import axios from 'axios'

const API_BASE = 'http://localhost:3007'

export default {
  name: 'TimeCredibility',
  data() {
    return {
      loading: true,
      allDateStrings: [],      // 所有日期字符串
      targetDateStr: null,
      targetCtime: null,
      // 直接存储后端返回的最小/最大/总数
      minDateStr: '—',
      maxDateStr: '—',
      totalCount: 0
    }
  },
  computed: {
    // 解析后的日期对象列表（用于图表）
    dateList() {
      return this.allDateStrings.map(d => this.parseDate(d)).filter(d => d !== null)
    },
    // 用于显示的有效 C_time（优先使用后端返回值）
    displayCtime() {
      if (this.targetCtime !== null) return this.targetCtime
      // 如果没有后端值，尝试计算（但应该不会发生）
      if (!this.dateList.length) return 0
      const target = this.parseDate(this.targetDateStr)
      if (!target) return 0
      const min = new Date(Math.min(...this.dateList.map(d => d.getTime())))
      const max = new Date(Math.max(...this.dateList.map(d => d.getTime())))
      return (target - min) / (max - min)
    },
    rankInDataset() {
      if (!this.targetDateStr || !this.dateList.length) return '—'
      const target = this.parseDate(this.targetDateStr)
      if (!target) return '—'
      const sorted = [...this.dateList].sort((a, b) => b - a)
      const index = sorted.findIndex(d => this.isSameDay(d, target))
      return index !== -1 ? index + 1 : '—'
    },
    ratingText() {
      const score = this.displayCtime
      if (score >= 0.8) return '非常新'
      if (score >= 0.6) return '较新'
      if (score >= 0.4) return '中等'
      if (score >= 0.2) return '较旧'
      return '非常旧'
    },
    ratingType() {
      const score = this.displayCtime
      if (score >= 0.8) return 'success'
      if (score >= 0.6) return 'primary'
      if (score >= 0.4) return 'warning'
      return 'danger'
    }
  },
  watch: {
    '$route.query.id': {
      immediate: true,
      async handler(newId) {
        if (newId) {
          await this.fetchDateRange()
          await this.loadPaperData(newId)
          this.loading = false
          this.$nextTick(() => this.initTimelineChart())
        } else {
          this.targetDateStr = null
          this.loading = false
          this.$message.info('请从文献列表选择文献查看时效可信度')
        }
      }
    }
  },
  methods: {
    parseDate(str) {
      if (!str) return null
      // 支持 YYYY-MM-DD
      if (str.match(/^\d{4}-\d{1,2}-\d{1,2}$/)) {
        const parts = str.split('-')
        return new Date(parseInt(parts[0]), parseInt(parts[1]) - 1, parseInt(parts[2]))
      }
      // 支持 YYYY.M.D
      if (str.match(/^\d{4}\.\d{1,2}\.\d{1,2}$/)) {
        const parts = str.split('.')
        return new Date(parseInt(parts[0]), parseInt(parts[1]) - 1, parseInt(parts[2]))
      }
      // 纯年份
      if (str.match(/^\d{4}$/)) return new Date(parseInt(str), 0, 1)
      return null
    },
    formatDate(date) {
      if (!date) return ''
      const y = date.getFullYear()
      const m = (date.getMonth() + 1).toString().padStart(2, '0')
      const d = date.getDate().toString().padStart(2, '0')
      return `${y}-${m}-${d}`
    },
    isSameDay(d1, d2) {
      return d1.toDateString() === d2.toDateString()
    },
    async fetchDateRange() {
      try {
        const res = await axios.get(`${API_BASE}/api/date-range`)
        if (res.data.success) {
          this.minDateStr = res.data.min_date || '—'
          this.maxDateStr = res.data.max_date || '—'
          this.totalCount = res.data.total_count
          this.allDateStrings = res.data.all_dates || []
        } else {
          console.error('获取日期范围失败', res.data.error)
        }
      } catch (error) {
        console.error('获取日期范围失败:', error)
        this.minDateStr = '—'
        this.maxDateStr = '—'
        this.totalCount = 0
      }
    },
    async loadPaperData(paperId) {
      try {
        const res = await axios.get(`${API_BASE}/api/id/${paperId}`)
        if (res.data.success) {
          const data = res.data.data
          this.targetDateStr = data['发表日期']
          this.targetCtime = data.C_time
        }
      } catch (error) {
        console.error('加载文献数据失败:', error)
      }
    },
    initTimelineChart() {
      const chartDom = this.$refs.timelineChart
      if (!chartDom || this.dateList.length === 0) return
      const myChart = echarts.init(chartDom)
      const points = this.dateList.map(d => d.getTime()).sort((a, b) => a - b)
      const markPoints = []
      const minDate = new Date(Math.min(...points))
      const maxDate = new Date(Math.max(...points))
      const target = this.targetDateStr ? this.parseDate(this.targetDateStr) : null

      if (minDate) markPoints.push({ name: '最小', value: minDate.getTime(), xAxis: minDate.getTime(), yAxis: 0, symbol: 'circle', symbolSize: 12, itemStyle: { color: '#f4a2a2' } })
      if (maxDate) markPoints.push({ name: '最大', value: maxDate.getTime(), xAxis: maxDate.getTime(), yAxis: 0, symbol: 'circle', symbolSize: 12, itemStyle: { color: '#9ac9a8' } })
      if (target) markPoints.push({ name: '目标', value: target.getTime(), xAxis: target.getTime(), yAxis: 0, symbol: 'diamond', symbolSize: 14, itemStyle: { color: '#fdbe85' } })

      const option = {
        tooltip: { trigger: 'item', formatter: (params) => this.formatDate(new Date(params.value)) },
        grid: { left: '5%', right: '8%', top: 15, bottom: 25, containLabel: true },
        xAxis: { type: 'time', splitLine: { show: false }, axisLabel: { fontSize: 11, rotate: 30 } },
        yAxis: { type: 'category', data: [''], show: false },
        series: [
          { name: '文献日期', type: 'scatter', data: points, symbolSize: 8, itemStyle: { color: '#6baed6', opacity: 0.7 } },
          { name: '标记点', type: 'scatter', data: markPoints, symbolSize: 12, label: { show: true, position: 'top', formatter: (params) => params.name, fontSize: 11, offset: [0, 10] } }
        ]
      }
      myChart.setOption(option)
      window.addEventListener('resize', () => myChart.resize())
    }
  }
}
</script>



<style scoped>
.time-credibility { font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; padding: 20px; }
.time-credibility .el-card { border: 2px solid #3C848C; border-radius: 0; box-shadow: none !important; background-color: #FFFFFF; }
.time-credibility .el-card__header { color: #3C848C; font-weight: 800; text-transform: uppercase; letter-spacing: 1px; padding: 14px 20px; border-bottom: 2px solid #3C848C; }
.retro-card-header { display: flex; align-items: center; justify-content: space-between; }
.retro-card-header i { color: #3C848C; margin-right: 8px; font-size: 18px; }
.card-title { color: #3C848C; font-size: 14px; font-weight: 800; text-transform: uppercase; letter-spacing: 1px; }
.retro-btn { border-radius: 0; border: 2px solid #3C848C; font-weight: 800; text-transform: uppercase; padding: 10px 20px; font-size: 14px; letter-spacing: 1px; transition: none; background-color: #FFFFFF; color: #3C848C; }
.retro-btn.primary { background-color: #3C848C; color: #FFFFFF; border-color: #3C848C; }
.retro-btn.primary:hover { background-color: #2F696F; }
.retro-btn:not(.primary):hover { background-color: #EEEEEE; }
.retro-collapse { border: 2px solid #3C848C; border-radius: 0; }
.retro-collapse .el-collapse-item__header { padding-left: 20px; font-size: 14px; font-weight: 800; background: white; color: #3C848C; text-transform: uppercase; border-bottom: 2px solid #3C848C; }
.collapse-title { font-weight: 800; color: #3C848C; }
.collapse-content { padding: 8px 16px 16px; background: #f9fafc; }
.inner-card { background: white; padding: 16px; border: 2px solid #3C848C; border-radius: 0; height: 100%; }
.accent-bg { background: #ecf5ff; }
.formula { font-size: 15px; font-weight: 600; margin: 8px 0; }
.stat-cards { margin: 16px 0; }
.stat-item { text-align: center; padding: 16px 0; border: 2px solid #3C848C; background: #f5f7fa; }
.stat-label { font-size: 13px; color: #666666; margin-bottom: 8px; font-weight: 700; text-transform: uppercase; }
.stat-number { font-size: 20px; font-weight: 800; color: #3C848C; }
.info-label { color: #666666; font-weight: 700; text-transform: uppercase; margin-right: 8px; }
.info-value { font-weight: 600; color: #3C848C; }
.result-number { margin: 10px 0 0; font-size: 20px; font-weight: 700; color: #3C848C; }
.result-badge { background: #3C848C; color: white; padding: 2px 12px; margin-left: 8px; display: inline-block; }
.chart-legend { margin-top: 16px; display: flex; justify-content: space-around; font-size: 13px; }
.legend-dot { display: inline-block; width: 10px; height: 10px; margin-right: 6px; border-radius: 2px; }
.interpretation-list { font-size: 14px; line-height: 2; }
.interpretation-list > div { display: flex; justify-content: space-between; border-bottom: 1px dashed #ebeef5; padding: 8px 0; }
.rating-tag { border-radius: 0; border: none; font-weight: 700; }
.retro-divider { background-color: #3C848C; height: 2px; }
.action-footer { margin-top: 24px; text-align: right; border-top: 2px solid #3C848C; padding-top: 16px; }
.empty-state { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 40px 0; color: #909399; }
.empty-state i { font-size: 40px; margin-bottom: 12px; }
.note { margin: 12px 0 0; color: #909399; font-size: 13px; }
</style>
