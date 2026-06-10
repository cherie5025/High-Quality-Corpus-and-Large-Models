<template>
  <div class="model-select-container">
    <!-- 页面标题 -->
    <div class="page-header">
      <p class="page-desc">从数据库中选择已转换的TXT文献，系统将逐句进行实体关系抽取</p>
    </div>

    <el-row :gutter="24">
      <!-- 左侧：文献搜索与列表 -->
      <el-col :span="16">
        <div class="paper-section">
          <!-- 搜索框和选中统计 -->
          <div class="search-header">
            <div class="search-box">
              <el-input
                v-model="searchKeyword"
                placeholder="输入文献标题或关键词搜索..."
                prefix-icon="el-icon-search"
                clearable
                @clear="loadPapers"
                @keyup.enter="handleSearch"
              >
                <template #append>
                  <el-button @click="handleSearch" :loading="loading">搜索</el-button>
                </template>
              </el-input>
            </div>
            <div class="selected-count" v-if="selectedPapers.length > 0">
              已选择 <span class="count-number">{{ selectedPapers.length }}</span> 篇文献
            </div>
          </div>

          <!-- 文献列表 -->
          <div class="paper-list">
            <div v-if="loading" class="loading-state">
              <i class="el-icon-loading"></i> 加载中...
            </div>
            
            <!-- 改为复选框组 -->
            <el-checkbox-group v-model="selectedPapers" class="paper-checkbox-group" v-else>
              <div v-for="paper in paperList" :key="paper.id" class="paper-item">
                <el-checkbox :label="paper.id" border class="paper-checkbox">
                  <div class="paper-info">
                    <div class="paper-title">
                      <span class="title-text">{{ paper.title }}</span>
                      <el-tag size="small" type="info">待抽取</el-tag>
                    </div>
                    <div class="paper-meta">
                      <span><i class="el-icon-document"></i> {{ paper.filename }}</span>
                      <span><i class="el-icon-time"></i> {{ formatDate(paper.createdAt) }}</span>
                    </div>
                  </div>
                </el-checkbox>
              </div>
            </el-checkbox-group>

            <!-- 空状态 -->
            <el-empty v-if="!loading && paperList.length === 0" description="暂无文献" :image-size="120" />
          </div>

          <!-- 添加全选/取消功能（可选） -->
          <div class="list-actions" v-if="paperList.length > 0">
            <el-button type="text" @click="selectAll" :disabled="paperList.length === 0">全选</el-button>
            <el-button type="text" @click="clearSelection" :disabled="selectedPapers.length === 0">取消选择</el-button>
          </div>
        </div>
      </el-col>

      <!-- 右侧：模型介绍卡片（保持不变，但更新底部按钮） -->
      <el-col :span="8">
        <div class="model-intro-card">
          <!-- ... 右侧内容保持不变 ... -->
          <div class="model-header">
            <h3>MatJER-LLM</h3>
            <div class="model-badge">材料领域知识嵌入大语言模型</div>
          </div>

          <div class="model-content">
            <p class="model-desc">
              <strong>MatJER-LLM</strong> 是基于"Primary Student-Teacher-Advanced Student"三阶段协同框架的实体关系抽取模型，专为材料科学文献设计。
            </p>

            <!-- 核心特点 -->
            <div class="feature-list">
              <div class="feature-item">
                <i class="el-icon-check" style="color: #67c23a;"></i>
                <span><strong>三阶段协同</strong> - Primary Student初步抽取，Teacher知识检索，Advanced Student约束纠错</span>
              </div>
              <div class="feature-item">
                <i class="el-icon-check" style="color: #67c23a;"></i>
                <span><strong>领域知识嵌入</strong> - 基于材料科学知识构建纠错规则库，有效缓解模型幻觉</span>
              </div>
              <div class="feature-item">
                <i class="el-icon-check" style="color: #67c23a;"></i>
                <span><strong>高精度抽取</strong> - 支持8种实体类型、9种关系类型</span>
              </div>
            </div>


            <!-- 实体类型速览 -->
            <div class="type-preview">
              <h4>实体类型速览</h4>
              <div class="type-tags">
                <el-tag size="small" type="success" class="type-tag">Composition</el-tag>
                <el-tag size="small" type="primary" class="type-tag">Structure</el-tag>
                <el-tag size="small" type="warning" class="type-tag">Property</el-tag>
                <el-tag size="small" type="info" class="type-tag">Processing</el-tag>
                <el-tag size="small" type="danger" class="type-tag">Characterization</el-tag>
                <el-tag size="small" class="type-tag">Application</el-tag>
                <el-tag size="small" type="success" class="type-tag">Feature</el-tag>
                <el-tag size="small" type="danger" class="type-tag">Condition</el-tag>
              </div>
            </div>

            <!-- 关系类型速览 -->
            <div class="relation-preview">
              <h4>关系类型速览</h4>
              <div class="relation-list">
                <span class="relation-item">cause_effect</span>
                <span class="relation-item">component_whole</span>
                <span class="relation-item">feature_of</span>
                <span class="relation-item">property_of</span>
                <span class="relation-item">instance_of</span>
                <span class="relation-item">condition_on</span>
                <span class="relation-item">used_for</span>
                <span class="relation-item">located_of</span>
                <span class="relation-item">other</span>
              </div>
            </div>

            <div class="model-footnote">
              <i class="el-icon-info"></i>
              <span>详细类型定义可在首页展开查看</span>
            </div>
          </div>

          <!-- 底部操作按钮 - 更新选中数量显示 -->
          <div class="model-actions">
            <el-button size="large" @click="$router.push('/view/extraction')">返回</el-button>
            <el-button 
              type="primary" 
              size="large" 
              :disabled="selectedPapers.length === 0"
              @click="nextStep"
            >
              开始抽取 {{ selectedPapers.length > 0 ? `(${selectedPapers.length}篇)` : '' }}
            </el-button>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { searchPapers, getAllPapers } from '@/api/literature'

