<template>
  <div class="preprocess-container">

    <div class="main-layout">
      <!-- 左侧：文件选择区域 -->
      <div class="left-panel">
        <div class="panel-header">
          <h3>已上传文件 <span class="count">({{ uploadedFiles.length }})</span></h3>
          <router-link to="/view/literature" class="upload-link">+ 上传新文件</router-link>
        </div>

        <div v-if="uploadedFiles.length > 0" class="file-select-list">
          <div
            v-for="file in uploadedFiles"
            :key="file.id"
            :class="['file-select-item', { active: selectedFileId === file.id }]"
            @click="selectFile(file)"
          >
            <div class="file-select-info">
              <span class="file-icon">📑</span>
              <div class="file-details">
                <div class="file-name">{{ file.name }}</div>
                <div class="file-meta">
                  <span>{{ file.size }}</span>
                  <span>{{ file.uploadTime }}</span>
                </div>
              </div>
            </div>
            <span v-if="selectedFileId === file.id" class="selected-badge">✓ 已选</span>
          </div>
        </div>

        <div v-else class="empty-state">
          <p>暂无已上传文件</p>
          <router-link to="/view/literature" class="upload-link">前往上传页面</router-link>
        </div>

        <!-- 操作按钮组 -->
        <div v-if="selectedFile" class="action-buttons">
          <button
            class="action-btn primary"
            :disabled="isProcessing"
            @click="startOCR"
          >
            <span v-if="isProcessing" class="spinner-small"></span>
            {{ isProcessing ? '识别中...' : '开始识别' }}
          </button>
          <button
            class="action-btn secondary"
            :disabled="!ocrResult.text"
            @click="exportResult"
          >
            💾 导出结果
          </button>
          <button
            class="action-btn secondary"
            :disabled="!ocrResult.text"
            @click="copyToClipboard"
          >
            📋 复制文本
          </button>
        </div>
      </div>

      <!-- 右侧：PDF 预览和识别结果区域 -->
      <div class="right-panel">
        <div class="tabs">
          <button
            :class="['tab-btn', { active: activeTab === 'preview' }]"
            @click="activeTab = 'preview'"
          >
            📄 PDF 预览
          </button>
          <button
            :class="['tab-btn', { active: activeTab === 'result' }]"
            @click="activeTab = 'result'"
          >
            📝 识别结果
            <span v-if="ocrResult.text" class="badge">新</span>
          </button>
        </div>

        <!-- PDF 预览标签页 -->
        <div v-show="activeTab === 'preview'" class="pdf-preview-container">
          <div v-if="isLoadingPdf" class="loading-state">
            <div class="spinner"></div>
            <p>正在加载 PDF...</p>
          </div>

          <div v-else-if="pdfPages.length > 0" class="pdf-viewer">
            <div
              v-for="(page, index) in pdfPages"
              :key="index"
              class="pdf-page"
            >
              <div class="page-header-inner">
                <span class="page-number">第 {{ index + 1 }} / {{ pdfPages.length }} 页</span>
              </div>
              <canvas :ref="`canvas-${index}`" class="pdf-canvas"></canvas>
            </div>
          </div>

          <div v-else class="empty-state">
            <p>请先选择要识别的文件</p>
          </div>
        </div>

        <!-- 识别结果标签页 -->
        <div v-show="activeTab === 'result'" class="result-container">
          <div v-if="isProcessing" class="loading-state">
            <div class="spinner"></div>
            <p>正在识别文档，请稍候...</p>
            <p class="loading-tip">首次使用需下载模型，可能需要 1-2 分钟</p>
          </div>

          <div v-else-if="error" class="error-state">
            <p>❌ {{ error }}</p>
            <button class="retry-btn" @click="startOCR">重试</button>
          </div>

          <div v-else-if="ocrResult.text" class="result-content">
            <div class="result-stats">
              <span>总页数: {{ ocrResult.total_pages || 1 }}</span>
              <span>字符数: {{ ocrResult.text.length }}</span>
              <span>行数: {{ lineCount }}</span>
            </div>

            <div class="result-text-area">
              <textarea
                v-model="editableText"
                class="result-textarea"
                placeholder="识别结果将显示在这里，您可以编辑..."
              ></textarea>
            </div>
          </div>

          <div v-else class="empty-state">
            <p>请先选择文件并点击"开始识别"</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios'

