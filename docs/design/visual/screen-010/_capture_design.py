#!/usr/bin/env python3
"""design.html 第 ⑩ 屏（回收站 · 封印之地 GRAVEYARD · 昼）独立截图。

使用 Chrome headless 截取：抽取该屏片段生成 standalone HTML，
强制 .phone 为 314×648、@2x 输出 628×1296，与 Android 裁剪同画布。
第 ⑩ 屏设计稿带底部导航（.nav），App 回收站页同样带导航，保持可见。
"""
from __future__ import annotations
import pathlib
import re
import subprocess
import sys

ROOT = pathlib.Path(__file__).resolve().parent
REPO = ROOT.parents[3]
DESIGN = REPO / "design.html"
OUT = ROOT / "design.png"
CHROME = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"

START_MARK = "10 回收站"
END_MARK = "11 删除确认"


def extract() -> str:
    html = DESIGN.read_text(encoding="utf-8")
    i = html.index(START_MARK)
    i = html.index("-->", i) + 3
    j = html.index(END_MARK)
    shot = html[i:j]
    shot = re.sub(r'<div class="cap">.*?</div>\s*', "", shot, count=1, flags=re.S)
    return shot


style = re.search(r"<style>(.*?)</style>", DESIGN.read_text(encoding="utf-8"), re.S).group(1)

overrides = """
html,body{margin:0!important;padding:0!important;background:#E8E0CC;background-image:none;width:314px;height:648px;overflow:hidden}
.page{max-width:none;margin:0;padding:0}
.grid{display:block;margin:0;gap:0;justify-items:stretch}
.shot{position:static}
.cap{display:none!important}
.phone{width:314px;height:648px;border:0;border-radius:0;box-shadow:none;margin:0;padding:0;display:flex;flex-direction:column;overflow:hidden}
"""

standalone = f"""<!DOCTYPE html>
<html lang="zh-CN"><head><meta charset="UTF-8">
<style>{style}</style>
<style>{overrides}</style>
</head><body>{extract()}</body></html>"""

scratch = ROOT / "_design_standalone.html"
scratch.write_text(standalone, encoding="utf-8")

cmd = [
    CHROME,
    "--headless=new",
    "--disable-gpu",
    "--hide-scrollbars",
    "--force-device-scale-factor=2",
    "--window-size=314,648",
    f"--screenshot={OUT}",
    scratch.resolve().as_uri(),
]
r = subprocess.run(cmd, capture_output=True, text=True, timeout=30)
if r.returncode != 0:
    print("Chrome failed:", r.stderr[:500], file=sys.stderr)
    sys.exit(1)
print("wrote", OUT)
