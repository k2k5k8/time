#!/usr/bin/env python3
"""从根目录 design.html 抽取第 ① 屏，生成无边框、无设备外壳的等尺寸设计基准。

产物：design-original.png（314x648 逻辑像素 @2x = 628x1296 设备像素）
复现：
    /opt/miniconda3/bin/python3 _capture_design.py
"""
from __future__ import annotations
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[4]          # 仓库根目录
HERE = Path(__file__).resolve().parent
DESIGN = ROOT / "design.html"

# HTML .phone 的 CSS 盒：330x664，border 8px，box-sizing:border-box
# => 内部内容区（= App 屏幕）恰好 314 x 648 CSS px。
INNER_W, INNER_H = 314, 648
SCALE = 2                                           # device scale factor
CHROME = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"

html = DESIGN.read_text(encoding="utf-8")

style = re.search(r"<style>(.*?)</style>", html, re.S)
assert style, "design.html <style> 未找到"
css = style.group(1)

start = html.index("<!-- ═══════════ 1 首页")
end = html.index("<!-- ═══════════ 2 铭刻时刻")
shot = html[start:end]

# 去掉截图编号角标（.cap 不属于手机屏幕内容）
shot = re.sub(r'<div class="cap">.*?</div>\s*', "", shot, count=1, flags=re.S)

overrides = f"""
html,body{{margin:0;padding:0;background:#E8E0CC;background-image:none;width:{INNER_W}px;height:{INNER_H}px;overflow:hidden}}
.page{{max-width:none;margin:0;padding:0}}
.grid{{display:block;margin:0;gap:0;justify-items:stretch}}
.shot{{position:static}}
.cap{{display:none!important}}
.phone{{width:{INNER_W}px;height:{INNER_H}px;border:0;border-radius:0;box-shadow:none;margin:0;display:flex;flex-direction:column}}
"""

standalone = f"""<!DOCTYPE html>
<html lang="zh-CN"><head><meta charset="UTF-8">
<style>{css}</style>
<style>{overrides}</style>
</head><body>{shot}</body></html>
"""

scratch = HERE / "_design_standalone.html"
scratch.write_text(standalone, encoding="utf-8")

raw = HERE / "_design_raw.png"
cmd = [
    CHROME, "--headless=new", "--hide-scrollbars", "--disable-gpu",
    "--force-device-scale-factor=2",
    f"--window-size={INNER_W},{INNER_H}",
    f"--screenshot={raw}",
    scratch.as_uri(),
]
subprocess.run(cmd, check=True, capture_output=True)

from PIL import Image  # noqa: E402

im = Image.open(raw).convert("RGB")
if im.size != (INNER_W * SCALE, INNER_H * SCALE):
    im = im.crop((0, 0, INNER_W * SCALE, INNER_H * SCALE))
out = HERE / "design-original.png"
im.save(out)
print(f"saved {out} {im.size} (scale {SCALE}, logical {INNER_W}x{INNER_H})")
