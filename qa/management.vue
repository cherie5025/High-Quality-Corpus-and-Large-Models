<template>
  <div class="qa-management-page">

    <!-- 页面标题 -->
    <div class="page-header">
      <div class="header-top">
        <div>
        </div>
        <el-button type="primary" icon="el-icon-arrow-left" @click="$router.go(-1)">
          返回上一页
        </el-button>
      </div>
    </div>

    <!-- 核心数据展示 -->
    <el-card class="stats-card" shadow="never" v-loading="statsLoading">
      <h3 class="section-title">核心统计数据</h3>

      <!-- 第一行：总数 + 综合质量分 -->
      <div class="stats-grid stats-grid-top">

        <div class="stat-box stat-box-large">
          <div class="stat-icon" style="background:#E8F4FF;">
            <i class="el-icon-chat-dot-round" style="color:#409EFF;"></i>
          </div>
          <div class="stat-content">
            <div class="stat-label">问答对总数量</div>
            <div class="stat-value" style="color:#409EFF;">{{ stats.total }}</div>
          </div>
        </div>

        <div class="stat-box stat-box-large">
          <div class="stat-icon" style="background:#F0F4FF;">
            <i class="el-icon-data-analysis" style="color:#5B6EF5;"></i>
          </div>
          <div class="stat-content">
            <div class="stat-label">整体综合质量得分</div>
            <div class="score-inline">
              <span class="score-big" :style="{ color: getProgressColor(stats.overall_score) }">
                {{ stats.overall_score }}
              </span>
              <span class="score-denom">/ 10.0</span>
            </div>
          </div>
        </div>

      </div>

      <!-- 第二行：三个质量等级 -->
      <div class="stats-grid stats-grid-bottom">

        <div class="stat-box">
          <div class="stat-icon" style="background:#E8F9F0;">
            <i class="el-icon-circle-check" style="color:#67C23A;"></i>
          </div>
          <div class="stat-content">
            <div class="stat-label">Accepted</div>
            <div class="stat-subtitle">高质量问答对数量</div>
            <div class="stat-value" style="color:#67C23A;">{{ judgmentCount('Accepted') }}</div>
          </div>
        </div>

        <div class="stat-box">
          <div class="stat-icon" style="background:#FFF7E6;">
            <i class="el-icon-warning-outline" style="color:#E6A23C;"></i>
          </div>
          <div class="stat-content">
            <div class="stat-label">Reluctantly</div>
            <div class="stat-subtitle">中质量问答对数量</div>
            <div class="stat-value" style="color:#E6A23C;">{{ judgmentCount('Reluctantly') }}</div>
          </div>
        </div>

        <div class="stat-box">
          <div class="stat-icon" style="background:#FEF0F0;">
            <i class="el-icon-circle-close" style="color:#F56C6C;"></i>
          </div>
          <div class="stat-content">
            <div class="stat-label">Rejected</div>
            <div class="stat-subtitle">低质量问答对数量</div>
            <div class="stat-value" style="color:#F56C6C;">{{ judgmentCount('Rejected') }}</div>
          </div>
        </div>

      </div>
    </el-card>

    <!-- 图表第一行：饼图 + 雷达图 -->
    <div class="charts-row">

      <!-- 判定结果分布饼图 -->
      <el-card class="chart-card" shadow="never">
        <h3 class="section-title">判定结果分布</h3>
        <div class="chart-wrapper">
          <canvas ref="pieCanvas"></canvas>
          <div class="chart-tooltip" v-show="pieTooltip.visible" :style="pieTooltip.style">
            <div class="tooltip-label">{{ pieTooltip.label }}</div>
            <div class="tooltip-value" :style="{ color: pieTooltip.color }">{{ pieTooltip.count }} 条</div>
            <div class="tooltip-pct">占比 {{ pieTooltip.pct }}%</div>
          </div>
        </div>
        <div class="pie-legend">
          <div class="legend-item" v-for="item in pieSegments" :key="item.label">
            <span class="legend-dot" :style="{ background: item.color }"></span>
            <span class="legend-text">{{ item.label }}</span>
            <span class="legend-count" :style="{ color: item.color }">{{ item.count }} 条</span>
          </div>
        </div>
      </el-card>

      <!-- 各评估维度雷达图 -->
      <el-card class="chart-card" shadow="never">
        <h3 class="section-title">各评估维度平均分</h3>
        <div class="chart-wrapper">
          <canvas ref="radarCanvas"></canvas>
          <div class="chart-tooltip" v-show="radarTooltip.visible" :style="radarTooltip.style">
            <div class="tooltip-label">{{ radarTooltip.label }}</div>
            <div class="tooltip-value" :style="{ color: radarTooltip.color }">{{ radarTooltip.value }} 分</div>
          </div>
        </div>
        <div class="radar-legend">
          <div class="radar-legend-item" v-for="(val, key) in stats.dimension_averages" :key="key">
            <span class="radar-dim-name">{{ dimNameMap[key] || key }}</span>
            <div class="radar-bar-wrap">
              <div class="radar-bar-fill"
                   :style="{ width: (val * 10) + '%', background: getProgressColor(val) }">
              </div>
            </div>
            <span class="radar-dim-score" :style="{ color: getProgressColor(val) }">{{ val }}</span>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 图表第二行：综合评分分布柱状图（全宽） -->
    <el-card class="chart-card chart-card-wide" shadow="never">
      <div class="bar-header">
        <h3 class="section-title" style="margin:0;">综合评分分布</h3>
        <div class="bar-header-badges">
          <span class="score-badge badge-green">
            <i class="el-icon-circle-check"></i>
            优秀（9–10）<strong>{{ stats.score_excellent }}</strong> 条
          </span>
          <span class="score-badge badge-blue">
            <i class="el-icon-success"></i>
            良好（7–9）<strong>{{ stats.score_good }}</strong> 条
          </span>
          <span class="score-badge badge-orange">
            <i class="el-icon-warning"></i>
            可接受（4–7）<strong>{{ stats.score_acceptable }}</strong> 条
          </span>
          <span class="score-badge badge-red">
            <i class="el-icon-error"></i>
            差（&lt;4）<strong>{{ stats.score_poor }}</strong> 条
          </span>
        </div>
      </div>
      <div class="bar-chart-wrapper" style="position:relative;">
        <canvas ref="barCanvas"></canvas>
        <div class="chart-tooltip" v-show="barTooltip.visible" :style="barTooltip.style">
          <div class="tooltip-label">{{ barTooltip.label }}</div>
          <div class="tooltip-value" :style="{ color: barTooltip.color }">{{ barTooltip.count }} 条</div>
          <div class="tooltip-pct">占比 {{ barTooltip.pct }}%</div>
        </div>
      </div>
