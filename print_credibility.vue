<template>
  <div class="print-credibility" v-loading="loading">
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
    <div style="margin-bottom: 24px;" v-if="paper.id">
      <h2 class="page-title">
        <i class="el-icon-document-copy" style="margin-right: 12px;"></i>
        期刊可信度 · C_print 评估
      </h2>
      <p class="page-subtitle">
        当前文献 <span class="paper-highlight">《{{ paper.title }}》</span>
        <span class="paper-meta">{{ paper.journal }}, {{ paper.year }}</span>
      </p>
    </div>

    <!-- 折叠面板 -->
    <el-collapse class="retro-collapse" style="margin-bottom: 20px;" v-model="activeCollapse">
      <!-- 1. 期刊可信度计算方法 -->
      <el-collapse-item name="method">
        <template slot="title">
          <i class="el-icon-info" style="color: #3C848C; margin-right: 8px;"></i>
          <span class="collapse-title">📐 期刊可信度 C_print 计算方法</span>
        </template>
        <div class="collapse-content">
          <div class="inner-card">
            <h4 class="inner-card-title">四步计算流程</h4>
            <p class="formula"><strong>第一步：分区间归一化影响因子</strong></p>
            <p class="formula">IF < 11：C_norm = IF / 11</p>
            <p class="formula">IF >= 11：C_norm = (IF - 11) / (IF_max - 11)</p>
            <p class="formula"><strong>第二步：权重调整</strong></p>
            <p class="formula">IF < 11：C_adj = C_norm × 0.9</p>
            <p class="formula">IF >= 11：C_adj = C_norm × 0.1 + 0.9</p>
            <p class="formula"><strong>第三步：加类型值</strong></p>
            <p class="formula">C_raw = C_adj + C_type</p>
            <p class="formula"><strong>第四步：二次归一化</strong></p>
            <p class="formula">C_print = C_raw / C_raw_max</p>
            <el-divider class="retro-divider"></el-divider>
            <p class="rule-title">C_type 取值规则</p>
            <ul class="rule-list">
              <li>综述 → 1.0</li>
              <li>研究型 → 0.8</li>
              <li>会议论文 → 0.6</li>
              <li>其他 → 0.2</li>
            </ul>
          </div>
        </div>
      </el-collapse-item>

      <!-- 2. 当前文献真实计算过程 -->
      <el-collapse-item name="calculation">
        <template slot="title">
          <i class="el-icon-document" style="color: #3C848C; margin-right: 8px;"></i>
          <span class="collapse-title">📄 当前文献 C_print 真实计算 (ID: {{ paper.id }})</span>
        </template>
        <div class="collapse-content">
          <!-- 输入值卡片 -->
          <el-row :gutter="20" style="margin-bottom: 16px;">
            <el-col :span="12">
              <div class="inner-card">
                <h4 class="inner-card-title">📌 输入值</h4>
                <p><span class="info-label">文献类型:</span> <span class="info-value">{{ paper.document_type || '—' }}</span></p>
                <p><span class="info-label">期刊:</span> <span class="info-value">{{ paper.journal || '—' }}</span></p>
                <p><span class="info-label">影响因子 (IF):</span> <span class="info-value">{{ paper.IS || '—' }}</span></p>
              </div>
            </el-col>
            <el-col :span="12">
              <div class="inner-card">
                <h4 class="inner-card-title">📌 区间信息</h4>
                <p><span class="info-label">所属区间:</span>
                  <el-tag :type="isLowIf ? 'info' : 'success'" size="small" class="info-tag">
                    {{ isLowIf ? '低 IF 区间 (IF < 11)' : '高 IF 区间 (IF ≥ 11)' }}
                  </el-tag>
                </p>
                <p v-if="isLowIf"><span class="info-label">分母:</span> <span class="info-value">{{ threshold }}</span></p>
                <p v-else>
                  <span class="info-label">区间范围:</span>
                  <span class="info-value">{{ intervalMin }} ~ {{ intervalMax.toFixed(2) }}</span>
                </p>
                <p v-if="!isLowIf" style="margin-top: 8px;">
                  <span class="info-label">分母:</span>
                  <span class="info-value">{{ (intervalMax - intervalMin).toFixed(2) }}</span>
                </p>
              </div>
            </el-col>
          </el-row>

          <!-- 第一步 + 第二步 -->
          <el-row :gutter="20" style="margin-bottom: 16px;">
            <el-col :span="12">
              <div class="inner-card">
                <h4 class="inner-card-title">📌 第一步：分区间归一化</h4>
                <p v-if="isLowIf" class="formula">
                  C<sub>publication</sub><sup>norm</sup> = {{ paper.IS }} / {{ threshold }} = {{ C_publication.toFixed(4) }}
                </p>
                <p v-else class="formula">
                  C<sub>publication</sub><sup>norm</sup> = ({{ paper.IS }} - {{ intervalMin }}) / ({{ intervalMax.toFixed(2) }} - {{ intervalMin }}) = {{ C_publication.toFixed(4) }}
                </p>
                <p class="note">将影响因子映射到 [0, 1] 区间</p>
              </div>
            </el-col>
            <el-col :span="12">
              <div class="inner-card">
                <h4 class="inner-card-title">📌 第二步：权重调整</h4>
                <p v-if="isLowIf" class="formula">
                  C<sub>adj</sub> = {{ C_publication.toFixed(4) }} × 0.9 = {{ C_publication_adjusted.toFixed(4) }}
                </p>
                <p v-else class="formula">
                  C<sub>adj</sub> = {{ C_publication.toFixed(4) }} × 0.1 + 0.9 = {{ C_publication_adjusted.toFixed(4) }}
                </p>
                <p class="note">目标：低 IF 压缩到 [0, 0.9)，高 IF 映射到 [0.9, 1.0]</p>
              </div>
            </el-col>
          </el-row>

          <!-- 第三步 + 第四步 -->
          <el-row :gutter="20" style="margin-bottom: 16px;">
            <el-col :span="12">
              <div class="inner-card">
                <h4 class="inner-card-title">📌 第三步：加类型值</h4>
                <p><span class="info-label">文献类型:</span> <span class="info-value">{{ paper.document_type || '其他' }}</span></p>
                <p><span class="info-label">C<sub>type</sub>:</span> <span class="info-value">{{ C_type.toFixed(1) }}</span></p>
                <p class="formula">
                  C<sub>print</sub><sup>raw</sup> = {{ C_publication_adjusted.toFixed(4) }} + {{ C_type.toFixed(1) }} = {{ rawCprint.toFixed(4) }}
                </p>
                <p class="note">C<sub>type</sub> 范围: 0.2 ~ 1.0，raw 值范围约 [0.9, 2.0]</p>
              </div>
            </el-col>
            <el-col :span="12">
              <div class="inner-card">
                <h4 class="inner-card-title">📌 第四步：二次归一化</h4>
                <p><span class="info-label">全局最大值 C<sub>print</sub><sup>raw</sup><sub>max</sub>:</span> <span class="info-value">{{ globalMaxCprint.toFixed(4) }}</span></p>
                <p class="formula">
                  C<sub>print</sub><sup>norm</sup> = {{ rawCprint.toFixed(4) }} / {{ globalMaxCprint.toFixed(4) }} = {{ C_print_norm.toFixed(4) }}
                </p>
                <p class="result-number">
                  最终结果 = {{ C_print_norm.toFixed(4) }} 
                  <span class="result-badge">{{ (C_print_norm * 100).toFixed(1) }}%</span>
                </p>
              </div>
            </el-col>
          </el-row>

          <!-- 额外说明卡片 -->
          <el-row :gutter="20">
            <el-col :span="24">
              <div class="inner-card accent-bg">
                <h4 class="inner-card-title">📌 计算说明</h4>
                <p class="desc">
                  • 第一步：分区间归一化，低 IF 区间固定分母 11，高 IF 区间动态分母 (IF<sub>max</sub> - 11)<br>
                  • 第二步：权重调整，低 IF 压缩到 [0, 0.9)，高 IF 映射到 [0.9, 1.0]<br>
                  • 第三步：根据文献类型加上 C<sub>type</sub> 值（综述 1.0，研究型 0.8，会议论文 0.6，其他 0.2）<br>
                  • 第四步：除以全局最大 raw 值进行二次归一化，确保最终结果在 [0, 1] 区间
                </p>
              </div>
            </el-col>
          </el-row>
        </div>
      </el-collapse-item>
    </el-collapse>

    <!-- 两列布局 -->
    <el-row :gutter="20">
      <el-col :xs="24" :lg="12">
        <el-card class="retro-card" shadow="never" style="margin-bottom: 20px;">
          <div slot="header" class="retro-card-header">
            <span><i class="el-icon-pie-chart"></i> <span class="card-title">文献类型分布</span></span>
          </div>
          <div ref="typeChart" style="width: 100%; height: 250px;"></div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="12">
        <el-card class="retro-card" shadow="never">
          <div slot="header" class="retro-card-header">
            <span><i class="el-icon-collection"></i> <span class="card-title">核心统计信息</span></span>
          </div>
          <div class="stat-list">
            <div><span class="stat-label">文献总数：</span><span class="stat-value">{{ allPapers.length }}</span></div>
            <div><span class="stat-label">综述篇数：</span><span class="stat-value">{{ typeStats.reviewCount }}</span></div>
            <div><span class="stat-label">研究型篇数：</span><span class="stat-value">{{ typeStats.articleCount }}</span></div>
            <div><span class="stat-label">会议论文：</span><span class="stat-value">{{ typeStats.proceedingsCount }}</span></div>
            <div><span class="stat-label">平均 IF：</span><span class="stat-value">{{ avgIf.toFixed(2) }}</span></div>
            <div><span class="stat-label">C_print 平均：</span><span class="stat-value">{{ avgCprint.toFixed(4) }}</span></div>
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

