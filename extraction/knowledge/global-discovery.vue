<template>
  <div class="global-analysis">
    <!-- 页面头部 -->
    <div class="analysis-header">
      <div class="header-title">
        <h2>全局分析</h2>
      </div>
      <div class="header-controls">
        <el-select 
          v-model="selectedDatabase" 
          placeholder="请选择数据库"
          size="small"
          style="width: 280px;"
          filterable
          clearable
        >
          <el-option
            label="xai数据库 (extraction_sentence)"
            value="xai"
          >
            <span>xai数据库</span>
            <span style="float: right; color: #8492a6; font-size: 12px">extraction_sentence表</span>
          </el-option>
          <el-option
            label="测试数据库"
            value="test"
          >
            <span>测试数据库</span>
            <span style="float: right; color: #8492a6; font-size: 12px">测试数据</span>
          </el-option>
        </el-select>
        <el-button type="primary" @click="loadGraphData" size="small" :loading="loading">
          <i class="el-icon-search"></i> 加载图谱数据
        </el-button>
      </div>
    </div>

    <!-- 主要内容区域 -->
    <div class="analysis-main">
      <!-- 图谱画布区域 -->
      <div class="graph-area">
        <div v-if="hasData" class="graph-container-wrapper">
          <div id="graph-container" class="graph-container"></div>
          
          <!-- 图谱控制按钮 -->
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
        </div>
        <div v-else class="empty-state">
          <i class="el-icon-info"></i>
          <p>请选择数据库后点击"加载图谱数据"</p>
        </div>
      </div>

      <!-- 右侧边栏 - 可拖动调整宽度 -->
      <div class="right-sidebar" :style="{ width: sidebarWidth + 'px' }">
        <div class="sidebar-resize" @mousedown="startResize"></div>
        <div class="sidebar-content">
          <div class="sidebar-tabs">
            <div 
              class="tab-btn" 
              :class="{ active: activeTab === 'overview' }"
              @click="setActiveTab('overview')"
            >
              <i class="el-icon-data-line"></i>
              <span>Overview</span>
            </div>
            <div 
              class="tab-btn" 
              :class="{ active: activeTab === 'entity' }"
              @click="setActiveTab('entity')"
            >
              <i class="el-icon-document"></i>
              <span>实体</span>
            </div>
            <div 
              class="tab-btn" 
              :class="{ active: activeTab === 'relation' }"
              @click="setActiveTab('relation')"
            >
              <i class="el-icon-share"></i>
              <span>关系</span>
            </div>
          </div>

          <!-- Overview 内容 -->
          <div v-show="activeTab === 'overview'" class="tab-content overview-content">
            <!-- 实体统计环形图 -->
            <div class="chart-container">
              <div class="chart-title">实体类型分布</div>
              <div id="entity-pie-chart" style="height: 240px;"></div>
            </div>
            
            <!-- 关系统计环形图 - 优化显示 -->
            <div class="chart-container">
              <div class="chart-title">关系类型分布</div>
              <div id="relation-pie-chart" style="height: 260px;"></div>
            </div>
            
            <!-- 数据表格 -->
            <div class="data-table-container">
              <div class="table-title">节点重要性排名</div>
              <el-table :data="topImportantNodes" size="small" max-height="280">
                <el-table-column prop="name" label="名称" min-width="100" show-overflow-tooltip></el-table-column>
                <el-table-column prop="pageRank" label="PR" width="70">
                  <template slot-scope="scope">
                    {{ scope.row.pageRank ? scope.row.pageRank.toFixed(4) : '-' }}
                  </template>
                </el-table-column>
                <el-table-column prop="betweenness" label="BTN" width="70">
                  <template slot-scope="scope">
                    {{ scope.row.betweenness ? scope.row.betweenness.toFixed(4) : '-' }}
                  </template>
                </el-table-column>
                <el-table-column prop="closeness" label="CSN" width="70">
                  <template slot-scope="scope">
                    {{ scope.row.closeness ? scope.row.closeness.toFixed(4) : '-' }}
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </div>

          <!-- 实体 Tab 内容 -->
          <div v-show="activeTab === 'entity'" class="tab-content">
            <div v-if="selectedNode" class="detail-panel">
              <div class="detail-panel-header">
                <i class="el-icon-info"></i> 实体详情
                <el-button type="text" size="small" @click="clearNodeSelection" class="close-btn">×</el-button>
              </div>
              <div class="detail-panel-body">
                <div class="detail-row">
                  <span class="detail-label">实体名称</span>
                  <span class="detail-value">{{ selectedNode.name }}</span>
                </div>
                <div class="detail-row">
                  <span class="detail-label">实体类型</span>
                  <el-tag size="small" :type="getNodeTypeTag(selectedNode.type)">
                    {{ selectedNode.type || 'unknown' }}
                  </el-tag>
                </div>
                <div class="detail-row">
                  <span class="detail-label">PageRank</span>
                  <span class="detail-value">{{ selectedNode.pageRank ? selectedNode.pageRank.toFixed(6) : '-' }}</span>
                </div>
                <div class="detail-row">
                  <span class="detail-label">度数</span>
                  <span class="detail-value">{{ selectedNode.degree || 0 }}</span>
                </div>
                <div class="detail-section" v-if="selectedNode.sentences && selectedNode.sentences.length">
                  <div class="section-title">
                    <i class="el-icon-document"></i> 来源文献
                  </div>
                  <div class="sentences-list">
                    <div v-for="(item, idx) in selectedNode.sentences" :key="idx" class="sentence-item">
                      <div class="sentence-literature">
                        <i class="el-icon-document-copy"></i> 
                        <span class="literature-id">文献: {{ item.literatureTitle || item.literatureId || '未知' }}</span>
                      </div>
                      <div class="sentence-text" v-html="highlightText(item.sentenceText || item, selectedNode.name)">
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            <div v-else class="empty-tip">
              <i class="el-icon-info"></i>
              <p>请点击图谱中的实体节点查看详情</p>
            </div>
          </div>

          <!-- 关系 Tab 内容 -->
          <div v-show="activeTab === 'relation'" class="tab-content">
            <div v-if="selectedEdge" class="detail-panel">
              <div class="detail-panel-header">
                <i class="el-icon-share"></i> 关系详情
                <el-button type="text" size="small" @click="clearEdgeSelection" class="close-btn">×</el-button>
              </div>
              <div class="detail-panel-body">
                <div class="detail-row">
                  <span class="detail-label">关系类型</span>
                  <el-tag size="small" :style="{ backgroundColor: getEdgeColor(selectedEdge.relation), color: '#fff', border: 'none' }">
                    {{ selectedEdge.relation }}
                  </el-tag>
                </div>
                <div class="detail-row">
                  <span class="detail-label">头实体</span>
                  <span class="detail-value">{{ getNodeNameById(selectedEdge.source) }}</span>
                </div>
                <div class="detail-row">
                  <span class="detail-label">尾实体</span>
                  <span class="detail-value">{{ getNodeNameById(selectedEdge.target) }}</span>
                </div>
                <div class="detail-section" v-if="selectedEdge.sentences && selectedEdge.sentences.length">
                  <div class="section-title">
                    <i class="el-icon-document"></i> 来源文献
                  </div>
                  <div class="sentences-list">
                    <div v-for="(item, idx) in selectedEdge.sentences" :key="idx" class="sentence-item">
                      <div class="sentence-literature">
                        <i class="el-icon-document-copy"></i> 
                        <span class="literature-id">文献: {{ item.literatureTitle || item.literatureId || '未知' }}</span>
                      </div>
                      <div class="sentence-text" v-html="highlightText(item.sentenceText || item, '')">
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            <div v-else class="empty-tip">
              <i class="el-icon-info"></i>
              <p>请点击图谱中的关系边查看详情</p>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部可上拉筛选面板 -->
    <div class="filter-drawer" :class="{ expanded: filterDrawerExpanded }" :style="{ height: filterDrawerHeight + 'px' }">
       <!-- 添加可拖动条 -->
      <div class="filter-drawer-resize" @mousedown="startResizeFilter"></div>

      <div class="filter-drawer-header" @click="toggleFilterDrawer">
        <div class="filter-drawer-title">
          <i class="el-icon-s-operation"></i>
          <span>高级筛选</span>
          <span v-if="hasActiveFilter" class="filter-badge">已筛选</span>
        </div>
        <div class="filter-drawer-toggle">
          <i :class="filterDrawerExpanded ? 'el-icon-arrow-down' : 'el-icon-arrow-up'"></i>
        </div>
      </div>
      <div class="filter-drawer-content" v-show="filterDrawerExpanded">
        <div class="filter-form">
          <el-row :gutter="16">
            <el-col :span="7">
              <div class="filter-group">
                <div class="filter-group-title">头结点属性</div>
                <el-select v-model="filterHeadType" placeholder="选择节点类型" clearable filterable size="small">
                  <el-option v-for="type in nodeTypeList" :key="type" :label="type" :value="type"></el-option>
                </el-select>
                <el-input v-model="filterHeadName" placeholder="节点名称关键字" clearable size="small" style="margin-top: 8px;"></el-input>
              </div>
            </el-col>
            <el-col :span="7">
              <div class="filter-group">
                <div class="filter-group-title">尾结点属性</div>
                <el-select v-model="filterTailType" placeholder="选择节点类型" clearable filterable size="small">
                  <el-option v-for="type in nodeTypeList" :key="type" :label="type" :value="type"></el-option>
                </el-select>
                <el-input v-model="filterTailName" placeholder="节点名称关键字" clearable size="small" style="margin-top: 8px;"></el-input>
              </div>
            </el-col>
            <el-col :span="7">
              <div class="filter-group">
                <div class="filter-group-title">关系类型</div>
                <el-select v-model="filterRelationType" placeholder="选择关系类型" clearable filterable size="small">
                  <el-option v-for="type in relationTypeList" :key="type" :label="type" :value="type"></el-option>
                </el-select>
              </div>
            </el-col>
            <el-col :span="3">
              <div class="filter-actions">
                <el-button type="primary" size="small" @click="applyAdvancedFilter" style="width: 100%; margin-bottom: 8px;">应用</el-button>
                <el-button size="small" @click="resetFilter" style="width: 100%;">重置</el-button>
              </div>
            </el-col>
          </el-row>
          <!-- 当前筛选条件展示 -->
          <div class="filter-tags" v-if="hasActiveFilter">
            <span class="filter-tags-label">当前筛选：</span>
            <el-tag v-if="filterHeadType" size="small" closable @close="filterHeadType = ''; applyAdvancedFilter()">头类型: {{ filterHeadType }}</el-tag>
            <el-tag v-if="filterHeadName" size="small" closable @close="filterHeadName = ''; applyAdvancedFilter()">头名称: {{ filterHeadName }}</el-tag>
            <el-tag v-if="filterTailType" size="small" closable @close="filterTailType = ''; applyAdvancedFilter()">尾类型: {{ filterTailType }}</el-tag>
            <el-tag v-if="filterTailName" size="small" closable @close="filterTailName = ''; applyAdvancedFilter()">尾名称: {{ filterTailName }}</el-tag>
            <el-tag v-if="filterRelationType" size="small" closable @close="filterRelationType = ''; applyAdvancedFilter()">关系: {{ filterRelationType }}</el-tag>
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
  data: function() {
    return {
      graph: null,
      hasData: false,
      loading: false,
      activeTab: 'overview',
      sidebarWidth: 420,
      filterDrawerHeight: 320,
      isResizingFilter: false,
      filterDrawerExpanded: false,
      selectedDatabase: 'xai',
      
      originalGraphData: { nodes: [], edges: [] },
      currentGraphData: { nodes: [], edges: [] },
      
      allNodes: [],
      allEdges: [],
      
      filterHeadType: '',
      filterHeadName: '',
      filterTailType: '',
      filterTailName: '',
      filterRelationType: '',
      
      selectedNodeId: null,
      selectedNode: null,
      selectedEdgeKey: null,
      selectedEdge: null,
      
      entityTypeCount: {},
      relationTypeCount: {},
      nodeTypeList: [],
      relationTypeList: [],
      
      entityPieChart: null,
      relationPieChart: null,
      
      nodeColors: {
        'Composition': '#5B8FF9',
        'Structure': '#5AD8A6',
        'Property': '#F6BD16',
        'Application': '#E8684A',
        'Processing': '#6DC8F3',
        'Characterization': '#9270CA',
        'Condition': '#FF99C3',
        'Feature': '#269A99',
        'AlloyDesignation': '#F08D5C',
        'unknown': '#B8C5D0'
      },
      edgeColors: {
        'cause_effect': '#F56C6C',
        'component_whole': '#67C23A',
        'condition_on': '#409EFF',
        'related_to': '#E6A23C',
        'value_of': '#909399',
        'property_of': '#9B59B6',
        'feature_of': '#1ABC9C',
        'used_for': '#E67E22',
        'composition_of': '#2ECC71',
        'instance_of': '#3498DB',
        'located_of': '#00BCD4',
        'other': '#AAAAAA'
      }
    }
  },
  
  computed: {
    topImportantNodes: function() {
      var self = this
      if (!self.allNodes || self.allNodes.length === 0) return []
      var sorted = self.allNodes.slice()
      sorted.sort(function(a, b) {
        return (b.pageRank || 0) - (a.pageRank || 0)
      })
      return sorted.slice(0, 20)
    },
    hasActiveFilter: function() {
      return this.filterHeadType || this.filterHeadName || this.filterTailType || this.filterTailName || this.filterRelationType
    }
  },

  mounted: function() {
    this.registerCustomNode()
    var self = this
    this.$nextTick(function() {
      self.initGraph()
    })
    window.addEventListener('resize', this.handleResize)
  },

  beforeDestroy: function() {
    if (this.graph) {
      this.graph.destroy()
    }
    if (this.entityPieChart) {
      this.entityPieChart.dispose()
    }
    if (this.relationPieChart) {
      this.relationPieChart.dispose()
    }
    window.removeEventListener('resize', this.handleResize)
  },

  methods: {
    // 高亮文本
    highlightText: function(text, keyword) {
      if (!text) return ''
      if (!keyword || keyword === '') return text
      var regex = new RegExp('(' + this.escapeRegExp(keyword) + ')', 'gi')
      return text.replace(regex, '<span class="highlight-text">$1</span>')
    },

     // 开始拖动筛选栏
  startResizeFilter: function(e) {
    e.stopPropagation()
    var self = this
    var startY = e.clientY
    var startHeight = this.filterDrawerHeight
    
    var onMouseMove = function(moveEvent) {
      var deltaY = startY - moveEvent.clientY  // 向上拖动增加高度
      var newHeight = startHeight + deltaY
      
      // 限制高度范围：最小 200px，最大 600px
      if (newHeight >= 200 && newHeight <= 600) {
        self.filterDrawerHeight = newHeight
      }
    }
    
    var onMouseUp = function() {
      document.removeEventListener('mousemove', onMouseMove)
      document.removeEventListener('mouseup', onMouseUp)
      self.isResizingFilter = false
    }
    
    document.addEventListener('mousemove', onMouseMove)
    document.addEventListener('mouseup', onMouseUp)
    this.isResizingFilter = true
  },
    
    escapeRegExp: function(str) {
      return str.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
    },
    
    toggleFilterDrawer: function() {
      this.filterDrawerExpanded = !this.filterDrawerExpanded
    },
    
    registerCustomNode: function() {
      G6.registerNode('circle-with-text', {
        draw: function(cfg, group) {
          var size = cfg.size || 70
          var color = (cfg.style && cfg.style.fill) || '#5B8FF9'
          var text = cfg.label || ''
          
          group.addShape('circle', {
            attrs: {
              x: 0,
              y: 0,
              r: size / 2 + 3,
              fill: color,
              opacity: 0.2,
              cursor: 'pointer'
            },
            name: 'glow-shape'
          })
          
          var keyShape = group.addShape('circle', {
            attrs: {
              x: 0,
              y: 0,
              r: size / 2,
              fill: color,
              stroke: '#ffffff',
              lineWidth: 2,
              cursor: 'pointer',
              shadowColor: 'rgba(0,0,0,0.2)',
              shadowBlur: 6
            },
            name: 'circle-shape'
          })
          
          if (text && text.length > 0) {
            var fontSize = 12
            if (text.length > 6) fontSize = 11
            if (text.length > 8) fontSize = 10
            
            group.addShape('text', {
              attrs: {
                x: 0,
                y: 0,
                text: text,
                fill: '#ffffff',
                fontSize: fontSize,
                fontWeight: 'bold',
                textAlign: 'center',
                textBaseline: 'middle',
                shadowColor: 'rgba(0,0,0,0.3)',
                shadowBlur: 2
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
    
    startResize: function(e) {
      var self = this
      var startX = e.clientX
      var startWidth = this.sidebarWidth
      
      var onMouseMove = function(moveEvent) {
        var newWidth = startWidth - (moveEvent.clientX - startX)
        if (newWidth >= 350 && newWidth <= 600) {
          self.sidebarWidth = newWidth
        }
      }
      
      var onMouseUp = function() {
        document.removeEventListener('mousemove', onMouseMove)
        document.removeEventListener('mouseup', onMouseUp)
      }
      
      document.addEventListener('mousemove', onMouseMove)
      document.addEventListener('mouseup', onMouseUp)
    },
    
    setActiveTab: function(tab) {
      var self = this
      this.activeTab = tab
      if (tab === 'overview' && this.hasData) {
        this.$nextTick(function() {
          self.initCharts()
        })
      }
    },
    
    initGraph: function() {
      var container = document.getElementById('graph-container')
      var self = this
      if (!container) {
        setTimeout(function() {
          if (!self.graph && document.getElementById('graph-container')) {
            self.initGraph()
          }
        }, 100)
        return
      }
      
      if (this.graph) {
        this.graph.destroy()
        this.graph = null
      }
      
      var vueInstance = this
      
      this.graph = new G6.Graph({
        container: 'graph-container',
        width: container.clientWidth,
        height: container.clientHeight,
        layout: {
          type: 'force',
          preventOverlap: true,
          linkDistance: 200,
          nodeStrength: 80,
          edgeStrength: 0.1,
          nodeSpacing: 70,
          alpha: 0.8,
          alphaDecay: 0.02
        },
        defaultNode: {
          type: 'circle-with-text',
          size: 70,
          style: {
            fill: '#5B8FF9',
            stroke: '#ffffff',
            lineWidth: 2,
            cursor: 'pointer'
          },
          labelCfg: {
            style: {
              fill: '#ffffff',
              fontSize: 12,
              fontWeight: 'bold'
            },
            position: 'center'
          }
        },
        defaultEdge: {
          type: 'line',
          style: function(edge) {
            var relation = edge.relation || edge.label
            var color = vueInstance.getEdgeColor(relation)
            return {
              stroke: color,
              lineWidth: 1.2,
              endArrow: {
                path: G6.Arrow.triangle(5, 6),
                fill: color
              },
              opacity: 0.7
            }
          },
          labelCfg: {
            autoRotate: true,
            style: {
              fill: '#666',
              fontSize: 9,
              background: {
                fill: 'rgba(255,255,255,0.8)',
                padding: [2, 4, 2, 4],
                radius: 4
              }
            }
          }
        },
        modes: {
          default: ['drag-canvas', 'zoom-canvas', 'drag-node']
        },
        nodeStateStyles: {
          hover: {
            stroke: '#ffa940',
            lineWidth: 3,
            shadowBlur: 15,
            shadowColor: '#ffa940'
          },
          selected: {
            stroke: '#ffa940',
            lineWidth: 3,
            shadowBlur: 15,
            shadowColor: '#ffa940'
          }
        },
        edgeStateStyles: {
          hover: {
            lineWidth: 2.5,
            opacity: 1
          }
        }
      })
      
      this.graph.on('node:click', function(evt) {
        var nodeModel = evt.item.getModel()
        var fullNodeData = null
        for (var i = 0; i < vueInstance.allNodes.length; i++) {
          if (vueInstance.allNodes[i].id === nodeModel.id) {
            fullNodeData = vueInstance.allNodes[i]
            break
          }
        }
        if (fullNodeData) {
          vueInstance.selectNode(fullNodeData)
        }
      })
      
      this.graph.on('edge:click', function(evt) {
        var edgeModel = evt.item.getModel()
        var fullEdgeData = null
        for (var i = 0; i < vueInstance.allEdges.length; i++) {
          var e = vueInstance.allEdges[i]
          if (e.source === edgeModel.source && e.target === edgeModel.target && e.relation === edgeModel.relation) {
            fullEdgeData = e
            break
          }
        }
        if (fullEdgeData) {
          vueInstance.selectRelation(fullEdgeData)
        }
      })
      
      this.graph.on('canvas:click', function() {
        vueInstance.clearNodeSelection()
        vueInstance.clearEdgeSelection()
      })
    },
    
    getNodeNameById: function(nodeId) {
      for (var i = 0; i < this.allNodes.length; i++) {
        if (this.allNodes[i].id === nodeId) {
          return this.allNodes[i].name
        }
      }
      return nodeId
    },
    
    selectNode: function(node) {
      var self = this
      this.selectedNodeId = node.id
      this.selectedNode = node
      this.selectedEdgeKey = null
      this.selectedEdge = null
      this.activeTab = 'entity'
      
      if (this.graph) {
        var nodeItem = this.graph.find('node', function(n) { return n.getID() === node.id })
        if (nodeItem) {
          this.graph.focusItem(nodeItem)
          this.graph.setItemState(nodeItem, 'selected', true)
          
          setTimeout(function() {
            if (self.graph) {
              self.graph.setItemState(nodeItem, 'selected', false)
            }
          }, 2000)
        }
      }
    },
    
    selectRelation: function(edge) {
      var self = this
      var key = edge.source + '_' + edge.target + '_' + edge.relation
      this.selectedEdgeKey = key
      this.selectedEdge = edge
      this.selectedNodeId = null
      this.selectedNode = null
      this.activeTab = 'relation'
      
      if (this.graph) {
        var edges = this.graph.getEdges()
        for (var i = 0; i < edges.length; i++) {
          var e = edges[i]
          var model = e.getModel()
          if (model.source === edge.source && model.target === edge.target && model.relation === edge.relation) {
            this.graph.focusItem(e)
            this.graph.setItemState(e, 'hover', true)
            setTimeout(function() {
              if (self.graph) {
                self.graph.setItemState(e, 'hover', false)
              }
            }, 2000)
            break
          }
        }
      }
    },
    
    clearNodeSelection: function() {
      this.selectedNodeId = null
      this.selectedNode = null
    },
    
    clearEdgeSelection: function() {
      this.selectedEdgeKey = null
      this.selectedEdge = null
    },

    handleResize: function() {
      if (this.graph && !this.graph.destroyed) {
        var container = document.getElementById('graph-container')
        if (container) {
          this.graph.changeSize(container.clientWidth, container.clientHeight)
        }
      }
      if (this.entityPieChart) {
        this.entityPieChart.resize()
      }
      if (this.relationPieChart) {
        this.relationPieChart.resize()
      }
    },

    loadGraphData: function() {
      var self = this
      if (!this.selectedDatabase) {
        this.$message.warning('请先选择数据库')
        return
      }
      
      this.loading = true
      var url = '/api/knowledge-graph/all-graph'
      
      client.get(url).then(function(response) {
        if (response.data && response.data.code === 200) {
          var graphData = response.data.data
          
          if (graphData.nodes && graphData.nodes.length > 0) {
            self.processGraphData(graphData)
            self.hasData = true
            
            if (!self.graph) {
              self.initGraph()
            }
            
            setTimeout(function() {
              if (self.graph) {
                self.renderGraph(self.currentGraphData.nodes, self.currentGraphData.edges)
              }
            }, 200)
            
            self.$message.success('加载成功，共 ' + graphData.nodes.length + ' 个节点，' + graphData.edges.length + ' 条关系')
          } else {
            self.$message.warning('暂无图谱数据')
            self.hasData = false
          }
        } else {
          self.$message.error('获取图谱数据失败')
        }
        self.loading = false
      }).catch(function(error) {
        console.error('加载图谱失败:', error)
        self.$message.error('加载图谱失败: ' + (error.message || '网络错误'))
        self.loading = false
      })
    },
    
    processGraphData: function(graphData) {
      this.originalGraphData = JSON.parse(JSON.stringify(graphData))
      this.currentGraphData = JSON.parse(JSON.stringify(graphData))
      
      this.allNodes = graphData.nodes || []
      this.allEdges = graphData.edges || []
      
      this.calculateNodeMetrics()
      this.extractStatistics()
      this.initCharts()
    },
    
    calculateNodeMetrics: function() {
      var nodes = this.allNodes
      var edges = this.allEdges
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
        if (N > 1) {
          nodes[p].betweenness = nodes[p].degree / (N - 1)
          nodes[p].closeness = nodes[p].degree / (N - 1) * 0.8
        } else {
          nodes[p].betweenness = 0
          nodes[p].closeness = 0
        }
      }
    },
    
    extractStatistics: function() {
      this.entityTypeCount = {}
      this.relationTypeCount = {}
      this.nodeTypeList = []
      this.relationTypeList = []
      
      for (var i = 0; i < this.allNodes.length; i++) {
        var type = this.allNodes[i].type || 'unknown'
        if (this.entityTypeCount[type]) {
          this.entityTypeCount[type]++
        } else {
          this.entityTypeCount[type] = 1
        }
      }
      
      for (var j = 0; j < this.allEdges.length; j++) {
        var relation = this.allEdges[j].relation
        if (relation) {
          if (this.relationTypeCount[relation]) {
            this.relationTypeCount[relation]++
          } else {
            this.relationTypeCount[relation] = 1
          }
        }
      }
      
      this.nodeTypeList = Object.keys(this.entityTypeCount)
      this.relationTypeList = Object.keys(this.relationTypeCount)
    },
    
    initCharts: function() {
      var self = this
      
      var entityData = []
      for (var key in this.entityTypeCount) {
        entityData.push({ name: key, value: this.entityTypeCount[key] })
      }
      
      var entityChartDom = document.getElementById('entity-pie-chart')
      if (entityChartDom && entityData.length > 0) {
        if (this.entityPieChart) {
          this.entityPieChart.dispose()
        }
        this.entityPieChart = echarts.init(entityChartDom)
        this.entityPieChart.setOption({
          tooltip: { trigger: 'item', formatter: '{b}: {d}%' },
          legend: { 
            orient: 'vertical', 
            left: 'left', 
            textStyle: { fontSize: 10 },
            itemWidth: 12,
            itemHeight: 12
          },
          series: [{
            type: 'pie',
            radius: ['40%', '65%'],
            center: ['55%', '50%'],
            data: entityData,
            label: { show: false },
            emphasis: { scale: true, label: { show: true } },
            itemStyle: {
              borderRadius: 6,
              borderColor: '#fff',
              borderWidth: 2
            }
          }]
        })
      }
      
      var relationData = []
      for (var rKey in this.relationTypeCount) {
        relationData.push({ name: rKey, value: this.relationTypeCount[rKey] })
      }
      
      var relationChartDom = document.getElementById('relation-pie-chart')
      if (relationChartDom && relationData.length > 0) {
        if (this.relationPieChart) {
          this.relationPieChart.dispose()
        }
        this.relationPieChart = echarts.init(relationChartDom)
        this.relationPieChart.setOption({
          tooltip: { trigger: 'item', formatter: '{b}: {d}%' },
          legend: { 
            orient: 'vertical', 
            left: 'left', 
            textStyle: { fontSize: 10 },
            itemWidth: 12,
            itemHeight: 12,
            type: 'scroll',
            pageIconColor: '#409EFF'
          },
          series: [{
            type: 'pie',
            radius: ['40%', '65%'],
            center: ['55%', '50%'],
            data: relationData,
            label: { show: false },
            emphasis: { scale: true, label: { show: true } },
            itemStyle: {
              borderRadius: 6,
              borderColor: '#fff',
              borderWidth: 2,
              color: function(params) {
                var relation = relationData[params.dataIndex].name
                return self.getEdgeColor(relation)
              }
            }
          }]
        })
      }
    },
    
    renderGraph: function(nodes, edges) {
      if (!this.graph) return
      var self = this
      
      var maxPr = 0.001, minPr = Infinity
      for (var i = 0; i < nodes.length; i++) {
        var pr = nodes[i].pageRank || 0
        if (pr > maxPr) maxPr = pr
        if (pr < minPr) minPr = pr
      }
      if (maxPr === minPr) maxPr = minPr + 0.001
      
      var graphNodes = []
      for (var j = 0; j < nodes.length; j++) {
        var node = nodes[j]
        var pr = node.pageRank || 0
        var size = 60 + ((pr - minPr) / (maxPr - minPr)) * 20
        if (size > 80) size = 80
        if (size < 60) size = 60
        
        var originalName = node.name || ''
        var displayName = originalName
        var maxLen = 8
        if (originalName.length > maxLen) {
          displayName = originalName.substring(0, maxLen - 1) + '...'
        }
        
        graphNodes.push({
          id: node.id,
          name: originalName,
          label: displayName,
          type: node.type,
          degree: node.degree,
          pageRank: pr,
          size: size,
          style: {
            fill: self.getNodeColor(node.type)
          },
          labelCfg: {
            style: {
              fill: '#ffffff',
              fontSize: 12,
              fontWeight: 'bold'
            }
          }
        })
      }
      
      var graphEdges = []
      for (var k = 0; k < edges.length; k++) {
        var edge = edges[k]
        graphEdges.push({
          source: edge.source,
          target: edge.target,
          label: edge.relation,
          relation: edge.relation
        })
      }
      
      this.graph.clear()
      this.graph.data({ nodes: graphNodes, edges: graphEdges })
      this.graph.render()
      
      setTimeout(function() {
        if (self.graph && !self.graph.destroyed) {
          self.graph.fitView()
        }
      }, 200)
    },
    
    applyAdvancedFilter: function() {
      var self = this
      var filteredEdges = []
      for (var i = 0; i < this.allEdges.length; i++) {
        var edge = this.allEdges[i]
        var sourceNode = null
        var targetNode = null
        
        for (var j = 0; j < this.allNodes.length; j++) {
          if (this.allNodes[j].id === edge.source) sourceNode = this.allNodes[j]
          if (this.allNodes[j].id === edge.target) targetNode = this.allNodes[j]
        }
        
        if (!sourceNode || !targetNode) continue
        
        var headMatch = true
        var tailMatch = true
        var relationMatch = true
        
        if (this.filterHeadType) {
          headMatch = sourceNode.type === this.filterHeadType
        }
        if (this.filterHeadName) {
          headMatch = headMatch && sourceNode.name.toLowerCase().indexOf(this.filterHeadName.toLowerCase()) !== -1
        }
        if (this.filterTailType) {
          tailMatch = targetNode.type === this.filterTailType
        }
        if (this.filterTailName) {
          tailMatch = tailMatch && targetNode.name.toLowerCase().indexOf(this.filterTailName.toLowerCase()) !== -1
        }
        if (this.filterRelationType) {
          relationMatch = edge.relation === this.filterRelationType
        }
        
        if (headMatch && tailMatch && relationMatch) {
          filteredEdges.push(edge)
        }
      }
      
      var nodeIds = {}
      for (var k = 0; k < filteredEdges.length; k++) {
        var e = filteredEdges[k]
        nodeIds[e.source] = true
        nodeIds[e.target] = true
      }
      
      var filteredNodes = []
      for (var m = 0; m < this.allNodes.length; m++) {
        if (nodeIds[this.allNodes[m].id]) {
          filteredNodes.push(this.allNodes[m])
        }
      }
      
      this.renderGraph(filteredNodes, filteredEdges)
      this.$message.success('筛选完成，显示 ' + filteredEdges.length + ' 条关系，' + filteredNodes.length + ' 个节点')
    },
    
    resetFilter: function() {
      this.filterHeadType = ''
      this.filterHeadName = ''
      this.filterTailType = ''
      this.filterTailName = ''
      this.filterRelationType = ''
      this.renderGraph(this.allNodes, this.allEdges)
      this.$message.info('已重置筛选')
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
    
    getEdgeColor: function(relation) {
      return this.edgeColors[relation] || '#AAAAAA'
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
        'Feature': 'info',
        'AlloyDesignation': 'danger'
      }
      return tagMap[type] || 'info'
    }
  }
}
</script>

<style scoped>
.global-analysis {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 16px;
  gap: 16px;
  background: #f5f7fa;
  position: relative;
}

.analysis-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: white;
  padding: 12px 20px;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.08);
}

.header-title h2 {
  font-size: 18px;
  font-weight: 600;
  color: #1f2f3d;
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
  border-radius: 12px;
  border: 1px solid #e8eef2;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}

.graph-area {
  flex: 1;
  position: relative;
  overflow: hidden;
  background: #fafbfd;
}

.graph-container-wrapper {
  width: 100%;
  height: 100%;
  position: relative;
}

.graph-container {
  width: 100%;
  height: 100%;
  background: #fafbfd;
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
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.1);
  z-index: 10;
}

.graph-controls .el-button {
  width: 32px;
  height: 32px;
  padding: 0;
  color: #5a6e7c;
}

.right-sidebar {
  width: 420px;
  background: #ffffff;
  border-left: 1px solid #e8eef2;
  display: flex;
  flex-direction: column;
  position: relative;
  flex-shrink: 0;
  overflow: hidden;
}

.sidebar-resize {
  position: absolute;
  left: -3px;
  top: 0;
  width: 6px;
  height: 100%;
  cursor: ew-resize;
  z-index: 20;
  background: transparent;
}

.sidebar-resize:hover {
  background: #409EFF;
  opacity: 0.3;
}

.sidebar-content {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.sidebar-tabs {
  display: flex;
  background: #f8f9fa;
  border-radius: 10px;
  padding: 4px;
  margin-bottom: 16px;
}

.tab-btn {
  flex: 1;
  padding: 8px 0;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s;
  color: #8c9aa8;
  border-radius: 8px;
  font-size: 13px;
}

.tab-btn i {
  font-size: 14px;
  margin-right: 6px;
}

.tab-btn span {
  font-size: 13px;
}

.tab-btn:hover {
  background: #eef2f6;
  color: #409EFF;
}

.tab-btn.active {
  background: #409EFF;
  color: white;
  box-shadow: 0 2px 6px rgba(64,158,255,0.2);
}

.tab-content {
  flex: 1;
  overflow-y: auto;
  padding-bottom: 16px;
}

.chart-container {
  background: #f8f9fa;
  border-radius: 12px;
  padding: 12px;
  margin-bottom: 20px;
  border: 1px solid #eef2f6;
}

.chart-title {
  font-size: 13px;
  font-weight: 600;
  color: #2c3e50;
  text-align: center;
  margin-bottom: 8px;
}

.data-table-container {
  background: #f8f9fa;
  border-radius: 12px;
  border: 1px solid #eef2f6;
  overflow: hidden;
}

.table-title {
  padding: 10px 16px;
  font-size: 13px;
  font-weight: 600;
  color: #2c3e50;
  border-bottom: 1px solid #eef2f6;
  background: white;
}

.detail-panel {
  background: #f8f9fa;
  border-radius: 12px;
  border: 1px solid #eef2f6;
  overflow: hidden;
}

.detail-panel-header {
  padding: 12px 16px;
  background: white;
  border-bottom: 1px solid #eef2f6;
  font-size: 14px;
  font-weight: 600;
  color: #2c3e50;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.detail-panel-header i {
  margin-right: 8px;
  color: #409EFF;
}

.close-btn {
  padding: 0;
  font-size: 18px;
  color: #8c9aa8;
}

.detail-panel-body {
  padding: 16px;
}

.detail-row {
  margin-bottom: 14px;
  display: flex;
  align-items: flex-start;
}

.detail-label {
  width: 80px;
  color: #8c9aa8;
  font-size: 12px;
  flex-shrink: 0;
}

.detail-value {
  flex: 1;
  color: #2c3e50;
  font-size: 12px;
  font-weight: 500;
  word-break: break-word;
}

.detail-section {
  margin-top: 16px;
}

.section-title {
  font-size: 12px;
  font-weight: 600;
  color: #8c9aa8;
  margin-bottom: 10px;
  padding-bottom: 6px;
  border-bottom: 1px solid #eef2f6;
}

.section-title i {
  margin-right: 6px;
  color: #409EFF;
}

.sentences-list {
  max-height: 280px;
  overflow-y: auto;
}

.sentence-item {
  background: white;
  padding: 12px;
  margin-bottom: 10px;
  border-radius: 8px;
  font-size: 12px;
  line-height: 1.5;
  color: #5a6e7c;
  border-left: 3px solid #409EFF;
}

.sentence-literature {
  font-size: 11px;
  color: #409EFF;
  margin-bottom: 8px;
  padding-bottom: 6px;
  border-bottom: 1px dashed #eef2f6;
}

.sentence-literature i {
  margin-right: 6px;
}

.literature-id {
  font-weight: 500;
}

.sentence-text {
  line-height: 1.6;
  word-break: break-word;
}

.highlight-text {
  background-color: #ffeb3b;
  color: #333;
  padding: 0 2px;
  border-radius: 3px;
  font-weight: bold;
}

.empty-tip {
  text-align: center;
  padding: 60px 20px;
  color: #8c9aa8;
}

.empty-tip i {
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.empty-tip p {
  font-size: 13px;
  margin: 0;
}

.empty-state {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
  color: #8c9aa8;
}

.empty-state i {
  font-size: 48px;
  margin-bottom: 12px;
  opacity: 0.5;
}

/* 底部可上拉筛选面板 */

.filter-drawer {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: white;
  box-shadow: 0 -4px 20px rgba(0,0,0,0.15);
  z-index: 200;
  transition: height 0.1s ease;  /* 拖动时平滑过渡 */
  border-top: 2px solid #409EFF;
  height: 320px;  /* 默认高度，会被动态覆盖 */
}

/* 可拖动条 */
.filter-drawer-resize {
  position: absolute;
  top: -6px;
  left: 0;
  right: 0;
  height: 12px;
  cursor: ns-resize;
  z-index: 201;
  background: transparent;
}

.filter-drawer-resize:hover {
  background: rgba(64, 158, 255, 0.3);
}

.filter-drawer-resize::before {
  content: '';
  position: absolute;
  top: 3px;
  left: 50%;
  transform: translateX(-50%);
  width: 60px;
  height: 4px;
  background: #c0c4cc;
  border-radius: 2px;
  transition: all 0.2s;
}

.filter-drawer-resize:hover::before {
  background: #409EFF;
  width: 80px;
}

.filter-drawer-header {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 14px 24px;
  cursor: pointer;
  background: linear-gradient(135deg, #f8f9fa 0%, #ffffff 100%);
  transition: all 0.2s;
  position: relative;
  margin-top: 6px;  /* 为拖动条留出空间 */
}


.filter-drawer-header:hover {
  background: linear-gradient(135deg, #eef2f6 0%, #f8f9fa 100%);
}

.filter-drawer-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 15px;
  font-weight: 600;
  color: #2c3e50;
  background: #ecf5ff;
  padding: 6px 24px;
  border-radius: 30px;
  box-shadow: 0 1px 4px rgba(64,158,255,0.2);
}

.filter-drawer-title i {
  color: #409EFF;
  font-size: 18px;
}

.filter-badge {
  background: #F56C6C;
  color: white;
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 20px;
  font-weight: normal;
  animation: pulse 1s ease-in-out;
}

@keyframes pulse {
  0% { transform: scale(1); }
  50% { transform: scale(1.1); }
  100% { transform: scale(1); }
}

.filter-drawer-toggle {
  position: absolute;
  right: 24px;
  top: 50%;
  transform: translateY(-50%);
  color: #8c9aa8;
  font-size: 14px;
}

.filter-drawer-content {
  padding: 16px 24px 20px;
  border-top: 1px solid #e8eef2;
  background: white;
  height: calc(100% - 60px);  /* 动态计算高度，减去头部高度 */
  overflow-y: auto;
}

.filter-drawer:not(.expanded) {
  height: auto !important;
}

.filter-drawer:not(.expanded) .filter-drawer-content {
  display: none;
}

.filter-drawer:not(.expanded) .filter-drawer-resize {
  display: none;  /* 收起时隐藏拖动条 */
}

/* 初始状态 - 默认展开 */
.filter-drawer .filter-drawer-content {
  display: block;
}

/* 收起状态 */
.filter-drawer:not(.expanded) .filter-drawer-content {
  display: none;
}

.filter-form {
  margin-bottom: 8px;
}

.filter-group {
  background: #f8f9fa;
  padding: 12px;
  border-radius: 8px;
  height: 100%;
}

.filter-group-title {
  font-size: 12px;
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 8px;
}

.filter-actions {
  padding-top: 28px;
}

.filter-actions .el-button--primary {
  background-color: #409EFF;
  border-color: #409EFF;
}

.filter-tags {
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid #eef2f6;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.filter-tags-label {
  font-size: 12px;
  color: #8c9aa8;
}

.sentences-list::-webkit-scrollbar,
.detail-panel-body::-webkit-scrollbar {
  width: 6px;
}

.sentences-list::-webkit-scrollbar-track,
.detail-panel-body::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.sentences-list::-webkit-scrollbar-thumb,
.detail-panel-body::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}
</style>