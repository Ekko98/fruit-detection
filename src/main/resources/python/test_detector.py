# -*- coding: utf-8 -*-
import sys
import os
import base64
import subprocess
import json

# 设置输出编码为UTF-8
if sys.platform == "win32":
    import codecs
    sys.stdout = codecs.getwriter("utf-8")(sys.stdout.buffer, "strict")
    sys.stderr = codecs.getwriter("utf-8")(sys.stderr.buffer, "strict")

def test_detector():
    """测试YOLO检测脚本是否正常工作"""

    # 检查模型文件是否存在
    models_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'models')
    model_path = os.path.join(models_dir, 'last.pt')

    print("=" * 50)
    print("YOLO检测脚本测试")
    print("=" * 50)

    print(f"\n1. 检查模型文件...")
    if os.path.exists(model_path):
        print(f"   [OK] 模型文件存在: {model_path}")
        print(f"   [OK] 文件大小: {os.path.getsize(model_path) / (1024*1024):.2f} MB")
    else:
        print(f"   [FAIL] 模型文件不存在: {model_path}")
        return False

    print(f"\n2. 检查Python依赖...")
    try:
        import torch
        print(f"   [OK] PyTorch: {torch.__version__}")
    except ImportError:
        print(f"   [FAIL] PyTorch未安装")
        return False

    try:
        import cv2
        print(f"   [OK] OpenCV: {cv2.__version__}")
    except ImportError:
        print(f"   [FAIL] OpenCV未安装")
        return False

    try:
        from ultralytics import YOLO
        print(f"   [OK] Ultralytics YOLO已安装")
    except ImportError:
        print(f"   [FAIL] Ultralytics未安装")
        return False

    print(f"\n3. 测试模型加载...")
    try:
        from ultralytics import YOLO
        model = YOLO(model_path)
        print(f"   [OK] 模型加载成功")
        print(f"   [OK] 模型类别: {model.names}")
    except Exception as e:
        print(f"   [FAIL] 模型加载失败: {e}")
        return False

    print(f"\n4. 测试检测脚本...")
    detector_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'yolov8_detector.py')
    if not os.path.exists(detector_path):
        print(f"   [FAIL] 检测脚本不存在: {detector_path}")
        return False

    # 使用示例图片测试（创建一个简单的测试图片）
    try:
        from PIL import Image
        import io

        # 创建一个简单的测试图片（红色背景，白色圆形）
        test_img = Image.new('RGB', (640, 480), color='red')
        import numpy as np
        img_array = np.array(test_img)
        cv2.circle(img_array, (320, 240), 100, (255, 255, 255), -1)

        # 转换为base64
        buffered = io.BytesIO()
        Image.fromarray(img_array).save(buffered, format="JPEG")
        img_str = base64.b64encode(buffered.getvalue()).decode()

        print(f"   [OK] 测试图片生成成功 (640x480)")

        # 调用检测脚本
        print(f"\n5. 运行检测...")
        process = subprocess.Popen(
            [sys.executable, detector_path],
            stdin=subprocess.PIPE,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True
        )

        stdout, stderr = process.communicate(input=img_str, timeout=30)

        if process.returncode == 0:
            print(f"   [OK] 检测脚本执行成功")
            print(f"\n6. 检测结果:")

            # 从输出中提取JSON（通常在最后一行）
            lines = stdout.strip().split('\n')
            json_line = lines[-1] if lines else ''

            try:
                result = json.loads(json_line)
                print(f"   [OK] 结果解析成功:")
                print(f"     - 水果类型: {result.get('fruit_type', 'N/A')}")
                print(f"     - 新鲜度: {result.get('freshness', 'N/A')}")
                print(f"     - 置信度: {result.get('confidence', 0):.2f}")
                print(f"     - 坐标: x={result.get('x', 0):.2f}, y={result.get('y', 0):.2f}")
                print(f"     - 尺寸: w={result.get('width', 0):.2f}, h={result.get('height', 0):.2f}")
            except json.JSONDecodeError:
                print(f"   [FAIL] 结果解析失败")
                print(f"   输出: {json_line}")
                return False
        else:
            print(f"   [FAIL] 检测脚本执行失败 (退出码: {process.returncode})")
            if stderr:
                print(f"   错误信息: {stderr}")
            return False

    except subprocess.TimeoutExpired:
        print(f"   [FAIL] 检测超时")
        process.kill()
        return False
    except Exception as e:
        print(f"   [FAIL] 测试失败: {e}")
        return False

    print(f"\n" + "=" * 50)
    print("[OK] 所有测试通过！")
    print("=" * 50)
    return True

if __name__ == "__main__":
    success = test_detector()
    sys.exit(0 if success else 1)
