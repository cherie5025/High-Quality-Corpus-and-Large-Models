<template>
  <div class="literature-upload-container">
    <!-- 父内容：仅在非子路由时显示 -->
    <template v-if="!isChildRoute">
      <!-- 第一行：上传卡片（两栏：PDF | EXCEL） -->
      <el-row :gutter="20">
        <el-col :span="12">
          <!-- PDF上传卡片 -->
          <el-card class="upload-card retro-card" shadow="never">
            <div slot="header" class="retro-card-header">
              <span><i class="el-icon-upload"></i> <span class="card-title">上传pdf</span></span>
              <el-tag size="small" class="retro-tag">.PDF</el-tag>
            </div>

            <div class="upload-area">
              <el-upload
                class="upload-dragger"
                drag
                action="#"
                :auto-upload="false"
                :on-change="handlePdfChange"
                :file-list="pdfFileList"
                accept=".pdf"
              >
                <i class="el-icon-upload"></i>
                <div class="el-upload__text">拖拽 PDF 文件至此处 或 <em>点击上传</em></div>
                <div class="el-upload__tip" slot="tip">支持学术文献 PDF，最大 50MB</div>
              </el-upload>
            </div>

            <div v-if="currentPdfFile" class="confirm-block">
              <div class="confirm-text">
                <span class="strong">是否确认上传该文件</span>
                <span class="file-name">（{{ currentPdfFile.name }}）</span>
              </div>
              <div class="confirm-actions">
                <div class="file-size-tag">
                  <i class="el-icon-data-analysis"></i> 文件大小: {{ formatFileSize(currentPdfFile.size) }}
                </div>
                <div class="button-group">
                  <el-button type="primary" size="medium" @click="handlePdfConfirm(true)" :loading="pdfUploading" class="retro-btn primary">是</el-button>
                  <el-button size="medium" @click="handlePdfConfirm(false)" class="retro-btn">否</el-button>
                </div>
              </div>
            </div>
          </el-card>
        </el-col>

        <el-col :span="12">
          <!-- EXCEL上传卡片 -->
          <el-card class="upload-card retro-card" shadow="never">
            <div slot="header" class="retro-card-header">
              <span><i class="el-icon-upload"></i> <span class="card-title">上传excel</span></span>
              <el-tag size="small" class="retro-tag">.XLSX .XLS .CSV</el-tag>
            </div>

            <div class="upload-area">
              <el-upload
                class="upload-dragger"
                drag
                action="#"
                :auto-upload="false"
                :on-change="handleFileChange"
                :on-remove="handleRemove"
                :file-list="fileList"
                accept=".xlsx,.xls,.csv"
              >
                <i class="el-icon-upload"></i>
                <div class="el-upload__text">拖拽 EXCEL 文件至此处 或 <em>点击上传</em></div>
                <div class="el-upload__tip" slot="tip">仅支持学术文献数据表格，最大 50MB</div>
              </el-upload>
            </div>

            <div v-if="currentFile" class="confirm-block">
              <div class="confirm-text">
                <span class="strong">是否确认上传该文件</span>
                <span class="file-name">（{{ currentFile.name }}）</span>
                <span class="hint-text">(如果选择手动填写信息则忽略此项)</span>
              </div>
              <div class="confirm-actions">
                <div class="file-size-tag">
                  <i class="el-icon-data-analysis"></i> 文件大小: {{ formatFileSize(currentFile.size) }}
                </div>
                <div class="button-group">
                  <el-button type="primary" size="medium" @click="handleConfirm(true)" :loading="uploading" class="retro-btn primary">是</el-button>
                  <el-button size="medium" @click="handleConfirm(false)" class="retro-btn">否</el-button>
                </div>
              </div>
            </div>

            <transition name="fade">
              <div v-if="uploadStatus" class="status-alert">
                <el-alert :title="uploadStatus.message" :type="uploadStatus.type" :closable="false" show-icon />
              </div>
            </transition>
          </el-card>
        </el-col>
      </el-row>

      <!-- 第二行：手动填写信息卡片 -->
      <el-row style="margin-top: 20px;">
        <el-col :span="24">
          <el-card class="retro-card" shadow="never">
            <div slot="header" class="retro-card-header">
              <span><i class="el-icon-edit-outline"></i> <span class="card-title">手动填写信息</span></span>
              <el-tag size="small" class="retro-tag">手动录入</el-tag>
            </div>

            <div class="manual-form">
              <el-form :model="manualForm" label-width="180px" label-position="left">
                <el-form-item label="标题（英文）">
                  <el-input v-model="manualForm.title" placeholder="请输入英文标题"></el-input>
                </el-form-item>
                <el-form-item label="作者（英文）">
                  <el-input v-model="manualForm.authors" placeholder="多个作者用英文逗号分隔"></el-input>
                </el-form-item>
                <el-form-item label="通讯作者（英文(以英文逗号相隔）">
                  <el-input v-model="manualForm.corresponding" placeholder="请输入通讯作者，多个用英文逗号分隔"></el-input>
                </el-form-item>
                <el-form-item label="摘要（英文）">
                  <el-input type="textarea" :rows="3" v-model="manualForm.abstract" placeholder="请输入英文摘要"></el-input>
                </el-form-item>
                <el-form-item label="关键词（英文）">
                  <el-input v-model="manualForm.keywords" placeholder="多个关键词用英文逗号分隔"></el-input>
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="submitManualForm" class="retro-btn primary">保存文献</el-button>
                  <el-button @click="resetManualForm" class="retro-btn">重置</el-button>
                </el-form-item>
              </el-form>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 第三行：手动录入数据列表卡片（新增） -->
      <el-row style="margin-top: 20px;">
        <el-col :span="24">
          <el-card class="retro-card" shadow="never">
            <div slot="header" class="retro-card-header">
              <span><i class="el-icon-document-copy"></i> <span class="card-title">手动录入数据列表</span></span>
              <el-badge :value="manualTotalCount" :hidden="!manualTotalCount" class="retro-badge">
                <el-button size="small" icon="el-icon-refresh" circle @click="refreshManualData" class="retro-icon-btn"></el-button>
              </el-badge>
            </div>

            <div class="table-wrapper">
              <template v-if="manualTableData.length > 0">
                <el-table
                  :data="manualTableData"
                  border
                  stripe
                  size="small"
                  style="width: 100%"
                  height="300"
                  v-loading="manualLoading"
                  class="retro-table"
                >
                  <el-table-column
                    v-for="(header, index) in manualHeaders"
                    :key="index"
                    :prop="header"
                    :label="header"
                    :min-width="header === 'abstract' ? '200' : '120'"
                    :show-overflow-tooltip="true"
                  >
                    <template slot-scope="scope">
                      <span v-if="header === 'abstract'">{{ truncateAbstract(scope.row[header]) }}</span>
                      <span v-else>{{ scope.row[header] || '—' }}</span>
                    </template>
                  </el-table-column>
                  <el-table-column label="操作" width="80" fixed="right">
                    <template slot-scope="scope">
                      <el-button type="primary" size="mini" icon="el-icon-view" circle @click="handleViewDetail(scope.row)" class="retro-icon-btn primary"></el-button>
                    </template>
                  </el-table-column>
                </el-table>

                <div class="pagination-wrapper">
                  <el-pagination
                    @current-change="handleManualPageChange"
                    :current-page.sync="manualCurrentPage"
                    :page-size="manualPageSize"
                    layout="total, prev, pager, next, jumper"
                    :total="manualTotalCount"
                    background
                    class="retro-pagination"
                  />
                </div>
              </template>

              <div v-else class="empty-state">
                <i class="el-icon-folder-opened"></i>
                <p>暂无手动录入数据</p>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 第四行：Excel文献数据列表卡片 -->
      <el-row style="margin-top: 20px;">
        <el-col :span="24">
          <el-card class="retro-card" shadow="never">
            <div slot="header" class="retro-card-header">
              <span><i class="el-icon-document-copy"></i> <span class="card-title">Excel文献数据列表</span></span>
              <el-badge :value="totalCount" :hidden="!totalCount" class="retro-badge">
                <el-button size="small" icon="el-icon-refresh" circle @click="refreshData" class="retro-icon-btn"></el-button>
              </el-badge>
            </div>

            <div class="table-wrapper">
              <template v-if="tableData.length > 0">
                <el-table
                  :data="tableData"
                  border
                  stripe
                  size="small"
                  style="width: 100%"
                  height="400"
                  v-loading="loading"
                  class="retro-table"
                >
                  <el-table-column
                    v-for="(header, index) in headers"
                    :key="index"
                    :prop="header"
                    :label="header"
                    :min-width="header === '摘要' ? '200' : '120'"
                    :show-overflow-tooltip="true"
                  >
                    <template slot-scope="scope">
                      <span v-if="header === '摘要'">{{ truncateAbstract(scope.row[header]) }}</span>
                      <span v-else>{{ scope.row[header] || '—' }}</span>
                    </template>
                  </el-table-column>
                  <el-table-column label="操作" width="80" fixed="right">
                    <template slot-scope="scope">
                      <el-button type="primary" size="mini" icon="el-icon-view" circle @click="handleViewDetail(scope.row)" class="retro-icon-btn primary"></el-button>
                    </template>
                  </el-table-column>
                </el-table>

                <div class="pagination-wrapper">
                  <el-pagination
                    @current-change="handlePageChange"
                    :current-page.sync="currentPage"
                    :page-size="pageSize"
                    layout="total, prev, pager, next, jumper"
                    :total="totalCount"
                    background
                    class="retro-pagination"
                  />
                </div>
              </template>

              <div v-else class="empty-state">
                <i class="el-icon-folder-opened"></i>
                <p>暂无Excel数据，请上传文件</p>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 综合可信度报告按钮已移除，现在放在弹窗中 -->
    </template>

    <!-- 子路由出口 -->
    <router-view></router-view>

    <!-- 详情弹窗（用于 Excel 数据），现在包含报告按钮 -->
    <el-dialog title="条目详情" :visible.sync="dialogVisible" width="50%" class="retro-dialog">
      <el-descriptions :column="1" border size="small" class="retro-descriptions" v-if="dialogDetailData">
        <el-descriptions-item v-for="(value, key) in dialogDetailData" :key="key" :label="key">
          <div class="detail-value">{{ value || '—' }}</div>
        </el-descriptions-item>
      </el-descriptions>
      <span slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false" class="retro-btn">关闭</el-button>
        <el-button type="primary" @click="goToCredibilityFromDialog" class="retro-btn primary">综合可信度报告</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import axios from 'axios'

