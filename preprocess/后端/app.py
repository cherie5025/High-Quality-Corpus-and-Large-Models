import os
os.environ['FLAGS_use_mkldnn'] = '0'
os.environ['FLAGS_use_mkldnn_quantizer'] = '0'
os.environ['PADDLE_PDX_DISABLE_MODEL_SOURCE_CHECK'] = 'True'

import uuid
from pathlib import Path
from fastapi import FastAPI, File, UploadFile, HTTPException
from fastapi.responses import JSONResponse
from fastapi.middleware.cors import CORSMiddleware
import uvicorn
from paddleocr import PaddleOCR
import pdf2image

# 创建临时文件目录
UPLOAD_DIR = Path("./uploads")
UPLOAD_DIR.mkdir(exist_ok=True)

# 初始化 PaddleOCR
print("正在初始化 PaddleOCR...")
try:
    ocr = PaddleOCR(
        use_angle_cls=False,
        lang='ch',
        use_gpu=False,
        enable_mkldnn=False,
        cpu_threads=4
    )
    print("PaddleOCR 初始化成功")
except Exception as e:
    print(f"PaddleOCR 初始化失败: {e}")
    ocr = PaddleOCR(use_angle_cls=False, lang='ch', use_gpu=False, enable_mkldnn=False)

# 创建 FastAPI 应用
app = FastAPI(title="PaddleOCR PDF Service")

# 配置 CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


def pdf_to_images(pdf_path: str):
    """将 PDF 转换为图片列表"""
    try:
        images = pdf2image.convert_from_path(
            pdf_path,
            dpi=200,
            poppler_path=r'E:\High-Quality-Corpus-and-Large-Models\Release-25.12.0-0\poppler-25.12.0\Library\bin'
        )
        return images
    except Exception as e:
        print(f"PDF 转换失败: {e}")
        return []


def ocr_image(image):
    """对单张图片进行 OCR 识别"""
    try:
        temp_img_path = UPLOAD_DIR / f"{uuid.uuid4()}.png"
        image.save(temp_img_path)

        result = ocr.ocr(str(temp_img_path), cls=False)

        texts = []
        if result and len(result) > 0:
            for line in result[0]:
                if line and len(line) >= 2:
                    texts.append(line[1][0])

        os.remove(temp_img_path)
        return "\n".join(texts)
    except Exception as e:
        print(f"OCR 识别失败: {e}")
        return ""


@app.get("/health")
async def health_check():
    """健康检查接口"""
    return {"status": "ok", "service": "PaddleOCR"}


@app.post("/api/ocr/pdf")
async def ocr_pdf(file: UploadFile = File(...)):
    """PDF OCR 识别接口"""
    if not file.filename.endswith(('.pdf', '.PDF')):
        raise HTTPException(400, "仅支持 PDF 文件")

    pdf_id = uuid.uuid4().hex
    pdf_path = UPLOAD_DIR / f"{pdf_id}.pdf"

    try:
        with open(pdf_path, "wb") as f:
            content = await file.read()
            f.write(content)

        images = pdf_to_images(str(pdf_path))

        if not images:
            raise HTTPException(500, "PDF 转换失败")

        all_text = []
        for i, image in enumerate(images):
            print(f"正在识别第 {i + 1}/{len(images)} 页...")
            page_text = ocr_image(image)
            if page_text:
                all_text.append(f"--- 第 {i + 1} 页 ---\n{page_text}")

        result_text = "\n\n".join(all_text)

        return JSONResponse({
            "success": True,
            "filename": file.filename,
            "total_pages": len(images),
            "text": result_text
        })

    except Exception as e:
        print(f"处理失败: {e}")
        return JSONResponse(
            status_code=500,
            content={"success": False, "error": str(e)}
        )
    finally:
        if pdf_path.exists():
            os.remove(pdf_path)


if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8082)