<template>
  <div class="qa-generation-page">

    <!-- 数据源选择区域 -->
    <el-card class="upload-card" shadow="never">
      <h3 class="section-title">选择数据源文献</h3>
      
      <!-- 搜索框 -->
      <div class="search-box">
        <el-input
          v-model="searchKeyword"
          placeholder="输入文献标题或关键词搜索..."
          prefix-icon="el-icon-search"
          clearable
          @clear="loadPapers"
          @keyup.enter="handleSearch"
        >
          <template #append>
            <el-button @click="handleSearch" :loading="loading">搜索</el-button>
          </template>
        </el-input>
      </div>

      <!-- 文献列表（多选） -->
      <div class="paper-list">
        <div v-if="loading" class="loading-state">
          <i class="el-icon-loading"></i> 加载中...
        </div>

        <el-checkbox-group v-model="selectedPapers" class="paper-checkbox-group" v-else>
          <div v-for="paper in paperList" :key="paper.id" class="paper-item">
            <el-checkbox :label="paper.id" border class="paper-checkbox">
              <div class="paper-info">
                <div class="paper-title">
                  <span class="title-text">{{ paper.title }}</span>
                  <el-tag size="small" type="info">待生成</el-tag>
                </div>
                <div class="paper-meta">
                  <span><i class="el-icon-document"></i> {{ paper.filename || '无文件名' }}</span>
                  <span><i class="el-icon-time"></i> {{ formatDate(paper.createdAt) }}</span>
                </div>
              </div>
            </el-checkbox>
          </div>
        </el-checkbox-group>

        <el-empty v-if="!loading && paperList.length === 0" description="暂无文献" :image-size="120" />
      </div>

      <!-- 全选/取消 -->
      <div class="list-actions" v-if="paperList.length > 0">
        <el-button type="text" @click="selectAll">全选</el-button>
        <el-button type="text" @click="clearSelection">取消选择</el-button>
        <span class="selected-count">已选择 <strong>{{ selectedPapers.length }}</strong> 篇文献</span>
      </div>

      <div v-if="selectedPapers.length > 0" class="file-info">
        <el-alert
          :title="`已选择 ${selectedPapers.length} 篇文献，预计 ${estimateWindowCount} 个文本段落`"
          type="success"
          :closable="false"
          show-icon>
        </el-alert>
      </div>
    </el-card>

    <!-- 生成配置 -->
    <el-card class="config-card" shadow="never">
      <h3 class="section-title">生成配置</h3>
      <el-form :model="configForm" label-width="130px">
        <el-form-item label="生成模型">
          <el-select v-model="configForm.model" style="width: 320px;">
            <el-option label="Qwen3-VL-32B-Thinking" value="Qwen/Qwen3-VL-32B-Thinking"></el-option>
            <el-option label="Qwen3-VL-235B-A22B-Thinking" value="Qwen/Qwen3-VL-235B-A22B-Thinking"></el-option>
            <el-option label="DeepSeek-R1" value="deepseek-ai/DeepSeek-R1"></el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="滑动窗口大小">
          <el-input-number v-model="configForm.window_size" :min="2" :max="16" :step="1"></el-input-number>
          <span class="form-tip">每次合并的句子数量（推荐：6）</span>
        </el-form-item>

        <el-form-item label="窗口重叠大小">
          <el-input-number v-model="configForm.overlap" :min="0" :max="8" :step="1"></el-input-number>
          <span class="form-tip">相邻窗口重叠的句子数（推荐：3）</span>
        </el-form-item>

        <el-form-item label="问答对生成上限">
          <template v-if="configForm.use_max_count">
            <el-input-number
              v-model="configForm.max_count_multiplier"
              :min="1" :max="20" :step="1"
              style="margin-left: 16px; width: 120px;"
            ></el-input-number>
            <span class="form-tip">
              × 段落数 = 上限约 <strong>{{ estimateWindowCount * configForm.max_count_multiplier }}</strong> 条
              &nbsp;·&nbsp;达到上限后剩余段落将跳过，适合快速采样
            </span>
          </template>
          <span v-else class="form-tip warn-tip">⚠ 无上限：所有段落全部处理，数量由模型决定</span>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 生成进度（生成中或完成后显示） -->
    <el-card class="progress-card" shadow="never" v-if="taskId">
      <h3 class="section-title">生成进度</h3>

      <!-- 指标统计行 -->
      <div class="progress-stats">
        <div class="stat-item">
          <div class="stat-label">已生成问答对</div>
          <div class="stat-value primary">{{ taskStatus.generated_count || 0 }}</div>
        </div>
        <div class="stat-item">
          <div class="stat-label">已处理段落</div>
          <div class="stat-value">
            {{ taskStatus.done || 0 }}<span class="stat-total"> / {{ taskStatus.total || '–' }}</span>
          </div>
        </div>
        <div class="stat-item">
          <div class="stat-label">处理失败</div>
          <div class="stat-value warning">{{ taskStatus.failed || 0 }}</div>
        </div>
        <div class="stat-item">
          <div class="stat-label">平均每段生成</div>
          <div class="stat-value info">{{ avgPerWindow }}</div>
        </div>
        <div class="stat-item">
          <div class="stat-label">预计剩余时间</div>
          <div class="stat-value">{{ formatTime(taskStatus.estimated_remaining_seconds) }}</div>
        </div>
      </div>

      <!-- 进度条 -->
      <el-progress
        :percentage="progressPercent"
        :status="progressStatus"
        :stroke-width="14"
        style="margin-bottom: 24px;"
      ></el-progress>

      <!-- 折线图：问答对生成趋势 -->
      <div class="chart-section" v-if="linePoints.length > 1">
        <div class="chart-title-row">
          <span class="chart-title-text">问答对生成趋势</span>
          <span class="chart-subtitle">每隔 2 秒记录一次累计生成量</span>
        </div>
        <div class="line-chart-wrap">
          <canvas ref="lineCanvas"></canvas>
        </div>
      </div>

      <p v-if="taskStatus.status === 'error'" class="error-text">
        <i class="el-icon-warning"></i> 生成出错：{{ taskStatus.error }}
      </p>
      <p v-if="taskStatus.status === 'done'" class="success-text">
        <i class="el-icon-circle-check"></i>
        生成完成！共处理 {{ taskStatus.total }} 个段落，生成
        <strong>{{ taskStatus.generated_count }}</strong> 个问答对。
        <span v-if="limitTriggered" class="limit-tip">
          <i class="el-icon-info"></i>
          已触发生成上限，部分段落被跳过以控制总量。
        </span>
      </p>
    </el-card>

    <!-- 操作按钮 -->
    <div class="action-buttons">
      <el-button
        type="primary"
        size="large"
        :loading="isGenerating"
        :disabled="selectedPapers.length === 0"
        @click="handleGenerate"
      >
        {{ isGenerating ? '生成中...' : '开始生成' }}
      </el-button>

      <el-button
        size="large"
        :disabled="!taskId || taskStatus.status !== 'done'"
        @click="handleDownload"
      >
        下载问答对数据
      </el-button>

      <el-button
        size="large"
        type="success"
        :disabled="!taskId || taskStatus.status !== 'done'"
        @click="handleNext"
      >
        下一步：质量评估 →
      </el-button>
    </div>
  </div>