// 后端 API 基础地址
const API_BASE_URL = 'http://localhost:3007'

export default {
  name: 'LiteratureUpload',
  data() {
    return {
      pdfFileList: [],
      currentPdfFile: null,
      pdfUploading: false,
      fileList: [],
      currentFile: null,
      uploadStatus: null,
      uploading: false,
      loading: false,
      headers: [],
      tableData: [],
      totalCount: 0,
      currentPage: 1,
      pageSize: 10,
      currentFileName: '',
      manualLoading: false,
      manualHeaders: ['title', 'authors', 'corresponding', 'abstract', 'keywords'],
      manualTableData: [],
      manualTotalCount: 0,
      manualCurrentPage: 1,
      manualPageSize: 10,
      detailData: null,
      manualForm: {
        title: '',
        authors: '',
        corresponding: '',
        abstract: '',
        keywords: ''
      },
      dialogVisible: false,
      dialogDetailData: null
    }
  },
  computed: {
    isChildRoute() {
      const path = this.$route.path
      return path.startsWith('/view/literature/') && path !== '/view/literature'
    }
  },
  mounted() {
    this.fetchManualData()
  },
  methods: {
    formatFileSize(bytes) {
      if (bytes === 0) return '0 B'
      const k = 1024
      const sizes = ['B', 'KB', 'MB', 'GB']
      const i = Math.floor(Math.log(bytes) / Math.log(k))
      return (bytes / Math.pow(k, i)).toFixed(2) + ' ' + sizes[i]
    },
    truncateAbstract(text) {
      if (!text) return '—'
      if (text.length > 100) return text.substring(0, 100) + '...'
      return text
    },
    handlePdfChange(file, fileList) {
      this.pdfFileList = fileList
      this.currentPdfFile = fileList.length ? fileList[0] : null
    },
    handlePdfConfirm(confirmed) {
      if (!this.currentPdfFile) {
        this.$message.warning('请先选择文件')
        return
      }
      if (!confirmed) {
        this.currentPdfFile = null
        this.pdfFileList = []
        return
      }
      this.pdfUploading = true
      setTimeout(() => {
        this.$message.success('PDF上传成功（模拟）')
        this.pdfUploading = false
        this.currentPdfFile = null
        this.pdfFileList = []
      }, 1500)
    },
    handleFileChange(file, fileList) {
      this.fileList = fileList
      this.currentFile = fileList.length ? fileList[0] : null
      this.uploadStatus = null
    },
    handleRemove(file, fileList) {
      this.fileList = fileList
      this.currentFile = fileList.length ? fileList[0] : null
    },
    async handleConfirm(confirmed) {
      if (!this.currentFile) {
        this.$message.warning('请先选择文件')
        return
      }
      if (!confirmed) {
        this.uploadStatus = { type: 'error', message: '已取消上传' }
        this.currentFile = null
        this.fileList = []
        return
      }
      this.uploading = true
      this.uploadStatus = { type: 'info', message: '正在上传...' }
      const formData = new FormData()
      formData.append('file', this.currentFile.raw)
      try {
        const response = await axios.post(`${API_BASE_URL}/api/upload`, formData)
        if (response.data.success) {
          this.uploadStatus = { type: 'success', message: '上传成功！共 ' + response.data.totalCount + ' 条记录' }
          this.handlePageChange(1)
          this.$message.success('文件上传成功，共 ' + response.data.totalCount + ' 条数据')
        } else {
          this.uploadStatus = { type: 'error', message: '上传失败：' + (response.data.error || '未知错误') }
        }
      } catch (error) {
        console.error('上传错误:', error)
        this.uploadStatus = { type: 'error', message: '上传失败：' + error.message }
      } finally {
        this.uploading = false
        this.currentFile = null
        this.fileList = []
      }
    },
    async handlePageChange(page) {
      this.currentPage = page
      this.loading = true
      try {
        const response = await axios.get(`${API_BASE_URL}/api/data`, {
          params: { page: page, pageSize: this.pageSize }
        })
        this.tableData = response.data.data
        this.totalCount = response.data.totalCount
        this.headers = response.data.headers
        this.currentFileName = response.data.fileName
      } catch (error) {
        console.error('获取数据错误:', error)
        this.$message.error('获取数据失败')
      } finally {
        this.loading = false
      }
    },
    refreshData() {
      if (this.totalCount > 0) this.handlePageChange(this.currentPage)
    },
    async fetchManualData(page = this.manualCurrentPage) {
      this.manualLoading = true
      try {
        const response = await axios.get(`${API_BASE_URL}/api/manual-data`, {
          params: { page, pageSize: this.manualPageSize }
        })
        this.manualTableData = response.data.data
        this.manualTotalCount = response.data.totalCount
      } catch (error) {
        console.error('获取手动数据错误:', error)
        this.$message.error('获取手动数据失败，请确保后端服务已启动')
      } finally {
        this.manualLoading = false
      }
    },
    handleManualPageChange(page) {
      this.manualCurrentPage = page
      this.fetchManualData(page)
    },
    refreshManualData() {
      this.fetchManualData(this.manualCurrentPage)
    },
    handleViewDetail(row) {
      this.dialogDetailData = row
      this.dialogVisible = true
    },
    async submitManualForm() {
      if (!this.manualForm.title || !this.manualForm.authors) {
        this.$message.warning('标题和作者为必填项')
        return
      }
      try {
        const response = await axios.post(`${API_BASE_URL}/api/manual`, this.manualForm)
        if (response.data.success) {
          this.$message.success('文献已保存')
          this.resetManualForm()
          this.manualCurrentPage = 1
          this.fetchManualData(1)
        } else {
          this.$message.error('保存失败')
        }
      } catch (error) {
        console.error('手动录入错误:', error)
        this.$message.error('保存失败，请检查后端服务')
      }
    },
    resetManualForm() {
      this.manualForm = {
        title: '',
        authors: '',
        corresponding: '',
        abstract: '',
        keywords: ''
      }
    },
    goToCredibilityFromDialog() {
      if (this.totalCount === 0 && this.manualTotalCount === 0) {
        this.$message.warning('请先上传文件或录入数据！')
        return
      }
      const id = this.dialogDetailData ? this.dialogDetailData.id : null
      if (!id) {
        this.$message.warning('无法获取文献ID')
        return
      }
      this.dialogVisible = false
      this.$router.push({
        path: '/view/literature/credibility',
        query: { id }
      })
    }
  }
}
</script>

