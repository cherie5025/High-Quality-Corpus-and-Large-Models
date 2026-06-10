<template>
  <div class="author-credibility" v-loading="loading">
    <!-- 顶部导航按钮组 -->
    <div style="margin-bottom: 24px; display: flex; gap: 12px; flex-wrap: wrap;">
      <el-button
        class="retro-btn"
        :class="{ primary: $route.path === '/view/literature/credibility' }"
        @click="$router.push({ path: '/view/literature/credibility', query: { id: $route.query.id } })"
      >
        综合可信度
      </el-button>
      <el-button
        class="retro-btn"
        :class="{ primary: $route.path === '/view/literature/author_credibility' }"
        @click="$router.push({ path: '/view/literature/author_credibility', query: { id: $route.query.id } })"
      >
        作者可信度
      </el-button>
      <el-button
        class="retro-btn"
        :class="{ primary: $route.path === '/view/literature/time_credibility' }"
        @click="$router.push({ path: '/view/literature/time_credibility', query: { id: $route.query.id } })"
      >
        时效可信度
      </el-button>
      <el-button
        class="retro-btn"
        :class="{ primary: $route.path === '/view/literature/print_credibility' }"
        @click="$router.push({ path: '/view/literature/print_credibility', query: { id: $route.query.id } })"
      >
        期刊可信度
      </el-button>
    </div>

    <!-- 页面标题 -->
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

    <!-- 折叠面板 -->
    <el-collapse class="retro-collapse" style="margin-bottom: 20px;">
      <el-collapse-item>
        <template slot="title">
          <i class="el-icon-info" style="color: #3C848C; margin-right: 8px;"></i>
          <span class="collapse-title">作者可接受度计算方法</span>
        </template>
        <div class="collapse-content">
          <el-row :gutter="20" style="margin-bottom: 16px;">
            <el-col :span="24">
              <div class="inner-card">
                <h4 class="inner-card-title">四个网络度量指标</h4>
                <el-row :gutter="16">
                  <el-col :span="12">
                    <p><strong>PageRank</strong> Rank = (1-q)/N + q·Σ[Rank_i / L(i)]</p>
                    <p><strong>Betweenness</strong> Bet = Σ[σₐ(Aᵢ,Aⱼ) / σ(Aᵢ,Aⱼ)]</p>
                  </el-col>
                  <el-col :span="12">
                    <p><strong>CPD</strong> CPD = Σ(Bet_max - Bet) / (N - 1)</p>
                    <p><strong>Closeness</strong> Cls = 1 / [ Σ d(Aᵢ,a) / (N - 1) ]</p>
                  </el-col>
                </el-row>
                <el-divider class="retro-divider"></el-divider>
                <p class="weighted-formula">
                  <span class="accent">综合公式 · 熵权法加权：</span>
                  Acc = {{ weights.pagerank.toFixed(2) }}×Rank + {{ weights.betweenness.toFixed(2) }}×Bet + {{ weights.cpd.toFixed(2) }}×CPD + {{ weights.closeness.toFixed(2) }}×Cls
                </p>
              </div>
            </el-col>
          </el-row>

          <el-row :gutter="20">
            <el-col :span="12">
              <div class="inner-card">
                <h4 class="inner-card-title">熵权法确定权重</h4>
                <ol class="step-list">
                  <li>归一化：x' = (x - min)/(max - min)</li>
                  <li>比重：p = x' / Σx'</li>
                  <li>熵值：e = -1/ln(10)·Σ p·ln(p)</li>
                  <li>权重：w = (1-e) / Σ(1-e)</li>
                </ol>
              </div>
            </el-col>
            <el-col :span="12">
              <div class="inner-card accent-bg">
                <span class="weight-label">最终权重：</span>
                <span class="weight-value">PageRank {{ (weights.pagerank*100).toFixed(0) }}% · Betweenness {{ (weights.betweenness*100).toFixed(0) }}%</span>
                <span class="weight-value">CPD {{ (weights.cpd*100).toFixed(0) }}% · Closeness {{ (weights.closeness*100).toFixed(0) }}%</span>
              </div>
            </el-col>
          </el-row>

          <div class="example-section">
            <h4 class="inner-card-title">作者可接受度计算示例 · 通讯作者 {{ cederName }}</h4>
            <el-row :gutter="20">
              <el-col :span="12">
                <div class="inner-card">
                  <p><span>PageRank:</span><span class="value">{{ cederMetrics.pagerank.toFixed(3) }}</span></p>
                  <p><span>Betweenness:</span><span class="value">{{ cederMetrics.betweenness.toFixed(3) }}</span></p>
                  <p><span>CPD:</span><span class="value">{{ cederMetrics.cpd.toFixed(3) }}</span></p>
                  <p><span>Closeness:</span><span class="value">{{ cederMetrics.closeness.toFixed(3) }}</span></p>
                  <el-divider class="retro-divider"></el-divider>
                  <p><span>权重:</span><span>{{ weights.pagerank.toFixed(2) }}·{{ weights.betweenness.toFixed(2) }}·{{ weights.cpd.toFixed(2) }}·{{ weights.closeness.toFixed(2) }}</span></p>
                </div>
              </el-col>
              <el-col :span="12">
                <div class="inner-card accent-bg calculation-result">
                  <p>Acc = {{ weights.pagerank.toFixed(2) }}×{{ cederMetrics.pagerank.toFixed(3) }} + ...</p>
                  <p>= {{ (weights.pagerank * cederMetrics.pagerank).toFixed(5) }} + ...</p>
                  <p class="result-number">= {{ cederMetrics.acc.toFixed(3) }} <span class="result-badge">{{ cederMetrics.acc.toFixed(3) }}</span></p>
                </div>
              </el-col>
            </el-row>
          </div>
        </div>
      </el-collapse-item>

      <el-collapse-item>
        <template slot="title">
          <i class="el-icon-document" style="color: #3C848C; margin-right: 8px;"></i>
          <span class="collapse-title">文献学术可信度 · 真实计算 (N={{ paperAuthors.length }})</span>
        </template>
        <div class="collapse-content">
          <el-row :gutter="20" style="margin-bottom: 16px;">
            <el-col :span="24">
              <div class="inner-card">
                <h4 class="inner-card-title">文献学术可信度定义</h4>
                <p class="formula"><strong>C_author(p)</strong> = Σ [ Acc × g(r, N) ]</p>
                <p class="rule-desc">p：文献 · A：作者集合 · Acc：作者可接受度 · g：作者贡献度</p>
              </div>
            </el-col>
          </el-row>

          <el-row :gutter="20">
            <el-col :span="12">
              <div class="inner-card">
                <h4 class="inner-card-title">作者贡献度函数</h4>
                <p class="formula">g(r, N) = 1 / [ r × H(N) ] , H(N) = Σ_{n=1}^N 1/n</p>
                <p class="rule-desc">r：作者排名（第一作者=1）<br>N：作者总人数（本论文 N={{ paperAuthors.length }}）<br>调和数：H(N) = 1 + 1/2 + ... + 1/N</p>
                <el-tag size="small" class="retro-tag" style="margin-top: 12px;">归一化条件：Σ g = 1</el-tag>
              </div>
            </el-col>
            <el-col :span="12">
              <div class="inner-card accent-bg">
                <h4 class="inner-card-title">本论文调和数 H({{ paperAuthors.length }})</h4>
                <p>H({{ paperAuthors.length }}) = 1 + 1/2 + 1/3 + ... + 1/{{ paperAuthors.length }}</p>
                <p class="harmonic-value">= {{ harmonicSum.toFixed(5) }}</p>
                <p>贡献度总和 Σ g = 1.00000</p>
              </div>
            </el-col>
          </el-row>

          <div class="example-section">
            <h4 class="inner-card-title">文献学术可信度真实计算 · 本论文全部作者</h4>
            <el-row :gutter="20">
              <el-col :span="12">
                <div class="inner-card">
                  <p class="table-title">作者贡献度分配 (N={{ paperAuthors.length }}, H={{ harmonicSum.toFixed(5) }})</p>
                  <el-table :data="contributionTable" size="small" class="retro-table" style="width: 100%;">
                    <el-table-column prop="rank" label="排名" width="50"></el-table-column>
                    <el-table-column prop="author" label="作者" width="110"></el-table-column>
                    <el-table-column prop="acc" label="Acc" width="60"></el-table-column>
                    <el-table-column prop="g" label="g" width="60"></el-table-column>
                    <el-table-column prop="product" label="Acc×g" width="80"></el-table-column>
                  </el-table>
                </div>
              </el-col>
              <el-col :span="12">
                <div class="inner-card accent-bg calculation-result">
                  <p class="formula">C_author = Σ (Acc × g)</p>
                  <p class="calc-line">= {{ contributionTable.map(function(item) { return item.acc + '×' + item.g }).join(' + ') }}</p>
                  <p class="calc-line">= {{ contributionTable.map(function(item) { return (item.product).toFixed(4) }).join(' + ') }}</p>
                  <p class="result-number">= {{ cAuthorValue.toFixed(4) }} <span class="result-badge">{{ cAuthorValue.toFixed(3) }}</span></p>
                  <p class="note">基于论文实际作者人数 N={{ paperAuthors.length }} 的真实计算，贡献度使用调和数 H({{ paperAuthors.length }}) 归一化。</p>
                </div>
              </el-col>
            </el-row>
          </div>
        </div>
      </el-collapse-item>
    </el-collapse>

    <!-- 合作网络图 + 作者排名表格 -->
    <el-row :gutter="20">
      <el-col :xs="24" :lg="14">
        <el-card class="retro-card" shadow="never" style="margin-bottom: 20px;">
          <div slot="header" class="retro-card-header">
            <span><i class="el-icon-share"></i> <span class="card-title">作者合作网络 · 节点大小 = PageRank，颜色 = 机构</span></span>
            <el-tooltip content="连线粗细表示合作强度" placement="top">
              <i class="el-icon-question" style="color: #909399; cursor: help;"></i>
            </el-tooltip>
          </div>
          <div ref="networkChart" style="width: 100%; height: 350px;"></div>
        </el-card>

        <el-card class="retro-card" shadow="never">
          <div slot="header" class="retro-card-header">
            <span><i class="el-icon-trophy"></i> <span class="card-title">作者可接受度排名 · 熵权法综合得分</span></span>
          </div>
          <el-table :data="authorRanking" stripe size="small" class="retro-table" style="width: 100%;">
            <el-table-column prop="rank" label="排名" width="70">
              <template slot-scope="scope">
                <el-tag :type="scope.row.rank === 1 ? 'danger' : (scope.row.rank === 2 ? 'warning' : 'info')" size="small" effect="dark" class="rank-tag">
                  {{ scope.row.rank }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="name" label="作者" width="150">
              <template slot-scope="scope">
                <span class="author-name">{{ scope.row.name }}</span>
                <span v-if="scope.row.role" class="author-role">({{ scope.row.role }})</span>
              </template>
            </el-table-column>
            <el-table-column prop="acceptability" label="可接受度" width="90">
              <template slot-scope="scope">
                <span class="score">{{ formatNumber(scope.row.acceptability) }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="interpretation" label="学术地位解读" min-width="160"></el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <!-- 右侧：权重饼图 + 论文核心信息 -->
      <el-col :xs="24" :lg="10">
        <el-card class="retro-card" shadow="never" style="margin-bottom: 20px;">
          <div slot="header" class="retro-card-header">
            <span><i class="el-icon-pie-chart"></i> <span class="card-title">指标权重分配 · 熵权法</span></span>
          </div>
          <div ref="weightChart" style="width: 100%; height: 250px;"></div>
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

    <!-- 返回按钮 -->
    <div class="action-footer">
      <el-button type="text" icon="el-icon-back" @click="$router.push('/view/literature')" class="retro-btn">
        返回文献上传
      </el-button>
    </div>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import axios from 'axios'

// 从 assets/data 加载 JSON 文件
import weightsData from '@/assets/data/weights.json'
import authorsMetricsData from '@/assets/data/authors_metrics.json'
import authorsRankingData from '@/assets/data/authors_ranking.json'
import contributionTableData from '@/assets/data/contribution_table.json'
import networkNodesData from '@/assets/data/network_nodes.json'
import networkLinksData from '@/assets/data/network_links.json'
import paperInfoData from '@/assets/data/paper_info.json'
import cAuthorValueData from '@/assets/data/c_author_value.json'

export default {
  name: 'AuthorCredibility',
  data: function() {
    return {
      loading: true,
      weights: { pagerank: 0.35, betweenness: 0.25, cpd: 0.20, closeness: 0.20 },
      authorsMetrics: {},
      authorRanking: [],
      contributionTable: [],
      networkNodes: [],
      networkLinks: [],
      paperInfo: {},
      cAuthorValue: 0,
      harmonicSum: 0,
      paperAuthors: []
    }
  },
  computed: {
    cederName: function() {
      return this.paperInfo.corresponding_author || 'Ceder'
    },
    cederMetrics: function() {
      var authorName = this.cederName
      var defaultVal = { pagerank: 0.85, betweenness: 0.72, cpd: 0.68, closeness: 0.91, acc: 0.82 }
      var metrics = this.authorsMetrics[authorName]
      return metrics || defaultVal
    },
    uniqueInstitutions: function() {
      var categories = new Set()
      for (var i = 0; i < this.networkNodes.length; i++) {
        if (this.networkNodes[i].category !== undefined) {
          categories.add(this.networkNodes[i].category)
        }
      }
      return categories.size || 1
    }
  },
  watch: {
    '$route.query.id': {
      immediate: true,
      handler: function(newId) {
        var self = this
        if (newId) {
          console.log('作者可信度接收到文献ID:', newId)
          self.loadDataFromBackend(newId)
        } else {
          self.loadStaticData()
        }
      }
    }
  },
  mounted: function() {},
  methods: {
    formatNumber: function(value) {
      var num = parseFloat(value)
      return isNaN(num) ? '0.000' : num.toFixed(3)
    },
    loadDataFromBackend: function(paperId) {
      var self = this
      self.loading = true
      axios.get('http://localhost:3007/api/id/' + paperId).then(function(paperRes) {
        if (paperRes.data && paperRes.data.success) {
          var paperData = paperRes.data.data
          self.paperInfo = {
            title: paperData['题目'] || '',
            journal: paperData['期刊'] || '',
            year: paperData['发表日期'] ? (typeof paperData['发表日期'] === 'string' ? paperData['发表日期'].split('-')[0] : paperData['发表日期']) : '',
            corresponding_author: paperData['通讯作者'] || '',
            citation: paperData['被引数'] || 0,
            material: paperData['实验材料'] || '暂无',
            ionic_conductivity: paperData['离子电导率'] || '暂无',
            activation_energy: paperData['活化能'] || '暂无',
            frameworks: paperData['框架'] || '暂无',
            screening_rate: paperData['筛选率'] || '暂无'
          }
        }
        self.loadStaticData()
      }).catch(function(error) {
        console.error('从后端加载失败:', error)
        self.loadStaticData()
      })
    },
    
    loadStaticData: function() {
      var self = this
      try {
        // 处理 weights - 确保是数字
        if (weightsData) {
          self.weights = {
            pagerank: Number(weightsData.pagerank) || 0.35,
            betweenness: Number(weightsData.betweenness) || 0.25,
            cpd: Number(weightsData.cpd) || 0.20,
            closeness: Number(weightsData.closeness) || 0.20
          }
        }
        
        // 处理 authorsMetrics
        if (authorsMetricsData) {
          var metrics = {}
          for (var key in authorsMetricsData) {
            if (authorsMetricsData.hasOwnProperty(key)) {
              var m = authorsMetricsData[key]
              metrics[key] = {
                pagerank: Number(m.pagerank) || 0,
                betweenness: Number(m.betweenness) || 0,
                cpd: Number(m.cpd) || 0,
                closeness: Number(m.closeness) || 0,
                acc: Number(m.acc) || 0
              }
            }
          }
          self.authorsMetrics = metrics
        }
        
        // 处理 authorRanking
        if (Array.isArray(authorsRankingData)) {
          var rankingList = []
          for (var i = 0; i < authorsRankingData.length; i++) {
            var item = authorsRankingData[i]
            rankingList.push({
              rank: item.rank,
              name: item.name,
              role: item.role || '',
              interpretation: item.interpretation || '',
              acceptability: Number(item.acc) || 0
            })
          }
          self.authorRanking = rankingList
        }
        
        // 处理 contributionTable
        if (Array.isArray(contributionTableData)) {
          var contribList = []
          for (var j = 0; j < contributionTableData.length; j++) {
            var c = contributionTableData[j]
            contribList.push({
              rank: c.rank,
              author: c.author,
              acc: Number(c.acc) || 0,
              g: Number(c.g) || 0,
              product: Number(c.product) || 0
            })
          }
          self.contributionTable = contribList
        }
        
        // 处理 networkNodes
        if (Array.isArray(networkNodesData)) {
          var nodesList = []
          for (var k = 0; k < networkNodesData.length; k++) {
            var n = networkNodesData[k]
            nodesList.push({
              name: n.name,
              category: n.category,
              symbolSize: Number(n.symbolSize) || 20,
              value: Number(n.value) || 0
            })
          }
          self.networkNodes = nodesList
        }
        
        // 处理 networkLinks
        if (Array.isArray(networkLinksData)) {
          self.networkLinks = networkLinksData
        }
        
        // 处理 paperInfo
        if (paperInfoData && Object.keys(paperInfoData).length > 0 && !self.paperInfo.title) {
          for (var key2 in paperInfoData) {
            if (paperInfoData.hasOwnProperty(key2) && !self.paperInfo[key2]) {
              self.paperInfo[key2] = paperInfoData[key2]
            }
          }
        }
        
        // 处理 cAuthorValue
        if (cAuthorValueData && cAuthorValueData.value !== undefined) {
          self.cAuthorValue = Number(cAuthorValueData.value) || 0.68
        }
        
        // 计算 paperAuthors 和 harmonicSum
        self.paperAuthors = []
        for (var m = 0; m < self.contributionTable.length; m++) {
          self.paperAuthors.push(self.contributionTable[m].author)
        }
        
        if (self.paperAuthors.length > 0) {
          var sum = 0
          for (var n = 0; n < self.paperAuthors.length; n++) {
            sum = sum + 1 / (n + 1)
          }
          self.harmonicSum = sum
        } else {
          self.harmonicSum = 1
        }
        
        console.log('数据加载完成', {
          weights: self.weights,
          authorsCount: Object.keys(self.authorsMetrics).length,
          rankingCount: self.authorRanking.length,
          contributionCount: self.contributionTable.length,
          networkNodes: self.networkNodes.length
        })
        
      } catch (error) {
        console.error('数据加载失败:', error)
      } finally {
        self.loading = false
        self.$nextTick(function() {
          setTimeout(function() {
            self.initNetworkChart()
            self.initWeightChart()
          }, 200)
        })
      }
    },
    
    initNetworkChart: function() {
      var chartDom = this.$refs.networkChart
      if (!chartDom) {
        console.log('networkChart DOM 不存在')
        return
      }
      if (!this.networkNodes.length) {
        console.log('网络图数据为空')
        return
      }
      var myChart = echarts.init(chartDom)
      var categories = [
        { name: '核心作者' },
        { name: '合作者' },
        { name: '产业界合作者' }
      ]
      var option = {
        tooltip: { trigger: 'item', formatter: '{b}' },
        legend: {
          data: [categories[0].name, categories[1].name, categories[2].name],
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
          label: { show: true, position: 'right', fontSize: 11, color: '#303133' },
          lineStyle: { color: '#aaa', curveness: 0.3, width: 2, opacity: 0.6 },
          emphasis: { focus: 'adjacency', lineStyle: { width: 3 } },
          categories: categories,
          data: this.networkNodes,
          links: this.networkLinks
        }]
      }
      myChart.setOption(option)
      var self = this
      window.addEventListener('resize', function() { myChart.resize() })
      console.log('网络图初始化完成，节点数:', this.networkNodes.length)
    },
    
    initWeightChart: function() {
      var chartDom = this.$refs.weightChart
      if (!chartDom) {
        console.log('weightChart DOM 不存在')
        return
      }
      var myChart = echarts.init(chartDom)
      var data = [
        { value: (this.weights.pagerank || 0.35) * 100, name: 'PageRank', itemStyle: { color: '#6baed6' } },
        { value: (this.weights.betweenness || 0.25) * 100, name: 'Betweenness', itemStyle: { color: '#9ac9a8' } },
        { value: (this.weights.cpd || 0.20) * 100, name: 'CPD', itemStyle: { color: '#fdbe85' } },
        { value: (this.weights.closeness || 0.20) * 100, name: 'Closeness', itemStyle: { color: '#f4a2a2' } }
      ]
      var option = {
        tooltip: { trigger: 'item', formatter: '{b}: {d}%' },
        legend: { show: true, orient: 'vertical', left: 'left', data: ['PageRank', 'Betweenness', 'CPD', 'Closeness'] },
        series: [{
          name: '权重',
          type: 'pie',
          radius: '55%',
          center: ['50%', '50%'],
          data: data,
          label: { show: true, formatter: '{b}: {d}%' },
          emphasis: { scale: true }
        }]
      }
      myChart.setOption(option)
      window.addEventListener('resize', function() { myChart.resize() })
      console.log('权重图初始化完成', data)
    }
  }
}
</script>

<style scoped>
.author-credibility {
  font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif;
  padding: 20px;
  background: #f5f7fa;
  min-height: 100vh;
}

.author-credibility .el-card {
  border: 2px solid #3C848C;
  border-radius: 0;
  box-shadow: none !important;
  background-color: #FFFFFF;
}

.author-credibility .el-card__header {
  color: #3C848C;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 1px;
  padding: 14px 20px;
  border-bottom: 2px solid #3C848C;
  background: white;
}

.retro-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.retro-card-header i {
  color: #3C848C;
  margin-right: 8px;
  font-size: 18px;
}

.card-title {
  color: #3C848C;
  font-size: 14px;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.retro-btn {
  border-radius: 0;
  border: 2px solid #3C848C;
  font-weight: 800;
  text-transform: uppercase;
  padding: 10px 20px;
  font-size: 14px;
  letter-spacing: 1px;
  background-color: #FFFFFF;
  color: #3C848C;
  cursor: pointer;
}

.retro-btn.primary {
  background-color: #3C848C;
  color: #FFFFFF;
}

.retro-btn.primary:hover {
  background-color: #2F696F;
}

.retro-table {
  border: 2px solid #3C848C;
}

.retro-table >>> .el-table__header-wrapper th {
  background-color: #3C848C;
  color: #FFFFFF;
  font-weight: 800;
  text-transform: uppercase;
  border-right: 1px solid #BDCACC;
  border-bottom: 2px solid #3C848C;
  font-size: 13px;
  padding: 10px 0;
}

.rank-tag {
  border-radius: 0;
  font-weight: 800;
  border: none;
}

.author-name {
  font-weight: 600;
}

.author-role {
  margin-left: 6px;
  color: #909399;
  font-size: 12px;
}

.score {
  color: #3C848C;
  font-weight: 600;
}

.retro-collapse {
  border: 2px solid #3C848C;
  border-radius: 0;
  background: white;
}

.retro-collapse .el-collapse-item__header {
  background: white;
  color: #3C848C;
  font-weight: 800;
  border-bottom: 2px solid #3C848C;
  padding-left: 20px;
}

.collapse-content {
  padding: 16px;
  background: #f9fafc;
}

.inner-card {
  background: white;
  padding: 16px;
  border: 2px solid #3C848C;
  border-radius: 0;
  height: 100%;
}

.inner-card-title {
  margin-top: 0;
  margin-bottom: 12px;
  font-size: 15px;
  color: #3C848C;
  font-weight: 800;
  text-transform: uppercase;
}

.accent-bg {
  background: #ecf5ff;
}

.weighted-formula {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
}

.accent {
  color: #3C848C;
}

.step-list {
  padding-left: 20px;
  margin: 0;
  color: #303133;
  line-height: 1.8;
}

.weight-label {
  font-weight: 600;
}

.weight-value {
  font-size: 15px;
  margin-top: 8px;
  display: block;
}

.example-section {
  margin-top: 20px;
  border-top: 2px solid #3C848C;
  padding-top: 16px;
}

.value {
  font-weight: 600;
  float: right;
}

.calculation-result p {
  margin: 5px 0;
}

.result-number {
  margin: 10px 0 0;
  font-size: 20px;
  font-weight: 700;
  color: #3C848C;
}

.result-badge {
  background: #3C848C;
  color: white;
  padding: 2px 12px;
  margin-left: 8px;
  display: inline-block;
}

.formula {
  font-size: 15px;
  font-weight: 600;
}

.rule-desc {
  color: #606266;
  font-size: 13px;
}

.harmonic-value {
  font-size: 16px;
  font-weight: 700;
  margin: 8px 0;
}

.table-title {
  font-weight: 600;
  text-align: center;
  margin: 0 0 8px;
}

.calc-line {
  font-size: 13px;
  margin: 0 0 5px;
}

.note {
  margin: 12px 0 0;
  color: #606266;
  font-size: 13px;
}

.page-title {
  font-size: 20px;
  font-weight: 800;
  color: #3C848C;
  display: flex;
  align-items: center;
  text-transform: uppercase;
  margin: 0 0 8px;
}

.page-title i {
  color: #3C848C;
  font-size: 24px;
}

.page-subtitle {
  color: #666666;
  font-size: 14px;
  margin-top: 8px;
  font-weight: 700;
  text-transform: uppercase;
}

.paper-highlight {
  font-weight: 800;
  color: #3C848C;
}

.paper-meta {
  margin-left: 12px;
  background: #BDCACC;
  padding: 4px 12px;
  font-size: 12px;
  color: #3C848C;
}

.weight-legend {
  margin-top: 16px;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  background: #f5f7fa;
  padding: 12px;
  border: 2px solid #3C848C;
}

.legend-dot {
  display: inline-block;
  width: 12px;
  height: 12px;
  margin-right: 6px;
  border-radius: 2px;
}

.paper-info {
  font-size: 14px;
  line-height: 2;
}

.paper-info > div {
  display: flex;
  justify-content: space-between;
  border-bottom: 1px dashed #ebeef5;
  padding: 8px 0;
}

.info-label {
  color: #666666;
  font-weight: 700;
  text-transform: uppercase;
}

.info-value {
  font-weight: 600;
  color: #3C848C;
}

.paper-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
}

.footer-label {
  color: #606266;
}

.retro-tag {
  background-color: #BDCACC;
  color: #3C848C;
  border: 2px solid #3C848C;
  border-radius: 0;
  font-weight: 700;
  padding: 4px 8px;
}

.retro-divider {
  background-color: #3C848C;
  height: 2px;
}

.action-footer {
  margin-top: 24px;
  text-align: right;
  border-top: 2px solid #3C848C;
  padding-top: 16px;
}
</style>