# -*- coding: utf-8 -*-
import urllib.request
import os
import sys

# 设置输出编码为UTF-8
if sys.platform == "win32":
    import codecs
    sys.stdout = codecs.getwriter("utf-8")(sys.stdout.buffer, "strict")
    sys.stderr = codecs.getwriter("utf-8")(sys.stderr.buffer, "strict")

def download_yolov8n():
    """下载YOLOv8n模型到指定目录"""
    url = "https://github.com/ultralytics/assets/releases/download/v0.0.0/yolov8n.pt"
    save_dir = os.path.join(os.path.dirname(__file__), "models")
    save_path = os.path.join(save_dir, "yolov8n.pt")

    # 创建目录
    os.makedirs(save_dir, exist_ok=True)

    print(f"正在下载 YOLOv8n 模型...")
    print(f"保存路径: {save_path}")

    try:
        urllib.request.urlretrieve(url, save_path)
        print(f"[OK] 下载成功!")
        print(f"文件大小: {os.path.getsize(save_path) / (1024*1024):.2f} MB")
    except Exception as e:
        print(f"[FAIL] 下载失败: {e}")

if __name__ == "__main__":
    download_yolov8n()