export default {
  name: 'ModelSelect',
  data() {
    return {
      searchKeyword: '',
      selectedPapers: [], // 改为数组，支持多选
      paperList: [],
      loading: false
    }
  },
  methods: {
    // 加载所有文献
    async loadPapers() {
      this.loading = true
      try {
        const res = await getAllPapers()
        this.paperList = res.data.map(item => ({
          id: item.id,
          title: item.title,
          filename: item.filename,
          createdAt: item.createdAt
        }))
        console.log('加载文献成功:', this.paperList)
      } catch (error) {
        console.error('加载文献失败:', error)
        this.$message.error('加载文献失败')
      } finally {
        this.loading = false
      }
    },

    // 搜索文献
    async handleSearch() {
      if (!this.searchKeyword.trim()) {
        this.$message.warning('请输入搜索关键词')
        return
      }

      this.loading = true
      try {
        const res = await searchPapers({ keyword: this.searchKeyword })
        this.paperList = res.data.map(item => ({
          id: item.id,
          title: item.title,
          filename: item.filename,
          createdAt: item.createdAt
        }))
        console.log('搜索结果:', this.paperList)
        
        if (this.paperList.length === 0) {
          this.$message.info('没有找到相关文献')
        }
      } catch (error) {
        console.error('搜索失败:', error)
        this.$message.error('搜索失败')
      } finally {
        this.loading = false
      }
    },

    // 全选功能
    selectAll() {
      this.selectedPapers = this.paperList.map(paper => paper.id)
    },

    // 取消选择
    clearSelection() {
      this.selectedPapers = []
    },

    // 格式化日期
    formatDate(dateStr) {
      if (!dateStr) return '未知'
      const date = new Date(dateStr)
      return `${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,'0')}-${String(date.getDate()).padStart(2,'0')}`
    },

    // 下一步 - 传递选中的多篇文献ID
    nextStep() {
      if (this.selectedPapers.length === 0) {
        this.$message.warning('请至少选择一篇文献')
        return
      }
      
      // 跳转到抽取页面，带上选中的文献ID数组
      this.$router.push({
        path: '/view/extraction/model-extract',
        query: { paperIds: this.selectedPapers.join(',') }
      })
    }
  },
  mounted() {
    this.loadPapers()
  }
}
</script>

<style scoped>
.model-select-container {
  padding: 24px;
  max-width: 1600px;
  margin: 0 auto;
  width: 95%;
  min-height: calc(100vh - 88px);
  display: flex;
  flex-direction: column;
}

/* 页面标题 */
.page-header {
  margin-bottom: 20px;
  flex-shrink: 0;
}

.page-header h2 {
  font-size: 26px;
  font-weight: 600;
  color: #1e2a3a;
  margin: 0 0 6px 0;
  background: linear-gradient(135deg, #1e2a3a 0%, #2c3e50 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.page-desc {
  font-size: 14px;
  color: #5a6a7a;
  margin: 0;
}

/* 左右布局容器 */
.el-row {
  flex: 1;
  min-height: 0;
}

/* 左侧文献区域 */
.paper-section {
  background: white;
  border-radius: 20px;
  padding: 20px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.06);
  border: 1px solid #edf2f7;
  height: 100%;
  display: flex;
  flex-direction: column;
  transition: box-shadow 0.3s;
}

.paper-section:hover {
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.08);
}

/* 搜索头部 */
.search-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
  flex-shrink: 0;
}

.search-box {
  flex: 1;
}

.search-box :deep(.el-input__inner) {
  height: 42px;
  border-radius: 21px 0 0 21px;
  border-color: #e2e8f0;
}