// 后端 API 地址
var API_BASE_URL = 'http://localhost:3007'

export default {
  name: 'PrintCredibility',
  data: function() {
    return {
      loading: false,
      activeCollapse: [], // 默认展开两个面板
      paper: {
        id: null,
        title: '',
        journal: '',
        year: '',
        document_type: '',
        IS: 0
      },
      allPapers: [],
      typeStats: {
        reviewCount: 0,
        articleCount: 0,
        proceedingsCount: 0
      },
      avgIf: 0,
      avgCprint: 0,
      // 计算过程变量
      C_publication: 0,
      C_publication_adjusted: 0,
      C_type: 0,
      rawCprint: 0,
      C_print_norm: 0,
      isLowIf: true,
      threshold: 11,
      intervalMin: 11,
      intervalMax: 11,
      globalMaxCprint: 0
    }
  },
  watch: {
    '$route.query.id': {
      immediate: true,
      handler: function(newId) {
        if (newId) {
          console.log('期刊可信度接收到文献ID:', newId)
          this.fetchPaperData(newId)
        }
      }
    }
  },
  mounted: function() {
    if (!this.$route.query.id) {
      this.$message.info('请从文献列表选择文献查看可信度')
      this.loading = false
    }
  },
  methods: {
    fetchPaperData: function(id) {
      var self = this
      self.loading = true
      
      // 第一步：先获取所有文献列表
      axios.get(API_BASE_URL + '/api/data?pageSize=1000').then(function(allRes) {
        if (allRes.data && Array.isArray(allRes.data.data)) {
          var getYear = function(date) {
            if (!date) return ''
            if (typeof date === 'string') {
              var match = date.match(/(\d{4})/)
              return match ? match[1] : ''
            }
            return String(date)
          }
          
          self.allPapers = allRes.data.data.map(function(p) {
            return {
              id: p.id,
              title: p['题目'] || '',
              journal: p['期刊'] || '',
              year: getYear(p['发表日期']),
              document_type: p['文献类型'] || '',
              IS: parseFloat(p['IS']) || 0,
              corresponding_author: p['通讯作者'] || '',
              citation_count: p['被引数'] || 0
            }
          })
          
          // 第二步：计算全局统计信息
          self.calculateGlobalStats()
          
          // 第三步：获取当前文献信息
          return axios.get(API_BASE_URL + '/api/id/' + id)
        } else {
          throw new Error('获取文献列表失败')
        }
      }).then(function(paperRes) {
        if (paperRes.data && paperRes.data.success) {
          var paperData = paperRes.data.data
          var getYear = function(date) {
            if (!date) return ''
            if (typeof date === 'string') {
              var match = date.match(/(\d{4})/)
              return match ? match[1] : ''
            }
            return String(date)
          }
          
          self.paper = {
            id: paperData.id,
            title: paperData['题目'] || '',
            journal: paperData['期刊'] || '',
            year: getYear(paperData['发表日期']),
            document_type: paperData['文献类型'] || '',
            IS: parseFloat(paperData['IS']) || 0,
            corresponding_author: paperData['通讯作者'] || '',
            citation_count: paperData['被引数'] || 0
          }
          
          // 第四步：计算当前文献的 C_print
          self.calculateCurrentPaperCprint()
          
          // 第五步：计算平均 C_print
          self.calculateAvgCprint()
          
          self.$nextTick(function() {
            self.initTypeChart()
          })
        } else {
          self.$message.error('获取文献详情失败')
        }
      }).catch(function(error) {
        console.error('加载数据失败:', error)
        self.$message.error('数据加载失败，请确保后端服务在 ' + API_BASE_URL + ' 运行')
      }).finally(function() {
        self.loading = false
      })
    },
    
    // 计算全局统计信息
    calculateGlobalStats: function() {
      var self = this
      var stats = {
        reviewCount: 0,
        articleCount: 0,
        proceedingsCount: 0,
        sumIf: 0
      }
      
      // 计算 IF 最大值
      var allIfs = []
      for (var i = 0; i < self.allPapers.length; i++) {
        var p = self.allPapers[i]
        var ifVal = p.IS || 0
        if (ifVal > 0) allIfs.push(ifVal)
        
        var docType = p.document_type || ''
        if (docType === '综述') stats.reviewCount++
        else if (docType === '研究型') stats.articleCount++
        else if (docType === '会议论文') stats.proceedingsCount++
        
        stats.sumIf += ifVal
      }
      
      var maxIf = allIfs.length > 0 ? Math.max.apply(null, allIfs) : 11
      self.intervalMax = Math.max(maxIf, 11)
      
      self.typeStats = {
        reviewCount: stats.reviewCount,
        articleCount: stats.articleCount,
        proceedingsCount: stats.proceedingsCount
      }
      self.avgIf = self.allPapers.length > 0 ? stats.sumIf / self.allPapers.length : 0
    },
    
    // 计算当前文献的 C_print
    calculateCurrentPaperCprint: function() {
      var self = this
      var IF = self.paper.IS || 0
      var docType = self.paper.document_type || ''
      
      // 判断区间
      self.isLowIf = IF < 11
      
      // 第一步：分区间归一化
      if (self.isLowIf) {
        self.C_publication = IF / 11
      } else {
        var denominator = self.intervalMax - 11
        self.C_publication = denominator > 0 ? (IF - 11) / denominator : 0
      }
      
      // 第二步：权重调整
      if (self.isLowIf) {
        self.C_publication_adjusted = self.C_publication * 0.9
      } else {
        self.C_publication_adjusted = self.C_publication * 0.1 + 0.9
      }
      
      // 第三步：加类型值
      if (docType === '综述') {
        self.C_type = 1.0
      } else if (docType === '研究型') {
        self.C_type = 0.8
      } else if (docType === '会议论文') {
        self.C_type = 0.6
      } else {
        self.C_type = 0.2
      }
      
      self.rawCprint = self.C_publication_adjusted + self.C_type
      
      // 计算全局最大 rawCprint
      self.calculateGlobalMaxCprint()
      
      // 第四步：二次归一化
      self.C_print_norm = self.globalMaxCprint > 0 ? self.rawCprint / self.globalMaxCprint : 0
    },
    
    // 计算全局最大 rawCprint
    calculateGlobalMaxCprint: function() {
      var self = this
      var allRawCprints = []
      
      for (var i = 0; i < self.allPapers.length; i++) {
        var p = self.allPapers[i]
        var IF = p.IS || 0
        var docType = p.document_type || ''
        var isLowIf = IF < 11
        
        // 第一步
        var C_publication = 0
        if (isLowIf) {
          C_publication = IF / 11
        } else {
          var denominator = self.intervalMax - 11
          C_publication = denominator > 0 ? (IF - 11) / denominator : 0
        }
        
        // 第二步
        var C_publication_adjusted = 0
        if (isLowIf) {
          C_publication_adjusted = C_publication * 0.9
        } else {
          C_publication_adjusted = C_publication * 0.1 + 0.9
        }
        
        // 第三步
        var C_type = 0.2
        if (docType === '综述') C_type = 1.0
        else if (docType === '研究型') C_type = 0.8
        else if (docType === '会议论文') C_type = 0.6
        
        var rawCprint = C_publication_adjusted + C_type
        allRawCprints.push(rawCprint)
      }
      
      self.globalMaxCprint = allRawCprints.length > 0 ? Math.max.apply(null, allRawCprints) : 1
      if (self.globalMaxCprint <= 0) self.globalMaxCprint = 1
    },
    
    // 计算平均 C_print
    calculateAvgCprint: function() {
      var self = this
      var sumCprint = 0
      
      for (var i = 0; i < self.allPapers.length; i++) {
        var p = self.allPapers[i]
        var IF = p.IS || 0
        var docType = p.document_type || ''
        var isLowIf = IF < 11
        
        var C_publication = 0
        if (isLowIf) {
          C_publication = IF / 11
        } else {
          var denominator = self.intervalMax - 11
          C_publication = denominator > 0 ? (IF - 11) / denominator : 0
        }
        
        var C_publication_adjusted = 0
        if (isLowIf) {
          C_publication_adjusted = C_publication * 0.9
        } else {
          C_publication_adjusted = C_publication * 0.1 + 0.9
        }
        
        var C_type = 0.2
        if (docType === '综述') C_type = 1.0
        else if (docType === '研究型') C_type = 0.8
        else if (docType === '会议论文') C_type = 0.6
        
        var rawCprint = C_publication_adjusted + C_type
        var cprint = self.globalMaxCprint > 0 ? rawCprint / self.globalMaxCprint : 0
        sumCprint += cprint
      }
      
      self.avgCprint = self.allPapers.length > 0 ? sumCprint / self.allPapers.length : 0
    },
    
    initTypeChart: function() {
      var chartDom = this.$refs.typeChart
      if (!chartDom || !this.allPapers.length) return
      var myChart = echarts.init(chartDom)
      var typeCount = {}
      for (var i = 0; i < this.allPapers.length; i++) {
        var docType = this.allPapers[i].document_type || '其他'
        if (typeCount[docType]) {
          typeCount[docType]++
        } else {
          typeCount[docType] = 1
        }
      }
      var data = []
      for (var key in typeCount) {
        data.push({ name: key, value: typeCount[key] })
      }
      var option = {
        tooltip: { trigger: 'item', formatter: '{b}: {c}篇 ({d}%)' },
        legend: { show: true, orient: 'horizontal', left: 'center', top: 'bottom' },
        series: [{
          type: 'pie',
          radius: ['50%', '70%'],
          avoidLabelOverlap: false,
          itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
          label: { show: true, position: 'outside', formatter: '{b}', color: '#303133' },
          data: data
        }]
      }
      myChart.setOption(option)
      window.addEventListener('resize', function() { myChart.resize() })
    }
  }
}
</script>

