<template>
  <div class="node-analysis">
    <!-- 页面头部 -->
    <div class="analysis-header">
      <div class="header-title">
        <h2>结点分析</h2>
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
      <!-- 左侧搜索面板 -->
      <div class="search-panel" :style="{ width: searchPanelWidth + 'px' }">
        <div class="panel-resize" @mousedown="startResize"></div>
        <div class="panel-content">
          <div class="panel-title">
            <i class="el-icon-search"></i> 查询范围
          </div>
          
          <div class="search-section">
            <div class="section-label">中心结点名称</div>
            <el-select 
              v-model="selectedNodeName" 
              placeholder="请选择中心结点"
              size="small"
              filterable
              clearable
              style="width: 100%"
            >
              <el-option
                v-for="node in nodeOptions"
                :key="node.value"
                :label="node.label"
                :value="node.value"
              >
                <div class="node-option">
                  <span>{{ node.label }}</span>
                  <el-tag size="mini" :type="getNodeTypeTag(node.type)" style="margin-left: 8px;">{{ node.type }}</el-tag>
                  <span class="node-degree">度: {{ node.degree }}</span>
                </div>
              </el-option>
            </el-select>
          </div>
          
          <div class="search-section">
            <div class="section-label">目标结点类型</div>
            <el-select 
              v-model="selectedNodeTypes" 
              placeholder="选择目标结点类型"
              size="small"
              multiple
              collapse-tags
              clearable
              style="width: 100%"
            >
              <el-option v-for="type in nodeTypeList" :key="type" :label="type" :value="type"></el-option>
            </el-select>
          </div>
          
          <div class="search-section">
            <div class="section-label">跳数</div>
            <el-slider v-model="hopDepth" :min="1" :max="3" :step="1" :marks="{1: '1跳', 2: '2跳', 3: '3跳'}" style="width: 100%;"></el-slider>
          </div>
          
          <div class="search-section">
            <div class="section-label">方向</div>
            <el-radio-group v-model="direction" size="small">
              <el-radio-button label="both">双向</el-radio-button>
              <el-radio-button label="out">正向</el-radio-button>
              <el-radio-button label="in">反向</el-radio-button>
            </el-radio-group>
          </div>
          
          <div class="search-actions">
            <el-button type="primary" size="small" @click="searchNode" :disabled="!selectedNodeName" style="width: 100%;">
              <i class="el-icon-search"></i> 搜索
            </el-button>
          </div>
        </div>
      </div>

      <!-- 图谱区域 -->
      <div class="graph-area">
        <div v-if="hasGraphData" class="graph-container-wrapper">
          <div id="node-graph-container" class="graph-container"></div>
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
          <p>请选择数据库后点击"加载图谱数据"，然后选择结点进行搜索</p>
        </div>
      </div>

      <!-- 右侧边栏 -->
      <div class="right-sidebar" :style="{ width: sidebarWidth + 'px' }">
        <div class="sidebar-resize" @mousedown="startSidebarResize"></div>
        <div class="sidebar-content">
          <div class="sidebar-tabs">
            <div class="tab-btn" :class="{ active: activeTab === 'overview' }" @click="setActiveTab('overview')">
              <i class="el-icon-data-line"></i><span>Overview</span>
            </div>
            <div class="tab-btn" :class="{ active: activeTab === 'entity' }" @click="setActiveTab('entity')">
              <i class="el-icon-document"></i><span>结点</span>
            </div>
            <div class="tab-btn" :class="{ active: activeTab === 'relation' }" @click="setActiveTab('relation')">
              <i class="el-icon-share"></i><span>关系</span>
            </div>
          </div>

          <!-- Overview 内容 -->
          <div v-show="activeTab === 'overview'" class="tab-content">
            <div class="stats-card">
              <div class="stats-header">当前查询信息</div>
              <div class="stats-body">
                <div class="stat-row"><span class="stat-label">中心结点</span><span class="stat-value">{{ selectedNodeName || '-' }}</span></div>
                <div class="stat-row"><span class="stat-label">展开深度</span><span class="stat-value">{{ hopDepth }}跳</span></div>
                <div class="stat-row"><span class="stat-label">方向</span><span class="stat-value">{{ direction === 'both' ? '双向' : (direction === 'out' ? '正向' : '反向') }}</span></div>
                <div class="stat-row"><span class="stat-label">节点数量</span><span class="stat-value">{{ subgraphNodes.length }}</span></div>
                <div class="stat-row"><span class="stat-label">关系数量</span><span class="stat-value">{{ subgraphEdges.length }}</span></div>
              </div>
            </div>
            <div class="stats-card">
              <div class="stats-header">数据库统计</div>
              <div class="stats-body">
                <div class="stat-row"><span class="stat-label">总节点数</span><span class="stat-value">{{ allNodes.length }}</span></div>
                <div class="stat-row"><span class="stat-label">总关系数</span><span class="stat-value">{{ allEdges.length }}</span></div>
              </div>
            </div>
          </div>

          <!-- 结点详情内容 -->
          <div v-show="activeTab === 'entity'" class="tab-content">
            <div v-if="selectedNode" class="detail-panel">
              <div class="detail-panel-header">
                <i class="el-icon-info"></i> 结点详情
                <el-button type="text" size="small" @click="clearNodeSelection" class="close-btn">×</el-button>
              </div>
              <div class="detail-panel-body">
                <div class="detail-row"><span class="detail-label">结点名称</span><span class="detail-value">{{ selectedNode.name }}</span></div>
                <div class="detail-row"><span class="detail-label">结点类型</span><el-tag size="small" :type="getNodeTypeTag(selectedNode.type)">{{ selectedNode.type || 'unknown' }}</el-tag></div>
                <div class="detail-row"><span class="detail-label">度数</span><span class="detail-value">{{ selectedNode.degree || 0 }}</span></div>
                <div class="detail-section" v-if="selectedNode.sentences && selectedNode.sentences.length">
                  <div class="section-title"><i class="el-icon-document"></i> 来源文献</div>
                  <div class="sentences-list">
                    <div v-for="(item, idx) in selectedNode.sentences" :key="idx" class="sentence-item">
                      <div class="sentence-literature"><i class="el-icon-document-copy"></i> <span class="literature-id">文献: {{  item.literatureTitle || item.literatureId || '未知' }}</span></div>
                      <div class="sentence-text" v-html="highlightText(item.sentenceText || item, selectedNode.name)"></div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            <div v-else class="empty-tip"><i class="el-icon-info"></i><p>请点击图谱中的结点查看详情</p></div>
          </div>

          <!-- 关系详情内容 -->
          <div v-show="activeTab === 'relation'" class="tab-content">
            <div v-if="selectedEdge" class="detail-panel">
              <div class="detail-panel-header">
                <i class="el-icon-share"></i> 关系详情
                <el-button type="text" size="small" @click="clearEdgeSelection" class="close-btn">×</el-button>
              </div>
              <div class="detail-panel-body">
                <div class="detail-row"><span class="detail-label">关系类型</span><el-tag size="small" :style="{ backgroundColor: getEdgeColor(selectedEdge.relation), color: '#fff', border: 'none' }">{{ selectedEdge.relation }}</el-tag></div>
                <div class="detail-row"><span class="detail-label">头实体</span><span class="detail-value">{{ getNodeNameById(selectedEdge.source) }}</span></div>
                <div class="detail-row"><span class="detail-label">尾实体</span><span class="detail-value">{{ getNodeNameById(selectedEdge.target) }}</span></div>
                <div class="detail-section" v-if="selectedEdge.sentences && selectedEdge.sentences.length">
                  <div class="section-title"><i class="el-icon-document"></i> 来源文献</div>
                  <div class="sentences-list">
                    <div v-for="(item, idx) in selectedEdge.sentences" :key="idx" class="sentence-item">
                      <div class="sentence-literature"><i class="el-icon-document-copy"></i> <span class="literature-id">文献: {{  item.literatureTitle || item.literatureId  || '未知' }}</span></div>
                      <div class="sentence-text">{{ item.sentenceText || item }}</div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            <div v-else class="empty-tip"><i class="el-icon-info"></i><p>请点击图谱中的关系边查看详情</p></div>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部筛选面板 -->
    <div class="filter-drawer" :class="{ expanded: filterDrawerExpanded }">
      <div class="filter-drawer-header" @click="toggleFilterDrawer">
        <div class="filter-drawer-title"><i class="el-icon-s-operation"></i><span>高级筛选</span><span v-if="hasActiveFilter" class="filter-badge">已筛选</span></div>
        <div class="filter-drawer-toggle"><i :class="filterDrawerExpanded ? 'el-icon-arrow-down' : 'el-icon-arrow-up'"></i></div>
      </div>
      <div class="filter-drawer-content" v-show="filterDrawerExpanded">
        <div class="filter-form">
          <el-row :gutter="16">
            <el-col :span="8"><div class="filter-group"><div class="filter-group-title">头结点属性</div><el-select v-model="filterHeadType" placeholder="选择节点类型" clearable filterable size="small"><el-option v-for="type in nodeTypeList" :key="type" :label="type" :value="type"></el-option></el-select><el-input v-model="filterHeadName" placeholder="节点名称关键字" clearable size="small" style="margin-top: 8px;"></el-input></div></el-col>
            <el-col :span="8"><div class="filter-group"><div class="filter-group-title">尾结点属性</div><el-select v-model="filterTailType" placeholder="选择节点类型" clearable filterable size="small"><el-option v-for="type in nodeTypeList" :key="type" :label="type" :value="type"></el-option></el-select><el-input v-model="filterTailName" placeholder="节点名称关键字" clearable size="small" style="margin-top: 8px;"></el-input></div></el-col>
            <el-col :span="8"><div class="filter-group"><div class="filter-group-title">关系类型</div><el-select v-model="filterRelationType" placeholder="选择关系类型" clearable filterable size="small"><el-option v-for="type in relationTypeList" :key="type" :label="type" :value="type"></el-option></el-select></div></el-col>
          </el-row>
          <div class="filter-actions-bottom"><el-button type="primary" size="small" @click="applyAdvancedFilter">应用筛选</el-button><el-button size="small" @click="resetFilter">重置</el-button></div>
          <div class="filter-tags" v-if="hasActiveFilter"><span class="filter-tags-label">当前筛选：</span><el-tag v-if="filterHeadType" size="small" closable @close="filterHeadType = ''; applyAdvancedFilter()">头类型: {{ filterHeadType }}</el-tag><el-tag v-if="filterHeadName" size="small" closable @close="filterHeadName = ''; applyAdvancedFilter()">头名称: {{ filterHeadName }}</el-tag><el-tag v-if="filterTailType" size="small" closable @close="filterTailType = ''; applyAdvancedFilter()">尾类型: {{ filterTailType }}</el-tag><el-tag v-if="filterTailName" size="small" closable @close="filterTailName = ''; applyAdvancedFilter()">尾名称: {{ filterTailName }}</el-tag><el-tag v-if="filterRelationType" size="small" closable @close="filterRelationType = ''; applyAdvancedFilter()">关系: {{ filterRelationType }}</el-tag></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import G6 from '@antv/g6'
