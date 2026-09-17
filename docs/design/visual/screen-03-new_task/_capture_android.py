#!/usr/bin/env python3
"""采集第 ④ 屏（接受新任务）Android 实现截图。

逻辑：
  1. wm size 628x1296 / density 320（内容区 314x648 dp @2x，与 design.png 同画布）。
  2. 安装 APK，清数据。
  3. `setprop debug.momentmark.nav task` 直接打开第 ④ 屏表单（不依赖坐标点击）。
  4. `setprop debug.momentmark.taskform sample|empty` 决定表单是 design 示例文本还是空表单。
  5. 截 current-implementation-<mode>.png。

用法：
  /Users/cch/.workbuddy/binaries/python/envs/default/bin/python3 _capture_android.py sample
  /Users/cch/.workbuddy/binaries/python/envs/default/bin/python3 _capture_android.py empty
  # 附加开关：
  #   --with-cursor   点进 NAME 输入框连拍 3 张取光标可见帧（与 design.html 的 ▮ 对应）
  #   --scroll-bottom 上滑到底，检查提交按钮（表单高于可视区时）
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


# 1) 调整显示尺寸（与 screen-01/02 同一画布）。
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


def tap(x_content: int, y_content: int) -> None:
    sh("shell", "input", "tap", str(x_content), str(y_content + top))
    time.sleep(1.0)


def cursor_pixels(img, box=(60, 240, 560, 330)) -> int:
    """统计聚焦白框内（内容像素）「深色墨迹」像素，判断方块光标是否处于亮相位。"""
    import numpy as _np
    a = _np.asarray(img.convert("RGB")).astype(int)
    x0, y0, x1, y1 = box
    sub = a[y0:y1, x0:x1]
    return int(((sub < 120).all(axis=2)).sum())


def capture_with_cursor(out_name: str, *, focus_cell: bool = False) -> None:
    """点进 NAME 输入框使其聚焦，连拍多张覆盖 1s 闪烁的两个相位，取光标可见的那张。

    软键盘必须真正禁用：Gboard 弹出会经 imePadding 把表单整体顶起，NAME 白框位移、
    下半屏被遮挡。`show_ime_with_hard_keyboard 0` 不足以阻止软键盘，这里临时
    `ime disable` 所有软 IME，截图结束后 re-enable 恢复设备状态。
    """
    sh("shell", "settings", "put", "secure", "show_ime_with_hard_keyboard", "0")
    ime_list = [s.strip() for s in sh("shell", "ime", "list", "-s").split() if s.strip()]
    for ime in ime_list:
        sh("shell", "ime", "disable", ime)
    try:
        if focus_cell:
            tap(168, 478)                   # 日期白框中部（内容像素；白框 y≈445..512）
            cursor_box = (60, 432, 560, 524)
        else:
            tap(200, 280)                   # NAME 白框中部（内容像素；白框 y≈248..324）
            cursor_box = (60, 240, 560, 330)
        time.sleep(0.4)
        # tap 会把光标落到点击处，先按 End 移回文本末尾（与 design.html 的「文字 + 末尾光标」一致）；
        # 再敲一个字符并删掉 —— 文本变化会让 Compose 收起点击触发的系统插入手柄。
        sh("shell", "input", "keyevent", "123")   # KEYCODE_MOVE_END
        time.sleep(0.3)
        sh("shell", "input", "text", "a")
        time.sleep(0.3)
        sh("shell", "input", "keyevent", "67")    # KEYCODE_DEL
        time.sleep(0.4)
        cands = []
        for i in range(6):
            raw = HERE / f"_android_raw_cursor{i}.png"
            p = subprocess.run([ADB, "exec-out", "screencap", "-p"], capture_output=True)
            raw.write_bytes(p.stdout)
            im = Image.open(raw).convert("RGB")
            if im.size != (TARGET_W, h):
                im = im.crop((0, 0, TARGET_W, h))
            crop = im.crop((0, top, TARGET_W, top + TARGET_H))
            cands.append((cursor_pixels(crop, cursor_box), crop))
            time.sleep(0.25)
    finally:
        for ime in ime_list:
            sh("shell", "ime", "enable", ime)
    best = max(cands, key=lambda c: c[0])
    out = HERE / f"{out_name}.png"
    best[1].save(out)
    print(f"saved {out} {best[1].size} 光标像素={best[0]}（候选 {[c[0] for c in cands]}）")


def reset_debug_props() -> None:
    """复位所有 debug fixture 属性。

    必须做：setprop 是设备级状态，不回滚的话脚本跑完属性仍留在设备上，
    之后正常打开 App 会直接进第 ④ 屏并带着截图用的示例数据，被误认为产品行为。

    值必须写成带引号的 `""`：adb 会把参数拼成设备侧 shell 命令，
    直接传空串会变成 `setprop NAME`（少一个参数）而被 setprop 拒绝。
    """
    for prop in ("debug.momentmark.nav", "debug.momentmark.taskform"):
        sh("shell", "setprop", prop, '""')
    sh("shell", "am", "force-stop", PKG)


seed = sys.argv[1] if len(sys.argv) > 1 else "sample"
out_name = sys.argv[2] if len(sys.argv) > 2 and not sys.argv[2].startswith("--") else f"current-implementation-{seed}"
with_cursor = "--with-cursor" in sys.argv
focus_cell = "--cursor-cell" in sys.argv
scroll_bottom = "--scroll-bottom" in sys.argv

# 2) 清数据 + fixture 直启第 ④ 屏。
sh("shell", "pm", "clear", PKG)
sh("shell", "setprop", "debug.momentmark.nav", "task")
sh("shell", "setprop", "debug.momentmark.taskform", seed)
sh("shell", "am", "start", "-n", f"{PKG}/{ACTIVITY}")
time.sleep(4)

try:
    if scroll_bottom:
        # 上滑到底：若表单内容高于可视区，提交按钮需要滚动才可见（design.html 同样如此）。
        for _ in range(4):
            sh("shell", "input", "swipe", "314", "1000", "314", "500", "180")
            time.sleep(0.5)
        time.sleep(0.6)
    if with_cursor:
        capture_with_cursor(out_name, focus_cell=focus_cell)
    else:
        screencap("tmp", out_name=out_name)
finally:
    reset_debug_props()

print("done")