</template>

<script>
import { searchPapers, getAllPapers, getPaperById } from '@/api/literature'

const API_BASE = 'http://localhost:5000'

export default {
  name: 'QAGeneration',
  data() {
    return {
      // 文献选择
      searchKeyword: '',
      selectedPapers: [],
      paperList: [],
      loading: false,
      
      // 配置
      configForm: {
        model: 'Qwen/Qwen3-VL-32B-Thinking',
        window_size: 6,
        overlap: 3,
        use_max_count: true,
        max_count_multiplier: 3
      },
      
      // 任务状态
      taskId: null,
      taskStatus: {},
      isGenerating: false,
      pollTimer: null,
      linePoints: [],
      limitTriggered: false,
      
      // 文献内容
      selectedPapersContent: ''
    }
  },

  computed: {
    progressPercent: function() {
      var done = this.taskStatus.done || 0
      var total = this.taskStatus.total
      if (!total) return 0
      return Math.min(100, Math.floor((done / total) * 100))
    },
     progressStatus: function() {
    if (this.taskStatus.status === 'done') {
      return 'success'
    } else if (this.taskStatus.status === 'error') {
      return 'exception'
    } else {
      return ''
    }
  },
    avgPerWindow: function() {
      var done = this.taskStatus.done || 0
      var gen = this.taskStatus.generated_count || 0
      if (!done) return '–'
      return (gen / done).toFixed(1)
    },
    estimateWindowCount: function() {
      var self = this
      var totalSentences = 0
      this.selectedPapers.forEach(function(id) {
        var paper = self.paperList.find(function(p) { return p.id === id })
        if (paper && paper.sentenceCount) {
          totalSentences += paper.sentenceCount
        }
      })
      var n = this.configForm.window_size
      var m = this.configForm.overlap
      var step = n - m
      if (step <= 0 || totalSentences < n) return totalSentences
      return Math.floor((totalSentences - n) / step) + 1
    }
  },

  mounted: function() {
    this.loadPapers()
  },

  beforeDestroy: function() {
    if (this.pollTimer) clearInterval(this.pollTimer)
  },

  methods: {
    // 加载所有文献
    loadPapers: async function() {
  var self = this
  this.loading = true
  try {
    var res = await getAllPapers()
    console.log('后端返回数据:', res.data)
    
    var dataList = res.data || []
    this.paperList = dataList.map(function(item) {
      // 尝试多种字段名
      var fullText = item.full_text || item.fullText || item.fulltext || ''
      console.log('文献ID:', item.id, 'fullText长度:', fullText.length)
      
      var sentenceCount = 0
      if (fullText && fullText.length > 0) {
        sentenceCount = fullText.split(/[.!?]+/).filter(function(s) { return s.trim().length > 0 }).length
      }
      
      return {
        id: item.id,
        title: item.title,
        filename: item.filename,
        createdAt: item.created_at || item.createdAt,
        sentenceCount: sentenceCount,
        fullText: fullText
      }
    })
    
    console.log('paperList:', this.paperList)
  } catch (error) {
    console.error('加载文献失败:', error)
    this.$message.error('加载文献失败')
  } finally {
    this.loading = false
  }
},

    // 搜索文献
    handleSearch: async function() {
      var self = this
      if (!this.searchKeyword.trim()) {
        this.$message.warning('请输入搜索关键词')
        return
      }
      this.loading = true
      try {
        var res = await searchPapers({ keyword: this.searchKeyword })
        var dataList = res.data || []
        this.paperList = dataList.map(function(item) {
          return {
            id: item.id,
            title: item.title,
            filename: item.filename,
            createdAt: item.createdAt,
            sentenceCount: item.sentenceCount || 0
          }
        })
        if (this.paperList.length === 0) {
          this.$message.info('没有找到相关文献')
        }
      } catch (error) {
        console.error('搜索失败:', error)
        this.$message.error('搜索失败')
      } finally {
        this.loading = false
      }
    },

    // 全选
    selectAll: function() {
      var self = this
      this.selectedPapers = this.paperList.map(function(p) { return p.id })
    },

    // 取消选择
    clearSelection: function() {
      this.selectedPapers = []
    },

    formatDate: function(dateStr) {
      if (!dateStr) return '未知'
      var date = new Date(dateStr)
      return date.getFullYear() + '-' + String(date.getMonth() + 1).padStart(2, '0') + '-' + String(date.getDate()).padStart(2, '0')
    },

    // 获取选中文献的完整内容
    getSelectedPapersContent: async function() {
  var self = this
  var fullText = ''
  for (var i = 0; i < this.selectedPapers.length; i++) {
    var id = this.selectedPapers[i]
    try {
      var res = await getPaperById(id)
      var paper = res.data
      console.log('获取文献详情:', paper)
      
      // 尝试多种字段名
      var content = paper.full_text || paper.fullText || paper.fulltext || paper.content || paper.text || ''
      console.log('文献ID:', id, '内容长度:', content.length)
      
      if (content) {
        fullText = fullText + content + '\n'
      }
    } catch (error) {
      console.error('获取文献 ' + id + ' 内容失败:', error)
    }
  }
  console.log('总内容长度:', fullText.length)
  return fullText
},

    handleGenerate: async function() {
      var self = this
      if (this.selectedPapers.length === 0) {
        this.$message.warning('请至少选择一篇文献')
        return
      }
      
      this.isGenerating = true
      this.taskStatus = {}
      this.linePoints = []
      this.limitTriggered = false
      
      try {
        var content = await this.getSelectedPapersContent()
        if (!content.trim()) {
          this.$message.error('无法获取文献内容，请检查文献是否包含文本')
          this.isGenerating = false
          return
        }
        
        var lines = content.split('\n').filter(function(l) { return l.trim() })
        
        var maxCount = this.configForm.use_max_count
          ? this.estimateWindowCount * this.configForm.max_count_multiplier
          : 999999
        
        var res = await fetch(API_BASE + '/api/qa/generate/start', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            lines: lines,
            model: this.configForm.model,
            window_size: this.configForm.window_size,
            overlap: this.configForm.overlap,
            max_count: maxCount,
            paper_ids: this.selectedPapers
          })
        })
        var data = await res.json()
        if (res.ok) {
          this.taskId = data.task_id
          this.$message.success('生成任务已启动')
          this.startPolling()
        } else {
          this.$message.error(data.error || '启动失败')
          this.isGenerating = false
        }
      } catch (e) {
        console.error('生成失败:', e)
        this.$message.error('连接后端失败')
        this.isGenerating = false
      }
    },

    startPolling: function() {
      var self = this
      this.pollTimer = setInterval(async function() {
        try {
          var res = await fetch(API_BASE + '/api/qa/generate/status/' + self.taskId)
          var data = await res.json()
          self.taskStatus = data
          if (data.limit_triggered) self.limitTriggered = true

          self.linePoints.push({
            done: data.done || 0,
            generated: data.generated_count || 0
          })
          self.$nextTick(function() { self.drawLineChart() })

          if (data.status === 'done') {
            self.isGenerating = false
            clearInterval(self.pollTimer)
            var total = data.total || 0
            var lastPt = self.linePoints[self.linePoints.length - 1]
            if (lastPt && lastPt.done < total) {
              self.linePoints.push({ done: total, generated: data.generated_count || 0 })
              self.$nextTick(function() { self.drawLineChart() })
            }
            self.$message.success('生成完成！共生成 ' + data.generated_count + ' 个问答对')
          } else if (data.status === 'error') {
            self.isGenerating = false
            clearInterval(self.pollTimer)
            self.$message.error('生成出错：' + data.error)
          }
        } catch (e) { }
      }, 2000)
    },

    drawLineChart: function() {
      var el = this.$refs.lineCanvas
      if (!el || this.linePoints.length < 2) return

      var dpr = window.devicePixelRatio || 1
      var W = el.parentElement.clientWidth || 640
      var H = 200
      el.style.width = W + 'px'
      el.style.height = H + 'px'
      el.width = W * dpr
      el.height = H * dpr
      var ctx = el.getContext('2d')
      ctx.scale(dpr, dpr)

      var PAD_L = 52, PAD_R = 24, PAD_T = 16, PAD_B = 36
      var cW = W - PAD_L - PAD_R
      var cH = H - PAD_T - PAD_B

      var total = this.taskStatus.total || 1
      var maxGen = 1
      for (var i = 0; i < this.linePoints.length; i++) {
        if (this.linePoints[i].generated > maxGen) maxGen = this.linePoints[i].generated
      }
      var yMax = Math.ceil(maxGen / 10) * 10 || 10

      ctx.clearRect(0, 0, W, H)

      var yTicks = 5
      for (i = 0; i <= yTicks; i++) {
        var y = PAD_T + cH - (i / yTicks) * cH
        var val = Math.round(yMax * i / yTicks)
        ctx.beginPath()
        ctx.moveTo(PAD_L, y)
        ctx.lineTo(PAD_L + cW, y)
        ctx.strokeStyle = i === 0 ? '#C0C4CC' : '#EBEEF5'
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

      var pts = this.linePoints
      var step = Math.max(1, Math.floor((pts.length - 1) / 8))
      for (i = 0; i < pts.length; i += step) {
        var x = PAD_L + (pts[i].done / total) * cW
        ctx.fillStyle = '#909399'
        ctx.font = '11px PingFang SC, Microsoft YaHei, sans-serif'
        ctx.textAlign = 'center'
        ctx.textBaseline = 'top'
        ctx.fillText(pts[i].done, x, PAD_T + cH + 6)
      }

      var grad = ctx.createLinearGradient(0, PAD_T, 0, PAD_T + cH)
      grad.addColorStop(0, 'rgba(64,158,255,0.25)')
      grad.addColorStop(1, 'rgba(64,158,255,0.02)')
      ctx.beginPath()
      for (i = 0; i < pts.length; i++) {
        x = PAD_L + (pts[i].done / total) * cW
        y = PAD_T + cH - (pts[i].generated / yMax) * cH
        i === 0 ? ctx.moveTo(x, y) : ctx.lineTo(x, y)
      }
      var lp = pts[pts.length - 1]
      ctx.lineTo(PAD_L + (lp.done / total) * cW, PAD_T + cH)
      ctx.lineTo(PAD_L, PAD_T + cH)
      ctx.closePath()
      ctx.fillStyle = grad
      ctx.fill()

      ctx.beginPath()
      for (i = 0; i < pts.length; i++) {
        x = PAD_L + (pts[i].done / total) * cW
        y = PAD_T + cH - (pts[i].generated / yMax) * cH
        i === 0 ? ctx.moveTo(x, y) : ctx.lineTo(x, y)
      }
      ctx.strokeStyle = '#409EFF'
      ctx.lineWidth = 2.5
      ctx.lineJoin = 'round'
      ctx.stroke()

      var lx = PAD_L + (lp.done / total) * cW
      var ly = PAD_T + cH - (lp.generated / yMax) * cH
      ctx.beginPath()
      ctx.arc(lx, ly, 5, 0, 2 * Math.PI)
      ctx.fillStyle = '#fff'
      ctx.fill()
      ctx.strokeStyle = '#409EFF'
      ctx.lineWidth = 2.5
      ctx.stroke()

      ctx.fillStyle = '#303133'
      ctx.font = 'bold 12px PingFang SC, Microsoft YaHei, sans-serif'
      ctx.textAlign = lx > W - 90 ? 'right' : 'left'
      ctx.textBaseline = 'bottom'
      ctx.fillText(lp.generated + ' 条', lx + (lx > W - 90 ? -8 : 8), ly - 4)

      ctx.fillStyle = '#C0C4CC'
      ctx.font = '11px PingFang SC, Microsoft YaHei, sans-serif'
      ctx.textAlign = 'center'
      ctx.textBaseline = 'top'
      ctx.fillText('已处理段落数', PAD_L + cW / 2, PAD_T + cH + 22)
      ctx.save()
      ctx.translate(12, PAD_T + cH / 2)
      ctx.rotate(-Math.PI / 2)
      ctx.textAlign = 'center'
      ctx.textBaseline = 'middle'
      ctx.fillText('累计生成量（条）', 0, 0)
      ctx.restore()
    },

    handleDownload: function() {
      window.open(API_BASE + '/api/qa/generate/download', '_blank')
    },

    handleNext: function() {
      this.$router.push('/view/qa/evaluate')
    },

    formatTime: function(seconds) {
      if (!seconds || seconds <= 0) return '–'
      var m = Math.floor(seconds / 60)
      var s = seconds % 60
      return m > 0 ? m + '分' + s + '秒' : s + '秒'
    }
  }
}
</script>

