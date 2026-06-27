//credibility.vue
<template>
  <div class="overall-credibility" v-loading="loading">
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
          <span class="collapse-title">📐 综合可信度计算方法</span>
        </template>
        <div class="collapse-content">
          <div class="inner-card">
            <h4 class="inner-card-title">📌 融合公式</h4>
            <p class="formula">C = w<sub>c1</sub>·C<sub>time</sub> + w<sub>c2</sub>·C<sub>print</sub> + w<sub>c3</sub>·C<sub>author</sub></p>
            <el-divider class="retro-divider"></el-divider>
            <el-row :gutter="20">
              <el-col :span="8"><p class="rule-title">⏱️ 时效可信度 C<sub>time</sub></p><p class="rule-desc">基于发表日期归一化</p></el-col>
              <el-col :span="8"><p class="rule-title">📄 权威可信度 C<sub>print</sub></p><p class="rule-desc">基于期刊IF/会议等级+文献类型</p></el-col>
              <el-col :span="8"><p class="rule-title">👥 学术可信度 C<sub>author</sub></p><p class="rule-desc">基于作者合作网络与贡献度</p></el-col>
            </el-row>
            <p class="note">* 权重 w 由熵权法根据三个指标在整个数据集上的离散程度确定。</p>
            <div class="inner-card accent-bg" style="margin-top: 12px;">
              <span class="weight-label">本论文权重：</span>
              <span class="weight-value">w<sub>time</sub> = {{ weights.time.toFixed(3) }} · w<sub>print</sub> = {{ weights.print.toFixed(3) }} · w<sub>author</sub> = {{ weights.author.toFixed(3) }}</span>
            </div>
          </div>
        </div>
      </el-collapse-item>
    </el-collapse>

    <el-row :gutter="20">
      <el-col :xs="24" :lg="16">
        <el-card class="retro-card" shadow="never" style="margin-bottom: 20px;">
          <div slot="header" class="retro-card-header">
            <span><i class="el-icon-data-analysis"></i> <span class="card-title">三个维度得分及综合结果</span></span>
          </div>
          <el-row :gutter="20">
            <el-col :span="8">
              <div class="score-card">
                <div class="score-label">⏱️ 时效可信度</div>
                <div class="score-value">{{ scores.time.toFixed(3) }}</div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="score-card">
                <div class="score-label">📄 期刊可信度</div>
                <div class="score-value">{{ scores.print.toFixed(3) }}</div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="score-card">
                <div class="score-label">👥 作者可信度</div>
                <div class="score-value">{{ scores.author.toFixed(3) }}</div>
              </div>
            </el-col>
          </el-row>
          <div class="calc-card">
            <h4 class="calc-title">🧮 综合可信度</h4>
            <p class="calc-result">综合得分 C = {{ overallScore.toFixed(4) }} ≈ <span class="result-badge">{{ (overallScore * 100).toFixed(1) }}%</span></p>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="8">
        <el-card class="retro-card" shadow="never" style="margin-bottom: 20px;">
          <div slot="header" class="retro-card-header">
            <span><i class="el-icon-pie-chart"></i> <span class="card-title">权重分配（熵权法）</span></span>
          </div>
          <div ref="weightChart" style="width: 100%; height: 220px;"></div>
          <div class="weight-legend">
            <div><span class="legend-dot" style="background: #6baed6;"></span> 时效可信度 w<sub>time</sub> = {{ (weights.time*100).toFixed(1) }}%</div>
            <div><span class="legend-dot" style="background: #9ac9a8;"></span> 权威可信度 w<sub>print</sub> = {{ (weights.print*100).toFixed(1) }}%</div>
            <div><span class="legend-dot" style="background: #fdbe85;"></span> 学术可信度 w<sub>author</sub> = {{ (weights.author*100).toFixed(1) }}%</div>
          </div>
        </el-card>

        <el-card class="retro-card" shadow="never">
          <div slot="header" class="retro-card-header">
            <span><i class="el-icon-document"></i> <span class="card-title">论文核心信息</span></span>
          </div>
          <div class="paper-info">
            <div><span class="info-label">通讯作者</span><span class="info-value">{{ paper.corresponding_author }}</span></div>
            <div><span class="info-label">期刊</span><span class="info-value">{{ paper.journal }}</span></div>
            <div><span class="info-label">发表日期</span><span class="info-value">{{ paper.date }}</span></div>
            <div><span class="info-label">被引次数</span><span class="info-value">{{ paper.citation }}</span></div>
            <div><span class="info-label">文献类型</span><el-tag :type="paper.document_type === '综述' ? 'success' : 'primary'" size="small" class="info-tag">{{ paper.document_type }}</el-tag></div>
          </div>
          <el-divider class="retro-divider"></el-divider>
          <div class="paper-footer">
            <span class="footer-label"><i class="el-icon-s-promotion"></i> 综合可信度得分：{{ overallScore.toFixed(3) }}</span>
            <el-tag size="small" :type="overallScore >= 0.7 ? 'success' : (overallScore >= 0.5 ? 'warning' : 'danger')" effect="plain" class="footer-tag">
              {{ overallScore >= 0.7 ? '高可信度' : (overallScore >= 0.5 ? '中可信度' : '低可信度') }}
            </el-tag>
          </div>
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
  name: 'OverallCredibility',
  data() {
    return {
      loading: false,
      paper: {
        title: '',
        corresponding_author: '',
        journal: '',
        date: '',
        citation: 0,
        document_type: ''
      },
      scores: { time: 0, print: 0, author: 0 },
      weights: { time: 0, print: 0, author: 0 }
    }
  },
  computed: {
    currentId() { return this.$route.query.id || null },
    overallScore() {
      return this.weights.time * this.scores.time +
        this.weights.print * this.scores.print +
        this.weights.author * this.scores.author
    }
  },
  watch: {
    currentId: {
      immediate: true,
      async handler(newId) {
        if (newId) await this.loadPaperData(newId)
      }
    }
  },
  mounted() {
    this.$nextTick(() => this.initWeightChart())
  },
  methods: {
    async loadPaperData(id) {
      this.loading = true
      try {
        const res = await axios.get(`${API_BASE}/api/id/${id}`)
        if (res.data.success) {
          const data = res.data.data
          this.paper = {
            title: data['题目'] || '',
            corresponding_author: data['通讯作者'] || '',
            journal: data['期刊'] || '',
            date: data['发表日期'] || '',
            citation: data['被引数'] || 0,
            document_type: data['文献类型'] || ''
          }
          this.scores = {
            time: data.C_time || 0,
            print: data.C_print || 0,
            author: data.C_author || 0
          }
          this.weights = {
            time: data.w_time || 0,
            print: data.w_print || 0,
            author: data.w_author || 0
          }
        } else {
          this.$message.error('获取文献数据失败')
        }
      } catch (error) {
        console.error('加载论文信息失败:', error)
        this.$message.error('加载数据失败，请检查后端是否运行')
      } finally {
        this.loading = false
        this.$nextTick(() => this.initWeightChart())
      }
    },
    initWeightChart() {
      const chartDom = this.$refs.weightChart
      if (!chartDom) return
      const myChart = echarts.init(chartDom)
      const data = [
        { value: this.weights.time * 100, name: '时效可信度', itemStyle: { color: '#6baed6' } },
        { value: this.weights.print * 100, name: '作者可信度', itemStyle: { color: '#9ac9a8' } },
        { value: this.weights.author * 100, name: '学术可信度', itemStyle: { color: '#fdbe85' } }
      ]
      const option = {
        tooltip: { trigger: 'item', formatter: '{b}: {d}%' },
        legend: { show: false },
        series: [{
          type: 'pie',
          radius: ['50%', '70%'],
          avoidLabelOverlap: false,
          itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
          label: { show: true, position: 'outside', formatter: '{b}\n{d}%', color: '#303133' },
          emphasis: { label: { show: true } },
          data: data
        }]
      }
      myChart.setOption(option)
      window.addEventListener('resize', () => myChart.resize())
    }
  }
}
</script>

