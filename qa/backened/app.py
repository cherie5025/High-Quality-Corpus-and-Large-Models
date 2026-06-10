"""
app.py - Flask 后端主程序
高质量问答对构建系统后端
"""

from flask import Flask, request, jsonify, send_file
from flask_cors import CORS
import json
import os
import threading
import time
import uuid
from datetime import datetime
import io
import csv
import pymysql
from dbutils.pooled_db import PooledDB

app = Flask(__name__)
CORS(app)

# ==================== 数据库连接池配置 ====================
db_pool = PooledDB(
    creator=pymysql,
    maxconnections=10,
    mincached=2,
    maxcached=5,
    host='localhost',
    port=3306,
    user='root',
    password='123456',
    database='literature_db',
    charset='utf8mb4'
)

def get_db_connection():
    return db_pool.connection()

# ==================== 数据存储（内存 + 文件） ====================
DATA_DIR = "./data"
os.makedirs(DATA_DIR, exist_ok=True)

tasks = {}

def get_data_path(filename):
    return os.path.join(DATA_DIR, filename)

def load_json(path):
    if os.path.exists(path):
        with open(path, 'r', encoding='utf-8') as f:
            return json.load(f)
    return []

def save_json(path, data):
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False, indent=2)

def get_qa_data():
    return load_json(get_data_path("qa_generated.json"))

def get_eval_data():
    return load_json(get_data_path("qa_evaluated.json"))

def save_qa_data(data):
    save_json(get_data_path("qa_generated.json"), data)

def save_eval_data(data):
    save_json(get_data_path("qa_evaluated.json"), data)


# ==================== 问答对生成 API ====================

@app.route('/api/qa/generate/start', methods=['POST'])
def start_generation():
    body = request.json or {}
    paper_ids = body.get("paper_ids", [])
    model = body.get("model", "Qwen/Qwen3-VL-32B-Thinking")
    window_size = body.get("window_size", 16)
    overlap = body.get("overlap", 3)
    max_count = body.get("max_count", 3000)

    if not paper_ids:
        return jsonify({"error": "请选择文献"}), 400

    try:
        conn = get_db_connection()
        cursor = conn.cursor()
        placeholders = ','.join(['%s'] * len(paper_ids))
        sql = "SELECT id, title, full_text FROM literature WHERE id IN (" + placeholders + ")"
        cursor.execute(sql, paper_ids)
        rows = cursor.fetchall()
        cursor.close()
        conn.close()

        if not rows:
            return jsonify({"error": "未找到文献"}), 400

        all_text = []
        for row in rows:
            if row[2]:
                all_text.append(row[2])

        if not all_text:
            return jsonify({"error": "文献内容为空"}), 400

        content = '\n'.join(all_text)

    except Exception as e:
        print("数据库查询错误:", e)
        return jsonify({"error": "数据库查询失败: " + str(e)}), 500

    task_id = str(uuid.uuid4())
    tasks[task_id] = {
        "type": "generation",
        "status": "running",
        "total": 0,
        "done": 0,
        "failed": 0,
        "results": [],
        "start_time": time.time(),
        "model": model
    }

    thread = threading.Thread(
        target=_run_generation_from_content,
        args=(task_id, content, model, window_size, overlap, max_count, paper_ids)
    )
    thread.daemon = True
    thread.start()

    return jsonify({"task_id": task_id, "message": "生成任务已启动"})


def _run_generation_from_content(task_id, content, model, window_size, overlap, max_count, paper_ids):
    import sys
    sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

    try:
        from qa_core.generation import filter_texts, sliding_window_merge, dataProcess, getQA
        import json as _json

        task = tasks[task_id]
        data = content.split("\n")

        knowledge_list = dataProcess(
            sliding_window_merge(filter_texts(data), window_size, overlap),
            model=model
        )
        task["total"] = len(knowledge_list)

        results = []
        for knowledge in knowledge_list:
            if task.get("cancelled"):
                break

            try:
                qa_str = getQA(knowledge, model=model)
                if qa_str is None:
                    task["failed"] += 1
                    task["done"] += 1
                    continue

                qa_data = _json.loads(qa_str)

                if isinstance(qa_data, list):
                    for item in qa_data:
                        item["sentences"] = knowledge["sentences"]
                        item["paper_ids"] = paper_ids
                    results.extend(qa_data)
                elif isinstance(qa_data, dict):
                    qa_data["sentences"] = knowledge["sentences"]
                    qa_data["paper_ids"] = paper_ids
                    results.append(qa_data)

            except Exception as e:
                task["failed"] += 1
                print("生成失败:", e)

            task["done"] += 1
            task["results"] = [r for r in results if 'question' in r and 'answer' in r]

            if len(results) >= max_count:
                task["done"] = task["total"]
                task["limit_triggered"] = True
                break

        final_results = [r for r in results if 'question' in r and 'answer' in r]
        save_qa_data(final_results)
        task["status"] = "done"
        task["results"] = final_results

    except Exception as e:
        tasks[task_id]["status"] = "error"
        tasks[task_id]["error"] = str(e)
        print("生成任务错误:", e)