<style scoped>
.print-credibility {
  font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif;
  padding: 20px;
  background: #f5f7fa;
  min-height: 100vh;
}

.print-credibility .el-card {
  border: 2px solid #3C848C;
  border-radius: 0;
  box-shadow: none !important;
  background-color: #FFFFFF;
}

.print-credibility .el-card__header {
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
}

.card-title {
  color: #3C848C;
  font-size: 14px;
  font-weight: 800;
  text-transform: uppercase;
}

.retro-btn {
  border-radius: 0;
  border: 2px solid #3C848C;
  font-weight: 800;
  text-transform: uppercase;
  padding: 10px 20px;
  background-color: #FFFFFF;
  color: #3C848C;
  cursor: pointer;
}

.retro-btn.primary {
  background-color: #3C848C;
  color: #FFFFFF;
}

.retro-collapse {
  border: 2px solid #3C848C;
  margin-bottom: 20px;
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
  border: 2px solid #3C848C;
  padding: 16px;
  background: white;
}

.inner-card-title {
  color: #3C848C;
  font-weight: 800;
  margin-top: 0;
  margin-bottom: 12px;
}

.formula {
  font-size: 15px;
  font-weight: 600;
  margin: 8px 0;
}

.rule-title {
  font-weight: 600;
  margin-bottom: 8px;
}