<!--      <div class="bar-x-row">-->
<!--        <span class="bar-y-spacer"></span>-->
<!--        <div class="bar-x-labels">-->
<!--          <span v-for="l in ['0','1','2','3','4','5','6','7','8','9','10']" :key="l">{{ l }}</span>-->
<!--        </div>-->
<!--      </div>-->
      <div class="bar-color-legend">
        <span class="bcl-item"><span class="bcl-dot" style="background:#F56C6C;"></span>差（0–4）</span>
        <span class="bcl-item"><span class="bcl-dot" style="background:#E6A23C;"></span>可接受（4–7）</span>
        <span class="bcl-item"><span class="bcl-dot" style="background:#409EFF;"></span>良好（7–9）</span>
        <span class="bcl-item"><span class="bcl-dot" style="background:#67C23A;"></span>优秀（9–10）</span>
      </div>
    </el-card>

    <!-- 语料集导出 -->
    <el-card class="export-card" shadow="never">
      <h3 class="section-title">语料集导出</h3>
      <el-form :model="exportForm" label-width="140px">
        <el-form-item label="导出格式">
          <el-radio-group v-model="exportForm.format">
            <el-radio label="json">JSON</el-radio>
            <el-radio label="csv">CSV</el-radio>
            <el-radio label="txt">TXT（问答纯文本）</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="附加字段">
          <el-checkbox v-model="exportForm.include_score">包含各维度评分</el-checkbox>
          <el-checkbox v-model="exportForm.include_judgment" style="margin-left:16px;">包含判定结果</el-checkbox>
          <el-checkbox v-model="exportForm.include_sentences" style="margin-left:16px;">包含来源句子</el-checkbox>
        </el-form-item>
        <el-form-item label="质量筛选">
          <el-select v-model="exportForm.filter_quality" placeholder="全部" clearable @change="updateExportCount">
            <el-option label="全部" value=""></el-option>
            <el-option label="优秀（9–10分）" value="excellent"></el-option>
            <el-option label="良好（7–9分）" value="good"></el-option>
            <el-option label="可接受（4–7分）" value="acceptable"></el-option>
            <el-option label="差（<4分）" value="poor"></el-option>
          </el-select>
        </el-form-item>
      </el-form>
      <div class="export-preview">
        <p class="preview-text">
          <i class="el-icon-info"></i>
          根据当前条件，将导出 <strong>{{ exportCount }}</strong> 条问答对数据
        </p>
      </div>
      <div class="export-actions">
        <el-button type="primary" size="large" icon="el-icon-download" @click="handleDownload">
          下载语料集
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script>
const API_BASE = 'http://localhost:5000'

