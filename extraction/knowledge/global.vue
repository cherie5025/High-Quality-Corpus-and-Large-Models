<template>
  <div class="global-analysis">
    <!-- 页面头部 -->
    <div class="analysis-header">
      <div class="header-title">
        <h2>全局分析</h2>
      </div>
      <div class="header-controls">
        <el-select 
          v-model="selectedPaperId" 
          placeholder="请选择文献"
          size="small"
          style="width: 260px;"
          @change="onPaperChange"
        >
          <el-option
            v-for="item in paperList"
            :key="item.literatureId"
            :label="item.title"
            :value="item.literatureId"
          >
            <span style="float: left">{{ item.title }}</span>
            <span style="float: right; color: #8492a6; font-size: 12px">{{ item.year || item.date }}</span>
          </el-option>
        </el-select>
        <el-button type="primary" @click="loadGraphData" size="small">
          展示
        </el-button>
      </div>
    </div>

    <!-- 主要内容区域 -->
    <div class="analysis-main">
      <!-- 左侧标签栏 -->
      <div class="sidebar-tabs">
        <div 
          class="tab-btn" 
          :class="{ active: activeTab === 'graph' }"
          @click="setActiveTab('graph')"
        >
          <i class="el-icon-share"></i>
          <span>图谱</span>
        </div>
        <div 
          class="tab-btn" 
          :class="{ active: activeTab === 'data' }"
          @click="setActiveTab('data')"
        >
          <i class="el-icon-data-line"></i>
          <span>数据</span>
        </div>
      </div>

      <!-- 右侧内容区域 -->
      <div class="content-panel">
        <!-- 图谱模式 - 使用 v-show 而不是 v-if，确保DOM一直存在 -->
        <div v-show="activeTab === 'graph'" class="graph-mode">
          <div v-if="hasData" class="graph-container-wrapper">
            <div id="graph-container" class="graph-container"></div>
            
            <div class="graph-controls">
              <el-tooltip content="放大" placement="left">
                <el-button circle size="small" icon="el-icon-zoom-in" @click="zoomIn"></el-button>
              </el-tooltip>
              <el-tooltip content="缩小" placement="left">
                <el-button circle size="small" icon="el-icon-zoom-out" @click="zoomOut"></el-button>
              </el-tooltip>
              <el-tooltip content="适应屏幕" placement="left">
                <el-button circle size="small" icon="el-icon-rank" @click="fitView"></el-button>
              </el-tooltip>
            </div>

            <div class="filter-card">
              <div class="filter-header">
                <span><i class="el-icon-s-operation"></i> 关系筛选</span>
                <el-button 
                  v-if="selectedRelations.length > 0" 
                  type="text" 
                  size="small" 
                  @click="clearFilter"
                >
                  清除
                </el-button>
              </div>
              <el-select 
                v-model="selectedRelations" 
                multiple
                collapse-tags
                placeholder="选择关系类型"
                size="small"
                style="width: 100%"
                @change="applyFilter"
              >
                <el-option
                  v-for="type in relationTypeList"
                  :key="type"
                  :label="type"
                  :value="type"
                />
              </el-select>
            </div>
          </div>
          <div v-else class="empty-state">
            <i class="el-icon-info"></i>
            <p>请选择文献后点击"展示"</p>
          </div>
        </div>

        <!-- 数据模式 -->
        <div v-show="activeTab === 'data'" class="data-mode">
          <div v-if="hasData" class="data-container">
            <div class="metrics-cards">
              <div class="metric-card">
                <div class="metric-value">{{ stats.nodeCount }}</div>
                <div class="metric-label">节点总数</div>
              </div>
              <div class="metric-card">
                <div class="metric-value">{{ stats.relationCount }}</div>
                <div class="metric-label">关系总数</div>
              </div>
              <div class="metric-card">
                <div class="metric-value">{{ stats.avgDegree }}</div>
                <div class="metric-label">平均度数</div>
              </div>
              <div class="metric-card">
                <div class="metric-value">{{ stats.graphDensity }}</div>
                <div class="metric-label">图密度</div>
              </div>
            </div>

            <div class="importance-section">
              <div class="section-title">
                <i class="el-icon-star-on"></i> 节点重要性指标 (Top 10)
              </div>
              <div class="importance-table">
                <table>
                  <thead>
                    <tr>
                      <th>排名</th>
                      <th>节点名称</th>
                      <th>类型</th>
                      <th>PageRank</th>
                      <th>中介中心度</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="(node, idx) in topImportantNodes" :key="node.id">
                      <td>{{ idx + 1 }}</td>
                      <td class="node-name">{{ node.name }}</td>
                      <td>
                        <el-tag size="mini" :type="getNodeTypeTag(node.type)">
                          {{ node.type }}
                        </el-tag>
                      </td>
                      <td>{{ node.pageRank ? node.pageRank.toFixed(4) : '-' }}</td>
                      <td>{{ node.betweenness ? node.betweenness.toFixed(4) : '-' }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>

            <div class="charts-section">
              <div class="chart-card">
                <div class="chart-title">
                  <i class="el-icon-pie-chart"></i> 实体类型分布
                </div>
                <div id="entity-chart" style="height: 280px;"></div>
              </div>
              <div class="chart-card">
                <div class="chart-title">
                  <i class="el-icon-data-line"></i> 关系类型分布
                </div>
                <div id="relation-chart" style="height: 280px;"></div>
              </div>
            </div>
          </div>
          <div v-else class="empty-state">
            <i class="el-icon-info"></i>
            <p>请先选择文献并点击"展示"</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 节点详情弹窗卡片 -->
    <div class="detail-card" v-if="showNodeDetail" @click.stop>
      <div class="detail-header">
        <span><i class="el-icon-info"></i> 节点信息</span>
        <el-button type="text" @click="showNodeDetail = false" icon="el-icon-close"></el-button>
      </div>
      <div class="detail-body">
        <div class="detail-row">
          <span class="detail-label">节点名称</span>
          <span class="detail-value">{{ currentNodeDetail.name }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">节点类型</span>
          <el-tag size="mini" :type="getNodeTypeTag(currentNodeDetail.type)">
            {{ currentNodeDetail.type }}
          </el-tag>
        </div>
        <div class="detail-row">
          <span class="detail-label">连接数</span>
          <span class="detail-value">{{ currentNodeDetail.degree }}</span>
        </div>
        <div class="detail-row" v-if="currentNodeDetail.sentences && currentNodeDetail.sentences.length">
          <span class="detail-label">所属语句</span>
          <div class="sentences-list">
            <div v-for="(sentence, idx) in currentNodeDetail.sentences" :key="idx" class="sentence-item">
              {{ sentence }}
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 关系详情弹窗卡片 -->
    <div class="detail-card" v-if="showEdgeDetail" @click.stop>
      <div class="detail-header">
        <span><i class="el-icon-share"></i> 关系信息</span>
        <el-button type="text" @click="showEdgeDetail = false" icon="el-icon-close"></el-button>
      </div>
      <div class="detail-body">
        <div class="detail-row">
          <span class="detail-label">关系类型</span>
          <el-tag size="mini" type="warning">{{ currentEdgeDetail.relation }}</el-tag>
        </div>
        <div class="detail-row">
          <span class="detail-label">头实体</span>
          <span class="detail-value">{{ currentEdgeDetail.sourceNode }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">尾实体</span>
          <span class="detail-value">{{ currentEdgeDetail.targetNode }}</span>
        </div>
        <div class="detail-row" v-if="currentEdgeDetail.sentences && currentEdgeDetail.sentences.length">
          <span class="detail-label">所属语句</span>
          <div class="sentences-list">
            <div v-for="(sentence, idx) in currentEdgeDetail.sentences" :key="idx" class="sentence-item">
              {{ sentence }}
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import G6 from '@antv/g6'
import * as echarts from 'echarts'
import client from '@/api/client'

export default {
  name: 'GlobalAnalysis',
  data() {
    return {
      graph: null,
      paperList: [],
      selectedPaperId: '',
      hasData: false,
      activeTab: 'graph',
      showNodeDetail: false,
      showEdgeDetail: false,
      currentNodeDetail: {},
      currentEdgeDetail: {},
      selectedRelations: [],
      originalGraphData: { nodes: [], edges: [] },
      currentGraphData: { nodes: [], edges: [] },
      relationTypeList: [],
      entityTypeCount: {},
      relationTypeCount: {},
      entityChart: null,
      relationChart: null,
      stats: {
        nodeCount: 0,
        relationCount: 0,
        avgDegree: 0,
        graphDensity: 0
      },
      nodeColors: {
        'Composition': '#5B8FF9',
        'Structure': '#5AD8A6',
        'Property': '#F6BD16',
        'Application': '#E8684A',
        'Processing': '#6DC8F3',
        'Characterization': '#9270CA',
        'Condition': '#FF99C3',
        'Feature': '#269A99',
        'unknown': '#B8C5D0'
      }
    }
  },
  
  computed: {
    topImportantNodes() {
      if (!this.originalGraphData.nodes || this.originalGraphData.nodes.length === 0) return []
      var sorted = [...this.originalGraphData.nodes]
      sorted.sort(function(a, b) {
        return (b.pageRank || 0) - (a.pageRank || 0)
      })
      return sorted.slice(0, 10)
    }
  },

  mounted() {
    // 先注册自定义节点
    this.registerCustomNode()
    // 等待DOM完全渲染后再初始化图
    this.$nextTick(() => {
      this.initGraph()
    })
    this.fetchPaperList()
    window.addEventListener('resize', this.handleResize)
  },

  beforeDestroy() {
    if (this.graph) {
      this.graph.destroy()
    }
    if (this.entityChart) {
      this.entityChart.dispose()
    }
    if (this.relationChart) {
      this.relationChart.dispose()
    }
    window.removeEventListener('resize', this.handleResize)
  },

  methods: {
    // 注册自定义节点样式
    registerCustomNode() {
      G6.registerNode('circle-with-text', {
        draw: function(cfg, group) {
          var size = cfg.size || 70
          var color = cfg.style.fill || '#5B8FF9'
          var text = cfg.label || ''
          
          var keyShape = group.addShape('circle', {
            attrs: {
              x: 0,
              y: 0,
              r: size / 2,
              fill: color,
              stroke: '#ffffff',
              lineWidth: 2,
              cursor: 'pointer',
              shadowColor: 'rgba(0,0,0,0.1)',
              shadowBlur: 10
            },
            name: 'circle-shape'
          })
          
          // 处理文字换行
          var maxWidth = size - 16
          var fontSize = 11
          var lines = []
          var currentLine = ''
          
          for (var i = 0; i < text.length; i++) {
            var testLine = currentLine + text[i]
            var testWidth = testLine.length * fontSize * 0.6
            if (testWidth > maxWidth && currentLine.length > 0) {
              lines.push(currentLine)
              currentLine = text[i]
            } else {
              currentLine = testLine
            }
          }
          if (currentLine) {
            lines.push(currentLine)
          }
          
          if (lines.length > 2) {
            fontSize = 10
          }
          if (lines.length > 3) {
            fontSize = 9
          }
          if (lines.length > 4) {
            fontSize = 8
          }
          
          var lineHeight = fontSize + 2
          var startY = -((lines.length - 1) * lineHeight) / 2
          
          for (var j = 0; j < lines.length; j++) {
            group.addShape('text', {
              attrs: {
                x: 0,
                y: startY + j * lineHeight,
                text: lines[j],
                fill: '#ffffff',
                fontSize: fontSize,
                fontWeight: 'bold',
                textAlign: 'center',
                textBaseline: 'middle'
              },
              name: 'text-shape'
            })
          }
          
          return keyShape
        },
        getAnchorPoints: function() {
          return [
            [0, 0.5],
            [1, 0.5],
            [0.5, 0],
            [0.5, 1]
          ]
        }
      }, 'circle')
    },
    
    setActiveTab(tab) {
      this.activeTab = tab
      if (tab === 'data' && this.hasData) {
        this.$nextTick(() => {
          this.initCharts()
        })
      }
      // 切换到图谱标签时，如果图未初始化则初始化
      if (tab === 'graph' && !this.graph && this.hasData) {
        this.$nextTick(() => {
          this.initGraph()
          this.renderGraph(this.currentGraphData.nodes, this.currentGraphData.edges)
        })
      }
    },

    initGraph() {
      var container = document.getElementById('graph-container')
      if (!container) {
        console.error('找不到graph-container容器，稍后重试')
        // 延迟重试
        setTimeout(() => {
          if (!this.graph && document.getElementById('graph-container')) {
            this.initGraph()
          }
        }, 100)
        return
      }
      
      console.log('初始化图谱，容器尺寸:', container.clientWidth, container.clientHeight)
      
      if (this.graph) {
        this.graph.destroy()
        this.graph = null
      }
      
      this.graph = new G6.Graph({
        container: 'graph-container',
        width: container.clientWidth,
        height: container.clientHeight,
        layout: {
          type: 'force',
          preventOverlap: true,
          linkDistance: 200,
          nodeStrength: 50,
          edgeStrength: 0.1,
          nodeSpacing: 80
        },
        defaultNode: {
          type: 'circle-with-text',
          size: 70,
          style: {
            fill: '#5B8FF9',
            stroke: '#ffffff',
            lineWidth: 2,
            cursor: 'pointer'
          }
        },
        defaultEdge: {
          type: 'line',
          style: {
            stroke: '#aaa',
            lineWidth: 1.5,
            endArrow: {
              path: G6.Arrow.triangle(6, 8),
              fill: '#aaa'
            }
          },
          labelCfg: {
            autoRotate: true,
            style: {
              fill: '#666',
              fontSize: 10,
              background: {
                fill: '#ffffff',
                padding: [2, 4, 2, 4],
                radius: 4
              }
            }
          }
        },
        modes: {
          default: ['drag-canvas', 'zoom-canvas', 'drag-node']
        }
      })
      
      console.log('图初始化完成')
      
      this.graph.on('node:click', (evt) => {
        var nodeModel = evt.item.getModel()
        var fullNodeData = null
        for (var i = 0; i < this.originalGraphData.nodes.length; i++) {
          if (this.originalGraphData.nodes[i].id === nodeModel.id) {
            fullNodeData = this.originalGraphData.nodes[i]
            break
          }
        }
        this.currentNodeDetail = {
          name: nodeModel.label || nodeModel.name,
          type: nodeModel.type || 'unknown',
          degree: nodeModel.degree || 0,
          pageRank: fullNodeData ? fullNodeData.pageRank : null,
          betweenness: fullNodeData ? fullNodeData.betweenness : null,
          sentences: fullNodeData && fullNodeData.sentences ? fullNodeData.sentences : []
        }
        this.showNodeDetail = true
        this.showEdgeDetail = false
      })
      
      this.graph.on('edge:click', (evt) => {
        var edgeModel = evt.item.getModel()
        var fullEdgeData = null
        for (var i = 0; i < this.originalGraphData.edges.length; i++) {
          var e = this.originalGraphData.edges[i]
          if (e.source === edgeModel.source && e.target === edgeModel.target && e.relation === edgeModel.relation) {
            fullEdgeData = e
            break
          }
        }
        this.currentEdgeDetail = {
          relation: edgeModel.label || edgeModel.relation,
          sourceNode: this.getNodeName(edgeModel.source),
          targetNode: this.getNodeName(edgeModel.target),
          sentences: fullEdgeData && fullEdgeData.sentences ? fullEdgeData.sentences : []
        }
        this.showEdgeDetail = true
        this.showNodeDetail = false
      })
      
      this.graph.on('canvas:click', () => {
        this.showNodeDetail = false
        this.showEdgeDetail = false
      })
    },

    getNodeName: function(nodeId) {
      if (!this.graph) return nodeId
      var nodes = this.graph.getNodes()
      for (var i = 0; i < nodes.length; i++) {
        var node = nodes[i].getModel()
        if (node.id === nodeId) {
          return node.name || node.label || nodeId
        }
      }
      return nodeId
    },

    handleResize() {
      if (this.graph && !this.graph.destroyed) {
        var container = document.getElementById('graph-container')
        if (container) {
          this.graph.changeSize(container.clientWidth, container.clientHeight)
        }
      }
      if (this.entityChart) {
        this.entityChart.resize()
      }
      if (this.relationChart) {
        this.relationChart.resize()
      }
    },

    fetchPaperList: async function() {
      try {
        var response = await client.get('/api/knowledge-graph/literature/list')
        if (response.data && response.data.code === 200) {
          this.paperList = response.data.data
          if (this.paperList.length > 0) {
            this.selectedPaperId = this.paperList[0].literatureId
          }
        }
      } catch (error) {
        console.error('获取文献列表失败:', error)
      }
    },

    onPaperChange() {},

    loadGraphData: async function() {
      if (!this.selectedPaperId) {
        this.$message.warning('请先选择文献')
        return
      }
      
      this.$message.info('正在加载图谱...')
      
      try {
        var response = await client.get('/api/knowledge-graph/literature/' + this.selectedPaperId + '/graph')
        
        if (response.data && response.data.code === 200) {
          var graphData = response.data.data
          console.log('加载到的图谱数据:', graphData)
          
          if (graphData.nodes && graphData.nodes.length > 0) {
            this.calculateNodeMetrics(graphData)
            this.originalGraphData = JSON.parse(JSON.stringify(graphData))
            this.currentGraphData = JSON.parse(JSON.stringify(graphData))
            this.extractStatistics(this.originalGraphData)
            this.extractRelationTypes(this.originalGraphData)
            this.hasData = true
            this.selectedRelations = []
            this.calculateStats()
            this.initCharts()
            
            // 确保图已初始化，然后渲染
            if (!this.graph) {
              this.initGraph()
            }
            
            // 延迟渲染，确保图已就绪
            setTimeout(() => {
              if (this.graph) {
                this.renderGraph(this.currentGraphData.nodes, this.currentGraphData.edges)
              } else {
                console.error('图仍未初始化')
              }
            }, 200)
            
            this.$message.success('加载成功，共 ' + graphData.nodes.length + ' 个节点')
          } else {
            this.$message.warning('该文献暂无图谱数据')
            this.hasData = false
          }
        } else {
          this.$message.warning('获取图谱数据失败')
          this.hasData = false
        }
      } catch (error) {
        console.error('加载图谱失败:', error)
        this.$message.error('加载图谱失败')
      }
    },
    
    calculateNodeMetrics: function(graphData) {
      var nodes = graphData.nodes
      var edges = graphData.edges
      var N = nodes.length
      
      var adjList = {}
      var inLinks = {}
      for (var i = 0; i < nodes.length; i++) {
        adjList[nodes[i].id] = []
        inLinks[nodes[i].id] = []
      }
      
      for (var j = 0; j < edges.length; j++) {
        var edge = edges[j]
        adjList[edge.source].push(edge.target)
        inLinks[edge.target].push(edge.source)
      }
      
      var iterations = 20
      var damping = 0.85
      var pr = {}
      for (var k = 0; k < nodes.length; k++) {
        pr[nodes[k].id] = 1.0
      }
      
      for (var iter = 0; iter < iterations; iter++) {
        var newPr = {}
        for (var m = 0; m < nodes.length; m++) {
          var nodeId = nodes[m].id
          var sum = 0
          var inNodes = inLinks[nodeId]
          for (var n = 0; n < inNodes.length; n++) {
            var inNode = inNodes[n]
            var outDegree = adjList[inNode].length
            if (outDegree > 0) {
              sum = sum + (pr[inNode] / outDegree)
            }
          }
          newPr[nodeId] = (1 - damping) + damping * sum
        }
        pr = newPr
      }
      
      for (var p = 0; p < nodes.length; p++) {
        nodes[p].pageRank = pr[nodes[p].id]
        nodes[p].degree = adjList[nodes[p].id].length
        nodes[p].betweenness = nodes[p].degree / (N - 1)
      }
    },
    
    extractStatistics: function(graphData) {
      this.entityTypeCount = {}
      this.relationTypeCount = {}
      
      for (var i = 0; i < graphData.nodes.length; i++) {
        var type = graphData.nodes[i].type || 'unknown'
        if (this.entityTypeCount[type]) {
          this.entityTypeCount[type]++
        } else {
          this.entityTypeCount[type] = 1
        }
      }
      
      for (var j = 0; j < graphData.edges.length; j++) {
        var relation = graphData.edges[j].relation
        if (relation) {
          if (this.relationTypeCount[relation]) {
            this.relationTypeCount[relation]++
          } else {
            this.relationTypeCount[relation] = 1
          }
        }
      }
    },
    
    extractRelationTypes: function(graphData) {
      var types = {}
      for (var i = 0; i < graphData.edges.length; i++) {
        var relation = graphData.edges[i].relation
        if (relation) {
          types[relation] = true
        }
      }
      this.relationTypeList = Object.keys(types)
    },
    
    calculateStats: function() {
      var totalDegree = 0
      for (var i = 0; i < this.originalGraphData.nodes.length; i++) {
        totalDegree += this.originalGraphData.nodes[i].degree || 0
      }
      
      var N = this.originalGraphData.nodes.length
      var maxPossibleEdges = N * (N - 1) / 2
      var graphDensity = maxPossibleEdges > 0 ? (this.originalGraphData.edges.length / maxPossibleEdges).toFixed(4) : 0
      
      this.stats = {
        nodeCount: N,
        relationCount: this.originalGraphData.edges.length,
        avgDegree: N > 0 ? (totalDegree / N).toFixed(2) : 0,
        graphDensity: graphDensity
      }
    },
    
    initCharts: function() {
      var entityData = []
      for (var key in this.entityTypeCount) {
        entityData.push({ name: key, value: this.entityTypeCount[key] })
      }
      
      var entityChartDom = document.getElementById('entity-chart')
      if (entityChartDom && entityData.length > 0) {
        if (this.entityChart) {
          this.entityChart.dispose()
        }
        this.entityChart = echarts.init(entityChartDom)
        this.entityChart.setOption({
          tooltip: { trigger: 'item' },
          legend: { orient: 'vertical', left: 'left' },
          series: [{
            type: 'pie',
            radius: '55%',
            data: entityData,
            label: { show: true, formatter: '{b}: {d}%' }
          }]
        })
      }
      
      var relationCategories = []
      var relationValues = []
      for (var rKey in this.relationTypeCount) {
        relationCategories.push(rKey)
        relationValues.push(this.relationTypeCount[rKey])
      }
      
      var relationChartDom = document.getElementById('relation-chart')
      if (relationChartDom && relationCategories.length > 0) {
        if (this.relationChart) {
          this.relationChart.dispose()
        }
        this.relationChart = echarts.init(relationChartDom)
        this.relationChart.setOption({
          tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
          grid: { containLabel: true, bottom: 30 },
          xAxis: { type: 'category', data: relationCategories, axisLabel: { rotate: 30, interval: 0 } },
          yAxis: { type: 'value', name: '数量' },
          series: [{
            type: 'bar',
            data: relationValues,
            itemStyle: { color: '#e6a23c', borderRadius: [4, 4, 0, 0] }
          }]
        })
      }
    },
    
    applyFilter: function() {
      if (!this.originalGraphData.nodes || this.selectedRelations.length === 0) {
        this.renderGraph(this.originalGraphData.nodes, this.originalGraphData.edges)
        return
      }
      
      var filteredEdges = []
      for (var i = 0; i < this.originalGraphData.edges.length; i++) {
        var edge = this.originalGraphData.edges[i]
        if (this.selectedRelations.indexOf(edge.relation) !== -1) {
          filteredEdges.push(edge)
        }
      }
      
      var nodeIds = {}
      for (var j = 0; j < filteredEdges.length; j++) {
        var e = filteredEdges[j]
        nodeIds[e.source] = true
        nodeIds[e.target] = true
      }
      
      var filteredNodes = []
      for (var k = 0; k < this.originalGraphData.nodes.length; k++) {
        var node = this.originalGraphData.nodes[k]
        if (nodeIds[node.id]) {
          filteredNodes.push(node)
        }
      }
      
      this.renderGraph(filteredNodes, filteredEdges)
      this.$message.info('显示 ' + filteredEdges.length + ' 条关系')
    },
    
    clearFilter: function() {
      this.selectedRelations = []
      this.renderGraph(this.originalGraphData.nodes, this.originalGraphData.edges)
    },
    
    renderGraph: function(nodes, edges) {
      if (!this.graph) {
        console.error('graph对象不存在')
        return
      }
      
      console.log('渲染图谱，节点数:', nodes.length, '边数:', edges.length)
      
      var graphNodes = []
      for (var i = 0; i < nodes.length; i++) {
        graphNodes.push({
          id: nodes[i].id,
          name: nodes[i].name,
          label: nodes[i].name,
          type: nodes[i].type,
          degree: nodes[i].degree,
          style: {
            fill: this.getNodeColor(nodes[i].type)
          }
        })
      }
      
      var graphEdges = []
      for (var j = 0; j < edges.length; j++) {
        graphEdges.push({
          source: edges[j].source,
          target: edges[j].target,
          label: edges[j].relation,
          relation: edges[j].relation
        })
      }
      
      this.graph.data({ nodes: graphNodes, edges: graphEdges })
      this.graph.render()
      
      setTimeout(() => {
        if (this.graph && !this.graph.destroyed) {
          this.graph.fitView()
        }
      }, 200)
    },

    fitView: function() {
      if (this.graph) {
        this.graph.fitView()
      }
    },

    zoomIn: function() {
      if (this.graph) {
        var currentZoom = this.graph.getZoom()
        this.graph.zoomTo(currentZoom * 1.2, {
          x: this.graph.getWidth() / 2,
          y: this.graph.getHeight() / 2
        })
      }
    },

    zoomOut: function() {
      if (this.graph) {
        var currentZoom = this.graph.getZoom()
        this.graph.zoomTo(currentZoom * 0.8, {
          x: this.graph.getWidth() / 2,
          y: this.graph.getHeight() / 2
        })
      }
    },

    getNodeColor: function(type) {
      return this.nodeColors[type] || this.nodeColors['unknown']
    },

    getNodeTypeTag: function(type) {
      var tagMap = {
        'Composition': 'primary',
        'Structure': 'success',
        'Property': 'warning',
        'Application': 'danger',
        'Processing': 'info',
        'Characterization': 'success',
        'Condition': 'warning',
        'Feature': 'info'
      }
      return tagMap[type] || 'info'
    }
  }
}
</script>

<style scoped>
/* 样式保持不变 */
.global-analysis {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 16px;
  gap: 16px;
  background: #f5f7fa;
}

.analysis-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: white;
  padding: 12px 20px;
  border-radius: 8px;
  box-shadow: 0 1px 2px rgba(0,0,0,0.05);
}

.header-title h2 {
  font-size: 18px;
  font-weight: 500;
  color: #333;
  margin: 0;
}

.header-controls {
  display: flex;
  gap: 12px;
  align-items: center;
}

.analysis-main {
  flex: 1;
  display: flex;
  gap: 0;
  min-height: 0;
  background: white;
  border-radius: 8px;
  border: 1px solid #e8e8e8;
  overflow: hidden;
}

.sidebar-tabs {
  width: 80px;
  background: #fafafa;
  border-right: 1px solid #e8e8e8;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.tab-btn {
  padding: 16px 0;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s;
  color: #666;
  border-bottom: 1px solid #e8e8e8;
}

.tab-btn i {
  font-size: 20px;
  display: block;
  margin-bottom: 6px;
}

.tab-btn span {
  font-size: 12px;
}

.tab-btn:hover {
  background: #f0f0f0;
  color: #409EFF;
}

.tab-btn.active {
  background: white;
  color: #409EFF;
  border-right: 2px solid #409EFF;
}

.content-panel {
  flex: 1;
  position: relative;
  overflow: hidden;
}

.graph-mode {
  width: 100%;
  height: 100%;
  position: relative;
}

.graph-container-wrapper {
  width: 100%;
  height: 100%;
  position: relative;
}

.graph-container {
  width: 100%;
  height: 100%;
  background: #fafbfc;
}

.graph-controls {
  position: absolute;
  bottom: 20px;
  right: 20px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  background: white;
  padding: 8px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  z-index: 10;
}

.filter-card {
  position: absolute;
  top: 20px;
  right: 20px;
  width: 220px;
  background: white;
  border-radius: 8px;
  padding: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  z-index: 10;
}

.filter-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  font-size: 12px;
  font-weight: 500;
  color: #666;
}

.data-mode {
  width: 100%;
  height: 100%;
  overflow-y: auto;
  padding: 20px;
  background: white;
}

.data-container {
  max-width: 1200px;
  margin: 0 auto;
}

.metrics-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.metric-card {
  background: #f9f9f9;
  border-radius: 8px;
  padding: 20px;
  text-align: center;
  border: 1px solid #e8e8e8;
}

.metric-value {
  font-size: 28px;
  font-weight: 600;
  color: #409EFF;
  margin-bottom: 8px;
}

.metric-label {
  font-size: 13px;
  color: #666;
}

.importance-section {
  margin-bottom: 24px;
}

.section-title {
  font-size: 16px;
  font-weight: 500;
  color: #333;
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 1px solid #e8e8e8;
}

.section-title i {
  margin-right: 8px;
  color: #409EFF;
}

.importance-table {
  overflow-x: auto;
}

.importance-table table {
  width: 100%;
  border-collapse: collapse;
}

.importance-table th,
.importance-table td {
  padding: 10px 12px;
  text-align: left;
  border-bottom: 1px solid #f0f0f0;
}

.importance-table th {
  background: #fafafa;
  font-weight: 500;
  color: #666;
  font-size: 13px;
}

.importance-table td {
  font-size: 13px;
  color: #333;
}

.importance-table .node-name {
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.charts-section {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-top: 24px;
}

.chart-card {
  background: #fafafa;
  border-radius: 8px;
  padding: 16px;
  border: 1px solid #e8e8e8;
}

.chart-title {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  margin-bottom: 12px;
  text-align: center;
}

.chart-title i {
  margin-right: 6px;
  color: #409EFF;
}

.empty-state {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
  color: #999;
}

.empty-state i {
  font-size: 48px;
  margin-bottom: 12px;
}

.detail-card {
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 500px;
  max-width: 85%;
  background: white;
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(0,0,0,0.15);
  z-index: 1000;
  animation: fadeIn 0.2s ease;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 18px;
  border-bottom: 1px solid #f0f0f0;
  font-weight: 600;
  font-size: 15px;
  background: #fafafa;
  border-radius: 12px 12px 0 0;
}

.detail-body {
  padding: 18px;
  max-height: 60vh;
  overflow-y: auto;
}

.detail-row {
  margin-bottom: 14px;
  display: flex;
}

.detail-label {
  width: 80px;
  color: #8c8c8c;
  font-size: 13px;
  flex-shrink: 0;
}

.detail-value {
  flex: 1;
  color: #333;
  font-size: 13px;
  font-weight: 500;
  word-break: break-word;
}

.sentences-list {
  flex: 1;
  max-height: 200px;
  overflow-y: auto;
}

.sentence-item {
  background: #f5f7fa;
  padding: 10px;
  margin-bottom: 8px;
  border-radius: 6px;
  font-size: 12px;
  line-height: 1.5;
  color: #666;
  border-left: 3px solid #409EFF;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translate(-50%, -48%);
  }
  to {
    opacity: 1;
    transform: translate(-50%, -50%);
  }
}

.sentences-list::-webkit-scrollbar,
.detail-body::-webkit-scrollbar {
  width: 6px;
}

.sentences-list::-webkit-scrollbar-track,
.detail-body::-webkit-scrollbar-track {
  background: #f1f1f1;
}

.sentences-list::-webkit-scrollbar-thumb,
.detail-body::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}
</style>