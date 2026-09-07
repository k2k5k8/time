#!/usr/bin/env python3
"""Visual regression: original-card vs component mirror for the MAIN QUEST card."""
import json
import numpy as np
from PIL import Image
from skimage.metrics import structural_similarity as ssim

DIR = "/Users/cch/Desktop/Time/docs/design/visual/home-main-quest"

def load(path):
    im = Image.open(path).convert("RGB")
    return im, np.asarray(im).astype(np.float32)

orig_img, orig = load(f"{DIR}/original.png")
comp_img, comp = load(f"{DIR}/component.png")

# 1) geometry must match
assert orig.shape == comp.shape, f"shape mismatch {orig.shape} vs {comp.shape}"

H, W, _ = orig.shape
total = H * W

# 2) pixel difference
diff = np.abs(orig - comp)
maxchan = diff.max(axis=2)
diff_mask = maxchan > 0
diff_count = int(diff_mask.sum())
pixel_diff_rate = diff_count / total * 100.0
pixel_similarity = 100.0 - pixel_diff_rate

# 3) SSIM (grayscale, full image)
ssim_val = ssim(orig[..., 0], comp[..., 0], data_range=255)

# 4) color error (MAE per channel + overall)
mae = diff.mean()
bg = np.array([232, 224, 204], dtype=np.float32)  # --bg #E8E0CC
bg_mask = (np.abs(orig - bg).max(axis=2) <= 8)
mae_bg = float(diff[bg_mask].mean()) if bg_mask.any() else 0.0

# 5) layout deviation: bounding box of "content" (non-background) pixels
def content_bbox(a):
    # content = pixels that differ from the background color #E8E0CC
    bg = np.array([232, 224, 204], dtype=np.float32)
    nonbg = (np.abs(a - bg).max(axis=2) > 8)
    ys, xs = np.nonzero(nonbg)
    if len(ys) == 0:
        return None
    return (int(xs.min()), int(ys.min()), int(xs.max()), int(ys.max()))

bb_o = content_bbox(orig)
bb_c = content_bbox(comp)

# 6) difference overlay: mark differing pixels in magenta
overlay = comp_img.copy()
ov = np.asarray(overlay).copy()
ov[diff_mask] = [255, 0, 255]
Image.fromarray(ov).save(f"{DIR}/difference-overlay.png")

# 7) also save a side-by-side + diff heat map for the report
heat = (maxchan * 8).clip(0, 255).astype(np.uint8)
heat_rgb = np.stack([heat, heat, heat], axis=-1)
Image.fromarray(heat_rgb).save(f"{DIR}/difference-heat.png")

report = {
    "viewport": f"{W}x{H}",
    "total_pixels": total,
    "differing_pixels": diff_count,
    "pixel_difference_rate_pct": round(pixel_diff_rate, 6),
    "pixel_similarity_pct": round(pixel_similarity, 6),
    "ssim": round(float(ssim_val), 6),
    "color_mae_rgb": round(float(mae), 6),
    "color_mae_background": round(mae_bg, 6),
    "content_bbox_original": bb_o,
    "content_bbox_component": bb_c,
    "bbox_offset_px": None if bb_o is None or bb_c is None else [bb_c[0]-bb_o[0], bb_c[1]-bb_o[1], bb_c[2]-bb_o[2], bb_c[3]-bb_o[3]],
}

print(json.dumps(report, indent=2, ensure_ascii=False))

with open(f"{DIR}/metrics.json", "w", encoding="utf-8") as f:
    json.dump(report, f, indent=2, ensure_ascii=False)
