import base64
import io
import json
import os
import sys

import cv2
import numpy as np
from PIL import Image
from ultralytics import YOLO


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


def extract_base64_payload(image_data):
    if image_data is None:
        raise ValueError('image data is empty')

    payload = image_data.strip()
    if not payload:
        raise ValueError('image data is blank')

    if ',' in payload and payload.lower().startswith('data:'):
        payload = payload.split(',', 1)[1]

    return payload


def load_image_from_base64(image_data):
    payload = extract_base64_payload(image_data)
    image_bytes = base64.b64decode(payload)
    image = Image.open(io.BytesIO(image_bytes))
    image = image.convert('RGB')
    return cv2.cvtColor(np.array(image), cv2.COLOR_RGB2BGR)


def detect_fruit(image_data):
    try:
        image_cv = load_image_from_base64(image_data)

        script_dir = os.path.dirname(os.path.abspath(__file__))
        model_path = os.path.join(script_dir, 'models', 'last.pt')
        model = YOLO(model_path)

        results = model(image_cv, conf=0.5)
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
        print(f'Error in YOLO detection: {str(e)}', file=sys.stderr)
        return build_response([])


if __name__ == '__main__':
    image_data = sys.stdin.read()
    result = detect_fruit(image_data)
    print(json.dumps(result))