<style scoped>
.overall-credibility { font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; padding: 20px; }
.overall-credibility .el-card { border: 2px solid #3C848C; border-radius: 0; box-shadow: none !important; background-color: #FFFFFF; }
.overall-credibility .el-card__header { color: #3C848C; font-weight: 800; text-transform: uppercase; letter-spacing: 1px; padding: 14px 20px; border-bottom: 2px solid #3C848C; }
.retro-card-header { display: flex; align-items: center; justify-content: space-between; width: 100%; }
.retro-card-header i { color: #3C848C; margin-right: 8px; font-size: 18px; }
.card-title { color: #3C848C; font-size: 14px; font-weight: 800; text-transform: uppercase; letter-spacing: 1px; }
.retro-btn { border-radius: 0; border: 2px solid #3C848C; font-weight: 800; text-transform: uppercase; padding: 10px 20px; font-size: 14px; letter-spacing: 1px; transition: none; background-color: #FFFFFF; color: #3C848C; }
.retro-btn.primary { background-color: #3C848C; color: #FFFFFF; border-color: #3C848C; }
.retro-btn.primary:hover { background-color: #2F696F; }
.retro-btn:not(.primary):hover { background-color: #EEEEEE; }
.retro-collapse { border: 2px solid #3C848C; border-radius: 0; }
.retro-collapse .el-collapse-item__header { padding-left: 20px; font-size: 14px; font-weight: 800; background: white; color: #3C848C; text-transform: uppercase; border-bottom: 2px solid #3C848C; }
.retro-collapse .el-collapse-item__wrap { background: white; border: none; }
.collapse-title { font-weight: 800; color: #3C848C; }
.collapse-content { padding: 8px 16px 16px; background: #f9fafc; }
.inner-card { background: white; padding: 16px; border: 2px solid #3C848C; border-radius: 0; height: 100%; }
.inner-card-title { margin-top: 0; margin-bottom: 12px; font-size: 15px; color: #3C848C; font-weight: 800; text-transform: uppercase; }
.accent-bg { background: #ecf5ff; }
.formula { font-size: 16px; font-family: 'Times New Roman', serif; text-align: center; margin: 12px 0; }
.rule-title { font-weight: 600; margin-bottom: 8px; }
.rule-desc { margin: 8px 0 0; font-size: 14px; }
.note { margin: 12px 0 0; color: #909399; font-size: 13px; }
.weight-label { font-weight: 600; }
.weight-value { font-size: 15px; margin-top: 8px; display: block; }
.score-card { background: white; padding: 16px; border: 2px solid #3C848C; border-radius: 0; text-align: center; }
.score-label { font-size: 14px; color: #606266; margin-bottom: 8px; font-weight: 700; text-transform: uppercase; }
.score-value { font-size: 24px; font-weight: 700; color: #3C848C; }
.calc-card { margin-top: 20px; background: #ecf5ff; padding: 16px; border: 2px solid #3C848C; border-radius: 0; }
.calc-title { margin-top: 0; margin-bottom: 12px; font-size: 15px; color: #3C848C; font-weight: 800; text-transform: uppercase; }
.calc-result { margin: 10px 0 0; font-size: 20px; font-weight: 700; color: #3C848C; }
.result-badge { background: #3C848C; color: white; padding: 2px 12px; margin-left: 8px; display: inline-block; }
.weight-legend { margin-top: 16px; display: flex; flex-direction: column; gap: 8px; background: #f5f7fa; padding: 12px; border: 2px solid #3C848C; font-size: 13px; }
.legend-dot { display: inline-block; width: 10px; height: 10px; margin-right: 6px; border-radius: 2px; }
.paper-info { font-size: 14px; line-height: 2; }
.paper-info > div { display: flex; justify-content: space-between; border-bottom: 1px dashed #ebeef5; padding: 8px 0; }
.info-label { color: #666666; font-weight: 700; text-transform: uppercase; }
.info-value { font-weight: 600; color: #3C848C; }
.info-tag { border-radius: 0; border: none; font-weight: 700; }
.paper-footer { display: flex; align-items: center; justify-content: space-between; font-size: 13px; }
.footer-tag { border-radius: 0; border: none; font-weight: 700; }
.retro-divider { background-color: #3C848C; height: 2px; }
.action-footer { margin-top: 24px; text-align: right; border-top: 2px solid #3C848C; padding-top: 16px; }
</style>