export default {
  name: 'PreprocessView',
  data() {
    return {
      uploadedFiles: [],
      selectedFileId: null,
      selectedFile: null,
      selectedFileData: null,

      isProcessing: false,
      isLoadingPdf: false,
      error: null,

      ocrResult: {
        text: '',
        total_pages: 0,
        filename: ''
      },
      editableText: '',

      pdfPages: [],
      pdfDoc: null,
      activeTab: 'preview'
    }
  },

  computed: {
    lineCount() {
      if (!this.ocrResult.text) return 0
      return this.ocrResult.text.split('\n').length
    }
  },

  mounted() {
    this.loadFileList()
    this.loadPDFJS()

    const fileId = this.$route.query.fileId
    if (fileId) {
      const file = this.uploadedFiles.find(f => f.id == fileId)
      if (file) {
        this.selectFile(file)
      }
    }
  },

  methods: {
    loadFileList() {
      const files = localStorage.getItem('uploaded_pdfs')
      if (files) {
        this.uploadedFiles = JSON.parse(files)
      }
    },

    loadPDFJS() {
      return new Promise((resolve, reject) => {
        if (window.pdfjsLib) {
          resolve(window.pdfjsLib)
          return
        }

        const script = document.createElement('script')
        script.src = 'https://cdnjs.cloudflare.com/ajax/libs/pdf.js/2.16.105/pdf.min.js'
        script.onload = () => {
          window.pdfjsLib.GlobalWorkerOptions.workerSrc = 'https://cdnjs.cloudflare.com/ajax/libs/pdf.js/2.16.105/pdf.worker.min.js'
          resolve(window.pdfjsLib)
        }
        script.onerror = () => {
          console.error('PDF.js 加载失败')
          reject(new Error('PDF.js 加载失败'))
        }
        document.head.appendChild(script)
      })
    },

    formatFileSize(bytes) {
      if (bytes < 1024) return bytes + ' B'
      if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
      return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
    },

    async selectFile(file) {
      this.selectedFileId = file.id
      this.selectedFile = {
        name: file.name,
        size: this.formatFileSize(parseInt(file.size) || 0)
      }
      this.selectedFileData = file.content
      this.ocrResult = { text: '', total_pages: 0, filename: '' }
      this.editableText = ''
      this.error = null
      this.activeTab = 'preview'

      await this.loadPdfPreview()
    },

    async loadPdfPreview() {
  if (!this.selectedFileData) return

  if (!window.pdfjsLib) {
    await this.loadPDFJS()
  }

  this.isLoadingPdf = true
  this.pdfPages = []

  try {
    const base64Data = this.selectedFileData.split(',')[1]
    const binaryData = atob(base64Data)
    const array = new Uint8Array(binaryData.length)
    for (let i = 0; i < binaryData.length; i++) {
      array[i] = binaryData.charCodeAt(i)
    }

    const loadingTask = window.pdfjsLib.getDocument({ data: array })
    this.pdfDoc = await loadingTask.promise

    const totalPages = this.pdfDoc.numPages
    const pages = []
    const displayCount = Math.min(totalPages, 3)
    
    for (let i = 1; i <= displayCount; i++) {
      const page = await this.pdfDoc.getPage(i)
      const viewport = page.getViewport({ scale: 1.5 })

      pages.push({
        pageNum: i,
        width: viewport.width,
        height: viewport.height
      })
    }
    
    this.pdfPages = pages
    
    await this.$nextTick()
    
    for (let i = 1; i <= displayCount; i++) {
      this.renderPage(i)
    }

  } catch (error) {
    console.error('PDF 加载失败:', error)
    this.error = 'PDF 加载失败，请检查文件是否损坏'
  } finally {
    this.isLoadingPdf = false
  }
},

    async renderPage(pageNum, retryCount = 0) {
  if (!this.pdfDoc) return

  try {
    const page = await this.pdfDoc.getPage(pageNum)
    const canvasRef = this.$refs[`canvas-${pageNum - 1}`]
    
    if (!canvasRef || !canvasRef[0]) {
      if (retryCount < 5) {
        setTimeout(() => {
          this.renderPage(pageNum, retryCount + 1)
        }, 200)
      }
      return
    }

    const canvas = canvasRef[0]
    const context = canvas.getContext('2d')
    const viewport = page.getViewport({ scale: 1.5 })

    canvas.width = viewport.width
    canvas.height = viewport.height

    await page.render({
      canvasContext: context,
      viewport: viewport
    }).promise

  } catch (error) {
    console.error(`渲染第 ${pageNum} 页失败:`, error)
  }
},

    async startOCR() {
  if (!this.selectedFileData) {
    this.error = '请先选择文件'
    return
  }

  this.isProcessing = true
  this.error = null
  this.activeTab = 'result'

  try {
    const base64Data = this.selectedFileData.split(',')[1]
    const binaryData = atob(base64Data)
    const array = new Uint8Array(binaryData.length)
    for (let i = 0; i < binaryData.length; i++) {
      array[i] = binaryData.charCodeAt(i)
    }
    const blob = new Blob([array], { type: 'application/pdf' })

    const formData = new FormData()
    formData.append('file', blob, this.selectedFile.name)

    // 调用 Python OCR 服务 (端口 8082)
    const response = await axios.post('http://localhost:8082/api/ocr/pdf', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 120000
    })

    if (response.data.success) {
      const ocrText = response.data.text
      
      this.ocrResult = {
        text: ocrText,
        total_pages: response.data.total_pages || 1,
        filename: this.selectedFile.name
      }
      this.editableText = ocrText
      
      this.$message.success('识别完成')
    } else {
      this.error = response.data.error || '识别失败'
    }

  } catch (err) {
    console.error('OCR 请求失败:', err)
    if (err.code === 'ECONNABORTED') {
      this.error = '请求超时，文件可能过大，请尝试较小的文件'
    } else if (err.response && err.response.status === 500) {
      this.error = '服务器处理失败，请检查后端服务是否正常运行'
    } else if (err.response && err.response.status === 400) {
      this.error = err.response.data.detail || '文件格式错误'
    } else {
      this.error = '识别失败: ' + (err.message || '网络错误')
    }
  } finally {
    this.isProcessing = false
  }
},

// 新增：保存 OCR 结果到数据库
async saveOCRResult(text) {
  try {
    // 构造文献数据对象
    const literatureData = {
      title: this.selectedFile.name.replace('.pdf', ''),
      content: text,
      fileName: this.selectedFile.name,
      fileSize: this.selectedFile.size,
      uploadTime: new Date().toISOString(),
      textLength: text.length
    }
    
    // 调用 Spring Boot 的保存接口
    const response = await axios.post('http://localhost:8080/api/literature/save', literatureData, {
      headers: { 'Content-Type': 'application/json' }
    })
    
    if (response.data.success || response.status === 200) {
      console.log('文献已保存到数据库')
      this.$message.success('识别结果已保存到数据库')
    }
  } catch (error) {
    console.error('保存 OCR 结果失败:', error)
    // 即使保存失败也不影响显示识别结果
  }
},

    exportResult() {
      if (!this.editableText) return

      const blob = new Blob([this.editableText], { type: 'text/plain' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `${this.selectedFile.name.replace('.pdf', '')}_识别结果.txt`
      a.click()
      URL.revokeObjectURL(url)
    },

    async copyToClipboard() {
      try {
        await navigator.clipboard.writeText(this.editableText)
        alert('已复制到剪贴板')
      } catch (err) {
        alert('复制失败，请手动选择复制')
      }
    }
  }
}
</script>

<style scoped>
.preprocess-container {
  padding: 24px;
  min-height: 100vh;
  background: #f5f7fa;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h2 {
  font-size: 24px;
  font-weight: 600;
  color: #1f2f3d;
  margin-bottom: 8px;
}

.page-header p {
  color: #6c7a8e;
  font-size: 14px;
}

.main-layout {
  display: grid;
  grid-template-columns: 360px 1fr;
  gap: 24px;
}

@media (max-width: 768px) {
  .main-layout {
    grid-template-columns: 1fr;
  }
}

/* 左侧面板 */
.left-panel {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  display: flex;
  flex-direction: column;
  max-height: calc(100vh - 120px);
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #e8ecf0;
}

.panel-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: #1f2f3d;
}