const PIE_COLORS   = { Accepted: '#67C23A', Reluctantly: '#E6A23C', Rejected: '#F56C6C' }

// Bucket colour: index 0-3 red, 4-6 orange, 7-8 blue, 9-10 green
const BAR_COLORS = [
  '#F56C6C','#F56C6C','#F56C6C','#F56C6C',
  '#E6A23C','#E6A23C','#E6A23C',
  '#409EFF','#409EFF',
  '#67C23A','#67C23A'
]

export default {
  name: 'QAManagement',
  data() {
    return {
      statsLoading: false,
      stats: {
        total: 0,
        score_excellent: 0,
        score_good: 0,
        score_acceptable: 0,
        score_poor: 0,
        overall_score: 0,
        judgment_distribution: {},
        dimension_averages: {}
      },
      exportForm: {
        format: 'json',
        include_score: false,
        include_judgment: false,
        include_sentences: false,
        filter_quality: ''
      },
      exportCount: 0,
      dimNameMap: {
        Consistency:  '一致性', Integrity:    '完整性', Accuracy:     '准确性',
        Independence: '独立性', Readability:  '可读性', Relevance:    '相关性', Simplicity: '简洁性'
      },
      pieSegments:  [],
      pieTooltip:   { visible: false, label: '', count: 0, pct: 0, color: '#fff', style: {} },
      radarTooltip: { visible: false, label: '', value: 0, color: '#fff', style: {} },
      radarPoints:  [],
      barTooltip:   { visible: false, label: '', count: 0, pct: 0, color: '#fff', style: {} },
    }
  },

  mounted() {
    this.loadStats()
  },

  methods: {
    judgmentCount(key) {
      return (this.stats.judgment_distribution || {})[key] || 0
    },

    //修改后
    async loadStats() {
      this.statsLoading = true
      try {
        const res  = await fetch(`${API_BASE}/api/qa/corpus/statistics`)
        this.stats = await res.json()
        this.exportCount = this.stats.total
      } catch (e) {
        this.$message.warning('加载统计数据失败，请确认后端服务已启动，使用演示数据')
        // fallback demo data - 添加 score_distribution 字段
        this.stats = {
          total: 48,
          score_excellent: 8,
          score_good: 27,
          score_acceptable: 12,
          score_poor: 1,
          overall_score: 8.24,
          judgment_distribution: { Accepted: 41, Reluctantly: 7, Rejected: 0 },
          dimension_averages: {
            Consistency: 7.92, Integrity: 6.88, Accuracy: 8.48,
            Independence: 8.48, Readability: 6.95, Relevance: 8.10, Simplicity: 7.60
          },
          score_distribution: [0, 0, 0, 1, 2, 4, 6, 12, 15, 6, 2]  // 新增这一行
        }
        this.exportCount = this.stats.total
      } finally {
        this.statsLoading = false
        this.$nextTick(() => {
          this.drawPie()
          this.drawRadar()
          this.drawBar()
        })
      }
    },

    /* ══════════════════════════════
       饼图（含引导线 + 标签）
    ══════════════════════════════ */
    drawPie() {
      const el = this.$refs.pieCanvas
      if (!el) return
      const dpr  = window.devicePixelRatio || 1
      const SIZE = 320
      el.style.width  = SIZE + 'px'
      el.style.height = SIZE + 'px'
      el.width  = SIZE * dpr
      el.height = SIZE * dpr
      const ctx = el.getContext('2d')
      ctx.scale(dpr, dpr)

      const dist  = this.stats.judgment_distribution || {}
      const total = Object.values(dist).reduce((a, b) => a + b, 0) || 1
      const cx = SIZE / 2, cy = SIZE / 2, R = 96

      const segments = []
      let startAngle = -Math.PI / 2
      for (const [label, count] of Object.entries(dist)) {
        if (count === 0) continue
        const angle = (count / total) * 2 * Math.PI
        segments.push({
          label, count,
          color: PIE_COLORS[label] || '#909399',
          startAngle,
          endAngle: startAngle + angle
        })
        startAngle += angle
      }
      this.pieSegments = segments

      const drawLabels = (hoverIdx) => {
        segments.forEach((seg, i) => {
          const midAngle = (seg.startAngle + seg.endAngle) / 2
          const ex = i === hoverIdx ? Math.cos(midAngle) * 8 : 0
          const ey = i === hoverIdx ? Math.sin(midAngle) * 8 : 0
          const lineR1 = R + (i === hoverIdx ? 10 : 6)
          const lineR2 = R + 26
          const x1 = cx + ex + Math.cos(midAngle) * lineR1
          const y1 = cy + ey + Math.sin(midAngle) * lineR1
          const x2 = cx + ex + Math.cos(midAngle) * lineR2
          const y2 = cy + ey + Math.sin(midAngle) * lineR2
          const isRight  = Math.cos(midAngle) >= 0
          const x3 = x2 + (isRight ? 18 : -18), y3 = y2
          ctx.beginPath()
          ctx.moveTo(x1, y1); ctx.lineTo(x2, y2); ctx.lineTo(x3, y3)
          ctx.strokeStyle = seg.color; ctx.lineWidth = 1.5; ctx.stroke()
          ctx.beginPath()
          ctx.arc(x3, y3, 2.5, 0, 2 * Math.PI)
          ctx.fillStyle = seg.color; ctx.fill()
          const pct   = ((seg.count / total) * 100).toFixed(1)
          const textX = x3 + (isRight ? 6 : -6)
          ctx.textAlign    = isRight ? 'left' : 'right'
          ctx.textBaseline = 'bottom'
          ctx.font         = 'bold 12px PingFang SC, Microsoft YaHei, sans-serif'
          ctx.fillStyle    = seg.color
          ctx.fillText(seg.label, textX, y3 - 1)
          ctx.textBaseline = 'top'
          ctx.font         = '11px PingFang SC, Microsoft YaHei, sans-serif'
          ctx.fillStyle    = '#606266'
          ctx.fillText(`${pct}%  (${seg.count}条)`, textX, y3 + 2)
        })
      }

      const render = (hoverIdx) => {
        ctx.clearRect(0, 0, SIZE, SIZE)
        segments.forEach((seg, i) => {
          const explode  = i === hoverIdx ? 8 : 0
          const midAngle = (seg.startAngle + seg.endAngle) / 2
          const ex = Math.cos(midAngle) * explode, ey = Math.sin(midAngle) * explode
          const r  = i === hoverIdx ? R + 4 : R
          ctx.beginPath()
          ctx.moveTo(cx + ex, cy + ey)
          ctx.arc(cx + ex, cy + ey, r, seg.startAngle, seg.endAngle)
          ctx.closePath()
          ctx.fillStyle   = seg.color; ctx.fill()
          ctx.strokeStyle = '#fff'; ctx.lineWidth = 2.5; ctx.stroke()
        })
        ctx.beginPath()
        ctx.arc(cx, cy, R * 0.46, 0, 2 * Math.PI)
        ctx.fillStyle = '#fff'; ctx.fill()
        ctx.fillStyle    = '#303133'
        ctx.font         = 'bold 13px PingFang SC, Microsoft YaHei, sans-serif'
        ctx.textAlign    = 'center'
        ctx.textBaseline = 'middle'
        ctx.fillText('判定分布', cx, cy - 11)
        ctx.font      = '12px PingFang SC, Microsoft YaHei, sans-serif'
        ctx.fillStyle = '#909399'
        ctx.fillText(`共 ${segments.reduce((a, s) => a + s.count, 0)} 条`, cx, cy + 13)
        drawLabels(hoverIdx)
      }

      render(-1)

      el.onmousemove = (e) => {
        const rect = el.getBoundingClientRect()
        const mx = e.clientX - rect.left, my = e.clientY - rect.top
        const dx = mx - cx, dy = my - cy, d = Math.sqrt(dx * dx + dy * dy)
        if (d > R || d < R * 0.46) { this.pieTooltip.visible = false; render(-1); return }
        let a = Math.atan2(dy, dx) + Math.PI / 2
        if (a < 0) a += 2 * Math.PI
        const hit = segments.findIndex(seg => {
          let sa = seg.startAngle + Math.PI / 2, ea = seg.endAngle + Math.PI / 2
          if (sa < 0) sa += 2 * Math.PI; if (ea < 0) ea += 2 * Math.PI
          if (ea < sa) ea += 2 * Math.PI
          let aa = a; if (aa < sa) aa += 2 * Math.PI
          return aa >= sa && aa <= ea
        })
        render(hit)
        if (hit >= 0) {
          const seg = segments[hit]
          this.pieTooltip = {
            visible: true, label: seg.label, count: seg.count,
            pct: ((seg.count / total) * 100).toFixed(1), color: seg.color,
            style: { left: (e.offsetX + 14) + 'px', top: (e.offsetY - 12) + 'px' }
          }
        } else { this.pieTooltip.visible = false }
      }
      el.onmouseleave = () => { this.pieTooltip.visible = false; render(-1) }
    },

    /* ══════════════════════════════
       雷达图
    ══════════════════════════════ */
    drawRadar() {
      const el = this.$refs.radarCanvas
      if (!el) return
      const dpr  = window.devicePixelRatio || 1
      const SIZE = 280
      el.style.width  = SIZE + 'px'
      el.style.height = SIZE + 'px'
      el.width  = SIZE * dpr
      el.height = SIZE * dpr
      const ctx = el.getContext('2d')
      ctx.scale(dpr, dpr)

      const avgs   = this.stats.dimension_averages || {}
      const keys   = Object.keys(avgs)
      if (!keys.length) return
      const values = keys.map(k => avgs[k])
      const cx = SIZE / 2, cy = SIZE / 2, R = SIZE / 2 - 42, n = keys.length

      const getXY = (i, r) => {
        const angle = (2 * Math.PI * i / n) - Math.PI / 2
        return { x: cx + r * Math.cos(angle), y: cy + r * Math.sin(angle) }
      }

      // Grid rings
      for (let lv = 1; lv <= 5; lv++) {
        const r = R * lv / 5
        ctx.beginPath()
        for (let i = 0; i < n; i++) {
          const { x, y } = getXY(i, r)
          i === 0 ? ctx.moveTo(x, y) : ctx.lineTo(x, y)
        }
        ctx.closePath()
        ctx.strokeStyle = lv === 5 ? '#C0C4CC' : '#EBEEF5'
        ctx.lineWidth   = lv === 5 ? 1.5 : 1
        ctx.stroke()
        ctx.fillStyle   = lv % 2 === 0 ? 'rgba(64,158,255,0.03)' : 'transparent'
        ctx.fill()
        // ring score label
        if (lv < 5) {
          ctx.fillStyle    = '#C0C4CC'
          ctx.font         = '10px PingFang SC, Microsoft YaHei, sans-serif'
          ctx.textAlign    = 'center'
          ctx.textBaseline = 'middle'
          ctx.fillText(lv * 2, cx + 4, cy - r + 4)
        }
      }

      // Spokes
      for (let i = 0; i < n; i++) {
        const { x, y } = getXY(i, R)
        ctx.beginPath()
        ctx.moveTo(cx, cy)
        ctx.lineTo(x, y)
        ctx.strokeStyle = '#DCDFE6'; ctx.lineWidth = 1; ctx.stroke()
      }

      // Data polygon fill
      ctx.beginPath()
      values.forEach((v, i) => {
        const { x, y } = getXY(i, R * v / 10)
        i === 0 ? ctx.moveTo(x, y) : ctx.lineTo(x, y)
      })
      ctx.closePath()
      ctx.fillStyle   = 'rgba(64,158,255,0.15)'; ctx.fill()
      ctx.strokeStyle = '#409EFF'; ctx.lineWidth = 2.5; ctx.stroke()

      // Data points
      const pts = []
      values.forEach((v, i) => {
        const { x, y } = getXY(i, R * v / 10)
        ctx.beginPath()
        ctx.arc(x, y, 5, 0, 2 * Math.PI)
        ctx.fillStyle   = '#fff'; ctx.fill()
        ctx.strokeStyle = '#409EFF'; ctx.lineWidth = 2; ctx.stroke()
        pts.push({ x, y, label: this.dimNameMap[keys[i]] || keys[i], value: v,
          color: this.getProgressColor(v) })
      })
      this.radarPoints = pts

      // Axis labels
      keys.forEach((k, i) => {
        const { x, y } = getXY(i, R + 24)
        ctx.font         = 'bold 12px PingFang SC, Microsoft YaHei, sans-serif'
        ctx.fillStyle    = '#606266'
        ctx.textAlign    = 'center'
        ctx.textBaseline = 'middle'
        ctx.fillText(this.dimNameMap[k] || k, x, y)
      })

      el.onmousemove = (e) => {
        const rect = el.getBoundingClientRect()
        const mx = e.clientX - rect.left, my = e.clientY - rect.top
        const hit = this.radarPoints.find(p => Math.hypot(mx - p.x, my - p.y) < 14)
        if (hit) {
          this.radarTooltip = {
            visible: true, label: hit.label, value: hit.value, color: hit.color,
            style: { left: (e.offsetX + 14) + 'px', top: (e.offsetY - 12) + 'px' }
          }
          el.style.cursor = 'pointer'
        } else {
          this.radarTooltip.visible = false; el.style.cursor = 'default'
        }
      }
      el.onmouseleave = () => { this.radarTooltip.visible = false }
    },

    /* ══════════════════════════════
   综合评分分布柱状图（修复版）
══════════════════════════════ */
    drawBar() {
      const el = this.$refs.barCanvas
      if (!el) return

      // 使用后端返回的真实分数分布
      let buckets = this.stats.score_distribution

      // 如果没有真实分布数据，使用全0数组
      if (!buckets || !Array.isArray(buckets) || buckets.length !== 11) {
        console.warn('未获取到真实的分数分布数据')
        buckets = new Array(11).fill(0)
      }

      const total = this.stats.total || 1
      const dpr = window.devicePixelRatio || 1
      const W = (el.parentElement ? el.parentElement.clientWidth : 0) || 700
      const H = 240  // 增加高度以容纳X轴标签
      el.style.width = W + 'px'
      el.style.height = H + 'px'
      el.width = W * dpr
      el.height = H * dpr
      const ctx = el.getContext('2d')
      ctx.scale(dpr, dpr)

      const PAD_L = 48, PAD_R = 20, PAD_T = 20, PAD_B = 40  // 增加底部内边距
      const cW = W - PAD_L - PAD_R
      const cH = H - PAD_T - PAD_B
      const maxV = Math.max(...buckets, 1)
      const n = buckets.length
      const slotW = cW / n
      const barW = slotW * 0.62
      const barRects = []

      const render = (hoverIdx) => {
        ctx.clearRect(0, 0, W, H)

        // Y轴网格线
        const yTicks = 5
        for (let i = 0; i <= yTicks; i++) {
          const y = PAD_T + cH - (i / yTicks) * cH
          const val = Math.round(maxV * i / yTicks)
          ctx.beginPath()
          ctx.moveTo(PAD_L, y)
          ctx.lineTo(PAD_L + cW, y)
          ctx.strokeStyle = i === 0 ? '#C0C4CC' : '#F0F2F5'
          ctx.lineWidth = i === 0 ? 1.5 : 1
          ctx.setLineDash(i === 0 ? [] : [4, 4])
          ctx.stroke()
          ctx.setLineDash([])
          ctx.fillStyle = '#909399'
          ctx.font = '11px PingFang SC, Microsoft YaHei, sans-serif'
          ctx.textAlign = 'right'
          ctx.textBaseline = 'middle'
          ctx.fillText(val, PAD_L - 6, y)
        }

        // 绘制X轴标签（0-10）
        for (let i = 0; i < n; i++) {
          const labelX = PAD_L + i * slotW + slotW / 2
          const labelY = PAD_T + cH + 18
          ctx.fillStyle = '#909399'
          ctx.font = '11px PingFang SC, Microsoft YaHei, sans-serif'
          ctx.textAlign = 'center'
          ctx.textBaseline = 'top'
          ctx.fillText(i.toString(), labelX, labelY)
        }

        barRects.length = 0
        buckets.forEach((v, i) => {
          const bh = v > 0 ? Math.max(4, (v / maxV) * cH) : 0
          const bx = PAD_L + i * slotW + (slotW - barW) / 2
          const by = PAD_T + cH - bh
          const col = BAR_COLORS[i]
          const hov = i === hoverIdx

          if (hov) {
            ctx.shadowColor = col
            ctx.shadowBlur = 12
            ctx.shadowOffsetY = 2
          }

          if (bh > 0) {
            ctx.beginPath()
            const rad = Math.min(5, barW / 2)
            ctx.moveTo(bx + rad, by)
            ctx.lineTo(bx + barW - rad, by)
            ctx.quadraticCurveTo(bx + barW, by, bx + barW, by + rad)
            ctx.lineTo(bx + barW, by + bh)
            ctx.lineTo(bx, by + bh)
            ctx.lineTo(bx, by + rad)
            ctx.quadraticCurveTo(bx, by, bx + rad, by)
            ctx.closePath()
            ctx.fillStyle = hov ? col : col + 'B0'
            ctx.fill()
          }

          ctx.shadowColor = 'transparent'
          ctx.shadowBlur = 0

          if (v > 0) {
            ctx.fillStyle = hov ? '#303133' : '#909399'
            ctx.font = hov ? 'bold 12px PingFang SC, Microsoft YaHei, sans-serif'
              : '10px PingFang SC, Microsoft YaHei, sans-serif'
            ctx.textAlign = 'center'
            ctx.textBaseline = 'bottom'
            ctx.fillText(v, bx + barW / 2, by - 2)
          }

          barRects.push({ x: bx, y: by, w: barW, h: bh, v, color: col, scoreIndex: i })
        })
      }

      render(-1)

      el.onmousemove = (e) => {
        const rect = el.getBoundingClientRect()
        const mx = e.clientX - rect.left
        const my = e.clientY - rect.top
        const hit = barRects.findIndex(b => mx >= b.x && mx <= b.x + b.w && my >= b.y && my <= b.y + b.h + 2)
        render(hit)
        if (hit >= 0 && barRects[hit].v > 0) {
          const b = barRects[hit]
          const pct = total > 0 ? ((b.v / total) * 100).toFixed(1) : '0.0'
          // 修复：正确的分数区间显示，不再出现 "10-11"
          let label
          if (b.scoreIndex === 10) {
            label = '10 分'
          } else {
            label = `${b.scoreIndex}–${b.scoreIndex + 1} 分`
          }
          this.barTooltip = {
            visible: true,
            label: label,
            count: b.v,
            pct: pct,
            color: b.color,
            style: { left: (e.offsetX + 14) + 'px', top: (e.offsetY - 44) + 'px' }
          }
          el.style.cursor = 'pointer'
        } else {
          this.barTooltip.visible = false
          el.style.cursor = 'default'
        }
      }

      el.onmouseleave = () => {
        this.barTooltip.visible = false
        render(-1)
      }
    },

    /* ══════════ 工具方法 ══════════ */
    async updateExportCount() {
      try {
        const res  = await fetch(`${API_BASE}/api/qa/corpus/export_count`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ filter_quality: this.exportForm.filter_quality })
        })
        const data = await res.json()
        this.exportCount = data.count
      } catch (e) { }
    },

    async handleDownload() {
      try {
        const res = await fetch(`${API_BASE}/api/qa/corpus/export`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(this.exportForm)
        })
        if (!res.ok) { const err = await res.json(); this.$message.error(err.error || '导出失败'); return }
        const blob = await res.blob()
        const url  = URL.createObjectURL(blob)
        const a    = document.createElement('a')
        a.href = url; a.download = `corpus.${this.exportForm.format}`; a.click()
        URL.revokeObjectURL(url)
        this.$message.success('导出成功')
      } catch (e) {
        this.$message.error('导出失败，请确认后端服务已启动')
      }
    },

    getProgressColor(score) {
      if (score >= 9) return '#67C23A'
      if (score >= 7) return '#409EFF'
      if (score >= 4) return '#E6A23C'
      return '#F56C6C'
    }
  }
}
</script>

