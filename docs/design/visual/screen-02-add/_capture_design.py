#!/usr/bin/env python3
"""从 design.html 抽取第 ②③ 屏（铭刻时刻 · 未来 / 过去 两种模式），生成与 Android 实现等尺寸基准图。

产物：design-future.png / design-past.png（628x1296 设备像素 = 314x648 逻辑像素 @2x）
复现：
    /Users/cch/.workbuddy/binaries/python/envs/default/bin/python3 _capture_design.py future
    /Users/cch/.workbuddy/binaries/python/envs/default/bin/python3 _capture_design.py past
"""
from __future__ import annotations
import json
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path("/Users/cch/Desktop/Time")
HERE = Path(__file__).resolve().parent
DESIGN = ROOT / "design.html"

INNER_W, INNER_H = 314, 648
SCALE = 2
CHROME = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"

MARKERS = {
    "future": ("<!-- ═══════════ 2 铭刻时刻 · 未来模式", "<!-- ═══════════ 3 铭刻时刻"),
    "past": ("<!-- ═══════════ 3 铭刻时刻 · 过去模式", "<!-- ═══════════ 4 新任务"),
}


def extract(mode: str) -> str:
    html = DESIGN.read_text(encoding="utf-8")
    start, end = MARKERS[mode]
    i = html.index(start)
    j = html.index(end)
    shot = html[i:j]
    # 去掉角标（.cap 不属于手机屏幕内容）
    shot = re.sub(r'<div class="cap">.*?</div>\s*', "", shot, count=1, flags=re.S)
    return shot


style = re.search(r"<style>(.*?)</style>", DESIGN.read_text(encoding="utf-8"), re.S)
assert style, "design.html <style> 未找到"
css = style.group(1)


def capture(mode: str) -> None:
    overrides = f"""
html,body{{margin:0!important;padding:0!important;background:#E8E0CC;background-image:none;width:{INNER_W}px;height:{INNER_H}px;overflow:hidden}}
.page{{max-width:none;margin:0;padding:0}}
.grid{{display:block;margin:0;gap:0;justify-items:stretch}}
.shot{{position:static}}
.cap{{display:none!important}}
.phone{{width:{INNER_W}px;height:{INNER_H}px;border:0;border-radius:0;box-shadow:none;margin:0;padding:0;display:flex;flex-direction:column;overflow:hidden}}
"""
    shot = extract(mode)
    measure_js = _measure_script()
    standalone = f"""<!DOCTYPE html>
<html lang="zh-CN"><head><meta charset="UTF-8">
<style>{css}</style>
<style>{overrides}</style>
</head><body>{shot}{measure_js}</body></html>
"""
    scratch = HERE / f"_design_standalone_{mode}.html"
    scratch.write_text(standalone, encoding="utf-8")
    raw = HERE / f"_design_raw_{mode}.png"
    cmd = [
        CHROME, "--headless=new", "--hide-scrollbars", "--disable-gpu",
        "--force-device-scale-factor=2",
        f"--window-size={INNER_W},{INNER_H}",
        f"--screenshot={raw}",
        scratch.as_uri(),
    ]
    subprocess.run(cmd, check=True, capture_output=True)
    from PIL import Image
    im = Image.open(raw).convert("RGB")
    if im.size != (INNER_W * SCALE, INNER_H * SCALE):
        im = im.crop((0, 0, INNER_W * SCALE, INNER_H * SCALE))
    out = HERE / f"design-{mode}.png"
    im.save(out)
    print(f"saved {out} {im.size} (mode={mode})")

    # 同时导出真实布局几何（CSS px → dp，1:1），供 Compose 对齐使用。
    dump = subprocess.run(
        [CHROME, "--headless=new", "--disable-gpu", f"--window-size={INNER_W},{INNER_H}",
         "--virtual-time-budget=3000", "--dump-dom", scratch.as_uri()],
        check=True, capture_output=True, text=True,
    ).stdout
    m = re.search(r'<pre id="__measure"[^>]*>(.*?)</pre>', dump, re.S)
    assert m, f"{mode}: __measure 未出现在 dump-dom 输出中（脚本未执行或超时）"
    import html as _html
    geom = json.loads(_html.unescape(m.group(1)))
    (HERE / f"design-geometry-{mode}.json").write_text(
        json.dumps(geom, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"saved {HERE / f'design-geometry-{mode}.json'}")


def _measure_script() -> str:
    """注入测量脚本：把关键元素的 getBoundingClientRect 写入 <pre id="__measure">。"""
    return """
<pre id="__measure" style="display:none"></pre>
<script>
(function () {
  function rect(e) {
    if (!e) return null;
    var r = e.getBoundingClientRect();
    return {x: +r.x.toFixed(2), y: +r.y.toFixed(2),
            w: +r.width.toFixed(2), h: +r.height.toFixed(2)};
  }
  function cs(e, props) {
    if (!e) return null;
    var s = getComputedStyle(e), o = {};
    props.forEach(function (p) { o[p] = s.getPropertyValue(p); });
    return o;
  }
  var out = {};
  out.phone = rect(document.querySelector('.phone'));
  out.statusbar = rect(document.querySelector('.statusbar'));
  out.pagetitle = rect(document.querySelector('.pagetitle'));
  out.pagetitle_back = rect(document.querySelector('.pagetitle .back'));
  out.pagetitle_title = rect(document.querySelector('.pagetitle .t'));
  out.pagetitle_hud = rect(document.querySelector('.pagetitle .r'));
  out.pagetitle_cs = cs(document.querySelector('.pagetitle'),
                        ['padding', 'border-width', 'font-size']);
  out.back_cs = cs(document.querySelector('.pagetitle .back'),
                   ['width', 'height', 'margin-right', 'font-size', 'border-width', 'box-shadow']);
  out.statusbar_cs = cs(document.querySelector('.statusbar'), ['padding', 'font-size', 'font-weight']);
  out.blocks = [];
  var scroller = document.querySelector('.form-scroll');
  if (scroller) {
    out.scroll = rect(scroller);
    var inner = scroller.firstElementChild;
    if (inner) {
      out.scroll_inner = rect(inner);
      out.scroll_inner_cs = cs(inner, ['padding', 'gap']);
      Array.prototype.forEach.call(inner.children, function (el, i) {
        var o = {i: i, cls: el.className, rect: rect(el)};
        o.cs = cs(el, ['padding', 'border-width', 'box-shadow', 'font-size', 'background-color']);
        if (i < 8) {
          // 记录前两层子元素，便于定位 DIRECTION 按钮、日期三格、chip 等。
          o.children = [];
          Array.prototype.forEach.call(el.querySelectorAll(':scope > *'), function (c, j) {
            if (j < 12) {
              o.children.push({j: j, cls: c.className, rect: rect(c),
                               cs: cs(c, ['padding', 'border-width', 'font-size', 'font-weight', 'gap', 'margin-top'])});
            }
          });
        }
        out.blocks.push(o);
      });
    }
  }
  document.getElementById('__measure').textContent = JSON.stringify(out);
})();
</script>
"""


if __name__ == "__main__":
    mode = sys.argv[1] if len(sys.argv) > 1 else "future"
    capture(mode)