@app.route('/api/qa/generate/status/<task_id>', methods=['GET'])
def generation_status(task_id):
    task = tasks.get(task_id)
    if not task:
        return jsonify({"error": "任务不存在"}), 404

    elapsed = time.time() - task["start_time"]
    speed = task["done"] / elapsed if elapsed > 0 and task["done"] > 0 else 0
    remaining = (task["total"] - task["done"]) / speed if speed > 0 else 0

    return jsonify({
        "status": task["status"],
        "total": task["total"],
        "done": task["done"],
        "failed": task["failed"],
        "generated_count": len(task.get("results", [])),
        "elapsed_seconds": int(elapsed),
        "estimated_remaining_seconds": int(remaining),
        "limit_triggered": task.get("limit_triggered", False),
        "error": task.get("error")
    })


@app.route('/api/qa/generate/cancel/<task_id>', methods=['POST'])
def cancel_generation(task_id):
    task = tasks.get(task_id)
    if task:
        task["cancelled"] = True
    return jsonify({"message": "已请求取消"})


@app.route('/api/qa/generate/upload', methods=['POST'])
def upload_file():
    if 'file' not in request.files:
        return jsonify({"error": "未找到文件"}), 400

    file = request.files['file']
    if not file.filename.endswith('.txt'):
        return jsonify({"error": "仅支持 .txt 文件"}), 400

    upload_dir = os.path.join(DATA_DIR, "uploads")
    os.makedirs(upload_dir, exist_ok=True)
    save_path = os.path.join(upload_dir, file.filename)
    file.save(save_path)

    with open(save_path, 'r', encoding='utf-8') as f:
        lines = f.read().split("\n")

    return jsonify({
        "file_path": save_path,
        "filename": file.filename,
        "line_count": len([l for l in lines if l.strip()])
    })


@app.route('/api/qa/generate/download', methods=['GET'])
def download_generated():
    data = get_qa_data()
    buf = io.BytesIO(json.dumps(data, ensure_ascii=False, indent=2).encode('utf-8'))
    buf.seek(0)
    return send_file(buf, mimetype='application/json',
                     download_name='qa_generated.json', as_attachment=True)


# ==================== 质量评估 API ====================

@app.route('/api/qa/evaluate/start', methods=['POST'])
def start_evaluation():
    body = request.json or {}
    model = body.get("model", "deepseek-ai/DeepSeek-R1")

    qa_data = get_qa_data()
    if not qa_data:
        return jsonify({"error": "没有可评估的问答对，请先完成生成步骤"}), 400

    task_id = str(uuid.uuid4())
    tasks[task_id] = {
        "type": "evaluation",
        "status": "running",
        "total": len(qa_data),
        "done": 0,
        "failed": 0,
        "results": [],
        "start_time": time.time(),
        "model": model
    }

    thread = threading.Thread(target=_run_evaluation, args=(task_id, qa_data, model))
    thread.daemon = True
    thread.start()

    return jsonify({"task_id": task_id, "message": "评估任务已启动", "total": len(qa_data)})


def _run_evaluation(task_id, qa_data, model):
    try:
        from qa_core.evaluation import dataProcess, getScore

        task = tasks[task_id]
        data_pro = dataProcess(qa_data, model=model)
        results = []

        for item in data_pro:
            if task.get("cancelled"):
                break
            try:
                score = getScore(item, model=model)
                item["score"] = score['Score']
                item['Judgment'] = score['Judgment']
                del item["message"]
                results.append(item)
            except Exception as e:
                task["failed"] += 1

            task["done"] += 1
            task["results"] = results

        save_eval_data(results)
        task["status"] = "done"
        task["results"] = results

    except Exception as e:
        tasks[task_id]["status"] = "error"
        tasks[task_id]["error"] = str(e)


@app.route('/api/qa/evaluate/status/<task_id>', methods=['GET'])
def evaluation_status(task_id):
    task = tasks.get(task_id)
    if not task:
        return jsonify({"error": "任务不存在"}), 404

    elapsed = time.time() - task["start_time"]
    speed = task["done"] / elapsed if elapsed > 0 and task["done"] > 0 else 0
    remaining = (task["total"] - task["done"]) / speed if speed > 0 else 0

    return jsonify({
        "status": task["status"],
        "total": task["total"],
        "done": task["done"],
        "failed": task["failed"],
        "elapsed_seconds": int(elapsed),
        "estimated_remaining_seconds": int(remaining),
        "error": task.get("error")
    })


