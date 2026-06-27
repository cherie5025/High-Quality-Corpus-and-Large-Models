//author_credibility.vue
<template>
  <div class="author-credibility" v-loading="loading">
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

    <div style="margin-bottom: 24px;">
      <h2 class="page-title">
        <i class="el-icon-s-custom" style="margin-right: 12px;"></i>
        作者可信度 · 可接受度评估
      </h2>
      <p class="page-subtitle">
        基于论文 <span class="paper-highlight">《{{ paperInfo.title }}》</span>
        <span class="paper-meta">{{ paperInfo.journal }}, {{ paperInfo.year }}</span>
      </p>
    </div>

    <el-collapse class="retro-collapse" style="margin-bottom: 20px;">
      <el-collapse-item>
        <template slot="title">
          <i class="el-icon-info" style="color: #3C848C; margin-right: 8px;"></i>
          <span class="collapse-title">📊 作者可接受度计算方法</span>
        </template>
        <div class="collapse-content">
          <div class="inner-card">
            <h4 class="inner-card-title">📐 四个网络度量指标</h4>
            <el-row :gutter="16">
              <el-col :span="12">
                <p><strong>PageRank</strong>  Rankₐ = (1‑q)/N_A + q·Σ[Rank_i / L(i)]</p>
                <p><strong>Betweenness</strong>  Betₐ = Σ[σₐ(Aᵢ,Aⱼ) / σ(Aᵢ,Aⱼ)]</p>
              </el-col>
              <el-col :span="12">
                <p><strong>CPD</strong>  CPDₐ = Σ(Bet_max - Betₐ) / (N_A - 1)</p>
                <p><strong>Closeness</strong>  Clsₐ = 1 / [ Σ d(Aᵢ,a) / (N_A - 1) ]</p>
              </el-col>
            </el-row>
            <el-divider class="retro-divider"></el-divider>
            <p class="weighted-formula">
              <span class="accent">综合公式 · 熵权法加权：</span>
              Accₐ = {{ weights.pagerank.toFixed(2) }}×Rankₐ + {{ weights.betweenness.toFixed(2) }}×Betₐ +
              {{ weights.cpd.toFixed(2) }}×CPDₐ + {{ weights.closeness.toFixed(2) }}×Clsₐ
            </p>
            <div class="inner-card accent-bg" style="margin-top: 12px;">
              <span class="weight-label">最终权重：</span>
              <span class="weight-value">PageRank {{ (weights.pagerank*100).toFixed(0) }}% · Betweenness {{ (weights.betweenness*100).toFixed(0) }}%</span>
              <span class="weight-value">CPD {{ (weights.cpd*100).toFixed(0) }}% · Closeness {{ (weights.closeness*100).toFixed(0) }}%</span>
            </div>
          </div>
        </div>
      </el-collapse-item>
    </el-collapse>

    <el-row :gutter="20">
      <el-col :xs="24" :lg="14">
        <el-card class="retro-card" shadow="never" style="margin-bottom: 20px; height: 400px;">
          <div slot="header" class="retro-card-header">
            <span><i class="el-icon-share"></i> <span class="card-title">作者合作网络 · 节点大小 = PageRank，颜色 = 机构</span></span>
            <el-tooltip content="连线粗细表示合作强度" placement="top">
              <i class="el-icon-question" style="color: #909399; cursor: help;"></i>
            </el-tooltip>
          </div>
          <div ref="networkChart" style="width: 100%; height: 300px;"></div>
        </el-card>

        <el-card class="retro-card" shadow="never">
          <div slot="header" class="retro-card-header">
            <span><i class="el-icon-document"></i> <span class="card-title">作者贡献度分配</span></span>
          </div>
          <el-row :gutter="20">
            <el-col :span="12">
              <div class="inner-card" style="margin: 0; padding: 0; border: none;">
                <el-table :data="contributionTable" size="small" class="retro-table" style="width: 100%;">
                  <el-table-column prop="rank" label="排名" width="50"></el-table-column>
                  <el-table-column prop="author" label="作者" width="130"></el-table-column>
                  <el-table-column prop="acc" label="Acc" width="70"></el-table-column>
                  <el-table-column prop="g" label="g" width="60"></el-table-column>
                  <el-table-column prop="product" label="Acc×g" width="80"></el-table-column>
                </el-table>
              </div>
            </el-col>
            <el-col :span="12">
              <div class="inner-card accent-bg calculation-result" style="height: 100%;">
                <p class="formula">C_author = Σ (Acc × g)</p>
                <p class="result-number">= {{ cAuthorValue.toFixed(4) }} <span class="result-badge">{{ cAuthorValue.toFixed(3) }}</span></p>
                <p class="note">✅ 基于论文实际作者人数 N={{ paperAuthors.length }} 的真实计算。</p>
              </div>
            </el-col>
          </el-row>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="10">
        <el-card class="retro-card" shadow="never" style="margin-bottom: 20px;">
          <div slot="header" class="retro-card-header">
            <span><i class="el-icon-pie-chart"></i> <span class="card-title">指标权重分配 · 熵权法</span></span>
          </div>
          <div ref="weightChart" style="width: 100%; height: 220px;"></div>
          <div class="weight-legend">
            <div><span class="legend-dot" style="background: #6baed6;"></span> PageRank {{ (weights.pagerank*100).toFixed(0) }}%</div>
            <div><span class="legend-dot" style="background: #9ac9a8;"></span> Betweenness {{ (weights.betweenness*100).toFixed(0) }}%</div>
            <div><span class="legend-dot" style="background: #fdbe85;"></span> CPD {{ (weights.cpd*100).toFixed(0) }}%</div>
            <div><span class="legend-dot" style="background: #f4a2a2;"></span> Closeness {{ (weights.closeness*100).toFixed(0) }}%</div>
          </div>
        </el-card>

        <el-card class="retro-card" shadow="never">
          <div slot="header" class="retro-card-header">
            <span><i class="el-icon-document"></i> <span class="card-title">论文核心发现 · 实验验证</span></span>
          </div>
          <div class="paper-info">
            <div><span class="info-label">通讯作者</span><span class="info-value">{{ paperInfo.corresponding_author }}</span></div>
            <div><span class="info-label">实验验证材料</span><span class="info-value">{{ paperInfo.material }}</span></div>
            <div><span class="info-label">离子电导率 (RT)</span><span class="info-value">{{ paperInfo.ionic_conductivity }}</span></div>
            <div><span class="info-label">活化能</span><span class="info-value">{{ paperInfo.activation_energy }}</span></div>
            <div><span class="info-label">新超离子导体框架</span><span class="info-value">{{ paperInfo.frameworks }}</span></div>
            <div><span class="info-label">高通量筛选成功率</span><span class="info-value">{{ paperInfo.screening_rate }}</span></div>
          </div>
          <el-divider class="retro-divider"></el-divider>
          <div class="paper-footer">
            <span class="footer-label"><i class="el-icon-s-promotion"></i> 合作网络规模：{{ networkNodes.length }}位作者 · {{ uniqueInstitutions }}家机构</span>
            <el-tag size="small" class="retro-tag">ESI高被引论文 · 被引{{ paperInfo.citation }}次</el-tag>
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
  name: 'AuthorCredibility',
  data() {
    return {
      loading: true,
      weights: { pagerank: 0, betweenness: 0, cpd: 0, closeness: 0 },
      contributionTable: [],
      networkNodes: [],
      networkLinks: [],
      paperInfo: {},
      cAuthorValue: 0,
      paperAuthors: []
    }
  },
  computed: {
    uniqueInstitutions() {
      const categories = new Set(this.networkNodes.map(node => node.category))
      return categories.size
    }
  },
  watch: {
    '$route.query.id': {
      immediate: true,
      async handler(newId) {
        if (newId) await this.loadDataFromBackend(newId)
      }
    }
  },
  methods: {
    async loadDataFromBackend(paperId) {
      this.loading = true
      try {
        // 1. 获取论文基本信息及 C_author
        const paperRes = await axios.get(`${API_BASE}/api/id/${paperId}`)
        if (paperRes.data.success) {
          const data = paperRes.data.data
          this.paperInfo = {
            title: data['题目'] || '',
            journal: data['期刊'] || '',
            year: data['发表日期'] ? (typeof data['发表日期'] === 'string' ? data['发表日期'].split('.')[0] : data['发表日期']) : '',
            corresponding_author: data['通讯作者'] || '',
            citation: data['被引数'] || 0,
            material: '暂无',
            ionic_conductivity: '暂无',
            activation_energy: '暂无',
            frameworks: '暂无',
            screening_rate: '暂无'
          }
          this.cAuthorValue = data.C_author || 0
        }

        // 2. 获取作者贡献度表格
        const contribRes = await axios.get(`${API_BASE}/api/author-contribution/${paperId}`)
        if (contribRes.data.success) {
          this.contributionTable = contribRes.data.data
          this.paperAuthors = this.contributionTable.map(item => item.author)
          if (this.cAuthorValue === 0 && this.contributionTable.length) {
            this.cAuthorValue = this.contributionTable.reduce((sum, item) => sum + (item.product || 0), 0)
          }
        }

        // 3. 加载网络数据（从后端API或静态文件）
        await this.loadNetworkData()
      } catch (error) {
        console.error('加载失败:', error)
        this.$message.error('数据加载失败，请检查后端')
      } finally {
        this.loading = false
        this.$nextTick(() => {
          this.initNetworkChart()
          this.initWeightChart()
        })
      }
    },

    /**
     * 加载网络数据（优先从后端API获取，失败时降级使用静态JSON文件）
     */
    async loadNetworkData() {
      // 优先从后端 API 获取
      try {
        const res = await axios.get(`${API_BASE}/api/network-data`)
        if (res.data.success) {
          this.networkNodes = res.data.nodes || []
          this.networkLinks = res.data.links || []
          console.log('✅ 网络数据从后端API加载成功，节点数:', this.networkNodes.length)
        } else {
          throw new Error('后端返回失败')
        }
      } catch (error) {
        console.warn('⚠️ 从后端加载网络数据失败，降级使用静态文件:', error)
        // 降级：从静态 JSON 文件加载
        const baseUrl = window.location.origin + '/static/data/'
        let nodes, links
        try { nodes = (await axios.get(baseUrl + 'network_nodes.json')).data } catch (e) {
          console.warn('加载 network_nodes.json 失败:', e)
        }
        try { links = (await axios.get(baseUrl + 'network_links.json')).data } catch (e) {
          console.warn('加载 network_links.json 失败:', e)
        }
        this.networkNodes = Array.isArray(nodes) ? nodes : []
        this.networkLinks = Array.isArray(links) ? links : []
      }

      // 权重数据仍从静态文件获取（后续可改为后端接口）
      try {
        const baseUrl = window.location.origin + '/static/data/'
        const weightsRes = await axios.get(baseUrl + 'weights.json')
        this.weights = weightsRes.data || { pagerank: 0, betweenness: 0, cpd: 0, closeness: 0 }
        console.log('✅ 权重数据加载成功')
      } catch (e) {
        console.warn('⚠️ 加载 weights.json 失败，使用默认权重:', e)
        this.weights = { pagerank: 0, betweenness: 0, cpd: 0, closeness: 0 }
      }
    },

    initNetworkChart() {
      const chartDom = this.$refs.networkChart
      if (!chartDom || !this.networkNodes.length) {
        console.warn('网络图数据为空，跳过渲染')
        return
      }
      const myChart = echarts.init(chartDom)
      const categories = [
        { name: '核心作者' },
        { name: '合作者' },
        { name: '产业界合作者' }
      ]
      const option = {
        tooltip: { trigger: 'item', formatter: '{b}' },
        legend: {
          data: categories.map(c => c.name),
          orient: 'horizontal',
          left: 'center',
          top: 0,
          itemWidth: 10,
          itemHeight: 10,
          textStyle: { color: '#606266', fontSize: 12 }
        },
        series: [{
          type: 'graph',
          layout: 'force',
          force: { repulsion: 400, edgeLength: 120, gravity: 0.1, friction: 0.2 },
          roam: true,
          draggable: true,
          edgeSymbol: ['none', 'arrow'],
          edgeSymbolSize: [0, 8],
          label: { show: true, position: 'right', fontSize: 11, offset: [5, 0], color: '#303133' },
          lineStyle: { color: '#aaa', curveness: 0.3, width: 2, opacity: 0.6 },
          emphasis: { focus: 'adjacency', lineStyle: { width: 3 } },
          categories: categories,
          data: this.networkNodes,
          links: this.networkLinks
        }]
      }
      myChart.setOption(option)
      window.addEventListener('resize', () => myChart.resize())
    },

    initWeightChart() {
      const chartDom = this.$refs.weightChart
      if (!chartDom) return
      const myChart = echarts.init(chartDom)
      const data = [
        { value: this.weights.pagerank * 100, name: 'PageRank', itemStyle: { color: '#6baed6' } },
        { value: this.weights.betweenness * 100, name: 'Betweenness', itemStyle: { color: '#9ac9a8' } },
        { value: this.weights.cpd * 100, name: 'CPD', itemStyle: { color: '#fdbe85' } },
        { value: this.weights.closeness * 100, name: 'Closeness', itemStyle: { color: '#f4a2a2' } }
      ]
      const option = {
        tooltip: { trigger: 'item', formatter: '{b}: {d}%' },
        legend: { show: false },
        series: [{
          name: '权重',
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
.author-credibility { font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; padding: 20px; }
.author-credibility .el-card { border: 2px solid #3C848C; border-radius: 0; box-shadow: none !important; background-color: #FFFFFF; }
.author-credibility .el-card__header { color: #3C848C; font-weight: 800; text-transform: uppercase; letter-spacing: 1px; padding: 14px 20px; border-bottom: 2px solid #3C848C; }
.retro-card-header { display: flex; align-items: center; justify-content: space-between; width: 100%; }
.retro-card-header i { color: #3C848C; margin-right: 8px; font-size: 18px; }
.card-title { color: #3C848C; font-size: 14px; font-weight: 800; text-transform: uppercase; letter-spacing: 1px; }
.retro-btn { border-radius: 0; border: 2px solid #3C848C; font-weight: 800; text-transform: uppercase; padding: 10px 20px; font-size: 14px; letter-spacing: 1px; transition: none; background-color: #FFFFFF; color: #3C848C; }
.retro-btn.primary { background-color: #3C848C; color: #FFFFFF; border-color: #3C848C; }
.retro-btn.primary:hover { background-color: #2F696F; border-color: #2F696F; }
.retro-btn:not(.primary):hover { background-color: #EEEEEE; }
.retro-collapse { border: 2px solid #3C848C; border-radius: 0; }
.retro-collapse .el-collapse-item__header { padding-left: 20px; font-size: 14px; font-weight: 800; background: white; color: #3C848C; text-transform: uppercase; border-bottom: 2px solid #3C848C; }
.retro-collapse .el-collapse-item__wrap { background: white; border: none; }
.collapse-title { font-weight: 800; color: #3C848C; }
.collapse-content { padding: 8px 16px 16px; background: #f9fafc; }
.inner-card { background: white; padding: 16px; border: 2px solid #3C848C; border-radius: 0; height: 100%; }
.inner-card-title { margin-top: 0; margin-bottom: 12px; font-size: 15px; color: #3C848C; font-weight: 800; text-transform: uppercase; }
.accent-bg { background: #ecf5ff; }
.weighted-formula { font-size: 14px; font-weight: 600; }
.weight-label { font-weight: 600; }
.weight-value { font-size: 15px; margin-top: 8px; display: block; }
.weight-legend { margin-top: 16px; display: flex; flex-direction: column; gap: 8px; background: #f5f7fa; padding: 12px; border: 2px solid #3C848C; font-size: 13px; }
.legend-dot { display: inline-block; width: 10px; height: 10px; margin-right: 6px; border-radius: 2px; }
.paper-info { font-size: 14px; line-height: 2; }
.paper-info > div { display: flex; justify-content: space-between; border-bottom: 1px dashed #ebeef5; padding: 8px 0; }
.info-label { color: #666666; font-weight: 700; text-transform: uppercase; }
.info-value { font-weight: 600; color: #3C848C; }
.paper-footer { display: flex; align-items: center; justify-content: space-between; font-size: 13px; }
.retro-divider { background-color: #3C848C; height: 2px; }
.action-footer { margin-top: 24px; text-align: right; border-top: 2px solid #3C848C; padding-top: 16px; }
.result-number { margin: 10px 0 0; font-size: 20px; font-weight: 700; color: #3C848C; }
.result-badge { background: #3C848C; color: white; padding: 2px 12px; margin-left: 8px; display: inline-block; }
.note { margin: 12px 0 0; color: #909399; font-size: 13px; }
.formula { font-size: 15px; font-weight: 600; margin: 8px 0; }
.page-title { font-size: 20px; font-weight: 800; color: #3C848C; display: flex; align-items: center; text-transform: uppercase; margin: 0 0 8px; }
.page-subtitle { color: #666666; font-size: 14px; margin-top: 8px; font-weight: 700; text-transform: uppercase; }
.paper-highlight { font-weight: 800; color: #3C848C; }
.paper-meta { margin-left: 12px; background: #BDCACC; padding: 4px 12px; font-size: 12px; color: #3C848C; }
.retro-tag { background-color: #BDCACC; color: #3C848C; border: none; font-weight: 700; text-transform: uppercase; border-radius: 0; padding: 4px 12px; font-size: 12px; }
</style>
