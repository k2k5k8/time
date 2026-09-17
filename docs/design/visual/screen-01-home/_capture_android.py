#!/usr/bin/env python3
"""采集第 ① 屏 Android 实现截图，并裁剪到与 design-original.png 完全相同的画布。

对齐条件（与 design.html `.phone` 内部内容区一一对应）：
  App 内容区 = 314 x 648 dp  →  display = 628 x (1296 + statusBar + navBar) px @ density 320
  裁剪掉系统状态栏（含挖孔）与手势条，只留 App 自身内容
产物：current-android.png（整屏原始帧）、current-implementation.png（628x1296）
运行：/opt/miniconda3/bin/python3 _capture_android.py
"""
from __future__ import annotations
import re
import subprocess
import sys
import time
from pathlib import Path

from PIL import Image

HERE = Path(__file__).resolve().parent
REPO = HERE.parents[3]
PKG = "com.cch.momentmark.app"
ACTIVITY = "com.cch.momentmark.MainActivity"
APK = REPO / "app/build/outputs/apk/debug/app-debug.apk"
TARGET_W, TARGET_H = 628, 1296   # = 314 x 648 dp @2x


def sh(*args: str, check: bool = True) -> str:
    r = subprocess.run(["adb", *args], capture_output=True, text=True)
    if check and r.returncode != 0:
        print("adb failed:", args, r.stderr[:400], file=sys.stderr)
    return r.stdout


def system_bar_insets() -> tuple[int, int]:
    out = sh("shell", "dumpsys", "window", "displays")
    top = bottom = 0
    for m in re.finditer(r"type=statusBars frame=\[(-?\d+),(-?\d+)\]\[(-?\d+),(-?\d+)\]", out):
        top = max(top, int(m.group(4)) - int(m.group(2)))
    for m in re.finditer(r"type=navigationBars frame=\[(-?\d+),(-?\d+)\]\[(-?\d+),(-?\d+)\]", out):
        bottom = max(bottom, int(m.group(4)) - int(m.group(2)))
    return top, bottom


# 1. 设置显示尺寸，使 App 内容区恰好 1296px 高
h = TARGET_H
for attempt in range(4):
    sh("shell", "wm", "density", "320")
    sh("shell", "wm", "size", f"{TARGET_W}x{h}")
    time.sleep(0.6)
    top, bottom = system_bar_insets()
    content = h - top - bottom
    print(f"尝试 {attempt + 1}: display={TARGET_W}x{h} 状态栏={top} 手势条={bottom} 内容={content}")
    if content == TARGET_H:
        break
    h += TARGET_H - content
else:
    raise SystemExit("无法把 App 内容区调到 1296px")

# 2. 安装当前构建并重置数据（debug 冻结时钟 + SampleMoments 种子 => 确定性 fixture）
sh("install", "-r", "-d", str(APK))
sh("shell", "pm", "clear", PKG)
sh("shell", "am", "start", "-n", f"{PKG}/{ACTIVITY}")
time.sleep(4)

# 3. 截图并裁剪
raw = HERE / "current-android.png"
p = subprocess.run(["adb", "exec-out", "screencap", "-p"], capture_output=True)
raw.write_bytes(p.stdout)
im = Image.open(raw).convert("RGB")
print("原始帧:", im.size)
assert im.size == (TARGET_W, h), f"帧尺寸 {im.size} 与预期 {(TARGET_W, h)} 不符"
crop = im.crop((0, top, TARGET_W, top + TARGET_H))
crop.save(HERE / "current-implementation.png")
print(f"已裁剪系统栏（上 {top}px / 下 {bottom}px）→ current-implementation.png {crop.size}")