<style scoped>
/* ========== 全局重置 ========== */
.literature-upload-container {
  font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif;
  padding: 20px;
}

/* ========== 动画 ========== */
.fade-enter-active, .fade-leave-active {
  transition: opacity 0.3s;
}
.fade-enter, .fade-leave-to {
  opacity: 0;
}

/* ========== 卡片通用样式 ========== */
.literature-upload-container .el-card {
  border: 2px solid #3C848C;
  border-radius: 0;
  box-shadow: none !important;
  margin-bottom: 0;
  background-color: #FFFFFF;
}

/* 卡片头部 */
.literature-upload-container .el-card__header {
  color: #3C848C;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 1px;
  padding: 14px 20px;
  border-bottom: 2px solid #3C848C;
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
  color: #3C848C;
  font-size: 14px;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.retro-tag {
  background-color: #BDCACC;
  color: #3C848C;
  border: none;
  font-weight: 700;
  text-transform: uppercase;
  border-radius: 0;
  padding: 4px 12px;
  font-size: 12px;
}

/* ========== 上传区域 ========== */
.upload-area {
  padding: 10px 0;
}

.upload-dragger {
  width: 100%;
}

.upload-dragger .el-upload-dragger {
  border: 2px dashed #3C848C;
  border-radius: 0;
  background-color: #FFFFFF;
  width: 100%;
  height: auto;
  padding: 30px 0;
}

.upload-dragger .el-upload-dragger .el-icon-upload {
  color: #3C848C;
  font-size: 48px;
}

.upload-dragger .el-upload__text {
  color: #3C848C;
  font-weight: 700;
  text-transform: uppercase;
  font-size: 14px;
}

.upload-dragger .el-upload__text em {
  color: #BDCACC;
  font-style: normal;
  font-weight: 800;
}

.upload-dragger .el-upload__tip {
  color: #666666;
  margin-top: 10px;
  font-size: 12px;
  text-transform: uppercase;
}

/* 确认区块 */
.confirm-block {
  background-color: #F5F5F5;
  border: 2px solid #3C848C;
  padding: 20px;
  margin-top: 20px;
}

.confirm-text {
  margin-bottom: 15px;
  font-size: 16px;
}

.confirm-text .strong {
  font-weight: 800;
  text-transform: uppercase;
  color: #3C848C;
}

.confirm-text .file-name {
  color: #666666;
  font-weight: 400;
}

.confirm-text .hint-text {
  color: #BDCACC;
  font-weight: 600;
  margin-left: 8px;
  font-size: 13px;
  text-transform: none;
}

.confirm-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 15px;
}

