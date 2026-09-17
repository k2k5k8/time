#!/usr/bin/env python3
"""从 design.html 抽取第 ④ 屏（接受新任务），生成与 Android 实现等尺寸基准图。

产物：design.png（628x1296 设备像素 = 314x648 逻辑像素 @2x）

用法：
    /Users/cch/.workbuddy/binaries/python/envs/default/bin/python3 _capture_design.py
"""
from __future__ import annotations
import json
import re
import subprocess
from pathlib import Path

ROOT = Path("/Users/cch/Desktop/Time")
HERE = Path(__file__).resolve().parent
DESIGN = ROOT / "design.html"

INNER_W, INNER_H = 314, 648
SCALE = 2
CHROME = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"

START_MARK = "<!-- ═══════════ 4 新任务 · 待办"
END_MARK = "<!-- ═══════════ 5 日子簿"


def extract() -> str:
    html = DESIGN.read_text(encoding="utf-8")
    i = html.index(START_MARK)
    j = html.index(END_MARK)
    shot = html[i:j]
    # 去掉角标（.cap 不属于手机屏幕内容）
    shot = re.sub(r'<div class="cap">.*?</div>\s*', "", shot, count=1, flags=re.S)
    return shot


style = re.search(r"<style>(.*?)</style>", DESIGN.read_text(encoding="utf-8"), re.S)
assert style, "design.html <style> 未找到"
css = style.group(1)


def _measure_script() -> str:
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
  // 第 ④ 屏：4 个 .px 面板 + 1 个提交按钮 .btn.green
  var labelTitles = ['TASK_NAME', 'DUE_DATE', 'LORE', 'DIFFICULTY', 'SUBMIT'];
  var scrollers = document.querySelectorAll('.px, .btn.green');
  var joined = [];
  document.querySelectorAll('div').forEach(function (el) {
    var cl = el.className || '';
    if (cl.indexOf('px') !== -1 || cl.indexOf('btn green') !== -1) {
      joined.push({el: el, cls: cl});
    }
  });
  // 也把 statusbar / pagetitle 加进来作为顶部对齐基线
  joined.unshift({el: document.querySelector('.statusbar'), cls: 'statusbar'});
  joined.splice(1, 0, {el: document.querySelector('.pagetitle'), cls: 'pagetitle'});
  joined.forEach(function (item, i) {
    var el = item.el;
    if (!el) return;
    var o = {i: i, cls: item.cls, rect: rect(el)};
    o.cs = cs(el, ['padding', 'border-width', 'box-shadow', 'font-size', 'background-color', 'font-weight']);
    out.blocks.push(o);
  });
  document.getElementById('__measure').textContent = JSON.stringify(out);
})();
</script>
"""


def capture() -> None:
    overrides = f"""
html,body{{margin:0!important;padding:0!important;background:#E8E0CC;background-image:none;width:{INNER_W}px;height:{INNER_H}px;overflow:hidden}}
.page{{max-width:none;margin:0;padding:0}}
.grid{{display:block;margin:0;gap:0;justify-items:stretch}}
.shot{{position:static}}
.cap{{display:none!important}}
.phone{{width:{INNER_W}px;height:{INNER_H}px;border:0;border-radius:0;box-shadow:none;margin:0;padding:0;display:flex;flex-direction:column;overflow:hidden}}
"""
    shot = extract()
    measure_js = _measure_script()
    standalone = f"""<!DOCTYPE html>
<html lang="zh-CN"><head><meta charset="UTF-8">
<style>{css}</style>
<style>{overrides}</style>
</head><body>{shot}{measure_js}</body></html>
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
    from PIL import Image
    im = Image.open(raw).convert("RGB")
    if im.size != (INNER_W * SCALE, INNER_H * SCALE):
        im = im.crop((0, 0, INNER_W * SCALE, INNER_H * SCALE))
    out = HERE / "design.png"
    im.save(out)
    print(f"saved {out} {im.size}")

    # 同时导出真实布局几何（CSS px → dp，1:1），供 Compose 对齐使用。
    dump = subprocess.run(
        [CHROME, "--headless=new", "--disable-gpu", f"--window-size={INNER_W},{INNER_H}",
         "--virtual-time-budget=3000", "--dump-dom", scratch.as_uri()],
        check=True, capture_output=True, text=True,
    ).stdout
    m = re.search(r'<pre id="__measure"[^>]*>(.*?)</pre>', dump, re.S)
    assert m, "__measure 未出现在 dump-dom 输出中（脚本未执行或超时）"
    import html as _html
    geom = json.loads(_html.unescape(m.group(1)))
    (HERE / "design-geometry.json").write_text(
        json.dumps(geom, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"saved {HERE / 'design-geometry.json'}")


if __name__ == "__main__":
    capture()
