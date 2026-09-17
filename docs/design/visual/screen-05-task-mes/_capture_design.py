#!/usr/bin/env python3
"""design.html 第 ⑥ 屏（任务详情 / QUEST DETAIL · 未来 · 昼）独立截图。

与 screen-04-day 同一做法：抽取该屏片段，注入 override 把 .phone 强制成
314×648、无边框无阴影，整页即内容；@2x 输出 628×1296，与 Android 裁剪同画布。
"""
from __future__ import annotations
import pathlib
import re
from playwright.sync_api import sync_playwright

ROOT = pathlib.Path(__file__).resolve().parent
REPO = ROOT.parents[3]
DESIGN = REPO / "design.html"
OUT = ROOT / "design.png"

START_MARK = "6 任务详情"
END_MARK = "7 成就详情"


def extract() -> str:
    html = DESIGN.read_text(encoding="utf-8")
    i = html.index(START_MARK)
    i = html.index("-->", i) + 3  # 跳过屏号注释本身，避免注释文字被渲染
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

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    ctx = browser.new_context(viewport={"width": 314, "height": 648}, device_scale_factor=2)
    page = ctx.new_page()
    page.goto(scratch.resolve().as_uri())
    page.wait_for_timeout(400)
    page.evaluate("document.fonts.ready")
    page.wait_for_timeout(200)
    page.screenshot(path=str(OUT), full_page=False)
    browser.close()
print("wrote", OUT)
