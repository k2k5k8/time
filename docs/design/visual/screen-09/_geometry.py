#!/usr/bin/env python3
"""design.png vs current-implementation.png 关键几何测量（@2x 画布，单位 px）。

测量项（按面板色行检测）：
  - 页题面板 top / bottom
  - 冒险者卡 top / bottom
  - 显示模式面板 top / bottom
  - 设置列表面板 top / bottom
  - 底部导航 top
"""
from pathlib import Path
import json

import numpy as np
from PIL import Image

HERE = Path(__file__).resolve().parent

BG = np.array([0xE8, 0xE0, 0xCC])
PANEL = np.array([0xF6, 0xF1, 0xE3])
OUTLINE = np.array([0x2B, 0x26, 0x20])


def near(px: np.ndarray, color: np.ndarray, tol: int = 12) -> np.ndarray:
    return np.all(np.abs(px.astype(int) - color) <= tol, axis=-1)


def find_panel_runs(im: np.ndarray) -> list[tuple[int, int]]:
    """按行检测面板色（surface）占比 > 40% 的行段，返回 (top, bottom) 列表。"""
    h, w, _ = im.shape
    runs = []
    start = None
    for y in range(h):
        frac = near(im[y], PANEL, 10).mean()
        if frac > 0.4:
            if start is None:
                start = y
        else:
            if start is not None:
                if y - start > 10:  # 忽略 <10px 的噪声行
                    runs.append((start, y - 1))
                start = None
    if start is not None and h - start > 10:
        runs.append((start, h - 1))
    return runs


def analyze(path: str) -> dict:
    im = np.asarray(Image.open(HERE / path).convert("RGB"))
    h, w, _ = im.shape
    res = {"file": path, "size": [w, h]}

    runs = find_panel_runs(im)
    res["panel_runs"] = [{"top": r[0], "bottom": r[1], "height": r[1] - r[0] + 1} for r in runs]

    # 底部导航：最底部的面板色行段
    if runs:
        nav = runs[-1]
        res["nav_top"] = nav[0]
        res["nav_bottom"] = nav[1]
        res["nav_height"] = nav[1] - nav[0] + 1

    return res


out = [analyze("design.png"), analyze("current-implementation.png")]
print(json.dumps(out, ensure_ascii=False, indent=2))
(HERE / "geometry.json").write_text(json.dumps(out, ensure_ascii=False, indent=2) + "\n")
