-- 创建数据库
CREATE DATABASE IF NOT EXISTS literature_db CHARACTER SET utf8mb4;

-- 创建用户并授权
CREATE USER IF NOT EXISTS 'lit_user'@'localhost' IDENTIFIED BY '123456';
GRANT ALL PRIVILEGES ON literature_db.* TO 'lit_user'@'localhost';
FLUSH PRIVILEGES;

-- 使用数据库
USE literature_db;

-- 创建 papers 表
CREATE TABLE IF NOT EXISTS papers (
    id INT PRIMARY KEY AUTO_INCREMENT,
    chemical_system VARCHAR(100),
    document_type VARCHAR(20),
    title TEXT,
    corresponding_author VARCHAR(200),
    journal VARCHAR(200),
    citation_count INT,
    impact_factor FLOAT,
    year INT,
    _source VARCHAR(10) DEFAULT 'csv',
    abstract TEXT,
    keywords TEXT,
    authors TEXT,
    publication_date VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    C_time FLOAT DEFAULT NULL,
    C_print FLOAT DEFAULT NULL,
    C_author FLOAT DEFAULT NULL,
    C_overall FLOAT DEFAULT NULL,
    w_time FLOAT DEFAULT NULL,
    w_print FLOAT DEFAULT NULL,
    w_author FLOAT DEFAULT NULL,
    wa1 FLOAT DEFAULT NULL,
    wa2 FLOAT DEFAULT NULL,
    wa3 FLOAT DEFAULT NULL,
    wa4 FLOAT DEFAULT NULL
);

-- 创建 author_acc 表
CREATE TABLE IF NOT EXISTS author_acc (
    author_name VARCHAR(200) PRIMARY KEY,
    acc FLOAT
);