@app.route('/api/qa/evaluate/data', methods=['GET'])
def get_evaluation_data():
    data = get_eval_data()

    judgment = request.args.get('judgment', '')
    score_range = request.args.get('score_range', '')
    page = int(request.args.get('page', 1))
    page_size = int(request.args.get('page_size', 10))

    def get_avg(score_obj):
        vals = list(score_obj.values())
        return sum(vals) / len(vals) if vals else 0

    filtered = data
    if judgment:
        filtered = [d for d in filtered if d.get('Judgment') == judgment]
    if score_range == 'excellent':
        filtered = [d for d in filtered if get_avg(d.get('score', {})) >= 9]
    elif score_range == 'good':
        filtered = [d for d in filtered if 7 <= get_avg(d.get('score', {})) < 9]
    elif score_range == 'acceptable':
        filtered = [d for d in filtered if 4 <= get_avg(d.get('score', {})) < 7]
    elif score_range == 'poor':
        filtered = [d for d in filtered if get_avg(d.get('score', {})) < 4]

    total = len(filtered)
    start = (page - 1) * page_size
    page_data = filtered[start: start + page_size]

    return jsonify({
        "total": total,
        "page": page,
        "page_size": page_size,
        "data": page_data
    })


@app.route('/api/qa/evaluate/delete', methods=['POST'])
def delete_items():
    body = request.json or {}
    questions_to_delete = set(body.get("questions", []))

    data = get_eval_data()
    remaining = [d for d in data if d['question'] not in questions_to_delete]
    deleted_items = [d for d in data if d['question'] in questions_to_delete]

    save_eval_data(remaining)

    deleted_path = get_data_path("qa_deleted.json")
    existing_deleted = load_json(deleted_path)
    existing_deleted.extend(deleted_items)
    save_json(deleted_path, existing_deleted)

    return jsonify({"deleted": len(deleted_items), "remaining": len(remaining)})


@app.route('/api/qa/evaluate/undo', methods=['POST'])
def undo_delete():
    body = request.json or {}
    questions_to_restore = set(body.get("questions", []))

    deleted_path = get_data_path("qa_deleted.json")
    deleted = load_json(deleted_path)

    restore_items = [d for d in deleted if d['question'] in questions_to_restore]
    remaining_deleted = [d for d in deleted if d['question'] not in questions_to_restore]

    save_json(deleted_path, remaining_deleted)

    current = get_eval_data()
    current.extend(restore_items)
    save_eval_data(current)

    return jsonify({"restored": len(restore_items)})


@app.route('/api/qa/evaluate/upload', methods=['POST'])
def upload_eval_json():
    if 'file' not in request.files:
        return jsonify({"error": "未找到文件"}), 400

    file = request.files['file']
    try:
        data = json.load(file)
        save_eval_data(data)
        return jsonify({"message": "成功加载 " + str(len(data)) + " 条评估数据", "count": len(data)})
    except Exception as e:
        return jsonify({"error": "JSON格式错误: " + str(e)}), 400


@app.route('/api/qa/evaluate/export', methods=['GET'])
def export_evaluation():
    data = get_eval_data()
    buf = io.BytesIO(json.dumps(data, ensure_ascii=False, indent=2).encode('utf-8'))
    buf.seek(0)
    return send_file(buf, mimetype='application/json',
                     download_name='qa_evaluation_result.json', as_attachment=True)


# ==================== 语料集管理 API ====================

@app.route('/api/qa/corpus/statistics', methods=['GET'])
def corpus_statistics():
    data = get_eval_data()

    def get_avg(score_obj):
        vals = list(score_obj.values())
        return sum(vals) / len(vals) if vals else 0

    total = len(data)
    score_distribution = [0] * 11
    scores = []

    for d in data:
        if d.get('score'):
            avg_score = get_avg(d['score'])
            scores.append(avg_score)
            bucket_index = min(int(avg_score), 10)
            score_distribution[bucket_index] += 1

    score_excellent = len([s for s in scores if s >= 9])
    score_good = len([s for s in scores if 7 <= s < 9])
    score_acceptable = len([s for s in scores if 4 <= s < 7])
    score_poor = len([s for s in scores if s < 4])
    overall_score = round(sum(scores) / len(scores), 2) if scores else 0

    judgment_dist = {}
    for d in data:
        j = d.get('Judgment', 'Unknown')
        judgment_dist[j] = judgment_dist.get(j, 0) + 1

    dimension_avg = {}
    dims = ['Consistency', 'Integrity', 'Accuracy', 'Independence', 'Readability', 'Relevance', 'Simplicity']
    for dim in dims:
        vals = [d['score'][dim] for d in data if d.get('score') and dim in d['score']]
        dimension_avg[dim] = round(sum(vals) / len(vals), 2) if vals else 0

    return jsonify({
        "total": total,
        "score_excellent": score_excellent,
        "score_good": score_good,
        "score_acceptable": score_acceptable,
        "score_poor": score_poor,
        "overall_score": overall_score,
        "judgment_distribution": judgment_dist,
        "dimension_averages": dimension_avg,
        "score_distribution": score_distribution
    })


