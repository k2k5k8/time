#!/usr/bin/env python3
"""从 design.html 抽取第 ⑤ 屏（日子簿 · 副本地图），生成与 Android 实现等尺寸基准图。

产物：
  design.png           628x1296 设备像素（= 314x648 CSS px @2x）
  design-geometry.json 全部关键元素的 getBoundingClientRect + getComputedStyle

用法：
    /Users/cch/.workbuddy/binaries/python/envs/default/bin/python3 _capture_design.py
    # 附加：把日历横滑容器定位回第 1 个月（七月）以留证
    ... _capture_design.py month1

关于「可见月份」：design.html 的 `.cal-swipe` 是 3 个 `.cal-month` 的横向滚动容器
（七月 / 八月 / 九月 2026）。DOM 顺序从「七月」开始，因此静态截图默认停在七月的
左边界；但同屏的 `.cal-label` 写的是「八月 2026」、HUD 写 `CAL-08`、`today` 落在
八月 12 日、下方列表标题写「今日掉落 · 08.12 TODO」—— 四处共同指向「八月是当月」。
本脚本默认用 `scrollLeft` 把容器定位到八月，使设计侧截图与其余元素自洽；
`month1` 参数用于保留「未滚动」的原始帧作为证据。
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

START_MARK = "<!-- ═══════════ 5 日子簿"
END_MARK = "<!-- ═══════════ 6 任务详情"

# 3 个 cal-month，DOM 索引 1 = 八月 2026
VISIBLE_MONTH_INDEX = 1


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

MEASURE_PROPS = [
    "padding", "border-width", "border-bottom-width", "border-style", "box-shadow",
    "font-size", "font-weight", "font-family", "line-height", "letter-spacing",
    "background-color", "color", "gap", "margin", "margin-top", "aspect-ratio",
    "grid-template-columns", "justify-content", "align-items", "text-decoration-color",
    "opacity", "border-color", "width", "height",
]


def _measure_script(month_index: int) -> str:
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
  function pick(sel, i) {
    var list = document.querySelectorAll(sel);
    return list[i || 0] || null;
  }
  var PROPS = %s;
  var out = {props: PROPS};

  // 1) 把横滑容器定位到目标月份（默认八月）。
  var swipe = document.querySelector('.cal-swipe');
  var months = document.querySelectorAll('.cal-month');
  if (swipe && months.length > %d) {
    var gap = parseFloat(getComputedStyle(swipe).columnGap || '0') || 0;
    var step = months[0].getBoundingClientRect().width + gap;
    swipe.scrollLeft = step * %d;
  }

  function node(key, el) {
    if (!el) { out[key] = null; return; }
    out[key] = {rect: rect(el), cs: cs(el, PROPS)};
  }

  node('phone', document.querySelector('.phone'));
  node('statusbar', document.querySelector('.statusbar'));
  node('pagetitle', document.querySelector('.pagetitle'));
  node('pagetitle_title', document.querySelector('.pagetitle .t'));
  node('cal_label', document.querySelector('.cal-label'));
  node('cal_arrow_prev', pick('.cal-arrow', 0));
  node('cal_arrow_next', pick('.cal-arrow', 1));
  node('pagetitle_hud', document.querySelector('.pagetitle .r'));

  var scroll = document.querySelector('.form-scroll');
  node('scroll', scroll);
  if (scroll) {
    out.scroll_metrics = {
      scrollHeight: scroll.scrollHeight, clientHeight: scroll.clientHeight,
      overflow_y: getComputedStyle(scroll).overflowY
    };
  }
  var padWrap = scroll ? scroll.firstElementChild : null;
  node('scroll_pad', padWrap);
  if (padWrap) {
    out.scroll_pad_metrics = {
      scrollHeight: padWrap.scrollHeight, clientHeight: padWrap.clientHeight,
      offsetHeight: padWrap.offsetHeight
    };
  }
  node('weekday_row', padWrap ? pick('div.mono', 0) : null);

  node('cal_swipe', swipe);
  node('cal_month', pick('.cal-month', %d));
  node('cal_grid', pick('.cal-grid', %d));

  // 日格：普通 / 本月 / 跨月 dim / today / 带内容标记
  var cells = document.querySelectorAll('.cal-month')[%d]
      ? document.querySelectorAll('.cal-month')[%d].querySelectorAll('.dg') : [];
  var idx = {};
  Array.prototype.forEach.call(cells, function (c, i) {
    if (c.classList.contains('today')) idx.today = i;
    if (c.classList.contains('dim') && idx.dim === undefined) idx.dim = i;
    if (c.querySelector('.fx')) idx.fx = i;
  });
  out.cell_index = idx;
  node('dg_plain', cells[8] || null);      // 第二行第一列（本月普通日）
  node('dg_dim', idx.dim !== undefined ? cells[idx.dim] : null);
  node('dg_today', idx.today !== undefined ? cells[idx.today] : null);
  node('dg_fx', idx.fx !== undefined ? cells[idx.fx] : null);
  if (idx.fx !== undefined) node('dg_fx_mark', cells[idx.fx].querySelector('.fx'));
  if (cells.length) {
    out.cell_count = cells.length;
    out.cell_row_tops = [];
    var prevTop = null, rowTop = null;
    Array.prototype.forEach.call(cells, function (c, i) {
      if (i %% 7 === 0) { out.cell_row_tops.push(+c.getBoundingClientRect().top.toFixed(2)); }
    });
  }

  // 「今日掉落」面板 = .form-scroll 内带 margin-top:12px 的同级块
  var panel = scrapePanel(padWrap);
  function scrapePanel(wrap) {
    if (!wrap) return null;
    var kids = wrap.children;
    for (var i = 0; i < kids.length; i++) {
      var s = getComputedStyle(kids[i]);
      if (parseFloat(s.marginTop) >= 10) return kids[i];
    }
    return null;
  }
  node('task_panel', panel);
  if (panel) {
    node('task_panel_title', panel.children[0]);
    out.task_rows = [];
    Array.prototype.forEach.call(panel.children, function (row, i) {
      if (i === 0) return;
      var r = {i: i, rect: rect(row), cs: cs(row, PROPS)};
      r.children = [];
      Array.prototype.forEach.call(row.children, function (c, j) {
        r.children.push({j: j, cls: c.className, text: (c.textContent || '').trim(),
                         rect: rect(c),
                         cs: cs(c, ['width', 'height', 'border-width', 'border-style',
                                    'font-size', 'font-weight', 'color', 'background-color',
                                    'border-color', 'text-decoration-color', 'opacity',
                                    'padding', 'margin-left'])});
      });
      out.task_rows.push(r);
    });
  }

  node('nav', document.querySelector('.nav'));
  out.nav_items = [];
  Array.prototype.forEach.call(document.querySelectorAll('.nav > *'), function (n, i) {
    out.nav_items.push({i: i, cls: n.className, text: (n.textContent || '').trim(),
                        rect: rect(n),
                        cs: cs(n, ['padding', 'border-width', 'font-size', 'font-weight',
                                   'background-color', 'color', 'box-shadow', 'line-height',
                                   'font-family', 'flex-grow', 'flex'])});
  });
  out.body_bg = getComputedStyle(document.body).backgroundColor;

  document.getElementById('__measure').textContent = JSON.stringify(out);
})();
</script>
""" % (json.dumps(MEASURE_PROPS), VISIBLE_MONTH_INDEX, VISIBLE_MONTH_INDEX,
       VISIBLE_MONTH_INDEX, VISIBLE_MONTH_INDEX, VISIBLE_MONTH_INDEX, VISIBLE_MONTH_INDEX)