.search-box :deep(.el-input-group__append) {
  border-radius: 0 21px 21px 0;
  background-color: #409EFF;
  border-color: #409EFF;
}

.search-box :deep(.el-input-group__append .el-button) {
  color: white;
  min-width: 70px;
}

.search-box :deep(.el-input-group__append .el-button:hover) {
  background-color: #66b1ff;
  border-color: #66b1ff;
}

.selected-count {
  font-size: 14px;
  color: #5a6a7a;
  background: #f0f9ff;
  padding: 0 16px;
  height: 42px;
  line-height: 42px;
  border-radius: 21px;
  border: 1px solid #409EFF;
  white-space: nowrap;
  font-weight: 500;
}

.count-number {
  font-weight: 700;
  color: #409EFF;
  font-size: 18px;
  margin: 0 2px;
}

/* 文献列表 */
.paper-list {
  flex: 1;
  min-height: 400px;
  max-height: 600px;
  overflow-y: auto;
  padding-right: 6px;
  margin-bottom: 16px;
}

/* 自定义滚动条 */
.paper-list::-webkit-scrollbar {
  width: 6px;
}

.paper-list::-webkit-scrollbar-thumb {
  background: #d0d7de;
  border-radius: 3px;
}

.paper-list::-webkit-scrollbar-thumb:hover {
  background: #a0aec0;
}

.paper-list::-webkit-scrollbar-track {
  background: #f1f5f9;
  border-radius: 3px;
}

.loading-state {
  text-align: center;
  padding: 60px 20px;
  color: #909399;
  font-size: 15px;
}

.loading-state i {
  margin-right: 8px;
  font-size: 20px;
}

/* 复选框组 */
.paper-checkbox-group {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.paper-item {
  width: 100%;
  transition: transform 0.2s;
}

.paper-item:hover {
  transform: translateX(4px);
}

.paper-checkbox {
  width: 100%;
  height: auto;
  padding: 16px;
  border-radius: 16px;
  border: 1px solid #e9ecef;
  background: #ffffff;
  transition: all 0.3s;
  margin-right: 0;
}

.paper-checkbox :deep(.el-checkbox__input) {
  margin-top: 2px;
}

.paper-checkbox :deep(.el-checkbox__label) {
  width: 100%;
  padding-left: 12px;
}

.paper-checkbox:hover {
  border-color: #409EFF;
  background: #fafcff;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.1);
}

.paper-checkbox.is-checked {
  border-color: #409EFF;
  background: #f0f9ff;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.2);
}

.paper-info {
  width: 100%;
}

.paper-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.title-text {
  font-size: 15px;
  font-weight: 500;
  color: #1e2a3a;
  line-height: 1.4;
  flex: 1;
  margin-right: 12px;
}

.paper-meta {
  display: flex;
  gap: 24px;
  font-size: 13px;
  color: #6b7a8a;
}

.paper-meta i {
  margin-right: 4px;
  font-size: 13px;
}

/* 列表操作按钮 */
.list-actions {
  margin-top: 4px;
  padding-top: 16px;
  border-top: 1px solid #edf2f7;
  display: flex;
  gap: 20px;
  flex-shrink: 0;
}

.list-actions .el-button {
  padding: 6px 0;
  font-size: 14px;
  font-weight: 500;
}

.list-actions .el-button:hover {
  color: #409EFF;
  background: transparent;
}

/* 右侧模型介绍卡片 */
.model-intro-card {
  background: white;
  border-radius: 20px;
  padding: 24px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.06);
  border: 1px solid #edf2f7;
  position: sticky;
  top: 24px;
  height: fit-content;
  max-height: calc(100vh - 48px);
  overflow-y: auto;
  transition: box-shadow 0.3s;
}

.model-intro-card:hover {
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.08);
}

/* 右侧滚动条 */
.model-intro-card::-webkit-scrollbar {
  width: 4px;
}

.model-intro-card::-webkit-scrollbar-thumb {
  background: #d0d7de;
  border-radius: 2px;
}

.model-header {
  text-align: center;
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid #edf2f7;
}

.model-icon {
  font-size: 44px;
  margin-bottom: 12px;
  filter: drop-shadow(0 4px 8px rgba(0,0,0,0.1));
}

.model-header h3 {
  font-size: 24px;
  font-weight: 700;
  color: #1e2a3a;
  margin: 0 0 8px 0;
  letter-spacing: 1px;
}

