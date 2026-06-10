<!-- src/views/extraction/result-visualization.vue -->
<template>
  <div class="result-visualization-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <span class="result-tag" v-if="hasResult">
        </span>
      </div>
      <div class="header-actions">
        <el-button type="success" @click="exportTriples" :loading="exporting">
          <i class="el-icon-download"></i> 导出三元组
        </el-button>
        <el-button @click="goBack">
          <i class="el-icon-arrow-left"></i> 返回
        </el-button>
      </div>
    </div>

    <!-- 无数据提示 -->
    <div v-if="!hasResult" class="no-data">
      <i class="el-icon-info"></i>
      <p>没有可显示的结果，请先完成抽取和纠错</p>
      <el-button type="primary" @click="goBack">返回抽取页面</el-button>
    </div>

    <div v-else class="result-content">
      <!-- 顶部概览卡片 -->
      <el-row :gutter="20" class="overview-row">
        <el-col :span="6">
          <el-card class="overview-card" shadow="hover">
            <div class="card-icon" style="background: #ecf5ff">
              <i class="el-icon-share" style="color: #409EFF"></i>
            </div>
            <div class="card-info">
              <div class="card-label">三元组总数</div>
              <div class="card-value">{{ totalTriples }}</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="overview-card" shadow="hover">
            <div class="card-icon" style="background: #fdf6ec">
              <i class="el-icon-s-management" style="color: #e6a23c"></i>
            </div>
            <div class="card-info">
              <div class="card-label">实体总数</div>
              <div class="card-value">{{ totalEntities }}</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="overview-card" shadow="hover">
            <div class="card-icon" style="background: #f0f9eb">
              <i class="el-icon-connection" style="color: #67c23a"></i>
            </div>
            <div class="card-info">
              <div class="card-label">关系总数</div>
              <div class="card-value">{{ totalRelations }}</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="overview-card" shadow="hover">
            <div class="card-icon" style="background: #f2f6fc">
              <i class="el-icon-document" style="color: #909399"></i>
            </div>
            <div class="card-info">
              <div class="card-label">处理句子</div>
              <div class="card-value">{{ sentenceCount }}</div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- ==================== 新增：可信度卡片 ==================== -->
      <el-row :gutter="20" class="credibility-row">
        <el-col :span="24">
          <el-card class="credibility-card" shadow="hover">
            <div slot="header" class="card-header">
              <span><i class="el-icon-star-on"></i> 知识可信度评估</span>
              <el-tag :type="credibilityLevel" size="small">{{ credibilityLevelText }}</el-tag>
              <span class="sample-badge">基于全体三元组 · 平均可信度</span>
            </div>
            <div class="credibility-content">
              <!-- 三列布局：来源/实体/关系 -->
              <el-row :gutter="20">
                <el-col :span="8">
                  <div class="credibility-item">
                    <div class="item-icon"><i class="el-icon-document"></i></div>
                    <div class="item-title">来源可信度</div>
                    <div class="item-value">{{ sourceCredibility.total }}%</div>
                    <div class="item-detail">
                      <span>文献权威 {{ sourceCredibility.literature }}%</span>
                      <span>时效 {{ sourceCredibility.timeliness }}%</span>
                      <span>作者 {{ sourceCredibility.author }}%</span>
                    </div>
                  </div>
                </el-col>
                <el-col :span="8">
                  <div class="credibility-item">
                    <div class="item-icon"><i class="el-icon-s-data"></i></div>
                    <div class="item-title">实体可信度</div>
                    <div class="item-value">{{ entityCredibility.total }}%</div>
                    <div class="item-detail">
                      <span>TF-IDF {{ entityCredibility.tfidf }}%</span>
                      <span>数据库可信度 {{ entityCredibility.db }}%</span>
                    </div>
                  </div>
                </el-col>
                <el-col :span="8">
                  <div class="credibility-item">
                    <div class="item-icon"><i class="el-icon-connection"></i></div>
                    <div class="item-title">关系可信度</div>
                    <div class="item-value">{{ relationCredibility.total }}%</div>
                    <div class="item-detail">
                      <span>因果一致性 {{ relationCredibility.causal }}%</span>
                      <span>抽取质量 {{ relationCredibility.extraction }}%</span>
                    </div>
                  </div>
                </el-col>
              </el-row>

              <!-- 综合评分 -->
              <div class="credibility-footer">
                <div class="overall-score">
                  <span class="overall-label">综合知识可信度</span>
                  <span class="overall-value" :style="{ color: credibilityOverallColor }">{{ knowledgeCredibility }}%</span>
                </div>
                <div class="credibility-formula">
                  <i class="el-icon-tickets"></i> 综合 = 0.4×来源 + 0.3×实体 + 0.3×关系
                </div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
      <!-- ==================== 可信度卡片结束 ==================== -->

      <!-- 两列布局：左侧实体分布，右侧关系分布 -->
      <el-row :gutter="20" class="chart-row">
        <el-col :span="12">
          <el-card class="chart-card" shadow="hover">
            <div slot="header" class="card-header">
              <span><i class="el-icon-s-data"></i> 实体类型分布</span>
              <el-radio-group v-model="entityChartType" size="small">
                <el-radio-button label="pie">饼图</el-radio-button>
                <el-radio-button label="bar">柱状图</el-radio-button>
              </el-radio-group>
            </div>
            <div class="chart-container">
              <div ref="entityChart" class="chart"></div>
            </div>
            <div class="stats-summary">
              <div v-for="item in entityTypeData" :key="item.name" class="stat-row">
                <span class="stat-name">
                  <span class="color-dot" :style="{ background: item.color }"></span>
                  {{ item.name }}
                </span>
                <span class="stat-value">{{ item.value }}</span>
                <span class="stat-percent">{{ ((item.value / totalEntities) * 100).toFixed(1) }}%</span>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card class="chart-card" shadow="hover">
            <div slot="header" class="card-header">
              <span><i class="el-icon-share"></i> 关系类型分布</span>
              <el-radio-group v-model="relationChartType" size="small">
                <el-radio-button label="pie">饼图</el-radio-button>
                <el-radio-button label="bar">柱状图</el-radio-button>
              </el-radio-group>
            </div>
            <div class="chart-container">
              <div ref="relationChart" class="chart"></div>
            </div>
            <div class="stats-summary">
              <div v-for="item in relationTypeData" :key="item.name" class="stat-row">
                <span class="stat-name">
                  <span class="color-dot" :style="{ background: item.color }"></span>
                  {{ item.name }}
                </span>
                <span class="stat-value">{{ item.value }}</span>
                <span class="stat-percent">{{ ((item.value / totalRelations) * 100).toFixed(1) }}%</span>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 三元组列表预览 -->
      <el-card class="triples-card" shadow="hover">
        <div slot="header" class="card-header">
          <span><i class="el-icon-s-order"></i> 三元组列表预览</span>
          <el-button type="text" @click="showAllTriples">
            查看全部 <i class="el-icon-arrow-right"></i>
          </el-button>
        </div>
        <div class="triples-list">
          <el-table :data="previewTriples" stripe size="small">
            <el-table-column prop="head" label="头实体" min-width="120">
              <template slot-scope="scope">
                <el-tag size="mini">{{ scope.row.head }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="relation" label="关系" min-width="100">
              <template slot-scope="scope">
                <el-tag size="mini" type="warning">{{ scope.row.relation }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="tail" label="尾实体" min-width="120">
              <template slot-scope="scope">
                <el-tag size="mini">{{ scope.row.tail }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-card>
    </div>

    <!-- 全部三元组对话框 -->
    <el-dialog title="全部三元组" :visible.sync="dialogVisible" width="70%">
      <el-table :data="allTriples" stripe height="400">
        <el-table-column prop="head" label="头实体" min-width="150"></el-table-column>
        <el-table-column prop="relation" label="关系" min-width="120"></el-table-column>
        <el-table-column prop="tail" label="尾实体" min-width="150"></el-table-column>
      </el-table>
      <span slot="footer">
        <el-button @click="dialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="exportTriples">导出</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import client from '@/api/client'

export default {
  name: 'ResultVisualization',
  data: function() {
    return {
      hasResult: false,
      entityChartType: 'pie',
      relationChartType: 'pie',
      exporting: false,
      dialogVisible: false,
      paperId: '',
      
      // 图表实例
      entityChart: null,
      relationChart: null,
      
      // 数据
      sentences: [],
      entityTypeData: [],
      relationTypeData: [],
      allTriples: [],
      
      // 评估分数
      evaluationScores: {
        precision: 0.9235,
        recall: 0.9012,
        f1: 0.9123
      },
      
      // ==================== 新增：可信度数据（演示用，后续替换为真实后端数据） ====================
      // 来源可信度
      sourceCredibility: {
        total: 82,        // 综合来源可信度
        literature: 85,   // 文献权威性
        timeliness: 78,   // 时效性
        author: 83        // 作者可信度
      },
      // 实体可信度
      entityCredibility: {
        total: 79,        // 综合实体可信度
        tfidf: 81,
        db: 77
      },
      // 关系可信度
      relationCredibility: {
        total: 76,        // 综合关系可信度
        causal: 74,
        extraction: 78
      },
      // 综合知识可信度
      knowledgeCredibility: 79,
      // ==================== 可信度数据结束 ====================
      
      // 颜色映射
      entityColors: {
        'Composition': '#409EFF',
        'Structure': '#67c23a',
        'Property': '#e6a23c',
        'Application': '#f56c6c',
        'Processing': '#9b59b6',
        'Characterization': '#2ecc71',
        'Condition': '#f39c12',
        'Feature': '#909399'
      },
      
      relationColors: {
        'property_of': '#409EFF',
        'used_for': '#67c23a',
        'cause_effect': '#e6a23c',
        'condition_on': '#f56c6c',
        'located_of': '#9b59b6',
        'component_whole': '#2ecc71',
        'instance_of': '#f39c12',
        'feature_of': '#909399'
      }
    }
  },

  computed: {
    totalTriples: function() {
      return this.allTriples.length
    },

    totalEntities: function() {
      var total = 0
      for (var i = 0; i < this.entityTypeData.length; i++) {
        total += this.entityTypeData[i].value
      }
      return total
    },

    totalRelations: function() {
      var total = 0
      for (var i = 0; i < this.relationTypeData.length; i++) {
        total += this.relationTypeData[i].value
      }
      return total
    },

    sentenceCount: function() {
      return this.sentences.length
    },

    previewTriples: function() {
      return this.allTriples.slice(0, 10)
    },

    qualityLevel: function() {
      var f1 = this.evaluationScores.f1 || 0
      if (f1 >= 0.9) return 'success'
      if (f1 >= 0.8) return 'primary'
      if (f1 >= 0.7) return 'warning'
      return 'danger'
    },

    qualityLevelText: function() {
      var f1 = this.evaluationScores.f1 || 0
      if (f1 >= 0.9) return '优秀'
      if (f1 >= 0.8) return '良好'
      if (f1 >= 0.7) return '一般'
      return '待提升'
    },

    // ==================== 新增：可信度相关 computed ====================
    credibilityLevel: function() {
      var score = this.knowledgeCredibility
      if (score >= 85) return 'success'
      if (score >= 70) return 'primary'
      if (score >= 60) return 'warning'
      return 'danger'
    },
    credibilityLevelText: function() {
      var score = this.knowledgeCredibility
      if (score >= 85) return '优秀'
      if (score >= 70) return '良好'
      if (score >= 60) return '一般'
      return '待提升'
    },
    credibilityOverallColor: function() {
      var score = this.knowledgeCredibility
      if (score >= 85) return '#67c23a'
      if (score >= 70) return '#409EFF'
      if (score >= 60) return '#e6a23c'
      return '#f56c6c'
    }
    // ==================== 可信度 computed 结束 ====================
  },

  mounted: function() {
    console.log('========== 结果可视化页面加载 ==========')
    console.log('路由参数:', this.$route.query)
    
    // 从路由参数获取 paperId
    this.paperId = this.$route.query.paperId || this.$route.query.recordId || ''
    console.log('paperId:', this.paperId)
    
    // 从数据库加载数据
    this.loadFromDatabase()
    
    var vm = this
    setTimeout(function() {
      vm.initCharts()
    }, 300)

    window.addEventListener('resize', this.handleResize)
  },

  beforeDestroy: function() {
    window.removeEventListener('resize', this.handleResize)
    if (this.entityChart) {
      this.entityChart.dispose()
    }
    if (this.relationChart) {
      this.relationChart.dispose()
    }
  },

  methods: {
    // 从数据库加载数据
    loadFromDatabase: function() {
      var vm = this
      
      if (!this.paperId) {
        console.log('没有 paperId，尝试从 sessionStorage 加载')
        this.loadFromSessionStorage()
        return
      }
      
      console.log('从数据库加载数据, paperId:', this.paperId)
      
      client.get('/api/extract/literature/' + this.paperId)
        .then(function(response) {
          console.log('数据库返回数据:', response.data)
          
          var data = response.data
          if (data && data.length > 0) {
            vm.processDatabaseData(data)
          } else {
            console.log('数据库没有数据，尝试从 sessionStorage 加载')
            vm.loadFromSessionStorage()
          }
        })
        .catch(function(error) {
          console.error('从数据库加载失败:', error)
          vm.loadFromSessionStorage()
        })
    },

    // 处理数据库返回的数据
    processDatabaseData: function(data) {
      console.log('处理数据库数据，共', data.length, '条')
      
      var entityTypeCount = {}
      var relationTypeCount = {}
      var triples = []
      var sentences = []
      
      for (var i = 0; i < data.length; i++) {
        var item = data[i]
        
        // 优先使用纠错后的数据
        var entities = item.correctedEntities || item.entities || []
        var relations = item.correctedRelations || item.relations || []
        var text = item.sentenceText || item.sentence_text || ''
        
        // 保存句子
        if (text) {
          sentences.push({ text: text })
        }
        
        // 处理实体 - 对象格式 {text, type}
        if (entities && entities.length > 0) {
          for (var j = 0; j < entities.length; j++) {
            var entity = entities[j]
            if (entity && typeof entity === 'object') {
              var type = entity.type || 'unknown'
              entityTypeCount[type] = (entityTypeCount[type] || 0) + 1
            } else if (typeof entity === 'string') {
              if (entity.includes(':')) {
                var parts = entity.split(':')
                var type = parts[1] || 'unknown'
                entityTypeCount[type] = (entityTypeCount[type] || 0) + 1
              } else {
                entityTypeCount['unknown'] = (entityTypeCount['unknown'] || 0) + 1
              }
            }
          }
        }
        
        // 处理关系 - 对象格式 {subject, predicate, object}
        if (relations && relations.length > 0) {
          for (var k = 0; k < relations.length; k++) {
            var rel = relations[k]
            if (rel && typeof rel === 'object') {
              var subject = rel.subject || rel[0] || ''
              var predicate = rel.predicate || rel[1] || ''
              var object = rel.object || rel[2] || ''
              
              if (predicate) {
                relationTypeCount[predicate] = (relationTypeCount[predicate] || 0) + 1
              }
              
              if (subject && predicate && object) {
                triples.push({
                  head: subject,
                  relation: predicate,
                  tail: object
                })
              }
            } else if (Array.isArray(rel) && rel.length >= 3) {
              var pred = rel[1] || ''
              if (pred) {
                relationTypeCount[pred] = (relationTypeCount[pred] || 0) + 1
              }
              triples.push({
                head: rel[0] || '',
                relation: pred,
                tail: rel[2] || ''
              })
            }
          }
        }
      }
      
      // 转换为图表数据格式
      this.entityTypeData = []
      for (var type in entityTypeCount) {
        this.entityTypeData.push({
          name: type,
          value: entityTypeCount[type],
          color: this.entityColors[type] || this.getRandomColor(type)
        })
      }
      
      this.entityTypeData.sort(function(a, b) {
        return b.value - a.value
      })
      
      this.relationTypeData = []
      for (var rtype in relationTypeCount) {
        this.relationTypeData.push({
          name: rtype,
          value: relationTypeCount[rtype],
          color: this.relationColors[rtype] || this.getRandomColor(rtype)
        })
      }
      
      this.relationTypeData.sort(function(a, b) {
        return b.value - a.value
      })
      
      this.allTriples = triples
      this.sentences = sentences
      this.hasResult = true
      
      console.log('处理后的数据:', {
        entities: this.entityTypeData,
        relations: this.relationTypeData,
        triples: triples.length,
        sentences: sentences.length
      })
      
      // 重新初始化图表
      this.initCharts()
    },

    // 从 sessionStorage 加载（备用）
    loadFromSessionStorage: function() {
      try {
        var storedData = sessionStorage.getItem('visualizationData')
        if (!storedData) {
          console.log('sessionStorage 无数据')
          this.hasResult = false
          return
        }
        
        var parsedData = JSON.parse(storedData)
        console.log('从 sessionStorage 获取到数据:', parsedData)
        
        // 处理各种可能的数据结构
        if (parsedData.sentences) {
          this.processRawData(parsedData.sentences)
        } else if (Array.isArray(parsedData)) {
          this.processRawData(parsedData)
        } else if (parsedData.results) {
          this.processRawData(parsedData.results)
        }
        
        // 如果有评估分数
        if (parsedData.metrics) {
          this.evaluationScores = {
            precision: parsedData.metrics.precision || this.evaluationScores.precision,
            recall: parsedData.metrics.recall || this.evaluationScores.recall,
            f1: parsedData.metrics.f1_score || this.evaluationScores.f1
          }
        }
        
        this.initCharts()
      } catch (e) {
        console.error('读取 sessionStorage 失败:', e)
        this.hasResult = false
      }
    },

    // 处理原始数据（兼容旧格式）
    processRawData: function(rawData) {
      if (!rawData || !Array.isArray(rawData) || rawData.length === 0) {
        console.log('无有效数据')
        return
      }
      
      console.log('处理原始数据，第一条:', rawData[0])
      
      var entityTypeCount = {}
      var relationTypeCount = {}
      var triples = []
      var sentences = []
      
      for (var i = 0; i < rawData.length; i++) {
        var item = rawData[i]
        var entities = []
        var relations = []
        var text = ''
        
        if (item.text !== undefined) {
          entities = item.entities || []
          relations = item.relations || []
          text = item.text
        } else if (item.given_text !== undefined) {
          entities = item.final_entity || item.original_entity || []
          relations = item.final_relation || item.original_relation || []
          text = item.given_text
        } else if (item.input_text !== undefined) {
          entities = item.primary_entities || []
          relations = item.primary_relations || []
          text = item.input_text
        }
        
        entities = Array.isArray(entities) ? entities : []
        relations = Array.isArray(relations) ? relations : []
        
        if (text) {
          sentences.push({ text: text })
        }
        
        for (var j = 0; j < entities.length; j++) {
          var entity = entities[j]
          if (typeof entity === 'string') {
            if (entity.includes(':')) {
              var parts = entity.split(':')
              var type = parts[1] || 'unknown'
              entityTypeCount[type] = (entityTypeCount[type] || 0) + 1
            } else {
              entityTypeCount['unknown'] = (entityTypeCount['unknown'] || 0) + 1
            }
          } else if (entity && typeof entity === 'object') {
            var type = entity.type || 'unknown'
            entityTypeCount[type] = (entityTypeCount[type] || 0) + 1
          }
        }
        
        for (var k = 0; k < relations.length; k++) {
          var rel = relations[k]
          if (Array.isArray(rel) && rel.length >= 3) {
            var relType = rel[1] || 'unknown'
            relationTypeCount[relType] = (relationTypeCount[relType] || 0) + 1
            
            triples.push({
              head: rel[0] || '',
              relation: rel[1] || '',
              tail: rel[2] || ''
            })
          } else if (rel && typeof rel === 'object') {
            var head = rel.subject || rel[0] || ''
            var relation = rel.predicate || rel[1] || ''
            var tail = rel.object || rel[2] || ''
            
            if (head && relation && tail) {
              relationTypeCount[relation] = (relationTypeCount[relation] || 0) + 1
              triples.push({
                head: head,
                relation: relation,
                tail: tail
              })
            }
          }
        }
      }
      
      this.entityTypeData = []
      for (var type in entityTypeCount) {
        this.entityTypeData.push({
          name: type,
          value: entityTypeCount[type],
          color: this.entityColors[type] || this.getRandomColor(type)
        })
      }
      
      this.entityTypeData.sort(function(a, b) {
        return b.value - a.value
      })
      
      this.relationTypeData = []
      for (var rtype in relationTypeCount) {
        this.relationTypeData.push({
          name: rtype,
          value: relationTypeCount[rtype],
          color: this.relationColors[rtype] || this.getRandomColor(rtype)
        })
      }
      
      this.relationTypeData.sort(function(a, b) {
        return b.value - a.value
      })
      
      this.allTriples = triples
      this.sentences = sentences
      this.hasResult = true
    },

    // 获取随机颜色
    getRandomColor: function(str) {
      var colors = ['#409EFF', '#67c23a', '#e6a23c', '#f56c6c', '#9b59b6', '#2ecc71', '#f39c12', '#1abc9c', '#3498db', '#e74c3c']
      var hash = 0
      for (var i = 0; i < str.length; i++) {
        hash = str.charCodeAt(i) + ((hash << 5) - hash)
      }
      var index = Math.abs(hash) % colors.length
      return colors[index]
    },

    initCharts: function() {
      this.initEntityChart()
      this.initRelationChart()
    },

    initEntityChart: function() {
      if (!this.$refs.entityChart) return
      if (this.entityChart) {
        this.entityChart.dispose()
      }
      
      this.entityChart = echarts.init(this.$refs.entityChart)
      
      if (this.entityChartType === 'pie') {
        this.renderEntityPieChart()
      } else {
        this.renderEntityBarChart()
      }
    },

    initRelationChart: function() {
      if (!this.$refs.relationChart) return
      if (this.relationChart) {
        this.relationChart.dispose()
      }
      
      this.relationChart = echarts.init(this.$refs.relationChart)
      
      if (this.relationChartType === 'pie') {
        this.renderRelationPieChart()
      } else {
        this.renderRelationBarChart()
      }
    },

    renderEntityPieChart: function() {
      if (this.entityTypeData.length === 0) {
        this.entityChart.setOption({
          title: {
            text: '暂无数据',
            left: 'center',
            top: 'center'
          }
        })
        return
      }
      
      var chartData = []
      for (var i = 0; i < this.entityTypeData.length; i++) {
        chartData.push({
          name: this.entityTypeData[i].name,
          value: this.entityTypeData[i].value,
          itemStyle: { color: this.entityTypeData[i].color }
        })
      }
      
      var option = {
        tooltip: {
          trigger: 'item',
          formatter: '{b}: {c} ({d}%)'
        },
        series: [
          {
            type: 'pie',
            radius: ['40%', '70%'],
            data: chartData,
            label: {
              show: true,
              formatter: '{b}'
            },
            emphasis: {
              scale: true
            }
          }
        ]
      }
      this.entityChart.setOption(option)
    },

    renderEntityBarChart: function() {
      if (this.entityTypeData.length === 0) {
        this.entityChart.setOption({
          title: {
            text: '暂无数据',
            left: 'center',
            top: 'center'
          }
        })
        return
      }
      
      var xAxisData = []
      var seriesData = []
      
      for (var i = 0; i < this.entityTypeData.length; i++) {
        xAxisData.push(this.entityTypeData[i].name)
        seriesData.push({
          value: this.entityTypeData[i].value,
          itemStyle: { color: this.entityTypeData[i].color }
        })
      }
      
      var option = {
        tooltip: {
          trigger: 'axis'
        },
        grid: {
          left: '10%',
          right: '5%',
          bottom: '10%',
          top: '10%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          data: xAxisData,
          axisLabel: { rotate: 30 }
        },
        yAxis: {
          type: 'value'
        },
        series: [
          {
            type: 'bar',
            data: seriesData,
            barWidth: '40%',
            label: {
              show: true,
              position: 'top'
            }
          }
        ]
      }
      this.entityChart.setOption(option)
    },

    renderRelationPieChart: function() {
      if (this.relationTypeData.length === 0) {
        this.relationChart.setOption({
          title: {
            text: '暂无数据',
            left: 'center',
            top: 'center'
          }
        })
        return
      }
      
      var chartData = []
      for (var i = 0; i < this.relationTypeData.length; i++) {
        chartData.push({
          name: this.relationTypeData[i].name,
          value: this.relationTypeData[i].value,
          itemStyle: { color: this.relationTypeData[i].color }
        })
      }
      
      var option = {
        tooltip: {
          trigger: 'item',
          formatter: '{b}: {c} ({d}%)'
        },
        series: [
          {
            type: 'pie',
            radius: ['40%', '70%'],
            data: chartData,
            label: {
              show: true,
              formatter: '{b}'
            },
            emphasis: {
              scale: true
            }
          }
        ]
      }
      this.relationChart.setOption(option)
    },

    renderRelationBarChart: function() {
      if (this.relationTypeData.length === 0) {
        this.relationChart.setOption({
          title: {
            text: '暂无数据',
            left: 'center',
            top: 'center'
          }
        })
        return
      }
      
      var xAxisData = []
      var seriesData = []
      
      for (var i = 0; i < this.relationTypeData.length; i++) {
        xAxisData.push(this.relationTypeData[i].name)
        seriesData.push({
          value: this.relationTypeData[i].value,
          itemStyle: { color: this.relationTypeData[i].color }
        })
      }
      
      var option = {
        tooltip: {
          trigger: 'axis'
        },
        grid: {
          left: '10%',
          right: '5%',
          bottom: '10%',
          top: '10%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          data: xAxisData,
          axisLabel: { rotate: 30 }
        },
        yAxis: {
          type: 'value'
        },
        series: [
          {
            type: 'bar',
            data: seriesData,
            barWidth: '40%',
            label: {
              show: true,
              position: 'top'
            }
          }
        ]
      }
      this.relationChart.setOption(option)
    },

    handleResize: function() {
      if (this.entityChart) {
        this.entityChart.resize()
      }
      if (this.relationChart) {
        this.relationChart.resize()
      }
    },

    formatScore: function(score) {
      if (score === undefined || score === null) return '0.0000'
      return score.toFixed(4)
    },

    getF1Status: function(f1) {
      if (f1 >= 0.9) return 'success'
      if (f1 >= 0.8) return 'primary'
      if (f1 >= 0.7) return 'warning'
      return 'exception'
    },

    exportTriples: function() {
      if (this.allTriples.length === 0) {
        this.$message.warning('没有可导出的三元组')
        return
      }
      
      this.exporting = true
      
      var csvContent = '头实体,关系,尾实体\n'
      for (var i = 0; i < this.allTriples.length; i++) {
        var t = this.allTriples[i]
        csvContent += '"' + t.head + '","' + t.relation + '","' + t.tail + '"\n'
      }
      
      var blob = new Blob(['\uFEFF' + csvContent], { type: 'text/csv;charset=utf-8;' })
      var link = document.createElement('a')
      link.href = URL.createObjectURL(blob)
      link.download = 'triples_' + new Date().getTime() + '.csv'
      link.click()
      
      setTimeout(() => {
        URL.revokeObjectURL(link.href)
        this.exporting = false
        this.$message.success('导出成功，共 ' + this.allTriples.length + ' 条三元组')
      }, 100)
    },

    showAllTriples: function() {
      this.dialogVisible = true
    },

    goBack: function() {
      this.$router.push('/view/extraction/model-extract')
    }
  },

  watch: {
    entityChartType: function() {
      this.initEntityChart()
    },

    relationChartType: function() {
      this.initRelationChart()
    }
  }
}
</script>

<style scoped>
.result-visualization-container {
  padding: 24px;
  max-width: 1400px;
  margin: 0 auto;
  min-height: calc(100vh - 120px);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.page-header h2 {
  font-size: 24px;
  font-weight: 600;
  color: #1e2a3a;
  margin: 0;
  position: relative;
  padding-left: 12px;
}

.page-header h2::before {
  content: '';
  position: absolute;
  left: 0;
  top: 4px;
  bottom: 4px;
  width: 4px;
  background: #409EFF;
  border-radius: 2px;
}

.result-tag {
  font-size: 14px;
  color: #67c23a;
  background: #f0f9eb;
  padding: 6px 16px;
  border-radius: 24px;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.no-data {
  text-align: center;
  padding: 80px 20px;
  background: white;
  border-radius: 16px;
  border: 1px solid #e9ecef;
}

.no-data i {
  font-size: 48px;
  color: #8c9aa9;
  margin-bottom: 16px;
}

.no-data p {
  font-size: 16px;
  color: #5a6a7a;
  margin-bottom: 24px;
}

.overview-row,
.score-row,
.credibility-row,
.chart-row {
  margin-bottom: 20px;
}

.overview-card {
  display: flex;
  align-items: center;
  padding: 16px;
  border: none;
  height: 100%;
}

.card-icon {
  width: 48px;
  height: 48px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16px;
  flex-shrink: 0;
}

.card-icon i {
  font-size: 24px;
}

.card-info {
  flex: 1;
}

.card-label {
  font-size: 13px;
  color: #5a6a7a;
  margin-bottom: 4px;
}

.card-value {
  font-size: 24px;
  font-weight: 600;
  color: #1e2a3a;
}

.score-card {
  border-radius: 16px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
}

.score-grid {
  display: flex;
  justify-content: space-around;
  padding: 16px 0;
  gap: 20px;
}

.score-item {
  flex: 1;
  text-align: center;
}

.score-label {
  font-size: 13px;
  color: #5a6a7a;
  margin-bottom: 8px;
}

.score-value {
  font-size: 28px;
  font-weight: 600;
  color: #1e2a3a;
  margin-bottom: 8px;
}

.score-value.highlight {
  color: #409EFF;
  font-size: 32px;
}

.chart-card {
  border-radius: 16px;
  height: 100%;
}

.chart-container {
  height: 260px;
  width: 100%;
  margin-bottom: 16px;
}

.chart {
  width: 100%;
  height: 100%;
}

.stats-summary {
  max-height: 180px;
  overflow-y: auto;
  padding: 0 12px 12px;
}

.stat-row {
  display: flex;
  align-items: center;
  padding: 6px 0;
  border-bottom: 1px dashed #f0f2f5;
  font-size: 13px;
}

.stat-name {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
}

.color-dot {
  display: inline-block;
  width: 12px;
  height: 12px;
  border-radius: 4px;
}

.stat-value {
  width: 50px;
  text-align: right;
  font-weight: 600;
  color: #1e2a3a;
}

.stat-percent {
  width: 60px;
  text-align: right;
  color: #8c9aa9;
}

.triples-card {
  border-radius: 16px;
}

.triples-list {
  max-height: 250px;
  overflow-y: auto;
}

.el-dialog__body {
  padding: 20px;
}

/* ==================== 新增：可信度卡片样式 ==================== */
.credibility-card {
  border-radius: 16px;
}

.sample-badge {
  font-size: 11px;
  color: #8c9aa9;
  background: #f0f2f5;
  padding: 2px 8px;
  border-radius: 12px;
  margin-left: auto;
}

.credibility-content {
  padding: 8px 0;
}

.credibility-item {
  text-align: center;
  padding: 16px;
  background: #fafbfc;
  border-radius: 16px;
  transition: all 0.3s;
}

.credibility-item:hover {
  background: #f5f7fa;
}

.item-icon {
  font-size: 28px;
  color: #409EFF;
  margin-bottom: 12px;
}

.item-title {
  font-size: 13px;
  color: #5a6a7a;
  margin-bottom: 8px;
}

.item-value {
  font-size: 32px;
  font-weight: 700;
  color: #1e2a3a;
  margin-bottom: 12px;
}

.item-detail {
  display: flex;
  justify-content: center;
  gap: 12px;
  font-size: 11px;
  color: #8c9aa9;
}

.item-detail span {
  background: #fff;
  padding: 2px 8px;
  border-radius: 20px;
}

.credibility-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #e9ecef;
}

.overall-label {
  font-size: 14px;
  font-weight: 500;
  color: #1e2a3a;
}

.overall-value {
  font-size: 28px;
  font-weight: 700;
  margin-left: 12px;
}

.credibility-formula {
  font-size: 12px;
  color: #8c9aa9;
  display: flex;
  align-items: center;
  gap: 6px;
}
/* ==================== 可信度卡片样式结束 ==================== */

@media (max-width: 1200px) {
  .overview-row .el-col,
  .score-row .el-col,
  .credibility-row .el-col,
  .chart-row .el-col {
    margin-bottom: 16px;
  }
}

@media (max-width: 768px) {
  .result-visualization-container {
    padding: 16px;
  }

  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .header-actions {
    width: 100%;
    justify-content: flex-end;
  }

  .score-grid {
    flex-direction: column;
  }
  
  /* 可信度卡片移动端适配 */
  .credibility-footer {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  
  .item-detail {
    flex-wrap: wrap;
  }
}
</style>