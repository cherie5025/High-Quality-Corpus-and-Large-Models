//print_credibility.vue
<template>
  <div class="print-credibility" v-loading="loading">
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

    <div style="margin-bottom: 24px;" v-if="paper.title">
      <h2 class="page-title"><i class="el-icon-document-copy" style="margin-right: 12px;"></i>期刊可信度 · C_print 评估</h2>
      <p class="page-subtitle">当前文献 <span class="paper-highlight">《{{ paper.title }}》</span><span class="paper-meta">{{ paper.journal }}, {{ paper.year }}</span></p>
    </div>

    <el-collapse class="retro-collapse" style="margin-bottom: 20px;">
      <el-collapse-item>
        <template slot="title"><i class="el-icon-info" style="color: #3C848C; margin-right: 8px;"></i><span class="collapse-title">📐 期刊可信度 C_print 计算方法</span></template>
        <div class="collapse-content">
          <div class="inner-card">
            <h4 class="inner-card-title">📌 计算流程</h4>
            <p class="formula"><strong>第一步：分区间归一化影响因子</strong><br>IF &lt; 11：C<sub>publication</sub><sup>norm</sup> = IF / 11<br>IF ≥ 11：C<sub>publication</sub><sup>norm</sup> = (IF - 11) / (IF<sub>max</sub> - 11)</p>
            <p class="formula"><strong>第二步：权重调整</strong><br>IF &lt; 11：C<sub>adj</sub> = C<sub>publication</sub><sup>norm</sup> × 0.9<br>IF ≥ 11：C<sub>adj</sub> = C<sub>publication</sub><sup>norm</sup> × 0.1 + 0.9</p>
            <p class="formula"><strong>第三步：加类型值</strong><br>C<sub>print</sub><sup>raw</sup> = C<sub>adj</sub> + C<sub>type</sub><br>综述=1.0，研究型=0.8，会议论文=0.6，其他=0.2</p>
            <p class="formula"><strong>第四步：二次归一化</strong><br>C<sub>print</sub><sup>norm</sup> = C<sub>print</sub><sup>raw</sup> / C<sub>print</sub><sup>raw</sup><sub>max</sub></p>
          </div>
        </div>
      </el-collapse-item>
    </el-collapse>

    <!-- 显示本论文期刊可信度得分 -->
    <el-card class="retro-card" shadow="never" style="margin-bottom: 20px;">
      <div slot="header" class="retro-card-header">
        <span><i class="el-icon-document"></i> <span class="card-title">本论文期刊可信度结果</span></span>
      </div>
      <div class="inner-card accent-bg calculation-result">
        <p class="result-number">C_print = {{ backendCprint.toFixed(4) }} <span class="result-badge">{{ (backendCprint * 100).toFixed(1) }}%</span></p>
      </div>
    </el-card>

    <el-row :gutter="20">
      <el-col :xs="24" :lg="12">
        <el-card class="retro-card" shadow="never" style="margin-bottom: 20px;">
          <div slot="header" class="retro-card-header"><span><i class="el-icon-pie-chart"></i> <span class="card-title">文献类型分布 (所有文献)</span></span></div>
          <div ref="typeChart" style="width: 100%; height: 250px;"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card class="retro-card" shadow="never">
          <div slot="header" class="retro-card-header"><span><i class="el-icon-collection"></i> <span class="card-title">核心统计信息</span></span></div>
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
  name: 'PrintCredibility',
  data() {
    return {
      loading: false,
      paper: {
        title: '',
        journal: '',
        year: '',
        document_type: '',
        IS: 0,
        corresponding_author: ''
      },
      backendCprint: 0,
      allPapers: [],
      typeStats: { reviewCount: 0, articleCount: 0, proceedingsCount: 0 },
      avgIf: 0,
      avgCprint: 0
    }
  },
  watch: {
    '$route.query.id': {
      immediate: true,
      async handler(newId) {
        if (newId) {
          await this.fetchPaperData(newId)
        } else {
          this.$message.info('请从文献列表选择文献查看可信度')
          this.loading = false
        }
      }
    }
  },
  methods: {
    async fetchPaperData(id) {
      this.loading = true
      try {
        const paperRes = await axios.get(`${API_BASE}/api/id/${id}`)
        if (paperRes.data.success) {
          const data = paperRes.data.data
          this.paper = {
            title: data['题目'] || '',
            journal: data['期刊'] || '',
            year: data['发表日期'] ? (typeof data['发表日期'] === 'string' ? parseInt(data['发表日期'].split('.')[0]) : data['发表日期']) : '',
            document_type: data['文献类型'] || '',
            IS: parseFloat(data['IS']) || 0,
            corresponding_author: data['通讯作者'] || ''
          }
          this.backendCprint = data.C_print !== undefined ? data.C_print : 0
          console.log('后端返回的 C_print:', this.backendCprint)
        }
        await this.loadAllPapers()
      } catch (error) {
        console.error('加载失败:', error)
        this.$message.error('数据加载失败，请检查后端')
      } finally {
        this.loading = false
        this.$nextTick(() => this.initTypeChart())
      }
    },
    async loadAllPapers() {
      try {
        const allRes = await axios.get(`${API_BASE}/api/data?pageSize=1000`);
        if (allRes.data && Array.isArray(allRes.data.data)) {
          const papers = allRes.data.data;
          this.allPapers = papers;

          // 直接从前端数据计算统计量
          let reviewCount = 0, articleCount = 0, proceedingsCount = 0;
          let sumIf = 0, sumCprint = 0;
          papers.forEach(p => {
            if (p.document_type === '综述') reviewCount++;
            else if (p.document_type === '研究型') articleCount++;
            else if (p.document_type === '会议论文') proceedingsCount++;
            sumIf += p.impact_factor || 0;
            sumCprint += p.C_print || 0;
          });
          const total = papers.length;

          this.typeStats = {
            reviewCount,
            articleCount,
            proceedingsCount
          };
          this.avgIf = total ? sumIf / total : 0;
          this.avgCprint = total ? sumCprint / total : 0;
        }
      } catch (error) {
        console.error('加载文献列表失败:', error);
      }
    },




    calculateStats() {
      if (!this.allPapers.length) return
      let reviewCount = 0, articleCount = 0, proceedingsCount = 0, sumIf = 0, sumCprint = 0
      this.allPapers.forEach(p => {
        if (p.document_type === '综述') reviewCount++
        else if (p.document_type === '研究型') articleCount++
        else if (p.document_type === '会议论文') proceedingsCount++
        sumIf += p.IS || 0
        sumCprint += p.C_print || 0
      })
      this.typeStats = { reviewCount, articleCount, proceedingsCount }
      this.avgIf = sumIf / this.allPapers.length
      this.avgCprint = sumCprint / this.allPapers.length
    },
    initTypeChart() {
      const chartDom = this.$refs.typeChart
      if (!chartDom || !this.allPapers.length) return
      const myChart = echarts.init(chartDom)
      const typeCount = {}
      this.allPapers.forEach(p => {
        const docType = p.document_type || '其他'
        typeCount[docType] = (typeCount[docType] || 0) + 1
      })
      const data = Object.keys(typeCount).map(key => ({ name: key, value: typeCount[key] }))
      const colors = data.map((_, i) => `hsl(${(i * 360 / data.length) % 360}, 30%, 65%)`)
      const option = {
        tooltip: { trigger: 'item', formatter: '{b}: {c}篇 ({d}%)' },
        legend: { show: true, orient: 'horizontal', left: 'center', top: 'bottom' },
        color: colors,
        series: [{
          type: 'pie', radius: ['50%', '70%'], avoidLabelOverlap: false,
          itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
          label: { show: true, position: 'outside', formatter: '{b}', color: '#303133' },
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
/* 样式与原文件完全相同，请从你的原文件中复制，或直接使用下方样式确保显示正常 */
.print-credibility { font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; padding: 20px; }
.print-credibility .el-card { border: 2px solid #3C848C; border-radius: 0; box-shadow: none !important; background-color: #FFFFFF; }
.print-credibility .el-card__header { color: #3C848C; font-weight: 800; text-transform: uppercase; letter-spacing: 1px; padding: 14px 20px; border-bottom: 2px solid #3C848C; }
.retro-card-header { display: flex; align-items: center; justify-content: space-between; }
.retro-card-header i { color: #3C848C; margin-right: 8px; font-size: 18px; }
.card-title { color: #3C848C; font-size: 14px; font-weight: 800; text-transform: uppercase; letter-spacing: 1px; }
.retro-btn { border-radius: 0; border: 2px solid #3C848C; font-weight: 800; text-transform: uppercase; padding: 10px 20px; font-size: 14px; letter-spacing: 1px; transition: none; background-color: #FFFFFF; color: #3C848C; }
.retro-btn.primary { background-color: #3C848C; color: #FFFFFF; border-color: #3C848C; }
.retro-btn.primary:hover { background-color: #2F696F; }
.retro-btn:not(.primary):hover { background-color: #EEEEEE; }
.retro-collapse { border: 2px solid #3C848C; border-radius: 0; }
.retro-collapse .el-collapse-item__header { padding-left: 20px; font-size: 14px; font-weight: 800; background: white; color: #3C848C; text-transform: uppercase; border-bottom: 2px solid #3C848C; }
.collapse-title { font-weight: 800; color: #3C848C; }
.collapse-content { padding: 8px 16px 16px; background: #f9fafc; }
.inner-card { background: white; padding: 16px; border: 2px solid #3C848C; border-radius: 0; height: 100%; }
.inner-card-title { margin-top: 0; margin-bottom: 12px; font-size: 15px; color: #3C848C; font-weight: 800; text-transform: uppercase; }
.accent-bg { background: #ecf5ff; }
.formula { font-size: 14px; font-weight: 600; margin: 8px 0; text-align: left; }
.page-title { font-size: 20px; font-weight: 800; color: #3C848C; display: flex; align-items: center; text-transform: uppercase; margin: 0 0 8px; }
.page-subtitle { color: #666666; font-size: 14px; margin-top: 8px; font-weight: 700; text-transform: uppercase; }
.paper-highlight { font-weight: 800; color: #3C848C; }
.paper-meta { margin-left: 12px; background: #BDCACC; padding: 4px 12px; font-size: 12px; color: #3C848C; }
.stat-list { font-size: 14px; line-height: 2; }
.stat-list > div { display: flex; justify-content: space-between; border-bottom: 1px dashed #ebeef5; padding: 8px 0; }
.stat-label { color: #666666; font-weight: 700; text-transform: uppercase; }
.stat-value { font-weight: 600; color: #3C848C; }
.retro-divider { background-color: #3C848C; height: 2px; }
.action-footer { margin-top: 24px; text-align: right; border-top: 2px solid #3C848C; padding-top: 16px; }
.result-number { margin: 10px 0 0; font-size: 20px; font-weight: 700; color: #3C848C; }
.result-badge { background: #3C848C; color: white; padding: 2px 12px; margin-left: 8px; display: inline-block; }
</style>