.model-badge {
  display: inline-block;
  background: linear-gradient(135deg, #ecf5ff 0%, #e3f0ff 100%);
  color: #409EFF;
  font-size: 13px;
  font-weight: 500;
  padding: 4px 14px;
  border-radius: 30px;
  border: 1px solid rgba(64, 158, 255, 0.2);
}

.model-desc {
  font-size: 14px;
  line-height: 1.7;
  color: #4a5a6a;
  margin: 0 0 20px 0;
  background: #f8fafc;
  padding: 16px;
  border-radius: 14px;
  border-left: 4px solid #409EFF;
}

/* 特点列表 */
.feature-list {
  margin-bottom: 24px;
}

.feature-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 14px;
  font-size: 14px;
  color: #2c3e50;
  line-height: 1.5;
  padding: 4px 0;
}

.feature-item i {
  flex-shrink: 0;
  margin-top: 3px;
  font-size: 16px;
}

/* 性能指标 */
.performance-mini {
  margin-bottom: 24px;
  padding: 16px;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border-radius: 16px;
  border: 1px solid #e9ecef;
}

.performance-mini h4 {
  font-size: 15px;
  font-weight: 600;
  color: #1e2a3a;
  margin: 0 0 16px 0;
  text-align: center;
}

.metric-card {
  text-align: center;
  padding: 10px 4px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.02);
  transition: transform 0.2s;
}

.metric-card:hover {
  transform: translateY(-2px);
}

.metric-value {
  font-size: 22px;
  font-weight: 700;
  color: #409EFF;
  line-height: 1.2;
  margin-bottom: 4px;
}

.metric-label {
  font-size: 12px;
  color: #6b7a8a;
  font-weight: 500;
}

/* 类型速览 */
.type-preview,
.relation-preview {
  margin-bottom: 24px;
}

.type-preview h4,
.relation-preview h4 {
  font-size: 15px;
  font-weight: 600;
  color: #1e2a3a;
  margin: 0 0 12px 0;
  display: flex;
  align-items: center;
}

.type-preview h4:before,
.relation-preview h4:before {
  content: '';
  display: inline-block;
  width: 4px;
  height: 16px;
  background: #409EFF;
  border-radius: 2px;
  margin-right: 8px;
}

.type-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.type-tag {
  font-size: 12px;
  padding: 5px 12px;
  border-radius: 20px;
  font-weight: 500;
  border: none;
}

.relation-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.relation-item {
  background: #f1f5f9;
  color: #2c3e50;
  padding: 5px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
  border: 1px solid #e2e8f0;
  transition: all 0.2s;
}

.relation-item:hover {
  background: #e2e8f0;
  border-color: #cbd5e0;
}

/* 脚注 */
.model-footnote {
  margin-bottom: 20px;
  padding: 12px 16px;
  background: #f8fafc;
  border-radius: 12px;
  font-size: 13px;
  color: #5a6a7a;
  display: flex;
  align-items: center;
  gap: 8px;
  border: 1px dashed #cbd5e0;
}

.model-footnote i {
  color: #409EFF;
  font-size: 16px;
}

/* 底部按钮 */
.model-actions {
  display: flex;
  gap: 12px;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #edf2f7;
}

.model-actions .el-button {
  flex: 1;
  height: 44px;
  font-size: 15px;
  font-weight: 500;
  border-radius: 22px;
}

.model-actions .el-button--primary {
  background: linear-gradient(135deg, #409EFF 0%, #2b7ed7 100%);
  border: none;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
}

.model-actions .el-button--primary:hover {
  background: linear-gradient(135deg, #66b1ff 0%, #409EFF 100%);
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(64, 158, 255, 0.4);
}

.model-actions .el-button--primary:active {
  transform: translateY(0);
}

.model-actions .el-button--primary.is-disabled {
  background: #a0cfff;
  box-shadow: none;
}

/* 响应式 */
@media (max-width: 1200px) {
  .model-select-container {
    width: 98%;
    padding: 16px;
  }
}

@media (max-width: 768px) {
  .model-select-container {
    padding: 12px;
    width: 100%;
  }
  
  .search-header {
    flex-direction: column;
    align-items: stretch;
  }
  
  .selected-count {
    text-align: center;
  }
  
  .paper-meta {
    flex-direction: column;
    gap: 6px;
  }
  
  .model-actions {
    flex-direction: column;
  }
  
  .paper-list {
    max-height: 500px;
  }
}

/* 大屏幕优化 */
@media screen and (min-width: 1920px) {
  .model-select-container {
    max-width: 1800px;
    width: 90%;
  }
  
  .paper-list {
    max-height: 700px;
  }
}

@media screen and (min-width: 2560px) {
  .model-select-container {
    max-width: 2200px;
    width: 85%;
  }
}

/* 动画效果 */
.paper-checkbox,
.metric-card,
.model-actions .el-button {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

/* 空状态美化 */
.el-empty {
  padding: 60px 0;
}

.el-empty :deep(.el-empty__description) {
  margin-top: 16px;
}

.el-empty :deep(p) {
  color: #909399;
  font-size: 14px;
}
</style>