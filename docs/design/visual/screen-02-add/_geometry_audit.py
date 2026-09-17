#!/usr/bin/env python3
"""第 ②③ 屏几何偏差审计：设计稿 DOM 实测值 vs Android 实现截图像素测量。

设计侧：`design-geometry-*.json`（headless Chrome `getBoundingClientRect` + `getComputedStyle`，
单位 CSS px，与 Android 的 dp 1:1）。
实现侧：从 `current-implementation-*.png` 提取面板水平边框线的中心 y（设备像素 ÷2 = dp），
与设计侧对应的边框线配对比较。

产物：`geometry-audit-<mode>.json`

用法：
  /Users/cch/.workbuddy/binaries/python/envs/default/bin/python3 _geometry_audit.py future
"""
from __future__ import annotations
import json
import sys
from pathlib import Path

import numpy as np
from PIL import Image

HERE = Path(__file__).resolve().parent
INK = np.array([43, 38, 32])

# 设计侧水平边框线（由 design-geometry-*.json 的面板 y/h 推导；y 与 y+h 各一条）。
# 这些面板在 x=280..340 设备像素处被完整横穿，因此可用同一列带扫描配对。
PANEL_ORDER = [
    "pagetitle",        # .pagetitle 顶/底
    "direction",        # .px4 顶/底（含内部按钮行边框）
    "name",             # .px 顶 / 白框顶 / 白框底 / .px 底
    "target",           # .px 顶 / 三格顶 / 三格底 / .px 底
    "lore",             # .px 顶 / notebox 顶 / notebox 底 / .px 底
    "party",            # .px 顶 / party-rail 顶 / party-rail 底 / rarity 行 / .px 底
]


def hlines(png: Path, x0: int = 280, x1: int = 340) -> list[int]:
    """返回被横穿的深色水平线中心 y（设备像素）。"""
    im = np.asarray(Image.open(png).convert("RGB")).astype(int)
    sub = im[:, x0:x1]
    hit = (np.abs(sub - INK).max(axis=2) <= 40).sum(axis=1) > (x1 - x0) * 0.8
    out, start = [], None
    for y, v in enumerate(hit):
        if v and start is None:
            start = y
        elif not v and start is not None:
            out.append((start + y - 1) // 2)
            start = None
    if start is not None:
        out.append((start + len(hit) - 1) // 2)
    return out


def main(mode: str) -> None:
    design = hlines(HERE / f"design-{mode}.png")
    impl = hlines(HERE / f"current-implementation-{mode}.png")
    n = min(len(design), len(impl))
    pairs = [
        {
            "index": i,
            "design_px": design[i],
            "impl_px": impl[i],
            "offset_px": impl[i] - design[i],
            "offset_dp": round((impl[i] - design[i]) / 2, 1),
        }
        for i in range(n)
    ]
    offsets = [abs(p["offset_px"]) for p in pairs]
    report = {
        "mode": mode,
        "unit": "设备像素 @2x（÷2 = dp/CSS px）",
        "method": "同一 x 带 (280..340) 扫描被完整横穿的深色水平边框线，按出现顺序一一配对",
        "pair_count": n,
        "borders": pairs,
        "summary": {
            "max_abs_offset_px": max(offsets) if offsets else 0,
            "mean_abs_offset_px": round(float(np.mean(offsets)), 2) if offsets else 0,
            "within_2px_pct": round(100.0 * sum(o <= 2 for o in offsets) / len(offsets), 1) if offsets else 0,
            "within_4px_pct": round(100.0 * sum(o <= 4 for o in offsets) / len(offsets), 1) if offsets else 0,
        },
    }
    (HERE / f"geometry-audit-{mode}.json").write_text(
        json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(report["summary"], ensure_ascii=False))
    print("offsets_px:", [p["offset_px"] for p in pairs])


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else "future")
