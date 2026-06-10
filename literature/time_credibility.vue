<template>
  <div class="time-credibility" v-loading="loading">
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

    <!-- 折叠面板 -->
    <el-collapse class="retro-collapse" style="margin-bottom: 20px;">
      <el-collapse-item>
        <template slot="title">
          <i class="el-icon-info" style="color: #3C848C; margin-right: 8px;"></i>
          <span class="collapse-title">时效可信度计算方法</span>
        </template>
        <div class="collapse-content">
          <div class="inner-card">
            <h4 class="inner-card-title">时效可信度定义</h4>
            <p class="formula"><strong>C_time(p)</strong> = (y - Y_min) / (Y_max - Y_min)</p>
            <p class="rule-desc">
              y：文献 p 的发表日期<br>
              Y_min：数据表内最小发表日期<br>
              Y_max：数据表内最大发表日期<br>
              结果归一化至 [0, 1]，越接近 1 表示时效性越好
            </p>
          </div>
        </div>
      </el-collapse-item>
    </el-collapse>

    <!-- 两列布局 -->
    <el-row :gutter="20">
      <el-col :xs="24" :lg="14">
        <el-card class="retro-card" shadow="never" style="margin-bottom: 20px;">
          <div slot="header" class="retro-card-header">
            <span><i class="el-icon-data-line"></i> <span class="card-title">文献数据表日期统计</span></span>
          </div>
          <el-row :gutter="20" class="stat-cards">
            <el-col :span="8">
              <div class="stat-item">
                <div class="stat-label">最小发表日期</div>
                <div class="stat-number">{{ minDateStr || '—' }}</div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="stat-item">
                <div class="stat-label">最大发表日期</div>
                <div class="stat-number">{{ maxDateStr || '—' }}</div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="stat-item">
                <div class="stat-label">文献总数</div>
                <div class="stat-number">{{ dateList.length }}</div>
              </div>
            </el-col>
          </el-row>
          <el-divider class="retro-divider"></el-divider>
          <div class="date-list">
            <span style="font-weight: 700;">所有发表日期：</span>
            <span v-for="(d, idx) in dateStrList" :key="idx" class="date-item">{{ d }}</span>
          </div>
        </el-card>

        <el-card class="retro-card" shadow="never" v-if="targetDate">
          <div slot="header" class="retro-card-header">
            <span><i class="el-icon-calculator"></i> <span class="card-title">本论文时效可信度计算</span></span>
          </div>
          <el-row :gutter="20">
            <el-col :span="12">
              <div class="inner-card">
                <p><span class="info-label">目标日期：</span><span class="info-value">{{ targetDateStr }}</span></p>
                <p><span class="info-label">最小日期：</span><span class="info-value">{{ minDateStr }}</span></p>
                <p><span class="info-label">最大日期：</span><span class="info-value">{{ maxDateStr }}</span></p>
                <el-divider class="retro-divider"></el-divider>
                <p><span class="info-label">时间跨度：</span><span class="info-value">{{ yearSpan }} 年</span></p>
              </div>
            </el-col>
            <el-col :span="12">
              <div class="inner-card accent-bg calculation-result">
                <p><strong>C_time</strong> = (y - Y_min) / (Y_max - Y_min)</p>
                <p>= ({{ targetTime }} - {{ minTime }}) / ({{ maxTime }} - {{ minTime }})</p>
                <p>= {{ diffTarget }} / {{ diffTotal }}</p>
                <p class="result-number">= {{ c_time.toFixed(4) }} <span class="result-badge">{{ (c_time * 100).toFixed(1) }}%</span></p>
                <p class="note">该文献时效性处于数据集前 {{ ((1 - c_time) * 100).toFixed(1) }}% 区间</p>
              </div>
            </el-col>
          </el-row>
        </el-card>
        <el-card class="retro-card" shadow="never" v-else>
          <div slot="header" class="retro-card-header">
            <span><i class="el-icon-calculator"></i> <span class="card-title">本论文时效可信度计算</span></span>
          </div>
          <div class="empty-state">
            <p>请从文献列表选择文献查看时效可信度</p>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="10">
        <el-card class="retro-card" shadow="never" style="margin-bottom: 20px;">
          <div slot="header" class="retro-card-header">
            <span><i class="el-icon-s-marketing"></i> <span class="card-title">文献发表日期分布</span></span>
          </div>
          <div ref="timelineChart" style="width: 100%; height: 250px;"></div>
          <div class="chart-legend">
            <span><span class="legend-dot" style="background: #6baed6;"></span> 文献日期</span>
            <span><span class="legend-dot" style="background: #f4a2a2;"></span> 最小日期</span>
            <span><span class="legend-dot" style="background: #9ac9a8;"></span> 最大日期</span>
            <span><span class="legend-dot" style="background: #fdbe85;"></span> 目标日期</span>
          </div>
        </el-card>

        <el-card class="retro-card" shadow="never" v-if="targetDate">
          <div slot="header" class="retro-card-header">
            <span><i class="el-icon-document"></i> <span class="card-title">时效性解读</span></span>
          </div>
          <div class="interpretation-list">
            <div><span class="info-label">本论文发表日期</span><span class="info-value">{{ targetDateStr }}</span></div>
            <div><span class="info-label">在数据集中的位置</span><span class="info-value">第 {{ rankInDataset }} / {{ dateList.length }} 新</span></div>
            <div><span class="info-label">时效可信度得分</span><span class="info-value">{{ (c_time * 100).toFixed(1) }}%</span></div>
            <div><span class="info-label">时效性评级</span><el-tag :type="ratingType" size="medium">{{ ratingText }}</el-tag></div>
          </div>
          <el-divider class="retro-divider"></el-divider>
          <div class="note">时效可信度越接近 100% 表示文献越新，在数据集中处于越靠前的位置。</div>
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