@app.route('/api/qa/corpus/export', methods=['POST'])
def export_corpus():
    body = request.json or {}
    fmt = body.get("format", "json")
    filter_quality = body.get("filter_quality", "")
    include_score = body.get("include_score", False)
    include_judgment = body.get("include_judgment", False)
    include_sentences = body.get("include_sentences", False)

    data = get_eval_data()

    def get_avg(score_obj):
        vals = list(score_obj.values())
        return sum(vals) / len(vals) if vals else 0

    if filter_quality == 'excellent':
        data = [d for d in data if get_avg(d.get('score', {})) >= 9]
    elif filter_quality == 'good':
        data = [d for d in data if 7 <= get_avg(d.get('score', {})) < 9]
    elif filter_quality == 'acceptable':
        data = [d for d in data if 4 <= get_avg(d.get('score', {})) < 7]
    elif filter_quality == 'poor':
        data = [d for d in data if get_avg(d.get('score', {})) < 4]

    export_data = []
    for d in data:
        item = {"question": d.get("question", ""), "answer": d.get("answer", "")}
        if include_sentences:
            item["sentences"] = d.get("sentences", "")
        if include_score:
            item["score"] = d.get("score", {})
            item["avg_score"] = round(get_avg(d.get('score', {})), 2)
        if include_judgment:
            item["Judgment"] = d.get("Judgment", "")
        export_data.append(item)

    if fmt == "json":
        buf = io.BytesIO(json.dumps(export_data, ensure_ascii=False, indent=2).encode('utf-8'))
        buf.seek(0)
        return send_file(buf, mimetype='application/json',
                         download_name='corpus.json', as_attachment=True)

    elif fmt == "csv":
        output = io.StringIO()
        if export_data:
            flat_data = []
            for item in export_data:
                flat = {"question": item["question"], "answer": item["answer"]}
                if include_sentences:
                    flat["sentences"] = item.get("sentences", "")
                if include_score:
                    for k, v in item.get("score", {}).items():
                        flat[k] = v
                    flat["avg_score"] = item.get("avg_score", "")
                if include_judgment:
                    flat["Judgment"] = item.get("Judgment", "")
                flat_data.append(flat)

            writer = csv.DictWriter(output, fieldnames=flat_data[0].keys())
            writer.writeheader()
            writer.writerows(flat_data)

        buf = io.BytesIO(output.getvalue().encode('utf-8-sig'))
        buf.seek(0)
        return send_file(buf, mimetype='text/csv',
                         download_name='corpus.csv', as_attachment=True)

    elif fmt == "txt":
        lines = []
        for item in export_data:
            lines.append("Q: " + item['question'])
            lines.append("A: " + item['answer'])
            lines.append("")
        buf = io.BytesIO("\n".join(lines).encode('utf-8'))
        buf.seek(0)
        return send_file(buf, mimetype='text/plain',
                         download_name='corpus.txt', as_attachment=True)

    return jsonify({"error": "不支持的格式"}), 400


@app.route('/api/qa/corpus/export_count', methods=['POST'])
def export_count():
    body = request.json or {}
    filter_quality = body.get("filter_quality", "")
    data = get_eval_data()

    def get_avg(score_obj):
        vals = list(score_obj.values())
        return sum(vals) / len(vals) if vals else 0

    if filter_quality == 'excellent':
        count = len([d for d in data if get_avg(d.get('score', {})) >= 9])
    elif filter_quality == 'good':
        count = len([d for d in data if 7 <= get_avg(d.get('score', {})) < 9])
    elif filter_quality == 'acceptable':
        count = len([d for d in data if 4 <= get_avg(d.get('score', {})) < 7])
    elif filter_quality == 'poor':
        count = len([d for d in data if get_avg(d.get('score', {})) < 4])
    else:
        count = len(data)

    return jsonify({"count": count})


# ==================== 健康检查 ====================

@app.route('/api/health', methods=['GET'])
def health():
    return jsonify({"status": "ok", "time": datetime.now().isoformat()})


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)