.file-size-tag {
  display: flex;
  align-items: center;
  background-color: #BDCACC;
  color: #3C848C;
  font-weight: 700;
  text-transform: uppercase;
  padding: 8px 16px;
  border: 2px solid #3C848C;
  font-size: 13px;
}

.file-size-tag i {
  margin-right: 8px;
  color: #3C848C;
}

.button-group {
  display: flex;
  gap: 12px;
}

/* ========== 按钮样式 ========== */
.retro-btn {
  border-radius: 0;
  border: 2px solid #3C848C;
  font-weight: 800;
  text-transform: uppercase;
  padding: 10px 20px;
  font-size: 14px;
  letter-spacing: 1px;
  transition: none;
}

.retro-btn.primary {
  background-color: #3C848C;
  color: #FFFFFF;
  border-color: #3C848C;
}

.retro-btn.primary:hover {
  background-color: #2F696F;
  border-color: #2F696F;
}

.retro-btn:not(.primary) {
  background-color: #FFFFFF;
  color: #3C848C;
}

.retro-btn:not(.primary):hover {
  background-color: #EEEEEE;
}

.retro-btn.large {
  padding: 12px 30px;
  font-size: 16px;
}

.retro-icon-btn {
  border: 2px solid #3C848C;
  border-radius: 0;
  background-color: #FFFFFF;
  color: #3C848C;
  font-weight: 800;
}

