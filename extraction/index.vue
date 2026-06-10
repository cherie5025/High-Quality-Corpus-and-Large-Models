<!-- src/view/extraction/index.vue -->
<template>
  <div class="extraction-container">
    <!--情况1：显示简介页（当在根路径时） -->
    <div v-if="isRootRoute" class="extraction-intro">
      <!-- 标题区域 -->
      <div class="intro-header">
        <h2>高质量三元组构建</h2>
        <div class="method-tag">MatJER-LLM · 材料领域知识嵌入大语言模型</div>
        <p class="intro-desc">
          从非结构化文献中自动识别专业实体，抽取语义关系，生成结构化三元组。
          支持8种实体类型、9种关系类型，通过领域知识嵌入与约束式纠错提升抽取精度。
        </p>
      </div>

      <!-- 实体关系类型定义 - 下拉式，默认折叠 -->
      <div class="definition-section">
        <div class="section-header" @click="showDefinition = !showDefinition">
          <div class="header-left">
            <h3>实体关系类型定义</h3>
            <span class="header-sub">点击展开查看8种实体类型、9种关系类型</span>
          </div>
          <el-button text class="toggle-btn">
            {{ showDefinition ? '收起' : '展开' }} 
            <i :class="showDefinition ? 'el-icon-arrow-up' : 'el-icon-arrow-down'"></i>
          </el-button>
        </div>
        
        <el-collapse-transition>
          <div v-show="showDefinition" class="definition-content">
            <el-tabs v-model="activeTab" class="definition-tabs">
              <!-- 实体类型标签页 -->
              <el-tab-pane label="实体类型 (8种)" name="entity">
                <el-table :data="entityTypes" border stripe size="default" class="type-table">
                  <el-table-column prop="type" label="类型" width="140">
                    <template #default="{ row }">
                      <el-tag :type="row.color" size="large" effect="light" class="type-tag">
                        {{ row.type }}
                      </el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column prop="definition" label="定义" min-width="300">
                    <template #default="{ row }">
                      <div class="definition-text">{{ row.definition }}</div>
                    </template>
                  </el-table-column>
                  <el-table-column prop="examples" label="示例" min-width="250">
                    <template #default="{ row }">
                      <div class="examples-list">
                        <span v-for="(ex, idx) in row.examples.split(';')" :key="idx" class="example-item">
                          {{ ex.trim() }}
                        </span>
                      </div>
                    </template>
                  </el-table-column>
                </el-table>
              </el-tab-pane>
              
              <!-- 关系类型标签页 -->
              <el-tab-pane label="关系类型 (9种)" name="relation">
                <el-table :data="relationTypes" border stripe size="default" class="type-table">
                  <el-table-column prop="type" label="类型" width="160">
                    <template #default="{ row }">
                      <el-tag type="info" size="large" effect="light" class="type-tag">
                        {{ row.type }}
                      </el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column prop="definition" label="定义" min-width="280">
                    <template #default="{ row }">
                      <div class="definition-text">{{ row.definition }}</div>
                    </template>
                  </el-table-column>
                  <el-table-column prop="applicable" label="适用实体对" min-width="250">
                    <template #default="{ row }">
                      <div class="applicable-list">
                        <span v-for="(app, idx) in row.applicable.split(';')" :key="idx" class="applicable-item">
                          {{ app.trim() }}
                        </span>
                      </div>
                    </template>
                  </el-table-column>
                </el-table>
              </el-tab-pane>
            </el-tabs>
            
            <div class="definition-footnote">
              <i class="el-icon-info"></i>
              <span>类型信息在结果中以 <span class="type-badge">[类型]</span> 形式标注，编辑时可修改</span>
            </div>
          </div>
        </el-collapse-transition>
      </div>

      <!-- 操作流程 - 大号横向 -->
      <div class="workflow-section">
        <h3>操作流程</h3>
        <div class="workflow-horizontal">
          <div class="workflow-step" @click="$router.push('/view/extraction/model-select')">
            <div class="step-number">1</div>
            
            <div class="step-title">选择文献</div>
            <div class="step-desc">从数据库中选择已转换的TXT文献</div>
          </div>
          
          <div class="workflow-arrow">→</div>
          
          <div class="workflow-step highlight" @click="$router.push('/view/extraction/extract')">
            <div class="step-number">2</div>
            
            <div class="step-title">开始抽取</div>
            <div class="step-desc">运行MatJER-LLM进行实体关系抽取</div>
          </div>
          
          <div class="workflow-arrow">→</div>
          
          <div class="workflow-step" @click="$router.push('/view/extraction/result')">
            <div class="step-number">3</div>
            
            <div class="step-title">查看结果</div>
            <div class="step-desc">编辑、导出CSV、导入知识图谱</div>
          </div>
          
          <div class="workflow-arrow">→</div>
          
          <div class="workflow-step optional" @click="$router.push('/view/extraction/evaluate')">
            <div class="step-number">4</div>
            <div class="step-title">质量评估</div>
            <div class="step-desc">与其他模型对比，评估表现</div>
            <el-tag size="small" type="info" class="optional-tag">可选</el-tag>
          </div>
        </div>
      </div>

      <!-- 快速操作卡片 - 三列等宽 -->
      <div class="quick-actions">
        <el-row :gutter="20">
          <el-col :span="8">
            <div class="action-card" @click="$router.push('/view/extraction/model-select')">
              <div class="action-title">选择文献</div>
              <div class="action-desc">从数据库中选择已转换的文献</div>
              <el-button type="primary" text class="action-btn">去选择 →</el-button>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="action-card primary" @click="$router.push('/view/extraction/extract')">
              
              <div class="action-title">开始抽取</div>
              <div class="action-desc">运行MatJER-LLM进行实体关系抽取</div>
              <el-button type="primary" class="action-btn">立即抽取</el-button>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="action-card" @click="$router.push('/view/extraction/result')">
              
              <div class="action-title">查看结果</div>
              <div class="action-desc">编辑、导出、导入知识图谱</div>
              <el-button type="primary" text class="action-btn">去查看 →</el-button>
            </div>
          </el-col>
        </el-row>
      </div>
    </div>

    <!-- ✅ 情况2：显示子页面（当在子路由时） -->
    <div v-else class="extraction-subpage">
      <router-view />
    </div>
  </div>
