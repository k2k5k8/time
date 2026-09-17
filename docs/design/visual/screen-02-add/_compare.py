#!/usr/bin/env python3
"""第 ②③ 屏视觉量化：HTML 设计稿 vs Android 实现。

与 screen-01-home 同口径：628x1296 = 314x648 逻辑像素 @2x；差异口径=任一通道变化即计入。
用法：
  /Users/cch/.workbuddy/binaries/python/envs/default/bin/python3 _compare.py future
  /Users/cch/.workbuddy/binaries/python/envs/default/bin/python3 _compare.py past
"""
from __future__ import annotations
import json
import sys
from pathlib import Path

import numpy as np
from PIL import Image
from skimage.metrics import structural_similarity as ssim

HERE = Path(__file__).resolve().parent

# DESIGN_SYSTEM.md §3.1 昼间 token（与 _compare.py 第①屏共享）
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


def color_error(design: np.ndarray, impl: np.ndarray, diff: np.ndarray, name: str):
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


def classify(rgb, best_threshold=2000):
    best, bd = None, 1e9
    for k, v in COLORS.items():
        d = sum((int(rgb[i]) - v[i]) ** 2 for i in range(3))
        if d < bd:
            bd, best = d, k
    return best if bd < best_threshold else f"#{int(rgb[0]):02X}{int(rgb[1]):02X}{int(rgb[2]):02X}"


def main(mode: str):
    design_img, design = load(f"design-{mode}.png")
    impl_img, impl = load(f"current-implementation-{mode}.png")
    assert design.shape == impl.shape, f"画布不一致 {design.shape} vs {impl.shape}"
    H, W, _ = design.shape

    diff = np.abs(design - impl)
    maxchan = diff.max(axis=2)
    mask = maxchan > 0
    total = H * W
    diff_count = int(mask.sum())
    pixel_diff_rate = diff_count / total * 100.0
    gray_d = design @ np.array([0.299, 0.587, 0.114], dtype=np.float32)
    gray_i = impl @ np.array([0.299, 0.587, 0.114], dtype=np.float32)
    ssim_val = float(ssim(gray_d, gray_i, data_range=255))

    def tolerant_rate(t: int) -> float:
        return float((np.abs(design - impl) > t).max(axis=2).mean() * 100.0)

    color_errors = [color_error(design, impl, diff, k) for k in
                    ("background", "surface", "onSurface", "outline",
                     "tertiary", "goldInk", "primary", "secondary",
                     "error", "amber", "labelTertiary")]

    # 元素 bbox：选关键色 token 在两张图上的边界。
    def bbox_of(m):
        ys, xs = np.nonzero(m)
        if len(ys) == 0:
            return None
        return [int(xs.min()), int(ys.min()), int(xs.max()), int(ys.max())]

    def bboxes_for(img, token):
        ref = np.array(COLORS[token], dtype=np.float32)
        m = (np.abs(img - ref).max(axis=2) <= 12)
        return bbox_of(m)

    element_boxes = {}
    for token in ("surface", "outline", "tertiary", "goldInk", "primary", "error",
                  "amber", "secondary"):
        bd = bboxes_for(design, token)
        bi = bboxes_for(impl, token)
        element_boxes[token] = {
            "design_bbox": bd,
            "impl_bbox": bi,
            "delta_wh": None if not bd or not bi else [bi[2] - bi[0] - (bd[2] - bd[0]),
                                                        bi[3] - bi[1] - (bd[3] - bd[1])],
            "delta_xy": None if not bd or not bi else [bi[0] - bd[0], bi[1] - bd[1]],
        }

    # 文本行带（墨色像素的连续行带）— 帮助评估字号、行高差异。
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

    # 产物
    overlay = np.asarray(impl_img).copy()
    ov = overlay.copy()
    ov[mask] = [255, 0, 255]
    Image.fromarray(ov).save(HERE / f"difference-overlay-{mode}.png")

    heat = np.clip(diff.max(axis=2) * 3, 0, 255).astype(np.uint8)
    Image.fromarray(np.stack([heat] * 3, axis=-1)).save(HERE / f"difference-heat-{mode}.png")

    side = Image.new("RGB", (W * 2 + 16, H), (255, 255, 255))
    side.paste(design_img, (0, 0))
    side.paste(impl_img, (W + 16, 0))
    side.save(HERE / f"side-by-side-{mode}.png")

    report = {
        "canvas": f"{W}x{H}",
        "logical_canvas": f"{W // 2}x{H // 2} dp @2x",
        "device_scale_factor": 2,
        "mode": mode,
        "design_source": f"design.html 第{'②③'[0 if mode == 'future' else 1]}屏（.phone 内容区 314x648 CSS px）",
        "impl_source": "Pixel AVD, wm size 628x1296 / density 320, immersive",
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
        "element_boxes": element_boxes,
        "text_bands_design": text_row_bands(design, 0, W),
        "text_bands_impl": text_row_bands(impl, 0, W),
    }
    (HERE / f"metrics-{mode}.json").write_text(json.dumps(report, ensure_ascii=False, indent=2),
                                                encoding="utf-8")
    print(json.dumps({"canvas": report["canvas"], "global": report["global"]},
                     ensure_ascii=False, indent=2))


if __name__ == "__main__":
    mode = sys.argv[1] if len(sys.argv) > 1 else "future"
    main(mode)