import client from '@/api/client'

export default {
  name: 'NodeAnalysis',
  data: function() {
    return {
      graph: null,
      loading: false,
      hasGraphData: false,
      hasSubgraphData: false,
      activeTab: 'overview',
      selectedDatabase: 'xai',
      
      searchPanelWidth: 280,
      sidebarWidth: 360,
      filterDrawerExpanded: false,
      
      selectedNodeName: '',
      selectedNodeTypes: [],
      hopDepth: 1,
      direction: 'both',
      
      allNodes: [],
      allEdges: [],
      nodeOptions: [],
      nodeTypeList: [],
      relationTypeList: [],
      
      subgraphNodes: [],
      subgraphEdges: [],
      
      filterHeadType: '',
      filterHeadName: '',
      filterTailType: '',
      filterTailName: '',
      filterRelationType: '',
      
      selectedNodeId: null,
      selectedNode: null,
      selectedEdgeKey: null,
      selectedEdge: null,
      
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
    hasActiveFilter: function() {
      return this.filterHeadType || this.filterHeadName || this.filterTailType || this.filterTailName || this.filterRelationType
    }
  },

  mounted: function() {
    this.registerCustomNode()
    this.initGraph()
    window.addEventListener('resize', this.handleResize)
  },

  beforeDestroy: function() {
    if (this.graph) {
      this.graph.destroy()
    }
    window.removeEventListener('resize', this.handleResize)
  },

  methods: {
    highlightText: function(text, keyword) {
      if (!text) return ''
      if (!keyword || keyword === '') return text
      var regex = new RegExp('(' + this.escapeRegExp(keyword) + ')', 'gi')
      return text.replace(regex, '<span class="highlight-text">$1</span>')
    },
    
    escapeRegExp: function(str) {
      return str.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
    },
    
    toggleFilterDrawer: function() {
      this.filterDrawerExpanded = !this.filterDrawerExpanded
    },
    
    startResize: function(e) {
      var self = this
      var startX = e.clientX
      var startWidth = this.searchPanelWidth
      var onMouseMove = function(moveEvent) {
        var newWidth = startWidth + (moveEvent.clientX - startX)
        if (newWidth >= 200 && newWidth <= 400) {
          self.searchPanelWidth = newWidth
        }
      }
      var onMouseUp = function() {
        document.removeEventListener('mousemove', onMouseMove)
        document.removeEventListener('mouseup', onMouseUp)
      }
      document.addEventListener('mousemove', onMouseMove)
      document.addEventListener('mouseup', onMouseUp)
    },
    
    startSidebarResize: function(e) {
      var self = this
      var startX = e.clientX
      var startWidth = this.sidebarWidth
      var onMouseMove = function(moveEvent) {
        var newWidth = startWidth - (moveEvent.clientX - startX)
        if (newWidth >= 300 && newWidth <= 500) {
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
      this.activeTab = tab
    },
    
    registerCustomNode: function() {
  var self = this
  G6.registerNode('circle-with-text', {
    draw: function(cfg, group) {
      var size = cfg.size || 70
      var isCenter = cfg.isCenter || false
      var color = (cfg.style && cfg.style.fill) || '#5B8FF9'
      var text = cfg.label || ''
      
      if (isCenter) {
        group.addShape('circle', {
          attrs: { x: 0, y: 0, r: size / 2 + 8, fill: color, opacity: 0.3, cursor: 'pointer' },
          name: 'outer-glow'
        })
      }
      
      group.addShape('circle', {
        attrs: { x: 0, y: 0, r: size / 2 + 3, fill: color, opacity: 0.2, cursor: 'pointer' },
        name: 'glow-shape'
      })
      
      var keyShape = group.addShape('circle', {
        attrs: { x: 0, y: 0, r: size / 2, fill: color, stroke: '#ffffff', lineWidth: 2, cursor: 'pointer', shadowColor: 'rgba(0,0,0,0.2)', shadowBlur: 6 },
        name: 'circle-shape'
      })
      
      if (text && text.length > 0) {
        var fontSize = isCenter ? 14 : 12
        if (text.length > 6 && !isCenter) fontSize = 11
        if (text.length > 8 && !isCenter) fontSize = 10
        group.addShape('text', {
          attrs: { x: 0, y: 0, text: text, fill: '#ffffff', fontSize: fontSize, fontWeight: 'bold', textAlign: 'center', textBaseline: 'middle', shadowColor: 'rgba(0,0,0,0.3)', shadowBlur: 2 },
          name: 'text-shape'
        })
      }
      return keyShape
    },
    getAnchorPoints: function() {
      return [[0, 0.5], [1, 0.5], [0.5, 0], [0.5, 1]]
    }
  }, 'circle')
},
    
    initGraph: function() {
      var container = document.getElementById('node-graph-container')
      var self = this
      if (!container) {
        setTimeout(function() { if (document.getElementById('node-graph-container')) { self.initGraph() } }, 100)
        return
      }
      
      if (this.graph) {
        this.graph.destroy()
        this.graph = null
      }
      
      var vueInstance = this
      
      this.graph = new G6.Graph({
        container: 'node-graph-container',
        width: container.clientWidth,
        height: container.clientHeight,
        layout: {
          type: 'force',
          preventOverlap: true,
          linkDistance: 180,
          nodeStrength: 80,
          edgeStrength: 0.2,
          collideStrength: 0.8
        },
        defaultNode: {
          type: 'circle-with-text',
          size: 70,
          style: { fill: '#5B8FF9', stroke: '#ffffff', lineWidth: 2, cursor: 'pointer' },
          labelCfg: { style: { fill: '#ffffff', fontSize: 12, fontWeight: 'bold' }, position: 'center' }
        },
        defaultEdge: {
           type: 'line',
  style: {
    stroke: '#aaa',
    lineWidth: 2,
    endArrow: true,
  },
          labelCfg: {
            autoRotate: true,
            style: { fill: '#666', fontSize: 10, background: { fill: 'rgba(255,255,255,0.9)', padding: [2, 4, 2, 4], radius: 4 } }
          }
        },
        modes: { default: ['drag-canvas', 'zoom-canvas', 'drag-node'] },
        nodeStateStyles: { hover: { stroke: '#ffa940', lineWidth: 3, shadowBlur: 15, shadowColor: '#ffa940' }, selected: { stroke: '#ffa940', lineWidth: 3, shadowBlur: 15, shadowColor: '#ffa940' } },
        edgeStateStyles: { hover: { lineWidth: 3, opacity: 1 } }
      })
      
      this.graph.on('node:click', function(evt) {
        var nodeModel = evt.item.getModel()
        var fullNodeData = null
        for (var i = 0; i < vueInstance.subgraphNodes.length; i++) {
          if (vueInstance.subgraphNodes[i].id === nodeModel.id) {
            fullNodeData = vueInstance.subgraphNodes[i]
            break
          }
        }
        if (fullNodeData) { vueInstance.selectNode(fullNodeData) }
      })
      
      this.graph.on('edge:click', function(evt) {
        var edgeModel = evt.item.getModel()
        var fullEdgeData = null
        for (var i = 0; i < vueInstance.subgraphEdges.length; i++) {
          var e = vueInstance.subgraphEdges[i]
          if (e.source === edgeModel.source && e.target === edgeModel.target && e.relation === edgeModel.relation) {
            fullEdgeData = e
            break
          }
        }
        if (fullEdgeData) { vueInstance.selectRelation(fullEdgeData) }
      })
      
      this.graph.on('canvas:click', function() {
        vueInstance.clearNodeSelection()
        vueInstance.clearEdgeSelection()
      })
    },
    
    getNodeNameById: function(nodeId) {
      for (var i = 0; i < this.allNodes.length; i++) {
        if (this.allNodes[i].id === nodeId) { return this.allNodes[i].name }
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
          setTimeout(function() { if (self.graph) { self.graph.setItemState(nodeItem, 'selected', false) } }, 2000)
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
            setTimeout(function() { if (self.graph) { self.graph.setItemState(e, 'hover', false) } }, 2000)
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
        var container = document.getElementById('node-graph-container')
        if (container) {
          this.graph.changeSize(container.clientWidth, container.clientHeight)
          this.graph.fitView()
        }
      }
    },

    loadGraphData: function() {
      var self = this
      if (!this.selectedDatabase) {
        this.$message.warning('请先选择数据库')
        return
      }
      this.loading = true
      client.get('/api/knowledge-graph/all-graph').then(function(response) {
        if (response.data && response.data.code === 200) {
          var graphData = response.data.data
          if (graphData.nodes && graphData.nodes.length > 0) {
            self.allNodes = graphData.nodes || []
            self.allEdges = graphData.edges || []
            self.buildNodeOptions()
            self.extractStatistics()
            self.hasGraphData = true
            if (!self.graph) { self.initGraph() }
            self.$message.success('加载成功，共 ' + graphData.nodes.length + ' 个节点，' + graphData.edges.length + ' 条关系')
          } else { self.$message.warning('暂无图谱数据') }
        } else { self.$message.error('获取图谱数据失败') }
        self.loading = false
      }).catch(function(error) {
        console.error('加载图谱失败:', error)
        self.$message.error('加载图谱失败')
        self.loading = false
      })
    },
    
    buildNodeOptions: function() {
      var degreeMap = {}
      for (var i = 0; i < this.allEdges.length; i++) {
        var edge = this.allEdges[i]
        degreeMap[edge.source] = (degreeMap[edge.source] || 0) + 1
        degreeMap[edge.target] = (degreeMap[edge.target] || 0) + 1
      }
      var options = []
      for (var j = 0; j < this.allNodes.length; j++) {
        var node = this.allNodes[j]
        options.push({ value: node.name, label: node.name, type: node.type, degree: degreeMap[node.name] || 0 })
      }
      options.sort(function(a, b) { return b.degree - a.degree })
      this.nodeOptions = options
    },
    
    extractStatistics: function() {
      this.entityTypeCount = {}
      this.relationTypeCount = {}
      this.nodeTypeList = []
      this.relationTypeList = []
      for (var i = 0; i < this.allNodes.length; i++) {
        var type = this.allNodes[i].type || 'unknown'
        this.entityTypeCount[type] = (this.entityTypeCount[type] || 0) + 1
      }
      for (var j = 0; j < this.allEdges.length; j++) {
        var relation = this.allEdges[j].relation
        if (relation) { this.relationTypeCount[relation] = (this.relationTypeCount[relation] || 0) + 1 }
      }
      this.nodeTypeList = Object.keys(this.entityTypeCount)
      this.relationTypeList = Object.keys(this.relationTypeCount)
    },
    
   searchNode: function() {
  if (!this.selectedNodeName) {
    this.$message.warning('请选择结点')
    return
  }
  
  var self = this
  var centerName = this.selectedNodeName
  
  var directEdges = []
  for (var i = 0; i < this.allEdges.length; i++) {
    var edge = this.allEdges[i]
    if (edge.source === centerName || edge.target === centerName) {
      directEdges.push(edge)
    }
  }
  
  console.log('与中心节点相连的边:', directEdges.length)
  
  var neighborNodes = []
  for (var j = 0; j < directEdges.length; j++) {
    var edge = directEdges[j]
    var neighborName = (edge.source === centerName) ? edge.target : edge.source
    var neighborNode = this.allNodes.find(function(n) { return n.name === neighborName })
    if (neighborNode) {
      neighborNodes.push(neighborNode)
    }
  }
  
  var filteredNeighbors = neighborNodes
  var filteredEdges = directEdges
  
  if (this.selectedNodeTypes && this.selectedNodeTypes.length > 0) {
    filteredNeighbors = neighborNodes.filter(function(node) {
      return self.selectedNodeTypes.indexOf(node.type) !== -1
    })
    
    var neighborNameSet = {}
    for (var k = 0; k < filteredNeighbors.length; k++) {
      neighborNameSet[filteredNeighbors[k].name] = true
    }
    
    filteredEdges = directEdges.filter(function(edge) {
      var neighborName = (edge.source === centerName) ? edge.target : edge.source
      return neighborNameSet[neighborName]
    })
  }
  
  var centerNodeObj = this.allNodes.find(function(n) { return n.name === centerName })
  var finalNodes = [centerNodeObj].concat(filteredNeighbors)
  
  this.subgraphNodes = finalNodes
  this.subgraphEdges = filteredEdges
  this.hasSubgraphData = true
  
  console.log('最终节点数:', finalNodes.length, '边数:', filteredEdges.length)
  
  this.renderGraph(this.subgraphNodes, this.subgraphEdges)
  this.$message.success('找到 ' + filteredNeighbors.length + ' 个符合条件的节点，' + filteredEdges.length + ' 条关系')
},
    
    renderGraph: function(nodes, edges) {
      if (!this.graph) return
      var self = this
      var centerNodeName = this.selectedNodeName
      
      if (!nodes || nodes.length === 0) {
        this.graph.clear()
        this.graph.data({ nodes: [], edges: [] })
        this.graph.render()
        return
      }
      
      var hasCenterNode = false
      for (var i = 0; i < nodes.length; i++) {
        if (nodes[i].name === centerNodeName) {
          hasCenterNode = true
          break
        }
      }
      
      if (!hasCenterNode && centerNodeName) {
        for (var j = 0; j < this.allNodes.length; j++) {
          if (this.allNodes[j].name === centerNodeName) {
            nodes.push(this.allNodes[j])
            break
          }
        }
      }
      
      var graphNodes = []
      for (var k = 0; k < nodes.length; k++) {
        var node = nodes[k]
        var isCenter = (node.name === centerNodeName)
        
        var size = 60
        if (isCenter) {
          size = 90
        } else {
          var pr = node.pageRank || 0
          size = 55 + pr * 20
          if (size > 75) size = 75
          if (size < 50) size = 50
        }
        
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
          isCenter: isCenter,
          style: { fill: self.getNodeColor(node.type) },
          labelCfg: { style: { fill: '#ffffff', fontSize: isCenter ? 14 : 12, fontWeight: 'bold' } }
        })
      }
      
      var graphEdges = []
      for (var m = 0; m < edges.length; m++) {
        var edge = edges[m]
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
        if (self.graph && !self.graph.destroyed && centerNodeName) {
          var allNodes = self.graph.getNodes()
          for (var n = 0; n < allNodes.length; n++) {
            var nodeModel = allNodes[n].getModel()
            if (nodeModel.name === centerNodeName) {
              self.graph.setItemState(allNodes[n], 'selected', true)
              setTimeout(function() {
                if (self.graph && !self.graph.destroyed) {
                  self.graph.setItemState(allNodes[n], 'selected', false)
                }
              }, 2000)
              break
            }
          }
          self.graph.fitView()
        }
      }, 300)
    },
    
    applyAdvancedFilter: function() {
  if (!this.hasSubgraphData) { 
    this.$message.warning('请先搜索结点'); 
    return 
  }
  
  var self = this
  var centerNodeName = this.selectedNodeName
  
  var centerNodeObj = null
  for (var i = 0; i < this.subgraphNodes.length; i++) {
    if (this.subgraphNodes[i].name === centerNodeName) {
      centerNodeObj = this.subgraphNodes[i]
      break
    }
  }
  
  if (!centerNodeObj) {
    this.$message.warning('未找到中心结点')
    return
  }
  
  var selectedTypes = this.selectedNodeTypes
  
  console.log('筛选的结点类型:', selectedTypes)
  console.log('中心结点:', centerNodeObj.name, centerNodeObj.type)
  
  if (!selectedTypes || selectedTypes.length === 0) {
    this.renderGraph(this.subgraphNodes, this.subgraphEdges)
    return
  }
  
  var filteredEdges = []
  var connectedNodeIds = {}
  connectedNodeIds[centerNodeObj.id] = true
  
  for (var j = 0; j < this.subgraphEdges.length; j++) {
    var edge = this.subgraphEdges[j]
    
    var isFromCenter = (edge.source === centerNodeObj.id)
    var isToCenter = (edge.target === centerNodeObj.id)
    
    if (!isFromCenter && !isToCenter) continue
    
    var otherNodeId = isFromCenter ? edge.target : edge.source
    var otherNode = null
    for (var k = 0; k < this.subgraphNodes.length; k++) {
      if (this.subgraphNodes[k].id === otherNodeId) {
        otherNode = this.subgraphNodes[k]
        break
      }
    }
    if (!otherNode) continue
    
    var typeMatched = (selectedTypes.indexOf(otherNode.type) !== -1)
    
    console.log('检查边:', edge.source, '->', edge.target, '另一端类型:', otherNode.type, '匹配:', typeMatched)
    
    if (typeMatched) {
      filteredEdges.push(edge)
      connectedNodeIds[otherNodeId] = true
    }
  }
  
  var filteredNodes = []
  for (var m = 0; m < this.subgraphNodes.length; m++) {
    if (connectedNodeIds[this.subgraphNodes[m].id]) {
      filteredNodes.push(this.subgraphNodes[m])
    }
  }
  
  console.log('筛选结果 - 边数:', filteredEdges.length, '结点数:', filteredNodes.length)
  
  if (filteredEdges.length === 0) {
    this.$message.warning('没有类型为 ' + selectedTypes.join(',') + ' 的相邻结点')
    this.renderGraph([centerNodeObj], [])
    return
  }
  
  this.renderGraph(filteredNodes, filteredEdges)
  this.$message.success('筛选完成，显示 ' + filteredEdges.length + ' 条关系，' + filteredNodes.length + ' 个结点')
},
    
    resetFilter: function() {
      this.filterHeadType = ''
      this.filterHeadName = ''
      this.filterTailType = ''
      this.filterTailName = ''
      this.filterRelationType = ''
      if (this.subgraphNodes.length > 0) { this.renderGraph(this.subgraphNodes, this.subgraphEdges) }
      this.$message.info('已重置筛选')
    },
    
    fitView: function() { if (this.graph) { this.graph.fitView() } },
    zoomIn: function() { if (this.graph) { var z = this.graph.getZoom(); this.graph.zoomTo(z * 1.2, { x: this.graph.getWidth() / 2, y: this.graph.getHeight() / 2 }) } },
    zoomOut: function() { if (this.graph) { var z = this.graph.getZoom(); this.graph.zoomTo(z * 0.8, { x: this.graph.getWidth() / 2, y: this.graph.getHeight() / 2 }) } },
    
    getNodeColor: function(type) { return this.nodeColors[type] || this.nodeColors['unknown'] },
    getEdgeColor: function(relation) { return this.edgeColors[relation] || '#AAAAAA' },
    
    getNodeTypeTag: function(type) {
      var tagMap = { 'Composition': 'primary', 'Structure': 'success', 'Property': 'warning', 'Application': 'danger', 'Processing': 'info', 'Characterization': 'success', 'Condition': 'warning', 'Feature': 'info', 'AlloyDesignation': 'danger' }
      return tagMap[type] || 'info'
    }
  }
}
</script>

