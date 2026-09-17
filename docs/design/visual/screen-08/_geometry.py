#!/usr/bin/env python3
"""design.png vs current-implementation.png 关键几何测量（@2x 画布，单位 px）。

测量项：
  - 搜索白框的 top / bottom / height（按白色行检测）
  - 抽屉面板 top / bottom / left / right / width（按面板色列检测）
  - 底部导航 top（按 surface 色行检测）
  - 背景卡列表首行 top
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


def analyze(path: str) -> dict:
    im = np.asarray(Image.open(HERE / path).convert("RGB"))
    h, w, _ = im.shape
    res = {"file": path}

    # 搜索白框：整行大部分为 #FFFFFF 的行段（限制在前 200 行内）
    white_rows = []
    for y in range(0, 200):
        frac = near(im[y], np.array([255, 255, 255]), 10).mean()
        if frac > 0.5:
            white_rows.append(y)
    if white_rows:
        res["search_top"] = white_rows[0]
        res["search_bottom"] = white_rows[-1]
        res["search_height"] = white_rows[-1] - white_rows[0] + 1

    # 抽屉面板：在 y=600 行找从左到右的第一段 panel 色游程（覆盖 >100px）
    row = im[600]
    runs = []
    start = None
    for x in range(w):
        if near(row[x], PANEL) or near(row[x], OUTLINE, 6):
            if start is None:
                start = x
        else:
            if start is not None and x - start > 100:
                runs.append((start, x - 1))
            start = None
    if start is not None and w - start > 100:
        runs.append((start, w - 1))
    if runs:
        res["drawer_left"] = runs[0][0]
        res["drawer_right"] = runs[0][1]
        res["drawer_width"] = runs[0][1] - runs[0][0] + 1

    # 抽屉 top/bottom：在 drawer_left+20 列找 panel 色游程（y 从 200 起）
    col = im[:, res.get("drawer_left", 40) + 20]
    ys = [y for y in range(150, h) if near(col[y], PANEL, 8)]
    if ys:
        res["drawer_top"] = ys[0]
        res["drawer_bottom"] = ys[-1]
        res["drawer_height"] = ys[-1] - ys[0] + 1

    # 底部导航 top：底部区域第一行整行 panel 色（y 从 1100 找）
    for y in range(1100, h):
        if near(im[y], PANEL, 6).mean() > 0.6:
            res["nav_top"] = y
            break

    # 背景卡首行 top：x=drawer_right+80 列在 scrim 区域内的 panel 色段起点
    bx = min(res.get("drawer_right", 440) + 80, w - 5)
    colb = im[:, bx]
    ys2 = [y for y in range(res.get("search_bottom", 150), 1100) if near(colb[y], PANEL, 10)]
    if ys2:
        res["backdrop_first_top"] = ys2[0]

    return res


out = [analyze("design.png"), analyze("current-implementation.png")]
print(json.dumps(out, ensure_ascii=False, indent=2))
(HERE / "geometry.json").write_text(json.dumps(out, ensure_ascii=False, indent=2) + "\n")
