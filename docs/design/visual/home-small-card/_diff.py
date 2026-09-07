#!/usr/bin/env python3
import json
from pathlib import Path
import numpy as np
from PIL import Image

root = Path(__file__).resolve().parent
orig_img = Image.open(root/'original.png').convert('RGB')
comp_img = Image.open(root/'component.png').convert('RGB')
orig, comp = np.asarray(orig_img).astype(np.float32), np.asarray(comp_img).astype(np.float32)
assert orig.shape == comp.shape
diff = np.abs(orig-comp); mask = diff.max(axis=2)>0
def ssim_gray(a, b):
    """Global SSIM fallback; keeps this audit runnable without scikit-image."""
    x, y = a[..., 0], b[..., 0]
    ux, uy = x.mean(), y.mean()
    vx, vy = x.var(), y.var()
    cov = ((x-ux)*(y-uy)).mean()
    c1, c2 = 6.5025, 58.5225
    return ((2*ux*uy+c1)*(2*cov+c2))/((ux*ux+uy*uy+c1)*(vx+vy+c2))
bg = np.array([232,224,204], dtype=np.float32)
def bbox(a):
    ys,xs=np.nonzero(np.abs(a-bg).max(axis=2)>8)
    return (int(xs.min()),int(ys.min()),int(xs.max()),int(ys.max())) if len(xs) else None
report={'viewport':f'{orig.shape[1]}x{orig.shape[0]}','total_pixels':int(orig.shape[0]*orig.shape[1]),'differing_pixels':int(mask.sum()),'pixel_difference_rate_pct':round(float(mask.mean()*100),6),'pixel_similarity_pct':round(float((1-mask.mean())*100),6),'ssim':round(float(ssim_gray(orig,comp)),6),'color_mae_rgb':round(float(diff.mean()),6),'color_mae_background':round(float(diff[np.abs(orig-bg).max(axis=2)<=8].mean()),6),'content_bbox_original':bbox(orig),'content_bbox_component':bbox(comp)}
report['bbox_offset_px']=[b-a for a,b in zip(report['content_bbox_original'],report['content_bbox_component'])]
ov=np.asarray(comp_img).copy(); ov[mask]=[255,0,255]; Image.fromarray(ov).save(root/'difference-overlay.png')
heat=np.clip(diff.max(axis=2)*8,0,255).astype('uint8'); Image.fromarray(np.stack([heat]*3,axis=-1)).save(root/'difference-heat.png')
(root/'metrics.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8'); print(json.dumps(report,ensure_ascii=False,indent=2))
