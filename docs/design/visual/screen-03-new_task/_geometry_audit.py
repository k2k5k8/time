#!/usr/bin/env python3
"""第 ④ 屏几何偏差审计：设计稿 DOM 实测值 vs Android 实现截图像素测量。

产物：`geometry-audit.json`
"""
from __future__ import annotations
import json
import sys
from pathlib import Path

import numpy as np
from PIL import Image

HERE = Path(__file__).resolve().parent
INK = np.array([43, 38, 32])


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


def main() -> None:
    design = hlines(HERE / "design.png")
    impl = hlines(HERE / "current-implementation-sample.png")
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
        "mode": "new_task",
        "unit": "设备像素 @2x（÷2 = dp/CSS px）",
        "method": "同一 x 带 (280..340) 扫描被完整横穿的深色水平边框线，按出现顺序一一配对",
        "design_line_count": len(design),
        "impl_line_count": len(impl),
        "pair_count": n,
        "borders": pairs,
        "summary": {
            "max_abs_offset_px": max(offsets) if offsets else 0,
            "mean_abs_offset_px": round(float(np.mean(offsets)), 2) if offsets else 0,
            "within_2px_pct": round(100.0 * sum(o <= 2 for o in offsets) / len(offsets), 1) if offsets else 0,
            "within_4px_pct": round(100.0 * sum(o <= 4 for o in offsets) / len(offsets), 1) if offsets else 0,
        },
    }
    (HERE / "geometry-audit.json").write_text(
        json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(report["summary"], ensure_ascii=False))
    print("design_lines:", design)
    print("impl_lines:", impl)
    print("offsets_px:", [p["offset_px"] for p in pairs])


if __name__ == "__main__":
    main()