</template>

<script>
export default {
  name: 'ExtractionIntro',
  data() {
    return {
      activeTab: 'entity',
      showDefinition: false, // 默认折叠
      // 实体类型定义（论文表3.1）
      entityTypes: [
        { type: 'Composition', color: 'success', 
          definition: '与化学式有关的内容：描述材料内部与含量相关的内容等', 
          examples: 'NaCl; CaCl2; Na concentration; Electrons charge carriers' },
        { type: 'Structure', color: 'primary', 
          definition: '晶体结构和相，用于刻画晶体结构的名称等', 
          examples: 'Fcc; Phase; Bottleneck; Channel; Path' },
        { type: 'Property', color: 'warning', 
          definition: '带单位的可度量值；材料表现出来的性质或现象；描述材料产生物理/化学行为或机制的名词', 
          examples: 'Conductivity; Activation; Radius; Ferroelectric; Metallic; Phase transition; Ionic reaction' },
        { type: 'Processing', color: 'info', 
          definition: '材料合成技术或加工工艺；材料改性手段等', 
          examples: 'Solid state reaction; Annealing; Doping' },
        { type: 'Characterization', color: 'danger', 
          definition: '用于表征材料的任何实验、理论、模型或公式等', 
          examples: 'XRD; STM; Photoluminescence; DFT; Bethe-Salpeter equation' },
        { type: 'Application', color: '', 
          definition: '任何高级的应用，任何特定的器件、系统等', 
          examples: 'Cathode; Photovoltaics; Battery Management System' },
        { type: 'Feature', color: 'success', 
          definition: '有关材料类型、形貌的特殊描述', 
          examples: 'Single crystal; Bulk; nanotube; Quantum dot' },
        { type: 'Condition', color: 'danger', 
          definition: '描述材料所处的环境或外部条件', 
          examples: '980℃; 1000 MPa' }
      ],
      // 关系类型定义（论文表3.2）
      relationTypes: [
        { type: 'cause_effect', 
          definition: 'A对B有影响', 
          applicable: 'Property-Property; Composition-Structure; Structure-Property' },
        { type: 'component_whole', 
          definition: 'A是B的部分', 
          applicable: 'Composition-Composition' },
        { type: 'feature_of', 
          definition: 'A是B的特征', 
          applicable: 'Feature-Composition; Feature-Application' },
        { type: 'property_of', 
          definition: 'A是B的属性', 
          applicable: 'Property-Feature; Property-Structure' },
        { type: 'instance_of', 
          definition: 'A是B的实例', 
          applicable: 'Composition-Composition' },
        { type: 'condition_on', 
          definition: 'A的条件是B', 
          applicable: 'Structure-Structure; Property-Property' },
        { type: 'used_for', 
          definition: 'A作用于B', 
          applicable: 'Processing-Condition' },
        { type: 'located_of', 
          definition: 'A占据了B的位置', 
          applicable: 'Feature-Application; Processing-Feature; Characterization-Property' },
        { type: 'other', 
          definition: 'A与B存在除上述关系类型外的其它关系', 
          applicable: 'Composition-Structure; Structure-Structure' }
      ]
    }
  },
  computed: {
    isRootRoute() {
      return this.$route.path === '/view/extraction' || this.$route.path === '/view/extraction/'
    }
  }
}
</script>

