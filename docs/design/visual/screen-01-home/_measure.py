#!/usr/bin/env python3
"""量测第 ① 屏卡片几何：沿卡片左侧墨线扫描得到每张卡的外框高度（不含硬影）。

单位：2x 设备像素（÷2 = 逻辑 px / dp）。
"""
from __future__ import annotations
import sys
from pathlib import Path

import numpy as np
from PIL import Image

HERE = Path(__file__).resolve().parent
BG = np.array([232, 224, 204])
INK = np.array([43, 38, 32])


def load(name):
    return np.asarray(Image.open(HERE / name).convert("RGB")).astype(np.int16)


def ink_runs(mask, bridge=2, min_len=20):
    idx = np.flatnonzero(mask)
    if len(idx) == 0:
        return []
    out, start, prev = [], idx[0], idx[0]
    for i in idx[1:]:
        if i - prev <= bridge + 1:
            prev = i
        else:
            out.append((start, prev))
            start = prev = i
    out.append((start, prev))
    return [(s, e, e - s + 1) for s, e in out if e - s + 1 >= min_len]


def report(name):
    a = load(name)
    H, W, _ = a.shape
    ink = np.abs(a - INK).max(axis=-1) <= 40
    print(f"\n===== {name}  {W}x{H}（2x）=====")
    for x in (26, 326):
        print(f"  卡片左墨线 x={x}（高度=外框，不含硬影）:")
        for s, e, h in ink_runs(ink[:, x]):
            print(f"     y {s:5d} → {e:5d}   高 {h:5d}px = {h/2:7.1f} 逻辑")
    # 卡片左右边界：取每张卡中部的一行
    for y in (150, 420, 680, 950, 1200):
        rr = ink_runs(ink[y, :], bridge=2, min_len=2)
        print(f"  横切 y={y}: " + ", ".join(f"[{s}→{e}]" for s, e, _ in rr[:12]))


for n in sys.argv[1:] or ["design-original.png", "current-implementation.png"]:
    report(n)
