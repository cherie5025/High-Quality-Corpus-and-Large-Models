<template>
  <div class="qa-evaluate-page">
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

    <!-- 配置卡片 -->
    <el-card class="intro-card" shadow="never">
      <div class="intro-content">
        <h3>评估配置</h3>
        <p>本系统从 7 个维度评估问答对：
          <strong>一致性</strong>、<strong>完整性</strong>、<strong>准确性</strong>、
          <strong>独立性</strong>、<strong>可读性</strong>、<strong>相关性</strong>、<strong>简洁性</strong>。
          每维度 0–10 分，最终给出 Accepted / Reluctantly / Rejected 判定。
        </p>

        <div class="dimension-intro">
          <el-collapse>
            <el-collapse-item title="评估维度详细说明" name="1">
              <div class="dimension-detail">
                <p><strong>一致性（Consistency）：</strong>答案是否直接明确地回应问题核心，信息无矛盾，深度广度符合预期。</p>
                <p><strong>完整性（Integrity）：</strong>答案是否覆盖所有核心要点，无关键遗漏，提供必要的背景说明。</p>
                <p><strong>准确性（Accuracy）：</strong>事实陈述、数据、日期等信息准确无误，推理逻辑严密。</p>
                <p><strong>独立性（Independence）：</strong>答案以独立完整的句子表达，不依赖外部引用，信息经过内化理解。</p>
                <p><strong>可读性（Readability）：</strong>语言流畅符合语法，结构清晰，句段连接自然。</p>
                <p><strong>相关性（Relevance）：</strong>答案所有部分紧密相关问题，无冗余信息，不跑题。</p>
                <p><strong>简洁性（Simplicity）：</strong>表达简洁精炼，直奔主题，信息密度高。</p>
                <p class="score-level-intro"><strong>评分等级：</strong>优秀(9-10)、良好(7-8)、可接受(4-6)、差(0-3)</p>
              </div>
            </el-collapse-item>
          </el-collapse>
        </div>

        <div class="model-select-section">
          <label class="model-label">评估模型</label>
          <el-select v-model="evaluateForm.model" style="width: 300px;">
            <el-option label="DeepSeek-R1" value="deepseek-ai/DeepSeek-R1"></el-option>
            <el-option label="GLM-4.7" value="Pro/zai-org/GLM-4.7"></el-option>
            <el-option label="Qwen3-VL-235B" value="Qwen/Qwen3-VL-235B-A22B-Thinking"></el-option>
          </el-select>
        </div>
      </div>
    </el-card>

    <!-- 操作按钮（未开始前） -->
    <div class="action-section" v-if="!taskId && evaluatedData.length === 0">
      <el-button type="primary" size="large" @click="handleStartEvaluate">
        开始评估
      </el-button>
      <el-button size="large" @click="$refs.fileInput.click()">
        上传已评估的 JSON 文件
      </el-button>
      <input type="file" ref="fileInput" style="display:none" accept=".json" @change="handleFileChange">
    </div>

    <!-- 评估进度 -->
    <el-card class="progress-card" shadow="never"
             v-if="taskId && taskStatus.status !== 'done' && taskStatus.status !== 'error'">
      <h3 class="section-title">评估进度</h3>
      <el-progress :percentage="evaluateProgress" :stroke-width="14"></el-progress>
      <p class="progress-text">
        正在评估第 {{ taskStatus.done || 0 }} / {{ taskStatus.total || '?' }} 个问答对…
        <span v-if="taskStatus.estimated_remaining_seconds">
          预计剩余 {{ formatTime(taskStatus.estimated_remaining_seconds) }}
        </span>
      </p>
    </el-card>

    <!-- 评估出错 -->
    <el-alert v-if="taskStatus.status === 'error'" type="error"
              :title="'评估出错：' + taskStatus.error" :closable="false" show-icon
              style="margin-bottom:20px;">
    </el-alert>

    <!-- ✦ 评估趋势折线图 -->
    <el-card class="progress-card" shadow="never" v-if="evalLinePoints.length > 1">
      <div class="chart-title-row">
        <span class="chart-title-text">评估趋势</span>
        <span class="chart-subtitle">每隔 2 秒记录一次累计已评估量</span>
      </div>
      <div class="line-chart-wrap">
        <canvas ref="evalLineCanvas"></canvas>
      </div>
    </el-card>



    <!-- 评估结果表格 -->
    <el-card class="result-card" shadow="never" v-if="paginatedData.length > 0 || evaluatedData.length > 0">
      <div class="result-header">
        <h3 class="section-title">
          评估结果（共 {{ filteredData.length }} 条，已删除 {{ deletedCount }} 条）
        </h3>
        <div class="result-actions">
          <el-button size="small" icon="el-icon-delete"
                     :disabled="selectedRows.length === 0"
                     @click="handleDeleteSelected">
            删除选中 ({{ selectedRows.length }})
          </el-button>
          <el-button size="small" icon="el-icon-refresh-left"
                     :disabled="deletedHistory.length === 0"
                     @click="handleUndo">
            撤回上一步
          </el-button>
          <el-button size="small" type="primary" icon="el-icon-download" @click="handleExport">
            导出结果
          </el-button>
        </div>
      </div>

      <!-- 筛选器 -->
      <div class="filter-bar">
        <el-form :inline="true" size="small">
          <el-form-item label="判定结果">
            <el-select v-model="filterJudgment" placeholder="全部" clearable @change="handleFilter">
              <el-option label="全部" value=""></el-option>
              <el-option label="Accepted" value="Accepted"></el-option>
              <el-option label="Reluctantly" value="Reluctantly"></el-option>
              <el-option label="Rejected" value="Rejected"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="综合评分">
            <el-select v-model="filterScoreRange" placeholder="全部" clearable @change="handleFilter">
              <el-option label="全部" value=""></el-option>
              <el-option label="优秀 (9–10)" value="excellent"></el-option>
              <el-option label="良好 (7–9)" value="good"></el-option>
              <el-option label="可接受 (4–7)" value="acceptable"></el-option>
              <el-option label="差 (<4)" value="poor"></el-option>
            </el-select>
          </el-form-item>
        </el-form>
      </div>

      <el-table
        ref="qaTable"
        :data="paginatedData"
        border
        @selection-change="handleSelectionChange"
        style="width: 100%"
        max-height="600">
        <el-table-column type="selection" width="55" fixed="left"></el-table-column>
        <el-table-column type="index" label="序号" width="60" fixed="left" :index="indexMethod"></el-table-column>

        <el-table-column prop="question" label="问题" min-width="200" fixed="left">
          <template slot-scope="scope">
            <div class="question-cell">{{ truncate(scope.row.question) }}</div>
          </template>
        </el-table-column>

        <el-table-column prop="answer" label="答案" min-width="250">
          <template slot-scope="scope">
            <div class="answer-cell">{{ truncate(scope.row.answer) }}</div>
          </template>
        </el-table-column>

        <el-table-column prop="sentences" label="来源句子" min-width="200">
          <template slot-scope="scope">
            <div class="sentence-cell">{{ truncate(scope.row.sentences) }}</div>
          </template>
        </el-table-column>

        <el-table-column v-for="dim in dimensions" :key="dim.key"
                         :label="dim.label" width="90" sortable>
          <template slot-scope="scope">
            <span :class="getScoreClass(scope.row.score && scope.row.score[dim.key])">
              {{ scope.row.score ? Math.round(scope.row.score[dim.key]) : '–' }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="综合评分" width="100" sortable :sort-method="sortByAvgScore">
          <template slot-scope="scope">
            <span :class="getScoreClass(getAvgScore(scope.row.score))" style="font-weight:bold;">
              {{ getAvgScore(scope.row.score).toFixed(1) }}
            </span>
          </template>
        </el-table-column>

        <el-table-column prop="Judgment" label="判定结果" width="120" sortable fixed="right">
          <template slot-scope="scope">
            <el-tag size="small" :type="getJudgmentType(scope.row.Judgment)" effect="dark">
              {{ scope.row.Judgment }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="100" fixed="right">
          <template slot-scope="scope">
            <el-button type="text" size="small" @click="handleViewDetail(scope.row)">详情</el-button>
            <el-button type="text" size="small" style="color:#F56C6C;" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
          :current-page="currentPage"
          :page-sizes="[10, 20, 50, 100]"
          :page-size="pageSize"
          layout="total, sizes, prev, pager, next, jumper"
          :total="filteredData.length">
        </el-pagination>
      </div>
    </el-card>

    <!-- 底部操作 -->
    <div class="action-buttons" v-if="evaluatedData.length > 0">
      <el-button size="large" @click="$router.push('/view/qa')">← 返回生成</el-button>
      <el-button type="primary" size="large" @click="$router.push('/view/qa/management')">
        下一步：语料集管理 →
      </el-button>
    </div>

    <!-- 详情对话框 -->
    <el-dialog title="问答对详情" :visible.sync="detailDialogVisible" width="800px" :close-on-click-modal="false">
      <div v-if="currentDetail" class="detail-content">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="问题">
            <div class="detail-text">{{ currentDetail.question }}</div>
          </el-descriptions-item>
          <el-descriptions-item label="答案">
            <div class="detail-text">{{ currentDetail.answer }}</div>
          </el-descriptions-item>
          <el-descriptions-item label="来源句子">
            <div class="detail-text">{{ currentDetail.sentences }}</div>
          </el-descriptions-item>
        </el-descriptions>

        <div class="score-detail-section" v-if="currentDetail.score">
          <h4>评估得分详情</h4>
          <el-row :gutter="20">
            <el-col :span="12" v-for="dim in dimensions" :key="dim.key">
              <div class="score-item">
                <span class="score-name">{{ dim.label }}：</span>
                <span :class="getScoreClass(currentDetail.score[dim.key])" class="score-value-large">
                  {{ Math.round(currentDetail.score[dim.key]) }}
                </span>
                <span class="score-level">{{ getScoreLevel(currentDetail.score[dim.key]) }}</span>
              </div>
            </el-col>
          </el-row>
          <el-divider></el-divider>
          <div class="avg-score-display">
            <span class="avg-label">综合评分：</span>
            <span :class="getScoreClass(getAvgScore(currentDetail.score))" class="avg-score">
              {{ getAvgScore(currentDetail.score).toFixed(2) }}
            </span>
          </div>
          <div class="judgment-display">
            <span class="judgment-label">判定结果：</span>
            <el-tag :type="getJudgmentType(currentDetail.Judgment)" effect="dark" size="large">
              {{ currentDetail.Judgment }}
            </el-tag>
          </div>
        </div>
      </div>
      <span slot="footer">
        <el-button @click="detailDialogVisible = false">关 闭</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
const API_BASE = 'http://localhost:5000'

const J_COLORS = { Accepted: '#67C23A', Reluctantly: '#E6A23C', Rejected: '#F56C6C' }
const DIM_ORDER = ['Consistency','Integrity','Accuracy','Independence','Readability','Relevance','Simplicity']
const DIM_CN    = {
  Consistency:  '一致性', Integrity: '完整性', Accuracy:  '准确性',
  Independence: '独立性', Readability: '可读性', Relevance: '相关性', Simplicity: '简洁性'
}

export default {
  name: 'QAEvaluate',
  data() {
    return {
      evaluateForm: { model: 'deepseek-ai/DeepSeek-R1' },
      taskId: null,
      taskStatus: {},
      evaluatedData: [],
      deletedHistory: [],
      deletedCount: 0,
      filterJudgment: '',
      filterScoreRange: '',
      selectedRows: [],
      currentPage: 1,
      pageSize: 10,
      detailDialogVisible: false,
      currentDetail: null,
      pollTimer: null,
      evalLinePoints: [],  // { done, total } per poll tick
      dimensions: [
        { key: 'Consistency',  label: '一致性' },
        { key: 'Integrity',    label: '完整性' },
        { key: 'Accuracy',     label: '准确性' },
        { key: 'Independence', label: '独立性' },
        { key: 'Readability',  label: '可读性' },
        { key: 'Relevance',    label: '相关性' },
        { key: 'Simplicity',   label: '简洁性' },
      ]
    }
  },

  computed: {
    evaluateProgress() {
      const { done, total } = this.taskStatus
      if (!total) return 0
      return Math.min(100, Math.floor((done / total) * 100))
    },

    filteredData() {
      let data = [...this.evaluatedData]
      if (this.filterJudgment) data = data.filter(d => d.Judgment === this.filterJudgment)
      if (this.filterScoreRange) {
        data = data.filter(d => {
          const avg = this.getAvgScore(d.score)
          if (this.filterScoreRange === 'excellent')  return avg >= 9
          if (this.filterScoreRange === 'good')       return avg >= 7 && avg < 9
          if (this.filterScoreRange === 'acceptable') return avg >= 4 && avg < 7
          if (this.filterScoreRange === 'poor')       return avg < 4
          return true
        })
      }
      return data
    },

    paginatedData() {
      const start = (this.currentPage - 1) * this.pageSize
      return this.filteredData.slice(start, start + this.pageSize)
    },

    // Judgment donut data
    judgmentItems() {
      const total = this.evaluatedData.length || 1
      return ['Accepted','Reluctantly','Rejected'].map(key => {
        const count = this.evaluatedData.filter(d => d.Judgment === key).length
        return { key, count, color: J_COLORS[key], pct: ((count / total) * 100).toFixed(1) }
      })
    },

    overallAvgScore() {
      if (!this.evaluatedData.length) return '–'
      const sum = this.evaluatedData.reduce((acc, d) => acc + this.getAvgScore(d.score), 0)
      return (sum / this.evaluatedData.length).toFixed(2)
    },

    avgScoreColor() {
      const s = parseFloat(this.overallAvgScore)
      if (s >= 9) return '#67C23A'
      if (s >= 7) return '#409EFF'
      if (s >= 4) return '#E6A23C'
      return '#F56C6C'
    },

    // 7-dimension average bar data
    dimensionStats() {
      if (!this.evaluatedData.length) return []
      return DIM_ORDER.map(key => {
        const vals = this.evaluatedData
          .filter(d => d.score && d.score[key] !== undefined)
          .map(d => d.score[key])
        const avg = vals.length ? +(vals.reduce((a, b) => a + b, 0) / vals.length).toFixed(2) : 0
        let color = '#F56C6C'
        if (avg >= 9) color = '#67C23A'
        else if (avg >= 7) color = '#409EFF'
        else if (avg >= 4) color = '#E6A23C'
        return { key, label: DIM_CN[key], avg, color }
      })
    }
  },

  watch: {
    evaluatedData(val) {
      //if (val.length > 0) {
      //  this.$nextTick(() => this.drawDonut())
      //}
    }
  },

  methods: {
    truncate(text, limit = 50) {
      if (!text) return ''
      return text.length > limit ? text.slice(0, limit) + '…' : text
    },

    async loadExistingData() {
      try {
        const res  = await fetch(`${API_BASE}/api/qa/evaluate/data?page=1&page_size=10000`)
        const data = await res.json()
        if (data.total > 0) {
          this.evaluatedData = data.data
          this.deletedCount  = 0
        }
      } catch (e) { /* 后端可能未启动 */ }
    },

    async handleStartEvaluate() {
      try {
        const res  = await fetch(`${API_BASE}/api/qa/evaluate/start`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ model: this.evaluateForm.model })
        })
        const data = await res.json()
        if (!res.ok) { this.$message.error(data.error || '启动失败'); return }
        this.taskId       = data.task_id
        this.evaluatedData = []
        this.evalLinePoints = []
        this.$message.success(`评估任务已启动，共 ${data.total} 个问答对`)
        this.startPolling()
      } catch (e) {
        this.$message.error('连接后端失败，请确认服务已启动（python app.py）')
      }
    },

    startPolling() {
      this.pollTimer = setInterval(async () => {
        try {
          const res  = await fetch(`${API_BASE}/api/qa/evaluate/status/${this.taskId}`)
          const data = await res.json()
          this.taskStatus = data
          this.evalLinePoints.push({
            done:  data.done  || 0,
            total: data.total || 0
          })
          this.$nextTick(() => this.drawEvalLineChart())
          if (data.status === 'done') {
            clearInterval(this.pollTimer)
            this.$message.success('评估完成！')
            await this.loadExistingData()
          } else if (data.status === 'error') {
            clearInterval(this.pollTimer)
            this.$message.error('评估出错：' + data.error)
          }
        } catch (e) { }
      }, 2000)
    },

    handleFileChange(event) {
      const file = event.target.files[0]
      if (!file) return
      const formData = new FormData()
      formData.append('file', file)
      fetch(`${API_BASE}/api/qa/evaluate/upload`, { method: 'POST', body: formData })
        .then(r => r.json())
        .then(data => { this.$message.success(data.message); this.loadExistingData() })
        .catch(() => this.$message.error('上传失败'))
      event.target.value = ''
    },

    /* ══ 评估趋势折线图 ══ */
    drawEvalLineChart() {
      const el = this.$refs.evalLineCanvas
      if (!el || this.evalLinePoints.length < 2) return

      const dpr = window.devicePixelRatio || 1
      const W   = el.parentElement.clientWidth || 640
      const H   = 200
      el.style.width  = W + 'px'
      el.style.height = H + 'px'
      el.width  = W * dpr
      el.height = H * dpr
      const ctx = el.getContext('2d')
      ctx.scale(dpr, dpr)

      const PAD_L = 52, PAD_R = 24, PAD_T = 16, PAD_B = 36
      const cW    = W - PAD_L - PAD_R
      const cH    = H - PAD_T - PAD_B

      const pts   = this.evalLinePoints
      const total = pts[pts.length - 1].total || Math.max(...pts.map(p => p.done)) || 1
      const yMax  = total

      ctx.clearRect(0, 0, W, H)

      // Grid & Y labels
      const yTicks = 5
      for (let i = 0; i <= yTicks; i++) {
        const y   = PAD_T + cH - (i / yTicks) * cH
        const val = Math.round(yMax * i / yTicks)
        ctx.beginPath()
        ctx.moveTo(PAD_L, y)
        ctx.lineTo(PAD_L + cW, y)
        ctx.strokeStyle = i === 0 ? '#C0C4CC' : '#EBEEF5'
        ctx.lineWidth   = i === 0 ? 1.5 : 1
        ctx.setLineDash(i === 0 ? [] : [4, 4])
        ctx.stroke()
        ctx.setLineDash([])
        ctx.fillStyle    = '#909399'
        ctx.font         = '11px PingFang SC, Microsoft YaHei, sans-serif'
        ctx.textAlign    = 'right'
        ctx.textBaseline = 'middle'
        ctx.fillText(val, PAD_L - 6, y)
      }

      // X axis labels — fixed interval ticks
      const niceIntervals = [1, 2, 5, 10, 20, 25, 50, 100, 200, 500]
      const rawInterval   = total / 7
      const tickInterval  = niceIntervals.find(v => v >= rawInterval) || niceIntervals[niceIntervals.length - 1]
      ctx.fillStyle    = '#909399'
      ctx.font         = '11px PingFang SC, Microsoft YaHei, sans-serif'
      ctx.textAlign    = 'center'
      ctx.textBaseline = 'top'
      for (let v = 0; v <= total; v += tickInterval) {
        const x = PAD_L + (v / total) * cW
        ctx.fillText(v, x, PAD_T + cH + 6)
      }
      if (total % tickInterval !== 0) {
        ctx.fillText(total, PAD_L + cW, PAD_T + cH + 6)
      }

      // Gradient fill
      const grad = ctx.createLinearGradient(0, PAD_T, 0, PAD_T + cH)
      grad.addColorStop(0, 'rgba(103,194,58,0.25)')
      grad.addColorStop(1, 'rgba(103,194,58,0.02)')
      ctx.beginPath()
      pts.forEach((p, i) => {
        const x = PAD_L + (p.done / total) * cW
        const y = PAD_T + cH - (p.done / yMax) * cH
        i === 0 ? ctx.moveTo(x, y) : ctx.lineTo(x, y)
      })
      const lp = pts[pts.length - 1]
      ctx.lineTo(PAD_L + (lp.done / total) * cW, PAD_T + cH)
      ctx.lineTo(PAD_L, PAD_T + cH)
      ctx.closePath()
      ctx.fillStyle = grad
      ctx.fill()

      // Line
      ctx.beginPath()
      pts.forEach((p, i) => {
        const x = PAD_L + (p.done / total) * cW
        const y = PAD_T + cH - (p.done / yMax) * cH
        i === 0 ? ctx.moveTo(x, y) : ctx.lineTo(x, y)
      })
      ctx.strokeStyle = '#67C23A'
      ctx.lineWidth   = 2.5
      ctx.lineJoin    = 'round'
      ctx.stroke()

      // Last point dot + callout
      const lx = PAD_L + (lp.done / total) * cW
      const ly = PAD_T + cH - (lp.done / yMax) * cH
      ctx.beginPath()
      ctx.arc(lx, ly, 5, 0, 2 * Math.PI)
      ctx.fillStyle   = '#fff'
      ctx.fill()
      ctx.strokeStyle = '#67C23A'
      ctx.lineWidth   = 2.5
      ctx.stroke()

      ctx.fillStyle    = '#303133'
      ctx.font         = 'bold 12px PingFang SC, Microsoft YaHei, sans-serif'
      ctx.textAlign    = lx > W - 90 ? 'right' : 'left'
      ctx.textBaseline = 'bottom'
      ctx.fillText(`${lp.done} 条`, lx + (lx > W - 90 ? -8 : 8), ly - 4)

      // Axis labels
      ctx.fillStyle    = '#C0C4CC'
      ctx.font         = '11px PingFang SC, Microsoft YaHei, sans-serif'
      ctx.textAlign    = 'center'
      ctx.textBaseline = 'top'
      ctx.fillText('已评估数量', PAD_L + cW / 2, PAD_T + cH + 22)
      ctx.save()
      ctx.translate(12, PAD_T + cH / 2)
      ctx.rotate(-Math.PI / 2)
      ctx.textAlign    = 'center'
      ctx.textBaseline = 'middle'
      ctx.fillText('累计评估量（条）', 0, 0)
      ctx.restore()
    },

    /* ══ 环形图 ══ */
    drawDonut() {
      const el = this.$refs.donutCanvas
      if (!el) return
      const dpr  = window.devicePixelRatio || 1
      const SIZE = 140
      el.style.width  = SIZE + 'px'
      el.style.height = SIZE + 'px'
      el.width  = SIZE * dpr
      el.height = SIZE * dpr
      const ctx = el.getContext('2d')
      ctx.scale(dpr, dpr)

      const cx = SIZE / 2, cy = SIZE / 2, R = 56, r = 34
      const items = this.judgmentItems
      const total = items.reduce((a, b) => a + b.count, 0) || 1

      let angle = -Math.PI / 2
      items.forEach(item => {
        if (!item.count) return
        const sweep = (item.count / total) * 2 * Math.PI
        ctx.beginPath()
        ctx.moveTo(cx, cy)
        ctx.arc(cx, cy, R, angle, angle + sweep)
        ctx.closePath()
        ctx.fillStyle = item.color
        ctx.fill()
        ctx.strokeStyle = '#fff'
        ctx.lineWidth   = 2.5
        ctx.stroke()
        angle += sweep
      })

      // inner hole
      ctx.beginPath()
      ctx.arc(cx, cy, r, 0, 2 * Math.PI)
      ctx.fillStyle = '#fff'
      ctx.fill()
    },

    /* ══ 工具 ══ */
    getAvgScore(scoreObj) {
      if (!scoreObj) return 0
      const vals = Object.values(scoreObj)
      return vals.reduce((a, b) => a + b, 0) / vals.length
    },

    getJudgmentType(j) {
      return { Accepted: 'success', Reluctantly: 'warning', Rejected: 'danger' }[j] || 'info'
    },

    getScoreClass(score) {
      if (score >= 9) return 'score-excellent'
      if (score >= 7) return 'score-good'
      if (score >= 4) return 'score-acceptable'
      return 'score-poor'
    },

    getScoreLevel(score) {
      if (score >= 9) return '优秀'
      if (score >= 7) return '良好'
      if (score >= 4) return '可接受'
      return '差'
    },

    sortByAvgScore(a, b) { return this.getAvgScore(a.score) - this.getAvgScore(b.score) },
    indexMethod(index)   { return (this.currentPage - 1) * this.pageSize + index + 1 },
    handleFilter()       { this.currentPage = 1 },
    handleSelectionChange(sel) { this.selectedRows = sel },
    handleSizeChange(s)  { this.pageSize = s; this.currentPage = 1 },
    handlePageChange(p)  { this.currentPage = p },

    async handleDeleteSelected() {
      const count = this.selectedRows.length
      await this.$confirm(`确定删除选中的 ${count} 个问答对？可通过"撤回"恢复。`, '提示',
        { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
      const questions = this.selectedRows.map(r => r.question)
      try {
        await fetch(`${API_BASE}/api/qa/evaluate/delete`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ questions })
        })
        this.deletedHistory.push({ questions })
        this.deletedCount += count
        this.evaluatedData = this.evaluatedData.filter(d => !questions.includes(d.question))
        this.$refs.qaTable.clearSelection()
        this.$message.success(`已删除 ${count} 个，可撤回恢复`)
      } catch (e) { this.$message.error('删除失败') }
    },

    async handleDelete(row) {
      await this.$confirm('确定删除该问答对？可通过"撤回"恢复。', '提示',
        { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
      const questions = [row.question]
      await fetch(`${API_BASE}/api/qa/evaluate/delete`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ questions })
      })
      this.deletedHistory.push({ questions })
      this.deletedCount++
      this.evaluatedData = this.evaluatedData.filter(d => d.question !== row.question)
      this.$message.success('已删除，可撤回恢复')
    },

    async handleUndo() {
      if (!this.deletedHistory.length) return
      const last = this.deletedHistory.pop()
      try {
        await fetch(`${API_BASE}/api/qa/evaluate/undo`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ questions: last.questions })
        })
        this.deletedCount -= last.questions.length
        await this.loadExistingData()
        this.$message.success(`已恢复 ${last.questions.length} 个问答对`)
      } catch (e) { this.$message.error('撤回失败') }
    },

    handleViewDetail(row) { this.currentDetail = row; this.detailDialogVisible = true },
    handleExport()        { window.open(`${API_BASE}/api/qa/evaluate/export`, '_blank') },

    formatTime(s) {
      if (!s) return ''
      const m = Math.floor(s / 60)
      return m > 0 ? `${m}分${s % 60}秒` : `${s}秒`
    }
  },

  beforeDestroy() {
    if (this.pollTimer) clearInterval(this.pollTimer)
  }
}
</script>

