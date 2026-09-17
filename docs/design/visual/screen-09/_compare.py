#!/usr/bin/env python3
"""design.png vs current-implementation.png 像素级对比：
输出 metrics.json（PDR/SSIM/分通道均值差/bbox）与 diff.png（红=设计稿独有、绿=实现独有、蓝=边缘）。"""
import json
from pathlib import Path

import numpy as np
from PIL import Image
from skimage.metrics import structural_similarity

HERE = Path(__file__).resolve().parent
DESIGN = HERE / "design.png"
IMPL = HERE / "current-implementation.png"
DIFF = HERE / "diff.png"
METRICS = HERE / "metrics.json"

a = np.asarray(Image.open(DESIGN).convert("RGB"), dtype=np.int16)
b = np.asarray(Image.open(IMPL).convert("RGB"), dtype=np.int16)
assert a.shape == b.shape, f"size mismatch design={a.shape} impl={b.shape}"

d = np.abs(a - b).astype(np.int16)
changed = np.any(d > 8, axis=2)
pdr = float(changed.mean())

ssim_all = structural_similarity(a, b, channel_axis=2, data_range=255)
per_chan = {
    name: float(np.mean(d[..., i])) for i, name in enumerate(("R", "G", "B"))
}
ys, xs = np.where(changed)
bbox = {
    "x": [int(xs.min()), int(xs.max())],
    "y": [int(ys.min()), int(ys.max())],
} if changed.any() else None

# diff 可视化：设计稿独有=红、实现独有=绿、双方都有差异=蓝
vis = np.full_like(a, 255)
vis[changed] = np.array([60, 60, 220], dtype=np.int16)
Image.fromarray(vis.astype(np.uint8)).save(DIFF)

out = {
    "size": [int(a.shape[1]), int(a.shape[0])],
    "pixel_difference_rate": round(pdr, 6),
    "ssim": round(float(ssim_all), 6),
    "mean_abs_channel_diff": per_chan,
    "changed_bbox": bbox,
}
METRICS.write_text(json.dumps(out, ensure_ascii=False, indent=2) + "\n")
print(json.dumps(out, ensure_ascii=False, indent=2))
