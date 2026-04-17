#!/usr/bin/env python3
"""
水果检测常驻服务 - 使用Flask提供HTTP接口
模型预加载，避免每次请求重新加载模型
"""

import base64
import io
import json
import os
import sys
import time
from threading import Lock

import cv2
import numpy as np
from flask import Flask, request, jsonify
from PIL import Image
from ultralytics import YOLO

app = Flask(__name__)

# 全局模型实例（启动时加载一次）
model = None
model_lock = Lock()  # 防止并发检测冲突


def load_model():
    """启动时加载模型"""
    global model
    script_dir = os.path.dirname(os.path.abspath(__file__))
    model_path = os.path.join(script_dir, 'models', 'last.pt')

    print(f"Loading YOLO model from: {model_path}")
    start_time = time.time()
    model = YOLO(model_path)
    load_time = time.time() - start_time
    print(f"Model loaded in {load_time:.2f} seconds")
    return model


def extract_base64_payload(image_data):
    """提取base64数据"""
    if image_data is None:
        raise ValueError('image data is empty')

    payload = image_data.strip()
    if not payload:
        raise ValueError('image data is blank')

    if ',' in payload and payload.lower().startswith('data:'):
        payload = payload.split(',', 1)[1]

    return payload


def load_image_from_base64(image_data):
    """从base64加载图像"""
    payload = extract_base64_payload(image_data)
    image_bytes = base64.b64decode(payload)
    image = Image.open(io.BytesIO(image_bytes))
    image = image.convert('RGB')
    return cv2.cvtColor(np.array(image), cv2.COLOR_RGB2BGR)


def build_response(detections):
    """构建响应 - 无水果时返回空detections"""
    response = {
        'fruit_type': None,
        'freshness': None,
        'confidence': 0,
        'x': 0,
        'y': 0,
        'width': 0,
        'height': 0,
        'detections': detections if detections else []
    }
    return response


def detect_fruit_internal(image_data):
    """内部检测逻辑（已加载模型）"""
    try:
        image_cv = load_image_from_base64(image_data)

        # 使用锁防止并发冲突
        with model_lock:
            results = model(image_cv, conf=0.5, verbose=False)

        detections = []
        for result in results:
            boxes = result.boxes
            for box in boxes:
                x1, y1, x2, y2 = box.xyxy[0].tolist()
                conf = box.conf[0].item()
                cls = box.cls[0].item()

                h, w = image_cv.shape[:2]
                class_name = result.names[int(cls)]
                freshness = 'fresh' if class_name.lower().startswith('good') else 'rotten'

                detections.append({
                    'fruit_type': class_name,
                    'freshness': freshness,
                    'confidence': conf,
                    'x': x1 / w,
                    'y': y1 / h,
                    'width': (x2 - x1) / w,
                    'height': (y2 - y1) / h,
                })

        detections.sort(key=lambda item: item['confidence'], reverse=True)
        return build_response(detections)
    except Exception as e:
        print(f'Error in detection: {str(e)}', file=sys.stderr)
        return build_response([])


@app.route('/detect', methods=['POST'])
def detect():
    """检测接口"""
    try:
        data = request.get_json()
        if not data:
            return jsonify({'error': 'No JSON data provided'}), 400

        image_data = data.get('imageUrl') or data.get('imageData')
        if not image_data:
            return jsonify({'error': 'No image data provided'}), 400

        result = detect_fruit_internal(image_data)
        return jsonify(result)
    except Exception as e:
        return jsonify({'error': str(e)}), 500


@app.route('/health', methods=['GET'])
def health():
    """健康检查接口"""
    return jsonify({
        'status': 'ok',
        'model_loaded': model is not None
    })


@app.route('/shutdown', methods=['POST'])
def shutdown():
    """关闭服务"""
    func = request.environ.get('werkzeug.server.shutdown')
    if func:
        func()
    return jsonify({'status': 'shutting down'})


if __name__ == '__main__':
    # 启动时预加载模型
    load_model()

    print("\n" + "="*50)
    print("Fruit Detection Service Started")
    print("Model pre-loaded, ready for requests")
    print("="*50 + "\n")

    # 启动Flask服务
    # 使用5005端口避免与Java后端冲突
    app.run(host='127.0.0.1', port=5005, threaded=True)