<style scoped>
.node-analysis {
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

.search-panel {
  width: 280px;
  background: #f8f9fa;
  border-right: 1px solid #e8eef2;
  display: flex;
  flex-direction: column;
  position: relative;
  flex-shrink: 0;
}

.panel-resize {
  position: absolute;
  right: -3px;
  top: 0;
  width: 6px;
  height: 100%;
  cursor: ew-resize;
  z-index: 20;
  background: transparent;
}

.panel-resize:hover {
  background: #409EFF;
  opacity: 0.3;
}

.panel-content {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.panel-title {
  font-size: 16px;
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 2px solid #409EFF;
}

.panel-title i {
  margin-right: 8px;
  color: #409EFF;
}

.search-section {
  margin-bottom: 20px;
}

.section-label {
  font-size: 13px;
  font-weight: 500;
  color: #5a6e7c;
  margin-bottom: 8px;
}

.search-actions {
  margin-top: 20px;
  padding-top: 12px;
  border-top: 1px solid #e8eef2;
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
  width: 360px;
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

.stats-card {
  background: #f8f9fa;
  border-radius: 12px;
  margin-bottom: 16px;
  border: 1px solid #eef2f6;
  overflow: hidden;
}

.stats-header {
  padding: 12px 16px;
  font-size: 13px;
  font-weight: 600;
  color: #2c3e50;
  border-bottom: 1px solid #eef2f6;
  background: white;
}

.stats-body {
  padding: 12px 16px;
}

.stat-row {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid #eef2f6;
  font-size: 12px;
}

.stat-row:last-child {
  border-bottom: none;
}

.stat-label {
  color: #8c9aa8;
}

.stat-value {
  color: #2c3e50;
  font-weight: 500;
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

.filter-drawer {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: white;
  box-shadow: 0 -2px 12px rgba(0,0,0,0.1);
  z-index: 200;
  transition: all 0.3s ease;
  border-top: 2px solid #409EFF;
}

.filter-drawer-header {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 12px 24px;
  cursor: pointer;
  background: linear-gradient(135deg, #f8f9fa 0%, #ffffff 100%);
  transition: all 0.2s;
  position: relative;
}

.filter-drawer-header:hover {
  background: linear-gradient(135deg, #eef2f6 0%, #f8f9fa 100%);
}

.filter-drawer-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  font-weight: 600;
  color: #2c3e50;
  background: #ecf5ff;
  padding: 4px 20px;
  border-radius: 30px;
}

.filter-drawer-title i {
  color: #409EFF;
  font-size: 16px;
}

.filter-badge {
  background: #F56C6C;
  color: white;
  font-size: 10px;
  padding: 2px 8px;
  border-radius: 20px;
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
  padding: 16px 24px;
  border-top: 1px solid #e8eef2;
  background: white;
  max-height: 260px;
  overflow-y: auto;
}

.filter-drawer:not(.expanded) .filter-drawer-content {
  display: none;
}

.filter-group {
  background: #f8f9fa;
  padding: 12px;
  border-radius: 8px;
}

.filter-group-title {
  font-size: 12px;
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 8px;
}

.filter-actions-bottom {
  text-align: center;
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid #eef2f6;
}

.filter-actions-bottom .el-button {
  margin: 0 8px;
}

.filter-tags {
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px solid #eef2f6;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.filter-tags-label {
  font-size: 11px;
  color: #8c9aa8;
}

.node-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.node-degree {
  font-size: 11px;
  color: #999;
  margin-left: 8px;
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