.retro-icon-btn.primary {
  background-color: #3C848C;
  color: #FFFFFF;
}

.retro-icon-btn:hover {
  background-color: #EEEEEE;
}

/* ========== 表格样式 ========== */
.retro-table {
  border: 2px solid #3C848C;
}

.retro-table::v-deep .el-table__header-wrapper th {
  background-color: #3C848C;
  color: #FFFFFF;
  font-weight: 800;
  text-transform: uppercase;
  border-right: 1px solid #BDCACC;
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

/* 分页 */
.pagination-wrapper {
  margin-top: 20px;
  text-align: right;
}

.retro-pagination::v-deep .el-pagination button,
.retro-pagination::v-deep .el-pagination span:not(.el-pagination__total) {
  border: 2px solid #3C848C;
  border-radius: 0;
  background-color: #FFFFFF;
  color: #3C848C;
  font-weight: 700;
  min-width: 35px;
  height: 35px;
  line-height: 35px;
}

.retro-pagination::v-deep .el-pagination .active {
  background-color: #3C848C;
  color: #FFFFFF;
  border-color: #3C848C;
}

.retro-pagination::v-deep .el-pagination__total {
  font-weight: 700;
  color: #3C848C;
}

/* ========== 空状态 ========== */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
  color: #999999;
  font-weight: 700;
  text-transform: uppercase;
  border: 2px dashed #BDCACC;
}