<style scoped>
.qa-evaluate-page { padding: 20px; }

.page-header { margin-bottom: 20px; }
.header-top { display: flex; justify-content: space-between; align-items: flex-start; }
.page-header h2 { margin: 0 0 8px; font-size: 24px; color: #303133; }
.page-desc { margin: 0; color: #909399; font-size: 14px; }

.intro-card, .progress-card, .result-card { margin-bottom: 20px; }
.intro-content h3 { margin-top: 0; margin-bottom: 12px; font-size: 18px; color: #303133; }
.intro-content p  { margin: 0 0 8px; color: #606266; line-height: 1.6; }
.section-title    { margin: 0 0 20px; font-size: 18px; color: #303133; }

.dimension-intro  { margin-top: 20px; }
.dimension-detail p { margin: 8px 0; line-height: 1.8; color: #606266; }
.score-level-intro  { color: #409EFF; margin-top: 12px; padding-top: 12px; border-top: 1px solid #EBEEF5; }

.model-select-section {
  margin-top: 20px; padding-top: 20px;
  border-top: 1px solid #EBEEF5;
  display: flex; align-items: center;
}
.model-label { font-size: 14px; color: #606266; margin-right: 16px; font-weight: 500; }

.action-section { text-align: center; margin: 30px 0; }
.action-section .el-button { margin: 0 10px; }

.progress-text { margin-top: 12px; text-align: center; color: #909399; font-size: 14px; }

/* ══ 评估图表看板 ══ */
.eval-charts-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 20px;
}
.eval-chart-card { }
.eval-chart-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 18px;
  padding-bottom: 10px;
  border-bottom: 1px solid #F0F2F5;
}

/* 环形图区域 */
.donut-body {
  display: flex;
  align-items: center;
  gap: 24px;
}
.donut-canvas-wrap {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}
.donut-canvas-wrap canvas { display: block; }
.donut-center {
  position: absolute;
  top: 50%; left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
  pointer-events: none;
}
.donut-total { font-size: 22px; font-weight: 700; color: #303133; line-height: 1; }
.donut-label { font-size: 11px; color: #909399; margin-top: 3px; }

.donut-legend { flex: 1; }
.donut-legend-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #F5F7FA;
}
.donut-legend-item:last-child { border-bottom: none; }
.donut-legend-left { display: flex; align-items: center; gap: 8px; }
.donut-dot  { width: 10px; height: 10px; border-radius: 50%; flex-shrink: 0; }
.donut-key  { font-size: 13px; color: #606266; }
.donut-legend-right { display: flex; align-items: baseline; gap: 6px; }
.donut-count { font-size: 18px; font-weight: 700; }
.donut-pct   { font-size: 12px; color: #909399; }
.donut-avg-row {
  margin-top: 12px;
  padding-top: 10px;
  border-top: 2px dashed #EBEEF5;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.donut-avg-label { font-size: 13px; color: #606266; }
.donut-avg-value { font-size: 24px; font-weight: 700; }

/* 7 维度横向条形图 */
.dim-bars { display: flex; flex-direction: column; gap: 10px; }
.dim-bar-row {
  display: flex;
  align-items: center;
  gap: 10px;
}
.dim-bar-label {
  width: 48px;
  font-size: 12px;
  color: #606266;
  flex-shrink: 0;
  text-align: right;
}
.dim-bar-track {
  flex: 1;
  height: 14px;
  background: #F0F2F5;
  border-radius: 7px;
  overflow: hidden;
  position: relative;
}
.dim-bar-fill {
  height: 100%;
  border-radius: 7px;
  transition: width 0.6s cubic-bezier(0.4,0,0.2,1);
}
.dim-bar-ticks {
  position: absolute;
  inset: 0;
  pointer-events: none;
}
.dim-tick {
  position: absolute;
  top: 0; bottom: 0;
  width: 1px;
  background: rgba(255,255,255,0.5);
}
.dim-bar-score {
  width: 36px;
  font-size: 13px;
  font-weight: 700;
  text-align: right;
  flex-shrink: 0;
}
.dim-x-axis {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 2px;
}
.dim-x-labels {
  flex: 1;
  display: flex;
  justify-content: space-between;
  padding: 0 0 0 0;
}
.dim-x-labels span { font-size: 10px; color: #C0C4CC; }

/* 表格区域 */
.result-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.result-actions { display: flex; gap: 10px; }
.filter-bar { margin-bottom: 16px; padding: 12px; background: #F5F7FA; border-radius: 4px; }

.question-cell, .answer-cell, .sentence-cell { line-height: 1.5; }

.score-excellent  { color: #67C23A; font-weight: 600; }
.score-good       { color: #409EFF; font-weight: 600; }
.score-acceptable { color: #E6A23C; font-weight: 600; }
.score-poor       { color: #F56C6C; font-weight: 600; }

.pagination-wrapper { margin-top: 20px; text-align: right; }

.action-buttons { margin-top: 30px; text-align: center; }
.action-buttons .el-button { margin: 0 10px; }

/* 详情弹窗 */
.detail-content { padding: 10px; }
.detail-text { line-height: 1.8; color: #606266; }
.score-detail-section { margin-top: 24px; }
.score-detail-section h4 { margin: 0 0 16px; font-size: 16px; color: #303133; }
.score-item {
  padding: 12px; background: #F5F7FA; border-radius: 4px;
  margin-bottom: 12px; display: flex; align-items: center; justify-content: space-between;
}
.score-name { font-size: 14px; color: #606266; }
.score-value-large { font-size: 20px; font-weight: bold; margin: 0 8px; }
.score-level { font-size: 12px; color: #909399; }
.avg-score-display, .judgment-display {
  padding: 16px; background: #F0F9FF; border-radius: 4px;
  margin-bottom: 12px; display: flex; align-items: center;
}
.avg-label, .judgment-label { font-size: 16px; color: #606266; margin-right: 12px; }
.avg-score { font-size: 32px; font-weight: bold; }

.chart-title-row {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin-bottom: 10px;
}
.chart-title-text { font-size: 14px; font-weight: 600; color: #303133; }
.chart-subtitle   { font-size: 12px; color: #C0C4CC; }
.line-chart-wrap  { width: 100%; }
.line-chart-wrap canvas { display: block; width: 100% !important; }

/* 响应式 */
@media (max-width: 900px) {
  .eval-charts-row { grid-template-columns: 1fr; }
  .donut-body { flex-direction: column; align-items: flex-start; }
}
</style>