def capture(month_index: int = VISIBLE_MONTH_INDEX, tag: str = "") -> None:
    overrides = f"""
html,body{{margin:0!important;padding:0!important;background:#E8E0CC;background-image:none;width:{INNER_W}px;height:{INNER_H}px;overflow:hidden}}
.page{{max-width:none;margin:0;padding:0}}
.grid{{display:block;margin:0;gap:0;justify-items:stretch}}
.shot{{position:static}}
.cap{{display:none!important}}
.phone{{width:{INNER_W}px;height:{INNER_H}px;border:0;border-radius:0;box-shadow:none;margin:0;padding:0;display:flex;flex-direction:column;overflow:hidden}}
.phone .form-scroll{{scrollbar-width:none}}
"""
    shot = extract()
    measure_js = _measure_script(month_index)
    standalone = f"""<!DOCTYPE html>
<html lang="zh-CN"><head><meta charset="UTF-8">
<style>{css}</style>
<style>{overrides}</style>
</head><body>{shot}{measure_js}</body></html>
"""
    scratch = HERE / f"_design_standalone{tag}.html"
    scratch.write_text(standalone, encoding="utf-8")

    raw = HERE / f"_design_raw{tag}.png"
    cmd = [
        CHROME, "--headless=new", "--hide-scrollbars", "--disable-gpu",
        "--force-device-scale-factor=2",
        "--virtual-time-budget=3000",
        f"--window-size={INNER_W},{INNER_H}",
        f"--screenshot={raw}",
        scratch.as_uri(),
    ]
    subprocess.run(cmd, check=True, capture_output=True)
    from PIL import Image
    im = Image.open(raw).convert("RGB")
    if im.size != (INNER_W * SCALE, INNER_H * SCALE):
        im = im.crop((0, 0, INNER_W * SCALE, INNER_H * SCALE))
    out = HERE / (f"design{tag}.png" if tag else "design.png")
    im.save(out)
    print(f"saved {out} {im.size}")

    dump = subprocess.run(
        [CHROME, "--headless=new", "--disable-gpu", f"--window-size={INNER_W},{INNER_H}",
         "--virtual-time-budget=3000", "--dump-dom", scratch.as_uri()],
        check=True, capture_output=True, text=True,
    ).stdout
    m = re.search(r'<pre id="__measure"[^>]*>(.*?)</pre>', dump, re.S)
    assert m, "__measure 未出现在 dump-dom 输出中（脚本未执行或超时）"
    import html as _html
    geom = json.loads(_html.unescape(m.group(1)))
    gpath = HERE / (f"design-geometry{tag}.json" if tag else "design-geometry.json")
    gpath.write_text(json.dumps(geom, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"saved {gpath}")


if __name__ == "__main__":
    if len(sys.argv) > 1 and sys.argv[1] == "month1":
        capture(month_index=0, tag="-month1-unscrolled")
    else:
        capture()