<style scoped>
.qa-generation-page { padding: 20px; }

.page-header { margin-bottom: 20px; }
.page-header h2 { margin: 0 0 8px 0; font-size: 24px; color: #303133; }
.page-desc { margin: 0; color: #909399; font-size: 14px; }

.upload-card, .config-card, .progress-card { margin-bottom: 20px; }

.section-title { margin: 0 0 20px 0; font-size: 18px; color: #303133; }

/* 新增文献选择样式 */
.search-box { margin-bottom: 20px; }

.paper-list {
  max-height: 400px;
  overflow-y: auto;
  margin-bottom: 16px;
}

.paper-checkbox-group {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.paper-item { width: 100%; }

.paper-checkbox {
  width: 100%;
  padding: 16px;
  border-radius: 8px;
  border: 1px solid #e9ecef;
}

.paper-checkbox.is-checked {
  border-color: #409EFF;
  background: #f0f9ff;
}

.paper-info { width: 100%; }

.paper-title {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.title-text { font-size: 14px; font-weight: 500; }

.paper-meta {
  display: flex;
  gap: 24px;
  font-size: 12px;
  color: #909399;
}

.list-actions {
  display: flex;
  align-items: center;
  gap: 16px;
  padding-top: 12px;
  border-top: 1px solid #e9ecef;
}

.selected-count { margin-left: auto; font-size: 13px; color: #606266; }
.selected-count strong { color: #409EFF; }

.loading-state { text-align: center; padding: 40px; color: #909399; }

.file-info { margin-top: 16px; }

.form-tip { margin-left: 12px; font-size: 13px; color: #909399; }

/* 进度统计行 */
.progress-stats {
  display: flex;
  justify-content: space-around;
  margin-bottom: 20px;
  padding: 16px 12px;
  background: linear-gradient(135deg, #F8FAFF 0%, #EEF4FF 100%);
  border-radius: 10px;
  border: 1px solid #E4EBFF;
}
.stat-item { text-align: center; }
.stat-label { font-size: 12px; color: #909399; margin-bottom: 6px; letter-spacing: 0.3px; }
.stat-value { font-size: 26px; font-weight: 700; color: #606266; line-height: 1.1; }
.stat-total { font-size: 13px; color: #C0C4CC; font-weight: 400; }
.stat-value.primary { color: #409EFF; }
.stat-value.warning { color: #E6A23C; }
.stat-value.info    { color: #909399; }

/* 折线图区域 */
.chart-section { margin-top: 16px; }
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

.warn-tip { color: #E6A23C !important; }
.limit-tip {
  display: block;
  margin-top: 8px;
  color: #E6A23C;
  font-size: 13px;
  line-height: 1.6;
}

.error-text   { margin-top: 12px; color: #F56C6C; font-size: 14px; }
.success-text { margin-top: 14px; color: #67C23A; font-size: 14px; line-height: 1.8; }
.success-text strong { color: #409EFF; font-size: 17px; }

.action-buttons { margin-top: 30px; text-align: center; }
.action-buttons .el-button { margin: 0 10px; }
</style>