export default {
  name: 'TimeCredibility',
  data: function() {
    return {
      loading: false,
      allDateStrings: [],
      targetDateStr: null,
      currentPaperId: null,
      mockDates: [
        '2018.6.15', '2016.12.19', '2016.1.21', '2021.2.1', '2023.6.30',
        '2020.9.12', '2019.4.18', '2022.11.5', '2017.7.14', '2024.3.15'
      ]
    }
  },
  computed: {
    dateList: function() {
      var self = this
      var dates = this.allDateStrings.length > 0 ? this.allDateStrings : this.mockDates
      return dates.map(function(d) { return self.parseDate(d) }).filter(function(d) { return d !== null })
    },
    dateStrList: function() {
      return this.dateList.map(function(d) { return this.formatDate(d) }.bind(this))
    },
    minDate: function() {
      if (this.dateList.length === 0) return null
      var times = this.dateList.map(function(d) { return d.getTime() })
      return new Date(Math.min.apply(null, times))
    },
    maxDate: function() {
      if (this.dateList.length === 0) return null
      var times = this.dateList.map(function(d) { return d.getTime() })
      return new Date(Math.max.apply(null, times))
    },
    minDateStr: function() {
      return this.minDate ? this.formatDate(this.minDate) : '—'
    },
    maxDateStr: function() {
      return this.maxDate ? this.formatDate(this.maxDate) : '—'
    },
    targetDate: function() {
      return this.targetDateStr ? this.parseDate(this.targetDateStr) : null
    },
    targetTime: function() {
      return this.targetDate ? this.targetDate.getTime() : 0
    },
    minTime: function() {
      return this.minDate ? this.minDate.getTime() : 0
    },
    maxTime: function() {
      return this.maxDate ? this.maxDate.getTime() : 0
    },
    diffTarget: function() {
      return this.targetTime - this.minTime
    },
    diffTotal: function() {
      return this.maxTime - this.minTime
    },
    c_time: function() {
      if (this.diffTotal === 0) return 0
      return this.diffTarget / this.diffTotal
    },
    yearSpan: function() {
      var days = this.diffTotal / (1000 * 3600 * 24)
      return (days / 365.25).toFixed(1)
    },
    rankInDataset: function() {
      if (!this.targetDate || this.dateList.length === 0) return '—'
      var sorted = this.dateList.slice().sort(function(a, b) { return b - a })
      for (var i = 0; i < sorted.length; i++) {
        if (sorted[i].toDateString() === this.targetDate.toDateString()) {
          return i + 1
        }
      }
      return '—'
    },
    ratingText: function() {
      var score = this.c_time
      if (score >= 0.8) return '非常新'
      if (score >= 0.6) return '较新'
      if (score >= 0.4) return '中等'
      if (score >= 0.2) return '较旧'
      return '非常旧'
    },
    ratingType: function() {
      var score = this.c_time
      if (score >= 0.8) return 'success'
      if (score >= 0.6) return 'primary'
      if (score >= 0.4) return 'warning'
      return 'danger'
    }
  },
  watch: {
    '$route.query.id': {
      immediate: true,
      handler: function(newId) {
        var self = this
        if (newId) {
          self.currentPaperId = newId
          self.loadPaperDate(newId)
        } else {
          self.targetDateStr = null
        }
        self.$nextTick(function() {
          setTimeout(function() {
            self.initTimelineChart()
          }, 200)
        })
      }
    }
  },
  mounted: function() {
    var self = this
    this.loadAllDates()
    if (!this.$route.query.id) {
      this.loading = false
    }
  },
  methods: {
    parseDate: function(str) {
      if (!str) return null
      if (/^\d{4}$/.test(str)) {
        return new Date(parseInt(str, 10), 0, 1)
      }
      var parts = str.split('.')
      if (parts.length === 3) {
        var year = parseInt(parts[0], 10)
        var month = parseInt(parts[1], 10) - 1
        var day = parseInt(parts[2], 10)
        return new Date(year, month, day)
      }
      parts = str.split('-')
      if (parts.length === 3) {
        year = parseInt(parts[0], 10)
        month = parseInt(parts[1], 10) - 1
        day = parseInt(parts[2], 10)
        return new Date(year, month, day)
      }
      return null
    },
    formatDate: function(date) {
      if (!date) return ''
      var y = date.getFullYear()
      var m = (date.getMonth() + 1).toString().padStart(2, '0')
      var d = date.getDate().toString().padStart(2, '0')
      return y + '-' + m + '-' + d
    },
    loadAllDates: function() {
      var self = this
      // 尝试从后端获取，如果失败则使用模拟数据
      axios.get('http://localhost:3007/api/data?pageSize=1000').then(function(response) {
        if (response.data && Array.isArray(response.data.data)) {
          var dates = []
          for (var i = 0; i < response.data.data.length; i++) {
            var pubDate = response.data.data[i]['发表日期']
            if (pubDate) {
              dates.push(pubDate)
            }
          }
          if (dates.length > 0) {
            self.allDateStrings = dates
          } else {
            self.allDateStrings = self.mockDates
          }
        } else {
          self.allDateStrings = self.mockDates
        }
      }).catch(function(error) {
        console.error('加载日期列表失败:', error)
        self.allDateStrings = self.mockDates
      })
    },
    loadPaperDate: function(paperId) {
      var self = this
      self.loading = true
      axios.get('http://localhost:3007/api/id/' + paperId).then(function(response) {
        if (response.data && response.data.success) {
          var paperData = response.data.data
          var pubDate = paperData['发表日期']
          if (pubDate) {
            if (typeof pubDate === 'number') {
              pubDate = pubDate + '.1.1'
            }
            self.targetDateStr = pubDate
          } else {
            self.$message.warning('该文献没有发表日期信息')
            self.targetDateStr = null
          }
        } else {
          self.$message.error('获取文献信息失败')
        }
      }).catch(function(error) {
        console.error('加载文献日期失败:', error)
        self.$message.warning('加载失败，使用模拟数据')
        self.targetDateStr = '2024.3.15'
      }).finally(function() {
        self.loading = false
        self.$nextTick(function() {
          setTimeout(function() {
            self.initTimelineChart()
          }, 200)
        })
      })
    },
    initTimelineChart: function() {
      var chartDom = this.$refs.timelineChart
      if (!chartDom) return
      if (this.dateList.length === 0) return
      
      var myChart = echarts.init(chartDom)
      var points = this.dateList.map(function(d) { return d.getTime() }).sort(function(a, b) { return a - b })
      
      var markPoints = []
      if (this.minDate) {
        markPoints.push({
          name: '最小',
          value: this.minDate.getTime(),
          symbol: 'circle',
          symbolSize: 12,
          itemStyle: { color: '#f4a2a2' }
        })
      }
      if (this.maxDate) {
        markPoints.push({
          name: '最大',
          value: this.maxDate.getTime(),
          symbol: 'circle',
          symbolSize: 12,
          itemStyle: { color: '#9ac9a8' }
        })
      }
      if (this.targetDate) {
        markPoints.push({
          name: '目标',
          value: this.targetDate.getTime(),
          symbol: 'diamond',
          symbolSize: 14,
          itemStyle: { color: '#fdbe85' }
        })
      }
      
      var option = {
        tooltip: {
          trigger: 'item',
          formatter: function(params) {
            var date = new Date(params.value)
            return date.getFullYear() + '-' + (date.getMonth() + 1) + '-' + date.getDate()
          }
        },
        grid: { left: '5%', right: '5%', top: 20, bottom: 20 },
        xAxis: { type: 'time', name: '发表日期' },
        yAxis: { type: 'value', show: false },
        series: [
          {
            name: '文献日期',
            type: 'scatter',
            data: points,
            symbolSize: 8,
            itemStyle: { color: '#6baed6' }
          },
          {
            name: '标记点',
            type: 'scatter',
            data: markPoints,
            symbolSize: 12,
            label: { show: true, position: 'top', formatter: function(params) { return params.name }, fontSize: 11 }
          }
        ]
      }
      
      myChart.setOption(option)
      window.addEventListener('resize', function() { myChart.resize() })
    }
  }
}
</script>

