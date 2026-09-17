#!/usr/bin/env python3
"""采集第 ⑥ 屏（任务详情 / QUEST DETAIL · 未来 · 昼）Android 实现截图。

逻辑：
  1. wm size 628x1296 / density 320（内容区 314x648 dp @2x，与 design.png 同画布）。
  2. 安装 APK，清数据。
  3. `setprop debug.momentmark.detail quest` 启动即注入第 ⑥ 屏冻结 QUEST 帧。

用法：
    <venv>/bin/python3 _capture_android.py
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
ADB = str(Path.home() / "Library/Android/sdk/platform-tools/adb")


def sh(*args: str, check: bool = True) -> str:
    r = subprocess.run([ADB, *args], capture_output=True, text=True)
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

sh("install", "-r", "-d", str(APK))


def screencap(name: str, *, out_name: str | None = None) -> None:
    raw = HERE / f"_android_raw_{name}.png"
    p = subprocess.run([ADB, "exec-out", "screencap", "-p"], capture_output=True)
    raw.write_bytes(p.stdout)
    im = Image.open(raw).convert("RGB")
    print(f"原始帧 {name}:", im.size)
    if im.size != (TARGET_W, h):
        im = im.crop((0, 0, TARGET_W, h))
    out = HERE / f"{out_name or f'current-implementation-{name}'}.png"
    crop = im.crop((0, top, TARGET_W, top + TARGET_H))
    crop.save(out)
    print(f"saved {out} {crop.size}")


def reset_debug_props() -> None:
    for prop in ("debug.momentmark.nav", "debug.momentmark.daybook", "debug.momentmark.detail"):
        sh("shell", "setprop", prop, '""')
    sh("shell", "am", "force-stop", PKG)


sh("shell", "pm", "clear", PKG)
sh("shell", "setprop", "debug.momentmark.detail", "quest")
sh("shell", "am", "start", "-n", f"{PKG}/{ACTIVITY}")
time.sleep(5)

try:
    screencap("tmp", out_name="current-implementation")
finally:
    reset_debug_props()

print("done")