<style scoped>
.qa-management-page { padding: 20px; }

/* 标题行 */
.page-header { margin-bottom: 20px; }
.header-top  { display: flex; justify-content: space-between; align-items: flex-start; }
.page-header h2 { margin: 0 0 8px; font-size: 24px; color: #303133; }
.page-desc { margin: 0; color: #909399; font-size: 14px; }

.stats-card, .export-card { margin-bottom: 20px; }
.section-title { margin: 0 0 20px; font-size: 18px; color: #303133; }

/* 统计卡片网格 */
.stats-grid-top {
  display: grid; grid-template-columns: 1fr 1fr;
  gap: 14px; margin-bottom: 14px;
}
.stats-grid-bottom {
  display: grid; grid-template-columns: repeat(3, 1fr); gap: 14px;
}
.stat-box {
  display: flex; align-items: center;
  padding: 16px 18px; background: #F5F7FA; border-radius: 10px;
  transition: box-shadow 0.2s;
}
.stat-box:hover { box-shadow: 0 4px 16px rgba(0,0,0,0.08); }
.stat-box-large { padding: 20px 22px; }
.stat-icon {
  width: 48px; height: 48px; border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  margin-right: 14px; flex-shrink: 0;
}
.stat-icon i { font-size: 22px; }
.stat-content { flex: 1; min-width: 0; }
.stat-label    { font-size: 14px; color: #606266; margin-bottom: 2px; }
.stat-subtitle { font-size: 12px; color: #909399; margin-bottom: 4px; }
.stat-value    { font-size: 26px; font-weight: bold; }
.score-inline  { display: flex; align-items: baseline; gap: 4px; margin-top: 4px; }
.score-big     { font-size: 28px; font-weight: bold; }
.score-denom   { font-size: 14px; color: #909399; }

/* 图表行 */
.charts-row {
  display: grid; grid-template-columns: 1fr 1fr;
  gap: 20px; margin-bottom: 20px;
}
.chart-card { position: relative; overflow: visible; }
.chart-card-wide { margin-bottom: 20px; }

.chart-wrapper {
  position: relative; display: flex;
  justify-content: center; margin-bottom: 16px;
}
.chart-wrapper canvas { display: block; }

/* Tooltip */
.chart-tooltip {
  position: absolute;
  background: rgba(25, 25, 35, 0.92); color: #fff;
  border-radius: 8px; padding: 8px 14px;
  pointer-events: none; z-index: 20;
  white-space: nowrap;
  box-shadow: 0 6px 18px rgba(0,0,0,0.25);
  backdrop-filter: blur(6px);
  border: 1px solid rgba(255,255,255,0.08);
}
.tooltip-label { font-size: 12px; color: #bbb; margin-bottom: 4px; font-weight: 500; }
.tooltip-value { font-size: 20px; font-weight: bold; line-height: 1.2; }
.tooltip-pct   { font-size: 11px; color: #aaa; margin-top: 3px; }

/* 饼图图例 */
.pie-legend  { display: flex; flex-direction: column; gap: 8px; padding: 0 8px; }
.legend-item { display: flex; align-items: center; gap: 8px; }
.legend-dot  { width: 11px; height: 11px; border-radius: 50%; flex-shrink: 0; }
.legend-text { flex: 1; font-size: 14px; color: #606266; }
.legend-count{ font-size: 14px; font-weight: 600; }

/* 雷达图条形图例 */
.radar-legend       { display: flex; flex-direction: column; gap: 8px; padding: 0 8px; }
.radar-legend-item  { display: flex; align-items: center; gap: 8px; }
.radar-dim-name     { width: 48px; font-size: 13px; color: #606266; flex-shrink: 0; }
.radar-bar-wrap     { flex: 1; height: 8px; background: #EBEEF5; border-radius: 4px; overflow: hidden; }
.radar-bar-fill     { height: 100%; border-radius: 4px; transition: width 0.5s ease; }
.radar-dim-score    { width: 36px; text-align: right; font-size: 13px; font-weight: 600; flex-shrink: 0; }

/* 柱状图区域 */
.bar-header {
  display: flex; align-items: center;
  justify-content: space-between;
  margin-bottom: 20px; flex-wrap: wrap; gap: 12px;
}
.bar-header-badges { display: flex; gap: 12px; flex-wrap: wrap; }
.score-badge {
  display: flex; align-items: center; gap: 6px;
  padding: 4px 12px; border-radius: 20px; font-size: 13px;
}
.score-badge i { font-size: 14px; }
.score-badge strong { font-size: 15px; margin-left: 2px; }
.badge-green  { background: #F0F9EB; color: #67C23A; }
.badge-blue   { background: #EEF4FF; color: #409EFF; }
.badge-orange { background: #FDF6EC; color: #E6A23C; }
.badge-red    { background: #FEF0F0; color: #F56C6C; }

.bar-chart-wrapper { width: 100%; }
.bar-chart-wrapper canvas { display: block; width: 100% !important; }

.bar-x-row { display: flex; align-items: center; margin-top: 4px; }
.bar-y-spacer { width: 48px; flex-shrink: 0; }
.bar-x-labels {
  flex: 1; display: flex; justify-content: space-between;
}
.bar-x-labels span { font-size: 11px; color: #909399; text-align: center; }

.bar-color-legend {
  display: flex; justify-content: center;
  gap: 20px; margin-top: 12px; flex-wrap: wrap;
}
.bcl-item { display: flex; align-items: center; gap: 6px; font-size: 12px; color: #606266; }
.bcl-dot  { width: 10px; height: 10px; border-radius: 2px; flex-shrink: 0; }

/* 导出区域 */
.export-preview {
  margin: 20px 0; padding: 12px 16px;
  background: #F0F9FF; border-radius: 4px; border: 1px solid #B3D8FF;
}
.preview-text { margin: 0; color: #409EFF; font-size: 14px; }
.preview-text i { margin-right: 8px; }
.preview-text strong { font-size: 16px; margin: 0 4px; }
.export-actions { text-align: center; margin-top: 20px; }

/* 响应式 */
@media (max-width: 1100px) {
  .stats-grid-top    { grid-template-columns: 1fr 1fr; }
  .stats-grid-bottom { grid-template-columns: repeat(3, 1fr); }
}
@media (max-width: 900px) {
  .stats-grid-top    { grid-template-columns: 1fr; }
  .stats-grid-bottom { grid-template-columns: 1fr 1fr; }
  .charts-row        { grid-template-columns: 1fr; }
  .bar-header        { flex-direction: column; align-items: flex-start; }
}
</style>
