#!/usr/bin/env python3
"""第 ⑤ 屏（日子簿 · 副本地图）几何偏差审计。

两种口径：
  A) 结构线扫描（x 带 280..340，被完整横穿 ≥80% 的深色水平线）→ 按顺序一一配对，
     给出 offset 数列，用来判断偏差是「逐条累积」（间距/内距用错）还是「整体平移」（某个 inset 一次错）。
  B) 相对几何：把第一条结构线归一为 0，比较各结构线之间的间距，剥离截图裁切偏移的影响。
  C) 日历行带（全宽扫描，等价 _compare.py 的 text_row_bands）→ 行距与行高。

产物：`geometry-audit.json`
"""
from __future__ import annotations
import json
from pathlib import Path

import numpy as np
from PIL import Image

HERE = Path(__file__).resolve().parent
INK = np.array([43, 38, 32])


def hlines(png: Path, x0: int = 280, x1: int = 340) -> list[dict]:
    im = np.asarray(Image.open(png).convert("RGB")).astype(int)
    sub = im[:, x0:x1]
    hit = (np.abs(sub - INK).max(axis=2) <= 40).sum(axis=1) > (x1 - x0) * 0.8
    out, start = [], None
    for y, v in enumerate(hit):
        if v and start is None:
            start = y
        elif not v and start is not None:
            out.append({"top": start, "bottom": y - 1, "center": (start + y - 1) // 2,
                        "thickness": y - start})
            start = None
    if start is not None:
        out.append({"top": start, "bottom": len(hit) - 1,
                    "center": (start + len(hit) - 1) // 2, "thickness": len(hit) - start})
    return out


def row_bands(png: Path) -> list[list[int]]:
    im = np.asarray(Image.open(png).convert("RGB")).astype(int)
    m = (np.abs(im - INK).max(axis=2) <= 40).sum(axis=1) > 2
    bands, start = [], None
    for y, v in enumerate(m):
        if v and start is None:
            start = y
        elif not v and start is not None:
            if y - start >= 3:
                bands.append([start, y - 1, y - start])
            start = None
    return bands


def main() -> None:
    design_lines = hlines(HERE / "design.png")
    impl_lines = hlines(HERE / "current-implementation.png")
    n = min(len(design_lines), len(impl_lines))
    pairs = [
        {
            "index": i,
            "design_center": design_lines[i]["center"],
            "impl_center": impl_lines[i]["center"],
            "offset_px": impl_lines[i]["center"] - design_lines[i]["center"],
            "offset_dp": round((impl_lines[i]["center"] - design_lines[i]["center"]) / 2, 1),
            "design_thickness": design_lines[i]["thickness"],
            "impl_thickness": impl_lines[i]["thickness"],
            "design_rel": design_lines[i]["center"] - design_lines[0]["center"],
            "impl_rel": impl_lines[i]["center"] - impl_lines[0]["center"],
        }
        for i in range(n)
    ]
    for p in pairs:
        p["rel_delta_px"] = p["impl_rel"] - p["design_rel"]
        p["rel_delta_dp"] = round(p["rel_delta_px"] / 2, 1)

    offsets = [abs(p["offset_px"]) for p in pairs]
    rel = [abs(p["rel_delta_px"]) for p in pairs]
    db = row_bands(HERE / "design.png")
    ib = row_bands(HERE / "current-implementation.png")

    report = {
        "mode": "daybook",
        "unit": "设备像素 @2x（÷2 = dp/CSS px）",
        "method": "结构线：x 带 280..340 内被完整横穿(≥80%)的深色水平线，按出现顺序配对",
        "design_line_count": len(design_lines),
        "impl_line_count": len(impl_lines),
        "pair_count": n,
        "borders": pairs,
        "design_lines": design_lines,
        "impl_lines": impl_lines,
        "summary": {
            "first_line_offset_px": pairs[0]["offset_px"] if pairs else None,
            "max_abs_offset_px": max(offsets) if offsets else 0,
            "mean_abs_offset_px": round(float(np.mean(offsets)), 2) if offsets else 0,
            "within_4px_pct": round(100.0 * sum(o <= 4 for o in offsets) / len(offsets), 1) if offsets else 0,
            "max_abs_rel_delta_px": max(rel) if rel else 0,
            "mean_abs_rel_delta_px": round(float(np.mean(rel)), 2) if rel else 0,
            "within_4px_rel_pct": round(100.0 * sum(o <= 4 for o in rel) / len(rel), 1) if rel else 0,
        },
        "row_bands_design": db,
        "row_bands_impl": ib,
    }
    (HERE / "geometry-audit.json").write_text(
        json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")

    print("== summary ==")
    print(json.dumps(report["summary"], ensure_ascii=False, indent=2))
    print("== order: design_center -> impl_center (offset_px / rel_delta_px) ==")
    for p in pairs:
        print(f"  #{p['index']:2d}  {p['design_center']:5d} -> {p['impl_center']:5d}   "
              f"off={p['offset_px']:+4d}  rel={p['rel_delta_px']:+4d}  "
              f"thick {p['design_thickness']}->{p['impl_thickness']}")
    print("== row bands ==")
    print("design:", db)
    print("impl  :", ib)


if __name__ == "__main__":
    main()