.rule-list {
  padding-left: 20px;
  color: #303133;
  line-height: 1.8;
}

.info-label {
  color: #666;
  font-weight: 700;
  margin-right: 8px;
}

.info-value {
  font-weight: 600;
  color: #3C848C;
}

.info-tag {
  border-radius: 0;
  border: none;
  font-weight: 700;
}

.note {
  margin: 12px 0 0;
  color: #909399;
  font-size: 13px;
}

.result-number {
  font-size: 18px;
  font-weight: 700;
  color: #3C848C;
  margin: 10px 0 0;
}

.result-badge {
  background: #3C848C;
  color: white;
  padding: 2px 12px;
  margin-left: 8px;
  display: inline-block;
}

.page-title {
  font-size: 20px;
  font-weight: 800;
  color: #3C848C;
  margin: 0 0 8px;
}

.page-subtitle {
  color: #666;
  font-size: 14px;
  margin-top: 8px;
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

.stat-list {
  font-size: 14px;
  line-height: 2;
}

.stat-list > div {
  display: flex;
  justify-content: space-between;
  border-bottom: 1px dashed #ebeef5;
  padding: 8px 0;
}

.stat-label {
  color: #666;
  font-weight: 700;
  text-transform: uppercase;
}

.stat-value {
  font-weight: 600;
  color: #3C848C;
}

.action-footer {
  margin-top: 24px;
  text-align: right;
  border-top: 2px solid #3C848C;
  padding-top: 16px;
}

.retro-divider {
  background-color: #3C848C;
  height: 2px;
}

.accent-bg {
  background: #ecf5ff;
}

.desc {
  margin: 8px 0 0;
  font-size: 14px;
  line-height: 1.8;
  color: #303133;
}

.formula sub, 
.formula sup {
  font-size: 12px;
}
</style>