.panel-header .count {
  color: #8a99aa;
  font-weight: normal;
}

.upload-link {
  color: #409eff;
  text-decoration: none;
  font-size: 13px;
}

.upload-link:hover {
  text-decoration: underline;
}

.file-select-list {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}

.file-select-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  background: #f8f9fc;
  border-radius: 8px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.file-select-item:hover {
  background: #f0f2f5;
}

.file-select-item.active {
  border: 1px solid #409eff;
  background: #ecf5ff;
}

.file-select-info {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 0;
}

.file-icon {
  font-size: 24px;
  flex-shrink: 0;
}

.file-details {
  flex: 1;
  min-width: 0;
}

.file-name {
  font-weight: 500;
  color: #1f2f3d;
  font-size: 14px;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-meta {
  display: flex;
  gap: 12px;
  font-size: 11px;
  color: #8a99aa;
}

.selected-badge {
  color: #409eff;
  font-weight: 500;
  font-size: 13px;
  flex-shrink: 0;
}

.empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  text-align: center;
  color: #8a99aa;
}

.empty-state .upload-link {
  margin-top: 12px;
}

.action-buttons {
  display: flex;
  gap: 12px;
  padding: 16px 20px;
  border-top: 1px solid #e8ecf0;
  flex-wrap: wrap;
}