.empty-state i {
  font-size: 48px;
  margin-bottom: 16px;
  color: #3C848C;
}

/* ========== 手动录入表单 ========== */
.manual-form {
  padding: 20px;
}

.manual-form .el-form-item {
  margin-bottom: 18px;
}

.manual-form .el-form-item__label {
  font-weight: 700;
  color: #3C848C;
  text-transform: uppercase;
  font-size: 13px;
}

.manual-form .el-input__inner,
.manual-form .el-textarea__inner {
  border: 2px solid #3C848C;
  border-radius: 0;
}

.manual-form .el-input__inner:focus,
.manual-form .el-textarea__inner:focus {
  border-color: #2F696F;
}

/* ========== 弹窗样式 ========== */
.retro-dialog ::v-deep .el-dialog {
  border: 2px solid #3C848C;
  border-radius: 0;
  box-shadow: none;
}

.retro-dialog ::v-deep .el-dialog__header {
  background-color: #3C848C;
  color: #FFFFFF;
  padding: 14px 20px;
  border-bottom: 2px solid #3C848C;
}

.retro-dialog ::v-deep .el-dialog__title {
  color: #FFFFFF;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.retro-dialog ::v-deep .el-dialog__headerbtn .el-dialog__close {
  color: #FFFFFF;
}

.retro-dialog ::v-deep .el-dialog__body {
  padding: 20px;
  background-color: #FFFFFF;
}

.retro-dialog .dialog-footer {
  text-align: right;
  padding-top: 10px;
}
</style>