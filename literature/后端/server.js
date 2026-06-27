const express = require('express');
const cors = require('cors');
const path = require('path');
const fs = require('fs');
const { exec } = require('child_process');

const app = express();
const PORT = 3007;

// 允许前端跨域携带凭证（解决 CORS 与 credentials 冲突）
app.use(cors({
  origin: 'http://localhost:7081',   // 请替换为你实际的前端地址和端口
  credentials: true
}));

app.use(express.json({ limit: '50mb' }));

const uploadDir = path.join(__dirname, 'uploads');
if (!fs.existsSync(uploadDir)) {
  fs.mkdirSync(uploadDir, { recursive: true });
}

// 路由
app.use('/api/upload', require('./routes/excel'));
app.use('/api/manual', require('./routes/manual'));
app.use('/api/upload-pdf', require('./routes/pdf'));
app.use('/api/data', require('./routes/data'));
app.use('/api/manual-data', require('./routes/manualData'));
app.use('/api/id', require('./routes/id'));
app.use('/api/author-contribution', require('./routes/authorContribution'));  // 新增
app.use('/api/date-range', require('./routes/dateRange'));
app.use('/api/network-data', require('./routes/network'));

// 计算全部文献
app.post('/api/compute-all', (req, res) => {
  const scriptPath = path.join(__dirname, 'python', 'compute_credibility.py');
  exec(`python "${scriptPath}"`, (error, stdout, stderr) => {
    if (error) {
      console.error(`执行错误: ${error}`);
      return res.status(500).json({ success: false, error: error.message });
    }
    console.log(stdout);
    console.error(stderr);
    res.json({ success: true, message: '批量计算完成', output: stdout });
  });
});

// 计算单篇（可选）
app.post('/api/compute/:id', (req, res) => {
  const paperId = req.params.id;
  const scriptPath = path.join(__dirname, 'python', 'compute_single.py');
  exec(`python "${scriptPath}" ${paperId}`, (error, stdout, stderr) => {
    // 实现逻辑...
  });
});

// 健康检查
app.get('/api/health', (req, res) => {
  res.json({ status: 'ok', message: '服务器运行正常' });
});

app.listen(PORT, () => {
  console.log(`服务器运行在 http://localhost:${PORT}`);
  console.log(`Excel上传: http://localhost:${PORT}/api/upload`);
  console.log(`手动录入: http://localhost:${PORT}/api/manual`);
  console.log(`PDF上传: http://localhost:${PORT}/api/upload-pdf`);
  console.log(`数据接口: http://localhost:${PORT}/api/data`);
  console.log(`文献详情: http://localhost:${PORT}/api/id/:id`);
  console.log(`作者贡献度: http://localhost:${PORT}/api/author-contribution/:id`);
  console.log(`日期范围: http://localhost:${PORT}/api/date-range`);
  console.log(`批量计算: http://localhost:${PORT}/api/compute-all`);
  console.log(`网络数据: http://localhost:${PORT}/api/network-data`);
});