.action-btn {
  padding: 8px 16px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 6px;
}

.action-btn.primary {
  background: #409eff;
  color: white;
}

.action-btn.primary:hover:not(:disabled) {
  background: #66b1ff;
}

.action-btn.secondary {
  background: #f0f2f5;
  color: #1f2f3d;
}

.action-btn.secondary:hover:not(:disabled) {
  background: #e4e7ed;
}

.action-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.spinner-small {
  display: inline-block;
  width: 14px;
  height: 14px;
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

/* 右侧面板 */
.right-panel {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  max-height: calc(100vh - 120px);
}

.tabs {
  display: flex;
  border-bottom: 1px solid #e8ecf0;
  background: white;
  padding: 0 20px;
  flex-shrink: 0;
}

.tab-btn {
  padding: 12px 20px;
  background: none;
  border: none;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  color: #8a99aa;
  transition: all 0.2s;
  position: relative;
  display: flex;
  align-items: center;
  gap: 6px;
}

.tab-btn.active {
  color: #409eff;
}

.tab-btn.active::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 0;
  right: 0;
  height: 2px;
  background: #409eff;
}

.badge {
  background: #409eff;
  color: white;
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 10px;
}

.pdf-preview-container {
  padding: 24px;
  overflow-y: auto;
  flex: 1;
}

.pdf-page {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  margin-bottom: 24px;
  padding: 16px;
}

.page-header-inner {
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #e8ecf0;
}

.page-number {
  font-size: 12px;
  color: #8a99aa;
}

.pdf-canvas {
  max-width: 100%;
  height: auto;
  display: block;
  margin: 0 auto;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.result-container {
  padding: 24px;
  overflow-y: auto;
  flex: 1;
}

.result-stats {
  display: flex;
  gap: 24px;
  padding: 12px 16px;
  background: #f8f9fc;
  border-radius: 8px;
  margin-bottom: 20px;
  font-size: 13px;
  color: #1f2f3d;
}

.result-textarea {
  width: 100%;
  min-height: 400px;
  padding: 16px;
  border: 1px solid #d9e0e8;
  border-radius: 8px;
  font-family: 'Monaco', 'Menlo', monospace;
  font-size: 13px;
  line-height: 1.6;
  resize: vertical;
  background: #fafbfc;
}

.result-textarea:focus {
  outline: none;
  border-color: #409eff;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  text-align: center;
  color: #8a99aa;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid #e8ecf0;
  border-top-color: #409eff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 16px;
}

.loading-tip {
  font-size: 12px;
  color: #f5a623;
  margin-top: 8px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  text-align: center;
  color: #8a99aa;
}

.error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  text-align: center;
  color: #f56c6c;
}

.retry-btn {
  margin-top: 16px;
  padding: 8px 24px;
  background: #f56c6c;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
