#!/usr/bin/env python3
"""第 ① 屏视觉量化：对齐后的设计基准 vs Android 实现。

两张图使用同一画布（314x648 逻辑像素 @2x = 628x1296 设备像素）、同一排版边界
（无系统状态栏 / 无手势条 / 无手机外壳），因此数值差异只反映：

  结构 · 尺寸 · 间距 · 字体 · 颜色 · 硬影 · 渲染引擎栅格化

产物：difference-overlay.png / difference-heat.png / metrics.json
运行：/opt/miniconda3/bin/python3 _compare.py
"""
from __future__ import annotations
import json
from pathlib import Path

import numpy as np
from PIL import Image
from skimage.metrics import structural_similarity as ssim

HERE = Path(__file__).resolve().parent

# DESIGN_SYSTEM.md §3.1 昼间 token
COLORS = {
    "background": (232, 224, 204),
    "surface": (246, 241, 227),
    "surfaceVariant": (223, 213, 187),
    "onSurface": (43, 38, 32),
    "outline": (43, 38, 32),
    "outlineVariant": (201, 191, 166),
    "primary": (70, 193, 104),
    "secondary": (62, 155, 255),
    "tertiary": (245, 179, 1),
    "goldInk": (185, 138, 0),
    "error": (229, 72, 77),
    "amber": (245, 166, 35),
    "manaPurple": (155, 109, 255),
    "labelTertiary": (167, 155, 133),
}


def load(name: str):
    im = Image.open(HERE / name).convert("RGB")
    return im, np.asarray(im).astype(np.float32)


design_img, design = load("design-original.png")
impl_img, impl = load("current-implementation.png")
assert design.shape == impl.shape, f"画布不一致 {design.shape} vs {impl.shape}"
H, W, _ = design.shape

# ── 1. 全局像素差 / SSIM ────────────────────────────────────────────────
diff = np.abs(design - impl)
maxchan = diff.max(axis=2)
mask = maxchan > 0                       # 与既有组件级报告同口径：任一通道变化即计入
total = H * W
diff_count = int(mask.sum())
pixel_diff_rate = diff_count / total * 100.0
gray_d = (design @ np.array([0.299, 0.587, 0.114], dtype=np.float32))
gray_i = (impl @ np.array([0.299, 0.587, 0.114], dtype=np.float32))
ssim_val = float(ssim(gray_d, gray_i, data_range=255))

# 容差版：允许 ±T 的栅格化抖动，反映“结构是否落在同一位置”
def tolerant_rate(t: int) -> float:
    return float((np.abs(design - impl) > t).max(axis=2).mean() * 100.0)

# ── 2. 颜色误差（按设计稿颜色分类统计）────────────────────────────────
def color_error(name: str):
    ref = np.array(COLORS[name], dtype=np.float32)
    sel = (np.abs(design - ref).max(axis=2) <= 10)
    if not sel.any():
        return {"token": name, "pixels": 0, "mae_rgb": None}
    return {
        "token": name,
        "pixels": int(sel.sum()),
        "share_pct": round(float(sel.mean() * 100), 3),
        "mae_rgb": round(float(diff[sel].mean()), 3),
        "mean_color_error": round(float(np.abs(design[sel] - impl[sel]).mean()), 3),
    }


color_errors = [color_error(k) for k in ("background", "surface", "onSurface", "outline",
                                          "tertiary", "goldInk", "primary", "secondary",
                                          "error", "amber", "labelTertiary")]

# ── 3. 版面几何：卡片边界（沿固定扫描线找颜色跳变）────────────────────
def transitions(line: np.ndarray, axis_label: str):
    """把一维颜色序列压缩为 (start,end,color) 的行程段。"""
    runs = []
    start = 0
    cur = tuple(int(v) for v in line[0])
    for i in range(1, len(line)):
        c = tuple(int(v) for v in line[i])
        if c != cur:
            runs.append((start, i - 1, cur))
            start, cur = i, c
    runs.append((start, len(line) - 1, cur))
    return [(a, b, c) for a, b, c in runs if b - a >= 2]


def classify(rgb):
    """把颜色归到最近的 token，用于摘要。"""
    best, bd = None, 1e9
    for k, v in COLORS.items():
        d = sum((int(rgb[i]) - v[i]) ** 2 for i in range(3))
        if d < bd:
            bd, best = d, k
    return best if bd < 2000 else f"#{rgb[0]:02X}{rgb[1]:02X}{rgb[2]:02X}"