<style scoped>
.time-credibility {
  font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif;
  padding: 20px;
  background: #f5f7fa;
  min-height: 100vh;
}

.time-credibility .el-card {
  border: 2px solid #3C848C;
  border-radius: 0;
  box-shadow: none !important;
  background-color: #FFFFFF;
}

.time-credibility .el-card__header {
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

.accent-bg {
  background: #ecf5ff;
}

.formula {
  font-size: 15px;
  font-weight: 600;
}

.rule-desc {
  color: #606266;
  font-size: 13px;
  margin-top: 8px;
}

.stat-cards {
  margin: 16px 0;
}

.stat-item {
  text-align: center;
  padding: 16px;
  border: 2px solid #3C848C;
  background: #f5f7fa;
}

.stat-label {
  font-size: 13px;
  color: #666;
  font-weight: 700;
  text-transform: uppercase;
}

.stat-number {
  font-size: 20px;
  font-weight: 800;
  color: #3C848C;
}

.date-list {
  font-size: 13px;
  color: #666;
  max-height: 120px;
  overflow-y: auto;
  padding: 8px;
  border: 1px solid #BDCACC;
  margin-top: 8px;
}

.date-item {
  margin-right: 12px;
  display: inline-block;
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

.note {
  margin: 12px 0 0;
  color: #909399;
  font-size: 13px;
}

.chart-legend {
  margin-top: 16px;
  display: flex;
  justify-content: space-around;
  font-size: 13px;
}

.legend-dot {
  display: inline-block;
  width: 12px;
  height: 12px;
  margin-right: 6px;
  border-radius: 2px;
}

.interpretation-list {
  font-size: 14px;
  line-height: 2;
}

.interpretation-list > div {
  display: flex;
  justify-content: space-between;
  border-bottom: 1px dashed #ebeef5;
  padding: 8px 0;
}

.empty-state {
  text-align: center;
  padding: 40px;
  color: #909399;
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
  margin: 16px 0;
}
</style>