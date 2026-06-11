import pymysql
import re
from pathlib import Path

# ===== 修改这里的配置 =====
MYSQL_CONFIG = {
    'host': 'localhost',
    'user': 'root',
    'password': '123456',  # ← 改成你的MySQL密码
    'database': 'literature_db',
    'charset': 'utf8mb4',
    'use_unicode': True,
    'autocommit': True
}

# 直接用当前目录
TXT_DIR = Path(__file__).parent
# =========================

print("="*60)
print("📚 文献导入工具 (MySQL版)")
print("="*60)

# 1. 连接MySQL
print(f"\n步骤1: 连接MySQL...")
try:
    # 先不指定数据库，创建数据库
    temp_config = MYSQL_CONFIG.copy()
    temp_config.pop('database')
    conn = pymysql.connect(**temp_config)
    conn.set_charset('utf8mb4')
    cursor = conn.cursor()
    
    # 创建数据库（如果不存在）
    cursor.execute("CREATE DATABASE IF NOT EXISTS literature_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci")
    cursor.close()
    conn.close()
    
    # 重新连接指定数据库
    conn = pymysql.connect(**MYSQL_CONFIG)
    conn.set_charset('utf8mb4')
    cursor = conn.cursor()
    print(f"  ✅ 连接成功！")
except Exception as e:
    print(f"  ❌ 连接失败: {e}")
    print(f"  请检查MySQL是否启动，用户名密码是否正确")
    exit(1)

# 2. 删除旧表（如果有问题）
print(f"\n步骤2: 重新创建表...")
cursor.execute("DROP TABLE IF EXISTS literature")

# 创建新表（正确设置id自增）
create_table_sql = """
CREATE TABLE literature (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    authors TEXT,
    abstract TEXT,
    full_text LONGTEXT,
    filename VARCHAR(255),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
"""
cursor.execute(create_table_sql)
print(f"  ✅ 表创建成功")

# 3. 查找TXT文件
print(f"\n步骤3: 查找文献文件...")
txt_files = list(Path(TXT_DIR).glob("*.txt"))
print(f"  找到 {len(txt_files)} 个TXT文件")

if len(txt_files) == 0:
    print(f"  ❌ 没有找到TXT文件")
    exit(1)

# 4. 导入数据
print(f"\n步骤4: 开始导入...")
success_count = 0

for file_path in txt_files:
    try:
        print(f"\n  📄 处理: {file_path.name}")
        
        # 读取文件
        content = None
        for enc in ['utf-8', 'gbk', 'gb2312', 'utf-8-sig']:
            try:
                with open(file_path, 'r', encoding=enc, errors='ignore') as f:
                    content = f.read()
                print(f"    使用编码: {enc}")
                break
            except:
                continue
        
        if content is None:
            print(f"    ❌ 无法读取文件")
            continue
        
        lines = content.split('\n')
        
        # 提取标题
        title = lines[0].strip() if lines else ''
        title = re.sub(r'^\d+[：:.\s]*', '', title)
        title = title[:500]
        
        # 提取作者
        authors = lines[1].strip() if len(lines) > 1 else ''
        authors = authors[:5000]
        
        # 提取摘要
        abstract = ''
        for i, line in enumerate(lines):
            if 'Introduction' in line or 'ABSTRACT' in line:
                abstract = '\n'.join(lines[2:i]).strip()
                break
        
        if not abstract and len(lines) > 5:
            abstract = '\n'.join(lines[2:10]).strip()
        
        abstract = abstract[:5000]
        
        # 插入数据库
        sql = """
            INSERT INTO literature (title, authors, abstract, full_text, filename)
            VALUES (%s, %s, %s, %s, %s)
        """
        cursor.execute(sql, (
            title,
            authors,
            abstract,
            content[:50000],
            file_path.name
        ))
        
        print(f"    ✅ 标题: {title[:40]}...")
        success_count += 1
        
    except Exception as e:
        print(f"    ❌ 导入失败: {e}")

# 5. 提交
conn.commit()

# 6. 统计
cursor.execute("SELECT COUNT(*) FROM literature")
total = cursor.fetchone()[0]

cursor.close()
conn.close()

print(f"\n" + "="*60)
print(f"🎉 导入完成！")
print(f"  成功导入: {success_count} 篇")
print(f"  数据库总记录: {total} 篇")
print("="*60)

# 7. 预览
if total > 0:
    print(f"\n📋 数据预览：")
    conn = pymysql.connect(**MYSQL_CONFIG)
    cursor = conn.cursor()
    cursor.execute("SELECT id, title, filename FROM literature ORDER BY id DESC LIMIT 5")
    for row in cursor.fetchall():
        print(f"  [{row[0]}] {row[1][:40]}... ({row[2]})")
    cursor.close()
    conn.close()
else:
    print(f"\n⚠️ 没有数据导入成功，请检查错误信息")