<style scoped>
.extraction-container {
  width: 100%;
  height: 100%;
  background: #f5f7fa;
  overflow-y: auto;
}

.extraction-intro {
  max-width: 1400px;
  margin: 0 auto;
  padding: 40px 24px;
}

/* 标题区域 */
.intro-header {
  margin-bottom: 32px;
  text-align: center;
}

.intro-header h2 {
  font-size: 32px;
  font-weight: 600;
  color: #1e2a3a;
  margin: 0 0 8px 0;
}

.method-tag {
  display: inline-block;
  background: #ecf5ff;
  color: #409EFF;
  font-size: 14px;
  font-weight: 500;
  padding: 4px 16px;
  border-radius: 30px;
  margin-bottom: 16px;
  border: 1px solid rgba(64, 158, 255, 0.3);
}

.intro-desc {
  font-size: 15px;
  color: #5a6a7a;
  max-width: 900px;
  margin: 0 auto;
  line-height: 1.6;
}

/* 定义区域 - 下拉式 */
.definition-section {
  background: white;
  border-radius: 20px;
  padding: 20px 24px;
  margin-bottom: 32px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04);
  border: 1px solid #e9ecef;
  transition: all 0.3s;
}

.definition-section:hover {
  border-color: #409EFF;
  box-shadow: 0 6px 20px rgba(64, 158, 255, 0.1);
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  cursor: pointer;
  padding: 4px 0;
}

.header-left {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.section-header h3 {
  font-size: 18px;
  font-weight: 600;
  color: #1e2a3a;
  margin: 0;
}

.header-sub {
  font-size: 14px;
  color: #909399;
  font-weight: 400;
}

.toggle-btn {
  color: #409EFF;
  font-size: 14px;
}

.toggle-btn i {
  margin-left: 4px;
}

.definition-content {
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px dashed #e9ecef;
}

.definition-tabs :deep(.el-tabs__nav) {
  margin-left: 0;
}

.definition-tabs :deep(.el-tabs__item) {
  font-size: 15px;
  padding: 0 24px;
  height: 48px;
  line-height: 48px;
}

.definition-tabs :deep(.el-tabs__item:hover) {
  color: #409EFF;
}

.definition-tabs :deep(.el-tabs__item.is-active) {
  color: #409EFF;
  font-weight: 500;
}

.definition-tabs :deep(.el-tabs__active-bar) {
  background-color: #409EFF;
  height: 3px;
}

.type-table {
  width: 100%;
  margin-top: 16px;
  border-radius: 12px;
  overflow: hidden;
}

.type-table :deep(.el-table th) {
  background-color: #f8fafc;
  color: #1e2a3a;
  font-weight: 600;
  font-size: 14px;
  padding: 12px 0;
}

.type-table :deep(.el-table td) {
  padding: 12px 8px;
}

.type-tag {
  font-size: 14px;
  font-weight: 500;
  padding: 6px 12px;
  border-radius: 16px;
}

.definition-text {
  line-height: 1.5;
  color: #2c3e50;
  font-size: 14px;
}

.examples-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.example-item {
  background: #f0f9ff;
  color: #409EFF;
  padding: 4px 10px;
  border-radius: 14px;
  font-size: 13px;
  border: 1px solid rgba(64, 158, 255, 0.2);
  white-space: nowrap;
}

.applicable-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.applicable-item {
  background: #f4f4f5;
  color: #606266;
  padding: 4px 10px;
  border-radius: 14px;
  font-size: 13px;
  border: 1px solid #e4e7ed;
  white-space: nowrap;
}

.definition-footnote {
  margin-top: 20px;
  padding: 14px 16px;
  background: #f8fafc;
  border-radius: 12px;
  font-size: 14px;
  color: #5a6a7a;
  display: flex;
  align-items: center;
  gap: 10px;
  border: 1px solid #e9ecef;
}

.type-badge {
  background: #ecf5ff;
  color: #409EFF;
  padding: 3px 10px;
  border-radius: 14px;
  font-size: 13px;
  font-weight: 500;
}

/* 操作流程区域 */
.workflow-section {
  margin-bottom: 32px;
  background: white;
  border-radius: 20px;
  padding: 32px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04);
  border: 1px solid #e9ecef;
}

