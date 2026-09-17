#!/usr/bin/env python3
"""采集第 ②③ 屏（铭刻时刻）Android 实现截图。

逻辑：
  1. wm size 628x1296 / density 320，immersive（与 screen-01 同一画布）。
  2. 安装 APK，重置数据，启动 MainActivity（首页）。
  3. 在沉浸式底栏上点击 "＋ 新时刻"，等待打开铭刻时刻表单（默认 FUTURE）。
  4. 截 future.png。
  5. 如果需要 past：点击 "过去·正数" 方向块，等待动画完成，截 past.png。

触摸坐标（628x1296 内容区）：
  - Home 底栏 "＋ 新时刻" 中心 ≈ (314, 1230)；form 内部的方向块需要根据实际布局动态调整。

用法：
  /Users/cch/.workbuddy/binaries/python/envs/default/bin/python3 _capture_android.py future
  /Users/cch/.workbuddy/binaries/python/envs/default/bin/python3 _capture_android.py past
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
TARGET_W, TARGET_H = 628, 1296


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


# 1) 调整显示尺寸（与 screen-01-home 一致）。
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

# 2) 装包 / 清数据 / 启动（方向由 debug fixture 决定，见步骤 4）。
sh("install", "-r", "-d", str(APK))
sh("shell", "pm", "clear", PKG)
sh("shell", "am", "start", "-n", f"{PKG}/{ACTIVITY}")
time.sleep(4)


def screencap(name: str, *, out_name: str | None = None) -> None:
    raw = HERE / f"_android_raw_{name}.png"
    p = subprocess.run(["adb", "exec-out", "screencap", "-p"], capture_output=True)
    raw.write_bytes(p.stdout)
    im = Image.open(raw).convert("RGB")
    print(f"原始帧 {name}:", im.size)
    if im.size != (TARGET_W, h):
        im = im.crop((0, 0, TARGET_W, h))
    out = HERE / f"{out_name or f'current-implementation-{name}'}.png"
    crop = im.crop((0, top, TARGET_W, top + TARGET_H))
    crop.save(out)
    print(f"saved {out} {crop.size}")


# 3) tap 在首页底部 + 新时刻。内容坐标 ≈ (314, 1234)，display 坐标需加状态栏偏移。
def tap(x_content: int, y_content: int) -> None:
    sh("shell", "input", "tap", str(x_content), str(y_content + top))
    time.sleep(1.0)


def tap_plus_nav() -> None:
    tap(TARGET_W // 2, TARGET_H - 62)
    time.sleep(0.8)


def cursor_pixels(img, box=(100, 545, 560, 640)) -> int:
    """统计 NAME 白框内「非白且非背景」的深色像素，用于判断光标是否处于亮相位。"""
    import numpy as _np
    a = _np.asarray(img.convert("RGB")).astype(int)
    x0, y0, x1, y1 = box
    sub = a[y0:y1, x0:x1]
    return int(((sub < 120).all(axis=2)).sum())


def capture_with_cursor(out_name: str) -> None:
    """点进 NAME 输入框使其聚焦，连拍三张覆盖 1s 闪烁的两个相位，取光标可见的那张。"""
    # 先关软键盘：模拟器有物理键盘映射，show_ime_with_hard_keyboard=0 时不弹出软键盘，
    # 焦点仍保留在输入框，能截到与 design.html 等价的光标而不被键盘遮挡下半屏。
    sh("shell", "settings", "put", "secure", "show_ime_with_hard_keyboard", "0")
    tap(200, 590)                      # NAME 白框中部（内容坐标）
    time.sleep(0.4)
    # tap 会把光标落到点击处，先按 End 移回文本末尾（与 design.html 的「文字 + 末尾光标」一致）；
    # 再敲一个字符并删掉 —— 文本变化会让 Compose 收起点击触发的系统插入手柄，
    # 否则截图里会多出一个 design.html 没有的黑色水滴。
    sh("shell", "input", "keyevent", "123")   # KEYCODE_MOVE_END
    time.sleep(0.3)
    sh("shell", "input", "text", "a")
    time.sleep(0.3)
    sh("shell", "input", "keyevent", "67")    # KEYCODE_DEL
    time.sleep(0.4)
    cands = []
    for i in range(3):
        raw = HERE / f"_android_raw_cursor{i}.png"
        p = subprocess.run(["adb", "exec-out", "screencap", "-p"], capture_output=True)
        raw.write_bytes(p.stdout)
        im = Image.open(raw).convert("RGB")
        if im.size != (TARGET_W, h):
            im = im.crop((0, 0, TARGET_W, h))
        crop = im.crop((0, top, TARGET_W, top + TARGET_H))
        cands.append((cursor_pixels(crop), crop))
        time.sleep(0.35)
    best = max(cands, key=lambda c: c[0])
    out = HERE / f"{out_name}.png"
    best[1].save(out)
    print(f"saved {out} {best[1].size} 光标像素={best[0]}（候选 {[c[0] for c in cands]}）")


def reset_debug_props() -> None:
    """复位 debug fixture 属性（否则 setprop 会留在设备上，正常打开 App 时会带出截图用数据）。

    值必须写成带引号的 `""`：adb 会把参数拼成设备侧 shell 命令，
    直接传空串会变成 `setprop NAME`（少一个参数）而被 setprop 拒绝。
    """
    sh("shell", "setprop", "debug.momentmark.form", '""')
    sh("shell", "am", "force-stop", PKG)


seed = sys.argv[1] if len(sys.argv) > 1 else "future"
out_name = sys.argv[2] if len(sys.argv) > 2 else f"current-implementation-{seed.replace('.', '-')}"
with_cursor = "--with-cursor" in sys.argv
scroll_bottom = "--scroll-bottom" in sys.argv

# 4) 用 debug fixture 直接进目标方向，避免依赖坐标点击切换。
#    取值：future / past（空表单，生产等价）或 future.sample / past.sample（带 design.html 示例文本）。
sh("shell", "pm", "clear", PKG)
sh("shell", "setprop", "debug.momentmark.form", seed)
sh("shell", "am", "start", "-n", f"{PKG}/{ACTIVITY}")
time.sleep(4)
tap_plus_nav()
time.sleep(1.0)
try:
    if scroll_bottom:
        # 上滑到底：表单内容高于可视区，提交按钮需要滚动才可见（design.html 同样如此）。
        for _ in range(4):
            sh("shell", "input", "swipe", "314", "1000", "314", "500", "180")
            time.sleep(0.5)
        time.sleep(0.6)
    if with_cursor:
        capture_with_cursor(out_name)
    else:
        screencap("tmp", out_name=out_name)
finally:
    reset_debug_props()

print("done")
