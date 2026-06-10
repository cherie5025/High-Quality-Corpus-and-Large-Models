<template>
  <div class="overall-credibility" v-loading="loading">
    <!-- 顶部导航按钮组：四个可信度页面切换，复古风格按钮 -->
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

    <!-- ========== 新增：子路由出口，传递 paperId ========== -->
    <router-view :paperId="currentId"></router-view>

    <!-- 折叠面板：综合可信度计算方法（复古风格） -->
    <el-collapse class="retro-collapse" style="margin-bottom: 20px;">
      <el-collapse-item>
        <template slot="title">
          <i class="el-icon-info" style="color: #3C848C; margin-right: 8px;"></i>
          <span class="collapse-title">📐 综合可信度计算方法</span>
        </template>
        <div class="collapse-content">
          <!-- 内容保持不变 -->
          <el-row :gutter="20" style="margin-bottom: 16px;">
            <el-col :span="24">
              <div class="inner-card">
                <h4 class="inner-card-title">📌 融合公式</h4>
                <p class="formula">
                  C = w<sub>c1</sub>·C<sub>time</sub> + w<sub>c2</sub>·C<sub>print</sub> + w<sub>c3</sub>·C<sub>author</sub>
                </p>
                <el-divider class="retro-divider"></el-divider>
                <el-row :gutter="20">
                  <el-col :span="8">
                    <p class="rule-title">⏱️ 时效可信度 C<sub>time</sub></p>
                    <p class="rule-desc">基于发表日期归一化</p>
                  </el-col>
                  <el-col :span="8">
                    <p class="rule-title">📄 权威可信度 C<sub>print</sub></p>
                    <p class="rule-desc">基于期刊IF/会议等级+文献类型</p>
                  </el-col>
                  <el-col :span="8">
                    <p class="rule-title">👥 学术可信度 C<sub>author</sub></p>
                    <p class="rule-desc">基于作者合作网络与贡献度</p>
                  </el-col>
                </el-row>
                <p class="note">* 权重 w 由熵权法根据三个指标在整个数据集上的离散程度确定。</p>
              </div>
            </el-col>
          </el-row>
          <!-- 熵权法步骤 -->
          <el-row :gutter="20">
            <el-col :span="12">
              <div class="inner-card">
                <h4 class="inner-card-title">⚖️ 熵权法确定权重</h4>
                <ol class="step-list">
                  <li>归一化各指标（已由各子模块完成）</li>
                  <li>计算每个指标下各文献的比重 p = x' / Σx'</li>
                  <li>熵值 e = -1/ln(n)·Σ p·ln(p)</li>
                  <li>权重 w = (1-e) / Σ(1-e)</li>
                </ol>
              </div>
            </el-col>
            <el-col :span="12">
              <div class="inner-card accent-bg">
                <span class="weight-label">本论文权重：</span>
                <span class="weight-value">
                  w<sub>time</sub> = {{ weights.time.toFixed(3) }} ·
                  w<sub>print</sub> = {{ weights.print.toFixed(3) }} ·
                  w<sub>author</sub> = {{ weights.author.toFixed(3) }}
                </span>
              </div>
            </el-col>
          </el-row>
        </div>
      </el-collapse-item>
    </el-collapse>

    <!-- 主内容：两列布局 -->
    <el-row :gutter="20">
      <!-- 左侧：三个指标详情 + 加权计算过程 -->
      <el-col :xs="24" :lg="16">
        <el-card class="retro-card" shadow="never" style="margin-bottom: 20px;">
          <div slot="header" class="retro-card-header">
            <span><i class="el-icon-data-analysis"></i> <span class="card-title">三个维度得分及加权计算</span></span>
          </div>
          <div style="padding: 8px 0;">
            <el-row :gutter="20">
              <el-col :span="8">
                <div class="score-card">
                  <div class="score-label">⏱️ 时效可信度</div>
                  <div class="score-value">{{ scores.time.toFixed(3) }}</div>
                  <div class="score-weight">权重 {{ weights.time.toFixed(3) }}</div>
                </div>
              </el-col>
              <el-col :span="8">
                <div class="score-card">
                  <div class="score-label">📄 期刊可信度</div>
                  <div class="score-value">{{ scores.print.toFixed(3) }}</div>
                  <div class="score-weight">权重 {{ weights.print.toFixed(3) }}</div>
                </div>
              </el-col>
              <el-col :span="8">
                <div class="score-card">
                  <div class="score-label">👥 作者可信度</div>
                  <div class="score-value">{{ scores.author.toFixed(3) }}</div>
                  <div class="score-weight">权重 {{ weights.author.toFixed(3) }}</div>
                </div>
              </el-col>
            </el-row>

            <!-- 加权计算过程卡片 -->
            <div class="calc-card">
              <h4 class="calc-title">🧮 加权计算过程</h4>
              <p class="calc-line">
                C = {{ weights.time.toFixed(3) }} × {{ scores.time.toFixed(3) }} + {{ weights.print.toFixed(3) }} × {{ scores.print.toFixed(3) }} + {{ weights.author.toFixed(3) }} × {{ scores.author.toFixed(3) }}
              </p>
              <p class="calc-line">
                = {{ (weights.time * scores.time).toFixed(5) }} + {{ (weights.print * scores.print).toFixed(5) }} + {{ (weights.author * scores.author).toFixed(5) }}
              </p>
              <p class="calc-result">
                综合可信度 C = {{ overallScore.toFixed(4) }} ≈
                <span class="result-badge">{{ (overallScore * 100).toFixed(1) }}%</span>
              </p>
            </div>
          </div>
        </el-card>

        <!-- 三个指标在数据集中的排名表格 -->
        <el-card class="retro-card" shadow="never">
          <div slot="header" class="retro-card-header">
            <span><i class="el-icon-trophy"></i> <span class="card-title">本论文在数据集中的单项排名</span></span>
          </div>
          <el-table :data="rankingData" stripe size="small" class="retro-table" style="width: 100%;">
            <el-table-column prop="indicator" label="指标" width="120"></el-table-column>
            <el-table-column prop="score" label="得分" width="100">
              <template slot-scope="scope">
                <span class="score-highlight">{{ scope.row.score.toFixed(3) }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="rank" label="排名" width="80">
              <template slot-scope="scope">
                <el-tag :type="scope.row.rank <= 5 ? 'success' : (scope.row.rank <= 10 ? 'warning' : 'info')" size="small" class="rank-tag">
                  {{ scope.row.rank }} / {{ totalPapers }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="interpretation" label="解读"></el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <!-- 右侧：权重饼图 + 文献基本信息 -->
      <el-col :xs="24" :lg="8">
        <!-- 权重饼图 -->
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

        <!-- 论文核心信息卡片 -->
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

export default {
  name: 'OverallCredibility',
  data() {
    return {
      loading: false,
      // 当前文献信息（初始值，待后续根据ID加载）
      paper: {
        id: null,
        title: '',
        corresponding_author: '',
        journal: '',
        year: '',
        date: '',
        citation: 0,
        document_type: ''
      },
      // 三个指标的得分（示例数据，实际应根据文献ID获取）
      scores: {
        time:  0.8984,
        print: 0.8419,
        author: 0.759
      },
      // 熵权法权重
      weights: {
        time: 0.0359581287527857,
        print: 0.0881590183825642,
        author: 0.87588285286465
      },
      // 排名数据
      totalPapers: 51,
      rankingData: [
        { indicator: '时效可信度', score: 0.842, rank: 12, interpretation: '处于前23%' },
        { indicator: '作者可信度', score: 0.639, rank: 28, interpretation: '处于中间偏上' },
        { indicator: '期刊可信度', score: 0.756, rank: 8, interpretation: '处于前15%' }
      ]
    }
  },
  computed: {
    // ========== 新增：从路由 query 获取文献 ID ==========
    currentId() {
      return this.$route.query.id || null
    },
    overallScore() {
      return this.weights.time * this.scores.time +
        this.weights.print * this.scores.print +
        this.weights.author * this.scores.author
    }
  },
  watch: {
    // ========== 监听 ID 变化，加载对应文献数据 ==========
    currentId: {
      immediate: true,
      handler(newId) {
        if (newId) {
          console.log('接收到文献ID:', newId)
          // TODO: 根据 ID 从后端获取文献信息和三个维度的可信度分数
          // 例如：
          // this.loadPaperData(newId)
        }
      }
    }
  },
  mounted() {
    this.$nextTick(() => {
      this.initWeightChart()
    })
  },
  methods: {
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
    // TODO: 可以添加 loadPaperData(id) 方法，从后端获取数据并更新 paper、scores、rankingData 等
  }
}
</script>

<style scoped>
/* ========== 全局重置 ========== */
.overall-credibility {
  font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif;
  padding: 20px;
}

/* ========== 卡片通用样式（复古硬朗风格） ========== */
.overall-credibility .el-card {
  border: 2px solid #3C848C;    /* 深青色边框 */
  border-radius: 0;              /* 直角 */
  box-shadow: none !important;
  margin-bottom: 0;
  background-color: #FFFFFF;
}

/* 卡片头部 */
.overall-credibility .el-card__header {
  color: #3C848C;                /* 深青色文字 */
  font-weight: 800;
  text-transform: uppercase;      /* 全大写 */
  letter-spacing: 1px;
  padding: 14px 20px;
  border-bottom: 2px solid #3C848C; /* 深青色底边 */
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
  color: #3C848C;                           /* 深青色 */
  font-size: 14px;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 1px;
}

/* ========== 按钮样式 ========== */
.retro-btn {
  border-radius: 0;
  border: 2px solid #3C848C;         /* 深青色边框 */
  font-weight: 800;
  text-transform: uppercase;
  padding: 10px 20px;
  font-size: 14px;
  letter-spacing: 1px;
  transition: none;
  background-color: #FFFFFF;
  color: #3C848C;                     /* 深青色文字 */
}

.retro-btn.primary {
  background-color: #3C848C;          /* 深青色背景 */
  color: #FFFFFF;
  border-color: #3C848C;
}

.retro-btn.primary:hover {
  background-color: #2F696F;          /* 更深一点的青 */
  border-color: #2F696F;
}

.retro-btn:not(.primary):hover {
  background-color: #EEEEEE;
}

/* ========== 表格样式 ========== */
.retro-table {
  border: 2px solid #3C848C;            /* 深青色边框 */
}

.retro-table::v-deep .el-table__header-wrapper th {
  background-color: #3C848C;             /* 深青色表头 */
  color: #FFFFFF;
  font-weight: 800;
  text-transform: uppercase;
  border-right: 1px solid #BDCACC;       /* 浅青分隔线 */
  border-bottom: 2px solid #3C848C;
  font-size: 13px;
  padding: 10px 0;
}

.retro-table::v-deep .el-table__body-wrapper td {
  border-right: 1px solid #DDDDDD;
  border-bottom: 1px solid #DDDDDD;
  font-weight: 500;
}

.retro-table::v-deep .el-table--border {
  border: none;
}

.score-highlight {
  color: #3C848C;
  font-weight: 600;
}

.rank-tag {
  border-radius: 0;
  font-weight: 700;
  border: none;
}

/* ========== 折叠面板 ========== */
.retro-collapse {
  border: 2px solid #3C848C;
  border-radius: 0;
}

.retro-collapse .el-collapse-item__header {
  padding-left: 20px;
  font-size: 14px;
  font-weight: 800;
  background: white;
  color: #3C848C;
  text-transform: uppercase;
  border-bottom: 2px solid #3C848C;
}

.retro-collapse .el-collapse-item__wrap {
  background: white;
  border: none;
}

.collapse-title {
  font-weight: 800;
  color: #3C848C;
}

.collapse-content {
  padding: 8px 16px 16px;
  background: #f9fafc;
}

/* ========== 内部卡片（用于折叠面板内） ========== */
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

.formula {
  font-size: 16px;
  font-family: 'Times New Roman', serif;
  text-align: center;
  margin: 12px 0;
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

.note {
  margin: 12px 0 0;
  color: #909399;
  font-size: 13px;
}

.desc {
  margin: 8px 0 0;
  font-size: 14px;
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

/* ========== 左侧得分卡片 ========== */
.score-card {
  background: white;
  padding: 16px;
  border: 2px solid #3C848C;
  border-radius: 0;
  text-align: center;
}

.score-label {
  font-size: 14px;
  color: #606266;
  margin-bottom: 8px;
  font-weight: 700;
  text-transform: uppercase;
}

.score-value {
  font-size: 24px;
  font-weight: 700;
  color: #3C848C;
}

.score-weight {
  margin-top: 12px;
  font-size: 13px;
  color: #666666;
}

/* ========== 加权计算卡片 ========== */
.calc-card {
  margin-top: 20px;
  background: #ecf5ff;
  padding: 16px;
  border: 2px solid #3C848C;
  border-radius: 0;
}

.calc-title {
  margin-top: 0;
  margin-bottom: 12px;
  font-size: 15px;
  color: #3C848C;
  font-weight: 800;
  text-transform: uppercase;
}

.calc-line {
  margin: 5px 0;
  color: #303133;
}

.calc-result {
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

/* ========== 权重图例 ========== */
.weight-legend {
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  background: #f5f7fa;
  padding: 12px;
  border: 2px solid #3C848C;
  font-size: 13px;
}

.legend-dot {
  display: inline-block;
  width: 10px;
  height: 10px;
  margin-right: 6px;
  border-radius: 2px;
}

/* ========== 论文核心信息 ========== */
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

.info-tag {
  border-radius: 0;
  border: none;
  font-weight: 700;
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

.footer-tag {
  border-radius: 0;
  border: none;
  font-weight: 700;
}

/* ========== 分割线 ========== */
.retro-divider {
  background-color: #3C848C;                /* 深青色分割线 */
  height: 2px;
}

/* ========== 底部按钮区域 ========== */
.action-footer {
  margin-top: 24px;
  text-align: right;
  border-top: 2px solid #3C848C;
  padding-top: 16px;
}

/* ========== 图表容器 ========== */
[ref="weightChart"] {
  width: 100% !important;
  height: 100% !important;
}
</style>