.workflow-section h3 {
  font-size: 20px;
  font-weight: 600;
  color: #1e2a3a;
  margin: 0 0 24px 0;
  text-align: center;
}

.workflow-horizontal {
  display: flex;
  align-items: stretch;
  justify-content: space-between;
  gap: 16px;
}

.workflow-step {
  flex: 1;
  background: #f8fafc;
  border-radius: 16px;
  padding: 28px 16px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
  border: 2px solid transparent;
  position: relative;
}

.workflow-step:hover {
  border-color: #409EFF;
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(64, 158, 255, 0.15);
}

.workflow-step.highlight {
  background: #f0f9ff;
  border: 2px solid #409EFF;
  box-shadow: 0 8px 24px rgba(64, 158, 255, 0.2);
}

.workflow-step.optional {
  opacity: 0.9;
}

.step-number {
  width: 36px;
  height: 36px;
  background: #409EFF;
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 600;
  margin: 0 auto 16px;
}

.workflow-step.highlight .step-number {
  background: #409EFF;
  transform: scale(1.1);
}

.step-icon {
  font-size: 42px;
  margin-bottom: 16px;
}

.step-title {
  font-size: 18px;
  font-weight: 600;
  color: #1e2a3a;
  margin-bottom: 8px;
}

.step-desc {
  font-size: 14px;
  color: #5a6a7a;
  line-height: 1.5;
  padding: 0 8px;
}

.optional-tag {
  position: absolute;
  top: -10px;
  right: -10px;
}

.workflow-arrow {
  display: flex;
  align-items: center;
  font-size: 32px;
  color: #409EFF;
  font-weight: 300;
}

/* 快速操作卡片 */
.quick-actions {
  margin-bottom: 20px;
}

.action-card {
  background: white;
  border: 1px solid #e9ecef;
  border-radius: 20px;
  padding: 28px 20px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.action-card:hover {
  border-color: #409EFF;
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(64, 158, 255, 0.12);
}

.action-card.primary {
  background: linear-gradient(135deg, #409EFF 0%, #1976d2 100%);
  border-color: #409EFF;
}

.action-card.primary .action-icon,
.action-card.primary .action-title,
.action-card.primary .action-desc {
  color: white;
}

.action-icon {
  font-size: 48px;
  margin-bottom: 20px;
  color: #409EFF;
}

.action-title {
  font-size: 20px;
  font-weight: 600;
  color: #1e2a3a;
  margin-bottom: 10px;
}

.action-desc {
  font-size: 14px;
  color: #5a6a7a;
  margin-bottom: 24px;
  flex: 1;
  line-height: 1.5;
}

.action-btn {
  margin-top: auto;
  width: 100%;
  height: 40px;
  font-size: 14px;
}

/* 子页面容器 */
.extraction-subpage {
  width: 100%;
  height: 100%;
}

/* 响应式 */
@media (max-width: 768px) {
  .extraction-intro {
    padding: 20px 16px;
  }
  
  .header-left {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }
  
  .workflow-horizontal {
    flex-direction: column;
    gap: 16px;
  }
  
  .workflow-arrow {
    transform: rotate(90deg);
    margin: 8px 0;
  }
  
  .quick-actions .el-col {
    margin-bottom: 16px;
  }
  
  .examples-list,
  .applicable-list {
    flex-wrap: wrap;
  }
  
  .example-item,
  .applicable-item {
    white-space: normal;
  }
}
</style>