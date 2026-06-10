<template>
  <div class="knowledge-discovery-container">
    <!-- 知识发现首页介绍，只在根路径显示 -->
    <div v-if="$route.path === '/view/extraction/knowledge-discovery'" class="knowledge-discovery-intro">
      <div class="intro-header">
        <h2>知识发现</h2>
        <p class="subtitle">Knowledge Discovery</p>
      </div>

      <div class="intro-content">
        <div class="intro-section">
          <h3>功能概述</h3>
          <p>
            本模块基于知识图谱与大语言模型协同推理技术，从已构建的材料知识图谱中
            发现隐含的实体关系、多跳路径和全局模式。通过图搜索算法、推理规则筛选和
            双重思维链机制，实现复杂材料科学问题的智能推理与解答。
          </p>
        </div>

        <div class="intro-section">
          <h3>支持任务</h3>
          <div class="task-grid">
            <div class="task-card" @click="$router.push('/view/extraction/knowledge-discovery/global-discovery')">
              <h4>全局发现</h4>
              <p>从全局视角分析知识图谱的结构特征和知识分布，可视化展示完整图谱</p>
              <el-button type="text" class="card-btn">进入 <i class="el-icon-arrow-right"></i></el-button>
            </div>
            <div class="task-card" @click="$router.push('/view/extraction/knowledge-discovery/node-discovery')">
              <h4>结点发现</h4>
              <p>识别知识图谱中的关键实体节点，分析节点中心性、聚类系数等指标</p>
              <el-button type="text" class="card-btn">进入 <i class="el-icon-arrow-right"></i></el-button>
            </div>
            <div class="task-card" @click="$router.push('/view/extraction/knowledge-discovery/path-discovery')">
              <h4>路径发现</h4>
              <p>发现实体间的多跳推理路径，揭示隐含的间接关系和知识关联</p>
              <el-button type="text" class="card-btn">进入 <i class="el-icon-arrow-right"></i></el-button>
            </div>
          </div>
        </div>

        <div class="intro-section">
          <h3>工作流程</h3>
          <div class="workflow-steps">
            <div class="step" @click="$router.push('/view/extraction/knowledge-discovery/global-discovery')">
              <div class="step-number">1</div>
              <div class="step-content">
                <h4>全局发现</h4>
                <p>选择文献，查看完整知识图谱，了解整体结构和知识分布</p>
                <span class="step-link">进入 <i class="el-icon-arrow-right"></i></span>
              </div>
            </div>
            <div class="step" @click="$router.push('/view/extraction/knowledge-discovery/node-discovery')">
              <div class="step-number">2</div>
              <div class="step-content">
                <h4>结点发现</h4>
                <p>分析关键节点，查看节点详细信息和关联关系</p>
                <span class="step-link">进入 <i class="el-icon-arrow-right"></i></span>
              </div>
            </div>
            <div class="step" @click="$router.push('/view/extraction/knowledge-discovery/path-discovery')">
              <div class="step-number">3</div>
              <div class="step-content">
                <h4>路径发现</h4>
                <p>输入实体对，检索多跳推理路径，发现隐含关系</p>
                <span class="step-link">进入 <i class="el-icon-arrow-right"></i></span>
              </div>
            </div>
          </div>
        </div>

        <div class="intro-section">
          <h3>快速开始</h3>
          <div class="quick-actions">
            <el-button type="primary" size="large" @click="$router.push('/view/extraction/knowledge-discovery/global-discovery')">
              <i class="el-icon-s-grid"></i> 全局发现
            </el-button>
            <el-button size="large" @click="$router.push('/view/extraction/knowledge-discovery/node-discovery')">
              <i class="el-icon-s-management"></i> 结点发现
            </el-button>
            <el-button size="large" @click="$router.push('/view/extraction/knowledge-discovery/path-discovery')">
              <i class="el-icon-share"></i> 路径发现
            </el-button>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 重点：这里是子路由的出口，当访问 /knowledge-discovery/global-discovery 等时，子组件会显示在这里 -->
    <router-view v-else></router-view>
  </div>
</template>

<script>
export default {
  name: 'KnowledgeDiscoveryIntro'
}
</script>

<style scoped>
.knowledge-discovery-container {
  height: 100%;
}

.knowledge-discovery-intro {
  max-width: 1000px;
  margin: 0 auto;
  padding: 40px 24px;
}

.intro-header {
  text-align: center;
  margin-bottom: 48px;
}

.intro-header h2 {
  font-size: 32px;
  font-weight: 600;
  color: #1e2a3a;
  margin: 0 0 8px 0;
}

.subtitle {
  font-size: 16px;
  color: #5a6a7a;
  letter-spacing: 2px;
}

.intro-section {
  margin-bottom: 48px;
}

.intro-section h3 {
  font-size: 20px;
  font-weight: 600;
  color: #1e2a3a;
  margin: 0 0 20px 0;
  position: relative;
  padding-left: 12px;
}

.intro-section h3::before {
  content: '';
  position: absolute;
  left: 0;
  top: 4px;
  bottom: 4px;
  width: 4px;
  background: #409EFF;
  border-radius: 2px;
}

.intro-section p {
  font-size: 16px;
  line-height: 1.8;
  color: #5a6a7a;
  margin: 0;
}

.task-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}

.task-card {
  background: white;
  border: 1px solid #e9ecef;
  border-radius: 12px;
  padding: 24px;
  transition: all 0.3s;
  cursor: pointer;
  display: flex;
  flex-direction: column;
}

.task-card:hover {
  border-color: #409EFF;
  box-shadow: 0 8px 20px rgba(64, 158, 255, 0.15);
  transform: translateY(-2px);
}

.task-card h4 {
  font-size: 18px;
  font-weight: 600;
  color: #1e2a3a;
  margin: 0 0 12px 0;
}

.task-card p {
  font-size: 14px;
  color: #5a6a7a;
  line-height: 1.6;
  margin: 0 0 16px 0;
  flex: 1;
}

.card-btn {
  align-self: flex-start;
  color: #409EFF;
  font-size: 14px;
}

.card-btn:hover {
  color: #66b1ff;
}

.workflow-steps {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.step {
  display: flex;
  gap: 20px;
  align-items: flex-start;
  padding: 16px;
  border-radius: 12px;
  background: white;
  border: 1px solid #e9ecef;
  transition: all 0.3s;
  cursor: pointer;
}

.step:hover {
  border-color: #409EFF;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.1);
  transform: translateX(4px);
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
  flex-shrink: 0;
}

.step-content {
  flex: 1;
}

.step-content h4 {
  font-size: 18px;
  font-weight: 600;
  color: #1e2a3a;
  margin: 0 0 8px 0;
}

.step-content p {
  font-size: 15px;
  color: #5a6a7a;
  line-height: 1.6;
  margin: 0 0 8px 0;
}

.step-link {
  color: #409EFF;
  font-size: 14px;
  display: inline-flex;
  align-items: center;
}

.step-link i {
  margin-left: 4px;
}

.quick-actions {
  display: flex;
  gap: 16px;
  justify-content: center;
  flex-wrap: wrap;
}

.quick-actions .el-button {
  min-width: 140px;
  height: 44px;
  border-radius: 22px;
}

.quick-actions .el-button i {
  margin-right: 6px;
}

@media (max-width: 768px) {
  .knowledge-discovery-intro {
    padding: 24px 16px;
  }
  
  .task-grid {
    grid-template-columns: 1fr;
  }
  
  .quick-actions {
    flex-direction: column;
  }
  
  .quick-actions .el-button {
    width: 100%;
  }
  
  .step {
    padding: 12px;
  }
}
</style>