server/
├── server.js                    // 主入口文件
├── package.json                 // Node.js 依赖列表
├── package-lock.json            // 依赖版本锁定（建议包含）
├── db.js                        // 数据库连接配置
│
├── routes/                      // 所有 API 路由文件
│   ├── excel.js                 // Excel 上传
│   ├── manual.js                // 手动录入
│   ├── manualData.js            // 手动录入数据列表
│   ├── id.js                    // 文献详情
│   ├── authorContribution.js    // 作者贡献度
│   ├── dateRange.js             // 日期范围
│   ├── network.js               // 作者合作网络
│   └── pdf.js                   // PDF 上传
│
├── utils/                       // 工具函数
│   ├── uploadConfig.js          // 上传配置（multer）
│   └── nameNormalizer.js        // 作者名标准化
│
├── python/                      // Python 计算引擎（全部）
│   ├── compute_credibility.py   // 主计算脚本
│   ├── cred_time.py             // 时效可信度算法
│   ├── cred_print.py            // 期刊可信度算法
│   ├── cred_author.py           // 作者可信度算法
│   ├── ewm.py                   // 熵权法
│   ├── data_utils.py            // 数据工具
│   └── requirements.txt         // Python 依赖列表
│
└── uploads/                     // 上传临时目录