# 左列卡片纵向扫描线（x 落在左列卡片内部）
x_scan = int(W * 0.12)
prof_design = transitions(design[:, x_scan], "y")
prof_impl = transitions(impl[:, x_scan], "y")


def summarize_bands(runs):
    out = []
    for a, b, c in runs:
        label = classify(c)
        if label in ("background",):
            continue
        out.append({"y0": a, "y1": b, "h": b - a + 1, "color": label})
    return out


bands_design = summarize_bands(prof_design)
bands_impl = summarize_bands(prof_impl)

# ── 4. 元素 bbox：按颜色类聚出关键块，比较位置与尺寸 ────────────────────
def bbox_of(mask2d):
    ys, xs = np.nonzero(mask2d)
    if len(ys) == 0:
        return None
    return [int(xs.min()), int(ys.min()), int(xs.max()), int(ys.max())]


def bboxes_for(img, token):
    ref = np.array(COLORS[token], dtype=np.float32)
    m = (np.abs(img - ref).max(axis=2) <= 12)
    return bbox_of(m)


element_boxes = {}
for token in ("surface", "outline", "tertiary", "goldInk", "primary", "error", "amber"):
    bd = bboxes_for(design, token)
    bi = bboxes_for(impl, token)
    element_boxes[token] = {
        "design_bbox": bd,
        "impl_bbox": bi,
        "delta_wh": None if not bd or not bi else [bi[2] - bi[0] - (bd[2] - bd[0]),
                                                   bi[3] - bi[1] - (bd[3] - bd[1])],
        "delta_xy": None if not bd or not bi else [bi[0] - bd[0], bi[1] - bd[1]],
    }

# ── 5. 文本行高度（字体度量代理）：统计墨色像素的连续行带 ───────────────
def text_row_bands(img, x0, x1):
    ink = np.array(COLORS["onSurface"], dtype=np.float32)
    sub = img[:, x0:x1]
    m = (np.abs(sub - ink).max(axis=2) <= 40).sum(axis=1) > 2
    bands, start = [], None
    for y, v in enumerate(m):
        if v and start is None:
            start = y
        elif not v and start is not None:
            if y - start >= 3:
                bands.append((start, y - 1, y - start))
            start = None
    return bands


# ── 6. 产物 ────────────────────────────────────────────────────────────
overlay = np.asarray(impl_img).copy()
ov = overlay.copy()
ov[mask] = [255, 0, 255]
Image.fromarray(ov).save(HERE / "difference-overlay.png")

heat = np.clip(diff.max(axis=2) * 3, 0, 255).astype(np.uint8)
Image.fromarray(np.stack([heat] * 3, axis=-1)).save(HERE / "difference-heat.png")

# 并排对照
side = Image.new("RGB", (W * 2 + 16, H), (255, 255, 255))
side.paste(design_img, (0, 0))
side.paste(impl_img, (W + 16, 0))
side.save(HERE / "side-by-side.png")

report = {
    "canvas": f"{W}x{H}",
    "logical_canvas": f"{W // 2}x{H // 2} dp @2x",
    "device_scale_factor": 2,
    "design_source": "design.html 第①屏，.phone 内容区 314x648 CSS px",
    "impl_source": "Pixel AVD，wm size 628x1296 / density 320，immersive（无系统栏）",
    "global": {
        "pixel_difference_rate_pct": round(pixel_diff_rate, 4),
        "pixel_similarity_pct": round(100 - pixel_diff_rate, 4),
        "ssim_gray": round(ssim_val, 6),
        "mae_rgb": round(float(diff.mean()), 4),
        "tolerance_1_pct": round(tolerant_rate(1), 4),
        "tolerance_8_pct": round(tolerant_rate(8), 4),
        "tolerance_24_pct": round(tolerant_rate(24), 4),
        "thresholds": {"pixel_difference_rate_pct": 1.0, "ssim_gray": 0.95},
        "passed": bool(pixel_diff_rate < 1.0 and ssim_val > 0.95),
    },
    "color_errors": color_errors,
    "bands_left_column_design": bands_design,
    "bands_left_column_impl": bands_impl,
    "element_boxes": element_boxes,
    "text_bands_design": text_row_bands(design, 0, W),
    "text_bands_impl": text_row_bands(impl, 0, W),
}

(HERE / "metrics.json").write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps({k: report[k] for k in ("canvas", "global")}, ensure_ascii=False, indent=2))
print("bands design:", bands_design)
print("bands impl  :", bands_impl)
